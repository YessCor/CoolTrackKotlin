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

    val formattedTotal: String get() = formatMoney(total)
    val formattedSubtotal: String get() = formatMoney(subtotal)
    val formattedTax: String get() = formatMoney(taxAmount)
}

/**
 * Formato de moneda único de la app: "$1.234,50" (separador de miles ".",
 * decimales ",", siempre 2 dígitos). Vale para todos los modelos que
 * muestran importes; para locale real más adelante, cambiar solo acá.
 */
fun formatMoney(value: Double): String {
    val rounded = kotlin.math.round(kotlin.math.abs(value) * 100) / 100.0
    val whole = rounded.toLong()
    val cents = kotlin.math.round((rounded - whole) * 100).toInt().toString().padStart(2, '0')
    val wholeStr = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    return (if (value < 0) "-$" else "$") + "$wholeStr,$cents"
}
