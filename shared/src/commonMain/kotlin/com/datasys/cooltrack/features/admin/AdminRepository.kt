package com.datasys.cooltrack.features.admin

import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.core.secureDelete
import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureSelect
import com.datasys.cooltrack.core.secureUpdate
import com.datasys.cooltrack.models.Client
import com.datasys.cooltrack.models.DashboardStats
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.ServiceCatalog
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Equivalente a features/admin/providers/admin_provider.dart.
 *
 * Los SELECT sobre `service_orders`/`quotes`/`equipment`/`service_catalog`
 * van directo contra Postgrest (RLS los deja leer sin problema). Todo lo
 * que toca la tabla `users` (SELECT incluido) y cualquier INSERT/UPDATE/
 * DELETE en cualquier tabla pasa por la Edge Function `secure-db` — este
 * proyecto de Supabase tiene RLS configurado para rechazar esas
 * operaciones desde el cliente sin importar el rol autenticado. Ver
 * `/supabase/functions/secure-db` y `core/SecureDb.kt`.
 */
class AdminRepository(private val supabase: SupabaseClient) {

    @Serializable
    private data class IdRow(val id: String)

    @Serializable
    private data class CompletedRevenueRow(
        val id: String? = null,
        @SerialName("total_amount") val totalAmount: Double? = null,
    )

    @Serializable
    private data class QuoteRevenueRow(
        @SerialName("order_id") val orderId: String? = null,
        val total: Double? = null,
        @SerialName("tax_rate") val taxRate: Double? = null,
        val items: List<QuoteRevenueItem>? = null,
    )

    @Serializable
    private data class QuoteRevenueItem(
        val quantity: Double? = null,
        @SerialName("unit_price") val unitPrice: Double? = null,
    )

    @Serializable
    private data class RatingRow(@SerialName("client_rating") val clientRating: Int? = null)

    private val activeStatuses = listOf(
        OrderStatus.PENDING,
        OrderStatus.ASSIGNED,
        OrderStatus.ACCEPTED,
        OrderStatus.IN_TRANSIT,
        OrderStatus.IN_PROGRESS,
    )

    /** Equivalente a adminDashboardStatsProvider. */
    suspend fun getDashboardStats(): DashboardStats {
        val totalOrders = supabase.from("service_orders")
            .select(Columns.list("id"))
            .decodeList<IdRow>()
            .size

        val activeOrders = supabase.from("service_orders")
            .select(Columns.list("id")) {
                filter { or { activeStatuses.forEach { eq("status", it.value) } } }
            }
            .decodeList<IdRow>()
            .size

        val completedOrders = supabase.from("service_orders")
            .select(Columns.list("id")) { filter { eq("status", OrderStatus.COMPLETED.value) } }
            .decodeList<IdRow>()
            .size

        val pendingQuotes = supabase.from("quotes")
            .select(Columns.list("id")) { filter { eq("status", "sent") } }
            .decodeList<IdRow>()
            .size

        // Ingresos: toda cotización aprobada cuenta como ingreso (esté vinculada o
        // no a una orden), y las órdenes completadas aportan su `total_amount`
        // solo cuando NO tienen una cotización aprobada — así nunca queda en
        // $0 por trabajos aprobados/completados y no se duplican montos.
        val completedRows = supabase.from("service_orders")
            .select(Columns.list("id,total_amount")) { filter { eq("status", OrderStatus.COMPLETED.value) } }
            .decodeList<CompletedRevenueRow>()
        val approvedQuotes = supabase.from("quotes")
            .select(Columns.raw("order_id,total,tax_rate,items:quote_items(quantity,unit_price)")) { filter { eq("status", QuoteStatus.APPROVED.value) } }
            .decodeList<QuoteRevenueRow>()
        val approvedOrderIds = approvedQuotes.mapNotNull { it.orderId }.toSet()

        // Total efectivo de una cotización aprobada: si el persisted quedó en 0
        // (datos creados antes del fix de precios) se recalcula desde los ítems.
        fun approvedQuoteTotal(q: QuoteRevenueRow): Double {
            val stored = q.total ?: 0.0
            if (stored > 0) return stored
            val subtotal = q.items?.sumOf { (it.quantity ?: 0.0) * (it.unitPrice ?: 0.0) } ?: 0.0
            if (subtotal <= 0) return 0.0
            val rate = q.taxRate ?: 16.0
            val percent = if (rate > 1) rate else rate * 100.0
            return subtotal + subtotal * percent / 100.0
        }

        val totalRevenue =
            approvedQuotes.sumOf(::approvedQuoteTotal) +
                completedRows
                    .filter { it.id != null && it.id !in approvedOrderIds }
                    .sumOf { it.totalAmount ?: 0.0 }

        // El original filtra en la query con `.not('client_rating', 'is', null)`;
        // acá se decodifica todo y se descartan los null con `mapNotNull`
        // (mismo resultado, sin depender de un filtro IS NOT NULL del DSL).
        val ratingRows = supabase.from("service_orders")
            .select(Columns.list("client_rating"))
            .decodeList<RatingRow>()
            .mapNotNull { it.clientRating }
        val averageRating = if (ratingRows.isEmpty()) 0.0 else ratingRows.average()

        return DashboardStats(
            totalOrders = totalOrders,
            activeOrders = activeOrders,
            completedOrders = completedOrders,
            pendingQuotes = pendingQuotes,
            totalRevenue = totalRevenue,
            averageRating = averageRating,
        )
    }

