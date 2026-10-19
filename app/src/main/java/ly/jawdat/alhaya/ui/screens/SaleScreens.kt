package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Market
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.data.Sale
import ly.jawdat.alhaya.data.SaleView
import ly.jawdat.alhaya.logic.Period
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
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import java.time.LocalDate

// ---------- List ----------

@Composable
fun SalesScreen() {
    var range by rememberSaveable { mutableStateOf("week") }
    val views = watch { Repo.saleViews() }
    val today = LocalDate.now()
    val shown = when (range) {
        "today" -> views.filter { it.sale.date == today }
        "week" -> Period.thisWeek().let { p -> views.filter { it.sale.date in p } }
        "month" -> Period.thisMonth().let { p -> views.filter { it.sale.date in p } }
        "unpaid" -> views.filter { !it.isPaid }
        else -> views
    }
    AppScreen(
        title = t("المبيعات", "Sales"),
        help = GuideIds.SALES,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { Nav.go(Screen.SaleEdit()) },
                icon = { Ic(R.drawable.ic_add) },
                text = { Text(t("بيع جديد", "New sale")) },
            )
        },
    ) { pad ->
        ListBody(pad) {
            item {
                ChipRow(
                    listOf(
                        "today" to t("اليوم", "Today"),
                        "week" to t("هذا الأسبوع", "This week"),
                        "month" to t("هذا الشهر", "This month"),
                        "unpaid" to t("غير مدفوعة", "Unpaid"),
                        "all" to t("الكل", "All"),
                    ),
                    range,
                ) { range = it }
            }
            item {
                StatRow(
                    Triple(t("المبلغ", "Amount"), money(shown.sumOf { it.sale.total }), null),
                    Triple(t("القوارير", "Bottles"), shown.sumOf { it.sale.qty }.toString(), null),
                    Triple(t("غير مدفوع", "Unpaid"), money(shown.sumOf { it.owed }), if (shown.any { !it.isPaid }) Warn else null),
                )
            }
            if (shown.isEmpty()) item { EmptyState(t("لا توجد مبيعات", "No sales"), t("اضغط \"بيع جديد\" لتسجيل بيع لمحل.", "Tap \"New sale\" to record a sale to a market.")) }
            items(shown, key = { it.sale.id }) { SaleCard(it) }
        }
    }
}

