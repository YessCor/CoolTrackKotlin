package com.datasys.cooltrack.services

/**
 * Equivalente a la API pública de PdfService en lib/services/pdf_service.dart
 * (`generateQuotePdf`, `generateOrderPdf`, `previewQuotePdf`, `previewOrderPdf`).
 *
 * El armado del *contenido* (fechas formateadas, filas de la tabla, totales)
 * es 100% compartido — ver PdfContentBuilder. Lo único que difiere por
 * plataforma es el *motor de dibujo*: Android usa `android.graphics.pdf.PdfDocument`
 * (Canvas), iOS usaría `UIGraphicsPDFRenderer` (Core Graphics). Por eso el
 * renderer final es `expect`/`actual`, mientras que todo el resto de esta
 * clase vive en commonMain.
 */
expect class PdfRenderer() {
    fun renderQuote(content: QuotePdfContent): ByteArray
    fun renderOrder(content: OrderPdfContent): ByteArray

    /** Abre el diálogo nativo de impresión/compartir con el PDF ya generado. */
    fun previewOrShare(bytes: ByteArray, suggestedFileName: String)
}

class PdfService(private val renderer: PdfRenderer) {

    fun generateQuotePdf(content: QuotePdfContent): ByteArray = renderer.renderQuote(content)

    fun generateOrderPdf(content: OrderPdfContent): ByteArray = renderer.renderOrder(content)

    fun previewQuotePdf(content: QuotePdfContent, quoteNumber: Int) {
        val bytes = generateQuotePdf(content)
        renderer.previewOrShare(bytes, "Cotizacion_QT-$quoteNumber.pdf")
    }

    fun previewOrderPdf(content: OrderPdfContent, orderNumber: Int) {
        val bytes = generateOrderPdf(content)
        renderer.previewOrShare(bytes, "Orden_Servicio_$orderNumber.pdf")
    }
}
