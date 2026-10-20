package ly.jawdat.alhaya.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Herb
import ly.jawdat.alhaya.data.ProductSize
import kotlin.math.PI
import kotlin.math.sin

// Small animated pieces shared by the screens: a drawn vinegar bottle, the bottle-size and type
// pickers, a quantity stepper that stacks up bottles, and a few entrance/pop helpers.

/** The colour of date vinegar in the drawn bottles. */
val Vinegar = Color(0xFFA8642A)
private val VinegarLight = Color(0xFFD39A55)

/** A dot colour for each vinegar type, used by the type picker. */
fun herbColor(h: String): Color = when (h) {
    Herb.PLAIN -> DateBrown
    Herb.THYME -> Color(0xFF6B8E23)
    Herb.ROSEMARY -> Color(0xFF3F7A5A)
    Herb.GINGER -> Color(0xFFD9A441)
    Herb.GARLIC -> Color(0xFFB9A98A)
    Herb.FENNEL -> Color(0xFF9DBF5A)
    Herb.MIXED -> Leaf
    else -> Color(0xFF8D9387)
}

/**
 * A glass bottle drawn to fill its box, with vinegar up to [fill] (0–1). [large] draws the tall
 * 1 L shape with a long neck; otherwise the short 330 ml one. [wave] lets the surface ripple.
 */
@Composable
fun Bottle(
    modifier: Modifier,
    fill: Float,
    large: Boolean = false,
    wave: Boolean = true,
    liquid: Color = Vinegar,
) {
    val glass = MaterialTheme.colorScheme.outline
    val cap = MaterialTheme.colorScheme.primary
    val phase = if (wave) {
        val tr = rememberInfiniteTransition(label = "wave")
        tr.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "phase").value
    } else 0f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w * 0.06f).coerceIn(1.2f, 6f)
        val capH = h * 0.08f
        val neckW = w * (if (large) 0.30f else 0.38f)
        val shoulder = h * (if (large) 0.34f else 0.24f)
        val bodyTop = h * (if (large) 0.48f else 0.40f)
        val left = stroke / 2
        val right = w - stroke / 2
        val bottom = h - stroke / 2
        val r = w * 0.16f
        val cx = w / 2
        val bottle = Path().apply {
            moveTo(cx - neckW / 2, capH)
            lineTo(cx - neckW / 2, shoulder)
            cubicTo(cx - neckW / 2, bodyTop - (bodyTop - shoulder) * 0.2f, left, bodyTop - (bodyTop - shoulder) * 0.5f, left, bodyTop)
            lineTo(left, bottom - r)
            quadraticTo(left, bottom, left + r, bottom)
            lineTo(right - r, bottom)
            quadraticTo(right, bottom, right, bottom - r)
            lineTo(right, bodyTop)
            cubicTo(right, bodyTop - (bodyTop - shoulder) * 0.5f, cx + neckW / 2, bodyTop - (bodyTop - shoulder) * 0.2f, cx + neckW / 2, shoulder)
            lineTo(cx + neckW / 2, capH)
            close()
        }
        // Vinegar, clipped to the glass, with a gentle wave on top.
        val f = fill.coerceIn(0f, 1f)
        if (f > 0f) clipPath(bottle) {
            val top = capH + h * 0.04f
            val level = bottom - f * (bottom - top)
            val amp = if (wave) h * 0.018f else 0f
            val surface = Path().apply {
                moveTo(0f, level)
                var x = 0f
                while (x <= w) {
                    lineTo(x, level + amp * sin(phase + x / w * 2f * PI.toFloat()))
                    x += w / 12f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(surface, Brush.verticalGradient(listOf(VinegarLight, liquid), startY = level, endY = h))
        }
        // Label band across the body.
        val labelTop = bodyTop + (bottom - bodyTop) * 0.28f
        val labelH = (bottom - bodyTop) * 0.34f
        drawRoundRect(
            Cream.copy(alpha = 0.9f), Offset(left + stroke, labelTop), Size(right - left - stroke * 2, labelH),
            CornerRadius(labelH * 0.2f),
        )
        drawRect(Leaf, Offset(left + stroke, labelTop + labelH * 0.38f), Size(right - left - stroke * 2, labelH * 0.24f))
        // Glass outline and a shine.
        drawPath(bottle, glass, style = Stroke(stroke))
        drawLine(
            Color.White.copy(alpha = 0.55f), Offset(left + stroke * 2.2f, bodyTop + h * 0.04f), Offset(left + stroke * 2.2f, labelTop - h * 0.02f),
            strokeWidth = stroke, cap = StrokeCap.Round,
        )
        // Cap.
        drawRoundRect(cap, Offset(cx - neckW * 0.62f, 0f), Size(neckW * 1.24f, capH + stroke), CornerRadius(stroke))
    }
}

/** Pops its content in with a little bounce the first time it appears. */
@Composable
fun PopIn(delayMs: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        s.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Box(modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = s.value.coerceIn(0f, 1f) }) { content() }
}

