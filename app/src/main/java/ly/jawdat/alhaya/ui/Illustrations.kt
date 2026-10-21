package ly.jawdat.alhaya.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.ProductSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Moving pictures for the welcome tour, drawn in code so they stay sharp and cost no image files.

enum class TourArt { LOGO, PRODUCTION, SALES, GIFTS, SHARE, REPORTS, BACKUP, GUIDE }

@Composable
fun TourIllustration(art: TourArt, modifier: Modifier = Modifier) {
    Box(modifier.size(240.dp), contentAlignment = Alignment.Center) {
        when (art) {
            TourArt.LOGO -> LogoArt()
            TourArt.PRODUCTION -> ProductionArt()
            TourArt.SALES -> SalesArt()
            TourArt.GIFTS -> GiftsArt()
            TourArt.REPORTS -> ReportsArt()
            TourArt.BACKUP -> BackupArt()
            TourArt.GUIDE -> GuideArt()
            TourArt.SHARE -> ShareArt()
        }
    }
}

/** A soft round backdrop behind each picture. */
@Composable
private fun Halo(color: Color) {
    val tr = rememberInfiniteTransition(label = "halo")
    val s by tr.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = s; scaleY = s }) {
        drawCircle(color.copy(alpha = 0.18f), size.minDimension * 0.48f)
        drawCircle(color.copy(alpha = 0.22f), size.minDimension * 0.38f)
    }
}

@Composable
private fun LogoArt() {
    val tr = rememberInfiniteTransition(label = "logo")
    val turn by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "turn")
    val breathe by tr.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val leaf = Leaf
    val date = DateBrown
    Halo(MaterialTheme.colorScheme.primary)
    // A ring of leaves and dates turning slowly around the logo.
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val r = size.minDimension * 0.44f
        rotate(turn, c) {
            for (i in 0 until 14) {
                val a = (i / 14f) * 2 * PI.toFloat()
                val p = Offset(c.x + r * cos(a), c.y + r * sin(a))
                rotate(i * 360f / 14f + 90f, p) {
                    if (i % 2 == 0) drawOval(leaf, Offset(p.x - 5.dp.toPx(), p.y - 10.dp.toPx()), Size(10.dp.toPx(), 20.dp.toPx()))
                    else drawOval(date, Offset(p.x - 5.dp.toPx(), p.y - 7.dp.toPx()), Size(10.dp.toPx(), 14.dp.toPx()))
                }
            }
        }
    }
    Image(painterResource(R.drawable.logo), null, Modifier.height(150.dp).graphicsLayer { scaleX = breathe; scaleY = breathe })
}

