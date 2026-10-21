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

// Small animated pieces shared by the screens: the bottle-size and type
// pickers, a quantity stepper that stacks up bottles, and a few entrance/pop helpers.

/** A dot colour for each vinegar type, used by the type picker. */
fun herbColor(h: String): Color = when (h) {
    Herb.PLAIN -> DateBrown
    Herb.THYME -> Color(0xFF6B8E23)
    Herb.ROSEMARY -> Color(0xFF3F7A5A)
    Herb.GINGER -> Color(0xFFC9A57A)
    Herb.GARLIC -> Color(0xFF9E9A8C)
    Herb.FENNEL -> Color(0xFF9DBF5A)
    Herb.MIXED -> Leaf
    else -> Color(0xFF8D9387)
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

/** "صغيرة" / "وسط" / "كبيرة" for the three bottles. */
fun sizeWord(size: String): String = when (size) {
    ProductSize.TINY -> t("صغيرة", "Small")
    ProductSize.LARGE -> t("كبيرة", "Large")
    else -> t("وسط", "Medium")
}

/**
 * The three bottle sizes as picture cards. A product saved before sizes were fixed may hold another
 * size ([selected] is then none of them); it is kept until one is chosen.
 */
@Composable
fun SizePicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("الحجم", "Size"), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProductSize.all.forEach { size ->
                SizeCard(selected == size, size, sizeName(size), sizeWord(size), Modifier.weight(1f)) { onSelect(size) }
            }
        }
        if (selected.isNotBlank() && selected !in ProductSize.all) Text(
            t("الحجم المسجّل سابقاً: $selected — اختر أحد الأحجام.", "Size saved before: $selected — choose one of the sizes."),
            style = MaterialTheme.typography.bodySmall, color = Warn,
        )
    }
}

@Composable
private fun SizeCard(selected: Boolean, size: String, label: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
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
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // All bottles stand on the same line at their real relative heights.
                Box(Modifier.height(112.dp), contentAlignment = Alignment.BottomCenter) {
                    BottleOf(size, 110.dp * bottleScale(size), fill)
                }
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
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
fun HerbPicker(selected: String, options: List<String> = Herb.all, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("النوع", "Type"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { h ->
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
    bottle: String = ProductSize.SMALL,
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
            BottleRow(n, bottle, liquid)
        }
    }
}

/** Up to 12 little bottles, then "+N". */
@Composable
fun BottleRow(n: Int, bottle: String = ProductSize.SMALL, liquid: Color = Vinegar) {
    val shown = n.coerceIn(0, 12)
    Row(
        Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until shown) key(i) {
            PopIn(delayMs = (i * 35).coerceAtMost(300)) {
                Bottle(
                    Modifier.width(38.dp * bottleScale(bottle) * bottleAspect(bottle)).height(38.dp * bottleScale(bottle)),
                    0.8f, bottle, wave = false, liquid = liquid,
                )
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
