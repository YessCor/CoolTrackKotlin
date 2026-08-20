package com.datasys.cooltrack.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Equivalente a ApiException en lib/core/api_client.dart */
class ApiException(
    message: String,
    val statusCode: Int? = null,
    val data: JsonElement? = null,
) : Exception(message) {
    override fun toString(): String = "ApiException: $message (status: $statusCode)"
}

/**
 * Equivalente a ApiClient (Dio) en lib/core/api_client.dart, ahora sobre Ktor.
 * Es un singleton multiplataforma: la misma instancia sirve para Android e iOS.
 */
object ApiClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var authToken: String? = null

    private val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
            }
            install(Logging) {
                level = LogLevel.INFO // equivalente a los prints con kDebugMode
            }
            defaultRequest {
                url(AppConfig.apiBaseUrl)
                headers.append(HttpHeaders.Accept, "application/json")
            }
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    suspend fun get(path: String, queryParams: Map<String, String>? = null): JsonObject {
        val response = client.get(path) {
            authHeader()
            queryParams?.forEach { (k, v) -> parameter(k, v) }
        }
        return handleResponse(response)
    }

    suspend fun post(path: String, body: JsonElement? = null): JsonObject {
        val response = client.post(path) {
            authHeader()
            contentType(ContentType.Application.Json)
            body?.let { setBody(it) }
        }
        return handleResponse(response)
    }

    suspend fun put(path: String, body: JsonElement? = null): JsonObject {
        val response = client.put(path) {
            authHeader()
            contentType(ContentType.Application.Json)
            body?.let { setBody(it) }
        }
        return handleResponse(response)
    }

    suspend fun patch(path: String, body: JsonElement? = null): JsonObject {
        val response = client.patch(path) {
            authHeader()
            contentType(ContentType.Application.Json)
            body?.let { setBody(it) }
        }
        return handleResponse(response)
    }

    suspend fun delete(path: String): JsonObject {
        val response = client.delete(path) { authHeader() }
        return handleResponse(response)
    }

    /**
     * Sube un archivo (fotos, firmas). `fileBytes` se obtiene con el selector
     * nativo de cada plataforma (ver PhotoUploadService, módulo de servicios).
     */
    suspend fun uploadFile(
        path: String,
        fileBytes: ByteArray,
        fileName: String,
        fieldName: String,
    ): JsonObject {
        val response = client.post(path) {
            authHeader()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(fieldName, fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                )
            )
        }
        return handleResponse(response)
    }

    private fun HttpRequestBuilder.authHeader() {
        authToken?.let { headers { append(HttpHeaders.Authorization, "Bearer $it") } }
    }

    private suspend fun handleResponse(response: HttpResponse): JsonObject {
        val bodyText = response.bodyAsTextSafe()
        val parsed = runCatching { json.parseToJsonElement(bodyText).jsonObject }.getOrNull()

        if (response.status.isSuccess()) {
            return parsed ?: JsonObject(emptyMap())
        }

        val message = parsed?.get("message")?.jsonPrimitive?.contentOrNull()
            ?: parsed?.get("error")?.jsonPrimitive?.contentOrNull()
            ?: "Error del servidor"

        throw ApiException(
            message = mapStatusMessage(response.status, message),
            statusCode = response.status.value,
            data = parsed,
        )
    }

    private fun mapStatusMessage(status: HttpStatusCode, fallback: String): String = when (status) {
        HttpStatusCode.RequestTimeout -> "Tiempo de conexión agotado"
        else -> fallback
    }
}

private suspend fun HttpResponse.bodyAsTextSafe(): String =
    runCatching { body<String>() }.getOrDefault("")

private fun JsonPrimitive?.contentOrNull(): String? =
    this?.let { if (it.isString) it.content else it.toString() }
