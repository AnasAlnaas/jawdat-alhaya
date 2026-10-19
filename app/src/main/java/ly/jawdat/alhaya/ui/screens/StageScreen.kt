package ly.jawdat.alhaya.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.ItemKind
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.StageItem
import ly.jawdat.alhaya.data.StagePhoto
import ly.jawdat.alhaya.data.StageStatus
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.DateField
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.Divider
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.FormBody
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.KeyValue
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.PickField
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.commonUnits
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.days
import ly.jawdat.alhaya.ui.itemKindName
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.stageStatusName
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun StageDetailScreen(id: Long) {
    val context = LocalContext.current
    val st = watch(id) { Repo.stage(id) } ?: return
    val batch = watch(st.batchId) { Repo.batch(st.batchId) }
    val items = watch(id) { Repo.stageItems(id) }
    val photos = watch(id) { Repo.photos(id) }
    // Text fields keep their own state and save as you type; everything else saves on tap.
    var name by remember(id) { mutableStateOf(st.name) }
    var notes by remember(id) { mutableStateOf(st.notes) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<StageItem?>(null) }
    var viewPhoto by remember { mutableStateOf<StagePhoto?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { Files.importPhoto(context, it)?.let { f -> Repo.addPhoto(id, f) } }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null) {
            Files.importPhoto(context, Uri.fromFile(f))?.let { name -> Repo.addPhoto(id, name) }
        }
        f?.delete()
    }

    AppScreen(
        title = st.name,
        help = GuideIds.STAGES,
        onBack = { Nav.back() },
        actions = {
            IconButton(onClick = { menu = true }) { Ic(R.drawable.ic_settings) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(t("نقل للأعلى", "Move up")) }, onClick = { menu = false; Repo.moveStage(st, true) })
                DropdownMenuItem(text = { Text(t("نقل للأسفل", "Move down")) }, onClick = { menu = false; Repo.moveStage(st, false) })
                DropdownMenuItem(text = { Text(t("حذف المرحلة", "Delete stage"), color = Danger) }, onClick = { menu = false; confirmDelete = true })
            }
        },
    ) { pad ->
        FormBody(pad) {
            batch?.let { Text(it.label, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge) }
            Field(t("اسم المرحلة", "Stage name"), name, { name = it; if (it.isNotBlank()) Repo.saveStage(st.copy(name = it)) })
            ly.jawdat.alhaya.data.Prefs.stageHint(st.name)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            ChipRow(
                listOf(StageStatus.PENDING, StageStatus.ACTIVE, StageStatus.DONE, StageStatus.SKIPPED).map { it to stageStatusName(it) },
                st.status,
            ) { s ->
                val today = LocalDate.now()
                Repo.saveStage(
                    st.copy(
                        status = s,
                        startedDate = if (s == StageStatus.ACTIVE || s == StageStatus.DONE) st.startedDate ?: today else st.startedDate,
                        doneDate = if (s == StageStatus.DONE) st.doneDate ?: today else null,
                    )
                )
            }

            DateField(t("تاريخ البدء", "Started on"), st.startedDate, { Repo.saveStage(st.copy(startedDate = it)) }, clearable = true)
            DateField(t("موعد المتابعة (متى تكون جاهزة؟)", "Check on (when should it be ready?)"), st.checkDate, { Repo.saveStage(st.copy(checkDate = it)) }, clearable = true)
            if (st.startedDate != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 21, 30).forEach { n ->
                        SuggestionChip(onClick = { Repo.saveStage(st.copy(checkDate = st.startedDate.plusDays(n.toLong()))) }, label = { Text(ly.jawdat.alhaya.ui.ltr("+$n")) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("تذكير في موعد المتابعة", "Remind me on the check date"), Modifier.weight(1f))
                Switch(st.remind, { Repo.saveStage(st.copy(remind = it)) })
            }
            if (st.status == StageStatus.DONE || st.doneDate != null) {
                DateField(t("تاريخ الانتهاء", "Done on"), st.doneDate, { Repo.saveStage(st.copy(doneDate = it)) }, clearable = true)
            }
            if (st.startedDate != null) {
                val end = st.doneDate ?: LocalDate.now()
                KeyValue(t("المدة", "Duration"), days(ChronoUnit.DAYS.between(st.startedDate, end)))
            }
            Field(t("ملاحظات (اللون، الرائحة، الحرارة...)", "Notes (colour, smell, temperature...)"), notes, { notes = it; Repo.saveStage(st.copy(notes = it)) }, singleLine = false)

            if (st.status != StageStatus.DONE) {
                Button(onClick = { Repo.completeStage(st, LocalDate.now()); Nav.back() }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("تمت المرحلة — ابدأ التالية", "Stage done — start the next"))
                }
            }

            SectionTitle(t("الكميات والتكاليف", "Quantities & costs"))
            AppCard {
                if (items.isEmpty()) Text(
                    t("سجّل ما استُخدم في هذه المرحلة: التمر، الماء، الأعشاب، القوارير... مع السعر.", "Record what was used in this stage: dates, water, herbs, bottles... with the price."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                items.forEach { it ->
                    Row(Modifier.fillMaxWidth().clickable { editItem = it }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(it.item, style = MaterialTheme.typography.bodyLarge)
                            Text("${itemKindName(it.kind)} · ${num(it.qty)} ${it.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(it.cost))
                    }
                }
                if (items.isNotEmpty()) {
                    Divider()
                    KeyValue(t("مجموع المرحلة", "Stage total"), money(items.sumOf { it.cost }), bold = true)
                }
                TextButton(onClick = {
                    editItem = StageItem(stageId = id, batchId = st.batchId, date = LocalDate.now(), item = "")
                }) { Text(t("+ إضافة بند", "+ Add item")) }
            }

            SectionTitle(t("الصور", "Photos"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEach { p -> Thumb(p) { viewPhoto = p } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val f = File(dir, "shot_${System.currentTimeMillis()}.jpg")
                    cameraFile = f
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", f))
                }) { Text(t("تصوير", "Camera")) }
                OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(t("من المعرض", "From gallery"))
                }
            }
        }
    }

    editItem?.let { ItemDialog(it) { editItem = null } }
    viewPhoto?.let { p ->
        AlertDialog(
            onDismissRequest = { viewPhoto = null },
            text = { PhotoImage(p, Modifier.fillMaxWidth().heightIn(max = 500.dp), full = true) },
            confirmButton = { TextButton(onClick = { viewPhoto = null }) { Text(t("إغلاق", "Close")) } },
            dismissButton = { TextButton(onClick = { Repo.deletePhoto(p); viewPhoto = null }) { Text(t("حذف", "Delete"), color = Danger) } },
        )
    }
    if (confirmDelete) ConfirmDialog(
        t("حذف المرحلة؟", "Delete stage?"),
        t("تُحذف بنودها وصورها أيضاً.", "Its items and photos are deleted too."),
        t("حذف", "Delete"), { Repo.deleteStage(id); Nav.back() }, { confirmDelete = false },
    )
}

@Composable
private fun Thumb(p: StagePhoto, onClick: () -> Unit) {
    PhotoImage(p, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), full = false)
}

