package com.datasys.cooltrack.features.admin

import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.models.DashboardStats
import com.datasys.cooltrack.models.ServiceCatalog
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Equivalente a features/admin/providers/admin_provider.dart. Todas las
 * consultas van directo contra Supabase (Postgrest), igual que el original
 * — a diferencia de la mayoría de las pantallas de admin, que en Flutter
 * pasaban por el backend REST propio (`ApiClient`).
 *
 * Nota fiel al original: `techniciansProvider`/`allClientsProvider` de acá
 * quedan migrados por paridad, pero ninguna pantalla de admin los termina
 * usando en el proyecto Flutter original (`admin_technicians_screen.dart`/
 * `admin_clients_screen.dart` definen su propio provider local contra el
 * backend REST en su lugar) — se preserva esa misma "duplicación" tal cual
 * estaba en el código fuente.
 */
class AdminRepository(private val supabase: SupabaseClient) {

    @Serializable
    private data class IdRow(val id: String)

    @Serializable
    private data class AmountRow(@SerialName("total_amount") val totalAmount: Double? = null)

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

        val revenueRows = supabase.from("service_orders")
            .select(Columns.list("total_amount")) { filter { eq("status", OrderStatus.COMPLETED.value) } }
            .decodeList<AmountRow>()
        val totalRevenue = revenueRows.sumOf { it.totalAmount ?: 0.0 }

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
                order("created_at", ascending = false)
                limit(limit)
            }
            .decodeList()

    /** Equivalente a techniciansProvider (Supabase-directo; ver nota de clase). */
    suspend fun getActiveTechnicians(): List<User> =
        supabase.from("users")
            .select(Columns.ALL) {
                filter {
                    eq("role", UserRole.TECHNICIAN.value)
                    eq("is_active", true)
                }
            }
            .decodeList()

    /** Equivalente a allClientsProvider (Supabase-directo; ver nota de clase). */
    suspend fun getActiveClients(): List<User> =
        supabase.from("users")
            .select(Columns.ALL) {
                filter {
                    eq("role", UserRole.CLIENT.value)
                    eq("is_active", true)
                }
            }
            .decodeList()

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
            .select(Columns.ALL) { order("name") }
            .decodeList()

    /** Equivalente a updateServicePriceProvider. */
    suspend fun updateServicePrice(id: String, newPrice: Double) {
        supabase.from("service_catalog")
            .update(buildJsonObject { put("base_price", newPrice) }) { filter { eq("id", id) } }
    }
}
