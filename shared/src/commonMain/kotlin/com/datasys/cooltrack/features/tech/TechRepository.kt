package com.datasys.cooltrack.features.tech

import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.secureUpdate
import com.datasys.cooltrack.models.ServiceOrder
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio para la lógica del técnico (Módulo 5c).
 * Maneja las órdenes asignadas y las actualizaciones de estado desde el campo.
 */
class TechRepository(private val supabase: SupabaseClient) {

    /** Obtener órdenes asignadas al técnico. */
    suspend fun getAssignedJobs(technicianId: String): List<ServiceOrder> =
        supabase.from("service_orders")
            .select(Columns.ALL) {
                filter {
                    eq("technician_id", technicianId)
                    // Filtrar para no mostrar las canceladas en la lista principal
                    neq("status", OrderStatus.CANCELLED.value)
                }
                order("scheduled_date", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList()

    /** Actualizar el estado de una orden. */
    suspend fun updateJobStatus(orderId: String, newStatus: OrderStatus) {
        supabase.secureUpdate(
            "service_orders",
            buildJsonObject {
                put("status", newStatus.value)
                put("updated_at", kotlinx.datetime.Clock.System.now().toString())

                // Si se completa, registrar la fecha
                if (newStatus == OrderStatus.COMPLETED) {
                    put("completed_at", kotlinx.datetime.Clock.System.now().toString())
                }
                // Si inicia, registrar la fecha
                if (newStatus == OrderStatus.IN_PROGRESS) {
                    put("started_at", kotlinx.datetime.Clock.System.now().toString())
                }
            },
            match = mapOf("id" to JsonPrimitive(orderId)),
        )
    }

    /** Guardar notas del técnico. */
    suspend fun saveTechnicianNotes(orderId: String, notes: String) {
        supabase.secureUpdate(
            "service_orders",
            buildJsonObject { put("technician_notes", notes) },
            match = mapOf("id" to JsonPrimitive(orderId)),
        )
    }
}
