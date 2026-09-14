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

    /**
     * Tasa de IVA en porcentaje, tolerando ambas convenciones de la tabla:
     * la app nueva guarda `16.0` (porcentaje) y la legada guardaba `0.16`
     * (fracción). Por eso el `* 100` al mostrarla.
     */
    val taxRatePercent: Double get() = if (taxRate > 1) taxRate else taxRate * 100

    /**
     * Totales efectivos calculados desde los ítems (`cantidad × unitario`, la
     * fuente de verdad), con respaldo a los campos persistentes cuando no hay
     * ítems. Permite recuperar importes correctos aunque el total persistido
     * quedara en 0 por cotizaciones creadas con el bug de parseo de precios.
     */
    val effectiveSubtotal: Double get() = items?.sumOf { it.quantity * it.unitPrice } ?: subtotal
    val effectiveTax: Double get() = items?.let { effectiveSubtotal * (taxRatePercent / 100.0) } ?: taxAmount
    val effectiveTotal: Double get() = items?.let { effectiveSubtotal + effectiveTax } ?: total

    val formattedTotal: String get() = formatMoney(effectiveTotal)
    val formattedSubtotal: String get() = formatMoney(effectiveSubtotal)
    val formattedTax: String get() = formatMoney(effectiveTax)
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

/**
 * Parsea un importe escrito por el usuario tolerando formatos de la región:
 * "1500", "1.500", "1.500,50", "1500,50", "1500.50", "$ 1.500,50".
 * Devuelve null si no es un número válido.
 *
 * `toDoubleOrNull()` de Kotlin solo acepta el punto como decimal, por lo que
 * cualquier coma o separador de miles hacía que el valor cayera a 0 en las
 * cotizaciones y en la edición del catálogo.
 */
fun parseMoney(text: String): Double? {
    var s = text.trim().replace(Regex("[^\\d.,-]"), "")
    if (s.isEmpty()) return null
    val negative = s.startsWith("-")
    if (negative) s = s.drop(1)
    val hasComma = s.contains(',')
    val hasDot = s.contains('.')
    val normalized: String = when {
        hasComma && hasDot -> {
            // El último separador es el decimal; el otro es el de miles.
            val decimal = if (s.lastIndexOf(',') > s.lastIndexOf('.')) ',' else '.'
            val thousands = if (decimal == ',') '.' else ','
            s.replace(thousands.toString(), "").replace(decimal.toString(), ".")
        }
        hasComma -> s.replace(",", ".")
        hasDot -> s
        else -> s
    }
    val parts = normalized.split('.')
    val last = parts.last()
    // Si queda un único punto y la última parte es un grupo de 3 dígitos y las
    // anteriores son grupos de cifras, era separador de miles ("1.500").
    val isThousands = parts.size > 1 && last.length == 3 &&
        parts.dropLast(1).all { it.isNotEmpty() && it.all { c -> c.isDigit() } }
    val value = if (isThousands) normalized.replace(".", "") else normalized
    return value.toDoubleOrNull()?.let { if (negative) -it else it }
}
