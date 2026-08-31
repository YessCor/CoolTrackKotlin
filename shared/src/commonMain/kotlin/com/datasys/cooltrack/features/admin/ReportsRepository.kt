package com.datasys.cooltrack.features.admin

import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.core.secureSelect
import com.datasys.cooltrack.models.User
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

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
 * El Dart original trae el nombre del técnico con un `select` embebido de
 * PostgREST (`users!service_orders_technician_id_fkey(name)`). Ese join se
 * eliminó porque RLS bloquea el SELECT de la tabla `users` para usuarios
 * `authenticated` (ver `SecureDb.kt`), lo que hacía fallar toda la consulta.
 * Acá las órdenes completadas se leen de `service_orders` (que sí tiene
 * política de lectura para admin) y los nombres de los técnicos se resuelven
 * por separado vía `secure-db` (service_role).
 *
 * Nota fiel al original: `completedOrders` cuenta cuántas órdenes
 * *tienen rating* (`client_rating > 0`) por técnico, no el total de
 * órdenes completadas — así estaba en el Dart original (`techStats[techId]`
 * solo acumula cuando `rating > 0`, y `completedOrders = e.value.length`).
 */
class ReportsRepository(private val supabase: SupabaseClient) {

    @Serializable
    private data class TechPerformanceRow(
        @SerialName("technician_id") val technicianId: String? = null,
        @SerialName("client_rating") val clientRating: Double? = null,
        val status: String? = null,
    )

    @Serializable
    private data class RevenueRow(
        @SerialName("service_type") val serviceType: String? = null,
        @SerialName("total_amount") val totalAmount: Double? = null,
    )

    suspend fun getReports(): ReportsData {
        // Las lecturas de service_orders van por secure-db (service_role) y no
        // por PostgREST directo, para no depender de que las políticas RLS de
        // LECTURA estén aplicadas (el fix_rls_read_policies.sql). Así el
        // informe de rendimiento funciona siempre, también para la distribución
        // de ingresos.
        val techRows = supabase.secureSelect<List<TechPerformanceRow>>(
            "service_orders",
            match = mapOf(
                "status" to JsonPrimitive("completed"),
            ),
            columns = "technician_id,client_rating,status",
        )

        val techRatings = mutableMapOf<String, MutableList<Double>>()
        val techIds = mutableSetOf<String>()
        for (row in techRows) {
            val techId = row.technicianId ?: continue
            techIds.add(techId)
            techRatings.getOrPut(techId) { mutableListOf() }
            val rating = row.clientRating ?: 0.0
            if (rating > 0) techRatings.getValue(techId).add(rating)
        }

        // Los nombres de los técnicos se resuelven vía secure-db (service_role),
        // igual que el resto de lecturas de la tabla `users`.
        val techNames = mutableMapOf<String, String>()
        if (techIds.isNotEmpty()) {
            val technicians = supabase.secureSelect<List<User>>(
                "users",
                match = mapOf("role" to JsonPrimitive(UserRole.TECHNICIAN.value)),
            )
            technicians.forEach { techNames[it.id] = it.name }
        }

        val performances = techRatings.map { (techId, ratings) ->
            TechPerformance(
                name = techNames[techId] ?: "Unknown",
                completedOrders = ratings.size,
                averageRating = if (ratings.isEmpty()) 0.0 else ratings.average(),
            )
        }

        val revenueRows = supabase.secureSelect<List<RevenueRow>>(
            "service_orders",
            match = mapOf("status" to JsonPrimitive("completed")),
            columns = "service_type,total_amount",
        )

        val revenueMap = mutableMapOf<String, Double>()
        for (row in revenueRows) {
            val type = row.serviceType ?: "Otros"
            revenueMap[type] = (revenueMap[type] ?: 0.0) + (row.totalAmount ?: 0.0)
        }
        val revenues = revenueMap.map { (type, amount) -> RevenueByService(type, amount) }

        return ReportsData(techPerformance = performances, revenueByService = revenues)
    }
}
