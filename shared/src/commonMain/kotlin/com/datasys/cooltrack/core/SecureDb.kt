package com.datasys.cooltrack.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SecureDbException(message: String) : Exception(message)

@PublishedApi
internal val secureDbJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Proxy hacia la Edge Function `secure-db` (ver `/supabase/functions/secure-db`
 * en la raíz del repo). Es necesaria porque este proyecto de Supabase tiene
 * RLS configurado para rechazar CUALQUIER escritura directa desde un cliente
 * (anon o `authenticated`, sin importar el rol) en todas las tablas
 * probadas — y además bloquea el SELECT de `users` para `authenticated`.
 * La función corre con `service_role` del lado del servidor, nunca en la
 * app, y valida quién llama antes de tocar la base.
 */
@PublishedApi
internal suspend fun SupabaseClient.callSecureDb(payload: JsonObject): JsonElement {
    // DEBUG BYPASS: Si el id es mock, no llamar a la función
    val userId = this.auth.currentUserOrNull()?.id
    if (userId?.startsWith("mock-") == true) {
        return JsonNull
    }

    val response = functions.invoke("secure-db") {
        header(HttpHeaders.ContentType, "application/json")
        setBody(payload.toString())
    }
    val text = response.bodyAsText()
    val parsed = runCatching { secureDbJson.parseToJsonElement(text).jsonObject }.getOrNull()
        ?: throw SecureDbException("Respuesta inválida de secure-db: $text")
    parsed["error"]?.jsonPrimitive?.content?.let { throw SecureDbException(it) }
    return parsed["data"] ?: JsonNull
}

suspend inline fun <reified T> SupabaseClient.secureSelect(
    table: String,
    match: Map<String, JsonElement> = emptyMap(),
    columns: String? = null,
    single: Boolean = false,
): T {
    val payload = buildJsonObject {
        put("table", table)
        put("op", "select")
        if (match.isNotEmpty()) put("match", JsonObject(match))
        columns?.let { put("columns", it) }
        put("single", single)
    }
    return secureDbJson.decodeFromJsonElement(callSecureDb(payload))
}

suspend inline fun <reified T> SupabaseClient.secureInsert(
    table: String,
    values: JsonObject,
    single: Boolean = true,
): T {
    val payload = buildJsonObject {
        put("table", table)
        put("op", "insert")
        put("values", values)
        put("single", single)
    }
    return secureDbJson.decodeFromJsonElement(callSecureDb(payload))
}

suspend fun SupabaseClient.secureUpdate(
    table: String,
    values: JsonObject,
    match: Map<String, JsonElement>,
) {
    val payload = buildJsonObject {
        put("table", table)
        put("op", "update")
        put("values", values)
        put("match", JsonObject(match))
    }
    callSecureDb(payload)
}

suspend fun SupabaseClient.secureDelete(
    table: String,
    match: Map<String, JsonElement>,
) {
    val payload = buildJsonObject {
        put("table", table)
        put("op", "delete")
        put("match", JsonObject(match))
    }
    callSecureDb(payload)
}
