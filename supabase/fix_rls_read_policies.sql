-- CoolTrack: políticas RLS de LECTURA para las tablas operativas.
--
-- Diagnóstico: las escrituras de la app pasan por la Edge Function
-- `secure-db` (usa `service_role`, evita RLS), pero las LECTURAS se hacen
-- directo contra Postgrest (`supabase.from("...").select()`), y RLS estaba
-- bloqueando esas lecturas para cualquier rol autenticado — por eso
-- dashboards, "Mis Servicios", "Mis Trabajos", etc. siempre mostraban
-- vacío aunque hubiera datos reales.
--
-- Este script SOLO agrega políticas de SELECT (no toca INSERT/UPDATE/
-- DELETE, que ya funcionan vía la Edge Function). Es idempotente: se
-- puede correr más de una vez sin error.
--
-- Cómo aplicarlo: Supabase Dashboard → SQL Editor → pegar todo → Run.

-- Helper: ¿el usuario autenticado es admin? SECURITY DEFINER para poder
-- leer public.users aunque su propio RLS bloquee el SELECT a 'authenticated'.
create or replace function public.is_admin()
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from public.users where id = auth.uid() and role = 'admin'
  );
$$;

-- service_orders: admin ve todo; cliente ve las suyas; técnico ve las asignadas.
alter table public.service_orders enable row level security;
drop policy if exists "service_orders_select" on public.service_orders;
create policy "service_orders_select" on public.service_orders
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or technician_id = auth.uid()
);

-- quotes: mismo patrón que service_orders.
alter table public.quotes enable row level security;
drop policy if exists "quotes_select" on public.quotes;
create policy "quotes_select" on public.quotes
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or technician_id = auth.uid()
);

-- quote_items: visibles si se puede ver la cotización padre.
alter table public.quote_items enable row level security;
drop policy if exists "quote_items_select" on public.quote_items;
create policy "quote_items_select" on public.quote_items
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1 from public.quotes q
    where q.id = quote_items.quote_id
      and (q.client_id = auth.uid() or q.technician_id = auth.uid())
  )
);

-- equipment: admin todo; cliente el suyo; técnico el de sus órdenes asignadas.
alter table public.equipment enable row level security;
drop policy if exists "equipment_select" on public.equipment;
create policy "equipment_select" on public.equipment
for select to authenticated
using (
  public.is_admin()
  or client_id = auth.uid()
  or exists (
    select 1 from public.service_orders so
    where so.equipment_id = equipment.id and so.technician_id = auth.uid()
  )
);

-- service_catalog: catálogo de referencia, visible para cualquier autenticado.
alter table public.service_catalog enable row level security;
drop policy if exists "service_catalog_select" on public.service_catalog;
create policy "service_catalog_select" on public.service_catalog
for select to authenticated
using (true);

-- technician_locations: admin ve todo (mapa de rastreo); técnico ve lo suyo.
alter table public.technician_locations enable row level security;
drop policy if exists "technician_locations_select" on public.technician_locations;
create policy "technician_locations_select" on public.technician_locations
for select to authenticated
using (
  public.is_admin() or technician_id = auth.uid()
);

-- notifications: cada usuario ve solo las suyas.
alter table public.notifications enable row level security;
drop policy if exists "notifications_select" on public.notifications;
create policy "notifications_select" on public.notifications
for select to authenticated
using (
  public.is_admin() or user_id = auth.uid()
);

-- media: admin todo; quien la subió; cliente/técnico de la orden asociada.
alter table public.media enable row level security;
drop policy if exists "media_select" on public.media;
create policy "media_select" on public.media
for select to authenticated
using (
  public.is_admin()
  or uploaded_by = auth.uid()
  or exists (
    select 1 from public.service_orders so
    where so.id = media.order_id
      and (so.client_id = auth.uid() or so.technician_id = auth.uid())
  )
);

-- service_order_history: visible si se puede ver la orden asociada.
alter table public.service_order_history enable row level security;
drop policy if exists "service_order_history_select" on public.service_order_history;
create policy "service_order_history_select" on public.service_order_history
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1 from public.service_orders so
    where so.id = service_order_history.order_id
      and (so.client_id = auth.uid() or so.technician_id = auth.uid())
  )
);
