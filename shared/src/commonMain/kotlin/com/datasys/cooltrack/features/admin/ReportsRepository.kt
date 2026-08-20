package com.datasys.cooltrack.features.admin

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a TechPerformance en reports_provider.dart. */
data class TechPerformance(val name: String, val completedOrders: Int, val averageRating: Double)

/** Equivalente a RevenueByService en reports_provider.dart. */
data class RevenueByService(val serviceType: String, val amount: Double)

/** Equivalente al resultado de reportsProvider (el `Map<String, dynamic>` original). */
data class ReportsData(
    val techPerformance: List<TechPerformance>,
    val revenueByService: List<RevenueByService>,
)

/**
 * Equivalente a features/admin/providers/reports_provider.dart.
 *
 * `technicianPerformanceResponse` original usa el `select` embebido de
 * PostgREST (`users!service_orders_technician_id_fkey(name)`) para traer
 * el nombre del técnico en la misma consulta — se replica con
 * `Columns.raw(...)`, el mismo mecanismo "escape hatch" de postgrest-kt
 * para columnas/joins que no tienen un builder tipado dedicado. Si la
 * versión de postgrest-kt resuelta difiere, este es el punto más probable
 * a ajustar (ver también la nota de `AdminQuoteNewScreen.kt`).
 *
 * Nota fiel al original: `completedOrders` cuenta cuántas órdenes
 * *tienen rating* (`client_rating > 0`) por técnico, no el total de
 * órdenes completadas — así estaba en el Dart original (`techStats[techId]`
 * solo acumula cuando `rating > 0`, y `completedOrders = e.value.length`).
 */
class ReportsRepository(private val supabase: SupabaseClient) {

    @Serializable
    private data class NestedUser(val name: String)

    @Serializable
    private data class TechPerformanceRow(
        @SerialName("technician_id") val technicianId: String? = null,
        @SerialName("client_rating") val clientRating: Double? = null,
        val status: String? = null,
        val users: NestedUser? = null,
    )

    @Serializable
    private data class RevenueRow(
        @SerialName("service_type") val serviceType: String? = null,
        @SerialName("total_amount") val totalAmount: Double? = null,
    )

    suspend fun getReports(): ReportsData {
        val techRows = supabase.from("service_orders")
            .select(Columns.raw("technician_id, client_rating, status, users!service_orders_technician_id_fkey(name)")) {
                filter { eq("status", "completed") }
            }
            .decodeList<TechPerformanceRow>()

        val techRatings = mutableMapOf<String, MutableList<Double>>()
        val techNames = mutableMapOf<String, String>()
        for (row in techRows) {
            val techId = row.technicianId ?: continue
            row.users?.name?.let { techNames[techId] = it }
            techRatings.getOrPut(techId) { mutableListOf() }
            val rating = row.clientRating ?: 0.0
            if (rating > 0) techRatings.getValue(techId).add(rating)
        }
        val performances = techRatings.map { (techId, ratings) ->
            TechPerformance(
                name = techNames[techId] ?: "Unknown",
                completedOrders = ratings.size,
                averageRating = if (ratings.isEmpty()) 0.0 else ratings.average(),
            )
        }

        val revenueRows = supabase.from("service_orders")
            .select(Columns.list("service_type", "total_amount")) { filter { eq("status", "completed") } }
            .decodeList<RevenueRow>()

        val revenueMap = mutableMapOf<String, Double>()
        for (row in revenueRows) {
            val type = row.serviceType ?: "Otros"
            revenueMap[type] = (revenueMap[type] ?: 0.0) + (row.totalAmount ?: 0.0)
        }
        val revenues = revenueMap.map { (type, amount) -> RevenueByService(type, amount) }

        return ReportsData(techPerformance = performances, revenueByService = revenues)
    }
}
