package ly.jawdat.alhaya.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import ly.jawdat.alhaya.data.ProductSize
import kotlin.math.PI
import kotlin.math.sin

// The three bottles as they look in the shop photos: clear plastic with a wide white ribbed cap,
// golden vinegar, and the cream label with palm leaves and dates.
//  - 100 ml: short and stout, a wide cap, grip rings above a label that wraps the lower body.
//  - 330 ml: a domed shoulder, two grip rings, the label in the middle and plain body below.
//  - 1 L: tall, with a long cone of twisted ridges, the label, then rings down to the base.

/** The colour of date vinegar in the drawn bottles: golden amber, as in the photos. */
val Vinegar = Color(0xFFE39A2E)
private val VinegarLight = Color(0xFFF7C860)
private val CapWhite = Color(0xFFF8F8F4)
private val LabelCream = Color(0xFFF7EFD3)
private val PalmGreen = Color(0xFF5E9E3A)
private val DateDark = Color(0xFF7A4A24)
private val LogoTeal = Color(0xFF3F6F7C)

/** Width ÷ height of each bottle, so callers can size the box to the real shape. */
fun bottleAspect(size: String): Float = when (size) {
    ProductSize.TINY -> 0.56f
    ProductSize.LARGE -> 0.30f
    else -> 0.36f
}

/** Height of each bottle relative to the 1 L one, for showing sizes side by side. */
fun bottleScale(size: String): Float = when (size) {
    ProductSize.TINY -> 0.42f
    ProductSize.LARGE -> 1f
    else -> 0.66f
}

/** A bottle of [size] standing [height] tall, its width following the real shape. */
@Composable
fun BottleOf(size: String, height: Dp, fill: Float, modifier: Modifier = Modifier, wave: Boolean = true) {
    Bottle(modifier.width(height * bottleAspect(size)).height(height), fill, size, wave)
}

/** Proportions of one bottle shape: y values are fractions of the height, half-widths of the width. */
private class Shape(
    val capBottom: Float, val capHalf: Float,
    val ringBottom: Float, val ringHalf: Float,
    val neckBottom: Float, val neckHalf: Float,
    val bodyTop: Float,
    /** Shoulder curve controls: a rounded dome (high values) or a long cone. */
    val c1: Float, val c2: Float,
    val labelTop: Float, val labelBottom: Float,
    val rings: List<Float>,
    val swirl: Boolean,
)

private val tinyShape = Shape(
    capBottom = 0.17f, capHalf = 0.35f, ringBottom = 0.20f, ringHalf = 0.37f, neckBottom = 0.235f, neckHalf = 0.31f,
    bodyTop = 0.36f, c1 = 0.6f, c2 = 0.25f, labelTop = 0.58f, labelBottom = 0.95f,
    rings = listOf(0.41f, 0.46f, 0.51f), swirl = false,
)

private val smallShape = Shape(
    capBottom = 0.085f, capHalf = 0.30f, ringBottom = 0.105f, ringHalf = 0.32f, neckBottom = 0.13f, neckHalf = 0.25f,
    bodyTop = 0.30f, c1 = 0.55f, c2 = 0.3f, labelTop = 0.43f, labelBottom = 0.79f,
    rings = listOf(0.335f, 0.375f, 0.92f), swirl = false,
)

private val largeShape = Shape(
    capBottom = 0.062f, capHalf = 0.21f, ringBottom = 0.078f, ringHalf = 0.23f, neckBottom = 0.10f, neckHalf = 0.18f,
    bodyTop = 0.41f, c1 = 0.3f, c2 = 0.45f, labelTop = 0.47f, labelBottom = 0.68f,
    rings = listOf(0.73f, 0.775f, 0.82f, 0.865f, 0.91f), swirl = true,
)

/**
 * A bottle drawn to fill its box, with vinegar up to [fill] (0–1). [size] is one of [ProductSize];
 * give the box the matching [bottleAspect] (or use [BottleOf]). [wave] lets the surface ripple.
 */
@Composable
fun Bottle(
    modifier: Modifier,
    fill: Float,
    size: String = ProductSize.SMALL,
    wave: Boolean = true,
    liquid: Color = Vinegar,
) {
    val edge = MaterialTheme.colorScheme.outline
    val phase = if (wave) {
        val tr = rememberInfiniteTransition(label = "wave")
        tr.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase").value
    } else 0f
    val shape = when (size) {
        ProductSize.TINY -> tinyShape
        ProductSize.LARGE -> largeShape
        else -> smallShape
    }
    Canvas(modifier) { drawBottle(shape, fill.coerceIn(0f, 1f), phase, wave, liquid, edge) }
}

