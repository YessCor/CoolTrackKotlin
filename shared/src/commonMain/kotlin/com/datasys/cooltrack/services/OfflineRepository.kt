package com.datasys.cooltrack.services

import com.datasys.cooltrack.db.CooltrackDatabase
import com.datasys.cooltrack.db.DatabaseDriverFactory
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Equivalente a lib/services/offline_repository.dart.
 * Hive guardaba listas de `Map<String, dynamic>` por "box"; acá se guarda el
 * mismo JSON crudo como texto en SQLDelight, para no perder flexibilidad de
 * esquema (los datos siguen viniendo tal cual del backend/Supabase), pero
 * ganando una base de datos real por debajo.
 */
class OfflineRepository(driverFactory: DatabaseDriverFactory) {

    private val db = CooltrackDatabase(driverFactory.createDriver())
    private val queries = db.cooltrackQueries
    private val json = Json { ignoreUnknownKeys = true }

    private object Box {
        const val ORDERS = "offline_orders"
        const val EQUIPMENT = "offline_equipment"
        const val QUOTES = "offline_quotes"
        const val SETTINGS = "settings"
    }

    // --- Orders Cache ---------------------------------------------------

    fun cacheOrders(orders: List<JsonObject>) = putList(Box.ORDERS, "orders", orders)
    fun getCachedOrders(): List<JsonObject> = getList(Box.ORDERS, "orders")

    // --- Equipment Cache --------------------------------------------------

    fun cacheEquipment(equipment: List<JsonObject>) = putList(Box.EQUIPMENT, "equipment", equipment)
    fun getCachedEquipment(): List<JsonObject> = getList(Box.EQUIPMENT, "equipment")

    // --- Quotes Cache -----------------------------------------------------

    fun cacheQuotes(quotes: List<JsonObject>) = putList(Box.QUOTES, "quotes", quotes)
    fun getCachedQuotes(): List<JsonObject> = getList(Box.QUOTES, "quotes")

    // --- Sync Queue (el "cerebro" de la sincronización offline) -----------

    fun addToSyncQueue(item: JsonObject) {
        val withTimestamp = JsonObject(
            item.toMutableMap().apply {
                put("timestamp", kotlinx.serialization.json.JsonPrimitive(Clock.System.now().toString()))
            }
        )
        queries.insertSyncItem(withTimestamp.toString(), Clock.System.now().toString())
    }

    /** Devuelve pares (id de fila SQLDelight, contenido) para poder borrar por id real. */
    fun getSyncQueue(): List<Pair<Long, JsonObject>> =
        queries.getAllSyncItems().executeAsList().map { row ->
            row.id to json.parseToJsonElement(row.jsonValue).let { it as JsonObject }
        }

    fun removeSyncItem(rowId: Long) = queries.deleteSyncItem(rowId)

    fun clearSyncQueue() = queries.clearSyncQueue()

    fun getPendingCount(): Int = queries.countSyncItems().executeAsOne().toInt()

    // --- Settings & Last Sync ----------------------------------------------

    fun setLastSyncTime(time: kotlinx.datetime.Instant) =
        putRaw(Box.SETTINGS, "last_sync", time.toString())

    fun getLastSyncTime(): kotlinx.datetime.Instant? =
        getRaw(Box.SETTINGS, "last_sync")?.let { runCatching { kotlinx.datetime.Instant.parse(it) }.getOrNull() }

    /** Configuración genérica; equivalente a saveSetting/getSetting<T> de Dart. */
    fun saveSetting(key: String, value: JsonElement) = putRaw(Box.SETTINGS, key, value.toString())

    fun getSetting(key: String): JsonElement? =
        getRaw(Box.SETTINGS, key)?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }

    // --- Clear all cache (útil para logout) --------------------------------

    fun clearAllCache() {
        queries.clearBox(Box.ORDERS)
        queries.clearBox(Box.EQUIPMENT)
        queries.clearBox(Box.QUOTES)
        clearSyncQueue()
    }

    // --- Helpers internos ---------------------------------------------------

    private fun putList(box: String, key: String, items: List<JsonObject>) {
        queries.putEntry(box, key, JsonArray(items).toString())
    }

    private fun getList(box: String, key: String): List<JsonObject> {
        val raw = getRaw(box, key) ?: return emptyList()
        return runCatching {
            json.parseToJsonElement(raw).let { it as JsonArray }.map { it as JsonObject }
        }.getOrDefault(emptyList())
    }

    private fun putRaw(box: String, key: String, value: String) = queries.putEntry(box, key, value)

    private fun getRaw(box: String, key: String): String? =
        queries.getEntry(box, key).executeAsOneOrNull()
}
