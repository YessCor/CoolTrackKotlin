package com.datasys.cooltrack.services

/** Representa una imagen elegida, ya leída a bytes (equivalente a XFile de image_picker). */
data class PickedImage(
    val path: String,
    val bytes: ByteArray,
    val fileName: String,
)

/**
 * Reemplaza a image_picker. Cada plataforma implementa la UI nativa de
 * selección (PhotoPicker/CameraX en Android, PHPickerViewController/
 * UIImagePickerController en iOS) y devuelve los bytes ya comprimidos.
 */
expect class ImagePickerService() {
    suspend fun pickFromCamera(maxWidth: Int = 1920, maxHeight: Int = 1080, quality: Int = 85): PickedImage?
    suspend fun pickFromGallery(maxWidth: Int = 1920, maxHeight: Int = 1080, quality: Int = 85): PickedImage?
    suspend fun pickMultipleFromGallery(maxImages: Int = 10): List<PickedImage>
}
