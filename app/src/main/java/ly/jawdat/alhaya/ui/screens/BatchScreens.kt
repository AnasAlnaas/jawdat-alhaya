package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Batch
import ly.jawdat.alhaya.data.BatchOutput
import ly.jawdat.alhaya.data.BatchStatus
import ly.jawdat.alhaya.data.BatchSummary
import ly.jawdat.alhaya.data.Herb
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.Stage
import ly.jawdat.alhaya.data.StageStatus
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.Badge
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.DateField
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.Divider
import ly.jawdat.alhaya.ui.EmptyState
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.FormBody
import ly.jawdat.alhaya.ui.Good
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.KeyValue
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.PickField
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.days
import ly.jawdat.alhaya.ui.herbName
import ly.jawdat.alhaya.ui.itemKindName
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.relative
import ly.jawdat.alhaya.ui.stageStatusName
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.ltr
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val herbOptions: List<Pair<String, String>> get() = Herb.all.map { it to herbName(it) }

// ---------- List ----------

@Composable
fun BatchesScreen() {
    var filter by rememberSaveable { mutableStateOf(BatchStatus.ACTIVE) }
    val all = watch { Repo.batchSummaries() }
    val shown = all.filter { filter == "all" || it.batch.status == filter }
    AppScreen(
        title = t("الإنتاج", "Production"),
        help = GuideIds.BATCHES,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { Nav.go(Screen.NewBatch) },
                icon = { Ic(R.drawable.ic_add) },
                text = { Text(t("دفعة جديدة", "New batch")) },
            )
        },
    ) { pad ->
        ListBody(pad) {
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
            items(shown, key = { it.batch.id }) { BatchCard(it) }
        }
    }
}

