package com.datasys.cooltrack.core

import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Pequeños helpers para decodificar la clave `"data"` de las respuestas de
 * `ApiClient` (Ktor) a modelos tipados con `kotlinx.serialization`, en vez
 * de repetir `response['data']` + `Model.fromJson(json)` en cada pantalla
 * como hacía cada provider de Riverpod en Flutter.
 *
 * Se usan mucho en las vistas de admin (`features/admin/views`),
 * que en su mayoría llaman a `ApiClient()` directo desde providers locales
 * por pantalla, en vez de pasar por un repositorio central.
 */
suspend inline fun <reified T> ApiClient.getListData(
    path: String,
    queryParams: Map<String, String>? = null,
): List<T> {
    val response = get(path, queryParams)
    val data = response["data"] ?: return emptyList()
    return json.decodeFromJsonElement(data)
}

suspend inline fun <reified T> ApiClient.getObjectDataOrNull(path: String): T? {
    return try {
        val response = get(path)
        val data = response["data"] ?: return null
        json.decodeFromJsonElement(data)
    } catch (_: Exception) {
        null
    }
}
