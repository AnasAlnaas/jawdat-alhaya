package ly.jawdat.alhaya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.jawdat.alhaya.data.ProductSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// A short celebration laid over a screen: confetti of dates, palm leaves and golden drops bursting
// from the middle. The big one (a new batch, a finished batch) also fills a bottle and waits for a
// tap; the small one (moving to the next stage) shows a check and goes away by itself.

private class Piece(
    val angle: Float, val speed: Float, val spin: Float, val kind: Int, val size: Float, val delay: Float,
)

@Composable
fun Celebration(
    visible: Boolean,
    title: String,
    subtitle: String,
    big: Boolean,
    onDone: () -> Unit,
) {
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(350))) {
        val haptic = LocalHapticFeedback.current
        BackHandler { onDone() }
        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            if (!big) {
                delay(2300)
                onDone()
            }
        }
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (big) 0.55f else 0.35f))
                .clickable(remember { MutableInteractionSource() }, indication = null) { onDone() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(32.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (big) FillingBottle() else CheckBurst()
                Reveal(delayMs = if (big) 500 else 150) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                Reveal(delayMs = if (big) 650 else 250) {
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (big) Reveal(delayMs = 900) {
                    Button(onClick = onDone, modifier = Modifier.padding(top = 6.dp)) { Text(t("متابعة", "Continue")) }
                }
            }
            // Over the card, so the pieces fly across it.
            Confetti(if (big) 90 else 45)
        }
    }
}

/** A bottle that fills up, then a check mark pops onto it. */
@Composable
private fun FillingBottle() {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        fill.animateTo(0.85f, tween(1300, easing = FastOutSlowInEasing))
    }
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(140.dp).clip(CircleShape).background(Leaf.copy(alpha = 0.16f)))
        PopIn {
            BottleOf(ProductSize.SMALL, 118.dp, fill.value)
        }
        if (fill.value > 0.8f) CheckBurst(Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }
}

/** Confetti that bursts up from the middle and falls, fading out at the end. */
@Composable
private fun Confetti(count: Int) {
    val pieces = remember {
        val rnd = Random(count)
        List(count) {
            Piece(
                angle = (-160 + rnd.nextFloat() * 140) * (Math.PI / 180).toFloat(),
                speed = 0.55f + rnd.nextFloat() * 0.9f,
                spin = (rnd.nextFloat() - 0.5f) * 4f,
                kind = rnd.nextInt(5),
                size = 6f + rnd.nextFloat() * 8f,
                delay = rnd.nextFloat() * 0.12f,
            )
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) { launch { time.animateTo(1f, tween(2800, easing = LinearEasing)) } }
    val colors = listOf(DateBrown, Leaf, Vinegar, Teal, Color(0xFFF7C860))
    Canvas(Modifier.fillMaxSize()) {
        val ox = size.width / 2
        val oy = size.height * 0.45f
        pieces.forEach { p ->
            val t = ((time.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = ox + cos(p.angle) * p.speed * t * size.width * 0.75f
            val y = oy + sin(p.angle) * p.speed * t * size.height * 0.75f + 0.9f * t * t * size.height
            val a = if (t < 0.7f) 1f else (1f - (t - 0.7f) / 0.3f)
            val s = p.size.dp.toPx()
            val c = colors[p.kind].copy(alpha = a)
            rotate(p.spin * t * 360f, Offset(x, y)) {
                when (p.kind) {
                    0 -> drawOval(c, Offset(x - s, y - s * 0.55f), Size(s * 2, s * 1.1f)) // a date
                    1 -> drawOval(c, Offset(x - s * 1.2f, y - s * 0.35f), Size(s * 2.4f, s * 0.7f)) // a palm leaflet
                    2 -> drawCircle(c, s * 0.6f, Offset(x, y)) // a golden drop
                    3 -> drawRect(c, Offset(x - s * 0.5f, y - s * 0.3f), Size(s, s * 0.6f))
                    else -> {
                        val star = Path().apply {
                            moveTo(x, y - s)
                            quadraticTo(x, y, x + s, y)
                            quadraticTo(x, y, x, y + s)
                            quadraticTo(x, y, x - s, y)
                            quadraticTo(x, y, x, y - s)
                            close()
                        }
                        drawPath(star, c)
                    }
                }
            }
        }
    }
}
