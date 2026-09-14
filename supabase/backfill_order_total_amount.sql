-- ============================================================================
--  CoolTrack — Recuperar montos de cotizaciones y registrar ingresos
-- ============================================================================
--  Las cotizaciones creadas antes del fix de parsing de precios quedaron con
--  total/subtotal/tax en 0 (el parser no aceptaba coma decimal, caía a 0.0).
--  Este script recalcula cada cotización a partir de sus propios ítems
--  (quantity * unit_price), según la misma fórmula que usa la app:
--    item.total   = quantity * unit_price
--    subtotal     = sum(item.total)
--    tax_amount   = subtotal * (tax_rate / 100)
--    total        = subtotal + tax_amount
--  Y sincroniza `service_orders.total_amount` para las cotizaciones aprobadas.
--
--  Aplicar UNA SOLA VEZ en Supabase Dashboard → SQL Editor.
-- ============================================================================

-- 1) Recalcular el total de cada ítem de cotización.
update public.quote_items
set total = quantity * unit_price
where total is null or total = 0;

-- 2) Recalcular montos de las cotizaciones a partir de sus ítems.
update public.quotes q
set
  subtotal  = s.subtotal,
  tax_amount = s.subtotal * (q.tax_rate / 100.0),
  total     = s.subtotal + (s.subtotal * (q.tax_rate / 100.0))
from (
  select qi.quote_id, sum(qi.quantity * qi.unit_price) as subtotal
  from public.quote_items qi
  group by qi.quote_id
) s
where s.quote_id = q.id
  and (q.total is null or q.total = 0);

-- 3) Registra el monto acordado en las órdenes vinculadas a cotizaciones
--    aprobadas (ingresos del dashboard y reportes).
update public.service_orders o
set total_amount = q.total
from public.quotes q
where q.order_id = o.id
  and q.status = 'approved'
  and (o.total_amount is null or o.total_amount = 0);