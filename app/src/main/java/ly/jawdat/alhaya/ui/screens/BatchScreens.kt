package ly.jawdat.alhaya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Batch
import ly.jawdat.alhaya.data.BatchHerb
import ly.jawdat.alhaya.data.BatchOutput
import ly.jawdat.alhaya.data.BatchStatus
import ly.jawdat.alhaya.data.BatchSummary
import ly.jawdat.alhaya.data.Herb
import ly.jawdat.alhaya.data.ItemKind
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.data.ProductSize
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.Stage
import ly.jawdat.alhaya.data.StageItem
import ly.jawdat.alhaya.data.StageStatus
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.Badge
import ly.jawdat.alhaya.ui.Celebration
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.DateField
import ly.jawdat.alhaya.ui.Divider
import ly.jawdat.alhaya.ui.EmptyState
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.FormBody
import ly.jawdat.alhaya.ui.Good
import ly.jawdat.alhaya.ui.HerbPicker
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.KeyValue
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.PickField
import ly.jawdat.alhaya.ui.QtyStepper
import ly.jawdat.alhaya.ui.Reveal
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.TourArt
import ly.jawdat.alhaya.ui.TourIllustration
import ly.jawdat.alhaya.ui.Vinegar
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.days
import ly.jawdat.alhaya.ui.herbColor
import ly.jawdat.alhaya.ui.herbName
import ly.jawdat.alhaya.ui.itemKindName
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.ltr
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.relative
import ly.jawdat.alhaya.ui.shared
import ly.jawdat.alhaya.ui.stageStatusName
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.title
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// A batch is plain date vinegar from start to finish. It moves through its stages one at a time
// ("next stage"), and at the last stage some litres of it can be mixed with herbs.

val herbOptions: List<Pair<String, String>> get() = Herb.all.map { it to herbName(it) }

/** "طبيعي · زنجبيل 10 لتر · زعتر 10 لتر": what the batch was made into. */
fun herbSummary(s: BatchSummary): String =
    (listOf(herbName(s.batch.herb)) + s.herbs.groupBy { it.herb }.map { (h, l) -> herbName(h) + " " + liters(l.sumOf { it.liters }) })
        .joinToString(" · ")

fun liters(v: Double): String = t("${ltr(num(v))} لتر", "${num(v)} L")

// ---------- List ----------

@Composable
fun BatchesScreen() {
    var filter by rememberSaveable { mutableStateOf(BatchStatus.ACTIVE) }
    val all = watch { Repo.batchSummaries() }
    val shown = all.filter { filter == "all" || it.batch.status == filter }
    val listState = rememberLazyListState()
    // The button shrinks to its icon once the list is scrolled.
    val expanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    AppScreen(
        title = t("الإنتاج", "Production"),
        help = GuideIds.BATCHES,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { Nav.go(Screen.NewBatch) },
                expanded = expanded,
                icon = { Ic(R.drawable.ic_add) },
                text = { Text(t("دفعة جديدة", "New batch")) },
            )
        },
    ) { pad ->
        ListBody(pad, listState) {
            item {
                ChipRow(
                    listOf(
                        BatchStatus.ACTIVE to t("جارية (${all.count { it.batch.status == BatchStatus.ACTIVE }})", "In progress (${all.count { it.batch.status == BatchStatus.ACTIVE }})"),
                        BatchStatus.DONE to t("منتهية", "Finished"),
                        "all" to t("الكل", "All"),
                    ),
                    filter,
                ) { filter = it }
            }
            if (shown.isEmpty()) item {
                EmptyState(
                    t("لا توجد دفعات هنا", "No batches here"),
                    t("اضغط \"دفعة جديدة\" عند بدء نقع التمر.", "Tap \"New batch\" when you start soaking the dates."),
                )
            }
            items(shown, key = { it.batch.id }) { Box(Modifier.animateItem()) { BatchCard(it) } }
        }
    }
}