/** A jar of soaking dates with bubbles rising. */
@Composable
private fun ProductionArt() {
    val tr = rememberInfiniteTransition(label = "prod")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "t")
    val glass = MaterialTheme.colorScheme.outline
    val lid = MaterialTheme.colorScheme.primary
    val date = DateBrown
    Halo(Leaf)
    Canvas(Modifier.size(150.dp, 170.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 4.dp.toPx()
        val jarTop = h * 0.16f
        // Liquid
        val level = jarTop + h * 0.14f
        drawRoundRect(Vinegar.copy(alpha = 0.85f), Offset(stroke, level), Size(w - stroke * 2, h - level - stroke), CornerRadius(18.dp.toPx()))
        // Dates bobbing at different speeds.
        for (i in 0 until 6) {
            val x = w * (0.2f + (i % 3) * 0.3f)
            val baseY = h * (0.58f + (i / 3) * 0.2f)
            val y = baseY + sin((t * 2 * PI + i * 1.3).toFloat()) * 5.dp.toPx()
            rotate((i * 37f) + t * 40f, Offset(x, y)) {
                drawOval(date, Offset(x - 13.dp.toPx(), y - 8.dp.toPx()), Size(26.dp.toPx(), 16.dp.toPx()))
                drawOval(Color.White.copy(alpha = 0.25f), Offset(x - 8.dp.toPx(), y - 6.dp.toPx()), Size(9.dp.toPx(), 4.dp.toPx()))
            }
        }
        // Bubbles rise from the bottom and fade near the surface.
        for (i in 0 until 7) {
            val p = (t + i / 7f) % 1f
            val x = w * (0.15f + ((i * 0.37f) % 0.7f)) + sin(p * 6f + i) * 4.dp.toPx()
            val y = h - stroke - p * (h - stroke - level)
            drawCircle(Color.White.copy(alpha = 0.6f * (1f - p)), (2.5f + i % 3).dp.toPx(), Offset(x, y), style = Stroke(1.5.dp.toPx()))
        }
        // Jar and cloth lid.
        drawRoundRect(glass, Offset(stroke / 2, jarTop), Size(w - stroke, h - jarTop - stroke / 2), CornerRadius(20.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(lid, Offset(w * 0.06f, jarTop - h * 0.1f), Size(w * 0.88f, h * 0.12f), CornerRadius(8.dp.toPx()))
        drawLine(Cream, Offset(w * 0.12f, jarTop - h * 0.04f), Offset(w * 0.88f, jarTop - h * 0.04f), 2.dp.toPx())
    }
}

/** The two bottle sizes hopping in turn, with a swinging price tag. */
@Composable
private fun SalesArt() {
    val tr = rememberInfiniteTransition(label = "sales")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t")
    val swing by tr.animateFloat(-12f, 12f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swing")
    Halo(MaterialTheme.colorScheme.primary)
    val hop1 = (-sin(t * 2 * PI).coerceAtLeast(0.0) * 14).toFloat()
    val hop2 = (-sin(t * 2 * PI + PI).coerceAtLeast(0.0) * 14).toFloat()
    Box(Modifier.size(180.dp, 150.dp)) {
        Bottle(Modifier.align(Alignment.BottomStart).offset(x = 14.dp, y = hop1.dp).width(30.dp).height(54.dp), 0.8f, ProductSize.TINY)
        Bottle(Modifier.align(Alignment.BottomCenter).offset(y = hop1.dp).width(30.dp).height(84.dp), 0.85f, ProductSize.SMALL)
        Bottle(Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = hop2.dp).width(38.dp).height(128.dp), 0.85f, ProductSize.LARGE)
        // A price tag on a string, swinging.
        Box(
            Modifier.align(Alignment.TopCenter).graphicsLayer {
                rotationZ = swing
                transformOrigin = TransformOrigin(0.5f, 0f)
            },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.width(2.dp).height(18.dp).background(MaterialTheme.colorScheme.outline))
            Text(
                currency,
                Modifier.padding(top = 16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

/** A gift box whose lid hops up to show a bottle, with sparkles. */
@Composable
private fun GiftsArt() {
    val tr = rememberInfiniteTransition(label = "gift")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    // Lid opens during the middle of each cycle.
    val open = when {
        t < 0.25f -> 0f
        t < 0.4f -> (t - 0.25f) / 0.15f
        t < 0.75f -> 1f
        t < 0.9f -> 1f - (t - 0.75f) / 0.15f
        else -> 0f
    }
    val ease = FastOutSlowInEasing.transform(open)
    val box = Leaf
    val ribbon = DateBrown
    val sparkle = MaterialTheme.colorScheme.secondary
    Halo(Leaf)
    Box(Modifier.size(170.dp), contentAlignment = Alignment.BottomCenter) {
        // The bottle sits inside and rises as the lid lifts.
        Bottle(Modifier.offset(y = (-40 - 48 * ease).dp).width(32.dp).height(88.dp), 0.85f, ProductSize.SMALL)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bodyTop = h * 0.55f
            drawRoundRect(box, Offset(w * 0.15f, bodyTop), Size(w * 0.7f, h - bodyTop - 6.dp.toPx()), CornerRadius(8.dp.toPx()))
            drawRect(ribbon, Offset(w * 0.45f, bodyTop), Size(w * 0.1f, h - bodyTop - 6.dp.toPx()))
            // Lid with bow, lifted and tilted.
            val lift = ease * h * 0.32f
            rotate(-14f * ease, Offset(w * 0.15f, bodyTop - lift)) {
                drawRoundRect(box, Offset(w * 0.1f, bodyTop - h * 0.12f - lift), Size(w * 0.8f, h * 0.13f), CornerRadius(8.dp.toPx()))
                drawRect(ribbon, Offset(w * 0.45f, bodyTop - h * 0.12f - lift), Size(w * 0.1f, h * 0.13f))
                bow(Offset(w * 0.5f, bodyTop - h * 0.12f - lift), ribbon)
            }
            // Sparkles twinkle while it is open.
            if (ease > 0.05f) for (i in 0 until 5) {
                val a = i * 72f * PI.toFloat() / 180f + t * 3f
                val r = w * (0.36f + 0.06f * sin(t * 12f + i))
                val p = Offset(w / 2 + r * cos(a), h * 0.35f + r * 0.6f * sin(a))
                star(p, (5 + i % 3).dp.toPx() * ease, sparkle.copy(alpha = ease))
            }
        }
    }
}

private fun DrawScope.bow(at: Offset, color: Color) {
    val s = 14.dp.toPx()
    drawOval(color, Offset(at.x - s * 1.6f, at.y - s * 1.1f), Size(s * 1.5f, s), style = Stroke(4.dp.toPx()))
    drawOval(color, Offset(at.x + s * 0.1f, at.y - s * 1.1f), Size(s * 1.5f, s), style = Stroke(4.dp.toPx()))
    drawCircle(color, s * 0.35f, Offset(at.x, at.y - s * 0.5f))
}

private fun DrawScope.star(at: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(at.x, at.y - r)
        quadraticTo(at.x, at.y, at.x + r, at.y)
        quadraticTo(at.x, at.y, at.x, at.y + r)
        quadraticTo(at.x, at.y, at.x - r, at.y)
        quadraticTo(at.x, at.y, at.x, at.y - r)
        close()
    }
    drawPath(p, color)
}

/** Columns growing one after another, again and again. */
@Composable
private fun ReportsArt() {
    val tr = rememberInfiniteTransition(label = "rep")
    val p by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val colors = listOf(MaterialTheme.colorScheme.primary, Leaf, DateBrown, MaterialTheme.colorScheme.primary, Leaf)
    val axis = MaterialTheme.colorScheme.outline
    val heights = remember { listOf(0.45f, 0.7f, 0.55f, 0.85f, 1f) }
    Halo(MaterialTheme.colorScheme.primary)
    Canvas(Modifier.size(170.dp, 140.dp)) {
        val w = size.width
        val h = size.height
        val bw = w / 8f
        heights.forEachIndexed { i, target ->
            val local = ((p * 1.6f) - i * 0.12f).coerceIn(0f, 1f)
            val bh = (h - 8.dp.toPx()) * target * FastOutSlowInEasing.transform(local)
            val x = bw * 0.8f + i * bw * 1.4f
            drawRoundRect(colors[i], Offset(x, h - bh - 4.dp.toPx()), Size(bw, bh), CornerRadius(6.dp.toPx()))
        }
        drawLine(axis, Offset(0f, h - 3.dp.toPx()), Offset(w, h - 3.dp.toPx()), 3.dp.toPx())
        // Trend line over the columns.
        val pts = heights.mapIndexed { i, v -> Offset(bw * 1.3f + i * bw * 1.4f, h - (h - 8.dp.toPx()) * v * p - 14.dp.toPx()) }
        for (i in 0 until pts.size - 1) drawLine(DateBrown, pts[i], pts[i + 1], 3.dp.toPx())
        pts.forEach { drawCircle(Cream, 4.dp.toPx(), it); drawCircle(DateBrown, 4.dp.toPx(), it, style = Stroke(2.dp.toPx())) }
    }
}

/** A phone sending little packets up to a cloud. */
@Composable
private fun BackupArt() {
    val tr = rememberInfiniteTransition(label = "backup")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "t")
    val phone = MaterialTheme.colorScheme.primary
    val cloud = MaterialTheme.colorScheme.primaryContainer
    val cloudEdge = MaterialTheme.colorScheme.primary
    Halo(MaterialTheme.colorScheme.primary)
    Canvas(Modifier.size(170.dp)) {
        val w = size.width
        val h = size.height
        // Cloud
        val cy = h * 0.24f
        listOf(Offset(w * 0.35f, cy + 8.dp.toPx()) to 24.dp, Offset(w * 0.52f, cy) to 32.dp, Offset(w * 0.68f, cy + 10.dp.toPx()) to 22.dp).forEach { (c, r) ->
            drawCircle(cloud, r.toPx(), c)
        }
        drawRoundRect(cloud, Offset(w * 0.28f, cy + 6.dp.toPx()), Size(w * 0.46f, 26.dp.toPx()), CornerRadius(13.dp.toPx()))
        drawRoundRect(cloudEdge, Offset(w * 0.28f, cy + 6.dp.toPx()), Size(w * 0.46f, 26.dp.toPx()), CornerRadius(13.dp.toPx()), style = Stroke(1.dp.toPx()))
        // Phone
        val pw = w * 0.3f
        val ph = h * 0.36f
        val px = (w - pw) / 2
        val py = h - ph - 4.dp.toPx()
        drawRoundRect(phone, Offset(px, py), Size(pw, ph), CornerRadius(10.dp.toPx()))
        drawRoundRect(Cream, Offset(px + 5.dp.toPx(), py + 10.dp.toPx()), Size(pw - 10.dp.toPx(), ph - 22.dp.toPx()), CornerRadius(4.dp.toPx()))
        // Packets travelling up.
        for (i in 0 until 3) {
            val p = (t + i / 3f) % 1f
            val y = py - p * (py - cy - 30.dp.toPx())
            val a = if (p < 0.15f) p / 0.15f else if (p > 0.8f) (1f - p) / 0.2f else 1f
            drawRoundRect(Leaf.copy(alpha = a), Offset(w / 2 - 7.dp.toPx(), y - 7.dp.toPx()), Size(14.dp.toPx(), 14.dp.toPx()), CornerRadius(3.dp.toPx()))
        }
    }
}

/** A benefits picture with the bottle on it, and green bubbles flying off it to be shared. */
@Composable
private fun ShareArt() {
    val tr = rememberInfiniteTransition(label = "share")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "t")
    val tilt by tr.animateFloat(-4f, 4f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    Halo(Leaf)
    Box(
        Modifier.size(120.dp, 150.dp).graphicsLayer { rotationZ = tilt }.clip(RoundedCornerShape(14.dp)).background(Cream)
            .padding(10.dp),
    ) {
        Column {
            Box(Modifier.width(60.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DateBrown.copy(alpha = 0.7f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(80.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFB8782E).copy(alpha = 0.7f)))
            Spacer(Modifier.height(10.dp))
            repeat(3) {
                Box(Modifier.padding(bottom = 6.dp).width(54.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.Gray.copy(alpha = 0.35f)))
            }
        }
        BottleOf(ProductSize.SMALL, 70.dp, 0.85f, Modifier.align(Alignment.BottomEnd))
    }
    // Message bubbles leaving the picture.
    Canvas(Modifier.fillMaxSize()) {
        for (i in 0 until 3) {
            val p = (t + i / 3f) % 1f
            val x = size.width * (0.62f + 0.28f * p)
            val y = size.height * (0.55f - 0.45f * p)
            val a = if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
            drawCircle(Color(0xFF25D366).copy(alpha = a), 13.dp.toPx() * (0.6f + 0.4f * p), Offset(x, y))
            drawCircle(Color.White.copy(alpha = a), 4.dp.toPx() * (0.6f + 0.4f * p), Offset(x, y))
        }
    }
}

/** A "?" with rings spreading out from it. */
@Composable
private fun GuideArt() {
    val tr = rememberInfiniteTransition(label = "guide")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "t")
    val c = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        for (i in 0 until 3) {
            val p = (t + i / 3f) % 1f
            drawCircle(c.copy(alpha = 0.35f * (1f - p)), size.minDimension * (0.22f + 0.26f * p), style = Stroke(3.dp.toPx()))
        }
        drawCircle(c, size.minDimension * 0.22f)
    }
    Text("?", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
}
