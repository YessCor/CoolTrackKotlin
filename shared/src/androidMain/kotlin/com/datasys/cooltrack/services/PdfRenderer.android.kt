package com.datasys.cooltrack.services

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.datasys.cooltrack.core.SecureStorageInitializer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Implementación Android de PdfRenderer usando `android.graphics.pdf.PdfDocument`
 * (Canvas de bajo nivel — el equivalente nativo más cercano al árbol de
 * widgets `pw.*` que usaba la librería `pdf` de Dart). Reproduce el mismo
 * contenido: encabezado con marca, datos de cliente/documento, tabla de
 * ítems, totales y notas.
 */
actual class PdfRenderer actual constructor() {

    private companion object {
        const val PAGE_WIDTH = 595 // A4 a 72dpi
        const val PAGE_HEIGHT = 842
        const val MARGIN = 35f
        val BLUE_900 = Color.rgb(13, 71, 161)
        val GREY_700 = Color.rgb(97, 97, 97)
        val GREY_200 = Color.rgb(238, 238, 238)
    }

    actual fun renderQuote(content: QuotePdfContent): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        var y = drawHeader(canvas, content.docTitle)
        y = drawClientAndDocInfo(
            canvas, y,
            title = "Cotización No:", docNumber = content.docNumber,
            issueDate = content.issueDate, validUntil = content.validUntil,
            clientName = content.clientName, clientIdShort = content.clientIdShort,
            clientEmail = content.clientEmail, clientPhone = content.clientPhone,
            clientAddress = content.clientAddress,
        )
        y += 15
        y = drawItemsTable(canvas, y, content.rows)
        y += 15
        y = drawTotals(canvas, y, content.subtotal, content.taxLabel, content.taxAmount, content.total)
        content.notes?.let { y = drawSectionCard(canvas, y, "NOTAS Y CONDICIONES", it) }
        drawFooter(canvas)

        doc.finishPage(page)
        return doc.toByteArrayAndClose()
    }

    actual fun renderOrder(content: OrderPdfContent): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        var y = drawHeader(canvas, content.docTitle)
        y = drawClientAndDocInfo(
            canvas, y,
            title = "Orden de Trabajo:", docNumber = content.docNumber,
            issueDate = content.issueDate, validUntil = null,
            clientName = content.clientName, clientIdShort = content.clientIdShort,
            clientEmail = content.clientEmail, clientPhone = content.clientPhone,
            clientAddress = content.clientAddress,
        )
        y += 15
        y = drawKeyValueBlock(
            canvas, y, "RESUMEN DEL SERVICIO",
            listOf(
                "Tipo de servicio" to content.serviceType,
                "Estado" to content.statusLabel,
                "Programado" to (content.scheduledDate ?: "-"),
                "Dirección" to content.address,
            ),
        )
        content.technicianNotes?.let { y = drawSectionCard(canvas, y, "OBSERVACIONES DEL TÉCNICO", it) }
        drawFooter(canvas)

        doc.finishPage(page)
        return doc.toByteArrayAndClose()
    }

    actual fun previewOrShare(bytes: ByteArray, suggestedFileName: String) {
        val context = SecureStorageInitializer.appContext
        val file = File(context.cacheDir, suggestedFileName)
        FileOutputStream(file).use { it.write(bytes) }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // --- Dibujo de bajo nivel (equivalente a _buildBusinessHeader, _buildItemsTable, etc.) ---

    private fun drawHeader(canvas: Canvas, title: String): Float {
        val paintTitle = Paint().apply { color = BLUE_900; textSize = 18f; isFakeBoldText = true }
        val paintSub = Paint().apply { color = GREY_700; textSize = 9f }
        canvas.drawText(CompanyInfo.NAME, MARGIN, 50f, paintTitle)
        canvas.drawText(CompanyInfo.TAX_ID, MARGIN, 65f, paintSub)
        canvas.drawText(CompanyInfo.ADDRESS, MARGIN, 78f, paintSub)
        canvas.drawText("${CompanyInfo.PHONE} | ${CompanyInfo.EMAIL}", MARGIN, 90f, paintSub)

        val badgePaint = Paint().apply { color = BLUE_900; textSize = 12f; isFakeBoldText = true }
        canvas.drawText(title, PAGE_WIDTH - MARGIN - badgePaint.measureText(title), 50f, badgePaint)

        val linePaint = Paint().apply { color = BLUE_900; strokeWidth = 2f }
        canvas.drawLine(MARGIN, 100f, PAGE_WIDTH - MARGIN, 100f, linePaint)
        return 120f
    }

    private fun drawClientAndDocInfo(
        canvas: Canvas, startY: Float,
        title: String, docNumber: String, issueDate: String, validUntil: String?,
        clientName: String, clientIdShort: String?, clientEmail: String?, clientPhone: String?, clientAddress: String?,
    ): Float {
        var y = startY
        val label = Paint().apply { color = BLUE_900; textSize = 8f; isFakeBoldText = true }
        val bold = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val normal = Paint().apply { textSize = 9f }

        canvas.drawText("CLIENTE / FACTURAR A:", MARGIN, y, label)
        y += 16
        canvas.drawText(clientName, MARGIN, y, bold)
        y += 14
        clientIdShort?.let { canvas.drawText("ID: $it", MARGIN, y, normal); y += 12 }
        clientEmail?.let { canvas.drawText("Email: $it", MARGIN, y, normal); y += 12 }
        clientPhone?.let { canvas.drawText("Tel: $it", MARGIN, y, normal); y += 12 }
        clientAddress?.let { canvas.drawText("Dir: $it", MARGIN, y, normal); y += 12 }

        val rightX = PAGE_WIDTH - MARGIN
        val docLabel = Paint().apply { color = GREY_700; textSize = 9f }
        val docNum = Paint().apply { color = BLUE_900; textSize = 16f; isFakeBoldText = true }
        canvas.drawText(title, rightX - docLabel.measureText(title), startY, docLabel)
        canvas.drawText(docNumber, rightX - docNum.measureText(docNumber), startY + 20, docNum)

        var ry = startY + 40
        canvas.drawText("Fecha Emisión: $issueDate", rightX - normal.measureText("Fecha Emisión: $issueDate"), ry, normal)
        ry += 12
        validUntil?.let {
            val red = Paint().apply { color = Color.rgb(198, 40, 40); textSize = 8f; isFakeBoldText = true }
            val text = "Válido Hasta: $it"
            canvas.drawText(text, rightX - red.measureText(text), ry, red)
        }

        return maxOf(y, ry) + 10
    }

    private fun drawItemsTable(canvas: Canvas, startY: Float, rows: List<PdfTableRow>): Float {
        var y = startY
        val headerBg = Paint().apply { color = BLUE_900 }
        val headerText = Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
        val cellText = Paint().apply { color = Color.BLACK; textSize = 9f }
        val border = Paint().apply { color = GREY_200; style = Paint.Style.STROKE; strokeWidth = 0.5f }

        val colX = floatArrayOf(MARGIN, MARGIN + 260f, MARGIN + 340f, MARGIN + 440f)
        val tableWidth = PAGE_WIDTH - 2 * MARGIN

        canvas.drawRect(MARGIN, y, MARGIN + tableWidth, y + 22, headerBg)
        canvas.drawText("Descripción de Servicio / Producto", colX[0] + 6, y + 15, headerText)
        canvas.drawText("Cant.", colX[1] + 6, y + 15, headerText)
        canvas.drawText("Unitario", colX[2] + 6, y + 15, headerText)
        canvas.drawText("Subtotal", colX[3] + 6, y + 15, headerText)
        y += 22

        for (row in rows) {
            canvas.drawRect(MARGIN, y, MARGIN + tableWidth, y + 20, border)
            canvas.drawText(row.description, colX[0] + 6, y + 14, cellText)
            canvas.drawText(row.quantity, colX[1] + 6, y + 14, cellText)
            canvas.drawText(row.unitPrice, colX[2] + 6, y + 14, cellText)
            canvas.drawText(row.subtotal, colX[3] + 6, y + 14, cellText)
            y += 20
        }
        return y
    }

    private fun drawTotals(canvas: Canvas, startY: Float, subtotal: String, taxLabel: String, taxAmount: String, total: String): Float {
        var y = startY
        val label = Paint().apply { color = GREY_700; textSize = 9f }
        val bold = Paint().apply { color = BLUE_900; textSize = 12f; isFakeBoldText = true }
        val rightX = PAGE_WIDTH - MARGIN

        fun row(text: String, value: String, paint: Paint) {
            canvas.drawText(text, rightX - 150f, y, label)
            canvas.drawText(value, rightX - paint.measureText(value), y, paint)
            y += 16
        }
        row("Subtotal:", subtotal, label)
        row(taxLabel + ":", taxAmount, label)
        row("TOTAL:", total, bold)
        return y + 10
    }

    private fun drawSectionCard(canvas: Canvas, startY: Float, title: String, body: String): Float {
        var y = startY
        val titlePaint = Paint().apply { color = BLUE_900; textSize = 9f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { color = Color.DKGRAY; textSize = 9f }
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 14
        canvas.drawText(body, MARGIN, y, bodyPaint)
        return y + 20
    }

    private fun drawKeyValueBlock(canvas: Canvas, startY: Float, title: String, pairs: List<Pair<String, String>>): Float {
        var y = startY
        val titlePaint = Paint().apply { color = BLUE_900; textSize = 9f; isFakeBoldText = true }
        val keyPaint = Paint().apply { color = GREY_700; textSize = 9f }
        val valPaint = Paint().apply { textSize = 9f; isFakeBoldText = true }

        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 16
        for ((k, v) in pairs) {
            canvas.drawText(k, MARGIN, y, keyPaint)
            canvas.drawText(v, MARGIN + 150f, y, valPaint)
            y += 14
        }
        return y + 10
    }

    private fun drawFooter(canvas: Canvas) {
        val paint = Paint().apply { color = GREY_700; textSize = 7f }
        val text = "${CompanyInfo.WEB} · Documento generado automáticamente por Cooltrack"
        canvas.drawText(text, MARGIN, PAGE_HEIGHT - 20f, paint)
    }

    private fun PdfDocument.toByteArrayAndClose(): ByteArray {
        val output = ByteArrayOutputStream()
        writeTo(output)
        close()
        return output.toByteArray()
    }
}
