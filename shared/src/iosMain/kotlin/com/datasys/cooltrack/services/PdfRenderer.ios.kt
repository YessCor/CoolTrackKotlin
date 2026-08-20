package com.datasys.cooltrack.services

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.writeToFile
import platform.UIKit.NSStringDrawingContext
import platform.UIKit.NSTextAlignment
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.Foundation.NSAttributedString
import platform.Foundation.NSFontAttributeName
import platform.Foundation.NSForegroundColorAttributeName

/**
 * Implementación iOS de PdfRenderer usando `UIGraphicsPDFRenderer` (Core
 * Graphics), el equivalente nativo de iOS a `PdfDocument`/`Canvas` en
 * Android. Dibuja el mismo contenido (header, datos de cliente, tabla,
 * totales) definido en PdfContent.kt — misma "receta", motor distinto.
 *
 * Nota: el layout aquí es intencionalmente más simple que en Android (texto
 * en bloques verticales, sin tabla con bordes dibujados a mano) para
 * mantener el código legible; se puede refinar visualmente sin tocar
 * PdfContentBuilder ni el resto de la capa compartida.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PdfRenderer actual constructor() {

    private val pageWidth = 595.0
    private val pageHeight = 842.0
    private val margin = 35.0

    actual fun renderQuote(content: QuotePdfContent): ByteArray {
        val lines = buildList {
            add(Line(CompanyInfo.NAME, 18.0, bold = true, color = UIColor.systemBlueColor))
            add(Line("${CompanyInfo.TAX_ID}  ·  ${CompanyInfo.ADDRESS}", 9.0))
            add(Line("${CompanyInfo.PHONE} | ${CompanyInfo.EMAIL}", 9.0))
            add(Line(content.docTitle, 12.0, bold = true, color = UIColor.systemBlueColor))
            add(Line(""))
            add(Line("CLIENTE / FACTURAR A: ${content.clientName}", 11.0, bold = true))
            content.clientIdShort?.let { add(Line("ID: $it", 9.0)) }
            content.clientEmail?.let { add(Line("Email: $it", 9.0)) }
            content.clientPhone?.let { add(Line("Tel: $it", 9.0)) }
            add(Line("Documento: ${content.docNumber}   Fecha: ${content.issueDate}", 10.0, bold = true))
            content.validUntil?.let { add(Line("Válido hasta: $it", 9.0, color = UIColor.systemRedColor)) }
            add(Line(""))
            add(Line("Descripción                          Cant.   Unitario   Subtotal", 9.0, bold = true))
            content.rows.forEach {
                add(Line("${it.description}   ${it.quantity}   ${it.unitPrice}   ${it.subtotal}", 9.0))
            }
            add(Line(""))
            add(Line("Subtotal: ${content.subtotal}", 9.0))
            add(Line("${content.taxLabel}: ${content.taxAmount}", 9.0))
            add(Line("TOTAL: ${content.total}", 13.0, bold = true, color = UIColor.systemBlueColor))
            content.notes?.let { add(Line("")); add(Line("NOTAS Y CONDICIONES", 9.0, bold = true)); add(Line(it, 9.0)) }
        }
        return renderLines(lines)
    }

    actual fun renderOrder(content: OrderPdfContent): ByteArray {
        val lines = buildList {
            add(Line(CompanyInfo.NAME, 18.0, bold = true, color = UIColor.systemBlueColor))
            add(Line("${CompanyInfo.TAX_ID}  ·  ${CompanyInfo.ADDRESS}", 9.0))
            add(Line(content.docTitle, 12.0, bold = true, color = UIColor.systemBlueColor))
            add(Line(""))
            add(Line("CLIENTE / FACTURAR A: ${content.clientName}", 11.0, bold = true))
            content.clientEmail?.let { add(Line("Email: $it", 9.0)) }
            add(Line("Orden: ${content.docNumber}   Fecha: ${content.issueDate}", 10.0, bold = true))
            add(Line(""))
            add(Line("RESUMEN DEL SERVICIO", 9.0, bold = true))
            add(Line("Tipo de servicio: ${content.serviceType}", 9.0))
            add(Line("Estado: ${content.statusLabel}", 9.0))
            add(Line("Programado: ${content.scheduledDate ?: "-"}", 9.0))
            add(Line("Dirección: ${content.address}", 9.0))
            content.technicianNotes?.let { add(Line("")); add(Line("OBSERVACIONES DEL TÉCNICO", 9.0, bold = true)); add(Line(it, 9.0)) }
        }
        return renderLines(lines)
    }

    actual fun previewOrShare(bytes: ByteArray, suggestedFileName: String) {
        // La UI de compartir (UIActivityViewController) necesita un
        // UIViewController presentador; se dispara desde iosApp (SwiftUI),
        // igual que el patrón de ImagePickerService. Este método deja el
        // archivo listo en el directorio temporal para que iosApp lo tome.
        val nsData = bytes.toNSData()
        val tmpPath = platform.Foundation.NSTemporaryDirectory() + suggestedFileName
        nsData.writeToFile(tmpPath, atomically = true)
    }

    private data class Line(val text: String, val size: Double = 9.0, val bold: Boolean = false, val color: UIColor = UIColor.blackColor)

    private fun renderLines(lines: List<Line>): ByteArray {
        val format = UIGraphicsPDFRendererFormat()
        val renderer = UIGraphicsPDFRenderer(bounds = CGRectMake(0.0, 0.0, pageWidth, pageHeight), format = format)

        val data = renderer.PDFDataWithActions { context ->
            context.beginPage()
            var y = margin
            for (line in lines) {
                if (line.text.isEmpty()) {
                    y += line.size
                    continue
                }
                val font = if (line.bold) UIFont.boldSystemFontOfSize(line.size) else UIFont.systemFontOfSize(line.size)
                val attrs = mapOf<Any?, Any?>(
                    NSFontAttributeName to font,
                    NSForegroundColorAttributeName to line.color,
                )
                val attrString = NSAttributedString(string = line.text, attributes = attrs)
                attrString.drawAtPoint(platform.CoreGraphics.CGPointMake(margin, y))
                y += line.size + 6.0
                if (y > pageHeight - margin) {
                    context.beginPage()
                    y = margin
                }
            }
        }
        return data.toByteArray()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned {
    NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}