@Composable
fun SaleCard(v: SaleView) {
    AppCard(onClick = { Nav.go(Screen.SaleEdit(v.sale.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.productName, style = MaterialTheme.typography.titleSmall)
                Text(
                    (v.marketName ?: t("زبون مباشر", "Direct customer")) + " · " + date(v.sale.date),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(v.sale.total), style = MaterialTheme.typography.titleSmall)
                Text("${v.sale.qty} × ${num(v.sale.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.padding(top = 6.dp)) {
            if (v.isPaid) Badge(t("مدفوع", "Paid"), Good)
            else Badge(t("متبقي ", "Owes ") + money(v.owed), Warn)
        }
    }
}

// ---------- Add / edit ----------

@Composable
fun SaleEditScreen(id: Long, presetMarket: Long?) {
    val existing = remember(id) { if (id != 0L) Repo.sale(id) else null }
    val products = watch { Repo.products() }
    val markets = watch { Repo.markets() }
    val batches = watch { Repo.batches() }

    var marketId by rememberSaveable { mutableStateOf(existing?.marketId ?: presetMarket ?: lastMarket()) }
    var productId by rememberSaveable { mutableStateOf(existing?.productId ?: products.firstOrNull()?.id) }
    var qty by rememberSaveable { mutableStateOf(existing?.qty?.toString() ?: "1") }
    var price by rememberSaveable { mutableStateOf(existing?.unitPrice?.let { num(it) } ?: products.firstOrNull()?.price?.let { num(it) } ?: "") }
    var day by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var batchId by rememberSaveable { mutableStateOf(existing?.batchId) }
    val paidBefore = remember(id) { if (id != 0L) Repo.paymentForSale(id) else null }
    var payMode by rememberSaveable {
        mutableStateOf(
            when {
                existing == null -> "full"
                paidBefore!! >= existing.total - 0.005 -> "full"
                paidBefore > 0 -> "part"
                else -> "later"
            }
        )
    }
    var partAmount by rememberSaveable { mutableStateOf(paidBefore?.takeIf { it > 0 }?.let { num(it) } ?: "") }
    var addingMarket by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val product = products.firstOrNull { it.id == productId }
    val q = parseNum(qty)?.toInt() ?: 0
    val p = parseNum(price) ?: 0.0
    val total = q * p
    val paidNow = when (payMode) {
        "full" -> total
        "part" -> (parseNum(partAmount) ?: 0.0).coerceIn(0.0, total)
        else -> 0.0
    }

    // When the product changes on a new sale, use that product's price.
    LaunchedEffect(productId) {
        if (existing == null || productId != existing.productId) products.firstOrNull { it.id == productId }?.let { price = num(it.price) }
    }

    fun save(): Boolean {
        if (productId == null || q <= 0) return false
        Repo.saveSale(
            Sale(id = id, date = day, marketId = marketId, productId = productId!!, batchId = batchId, qty = q, unitPrice = p, total = total, notes = notes),
            paidNow,
        )
        rememberLastMarket(marketId)
        return true
    }

    AppScreen(
        title = if (id == 0L) t("بيع جديد", "New sale") else t("تعديل البيع", "Edit sale"),
        help = GuideIds.SALES,
        onBack = { Nav.back() },
        actions = {
            if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Ic(R.drawable.ic_delete) }
        },
    ) { pad ->
        FormBody(pad) {
            if (products.isEmpty()) {
                AppCard {
                    Text(t("أضف منتجاتك وأسعارها أولاً.", "Add your products and prices first."))
                    TextButton(onClick = { Nav.go(Screen.ProductEdit()) }) { Text(t("+ إضافة منتج", "+ Add product")) }
                }
                return@FormBody
            }
            if (saved) {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(t("تم الحفظ. أضف منتجاً آخر لنفس المحل.", "Saved. Add another product for the same market."))
                }
            }
            PickField(
                t("المحل", "Market"),
                listOf<Pair<Long?, String>>(null to t("زبون مباشر (بدون محل)", "Direct customer (no market)")) + markets.map { it.id to it.name },
                marketId, { marketId = it },
                footer = { close -> TextButton(onClick = { close(); addingMarket = true }) { Text(t("+ محل جديد", "+ New market")) } },
            )
            PickField(
                t("المنتج", "Product"),
                products.map { it.id to "${it.name} ${it.size}".trim() + "  —  " + t("المتوفر ${it.stock}", "in stock ${it.stock}") },
                productId, { productId = it },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { qty = (q - 1).coerceAtLeast(1).toString() }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Field(t("الكمية", "Quantity"), qty, { qty = it }, Modifier.weight(1f), number = true)
                FilledTonalIconButton(onClick = { qty = (q + 1).toString() }) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            if (product != null && q > product.stock + (existing?.takeIf { it.productId == product.id }?.qty ?: 0)) {
                Text(t("تنبيه: الكمية أكبر من المخزون المسجّل (${product.stock}).", "Note: more than the recorded stock (${product.stock})."), color = Warn, style = MaterialTheme.typography.bodySmall)
            }
            Field(t("سعر القارورة (د.ل)", "Price per bottle (LYD)"), price, { price = it }, number = true)
            AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                KeyValue(t("الإجمالي", "Total"), money(total), bold = true)
            }

            Text(t("الدفع", "Payment"), style = MaterialTheme.typography.titleSmall)
            ChipRow(listOf("full" to t("مدفوع بالكامل", "Paid in full"), "later" to t("يدفع لاحقاً", "Pays later"), "part" to t("دفع جزء", "Paid part")), payMode) { payMode = it }
            if (payMode == "part") Field(t("المبلغ المدفوع الآن", "Amount paid now"), partAmount, { partAmount = it }, number = true)
            if (payMode != "full") KeyValue(t("المتبقي على المحل", "Left to pay"), money(total - paidNow), valueColor = Warn)
            if (payMode != "full" && marketId == null) {
                Text(t("اختر المحل حتى يُسجَّل الدين عليه.", "Choose the market so the debt is recorded against it."), color = Danger, style = MaterialTheme.typography.bodySmall)
            }

            DateField(t("التاريخ", "Date"), day, { it?.let { d -> day = d } })
            val productBatches = remember(productId, batches) {
                val ids = Repo.allOutputs().filter { it.productId == productId }.map { it.batchId }.toSet()
                batches.filter { it.id in ids }
            }
            if (productBatches.isNotEmpty()) {
                PickField(
                    t("من دفعة (اختياري)", "From batch (optional)"),
                    listOf<Pair<Long?, String>>(null to "—") + productBatches.map { it.id to (it.label) },
                    batchId, { batchId = it },
                )
            }
            Field(t("ملاحظات", "Notes"), notes, { notes = it })

            Button(
                enabled = q > 0 && productId != null && !(payMode != "full" && marketId == null),
                onClick = { if (save()) Nav.back() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("حفظ", "Save")) }
            if (id == 0L) OutlinedButton(
                enabled = q > 0 && productId != null && !(payMode != "full" && marketId == null),
                onClick = { if (save()) { saved = true; qty = "1"; notes = ""; batchId = null } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("حفظ وإضافة منتج آخر", "Save & add another product"), textAlign = TextAlign.Center) }
        }
    }

    if (addingMarket) QuickMarketDialog(onDone = { addingMarket = false }) { marketId = it }
    if (confirmDelete && existing != null) ConfirmDialog(
        t("حذف البيع؟", "Delete sale?"),
        t("تُعاد القوارير إلى المخزون ويُحذف ما دُفع معه.", "The bottles go back into stock and the money paid with it is removed."),
        t("حذف", "Delete"), { Repo.deleteSale(existing); Nav.back() }, { confirmDelete = false },
    )
}

@Composable
fun QuickMarketDialog(onDone: () -> Unit, onCreated: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("محل جديد", "New market")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("اسم المحل", "Market name"), name, { name = it })
                Field(t("الهاتف", "Phone"), phone, { phone = it }, phone = true)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreated(Repo.saveMarket(Market(name = name, phone = phone))); onDone() }) {
                Text(t("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

// The last market used is the default for the next sale (one shop often takes several products).
private var lastMarketId: Long? = null
private fun lastMarket(): Long? = lastMarketId
private fun rememberLastMarket(id: Long?) { lastMarketId = id }
