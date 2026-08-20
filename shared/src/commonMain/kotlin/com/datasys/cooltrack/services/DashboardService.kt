package com.datasys.cooltrack.services

import com.datasys.cooltrack.core.ApiClient
import com.datasys.cooltrack.models.DashboardStats
import kotlinx.serialization.json.jsonObject

/** Equivalente a lib/services/dashboard_service.dart (y el singleton `dashboardService`). */
class DashboardService {
    suspend fun getStats(): DashboardStats {
        val response = ApiClient.get("/dashboard/stats")
        val data = response["data"]!!.jsonObject
        return ApiClient.json.decodeFromJsonElement(DashboardStats.serializer(), data)
    }
}
