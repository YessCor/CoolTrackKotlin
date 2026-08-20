package com.datasys.cooltrack.models

import com.datasys.cooltrack.core.QuoteStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a QuoteItem en lib/models/quote.dart */
@Serializable
data class QuoteItem(
    val id: String,
    @SerialName("quote_id") val quoteId: String,
    @SerialName("catalog_item_id") val catalogItemId: String? = null,
    val description: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    val total: Double,
    @SerialName("created_at") val createdAt: Instant,
)

/** Equivalente a Quote en lib/models/quote.dart */
@Serializable
data class Quote(
    val id: String,
    @SerialName("quote_number") val quoteNumber: Int,
    @SerialName("display_quote_number") val displayQuoteNumber: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("technician_id") val technicianId: String? = null,
    val status: QuoteStatus = QuoteStatus.DRAFT,
    val subtotal: Double,
    @SerialName("tax_rate") val taxRate: Double,
    @SerialName("tax_amount") val taxAmount: Double,
    val total: Double,
    @SerialName("valid_until") val validUntil: Instant? = null,
    val notes: String? = null,
    val terms: String? = null,
    val items: List<QuoteItem>? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
) {
    val statusLabel: String get() = status.label

    // Formateo simple; para moneda/locale real usar una lib de formato (ver PdfService).
    val formattedTotal: String get() = "$" + formatAmount(total)
    val formattedSubtotal: String get() = "$" + formatAmount(subtotal)

    private fun formatAmount(value: Double): String {
        val rounded = kotlin.math.round(value * 100) / 100
        return rounded.toString()
    }
}
