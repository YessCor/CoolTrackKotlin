// Edge Function: secure-db
//
// Proxy autorizado hacia la base de datos de CoolTrack. Corre con
// `service_role` (salta RLS) SOLO del lado del servidor; la app Android
// nunca tiene esa llave. Toda operación se valida acá contra:
//   1. la sesión del que llama (JWT de Supabase Auth),
//   2. su rol en `public.users` (admin | technician | client),
//   3. la PROPIEDAD de la fila afectada (client_id / technician_id / user_id),
//   4. una lista blanca de columnas que ese rol puede escribir.
// Lo que no encaja en una regla explícita se rechaza (deny by default).
//
// --- Historia ---
// La versión anterior dejaba que CUALQUIER usuario autenticado (incluido un
// cliente recién auto-registrado) leyera y escribiera CUALQUIER fila de casi
// todas las tablas: solo comprobaba "¿hay sesión?" antes de operar con
// service_role. Esta versión cierra eso.
//
// Deploy: Supabase Dashboard → Edge Functions → secure-db → pegar este
// archivo → Deploy.  SUPABASE_URL / SUPABASE_ANON_KEY /
// SUPABASE_SERVICE_ROLE_KEY vienen inyectados en el entorno de la función.
//
// IMPORTANTE tras desplegar: revisá también las políticas RLS de las tablas
// que la app lee DIRECTO (sin pasar por acá). Ver /supabase/RLS_policies.sql
// y /SECURITY.md.