    /** Equivalente a recentOrdersProvider. */
    suspend fun getRecentOrders(limit: Long = 5): List<ServiceOrder> =
        supabase.from("service_orders")
            .select(Columns.ALL) {
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    /** Equivalente a techniciansProvider. */
    suspend fun getActiveTechnicians(): List<User> =
        supabase.secureSelect(
            "users",
            match = mapOf("role" to JsonPrimitive(UserRole.TECHNICIAN.value), "is_active" to JsonPrimitive(true)),
        )

    /** Equivalente a allClientsProvider. */
    suspend fun getActiveClients(): List<User> =
        supabase.secureSelect(
            "users",
            match = mapOf("role" to JsonPrimitive(UserRole.CLIENT.value), "is_active" to JsonPrimitive(true)),
        )

    /** Equivalente a adminOrderDetailProvider.family. */
    suspend fun getOrderDetail(id: String): ServiceOrder? = try {
        supabase.from("service_orders")
            .select(Columns.ALL) { filter { eq("id", id) } }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    /** Equivalente a serviceCatalogProvider. */
    suspend fun getServiceCatalog(): List<ServiceCatalog> =
        supabase.from("service_catalog")
            .select(Columns.ALL) { order("name", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING) }
            .decodeList()

    /** Equivalente a updateServicePriceProvider. */
    suspend fun updateServicePrice(id: String, newPrice: Double) {
        supabase.secureUpdate(
            "service_catalog",
            buildJsonObject { put("base_price", newPrice) },
            match = mapOf("id" to JsonPrimitive(id)),
        )
    }

    // --- Clientes -----------------------------------------------------------

    /** Todos los clientes (activos e inactivos), para admin_clients_screen. */
    suspend fun getAllClients(): List<Client> =
        supabase.secureSelect("users", match = mapOf("role" to JsonPrimitive(UserRole.CLIENT.value)))

    suspend fun getClientById(id: String): Client? = try {
        supabase.secureSelect(
            "users",
            match = mapOf("id" to JsonPrimitive(id), "role" to JsonPrimitive(UserRole.CLIENT.value)),
            single = true,
        )
    } catch (e: Exception) {
        null
    }

    /**
     * Crea el perfil del cliente en `public.users` vía la Edge Function
     * `secure-db` (admin-only). No crea una cuenta de Supabase Auth — eso
     * requiere `service_role`, que solo vive del lado de la función, nunca
     * en el cliente — mismo comportamiento que el endpoint REST original,
     * que tampoco pedía contraseña para este flujo.
     */
    suspend fun createClient(name: String, email: String, phone: String?, address: String?): Client =
        supabase.secureInsert(
            "users",
            buildJsonObject {
                put("name", name)
                put("email", email)
                put("role", UserRole.CLIENT.value)
                phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                address?.takeIf { it.isNotBlank() }?.let { put("address", it) }
            },
        )

    suspend fun updateClient(id: String, name: String, email: String?, phone: String?, address: String?) {
        supabase.secureUpdate(
            "users",
            buildJsonObject {
                put("name", name)
                email?.let { put("email", it) }
                phone?.let { put("phone", it) }
                address?.let { put("address", it) }
            },
            match = mapOf("id" to JsonPrimitive(id)),
        )
    }

    // --- Técnicos -----------------------------------------------------------

    /** Todos los técnicos (activos e inactivos), para admin_technicians_screen. */
    suspend fun getAllTechnicians(): List<User> =
        supabase.secureSelect("users", match = mapOf("role" to JsonPrimitive(UserRole.TECHNICIAN.value)))

    /**
     * Crea solo el perfil del técnico en `public.users` (mismo límite que
     * [createClient]: crear la cuenta de Auth con contraseña requiere
     * `service_role`, disponible solo dentro de la Edge Function, y crear
     * cuentas de acceso para terceros es una decisión de producto que se
     * dejó fuera de esta función a propósito — ver comentario en
     * `secure-db/index.ts`).
     */
    suspend fun createTechnicianProfile(name: String, email: String, phone: String?): User =
        supabase.secureInsert(
            "users",
            buildJsonObject {
                put("name", name)
                put("email", email)
                put("role", UserRole.TECHNICIAN.value)
                phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
            },
        )

    // --- Equipos --------------------------------------------------------------

    suspend fun getAllEquipment(clientId: String? = null): List<Equipment> =
        supabase.from("equipment")
            .select(Columns.ALL) {
                clientId?.let { filter { eq("client_id", it) } }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList()

    suspend fun getEquipmentById(id: String): Equipment? = try {
        supabase.from("equipment")
            .select(Columns.ALL) { filter { eq("id", id) } }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    suspend fun createEquipment(fields: Map<String, JsonElement>): Equipment =
        supabase.secureInsert("equipment", buildJsonObject { fields.forEach { (k, v) -> put(k, v) } })

    suspend fun updateEquipment(id: String, fields: Map<String, JsonElement>) {
        supabase.secureUpdate(
            "equipment",
            buildJsonObject { fields.forEach { (k, v) -> put(k, v) } },
            match = mapOf("id" to JsonPrimitive(id)),
        )
    }

    suspend fun deleteEquipment(id: String) {
        supabase.secureDelete("equipment", match = mapOf("id" to JsonPrimitive(id)))
    }

    // --- Órdenes --------------------------------------------------------------

    /** Todas las órdenes, para admin_orders_screen (a diferencia de [getRecentOrders], sin límite). */
    suspend fun getAllOrders(): List<ServiceOrder> =
        supabase.from("service_orders")
            .select(Columns.ALL) { order("created_at", order = Order.DESCENDING) }
            .decodeList()

    // --- Cotizaciones -----------------------------------------------------

    /** Todas las cotizaciones con sus items embebidos, para admin_quotes_screen. */
    suspend fun getAllQuotes(): List<Quote> =
        supabase.from("quotes")
            .select(Columns.raw("*, items:quote_items(*)")) { order("created_at", order = Order.DESCENDING) }
            .decodeList()

    suspend fun getQuoteById(id: String): Quote? = try {
        supabase.from("quotes")
            .select(Columns.raw("*, items:quote_items(*)")) { filter { eq("id", id) } }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    /** Obtener cotizaciones vinculadas a una orden específica. */
    suspend fun getQuotesForOrder(orderId: String): List<Quote> =
        supabase.from("quotes")
            .select(Columns.raw("*, items:quote_items(*)")) {
                filter { eq("order_id", orderId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList()
}
