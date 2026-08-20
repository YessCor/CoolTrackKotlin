package com.datasys.cooltrack.services

/**
 * Implementación iOS de ImagePickerService.
 *
 * Igual que en Android, la UI de selección (PHPickerViewController /
 * UIImagePickerController) necesita un `UIViewController` presentador, que
 * vive en la capa de SwiftUI/iosApp, no en `shared`. Este actor documenta el
 * contrato; el iosApp llama a un puente Swift que entrega los bytes ya
 * comprimidos.
 */
actual class ImagePickerService actual constructor() {

    actual suspend fun pickFromCamera(maxWidth: Int, maxHeight: Int, quality: Int): PickedImage? {
        throw NotImplementedError(
            "pickFromCamera se resuelve desde iosApp (Swift) presentando " +
                "UIImagePickerController con sourceType = .camera, y entrega " +
                "los bytes ya redimensionados/comprimidos a este servicio."
        )
    }

    actual suspend fun pickFromGallery(maxWidth: Int, maxHeight: Int, quality: Int): PickedImage? =
        pickFromCamera(maxWidth, maxHeight, quality) // mismo patrón: resuelto desde iosApp con PHPickerViewController

    actual suspend fun pickMultipleFromGallery(maxImages: Int): List<PickedImage> = emptyList()
}
