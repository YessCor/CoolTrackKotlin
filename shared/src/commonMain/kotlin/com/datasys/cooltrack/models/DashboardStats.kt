package com.datasys.cooltrack.models

import kotlinx.serialization.Serializable

/** Equivalente a lib/models/dashboard_stats.dart */
@Serializable
data class DashboardStats(
    val totalOrders: Int = 0,
    val activeOrders: Int = 0,
    val completedOrders: Int = 0,
    val pendingQuotes: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRating: Double = 0.0,
) {
    val formattedRevenue: String get() = "$" + (kotlin.math.round(totalRevenue * 100) / 100)
    val formattedRating: String get() = (kotlin.math.round(averageRating * 10) / 10).toString()
}