@Composable
private fun PhotoImage(p: StagePhoto, modifier: Modifier, full: Boolean) {
    val bmp = remember(p.file, full) {
        val f = File(Repo.photosDir, p.file)
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = if (full) 1 else 6 })?.asImageBitmap()
    } ?: return
    Image(bmp, null, modifier, contentScale = if (full) ContentScale.Fit else ContentScale.Crop)
}

/** Add or edit one used item: what, how much, and what it cost. */
@Composable
private fun ItemDialog(initial: StageItem, onDone: () -> Unit) {
    val materials = watch { Repo.materials() }
    val known = watch { Repo.knownItemNames() }
    var kind by remember { mutableStateOf(initial.kind) }
    var item by remember { mutableStateOf(initial.item) }
    var qty by remember { mutableStateOf(if (initial.id == 0L) "" else num(initial.qty)) }
    var unit by remember { mutableStateOf(initial.unit) }
    var cost by remember { mutableStateOf(if (initial.id == 0L) "" else num(initial.cost)) }
    var materialId by remember { mutableStateOf(initial.materialId) }
    var d by remember { mutableStateOf(initial.date) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (initial.id == 0L) t("بند جديد", "New item") else t("تعديل البند", "Edit item")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickField(t("الصنف", "Kind"), ItemKind.all.map { it to itemKindName(it) }, kind, {
                    kind = it
                    if (item.isBlank()) item = itemKindName(it)
                })
                Field(t("البند", "Item"), item, { item = it })
                if (known.isNotEmpty() && item.isBlank()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        known.take(10).forEach { k -> SuggestionChip(onClick = { item = k }, label = { Text(k) }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(t("الكمية", "Quantity"), qty, { qty = it }, Modifier.weight(1f), number = true)
                    Field(t("الوحدة", "Unit"), unit, { unit = it }, Modifier.weight(1f))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    commonUnits.forEach { u -> SuggestionChip(onClick = { unit = u }, label = { Text(u) }) }
                }
                Field(t("السعر الإجمالي (د.ل)", "Total price (LYD)"), cost, { cost = it }, number = true)
                if (materials.isNotEmpty()) {
                    PickField(
                        t("خصم من المخزون (اختياري)", "Take from stock (optional)"),
                        listOf<Pair<Long?, String>>(null to t("لا", "No")) + materials.map { it.id to "${it.name} (${num(it.stock)} ${it.unit})" },
                        materialId, { materialId = it },
                    )
                }
                DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
            }
        },
        confirmButton = {
            TextButton(enabled = item.isNotBlank(), onClick = {
                Repo.saveStageItem(
                    initial.copy(kind = kind, item = item, qty = parseNum(qty) ?: 0.0, unit = unit, cost = parseNum(cost) ?: 0.0, materialId = materialId, date = d)
                )
                onDone()
            }) { Text(t("حفظ", "Save")) }
        },
        dismissButton = {
            Row {
                if (initial.id != 0L) TextButton(onClick = { Repo.deleteStageItem(initial); onDone() }) { Text(t("حذف", "Delete"), color = Danger) }
                TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) }
            }
        },
    )
}
