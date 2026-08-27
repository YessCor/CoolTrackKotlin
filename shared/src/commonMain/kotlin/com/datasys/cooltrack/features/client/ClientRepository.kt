package com.datasys.cooltrack.features.client

import com.datasys.cooltrack.core.secureDelete
import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureSelect
import com.datasys.cooltrack.core.secureUpdate
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
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

    /** Obtener cotizaciones del cliente para una orden específica. */
    suspend fun getQuotesForOrder(orderId: String): List<Quote> =
        supabase.from("quotes")
            .select(Columns.ALL) {
                filter { eq("order_id", orderId) }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList()

    /** Obtener detalle de una cotización específica con sus items. */
    suspend fun getQuoteById(quoteId: String): Quote? = try {
        supabase.from("quotes")
            .select(Columns.raw("*, items:quote_items(*)")) {
                filter { eq("id", quoteId) }
            }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    /** Obtener información del equipo. */
    suspend fun getEquipmentById(equipmentId: String): Equipment? = try {
        supabase.from("equipment")
            .select(Columns.ALL) { filter { eq("id", equipmentId) } }
            .decodeSingle()
    } catch (e: Exception) {
        null
    }

    /** Obtener información del técnico (desde la tabla users). */
    suspend fun getTechnicianById(techId: String): User? = try {
        // Usamos secureSelect porque el acceso a la tabla users suele estar restringido
        // en este proyecto para SELECT directo si no es el propio usuario.
        supabase.secureSelect(
            table = "users",
            match = mapOf("id" to JsonPrimitive(techId)),
            single = true
        )
    } catch (e: Exception) {
        null
    }

    /** Obtener todos los equipos del cliente. */
    suspend fun getMyEquipment(clientId: String): List<Equipment> =
        supabase.from("equipment")
            .select(Columns.ALL) {
                filter { eq("client_id", clientId) }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList()

    /** Registrar un nuevo equipo (Cliente). */
    suspend fun createEquipment(
        clientId: String,
        name: String,
        type: String,
        brand: String?,
        model: String?,
        serialNumber: String?,
        capacityTons: Double?,
        location: String?,
        notes: String?
    ): Equipment = supabase.secureInsert(
        "equipment",
        buildJsonObject {
            put("client_id", clientId)
            put("name", name)
            put("type", type)
            brand?.let { put("brand", it) }
            model?.let { put("model", it) }
            serialNumber?.let { put("serial_number", it) }
            capacityTons?.let { put("capacity_tons", it) }
            location?.let { put("location_description", it) }
            notes?.let { put("notes", it) }
        }
    )

    /** Actualizar equipo existente. */
    suspend fun updateEquipment(
        id: String,
        name: String,
        type: String,
        brand: String?,
        model: String?,
        serialNumber: String?,
        capacityTons: Double?,
        location: String?,
        notes: String?
    ) {
        supabase.secureUpdate(
            "equipment",
            buildJsonObject {
                put("name", name)
                put("type", type)
                brand?.let { put("brand", it) }
                model?.let { put("model", it) }
                serialNumber?.let { put("serial_number", it) }
                capacityTons?.let { put("capacity_tons", it) }
                location?.let { put("location_description", it) }
                notes?.let { put("notes", it) }
            },
            match = mapOf("id" to JsonPrimitive(id))
        )
    }

    /** Eliminar equipo. */
    suspend fun deleteEquipment(id: String) {
        supabase.secureDelete("equipment", match = mapOf("id" to JsonPrimitive(id)))
    }

    /** Aceptar o Rechazar cotización. */
    suspend fun updateQuoteStatus(quoteId: String, status: QuoteStatus) {
        supabase.secureUpdate(
            "quotes",
            buildJsonObject { put("status", status.value) },
            match = mapOf("id" to JsonPrimitive(quoteId)),
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
