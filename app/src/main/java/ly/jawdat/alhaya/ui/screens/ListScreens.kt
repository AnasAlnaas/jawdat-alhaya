package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Expense
import ly.jawdat.alhaya.data.ExpenseCategory
import ly.jawdat.alhaya.data.Herb
import ly.jawdat.alhaya.data.Material
import ly.jawdat.alhaya.data.Product
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.logic.Period
import ly.jawdat.alhaya.logic.Reports
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.Badge
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.DateField
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
import ly.jawdat.alhaya.ui.commonUnits
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.expenseCategoryName
import ly.jawdat.alhaya.ui.herbName
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate

// ---------- Products & prices ----------

@Composable
fun ProductsScreen() {
    val context = LocalContext.current
    val products = watch { Repo.products(true) }
    val cpb = watch { Reports.costPerBottle(Repo.batchSummaries()) }
    var adjusting by remember { mutableStateOf<Product?>(null) }
    AppScreen(
        title = t("المنتجات والأسعار", "Products & prices"),
        help = GuideIds.PRODUCTS,
        onBack = { Nav.back() },
        actions = {
            IconButton(onClick = { Files.shareText(context, priceList(products.filter { !it.archived })) }) { Ic(R.drawable.ic_share) }
        },
        fab = { ExtendedFloatingActionButton(onClick = { Nav.go(Screen.ProductEdit()) }, icon = { Ic(R.drawable.ic_add) }, text = { Text(t("منتج جديد", "New product")) }) },
    ) { pad ->
        ListBody(pad) {
            if (products.isEmpty()) item {
                EmptyState(
                    t("لا توجد منتجات بعد", "No products yet"),
                    t("أضف كل نوع وحجم بسعره، مثل: خل التمر بالزعتر 500 مل.", "Add each type and size with its price, e.g. Date vinegar with thyme 500 ml."),
                )
            }
            if (products.isNotEmpty()) item {
                Text(t("زر المشاركة يرسل قائمة الأسعار عبر واتساب أو تيليجرام.", "The share button sends the price list by WhatsApp or Telegram."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(products, key = { it.id }) { p ->
                AppCard(onClick = { Nav.go(Screen.ProductEdit(p.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${p.name} ${p.size}".trim() + if (p.archived) t(" (مؤرشف)", " (archived)") else "", style = MaterialTheme.typography.titleSmall)
                            Text(herbName(p.herb), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(money(p.price), style = MaterialTheme.typography.titleSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val low = p.stock <= p.lowStock
                        Badge(t("المخزون ", "Stock ") + p.stock, if (low) Warn else Good)
                        cpb[p.id]?.let {
                            Text(
                                "  " + t("التكلفة ", "Cost ") + money(it) + " · " + t("الربح ", "Profit ") + money(p.price - it),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                            )
                        } ?: androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        TextButton(onClick = { adjusting = p }) { Text(t("تعديل المخزون", "Adjust stock")) }
                    }
                }
            }
        }
    }
    adjusting?.let { p ->
        AdjustDialog(
            t("مخزون ${p.name}", "${p.name} stock"),
            p.stock.toDouble(),
            t("مثلاً: قوارير مكسورة أو عدّ يدوي.", "e.g. broken bottles or a manual count."),
            onDone = { adjusting = null },
        ) { newValue -> Repo.adjustProductStock(p.id, newValue.toInt() - p.stock) }
    }
}

private fun priceList(products: List<Product>): String = buildString {
    appendLine(t("🌿 جودة الحياة — منتجات طبيعية بأيدٍ ليبية", "🌿 Jawdat Al-Hayah — natural products made in Libya"))
    appendLine(t("قائمة الأسعار:", "Price list:"))
    products.groupBy { it.herb }.forEach { (_, list) ->
        list.forEach { appendLine("• ${it.name} ${it.size}".trim() + " — " + money(it.price)) }
    }
    appendLine()
    append(t("للطلب: 091-3390662", "To order: 091-3390662"))
}

private fun suggestedName(herb: String): String = when (herb) {
    Herb.PLAIN -> t("خل التمر الطبيعي", "Natural date vinegar")
    Herb.THYME -> t("خل التمر بالزعتر", "Date vinegar with thyme")
    Herb.ROSEMARY -> t("خل التمر بإكليل الجبل", "Date vinegar with rosemary")
    Herb.GINGER -> t("خل التمر بالزنجبيل", "Date vinegar with ginger")
    Herb.GARLIC -> t("خل التمر بالثوم", "Date vinegar with garlic")
    Herb.FENNEL -> t("خل التمر بالشمر", "Date vinegar with fennel")
    Herb.MIXED -> t("خل التمر بخليط الأعشاب", "Date vinegar with mixed herbs")
    else -> t("خل التمر", "Date vinegar")
}

@Composable
fun ProductEditScreen(id: Long) {
    val existing = remember(id) { if (id != 0L) Repo.product(id) else null }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: suggestedName(Herb.PLAIN)) }
    var herb by rememberSaveable { mutableStateOf(existing?.herb ?: Herb.PLAIN) }
    var size by rememberSaveable { mutableStateOf(existing?.size ?: "") }
    var price by rememberSaveable { mutableStateOf(existing?.price?.let { num(it) } ?: "") }
    var stock by rememberSaveable { mutableStateOf(existing?.stock?.toString() ?: "") }
    var low by rememberSaveable { mutableStateOf(existing?.lowStock?.toString() ?: "5") }
    var archived by rememberSaveable { mutableStateOf(existing?.archived ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val used = remember(id) { id != 0L && Repo.productIsUsed(id) }

    AppScreen(if (id == 0L) t("منتج جديد", "New product") else t("تعديل المنتج", "Edit product"), onBack = { Nav.back() }, help = GuideIds.PRODUCTS) { pad ->
        FormBody(pad) {
            PickField(t("النوع", "Type"), herbOptions, herb, {
                // Keep the suggested name in step with the type, unless it was typed by hand.
                if (name.isBlank() || name == suggestedName(herb)) name = suggestedName(it)
                herb = it
            })
            Field(t("اسم المنتج", "Product name"), name, { name = it })
            Field(t("الحجم", "Size"), size, { size = it }, supporting = t("مثال: 500 مل، 1 لتر", "e.g. 500 ml, 1 L"))
            Field(t("سعر البيع للقارورة (د.ل)", "Selling price per bottle (LYD)"), price, { price = it }, number = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("المخزون الحالي", "Current stock"), stock, { stock = it }, Modifier.weight(1f), number = true)
                Field(t("نبّهني عند", "Warn me at"), low, { low = it }, Modifier.weight(1f), number = true)
            }
            if (id != 0L) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("أرشفة (لم يعد يُباع)", "Archive (no longer sold)"), Modifier.weight(1f))
                Switch(archived, { archived = it })
            }
            Button(enabled = name.isNotBlank(), onClick = {
                Repo.saveProduct(
                    Product(
                        id = id, name = name, herb = herb, size = size, price = parseNum(price) ?: 0.0,
                        stock = parseNum(stock)?.toInt() ?: 0, lowStock = parseNum(low)?.toInt() ?: 0, archived = archived,
                    )
                )
                Nav.back()
            }, modifier = Modifier.fillMaxWidth()) { Text(t("حفظ", "Save")) }
            if (id != 0L) {
                if (used) Text(t("لهذا المنتج مبيعات أو إنتاج مسجّل، لذا يمكن أرشفته فقط.", "This product has sales or production, so it can only be archived."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else TextButton(onClick = { confirmDelete = true }) { Text(t("حذف المنتج", "Delete product"), color = Danger) }
            }
        }
    }
    if (confirmDelete) ConfirmDialog(t("حذف المنتج؟", "Delete product?"), name, t("حذف", "Delete"), { Repo.deleteProduct(id); Nav.back() }, { confirmDelete = false })
}

// ---------- Raw stock (materials) ----------

@Composable
fun StockScreen() {
    val materials = watch { Repo.materials() }
    val products = watch { Repo.products() }
    var editing by remember { mutableStateOf<Material?>(null) }
    var adjusting by remember { mutableStateOf<Material?>(null) }
    AppScreen(
        title = t("المخزون", "Stock"),
        help = GuideIds.STOCK,
        onBack = { Nav.back() },
        fab = { ExtendedFloatingActionButton(onClick = { editing = Material(name = "") }, icon = { Ic(R.drawable.ic_add) }, text = { Text(t("مادة جديدة", "New material")) }) },
    ) { pad ->
        ListBody(pad) {
            item { SectionTitle(t("القوارير الجاهزة للبيع", "Bottles ready to sell")) }
            item {
                AppCard {
                    if (products.isEmpty()) Text(t("لا توجد منتجات.", "No products."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    products.forEach { p ->
                        KeyValue("${p.name} ${p.size}".trim(), p.stock.toString(), valueColor = if (p.stock <= p.lowStock) Warn else MaterialTheme.colorScheme.onSurface)
                    }
                    if (products.isNotEmpty()) {
                        KeyValue(t("المجموع", "Total"), products.sumOf { it.stock }.toString(), bold = true)
                        KeyValue(t("القيمة بسعر البيع", "Value at selling price"), money(products.sumOf { it.stock * it.price }))
                    }
                }
            }
            item { SectionTitle(t("المواد الخام والتغليف", "Raw materials & packaging")) }
            item {
                Text(
                    t("مثل: تمر، أعشاب، قوارير فارغة، ملصقات. عند إضافة بند في مرحلة يمكنك خصمه من هنا.", "e.g. dates, herbs, empty bottles, labels. Items added to a stage can be taken from here."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (materials.isEmpty()) item { EmptyState(t("لا توجد مواد بعد", "No materials yet")) }
            items(materials, key = { it.id }) { m ->
                AppCard(onClick = { editing = m }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Badge("${num(m.stock)} ${m.unit}", if (m.stock <= m.lowLevel) Warn else Good)
                        TextButton(onClick = { adjusting = m }) { Text(t("تعديل", "Adjust")) }
                    }
                }
            }
        }
    }
    editing?.let { MaterialDialog(it) { editing = null } }
    adjusting?.let { m ->
        AdjustDialog(
            t("كمية ${m.name}", "${m.name} amount"), m.stock,
            t("اكتب الكمية الموجودة الآن (بعد الشراء أو العدّ).", "Type the amount there is now (after buying or counting)."),
            onDone = { adjusting = null },
        ) { Repo.adjustMaterial(m.id, it - m.stock) }
    }
}

@Composable
private fun MaterialDialog(m: Material, onDone: () -> Unit) {
    var name by remember { mutableStateOf(m.name) }
    var unit by remember { mutableStateOf(m.unit) }
    var stock by remember { mutableStateOf(num(m.stock)) }
    var low by remember { mutableStateOf(num(m.lowLevel)) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (m.id == 0L) t("مادة جديدة", "New material") else t("تعديل المادة", "Edit material")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("الاسم", "Name"), name, { name = it })
                Field(t("الوحدة", "Unit"), unit, { unit = it })
                ChipRow(commonUnits.map { it to it }, unit) { unit = it }
                Field(t("الكمية الحالية", "Current amount"), stock, { stock = it }, number = true)
                Field(t("نبّهني عندما تصل إلى", "Warn me when it reaches"), low, { low = it }, number = true)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                Repo.saveMaterial(m.copy(name = name, unit = unit, stock = parseNum(stock) ?: 0.0, lowLevel = parseNum(low) ?: 0.0)); onDone()
            }) { Text(t("حفظ", "Save")) }
        },
        dismissButton = {
            Row {
                if (m.id != 0L) TextButton(onClick = { Repo.deleteMaterial(m.id); onDone() }) { Text(t("حذف", "Delete"), color = Danger) }
                TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) }
            }
        },
    )
}

/** Set a stock amount to a new value. */
@Composable
private fun AdjustDialog(title: String, current: Double, hint: String, onDone: () -> Unit, onSet: (Double) -> Unit) {
    var value by remember { mutableStateOf(num(current)) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                Field(t("الكمية", "Amount"), value, { value = it }, number = true)
            }
        },
        confirmButton = { TextButton(onClick = { parseNum(value)?.let(onSet); onDone() }) { Text(t("حفظ", "Save")) } },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

// ---------- Expenses ----------

@Composable
fun ExpensesScreen() {
    var range by rememberSaveable { mutableStateOf("month") }
    val all = watch { Repo.expenses() }
    val shown = when (range) {
        "month" -> Period.thisMonth().let { p -> all.filter { it.date in p } }
        "last" -> Period.lastMonth().let { p -> all.filter { it.date in p } }
        else -> all
    }
    AppScreen(
        title = t("المصاريف", "Expenses"),
        help = GuideIds.EXPENSES,
        onBack = { Nav.back() },
        fab = { ExtendedFloatingActionButton(onClick = { Nav.go(Screen.ExpenseEdit()) }, icon = { Ic(R.drawable.ic_add) }, text = { Text(t("مصروف جديد", "New expense")) }) },
    ) { pad ->
        ListBody(pad) {
            item {
                Text(
                    t("مصاريف عامة غير مرتبطة بدفعة معيّنة (نقل، معدات، إعلانات...). تكاليف التمر والأعشاب تُسجَّل داخل مراحل الدفعة.", "General costs not tied to one batch (transport, equipment, ads...). Dates and herbs are recorded inside the batch stages."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { ChipRow(listOf("month" to t("هذا الشهر", "This month"), "last" to t("الشهر الماضي", "Last month"), "all" to t("الكل", "All")), range) { range = it } }
            item { StatRow(Triple(t("المجموع", "Total"), money(shown.sumOf { it.amount }), null), Triple(t("العدد", "Count"), shown.size.toString(), null)) }
            if (shown.isEmpty()) item { EmptyState(t("لا توجد مصاريف", "No expenses")) }
            items(shown, key = { it.id }) { e ->
                AppCard(onClick = { Nav.go(Screen.ExpenseEdit(e.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.description.ifBlank { expenseCategoryName(e.category) }, style = MaterialTheme.typography.titleSmall)
                            Text(expenseCategoryName(e.category) + " · " + date(e.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(e.amount), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseEditScreen(id: Long) {
    val existing = remember(id) { if (id != 0L) Repo.expenses().firstOrNull { it.id == id } else null }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: ExpenseCategory.TRANSPORT) }
    var desc by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var amount by rememberSaveable { mutableStateOf(existing?.amount?.let { num(it) } ?: "") }
    var d by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val a = parseNum(amount) ?: 0.0
    AppScreen(
        if (id == 0L) t("مصروف جديد", "New expense") else t("تعديل المصروف", "Edit expense"),
        help = GuideIds.EXPENSES,
        onBack = { Nav.back() },
        actions = { if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Ic(R.drawable.ic_delete) } },
    ) { pad ->
        FormBody(pad) {
            PickField(t("الفئة", "Category"), ExpenseCategory.all.map { it to expenseCategoryName(it) }, category, { category = it })
            Field(t("الوصف", "Description"), desc, { desc = it })
            Field(t("المبلغ (د.ل)", "Amount (LYD)"), amount, { amount = it }, number = true)
            DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
            Button(enabled = a > 0, onClick = {
                Repo.saveExpense(Expense(id = id, date = d, category = category, description = desc, amount = a)); Nav.back()
            }, modifier = Modifier.fillMaxWidth()) { Text(t("حفظ", "Save")) }
        }
    }
    if (confirmDelete) ConfirmDialog(t("حذف المصروف؟", "Delete expense?"), money(a), t("حذف", "Delete"), { Repo.deleteExpense(id); Nav.back() }, { confirmDelete = false })
}
