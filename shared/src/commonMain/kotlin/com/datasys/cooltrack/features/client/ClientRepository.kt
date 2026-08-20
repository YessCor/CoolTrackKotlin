package com.datasys.cooltrack.features.client

import com.datasys.cooltrack.models.ServiceOrder
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
                order("created_at", ascending = false)
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
        supabase.from("service_orders")
            .update(kotlinx.serialization.json.buildJsonObject {
                put("client_rating", rating)
                feedback?.let { put("client_feedback", it) }
            }) { filter { eq("id", orderId) } }
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
    ): ServiceOrder {
        return supabase.from("service_orders")
            .insert(buildJsonObject {
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
            }) {
                select()
            }
            .decodeSingle()
    }
}
