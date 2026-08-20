package com.datasys.cooltrack.services

import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.QuoteItem
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Datos de la empresa embebidos en el PDF (equivalente a los `static const
 * String company*` de PdfService en Dart).
 */
object CompanyInfo {
    const val NAME = "CoolTrack HVAC Pro"
    const val TAX_ID = "NIT: 900.123.456-7"
    const val ADDRESS = "Calle de la Climatización #123, Bogotá, Colombia"
    const val PHONE = "+57 (601) 555-0199"
    const val EMAIL = "servicios@cooltrack.pro"
    const val WEB = "www.cooltrack.pro"
}

/** Fila lista para dibujar en la tabla de ítems (equivalente a _td/_th por fila). */
data class PdfTableRow(val description: String, val quantity: String, val unitPrice: String, val subtotal: String)

/**
 * "Receta" de contenido para un PDF de cotización — ya con todo formateado
 * (fechas, moneda), independiente del motor de dibujo. Cada plataforma
 * (Android/iOS) la recorre y la pinta con su propio renderer nativo.
 * Reemplaza el árbol de `pw.Widget` de la librería `pdf` de Dart.
 */
data class QuotePdfContent(
    val docTitle: String = "COTIZACIÓN COMERCIAL",
    val docNumber: String,
    val issueDate: String,
    val validUntil: String?,
    val clientName: String,
    val clientIdShort: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val clientAddress: String?,
    val rows: List<PdfTableRow>,
    val subtotal: String,
    val taxLabel: String,
    val taxAmount: String,
    val total: String,
    val notes: String?,
)

/** Igual que QuotePdfContent, pero para el reporte técnico de una orden de servicio. */
data class OrderPdfContent(
    val docTitle: String = "REPORTE TÉCNICO DE SERVICIO",
    val docNumber: String,
    val issueDate: String,
    val clientName: String,
    val clientIdShort: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val clientAddress: String?,
    val serviceType: String,
    val statusLabel: String,
    val scheduledDate: String?,
    val address: String,
    val technicianNotes: String?,
)

/**
 * Construye el contenido a partir de los modelos de dominio — equivalente a
 * la parte de PdfService que arma `_buildClientAndDocInfo`, `_buildItemsTable`,
 * `_buildTotalsSection`, etc., pero separado del dibujo en sí.
 */
object PdfContentBuilder {

    private val dateFormatTz = TimeZone.currentSystemDefault()

    private fun formatDate(instant: Instant): String {
        val dt = instant.toLocalDateTime(dateFormatTz)
        fun pad(n: Int) = n.toString().padStart(2, '0')
        return "${pad(dt.dayOfMonth)}/${pad(dt.monthNumber)}/${dt.year}"
    }

    private fun formatCurrency(value: Double): String {
        val rounded = kotlin.math.round(value * 100) / 100
        return "$" + rounded.toString()
    }

    fun forQuote(quote: Quote, client: User?): QuotePdfContent = QuotePdfContent(
        docNumber = "QT-" + quote.quoteNumber.toString().padStart(5, '0'),
        issueDate = formatDate(quote.createdAt),
        validUntil = quote.validUntil?.let { formatDate(it) },
        clientName = client?.name ?: "Cliente General",
        clientIdShort = client?.id?.take(8)?.uppercase(),
        clientEmail = client?.email,
        clientPhone = client?.phone,
        clientAddress = client?.address,
        rows = (quote.items ?: emptyList()).map(::toRow),
        subtotal = formatCurrency(quote.subtotal),
        taxLabel = "IVA (${(quote.taxRate * 100).toInt()}%)",
        taxAmount = formatCurrency(quote.taxAmount),
        total = formatCurrency(quote.total),
        notes = quote.notes?.takeIf { it.isNotBlank() },
    )

    fun forOrder(order: ServiceOrder, client: User?): OrderPdfContent = OrderPdfContent(
        docNumber = "#" + order.orderNumber,
        issueDate = formatDate(order.createdAt),
        clientName = client?.name ?: "Cliente General",
        clientIdShort = client?.id?.take(8)?.uppercase(),
        clientEmail = client?.email,
        clientPhone = client?.phone,
        clientAddress = client?.address,
        serviceType = order.serviceType,
        statusLabel = order.statusLabel,
        scheduledDate = order.scheduledDate?.let { formatDate(it) },
        address = order.address,
        technicianNotes = order.technicianNotes?.takeIf { it.isNotBlank() },
    )

    private fun toRow(item: QuoteItem) = PdfTableRow(
        description = item.description,
        quantity = item.quantity.toInt().toString(),
        unitPrice = formatCurrency(item.unitPrice),
        subtotal = formatCurrency(item.total),
    )
}