@Composable
fun BatchCard(s: BatchSummary) {
    val cur = s.current
    AppCard(onClick = { Nav.go(Screen.BatchDetail(s.batch.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    listOf(s.batch.label, s.batch.name).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(herbName(s.batch.herb), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            when (s.batch.status) {
                BatchStatus.DONE -> Badge(t("منتهية", "Finished"), Good)
                BatchStatus.CANCELLED -> Badge(t("ملغاة", "Cancelled"), Danger)
                else -> Badge("${s.doneCount}/${s.stages.size}", MaterialTheme.colorScheme.primary)
            }
        }
        if (s.batch.status == BatchStatus.ACTIVE && cur != null) {
            Divider()
            Text(t("المرحلة الحالية: ", "Current stage: ") + cur.name, style = MaterialTheme.typography.bodyMedium)
            cur.checkDate?.let { d ->
                val late = d.isBefore(LocalDate.now())
                val today = d == LocalDate.now()
                Text(
                    t("المتابعة: ", "Check on: ") + date(d) + " (" + relative(d) + ")",
                    color = when {
                        late -> Danger
                        today -> Warn
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(Modifier.padding(top = 6.dp)) {
            Text(t("بدأت ", "Started ") + date(s.batch.startDate), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(t("التكلفة ", "Cost ") + money(s.totalCost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var herb by rememberSaveable { mutableStateOf(Herb.PLAIN) }
    var notes by rememberSaveable { mutableStateOf("") }
    val stages = remember { mutableStateListOf<Pair<String, Boolean>>().apply { addAll(Prefs.stageTemplate.map { it to true }) } }
    var newStage by remember { mutableStateOf("") }

    AppScreen(t("دفعة جديدة", "New batch"), onBack = { Nav.back() }, help = GuideIds.BATCHES) { pad ->
        FormBody(pad) {
            Field(t("رقم الدفعة", "Batch number"), code, { code = it })
            Field(t("اسم أو وصف (اختياري)", "Name or note (optional)"), name, { name = it }, supporting = t("مثال: تمر دقلة 20 كجم", "e.g. Deglet dates 20 kg"))
            PickField(t("النوع", "Type"), herbOptions, herb, { h ->
                herb = h
                // Herb types get the herb-soaking stage just before bottling; plain vinegar doesn't.
                val i = stages.indexOfFirst { it.first == Prefs.herbStage }
                if (h != Herb.PLAIN && i < 0) stages.add((stages.size - 1).coerceAtLeast(0), Prefs.herbStage to true)
                if (h == Herb.PLAIN && i >= 0) stages.removeAt(i)
            })
            DateField(t("تاريخ البدء", "Start date"), start, { it?.let { d -> start = d } })
            Field(t("ملاحظات", "Notes"), notes, { notes = it }, singleLine = false)

            SectionTitle(t("المراحل", "Stages"))
            Text(
                t("اختر المراحل لهذه الدفعة. يمكنك تعديلها لاحقاً.", "Choose the stages for this batch. You can change them later."),
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
                        Batch(code = code.trim().ifBlank { Repo.nextBatchCode(start) }, name = name, herb = herb, startDate = start, notes = notes),
                        stages.filter { it.second }.map { it.first },
                    )
                    Nav.replace(Screen.BatchDetail(id))
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

    AppScreen(
        title = b.label,
        help = GuideIds.BATCHES,
        onBack = { Nav.back() },
        actions = {
            IconButton(onClick = {
                val qr = qrText(s, products.values.filter { p -> outputs.any { it.productId == p.id } }.map { it.name })
                Files.shareQrLabel(
                    context, qr,
                    listOf(t("جودة الحياة", "Jawdat Al-Hayah"), b.label + " · " + herbName(b.herb)),
                )
            }) { Ic(R.drawable.ic_qr) }
            Box {
                IconButton(onClick = { menu = true }) { Ic(R.drawable.ic_settings) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(t("تعديل البيانات", "Edit details")) }, onClick = { menu = false; editing = true })
                    if (b.status == BatchStatus.ACTIVE) {
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
                AppCard {
                    if (b.name.isNotBlank()) Text(b.name, style = MaterialTheme.typography.titleMedium)
                    KeyValue(t("النوع", "Type"), herbName(b.herb))
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
            item {
                StatRow(
                    Triple(t("التكلفة", "Cost"), money(s.totalCost), null),
                    Triple(t("القوارير", "Bottles"), s.bottles.toString(), null),
                    Triple(t("تكلفة القارورة", "Per bottle"), s.costPerBottle?.let { money(it) } ?: "—", MaterialTheme.colorScheme.secondary),
                )
            }

            item { SectionTitle(t("المراحل", "Stages")) }
            items(s.stages, key = { it.id }) { st -> StageRow(st, items.filter { it.stageId == st.id }.sumOf { it.cost }) }
            item {
                OutlinedButton(onClick = { addingStage = true }, modifier = Modifier.fillMaxWidth()) {
                    Ic(R.drawable.ic_add, Modifier.size(18.dp)); Text("  " + t("إضافة مرحلة", "Add stage"))
                }
            }

            if (items.isNotEmpty()) {
                item { SectionTitle(t("التكاليف حسب البند", "Costs by item")) }
                item {
                    AppCard {
                        items.groupBy { it.kind }.forEach { (k, l) ->
                            KeyValue(itemKindName(k), money(l.sumOf { it.cost }))
                        }
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
                            Text("${products[o.productId]?.name ?: "?"} — ${o.qty}", Modifier.weight(1f))
                            Text(date(o.date), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { Repo.deleteOutput(o) }) { Ic(R.drawable.ic_delete, Modifier.size(18.dp)) }
                        }
                    }
                    TextButton(onClick = { addingOutput = true }) { Text(t("+ تسجيل قوارير", "+ Record bottles")) }
                }
            }

            if (b.status == BatchStatus.ACTIVE) item {
                Button(onClick = { finishing = true }, modifier = Modifier.fillMaxWidth()) { Text(t("إنهاء الدفعة (جاهزة للبيع)", "Finish batch (ready to sell)")) }
            }
        }
    }

    if (editing) EditBatchDialog(b) { editing = false }
    if (confirmDelete) ConfirmDialog(
        t("حذف الدفعة؟", "Delete batch?"),
        t("ستُحذف المراحل والتكاليف والصور، وتُطرح القوارير المنتجة من المخزون.", "Its stages, costs and photos are deleted, and its bottles are removed from stock."),
        t("حذف", "Delete"), { Repo.deleteBatch(id); Nav.back() }, { confirmDelete = false },
    )
    if (addingOutput) AddOutputDialog(b) { addingOutput = false }
    if (addingStage) AddStageDialog(b.id, s.stages) { addingStage = false }
    if (finishing) FinishDialog(s, outputs.isEmpty(), onAddBottles = { finishing = false; addingOutput = true }) { finishing = false }
}

private fun qrText(s: BatchSummary, products: List<String>): String = buildString {
    appendLine(t("جودة الحياة — خل التمر الطبيعي", "Jawdat Al-Hayah — natural date vinegar"))
    appendLine(t("دفعة: ", "Batch: ") + ltr(s.batch.code))
    appendLine(t("النوع: ", "Type: ") + herbName(s.batch.herb))
    if (products.isNotEmpty()) appendLine(t("المنتج: ", "Product: ") + products.joinToString(", "))
    appendLine(t("بدء التحضير: ", "Started: ") + date(s.batch.startDate))
    s.stages.lastOrNull { it.doneDate != null }?.let { appendLine(t("آخر مرحلة: ", "Last stage: ") + it.name + " " + date(it.doneDate)) }
    s.batch.finishedDate?.let { appendLine(t("تاريخ التعبئة: ", "Bottled: ") + date(it)) }
    append("fb.com/jwdtalhya")
}

@Composable
private fun StageRow(st: Stage, cost: Double) {
    val color = when (st.status) {
        StageStatus.DONE -> Good
        StageStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        StageStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    AppCard(onClick = { Nav.go(Screen.StageDetail(st.id)) }, color = if (st.status == StageStatus.ACTIVE) MaterialTheme.colorScheme.primaryContainer else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(color, CircleShape))
            Text("  " + st.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EditBatchDialog(b: Batch, onDone: () -> Unit) {
    var code by remember { mutableStateOf(b.code) }
    var name by remember { mutableStateOf(b.name) }
    var herb by remember { mutableStateOf(b.herb) }
    var start by remember { mutableStateOf(b.startDate) }
    var notes by remember { mutableStateOf(b.notes) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("تعديل الدفعة", "Edit batch")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("رقم الدفعة", "Batch number"), code, { code = it })
                Field(t("الاسم", "Name"), name, { name = it })
                PickField(t("النوع", "Type"), herbOptions, herb, { herb = it })
                DateField(t("تاريخ البدء", "Start date"), start, { it?.let { d -> start = d } })
                Field(t("ملاحظات", "Notes"), notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { Repo.updateBatch(b.copy(code = code.trim().ifBlank { b.code }, name = name, herb = herb, startDate = start, notes = notes)); onDone() }) {
                Text(t("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

@Composable
private fun AddOutputDialog(b: Batch, onDone: () -> Unit) {
    val products = watch { Repo.products() }
    var productId by remember { mutableStateOf(products.firstOrNull { it.herb == b.herb }?.id ?: products.firstOrNull()?.id) }
    var qty by remember { mutableStateOf("") }
    var d by remember { mutableStateOf(LocalDate.now()) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("تسجيل قوارير منتجة", "Record bottles made")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (products.isEmpty()) {
                    Text(t("أضف منتجاً أولاً (المزيد ← المنتجات والأسعار).", "Add a product first (More → Products & prices)."))
                } else {
                    PickField(t("المنتج", "Product"), products.map { it.id to "${it.name} ${it.size}".trim() }, productId, { productId = it })
                    Field(t("عدد القوارير", "Number of bottles"), qty, { qty = it.filter { c -> c.isDigit() || c in '٠'..'٩' } }, number = true)
                    DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
                }
            }
        },
        confirmButton = {
            val n = ly.jawdat.alhaya.ui.parseNum(qty)?.toInt() ?: 0
            TextButton(enabled = productId != null && n > 0, onClick = {
                Repo.addOutput(BatchOutput(batchId = b.id, productId = productId!!, qty = n, date = d)); onDone()
            }) { Text(t("حفظ", "Save")) }
        },
        dismissButton = {
            if (products.isEmpty()) TextButton(onClick = { onDone(); Nav.go(Screen.ProductEdit()) }) { Text(t("إضافة منتج", "Add product")) }
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
                val anyActive = existing.any { it.status == StageStatus.ACTIVE }
                Repo.saveStage(
                    Stage(
                        batchId = batchId, position = pos, name = name,
                        status = if (anyActive || existing.any { it.status == StageStatus.PENDING }) StageStatus.PENDING else StageStatus.ACTIVE,
                        startedDate = if (anyActive) null else LocalDate.now(),
                    )
                )
                onDone()
            }) { Text(t("إضافة", "Add")) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

@Composable
private fun FinishDialog(s: BatchSummary, noBottles: Boolean, onAddBottles: () -> Unit, onDone: () -> Unit) {
    var d by remember { mutableStateOf(LocalDate.now()) }
    AlertDialog(
        onDismissRequest = onDone,
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
                onDone()
            }) { Text(t("إنهاء", "Finish")) }
        },
        dismissButton = {
            if (noBottles) TextButton(onClick = onAddBottles) { Text(t("تسجيل القوارير", "Record bottles")) }
            else TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) }
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