@Composable
fun BatchCard(s: BatchSummary) {
    val cur = s.current
    AppCard(onClick = { Nav.go(Screen.BatchDetail(s.batch.id)) }, modifier = Modifier.shared("batch-${s.batch.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    listOf(s.batch.label, s.batch.name).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(herbSummary(s), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            when (s.batch.status) {
                BatchStatus.DONE -> Badge(t("منتهية", "Finished"), Good)
                BatchStatus.CANCELLED -> Badge(t("ملغاة", "Cancelled"), Danger)
                else -> Badge("${s.doneCount}/${s.stages.size}", MaterialTheme.colorScheme.primary)
            }
        }
        if (s.batch.status == BatchStatus.ACTIVE) StageDots(s, Modifier.padding(vertical = 8.dp))
        if (s.batch.status == BatchStatus.ACTIVE && cur != null) {
            Text(t("المرحلة الحالية: ", "Current stage: ") + cur.name, style = MaterialTheme.typography.bodyMedium)
            cur.checkDate?.let { d ->
                Text(t("المتابعة: ", "Check on: ") + date(d) + " (" + relative(d) + ")", color = checkColor(d), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.padding(top = 6.dp)) {
            Text(t("بدأت ", "Started ") + date(s.batch.startDate), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("التكلفة ", "Cost ") + money(s.totalCost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One dot per stage joined by a line that fills up to the current stage. */
@Composable
private fun StageDots(s: BatchSummary, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(s.progress) { progress.animateTo(s.progress, tween(900, easing = FastOutSlowInEasing)) }
    val done = Good
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    val stages = s.stages.sortedBy { it.position }
    val n = stages.size
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        if (n == 0) return@Canvas
        val r = size.height / 2
        val y = size.height / 2
        val step = if (n > 1) (size.width - 2 * r) / (n - 1) else 0f
        // In right-to-left the first stage sits on the right.
        val rtl = layoutDirection == LayoutDirection.Rtl
        fun xAt(i: Float) = if (rtl) size.width - r - i * step else r + i * step
        drawLine(idle, Offset(xAt(0f), y), Offset(xAt((n - 1).toFloat()), y), r * 0.5f, StrokeCap.Round)
        drawLine(active, Offset(xAt(0f), y), Offset(xAt(progress.value * (n - 1)), y), r * 0.5f, StrokeCap.Round)
        stages.forEachIndexed { i, st ->
            val c = when (st.status) {
                StageStatus.DONE -> done
                StageStatus.ACTIVE -> active
                else -> idle
            }
            drawCircle(c, if (st.status == StageStatus.ACTIVE) r else r * 0.7f, Offset(xAt(i.toFloat()), y))
        }
    }
}

// ---------- New batch ----------

@Composable
fun NewBatchScreen() {
    val today = LocalDate.now()
    var start by remember { mutableStateOf(today) }
    var code by rememberSaveable { mutableStateOf(Repo.nextBatchCode(today)) }
    var name by rememberSaveable { mutableStateOf("") }
    var datesKg by rememberSaveable { mutableStateOf("") }
    var datesCost by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val stages = remember { mutableStateListOf<Pair<String, Boolean>>().apply { addAll(Prefs.stageTemplate.map { it to true }) } }
    var newStage by remember { mutableStateOf("") }

    AppScreen(t("دفعة جديدة", "New batch"), onBack = { Nav.back() }, help = GuideIds.BATCHES) { pad ->
        FormBody(pad) {
            Reveal(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TourIllustration(TourArt.PRODUCTION, Modifier.size(180.dp))
                    Text(
                        t("دفعة جديدة من التمر فقط. الأعشاب تُضاف في المرحلة الأخيرة لجزء من الكمية.", "A new batch of dates only. Herbs are added to part of it at the last stage."),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Field(t("رقم الدفعة", "Batch number"), code, { code = it })
            Field(t("اسم أو وصف (اختياري)", "Name or note (optional)"), name, { name = it }, supporting = t("مثال: تمر دقلة", "e.g. Deglet dates"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("كمية التمر (كجم)", "Dates (kg)"), datesKg, { datesKg = it }, Modifier.weight(1f), number = true)
                Field(t("سعر التمر (د.ل)", "Dates cost (LYD)"), datesCost, { datesCost = it }, Modifier.weight(1f), number = true)
            }
            DateField(t("تاريخ البدء", "Start date"), start, { it?.let { d -> start = d } })
            Field(t("ملاحظات", "Notes"), notes, { notes = it }, singleLine = false)

            SectionTitle(t("المراحل", "Stages"))
            Text(
                t("هذه مراحل الدفعة بالترتيب. الانتقال من مرحلة إلى التالية يدوي، واحدة كل مرة.", "These are the batch's stages in order. You move on to the next one by hand, one at a time."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            stages.forEachIndexed { i, (n, on) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(on, { stages[i] = n to it })
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1}. $n")
                        Prefs.stageHint(n)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(t("مرحلة إضافية", "Extra stage"), newStage, { newStage = it }, Modifier.weight(1f))
                TextButton(onClick = { if (newStage.isNotBlank()) { stages.add(newStage.trim() to true); newStage = "" } }) { Text(t("إضافة", "Add")) }
            }

            Button(
                onClick = {
                    val id = Repo.createBatch(
                        Batch(code = code.trim().ifBlank { Repo.nextBatchCode(start) }, name = name, herb = Herb.PLAIN, startDate = start, notes = notes),
                        stages.filter { it.second }.map { it.first },
                    )
                    // The dates go on the first stage, so their cost and kilos count from day one.
                    val kg = parseNum(datesKg) ?: 0.0
                    val cost = parseNum(datesCost) ?: 0.0
                    Repo.stages(id).firstOrNull()?.let { first ->
                        if (kg > 0 || cost > 0) Repo.saveStageItem(
                            StageItem(
                                stageId = first.id, batchId = id, date = start, kind = ItemKind.DATES,
                                item = itemKindName(ItemKind.DATES), qty = kg, unit = t("كجم", "kg"), cost = cost,
                            )
                        )
                    }
                    Nav.pendingCelebration = id
                    Nav.replace(Screen.BatchDetail(id))
                },
                enabled = stages.any { it.second },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
            ) { Text(t("بدء الدفعة", "Start batch")) }
        }
    }
}

// ---------- Batch detail ----------

@Composable
fun BatchDetailScreen(id: Long) {
    val context = LocalContext.current
    val s = watch(id) { Repo.batchSummary(id) } ?: return
    val b = s.batch
    val items = watch(id) { Repo.stageItemsForBatch(id) }
    val outputs = watch(id) { Repo.outputs(id) }
    val products = watch { Repo.products(true) }.associateBy { it.id }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var addingOutput by remember { mutableStateOf(false) }
    var addingStage by remember { mutableStateOf(false) }
    var advancing by remember { mutableStateOf(false) }
    var addingHerb by remember { mutableStateOf<BatchHerb?>(null) }

    // Celebrations: a new batch (once, right after it is created), a new stage, a finished batch.
    var celebrateNew by remember { mutableStateOf(Nav.pendingCelebration == id) }
    LaunchedEffect(Unit) { if (Nav.pendingCelebration == id) Nav.pendingCelebration = null }
    var celebrateStage by remember { mutableStateOf<String?>(null) }
    var celebrateDone by remember { mutableStateOf(false) }

    val active = b.status == BatchStatus.ACTIVE
    val cur = s.current?.takeIf { it.status == StageStatus.ACTIVE }

    Box(Modifier.fillMaxSize()) {
        AppScreen(
            title = b.label,
            help = GuideIds.BATCHES,
            onBack = { Nav.back() },
            actions = {
                IconButton(onClick = {
                    val qr = qrText(s, products.values.filter { p -> outputs.any { it.productId == p.id } }.map { it.title })
                    Files.shareQrLabel(context, qr, listOf(t("جودة الحياة", "Jawdat Al-Hayah"), b.label + " · " + herbSummary(s)))
                }) { Ic(R.drawable.ic_qr) }
                Box {
                    IconButton(onClick = { menu = true }) { Ic(R.drawable.ic_settings) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(t("تعديل البيانات", "Edit details")) }, onClick = { menu = false; editing = true })
                        if (active) {
                            DropdownMenuItem(text = { Text(t("إلغاء الدفعة (فشلت)", "Cancel batch (failed)")) }, onClick = {
                                menu = false
                                Repo.updateBatch(b.copy(status = BatchStatus.CANCELLED, finishedDate = LocalDate.now()))
                            })
                        } else {
                            DropdownMenuItem(text = { Text(t("إعادة فتح الدفعة", "Reopen batch")) }, onClick = {
                                menu = false
                                Repo.updateBatch(b.copy(status = BatchStatus.ACTIVE, finishedDate = null))
                            })
                        }
                        DropdownMenuItem(text = { Text(t("حذف الدفعة", "Delete batch"), color = Danger) }, onClick = { menu = false; confirmDelete = true })
                    }
                }
            },
        ) { pad ->
            ListBody(pad) {
                item {
                    AppCard(modifier = Modifier.shared("batch-$id"), animateIn = false) {
                        if (b.name.isNotBlank()) Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(herbSummary(s), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        StageDots(s, Modifier.padding(vertical = 10.dp))
                        KeyValue(t("تاريخ البدء", "Started"), date(b.startDate))
                        val end = b.finishedDate ?: LocalDate.now()
                        KeyValue(
                            if (b.finishedDate != null) t("المدة الكلية", "Total time") else t("مضى عليها", "Running for"),
                            days(ChronoUnit.DAYS.between(b.startDate, end)),
                        )
                        KeyValue(
                            t("الحالة", "Status"),
                            when (b.status) {
                                BatchStatus.DONE -> t("منتهية في ", "Finished on ") + date(b.finishedDate)
                                BatchStatus.CANCELLED -> t("ملغاة", "Cancelled")
                                else -> t("جارية", "In progress")
                            },
                        )
                        if (b.notes.isNotBlank()) {
                            Divider()
                            Text(b.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // The one thing to do next.
                if (active) item {
                    val next = s.pending.firstOrNull()
                    if (next != null) Button(onClick = { advancing = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(
                            if (cur != null) t("الانتقال إلى المرحلة التالية", "Move to the next stage")
                            else t("بدء المرحلة: ", "Start stage: ") + next.name,
                        )
                    } else Button(onClick = { finishing = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(t("إنهاء الدفعة (جاهزة للبيع)", "Finish batch (ready to sell)"))
                    }
                }

                item { SectionTitle(t("المراحل", "Stages")) }
                item {
                    AppCard {
                        StageTimeline(
                            s.stages.sortedBy { it.position },
                            items.groupBy { it.stageId }.mapValues { (_, l) -> l.sumOf { it.cost } },
                            s.progress,
                        )
                        TextButton(onClick = { addingStage = true }) { Text(t("+ إضافة مرحلة", "+ Add stage")) }
                    }
                }

                // Herbs: offered once the batch reaches its last stage.
                if (s.atLastStage || s.herbs.isNotEmpty()) {
                    item { SectionTitle(t("الأعشاب", "Herbs")) }
                    item {
                        HerbsCard(
                            s,
                            onAdd = { addingHerb = BatchHerb(batchId = id, herb = Herb.GINGER, liters = 0.0, date = LocalDate.now()) },
                            onEdit = { addingHerb = it },
                        )
                    }
                }

                item {
                    StatRow(
                        Triple(t("التكلفة", "Cost"), money(s.totalCost), null),
                        Triple(t("القوارير", "Bottles"), s.bottles.toString(), null),
                        Triple(t("تكلفة القارورة", "Per bottle"), s.costPerBottle?.let { money(it) } ?: "—", MaterialTheme.colorScheme.secondary),
                    )
                }

                if (items.isNotEmpty()) {
                    item { SectionTitle(t("التكاليف حسب البند", "Costs by item")) }
                    item {
                        AppCard {
                            items.groupBy { it.kind }.forEach { (k, l) -> KeyValue(itemKindName(k), money(l.sumOf { it.cost })) }
                            Divider()
                            KeyValue(t("الإجمالي", "Total"), money(s.totalCost), bold = true)
                        }
                    }
                }

                item { SectionTitle(t("القوارير المنتجة", "Bottles made")) }
                item {
                    AppCard {
                        if (outputs.isEmpty()) Text(
                            t("عند التعبئة سجّل عدد القوارير لكل منتج، فتُضاف إلى المخزون.", "When bottling, record how many bottles of each product; they are added to stock."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        outputs.forEach { o ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${products[o.productId]?.title ?: "?"} — ${o.qty}", Modifier.weight(1f))
                                Text(date(o.date), style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { Repo.deleteOutput(o) }) { Ic(R.drawable.ic_delete, Modifier.size(18.dp)) }
                            }
                        }
                        TextButton(onClick = { addingOutput = true }) { Text(t("+ تسجيل قوارير", "+ Record bottles")) }
                    }
                }

                if (active && s.pending.isNotEmpty()) item {
                    OutlinedButton(onClick = { finishing = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("إنهاء الدفعة الآن", "Finish the batch now"))
                    }
                }
            }
        }

        Celebration(
            celebrateNew,
            t("تم تجهيز دفعة جديدة!", "A new batch is ready!"),
            b.label + " · " + (s.current?.name ?: ""),
            big = true,
        ) { celebrateNew = false }
        Celebration(
            celebrateStage != null,
            t("المرحلة التالية", "Next stage"),
            celebrateStage ?: "",
            big = false,
        ) { celebrateStage = null }
        Celebration(
            celebrateDone,
            t("الدفعة جاهزة للبيع!", "The batch is ready to sell!"),
            b.label + if (s.bottles > 0) t(" · ${s.bottles} قارورة", " · ${s.bottles} bottles") else "",
            big = true,
        ) { celebrateDone = false }
    }

    if (advancing) AdvanceSheet(s, onDismiss = { advancing = false }) { next, d, check ->
        val nowLast = s.pending.count { it.id != next.id } == 0
        Repo.advanceStage(id, cur, next, d, check)
        advancing = false
        celebrateStage = next.name + if (nowLast) t("\nيمكنك الآن إضافة الأعشاب لجزء من الدفعة.", "\nYou can now add herbs to part of the batch.") else ""
    }
    if (editing) EditBatchDialog(b) { editing = false }
    if (confirmDelete) ConfirmDialog(
        t("حذف الدفعة؟", "Delete batch?"),
        t("ستُحذف المراحل والتكاليف والصور، وتُطرح القوارير المنتجة من المخزون.", "Its stages, costs and photos are deleted, and its bottles are removed from stock."),
        t("حذف", "Delete"), { Repo.deleteBatch(id); Nav.back() }, { confirmDelete = false },
    )
    if (addingOutput) AddOutputDialog(s) { addingOutput = false }
    if (addingStage) AddStageDialog(b.id, s.stages) { addingStage = false }
    addingHerb?.let { h -> HerbPortionDialog(h, s) { addingHerb = null } }
    if (finishing) FinishDialog(s, outputs.isEmpty(), onAddBottles = { finishing = false; addingOutput = true }) {
        finishing = false
        celebrateDone = it
    }
}

private fun qrText(s: BatchSummary, products: List<String>): String = buildString {
    appendLine(t("جودة الحياة — خل التمر الطبيعي", "Jawdat Al-Hayah — natural date vinegar"))
    appendLine(t("دفعة: ", "Batch: ") + ltr(s.batch.code))
    appendLine(t("النوع: ", "Type: ") + herbSummary(s))
    if (products.isNotEmpty()) appendLine(t("المنتج: ", "Product: ") + products.joinToString(", "))
    appendLine(t("بدء التحضير: ", "Started: ") + date(s.batch.startDate))
    s.stages.lastOrNull { it.doneDate != null }?.let { appendLine(t("آخر مرحلة: ", "Last stage: ") + it.name + " " + date(it.doneDate)) }
    s.batch.finishedDate?.let { appendLine(t("تاريخ التعبئة: ", "Bottled: ") + date(it)) }
    append("fb.com/jwdtalhya")
}

// ---------- Stage timeline ----------

/**
 * The stages top to bottom on a line that fills up as the batch moves on: done stages carry a check,
 * the current one pulses, waiting ones are hollow with their number.
 */
@Composable
private fun StageTimeline(stages: List<Stage>, costs: Map<Long, Double>, progress: Float) {
    val reach = remember { Animatable(0f) }
    LaunchedEffect(progress) { reach.animateTo(progress * stages.size, tween(1100, easing = FastOutSlowInEasing)) }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "p",
    )
    val lineOn = MaterialTheme.colorScheme.primary
    val lineOff = MaterialTheme.colorScheme.outlineVariant
    stages.forEachIndexed { i, st ->
        Reveal(delayMs = i * 70) {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(12.dp))
                    .clickable { Nav.go(Screen.StageDetail(st.id)) },
            ) {
                Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    // The connecting line, coloured as far as the batch has come.
                    Canvas(Modifier.fillMaxSize()) {
                        val x = size.width / 2
                        val top = if (i == 0) 20.dp.toPx() else 0f
                        val bottom = if (i == stages.lastIndex) 20.dp.toPx() else size.height
                        val f = (reach.value - i).coerceIn(0f, 1f)
                        drawLine(lineOff, Offset(x, top), Offset(x, bottom), 3.dp.toPx(), StrokeCap.Round)
                        if (f > 0f && bottom > top) drawLine(lineOn, Offset(x, top), Offset(x, top + (bottom - top) * f), 3.dp.toPx(), StrokeCap.Round)
                    }
                    Box(Modifier.padding(top = 8.dp).size(24.dp), contentAlignment = Alignment.Center) {
                        when (st.status) {
                            StageStatus.DONE -> Box(Modifier.size(24.dp).clip(CircleShape).background(Good), contentAlignment = Alignment.Center) {
                                Ic(R.drawable.ic_check, Modifier.size(16.dp), Color.White)
                            }
                            StageStatus.ACTIVE -> {
                                Box(
                                    Modifier.size(24.dp).graphicsLayer {
                                        val k = 1f + pulse * 0.8f
                                        scaleX = k
                                        scaleY = k
                                        alpha = 1f - pulse
                                    }.clip(CircleShape).background(lineOn.copy(alpha = 0.4f))
                                )
                                Box(Modifier.size(20.dp).clip(CircleShape).background(lineOn))
                            }
                            else -> Box(
                                Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                    .border(2.dp, lineOff, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                StageInfo(st, costs[st.id] ?: 0.0, Modifier.weight(1f).padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun StageInfo(st: Stage, cost: Double, modifier: Modifier) {
    val color = when (st.status) {
        StageStatus.DONE -> Good
        StageStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                st.name, Modifier.weight(1f),
                style = if (st.status == StageStatus.ACTIVE) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (st.status == StageStatus.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(stageStatusName(st.status), style = MaterialTheme.typography.labelMedium, color = color)
        }
        if (st.status == StageStatus.ACTIVE) Prefs.stageHint(st.name)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        val parts = buildList {
            st.startedDate?.let { add(t("بدأت ", "Started ") + date(it)) }
            if (st.status != StageStatus.DONE) st.checkDate?.let { add(t("المتابعة ", "Check ") + date(it) + " (" + relative(it) + ")") }
            st.doneDate?.let { add(t("تمت ", "Done ") + date(it)) }
            if (st.startedDate != null && st.doneDate != null) add(days(ChronoUnit.DAYS.between(st.startedDate, st.doneDate)))
            if (cost > 0) add(money(cost))
        }
        if (parts.isNotEmpty()) Text(
            parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
            color = if (st.status != StageStatus.DONE && st.checkDate?.isBefore(LocalDate.now()) == true) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ---------- Moving to the next stage ----------

/**
 * Finishes the current stage and starts the next. The next one is the first waiting stage unless
 * another is picked, or a new one typed; a check date can be set right away.
 */
@Composable
private fun AdvanceSheet(s: BatchSummary, onDismiss: () -> Unit, onConfirm: (Stage, LocalDate, LocalDate?) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cur = s.current?.takeIf { it.status == StageStatus.ACTIVE }
    var choice by remember { mutableStateOf(s.pending.firstOrNull()?.id ?: 0L) }
    var custom by remember { mutableStateOf("") }
    var d by remember { mutableStateOf(LocalDate.now()) }
    val chosenName = s.pending.firstOrNull { it.id == choice }?.name ?: custom
    var checkDays by remember(chosenName) { mutableStateOf(Prefs.checkDaysFor(chosenName)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(t("الانتقال إلى المرحلة التالية", "Move to the next stage"), style = MaterialTheme.typography.titleLarge)
            if (cur != null) Text(
                t("تنتهي «${cur.name}» وتبدأ:", "\"${cur.name}\" ends and this one starts:"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            s.pending.forEachIndexed { i, st ->
                ChoiceRow(choice == st.id, st.name, Prefs.stageHint(st.name), if (i == 0) t("التالية بالترتيب", "Next in order") else null) { choice = st.id }
            }
            ChoiceRow(choice == 0L, t("مرحلة أخرى…", "Another stage…"), null, null) { choice = 0L }
            AnimatedVisibility(choice == 0L, enter = fadeIn() + expandVertically()) {
                Field(t("اسم المرحلة", "Stage name"), custom, { custom = it })
            }
            DateField(t("تاريخ البدء", "Starts on"), d, { it?.let { x -> d = x } })
            Text(t("موعد المتابعة", "Check on"), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                listOf(0 to t("بدون", "None"), 3 to ltr("+3"), 5 to ltr("+5"), 7 to ltr("+7"), 14 to ltr("+14"), 30 to ltr("+30")),
                checkDays,
            ) { checkDays = it }
            if (checkDays > 0) Text(
                t("سيصلك تذكير يوم ", "You'll get a reminder on ") + date(d.plusDays(checkDays.toLong())),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                enabled = choice != 0L || custom.isNotBlank(),
                onClick = {
                    val next = s.pending.firstOrNull { it.id == choice } ?: Stage(batchId = s.batch.id, position = 0, name = custom.trim())
                    onConfirm(next, d, if (checkDays > 0) d.plusDays(checkDays.toLong()) else null)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(t("انتقال", "Move on")) }
        }
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, title: String, hint: String?, tag: String?, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        tag?.let { Badge(it, MaterialTheme.colorScheme.primary) }
    }
}

// ---------- Herbs at the last stage ----------

/**
 * The batch's vinegar as a bar: each herb's litres in its colour, the rest plain. The total litres
 * can be typed here; without it the bar shows only the herb portions.
 */
@Composable
private fun HerbsCard(s: BatchSummary, onAdd: () -> Unit, onEdit: (BatchHerb) -> Unit) {
    val b = s.batch
    var total by remember(b.id) { mutableStateOf(if (b.liters > 0) num(b.liters) else "") }
    val totalL = parseNum(total) ?: 0.0
    val herbL = s.herbLiters
    val plain = (totalL - herbL).coerceAtLeast(0.0)
    val whole = maxOf(totalL, herbL)
    AppCard {
        Text(
            t("يمكن وضع جزء من الدفعة مع الأعشاب، مثل 10 لتر مع الزنجبيل و10 لتر مع الزعتر. الباقي يبقى طبيعياً.", "Put part of the batch with herbs, e.g. 10 L with ginger and 10 L with thyme. The rest stays plain."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Field(
            t("كمية الخل في الدفعة (لتر)", "Vinegar in the batch (L)"), total,
            { total = it; Repo.updateBatch(b.copy(liters = parseNum(it) ?: 0.0)) }, number = true,
        )
        if (whole > 0) {
            Spacer(Modifier.height(10.dp))
            LitresBar(s.herbs, plain, whole)
            Spacer(Modifier.height(6.dp))
        }
        s.herbs.forEach { h ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onEdit(h) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(herbColor(h.herb)))
                Text("  " + herbName(h.herb), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(liters(h.liters), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (totalL > 0) Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Vinegar))
            Text("  " + t("طبيعي (الباقي)", "Plain (the rest)"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(liters(plain), style = MaterialTheme.typography.titleSmall)
        }
        if (totalL > 0 && herbL > totalL) Text(
            t("مجموع الأعشاب أكبر من كمية الدفعة.", "The herbs add up to more than the batch."),
            color = Warn, style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onAdd) { Text(t("+ إضافة أعشاب لجزء من الدفعة", "+ Add herbs to part of the batch")) }
    }
}

/** The litres bar; its pieces grow in from the start when it appears or changes. */
@Composable
private fun LitresBar(herbs: List<BatchHerb>, plain: Double, whole: Double) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(herbs.size, plain, whole) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(13.dp))) {
        drawRect(track)
        val rtl = layoutDirection == LayoutDirection.Rtl
        var at = 0f
        val pieces = herbs.map { herbColor(it.herb) to it.liters } + (Vinegar to plain)
        val gap = 2.dp.toPx()
        pieces.filter { it.second > 0 }.forEach { (c, l) ->
            val w = (l / whole).toFloat() * size.width * grow.value
            val x = if (rtl) size.width - at - w else at
            // A thin gap between pieces keeps neighbouring colours apart.
            drawRoundRect(c, Offset(x + gap / 2, 0f), Size((w - gap).coerceAtLeast(0f), size.height), CornerRadius(0f))
            at += w
        }
    }
}

@Composable
private fun HerbPortionDialog(initial: BatchHerb, s: BatchSummary, onDone: () -> Unit) {
    var herb by remember { mutableStateOf(initial.herb) }
    var litres by remember { mutableStateOf(if (initial.liters > 0) num(initial.liters) else "") }
    var d by remember { mutableStateOf(initial.date) }
    var notes by remember { mutableStateOf(initial.notes) }
    val l = parseNum(litres) ?: 0.0
    val left = if (s.batch.liters > 0) s.batch.liters - s.herbLiters + initial.liters else null
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (initial.id == 0L) t("أعشاب لجزء من الدفعة", "Herbs for part of the batch") else t("تعديل الأعشاب", "Edit herbs")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HerbPicker(herb, options = Herb.all - Herb.PLAIN) { herb = it }
                Field(t("الكمية (لتر)", "Amount (L)"), litres, { litres = it }, number = true)
                ChipRow(listOf(5, 10, 15, 20).map { it to t("${ltr(it.toString())} لتر", "$it L") }, l.toInt()) { litres = it.toString() }
                left?.let {
                    Text(
                        t("المتبقي طبيعياً: ", "Left plain: ") + liters((it - l).coerceAtLeast(0.0)),
                        style = MaterialTheme.typography.bodySmall, color = if (l > it) Warn else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
                Field(t("ملاحظات", "Notes"), notes, { notes = it })
            }
        },
        confirmButton = {
            TextButton(enabled = l > 0, onClick = { Repo.saveBatchHerb(initial.copy(herb = herb, liters = l, date = d, notes = notes)); onDone() }) {
                Text(t("حفظ", "Save"))
            }
        },
        dismissButton = {
            Row {
                if (initial.id != 0L) TextButton(onClick = { Repo.deleteBatchHerb(initial.id); onDone() }) { Text(t("حذف", "Delete"), color = Danger) }
                TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) }
            }
        },
    )
}

// ---------- Dialogs ----------

@Composable
private fun EditBatchDialog(b: Batch, onDone: () -> Unit) {
    var code by remember { mutableStateOf(b.code) }
    var name by remember { mutableStateOf(b.name) }
    var start by remember { mutableStateOf(b.startDate) }
    var notes by remember { mutableStateOf(b.notes) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("تعديل الدفعة", "Edit batch")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("رقم الدفعة", "Batch number"), code, { code = it })
                Field(t("الاسم", "Name"), name, { name = it })
                DateField(t("تاريخ البدء", "Start date"), start, { it?.let { d -> start = d } })
                Field(t("ملاحظات", "Notes"), notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { Repo.updateBatch(b.copy(code = code.trim().ifBlank { b.code }, name = name, startDate = start, notes = notes)); onDone() }) {
                Text(t("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

@Composable
private fun AddOutputDialog(s: BatchSummary, onDone: () -> Unit) {
    val products = watch { Repo.products() }
    // Suggest a product for a herb added to this batch, else a plain one.
    val firstHerb = s.herbs.firstOrNull()?.herb
    var productId by remember {
        mutableStateOf(
            products.firstOrNull { it.herb == firstHerb }?.id ?: products.firstOrNull { it.herb == Herb.PLAIN }?.id ?: products.firstOrNull()?.id
        )
    }
    var qty by remember { mutableStateOf("") }
    var d by remember { mutableStateOf(LocalDate.now()) }
    var addingProduct by remember { mutableStateOf(false) }
    if (addingProduct) {
        QuickProductDialog(firstHerb ?: Herb.PLAIN, onDone = { addingProduct = false }) { productId = it }
        return
    }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("تسجيل قوارير منتجة", "Record bottles made")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (products.isEmpty()) {
                    Text(t("لا توجد منتجات بعد. أضف المنتج هنا.", "No products yet. Add the product here."))
                } else {
                    PickField(
                        t("المنتج", "Product"), products.map { it.id to it.title }, productId, { productId = it },
                        footer = { close -> TextButton(onClick = { close(); addingProduct = true }) { Text(t("+ منتج جديد", "+ New product")) } },
                    )
                    QtyStepper(
                        t("عدد القوارير", "Number of bottles"), qty, { qty = it },
                        bottle = products.firstOrNull { it.id == productId }?.size ?: ProductSize.SMALL,
                    )
                    DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
                }
            }
        },
        confirmButton = {
            val n = parseNum(qty)?.toInt() ?: 0
            TextButton(enabled = productId != null && n > 0, onClick = {
                Repo.addOutput(BatchOutput(batchId = s.batch.id, productId = productId!!, qty = n, date = d)); onDone()
            }) { Text(t("حفظ", "Save")) }
        },
        dismissButton = {
            if (products.isEmpty()) TextButton(onClick = { addingProduct = true }) { Text(t("+ منتج جديد", "+ New product")) }
            else TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) }
        },
    )
}

@Composable
private fun AddStageDialog(batchId: Long, existing: List<Stage>, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("مرحلة جديدة", "New stage")) },
        text = { Field(t("اسم المرحلة", "Stage name"), name, { name = it }) },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val pos = (existing.maxOfOrNull { it.position } ?: -1) + 1
                // Added at the end, waiting; "next stage" starts it when its turn comes.
                Repo.saveStage(Stage(batchId = batchId, position = pos, name = name, status = StageStatus.PENDING))
                onDone()
            }) { Text(t("إضافة", "Add")) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

/** Finishes the batch; [onDone] gets true when it was finished (to celebrate). */
@Composable
private fun FinishDialog(s: BatchSummary, noBottles: Boolean, onAddBottles: () -> Unit, onDone: (Boolean) -> Unit) {
    var d by remember { mutableStateOf(LocalDate.now()) }
    AlertDialog(
        onDismissRequest = { onDone(false) },
        title = { Text(t("إنهاء الدفعة", "Finish batch")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("ستُعلَّم المراحل المتبقية كمنتهية.", "Any remaining stages will be marked done."))
                if (noBottles) Text(
                    t("لم تسجّل عدد القوارير بعد. سجّلها حتى تظهر في المخزون وتُحسب تكلفة القارورة.", "You haven't recorded bottles yet. Record them so they show in stock and the cost per bottle is known."),
                    color = Warn,
                )
                DateField(t("تاريخ الانتهاء", "Finish date"), d, { it?.let { x -> d = x } })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                s.stages.filter { it.status == StageStatus.ACTIVE || it.status == StageStatus.PENDING }.forEach {
                    Repo.saveStage(it.copy(status = StageStatus.DONE, startedDate = it.startedDate ?: d, doneDate = d))
                }
                Repo.updateBatch(s.batch.copy(status = BatchStatus.DONE, finishedDate = d))
                onDone(true)
            }) { Text(t("إنهاء", "Finish")) }
        },
        dismissButton = {
            if (noBottles) TextButton(onClick = onAddBottles) { Text(t("تسجيل القوارير", "Record bottles")) }
            else TextButton(onClick = { onDone(false) }) { Text(t("إلغاء", "Cancel")) }
        },
    )
}

/** Colour for a check date: red when late, orange today. */
@Composable
fun checkColor(d: LocalDate?): Color = when {
    d == null -> MaterialTheme.colorScheme.onSurfaceVariant
    d.isBefore(LocalDate.now()) -> Danger
    d == LocalDate.now() -> Warn
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