private fun DrawScope.drawBottle(s: Shape, fill: Float, phase: Float, wave: Boolean, liquid: Color, edge: Color) {
    val w = size.width
    val h = size.height
    val stroke = (w * 0.035f).coerceIn(0.8f, 4f)
    val cx = w / 2
    val left = stroke / 2
    val right = w - stroke / 2
    val bottom = h - stroke / 2
    val r = w * 0.14f
    fun x(half: Float, side: Int) = cx + side * half * (w - stroke)
    val neckTop = s.ringBottom * h
    val neckBottom = s.neckBottom * h
    val bodyTop = s.bodyTop * h
    val span = bodyTop - neckBottom

    // The plastic body, from the neck down, traced left side then right side.
    val body = Path().apply {
        moveTo(x(s.neckHalf, -1), neckTop)
        lineTo(x(s.neckHalf, -1), neckBottom)
        cubicTo(
            x(s.neckHalf + (0.5f - s.neckHalf) * 0.15f, -1), neckBottom + span * s.c1,
            left, bodyTop - span * s.c2,
            left, bodyTop,
        )
        lineTo(left, bottom - r)
        quadraticTo(left, bottom, left + r, bottom)
        lineTo(right - r, bottom)
        quadraticTo(right, bottom, right, bottom - r)
        lineTo(right, bodyTop)
        cubicTo(
            right, bodyTop - span * s.c2,
            x(s.neckHalf + (0.5f - s.neckHalf) * 0.15f, 1), neckBottom + span * s.c1,
            x(s.neckHalf, 1), neckBottom,
        )
        lineTo(x(s.neckHalf, 1), neckTop)
        close()
    }

    // Clear plastic, then the vinegar with a gentle wave on top.
    drawPath(body, Color.White.copy(alpha = 0.45f))
    if (fill > 0f) clipPath(body) {
        val top = neckBottom + h * 0.01f
        val level = bottom - fill * (bottom - top)
        val amp = if (wave) h * 0.012f else 0f
        val surface = Path().apply {
            moveTo(0f, level)
            var px = 0f
            while (px <= w) {
                lineTo(px, level + amp * sin(phase + px / w * 2f * PI.toFloat()))
                px += w / 12f
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(surface, Brush.verticalGradient(listOf(VinegarLight, liquid), startY = level, endY = h))
    }

    // Moulded ridges: twisted ones on the 1 L cone, straight grip rings elsewhere.
    clipPath(body) {
        val light = Color.White.copy(alpha = 0.45f)
        val dark = Color.Black.copy(alpha = 0.10f)
        val line = (w * 0.025f).coerceIn(0.7f, 3f)
        if (s.swirl) {
            for (i in -3..6) {
                val x0 = w * (i * 0.2f)
                val p = Path().apply {
                    moveTo(x0, neckBottom + span * 0.15f)
                    quadraticTo(x0 + w * 0.35f, neckBottom + span * 0.55f, x0 + w * 0.45f, bodyTop + h * 0.01f)
                }
                drawPath(p, dark, style = Stroke(line * 1.4f))
                drawPath(p, light, style = Stroke(line))
            }
        }
        s.rings.forEach { y ->
            val yy = y * h
            drawLine(dark, Offset(0f, yy + line), Offset(w, yy + line), line * 1.3f)
            drawLine(light, Offset(0f, yy), Offset(w, yy), line)
        }
    }

    drawLabel(left + stroke * 0.6f, right - stroke * 0.6f, s.labelTop * h, s.labelBottom * h)

    // Outline and a shine down the left side.
    drawPath(body, edge.copy(alpha = 0.55f), style = Stroke(stroke))
    drawLine(
        Color.White.copy(alpha = 0.7f),
        Offset(left + w * 0.12f, bodyTop + h * 0.02f), Offset(left + w * 0.12f, s.labelTop * h - h * 0.02f),
        strokeWidth = (w * 0.05f).coerceAtLeast(1f), cap = StrokeCap.Round,
    )

    // Neck ring and the white ribbed cap.
    val ringTop = s.capBottom * h
    drawRoundRect(
        CapWhite, Offset(x(s.ringHalf, -1), ringTop), Size(s.ringHalf * 2 * (w - stroke), neckTop - ringTop + stroke * 0.5f),
        CornerRadius(stroke),
    )
    drawRoundRect(
        edge.copy(alpha = 0.45f), Offset(x(s.ringHalf, -1), ringTop), Size(s.ringHalf * 2 * (w - stroke), neckTop - ringTop + stroke * 0.5f),
        CornerRadius(stroke), style = Stroke(stroke * 0.7f),
    )
    val capLeft = x(s.capHalf, -1)
    val capW = s.capHalf * 2 * (w - stroke)
    val capH = s.capBottom * h
    drawRoundRect(CapWhite, Offset(capLeft, stroke / 2), Size(capW, capH), CornerRadius(capW * 0.12f))
    val ribs = (capW / (w * 0.06f)).toInt().coerceIn(4, 16)
    for (i in 1 until ribs) {
        val rx = capLeft + capW * i / ribs
        drawLine(Color.Black.copy(alpha = 0.07f), Offset(rx, stroke + capH * 0.12f), Offset(rx, capH - capH * 0.05f), (w * 0.012f).coerceAtLeast(0.6f))
    }
    drawRoundRect(edge.copy(alpha = 0.5f), Offset(capLeft, stroke / 2), Size(capW, capH), CornerRadius(capW * 0.12f), style = Stroke(stroke * 0.8f))
}

/**
 * The cream label: palm leaves along the top and bottom, the round logo, a title line and two dates.
 * On tiny bottles only the band and a green stripe show.
 */
private fun DrawScope.drawLabel(l: Float, r: Float, top: Float, bottom: Float) {
    val lw = r - l
    val lh = bottom - top
    drawRoundRect(LabelCream, Offset(l, top), Size(lw, lh), CornerRadius(lh * 0.06f))
    if (lh < 14f || lw < 14f) {
        drawRect(PalmGreen, Offset(l, top + lh * 0.4f), Size(lw, lh * 0.2f))
        return
    }
    // Palm leaves: slanted leaflets along the top and bottom edges, kept inside the label.
    val leaf = lh * 0.12f
    val count = (lw / (leaf * 0.9f)).toInt().coerceIn(4, 14)
    clipRect(l, top, r, bottom) {
        for (i in 0..count) {
            val px = l + lw * i / count
            listOf(top + leaf * 0.5f to -35f, bottom - leaf * 0.5f to 35f).forEach { (py, angle) ->
                rotate(angle, Offset(px, py)) {
                    drawOval(PalmGreen.copy(alpha = 0.85f), Offset(px - leaf * 0.9f, py - leaf * 0.22f), Size(leaf * 1.8f, leaf * 0.44f))
                }
            }
        }
    }
    // Round logo, title line and two dates.
    val cx = l + lw / 2
    val logoR = (lh * 0.11f).coerceAtMost(lw * 0.12f)
    val logoY = top + lh * 0.30f
    drawCircle(Color.White, logoR, Offset(cx, logoY))
    drawCircle(LogoTeal, logoR, Offset(cx, logoY), style = Stroke(logoR * 0.25f))
    drawLine(DateDark, Offset(cx - lw * 0.28f, top + lh * 0.50f), Offset(cx + lw * 0.28f, top + lh * 0.50f), lh * 0.06f, StrokeCap.Round)
    drawLine(DateDark.copy(alpha = 0.5f), Offset(cx - lw * 0.18f, top + lh * 0.58f), Offset(cx + lw * 0.18f, top + lh * 0.58f), lh * 0.03f, StrokeCap.Round)
    val dw = lw * 0.2f
    val dh = lh * 0.09f
    rotate(-12f, Offset(cx - dw * 0.45f, top + lh * 0.71f)) {
        drawOval(DateDark, Offset(cx - dw * 0.95f, top + lh * 0.71f - dh / 2), Size(dw, dh))
    }
    rotate(10f, Offset(cx + dw * 0.45f, top + lh * 0.73f)) {
        drawOval(DateDark.copy(alpha = 0.85f), Offset(cx - dw * 0.05f, top + lh * 0.73f - dh / 2), Size(dw, dh))
    }
}
