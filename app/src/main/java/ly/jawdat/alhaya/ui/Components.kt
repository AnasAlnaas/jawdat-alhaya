package ly.jawdat.alhaya.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Repo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Re-reads data whenever anything in the database changes. */
@Composable
fun <T> watch(vararg keys: Any?, read: () -> T): T {
    val version by Repo.changes.collectAsState()
    return remember(version, Lang.arabic, *keys) { read() }
}

@Composable
fun Ic(@DrawableRes id: Int, modifier: Modifier = Modifier.size(22.dp), tint: Color? = null, mirror: Boolean = false) {
    val flip = mirror && LocalLayoutDirection.current == LayoutDirection.Rtl
    val m = if (flip) modifier.scale(-1f, 1f) else modifier
    if (tint == null) Icon(painterResource(id), null, m) else Icon(painterResource(id), null, m, tint = tint)
}

@Composable
fun AppScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    /** Guide section opened by the "?" button; null hides the button. */
    help: String? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Ic(R.drawable.ic_back, mirror = true) }
                },
                actions = {
                    if (help != null) IconButton(onClick = { Nav.go(Screen.Guide(help)) }) { Ic(R.drawable.ic_help) }
                    actions()
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = fab,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** A scrolling list with standard spacing, used by most screens. */
@Composable
fun ListBody(
    padding: PaddingValues,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun FormBody(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

/** A rounded card that slides softly into place when it appears and dips a little when pressed. */
@Composable
fun AppCard(
    onClick: (() -> Unit)? = null,
    color: Color? = null,
    modifier: Modifier = Modifier,
    /** False for cards that arrive by a shared-element transition and must not fade in again. */
    animateIn: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = color ?: MaterialTheme.colorScheme.surfaceContainer)
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.6f), label = "press")
    val mod = modifier.fillMaxWidth().graphicsLayer {
        alpha = appear.value
        translationY = (1f - appear.value) * 18.dp.toPx()
        scaleX = press
        scaleY = press
    }
    if (onClick != null) Card(onClick = onClick, modifier = mod, colors = colors, shape = RoundedCornerShape(16.dp), interactionSource = interaction) {
        Column(Modifier.padding(14.dp), content = content)
    } else Card(modifier = mod, colors = colors, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, note: String? = null) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedContent(
                value,
                transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                label = "stat",
            ) { v ->
                Text(v, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun StatRow(vararg tiles: Triple<String, String, Color?>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.forEach { (l, v, c) -> StatTile(l, v, Modifier.weight(1f), c ?: MaterialTheme.colorScheme.primary) }
    }
}

/** "Label ........ value" line used in details and reports. */
@Composable
fun KeyValue(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = if (bold) FontWeight.SemiBold else null)
    }
}

/** A horizontal bar for simple charts: label, bar sized against [max], and the value text. */
@Composable
fun BarRow(label: String, value: Double, max: Double, valueText: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val target = if (max > 0) (value / max).toFloat().coerceIn(0f, 1f) else 0f
            val grow = remember { Animatable(0f) }
            LaunchedEffect(target) { grow.animateTo(target, tween(800, easing = FastOutSlowInEasing)) }
            if (grow.value > 0f) Box(Modifier.fillMaxWidth(grow.value).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

/** Columns over time (one per day or week), heights relative to the largest. */
@Composable
fun ColumnChart(values: List<Double>, labels: List<String>, height: Dp = 120.dp) {
    val max = values.maxOrNull() ?: 0.0
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Row(Modifier.fillMaxWidth().height(height + 18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                // Columns rise one after another, left to right.
                val delayed = ((grow.value * 1.5f) - i.toFloat() / values.size.coerceAtLeast(1) * 0.5f).coerceIn(0f, 1f)
                val f = if (max > 0) (v / max).toFloat() * delayed else 0f
                Box(
                    Modifier.fillMaxWidth().height(height * f.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (v > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false,
    phone: Boolean = false,
    singleLine: Boolean = true,
    supporting: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                number -> KeyboardType.Decimal
                phone -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        ),
        shape = RoundedCornerShape(12.dp),
    )
}

/** A read-only field that opens something when tapped (date picker, option list). */
@Composable
fun TapField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = { Ic(R.drawable.ic_expand, Modifier.size(20.dp)) },
        )
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick))
    }
}

@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), clearable: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    TapField(label, value?.let { "${date(it)}  (${relative(it)})" } ?: "—", { open = true }, modifier)
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(t("تم", "OK")) }
            },
            dismissButton = {
                Row {
                    if (clearable) TextButton(onClick = { onChange(null); open = false }) { Text(t("بدون تاريخ", "No date")) }
                    TextButton(onClick = { open = false }) { Text(t("إلغاء", "Cancel")) }
                }
            },
        ) { DatePicker(state) }
    }
}

/** Pick one option from a list, shown in a dialog. */
@Composable
fun <T> PickField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    emptyText: String = "—",
    footer: (@Composable (close: () -> Unit) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    TapField(label, options.firstOrNull { it.first == selected }?.second ?: emptyText, { open = true }, modifier)
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options) { (value, text) ->
                        Text(
                            text,
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(if (value == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onSelect(value); open = false }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (footer != null) item { footer { open = false } }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(t("إغلاق", "Close")) } },
        )
    }
}

/** A row of choice chips that scrolls sideways. */
@Composable
fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) })
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = true) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("إلغاء", "Cancel")) } },
    )
}

@Composable
fun EmptyState(text: String, hint: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // A nearly empty bottle bobbing gently.
        val bob by rememberInfiniteTransition(label = "bob").animateFloat(
            -4f, 4f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
        )
        Bottle(Modifier.width(24.dp).height(80.dp).graphicsLayer { translationY = bob.dp.toPx() }, 0.15f, ly.jawdat.alhaya.data.ProductSize.LARGE)
        Spacer(Modifier.height(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Small coloured label like "Paid" / "Owes 20". */
@Composable
fun Badge(text: String, color: Color) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color, style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun Divider() = HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
fun rememberText(initial: String) = rememberSaveable { mutableStateOf(initial) }

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
