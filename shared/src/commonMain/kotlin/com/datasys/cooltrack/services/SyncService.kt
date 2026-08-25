@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.datasys.cooltrack.services

import com.datasys.cooltrack.core.secureDelete
import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureUpdate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Equivalente a SyncStatus (enum) en lib/services/sync_service.dart */
enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

/** Equivalente a SyncResult en lib/services/sync_service.dart */
data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int = 0,
    val error: String? = null,
)

/**
 * Equivalente a SyncService (singleton con listeners) en
 * lib/services/sync_service.dart. Los `listener(SyncStatus)` de Dart se
 * reemplazan por un StateFlow<SyncStatus> observable.
 */
class SyncService(
    private val offlineRepo: OfflineRepository,
    private val photoService: PhotoUploadService,
    private val supabase: SupabaseClient,
) {
    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun syncAll(): SyncResult {
        if (_status.value == SyncStatus.SYNCING) {
            return SyncResult(success = false, error = "Sync already in progress")
        }

        _status.value = SyncStatus.SYNCING

        return try {
            val queue = offlineRepo.getSyncQueue()
            var syncedCount = 0

            for ((rowId, item) in queue) {
                val handled = runCatching { processSyncItem(item) }.getOrDefault(false)
                if (handled) {
                    offlineRepo.removeSyncItem(rowId)
                    syncedCount++
                } else {
                    break // igual que el original: corta ante el primer error
                }
            }

            offlineRepo.setLastSyncTime(Clock.System.now())
            _status.value = SyncStatus.SUCCESS
            SyncResult(success = true, itemsSynced = syncedCount)
        } catch (e: Exception) {
            _status.value = SyncStatus.ERROR
            SyncResult(success = false, error = e.message)
        }
    }

    private suspend fun processSyncItem(item: JsonObject): Boolean {
        val action = item["action"]?.jsonPrimitive?.contentOrNull
        val table = item["table"]?.jsonPrimitive?.contentOrNull
        val data = item["data"]?.jsonObject
        val id = item["id"]?.jsonPrimitive?.contentOrNull

        return when (action) {
            "insert" -> {
                if (table == null || data == null) return false
                supabase.secureInsert<JsonObject>(table, data)
                true
            }
            "update" -> {
                if (table == null || data == null || id == null) return false
                supabase.secureUpdate(table, data, match = mapOf("id" to JsonPrimitive(id)))
                true
            }
            "delete" -> {
                if (table == null || id == null) return false
                supabase.secureDelete(table, match = mapOf("id" to JsonPrimitive(id)))
                true
            }
            "upload_media" -> processMediaUpload(item)
            "upload_signature" -> processSignatureUpload(item)
            else -> false
        }
    }

    private suspend fun processMediaUpload(item: JsonObject): Boolean {
        val fileBytesB64 = item["file_bytes_base64"]?.jsonPrimitive?.contentOrNull ?: return false
        val fileName = item["file_name"]?.jsonPrimitive?.contentOrNull ?: return false
        val metadata = item["metadata"]?.jsonObject ?: return false

        val image = PickedImage(
            path = fileName,
            bytes = kotlin.io.encoding.Base64.decode(fileBytesB64),
            fileName = fileName,
        )
        val context = metadata["context"]?.jsonPrimitive?.contentOrNull ?: "general"
        val uploadResult = photoService.uploadPhoto(image, folder = context)

        if (uploadResult.success && uploadResult.url != null) {
            supabase.secureInsert<JsonObject>(
                "media",
                buildJsonObject {
                    put("url", uploadResult.url)
                    put("public_id", uploadResult.publicId ?: "")
                    put("resource_type", "image")
                    metadata["order_id"]?.let { put("order_id", it) }
                    metadata["equipment_id"]?.let { put("equipment_id", it) }
                    metadata["context"]?.let { put("context", it) }
                    metadata["caption"]?.let { put("caption", it) }
                    supabase.auth.currentUserOrNull()?.id?.let { put("uploaded_by", it) }
                },
            )
            return true
        }
        return false
    }

    private suspend fun processSignatureUpload(item: JsonObject): Boolean {
        val fileBytesB64 = item["file_bytes_base64"]?.jsonPrimitive?.contentOrNull ?: return false
        val fileName = item["file_name"]?.jsonPrimitive?.contentOrNull ?: return false
        val orderId = item["order_id"]?.jsonPrimitive?.contentOrNull ?: return false

        val image = PickedImage(
            path = fileName,
            bytes = kotlin.io.encoding.Base64.decode(fileBytesB64),
            fileName = fileName,
        )
        val uploadResult = photoService.uploadPhoto(image, folder = "signatures")

        if (uploadResult.success && uploadResult.url != null) {
            supabase.secureUpdate(
                "service_orders",
                buildJsonObject { put("client_signature_url", uploadResult.url) },
                match = mapOf("id" to JsonPrimitive(orderId)),
            )
            return true
        }
        return false
    }

    // --- Helpers para encolar (equivalentes a queueMediaUpload, etc.) ---------

    fun queueMediaUpload(fileBytes: ByteArray, fileName: String, orderId: String?, equipmentId: String?, context: String?, caption: String?) {
        offlineRepo.addToSyncQueue(
            buildJsonObject {
                put("action", "upload_media")
                put("file_bytes_base64", kotlin.io.encoding.Base64.encode(fileBytes))
                put("file_name", fileName)
                put("metadata", buildJsonObject {
                    orderId?.let { put("order_id", it) }
                    equipmentId?.let { put("equipment_id", it) }
                    context?.let { put("context", it) }
                    caption?.let { put("caption", it) }
                })
            }
        )
    }

    fun queueSignatureUpload(orderId: String, fileBytes: ByteArray, fileName: String) {
        offlineRepo.addToSyncQueue(
            buildJsonObject {
                put("action", "upload_signature")
                put("order_id", orderId)
                put("file_bytes_base64", kotlin.io.encoding.Base64.encode(fileBytes))
                put("file_name", fileName)
            }
        )
    }

    fun queueOrderUpdate(orderId: String, data: JsonObject) {
        offlineRepo.addToSyncQueue(
            buildJsonObject {
                put("action", "update")
                put("table", "service_orders")
                put("id", orderId)
                put("data", data)
            }
        )
    }

    fun queueQuoteUpdate(quoteId: String, data: JsonObject) {
        offlineRepo.addToSyncQueue(
            buildJsonObject {
                put("action", "update")
                put("table", "quotes")
                put("id", quoteId)
                put("data", data)
            }
        )
    }

    suspend fun queueHistoryLog(orderId: String, status: String, notes: String?) {
        offlineRepo.addToSyncQueue(
            buildJsonObject {
                put("action", "insert")
                put("table", "service_order_history")
                put("data", buildJsonObject {
                    put("order_id", orderId)
                    put("status", status)
                    notes?.let { put("notes", it) }
                    supabase.auth.currentUserOrNull()?.id?.let { put("changed_by", it) }
                })
            }
        )
    }

    suspend fun fetchAndCacheAll() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        runCatching {
            val orders = supabase.from("service_orders")
                .select { filter { or { eq("technician_id", userId); eq("client_id", userId) } } }
                .decodeList<JsonObject>()
            offlineRepo.cacheOrders(orders)

            val equipment = supabase.from("equipment").select().decodeList<JsonObject>()
            offlineRepo.cacheEquipment(equipment)

            val quotes = supabase.from("quotes").select().decodeList<JsonObject>()
            offlineRepo.cacheQuotes(quotes)
        }
        // errores silenciados, igual que el catch + print de debug original
    }

    fun getPendingCount(): Int = offlineRepo.getPendingCount()
    fun getLastSyncTime(): Instant? = offlineRepo.getLastSyncTime()
}
