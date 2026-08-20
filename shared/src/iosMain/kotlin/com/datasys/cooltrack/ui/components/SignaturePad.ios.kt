package com.datasys.cooltrack.ui.components

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImagePNGRepresentation

/**
 * Implementación iOS de SignatureEncoder usando `UIGraphicsImageRenderer` +
 * `UIBezierPath` (Core Graphics), el mismo motor que `PdfRenderer.ios.kt`
 * usa para PDF. Igual que se documentó ahí, esta es la versión "funcional"
 * (trazo simple, sin antialiasing extra) — se puede refinar sin tocar el
 * resto de la capa compartida.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SignatureEncoder actual constructor() {
    actual fun toPng(strokes: List<List<Offset>>, width: Int, height: Int, strokeWidthPx: Float): ByteArray {
        val safeWidth = width.coerceAtLeast(1).toDouble()
        val safeHeight = height.coerceAtLeast(1).toDouble()

        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(safeWidth, safeHeight))
        val image = renderer.imageWithActions { _ ->
            UIColor.whiteColor.setFill()
            UIBezierPath.bezierPathWithRect(
                platform.CoreGraphics.CGRectMake(0.0, 0.0, safeWidth, safeHeight),
            ).fill()

            UIColor.blackColor.setStroke()
            for (stroke in strokes) {
                if (stroke.size < 2) continue
                val path = UIBezierPath()
                path.lineWidth = strokeWidthPx.toDouble()
                path.lineCapStyle = platform.CoreGraphics.kCGLineCapRound
                path.lineJoinStyle = platform.CoreGraphics.kCGLineJoinRound
                path.moveToPoint(CGPointMake(stroke.first().x.toDouble(), stroke.first().y.toDouble()))
                for (point in stroke.drop(1)) {
                    path.addLineToPoint(CGPointMake(point.x.toDouble(), point.y.toDouble()))
                }
                path.stroke()
            }
        }

        val data: NSData = UIImagePNGRepresentation(image) ?: NSData()
        return data.toByteArray()
    }
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