/** Fades and slides its content up the first time it appears. */
@Composable
fun Reveal(delayMs: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        a.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 28.dp.toPx() }) { content() }
}

/** A number that counts up or down to [target] instead of jumping. */
@Composable
fun animatedNumber(target: Double, durationMs: Int = 650): Double {
    val a by animateFloatAsState(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing), label = "number")
    return if (a == target.toFloat()) target else a.toDouble()
}

// ---------- Bottle size ----------

/**
 * The two bottle sizes as picture cards. A product saved before sizes were fixed may hold another
 * size ([selected] is then neither); it is kept until one of the two is chosen.
 */
@Composable
fun SizePicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("الحجم", "Size"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SizeCard(selected == ProductSize.SMALL, large = false, sizeName(ProductSize.SMALL), t("قارورة صغيرة", "Small bottle"), Modifier.weight(1f)) {
                onSelect(ProductSize.SMALL)
            }
            SizeCard(selected == ProductSize.LARGE, large = true, sizeName(ProductSize.LARGE), t("قارورة كبيرة", "Large bottle"), Modifier.weight(1f)) {
                onSelect(ProductSize.LARGE)
            }
        }
        if (selected.isNotBlank() && selected !in ProductSize.all) Text(
            t("الحجم المسجّل سابقاً: $selected — اختر أحد الحجمين.", "Size saved before: $selected — choose one of the two."),
            style = MaterialTheme.typography.bodySmall, color = Warn,
        )
    }
}

@Composable
private fun SizeCard(selected: Boolean, large: Boolean, label: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.95f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "scale")
    val fill by animateFloatAsState(if (selected) 0.82f else 0.18f, tween(900, easing = FastOutSlowInEasing), label = "fill")
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "border")
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, label = "bg")
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Both bottles stand in the same height so the sizes compare at a glance.
                Box(Modifier.height(96.dp), contentAlignment = Alignment.BottomCenter) {
                    Bottle(Modifier.width(if (large) 38.dp else 40.dp).height(if (large) 94.dp else 62.dp), fill, large)
                }
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.animation.AnimatedVisibility(
                selected,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Ic(R.drawable.ic_check, Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------- Vinegar type ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HerbPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("النوع", "Type"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Herb.all.forEach { h ->
                val on = h == selected
                val scale by animateFloatAsState(if (on) 1.06f else 1f, spring(dampingRatio = 0.4f), label = "chip")
                val dot by animateFloatAsState(if (on) 14f else 10f, spring(dampingRatio = 0.4f), label = "dot")
                FilterChip(
                    selected = on,
                    onClick = { onSelect(h) },
                    label = { Text(herbName(h)) },
                    leadingIcon = { Box(Modifier.size(dot.dp).clip(CircleShape).background(herbColor(h))) },
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                )
            }
        }
    }
}

// ---------- Quantity ----------

/**
 * − / + with the number in the middle (it can also be typed), and a row of little bottles that
 * pop in as the number grows.
 */
@Composable
fun QtyStepper(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    min: Int = 1,
    large: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    liquid: Color = Vinegar,
) {
    val n = parseNum(value)?.toInt() ?: 0
    val bump = remember { Animatable(1f) }
    LaunchedEffect(n) {
        bump.snapTo(1.18f)
        bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onChange((n - 1).coerceAtLeast(min).toString()) }, modifier = Modifier.size(52.dp)) {
                    Text("−", style = MaterialTheme.typography.headlineSmall)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { v -> onChange(v.filter { it.isDigit() || it in '٠'..'٩' }.take(4)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = accent,
                    ),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).graphicsLayer { scaleX = bump.value; scaleY = bump.value },
                )
                FilledTonalIconButton(onClick = { onChange((n + 1).toString()) }, modifier = Modifier.size(52.dp)) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
            BottleRow(n, large, liquid)
        }
    }
}

/** Up to 12 little bottles, then "+N". */
@Composable
fun BottleRow(n: Int, large: Boolean = false, liquid: Color = Vinegar) {
    val shown = n.coerceIn(0, 12)
    Row(
        Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until shown) key(i) {
            PopIn(delayMs = (i * 35).coerceAtMost(300)) {
                Bottle(Modifier.width(13.dp).height(if (large) 32.dp else 24.dp), 0.8f, large, wave = false, liquid = liquid)
            }
        }
        if (n > 12) Text(
            "+${n - 12}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** A small check mark that bounces in, for "saved" messages. */
@Composable
fun CheckBurst(modifier: Modifier = Modifier) {
    PopIn(modifier = modifier) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Good), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_check, Modifier.size(20.dp), Color.White)
        }
    }
}

/** A gift box that wobbles now and then. */
@Composable
fun GiftIcon(modifier: Modifier = Modifier, size: Float = 22f) {
    val tr = rememberInfiniteTransition(label = "gift")
    val angle by tr.animateFloat(
        -8f, 8f,
        infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "angle",
    )
    Text("🎁", modifier = modifier.graphicsLayer { rotationZ = angle }, fontSize = size.sp)
}