import { createClient, SupabaseClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

const ADMIN = "admin";
const TECH = "technician";
const CLIENT = "client";

type Op = "select" | "insert" | "update" | "delete";
type Row = Record<string, unknown>;

interface RequestBody {
  table: string;
  op: Op;
  values?: Row | Row[];
  match?: Row;
  columns?: string;
  single?: boolean;
}

// --- Columnas que un rol NO-admin puede escribir en cada tabla ------------
const CLIENT_ORDER_INSERT = new Set([
  "client_id", "equipment_id", "service_type", "description", "priority",
  "address", "latitude", "longitude", "status", "created_at", "updated_at",
]);
const CLIENT_ORDER_UPDATE = new Set(["client_rating", "client_feedback", "updated_at"]);
const TECH_ORDER_UPDATE = new Set([
  "status", "technician_notes", "started_at", "completed_at",
  "scheduled_date", "client_signature_url", "updated_at",
]);
const EQUIPMENT_FIELDS = new Set([
  "client_id", "name", "type", "brand", "model", "serial_number",
  "capacity_tons", "location_description", "installation_date",
  "last_service_date", "notes", "created_at", "updated_at",
]);
const CLIENT_QUOTE_UPDATE = new Set(["status"]);
const NOTIF_INSERT = new Set(["user_id", "title", "message", "order_id", "type", "created_at"]);
const NOTIF_UPDATE = new Set(["is_read"]);
const HISTORY_INSERT = new Set(["order_id", "status", "notes", "changed_by", "created_at"]);
const LOCATION_INSERT = new Set([
  "technician_id", "latitude", "longitude", "accuracy", "heading", "speed", "recorded_at",
]);
const MEDIA_INSERT = new Set([
  "url", "public_id", "resource_type", "format", "bytes", "order_id",
  "equipment_id", "context", "caption", "uploaded_by", "created_at",
]);

// PII oculta al devolver filas de `users` a quien no es admin ni el dueño.
const USER_PII = ["email", "phone", "address", "latitude", "longitude"];

function pick(obj: Row, allowed: Set<string>): Row {
  const out: Row = {};
  for (const k of Object.keys(obj)) if (allowed.has(k)) out[k] = obj[k];
  return out;
}
function redactUser<T extends Row | null>(row: T): T {
  if (!row) return row;
  const r = { ...row } as Row;
  for (const k of USER_PII) if (k in r) r[k] = k === "email" ? "" : null;
  return r as T;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Método no permitido" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "Falta el header Authorization" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

  const callerClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: callerAuth, error: callerErr } = await callerClient.auth.getUser();
  if (callerErr || !callerAuth.user) return json({ error: "Sesión inválida o expirada" }, 401);
  const callerId = callerAuth.user.id;
  const callerEmail = callerAuth.user.email ?? "";

  const admin: SupabaseClient = createClient(supabaseUrl, serviceRoleKey);

  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Body inválido, se esperaba JSON" }, 400);
  }
  const { table, op, match, columns, single } = body;
  let values = body.values;
  if (!table || !op) return json({ error: "Faltan 'table' y/o 'op'" }, 400);
  if (!["select", "insert", "update", "delete"].includes(op)) {
    return json({ error: `Operación inválida: ${op}` }, 400);
  }

  // Perfil / rol del que llama (se autocrea la primera vez, como cliente).
  let profile =
    (await admin.from("users").select("*").eq("id", callerId).maybeSingle()).data as Row | null;
  if (!profile) {
    profile = (await admin
      .from("users")
      .insert({ id: callerId, email: callerEmail, name: callerEmail.split("@")[0] || "usuario", role: CLIENT })
      .select()
      .single()).data as Row | null;
  }
  const role = (profile?.role as string) ?? CLIENT;
  const isAdmin = role === ADMIN;
  const isTech = role === TECH;

  const deny = (msg = "No autorizado para esta operación") => json({ error: msg }, 403);
  const matchId = typeof match?.id === "string" ? (match!.id as string) : null;
  const one = () => (Array.isArray(values) ? null : (values as Row | undefined)) ?? {};

  // Fila que una update/delete va a afectar — para chequear propiedad.
  const targetRow = async (): Promise<Row | null> => {
    if (!match || Object.keys(match).length === 0) return null;
    let q = admin.from(table).select("*");
    for (const [k, v] of Object.entries(match)) q = q.eq(k, v as never);
    return (await q.maybeSingle()).data as Row | null;
  };

  // Ejecuta la operación ya autorizada/saneada.
  const run = async () => {
    let result;
    if (op === "select") {
      let q = admin.from(table).select(columns ?? "*");
      for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
      result = single ? await q.maybeSingle() : await q;
    } else if (op === "insert") {
      const q = admin.from(table).insert(values as never);
      result = single ? await q.select().single() : await q.select();
    } else if (op === "update") {
      let q = admin.from(table).update(values as never);
      for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
      result = single ? await q.select().single() : await q.select();
    } else {
      let q = admin.from(table).delete();
      for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
      result = await q;
    }
    if (result.error) return json({ error: result.error.message }, 400);
    return json({ data: result.data });
  };

  try {
    // ---------- users ----------
    if (table === "users") {
      if (op === "select") {
        if (matchId === callerId) return json({ data: profile });
        if (isAdmin) return await run();
        // No-admin: solo puede mirar STAFF (fan-out de avisos, nombre del
        // técnico asignado). Nunca listar clientes.
        if (match?.role === CLIENT || (!match?.role && !matchId)) return deny("No podés listar usuarios");
        const res = await run();
        const parsed = await res.json();
        const data = Array.isArray(parsed.data) ? parsed.data.map(redactUser) : redactUser(parsed.data);
        return json({ data });
      }
      const v = one();
      const isSelfRegister =
        op === "insert" && !Array.isArray(values) && v.id === callerId && (v.role ?? CLIENT) === CLIENT;
      if (!isSelfRegister && !isAdmin) return deny("Requiere rol admin");
      return await run();
    }

    // ---------- service_catalog ----------
    if (table === "service_catalog") {
      if (op !== "select" && !isAdmin) return deny("El catálogo solo lo edita un admin");
      return await run();
    }

    // ---------- equipment ----------
    if (table === "equipment") {
      if (isAdmin || op === "select") return await run();
      if (op === "insert") {
        if (Array.isArray(values)) return deny();
        values = pick({ ...one(), client_id: callerId }, EQUIPMENT_FIELDS);
        return await run();
      }
      const row = await targetRow();
      if (!row) return json({ error: "Equipo no encontrado" }, 404);
      if (row.client_id !== callerId) return deny("Ese equipo no es tuyo");
      if (op === "update") values = pick(one(), EQUIPMENT_FIELDS);
      return await run();
    }

    // ---------- service_orders ----------
    if (table === "service_orders") {
      if (op === "select" || isAdmin) return await run();
      if (op === "insert") {
        if (isTech || Array.isArray(values)) return deny();
        const v: Row = { ...one(), client_id: callerId, status: "pending" };
        delete v.technician_id; delete v.total_amount; delete v.client_rating; delete v.client_feedback;
        values = pick(v, CLIENT_ORDER_INSERT);
        return await run();
      }
      if (op === "delete") return deny("Solo un admin puede borrar órdenes");
      const row = await targetRow();
      if (!row) return json({ error: "Orden no encontrada" }, 404);
      if (isTech) {
        if (row.technician_id !== callerId) return deny("Esa orden no está asignada a vos");
        values = pick(one(), TECH_ORDER_UPDATE);
      } else {
        if (row.client_id !== callerId) return deny("Esa orden no es tuya");
        values = pick(one(), CLIENT_ORDER_UPDATE);
      }
      return await run();
    }

    // ---------- service_order_history ----------
    if (table === "service_order_history") {
      if (op === "insert" && (isAdmin || isTech)) {
        if (!Array.isArray(values)) values = pick(one(), HISTORY_INSERT);
        return await run();
      }
      if (op === "select" && isAdmin) return await run();
      return deny();
    }

    // ---------- quotes ----------
    if (table === "quotes") {
      if (op === "select" || isAdmin) return await run();
      if (op === "insert" || op === "delete" || isTech) return deny("Solo un admin gestiona cotizaciones");
      const row = await targetRow();
      if (!row) return json({ error: "Cotización no encontrada" }, 404);
      if (row.client_id !== callerId) return deny("Esa cotización no es tuya");
      const v = pick(one(), CLIENT_QUOTE_UPDATE);
      if (v.status !== "approved" && v.status !== "rejected") return deny("Solo podés aprobar o rechazar");
      values = v;
      // Primero se aprueba/rechaza la cotización.
      const quoteResult = await run();
      if (quoteResult.status !== 200) return quoteResult;
      // Después, best-effort: si se aprobó una cotización vinculada a una
      // orden, registrar el total acordado en `service_orders.total_amount`
      // (alimenta los ingresos del dashboard y reportes). Si falla, no se
      // bloquea la aprobación.
      if (v.status === "approved" && row.order_id) {
        const quoted = row.total as number | string | null | undefined;
        const total = typeof quoted === "number" ? quoted
          : typeof quoted === "string" && quoted !== "" ? Number(quoted) : NaN;
        if (!Number.isNaN(total)) {
          await admin
            .from("service_orders")
            .update({ total_amount: total })
            .eq("id", row.order_id);
        }
      }
      return quoteResult;
    }

    // ---------- quote_items ----------
    if (table === "quote_items") {
      if (op === "select" || isAdmin) return await run();
      return deny("Solo un admin gestiona los ítems de cotización");
    }

    // ---------- notifications ----------
    if (table === "notifications") {
      if (op === "select") {
        if (!isAdmin && match?.user_id !== callerId) return deny("Solo tus notificaciones");
        return await run();
      }
      if (op === "insert") {
        values = Array.isArray(values)
          ? (values as Row[]).map((x) => ({ ...pick(x, NOTIF_INSERT), is_read: false }))
          : { ...pick(one(), NOTIF_INSERT), is_read: false };
        return await run();
      }
      if (!isAdmin) {
        const row = await targetRow();
        if (!row) return json({ error: "Notificación no encontrada" }, 404);
        if (row.user_id !== callerId) return deny();
      }
      if (op === "update") values = pick(one(), NOTIF_UPDATE);
      return await run();
    }

    // ---------- technician_locations ----------
    if (table === "technician_locations") {
      if (op === "select") {
        if (isAdmin || (isTech && match?.technician_id === callerId)) return await run();
        return deny("Sin acceso al rastreo");
      }
      if (op === "insert" && isTech && !Array.isArray(values)) {
        values = { ...pick(one(), LOCATION_INSERT), technician_id: callerId };
        return await run();
      }
      return deny();
    }

    // ---------- media ----------
    if (table === "media") {
      if (op === "select") return await run();
      if (op === "insert" && !Array.isArray(values)) {
        values = { ...pick(one(), MEDIA_INSERT), uploaded_by: callerId };
        return await run();
      }
      if (op === "delete" && isAdmin) return await run();
      return deny();
    }

    return json({ error: `Tabla no permitida: ${table}` }, 400);
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
