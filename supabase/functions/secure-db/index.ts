// Edge Function: secure-db
//
// Proxy de escritura (y, para `users`, también de lectura) para toda la
// base de datos de CoolTrack. Se descubrió en producción que este
// proyecto de Supabase tiene RLS configurado para que NINGUNA escritura
// directa desde un cliente (anon o `authenticated`) sea aceptada en
// ninguna tabla probada (`users`, `equipment`, `quotes`) — todo INSERT
// devuelve 42501 "new row violates row-level security policy" sin
// importar el rol del usuario. La tabla `users` además bloquea el SELECT
// para `authenticated` (RLS filtra todo, devuelve 200 con `[]`).
//
// Esto es consistente con la arquitectura original de la app: un backend
// REST propio (con `service_role`) mediaba todas las escrituras; Supabase
// solo se exponía directo para lecturas (dashboards, reportes, tracking).
// Esta función reemplaza a ese backend para las escrituras, sin necesidad
// de levantar un servidor aparte.
//
// Deploy: Supabase Dashboard → Edge Functions → "Deploy a new function" →
// nombre "secure-db" → pegar este archivo → Deploy. SUPABASE_URL,
// SUPABASE_ANON_KEY y SUPABASE_SERVICE_ROLE_KEY ya vienen inyectados
// automáticamente en el entorno de cada función.

import { createClient } from "jsr:@supabase/supabase-js@2";

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

// Tablas "operativas": cualquier usuario autenticado (cliente, técnico o
// admin) puede leer/escribir en ellas. `users` se maneja aparte porque es
// la tabla de identidad/roles — ver lógica más abajo.
const OPEN_TABLES = new Set([
  "service_orders",
  "service_order_history",
  "quotes",
  "quote_items",
  "equipment",
  "service_catalog",
  "technician_locations",
  "notifications",
  "media",
]);

type Op = "select" | "insert" | "update" | "delete";

interface RequestBody {
  table: string;
  op: Op;
  values?: Record<string, unknown> | Record<string, unknown>[];
  match?: Record<string, unknown>;
  columns?: string;
  single?: boolean;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

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

  // Cliente con service_role: puede leer/escribir cualquier tabla sin RLS.
  // Nunca sale de este entorno de función; el cliente Android no lo tiene.
  const admin = createClient(supabaseUrl, serviceRoleKey);

  let body: RequestBody;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Body inválido, se esperaba JSON" }, 400);
  }
  const { table, op, values, match, columns, single } = body;
  if (!table || !op) return json({ error: "Faltan 'table' y/o 'op'" }, 400);

  async function callerProfile() {
    const { data } = await admin.from("users").select("*").eq("id", callerId).maybeSingle();
    return data;
  }

  if (table === "users") {
    if (op === "select") {
      // "Traer mi propio perfil" está siempre permitido (y se autocompleta
      // la primera vez, ej. justo después de un signUp). Cualquier otro
      // select (listar clientes/técnicos, etc.) requiere admin.
      const isOwnProfile = match?.id === callerId;
      if (!isOwnProfile) {
        const me = await callerProfile();
        if (!me || me.role !== "admin") return json({ error: "Requiere rol admin" }, 403);
      } else {
        let profile = await callerProfile();
        if (!profile) {
          const { data, error } = await admin
            .from("users")
            .insert({ id: callerId, email: callerEmail, name: callerEmail.split("@")[0], role: "client" })
            .select()
            .single();
          if (error) return json({ error: error.message }, 400);
          profile = data;
        }
        return json({ data: profile });
      }
    } else {
      // insert/update/delete sobre `users`: admin, salvo el caso de
      // auto-registro (un usuario nuevo insertando su propia fila como
      // cliente — mismo flujo que AuthRepository.register()).
      const isSelfRegister =
        op === "insert" &&
        !Array.isArray(values) &&
        values?.id === callerId &&
        (values?.role ?? "client") === "client";
      if (!isSelfRegister) {
        const me = await callerProfile();
        if (!me || me.role !== "admin") return json({ error: "Requiere rol admin" }, 403);
      }
    }
  } else if (!OPEN_TABLES.has(table)) {
    return json({ error: `Tabla no permitida: ${table}` }, 400);
  }
  // Para OPEN_TABLES: cualquier usuario autenticado (ya validado arriba) puede operar.

  try {
    let query = admin.from(table);
    let result;

    switch (op) {
      case "select": {
        let q = query.select(columns ?? "*");
        for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
        result = single ? await q.maybeSingle() : await q;
        break;
      }
      case "insert": {
        let q = query.insert(values as never);
        result = single ? await q.select().single() : await q.select();
        break;
      }
      case "update": {
        let q = query.update(values as never);
        for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
        result = single ? await q.select().single() : await q.select();
        break;
      }
      case "delete": {
        let q = query.delete();
        for (const [k, v] of Object.entries(match ?? {})) q = q.eq(k, v as never);
        result = await q;
        break;
      }
      default:
        return json({ error: `Operación inválida: ${op}` }, 400);
    }

    if (result.error) return json({ error: result.error.message }, 400);
    return json({ data: result.data });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
