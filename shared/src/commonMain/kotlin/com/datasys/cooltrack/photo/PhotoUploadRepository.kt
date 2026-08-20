package com.datasys.cooltrack.photo

import com.datasys.cooltrack.services.PhotoUploadService
import com.datasys.cooltrack.services.PickedImage
import com.datasys.cooltrack.services.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Equivalente a UploadStatus (enum) en lib/providers/photo_upload_provider.dart */
enum class UploadStatus { IDLE, PICKING, UPLOADING, SUCCESS, ERROR }

/** Equivalente a PhotoUploadState en lib/providers/photo_upload_provider.dart */
data class PhotoUploadUiState(
    val selectedPhotos: List<PickedImage> = emptyList(),
    val status: UploadStatus = UploadStatus.IDLE,
    val error: String? = null,
)

/**
 * Equivalente a PhotoUploadNotifier + `photoUploadProvider.family` de
 * Riverpod. Riverpod resolvía "una instancia distinta por (orderId,
 * equipmentId, context)" con `.family`; en Kotlin/Koin el mismo patrón se
 * logra pidiendo la instancia con parámetros:
 *
 *   val repo: PhotoUploadRepository = koinInject { parametersOf(orderId, equipmentId, context) }
 *
 * (ver `factory { params -> PhotoUploadRepository(...) }` en SharedModule.kt)
 */
class PhotoUploadRepository(
    private val photoService: PhotoUploadService,
    private val syncService: SyncService,
    private val orderId: String? = null,
    private val equipmentId: String? = null,
    private val context: String? = null,
) {
    private val _state = MutableStateFlow(PhotoUploadUiState())
    val state: StateFlow<PhotoUploadUiState> = _state.asStateFlow()

    suspend fun pickFromCamera() {
        _state.value = _state.value.copy(status = UploadStatus.PICKING)
        val photo = photoService.pickFromCamera()
        if (photo != null) {
            _state.value = _state.value.copy(
                selectedPhotos = _state.value.selectedPhotos + photo,
                status = UploadStatus.IDLE,
            )
        } else {
            _state.value = _state.value.copy(status = UploadStatus.IDLE)
        }
    }

    suspend fun pickFromGallery() {
        _state.value = _state.value.copy(status = UploadStatus.PICKING)
        val photo = photoService.pickFromGallery()
        if (photo != null) {
            _state.value = _state.value.copy(
                selectedPhotos = _state.value.selectedPhotos + photo,
                status = UploadStatus.IDLE,
            )
        } else {
            _state.value = _state.value.copy(status = UploadStatus.IDLE)
        }
    }

    fun removePhoto(index: Int) {
        val updated = _state.value.selectedPhotos.toMutableList().apply { removeAt(index) }
        _state.value = _state.value.copy(selectedPhotos = updated)
    }

    /** Método clave: encola las fotos para subida offline-first. */
    suspend fun saveAndQueueUploads() {
        if (_state.value.selectedPhotos.isEmpty()) return

        _state.value = _state.value.copy(status = UploadStatus.UPLOADING)

        try {
            for (photo in _state.value.selectedPhotos) {
                syncService.queueMediaUpload(
                    fileBytes = photo.bytes,
                    fileName = photo.fileName,
                    orderId = orderId,
                    equipmentId = equipmentId,
                    context = context,
                    caption = null,
                )
            }

            _state.value = _state.value.copy(selectedPhotos = emptyList(), status = UploadStatus.SUCCESS)

            // Intenta sincronizar inmediatamente si hay red (igual que el original;
            // no se espera el resultado, corre en segundo plano).
            syncService.syncAll()
        } catch (e: Exception) {
            _state.value = _state.value.copy(status = UploadStatus.ERROR, error = e.message)
        }
    }

    fun reset() {
        _state.value = PhotoUploadUiState()
    }
}
