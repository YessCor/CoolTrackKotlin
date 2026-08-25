package com.datasys.cooltrack.features.client

import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureUpdate
import com.datasys.cooltrack.models.ServiceOrder
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio para la lógica del cliente (Módulo 5d).
 * Maneja el historial de servicios y la creación de nuevas solicitudes.
 */
class ClientRepository(private val supabase: SupabaseClient) {

    /** Obtener historial de órdenes del cliente. */
    suspend fun getMyOrders(clientId: String): List<ServiceOrder> =
        supabase.from("service_orders")
            .select(Columns.ALL) {
                filter { eq("client_id", clientId) }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList()

    /** Obtener detalle de una orden por ID. */
    suspend fun getOrderDetail(orderId: String): ServiceOrder? = try {
        supabase.from("service_orders")
            .select(Columns.ALL) { filter { eq("id", orderId) } }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    /** Calificar una orden completada. */
    suspend fun rateOrder(orderId: String, rating: Int, feedback: String?) {
        supabase.secureUpdate(
            "service_orders",
            buildJsonObject {
                put("client_rating", rating)
                feedback?.let { put("client_feedback", it) }
            },
            match = mapOf("id" to JsonPrimitive(orderId)),
        )
    }

    /** Crear una nueva solicitud de servicio. */
    suspend fun createServiceRequest(
        clientId: String,
        equipmentId: String?,
        serviceType: String,
        description: String,
        address: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): ServiceOrder = supabase.secureInsert(
        "service_orders",
        buildJsonObject {
            put("client_id", clientId)
            equipmentId?.let { put("equipment_id", it) }
            put("service_type", serviceType)
            put("description", description)
            put("address", address)
            latitude?.let { put("latitude", it) }
            longitude?.let { put("longitude", it) }
            put("status", "pending")
            put("created_at", kotlinx.datetime.Clock.System.now().toString())
            put("updated_at", kotlinx.datetime.Clock.System.now().toString())
        },
    )
}
