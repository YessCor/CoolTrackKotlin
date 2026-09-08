-- ============================================================================
--  CoolTrack — plantilla de políticas RLS
-- ============================================================================
--  La app lee VARIAS tablas DIRECTO desde el cliente con la anon key (sin
--  pasar por la Edge Function `secure-db`). Si RLS deja leer todo a
--  `authenticated`, cualquiera que baje el APK puede leer los datos de
--  todos los clientes/órdenes/cotizaciones.
--
--  Esta plantilla:
--    1. Habilita RLS en todas las tablas.
--    2. Da SELECT acotado por rol + propiedad.
--    3. REVOCA todo INSERT/UPDATE/DELETE a anon/authenticated  →  el único
--       camino de escritura queda siendo `secure-db` (service_role).
--
--  ⚠️  Revisá los nombres de columnas contra tu esquema real antes de correr.
--  Aplicar en: Supabase Dashboard → SQL Editor.
-- ============================================================================

-- --- Helper: rol del usuario actual -----------------------------------------
create or replace function public.current_user_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select role from public.users where id = auth.uid()
$$;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public
as $$ select coalesce(public.current_user_role() = 'admin', false) $$;

-- ============================================================================
--  users
-- ============================================================================
alter table public.users enable row level security;

drop policy if exists users_select on public.users;
create policy users_select on public.users
for select to authenticated
using (
  id = auth.uid()                        -- mi propio perfil
  or public.is_admin()                   -- admin ve todo
  or role in ('admin', 'technician')     -- staff visible (para avisos / nombre del técnico)
);
-- Sin políticas de insert/update/delete: se hace por `secure-db`.

-- ============================================================================
--  service_orders
-- ============================================================================
alter table public.service_orders enable row level security;

drop policy if exists orders_select on public.service_orders;
create policy orders_select on public.service_orders
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or technician_id = auth.uid()
);

-- ============================================================================
--  service_order_history
-- ============================================================================
alter table public.service_order_history enable row level security;

drop policy if exists history_select on public.service_order_history;
create policy history_select on public.service_order_history
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1 from public.service_orders o
    where o.id = service_order_history.order_id
      and (o.client_id = auth.uid() or o.technician_id = auth.uid())
  )
);

-- ============================================================================
--  quotes  /  quote_items
-- ============================================================================
alter table public.quotes enable row level security;

drop policy if exists quotes_select on public.quotes;
create policy quotes_select on public.quotes
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or technician_id = auth.uid()
);

alter table public.quote_items enable row level security;

drop policy if exists quote_items_select on public.quote_items;
create policy quote_items_select on public.quote_items
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1 from public.quotes q
    where q.id = quote_items.quote_id and q.client_id = auth.uid()
  )
);

-- ============================================================================
--  equipment
-- ============================================================================
alter table public.equipment enable row level security;

drop policy if exists equipment_select on public.equipment;
create policy equipment_select on public.equipment
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or public.current_user_role() = 'technician'   -- el técnico ve specs de equipos
);

-- ============================================================================
--  service_catalog  (precios) — lectura para todos los autenticados
-- ============================================================================
alter table public.service_catalog enable row level security;

drop policy if exists catalog_select on public.service_catalog;
create policy catalog_select on public.service_catalog
for select to authenticated
using (true);

-- ============================================================================
--  technician_locations  (GPS de técnicos)
-- ============================================================================
alter table public.technician_locations enable row level security;

drop policy if exists locations_select on public.technician_locations;
create policy locations_select on public.technician_locations
for select to authenticated
using (
  public.is_admin()
  or technician_id = auth.uid()
  -- Si el cliente necesita ver al técnico en camino, agregá:
  -- or exists (
  --   select 1 from public.service_orders o
  --   where o.technician_id = technician_locations.technician_id
  --     and o.client_id = auth.uid()
  --     and o.status in ('assigned','accepted','in_transit','in_progress')
  -- )
);

-- ============================================================================
--  notifications
-- ============================================================================
alter table public.notifications enable row level security;

drop policy if exists notifications_select on public.notifications;
create policy notifications_select on public.notifications
for select to authenticated
using (public.is_admin() or user_id = auth.uid());

-- ============================================================================
--  media  (fotos / firmas)
-- ============================================================================
alter table public.media enable row level security;

drop policy if exists media_select on public.media;
create policy media_select on public.media
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1 from public.service_orders o
    where o.id = media.order_id
      and (o.client_id = auth.uid() or o.technician_id = auth.uid())
  )
  or exists (
    select 1 from public.equipment e
    where e.id = media.equipment_id and e.client_id = auth.uid()
  )
);

-- ============================================================================
--  BLOQUEO DE ESCRITURA DIRECTA
--  Ninguna tabla debe aceptar INSERT/UPDATE/DELETE desde el cliente:
--  todo pasa por la Edge Function `secure-db` (service_role, que ignora RLS).
--  Como no creamos políticas de write arriba, RLS ya las rechaza; este
--  REVOKE es el cinturón extra a nivel de privilegios de tabla.
-- ============================================================================
do $$
declare t text;
begin
  foreach t in array array[
    'users','service_orders','service_order_history','quotes','quote_items',
    'equipment','service_catalog','technician_locations','notifications','media'
  ]
  loop
    execute format('revoke insert, update, delete on public.%I from anon, authenticated', t);
  end loop;
end $$;
