package com.datasys.cooltrack.services

import com.datasys.cooltrack.core.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Equivalente a UploadResult en lib/services/photo_upload_service.dart */
data class UploadResult(
    val success: Boolean,
    val url: String? = null,
    val publicId: String? = null,
    val error: String? = null,
)

/**
 * Equivalente a PhotoUploadService (subida directa a Cloudinary, sin pasar
 * por el backend propio — igual que en Dart). El selector de imágenes se
 * delega a ImagePickerService (expect/actual por plataforma).
 */
class PhotoUploadService(
    private val imagePicker: ImagePickerService,
) {
    private companion object {
        const val CLOUDINARY_BASE_URL = "https://api.cloudinary.com/v1_1"
    }

    private val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
            }
        }
    }

    suspend fun pickFromCamera() = imagePicker.pickFromCamera()
    suspend fun pickFromGallery() = imagePicker.pickFromGallery()
    suspend fun pickMultipleFromGallery(maxImages: Int = 10) = imagePicker.pickMultipleFromGallery(maxImages)

    suspend fun uploadPhoto(image: PickedImage, folder: String? = null): UploadResult {
        val cloudName = AppConfig.cloudinaryCloudName
        val uploadPreset = AppConfig.cloudinaryUploadPreset

        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            return UploadResult(success = false, error = "Cloudinary not configured")
        }

        return try {
            val response: HttpResponse = client.post("$CLOUDINARY_BASE_URL/$cloudName/image/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", image.bytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"${image.fileName}\"")
                            })
                            append("upload_preset", uploadPreset)
                            folder?.let { append("folder", it) }
                        }
                    )
                )
            }

            if (response.status.isSuccess()) {
                val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                UploadResult(
                    success = true,
                    url = data["secure_url"]?.jsonPrimitive?.content,
                    publicId = data["public_id"]?.jsonPrimitive?.content,
                )
            } else {
                UploadResult(success = false, error = "Upload failed")
            }
        } catch (e: Exception) {
            UploadResult(success = false, error = e.message ?: "Upload failed")
        }
    }

    suspend fun uploadMultiplePhotos(images: List<PickedImage>, folder: String? = null): List<UploadResult> =
        images.map { uploadPhoto(it, folder) }

    fun deletePhoto(publicId: String) {
        // La eliminación requiere firma server-side o el panel de Cloudinary;
        // se deja como placeholder, igual que en el original de Dart.
    }
}
