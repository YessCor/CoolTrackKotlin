package com.datasys.cooltrack.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Implementación Android de ImagePickerService.
 *
 * NOTA: la selección real de imagen (abrir cámara/galería) requiere un
 * `ActivityResultLauncher`, que vive en la capa de Activity/Compose, no en
 * `shared`. Este servicio expone las funciones de contrato + el
 * post-procesado (resize/compresión, igual que `maxWidth/maxHeight/quality`
 * en Dart); la Activity de androidApp le pasa la URI ya elegida via
 * `resolveAndCompress(uri, ...)`.
 */
actual class ImagePickerService actual constructor() {

    // Se conecta desde MainActivity con setPendingLauncher / resolveAndCompress.
    // Ver androidApp/.../ImagePickerBridge.kt (módulo de features/UI).

    actual suspend fun pickFromCamera(maxWidth: Int, maxHeight: Int, quality: Int): PickedImage? {
        // El disparo de la cámara ocurre en la Activity (permiso + intent);
        // acá solo queda documentado el contrato. Ver ImagePickerBridge.
        throw NotImplementedError(
            "pickFromCamera se resuelve desde la capa de UI (androidApp) " +
                "vía ActivityResultContracts.TakePicture(), y llama a " +
                "ImagePickerService.compress(bytes, ...) con el resultado."
        )
    }

    actual suspend fun pickFromGallery(maxWidth: Int, maxHeight: Int, quality: Int): PickedImage? =
        pickFromCamera(maxWidth, maxHeight, quality) // mismo patrón: resuelto desde la UI

    actual suspend fun pickMultipleFromGallery(maxImages: Int): List<PickedImage> = emptyList()

    /** Compresión equivalente a `imageQuality`/`maxWidth`/`maxHeight` de image_picker. */
    fun compress(rawBytes: ByteArray, fileName: String, maxWidth: Int, maxHeight: Int, quality: Int): PickedImage {
        val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        val scale = minOf(
            maxWidth.toFloat() / original.width,
            maxHeight.toFloat() / original.height,
            1f,
        )
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true,
            )
        } else {
            original
        }

        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return PickedImage(path = fileName, bytes = output.toByteArray(), fileName = fileName)
    }
}
