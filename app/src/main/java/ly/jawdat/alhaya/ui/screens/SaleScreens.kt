package ly.jawdat.alhaya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import ly.jawdat.alhaya.ui.CheckBurst
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.DateField
import ly.jawdat.alhaya.ui.EmptyState
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.FormBody
import ly.jawdat.alhaya.ui.GiftIcon
import ly.jawdat.alhaya.ui.Good
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.KeyValue
import ly.jawdat.alhaya.ui.Leaf
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.PickField
import ly.jawdat.alhaya.ui.QtyStepper
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.animatedNumber
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.ltr
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.title
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.data.ProductSize
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
        "gifts" -> views.filter { it.sale.giftQty > 0 }
        else -> views
    }
    val gifted = shown.sumOf { it.sale.giftQty }
    val listState = rememberLazyListState()
    // The button shrinks to its icon once the list is scrolled.
    val expanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    AppScreen(
        title = t("المبيعات", "Sales"),
        help = GuideIds.SALES,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { Nav.go(Screen.SaleEdit()) },
                expanded = expanded,
                icon = { Ic(R.drawable.ic_add) },
                text = { Text(t("بيع جديد", "New sale")) },
            )
        },
    ) { pad ->
        ListBody(pad, listState) {
            item {
                ChipRow(
                    listOf(
                        "today" to t("اليوم", "Today"),
                        "week" to t("هذا الأسبوع", "This week"),
                        "month" to t("هذا الشهر", "This month"),
                        "unpaid" to t("غير مدفوعة", "Unpaid"),
                        "gifts" to t("🎁 الهدايا", "🎁 Gifts"),
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
            if (gifted > 0) item {
                Text(
                    t("🎁 قوارير مهداة: $gifted", "🎁 Bottles given as gifts: $gifted"),
                    style = MaterialTheme.typography.bodySmall, color = Leaf,
                )
            }
            if (shown.isEmpty()) item { EmptyState(t("لا توجد مبيعات", "No sales"), t("اضغط \"بيع جديد\" لتسجيل بيع لمحل.", "Tap \"New sale\" to record a sale to a market.")) }
            items(shown, key = { it.sale.id }) { Box(Modifier.animateItem()) { SaleCard(it) } }
        }
    }
}

@Composable
fun SaleCard(v: SaleView) {
    val s = v.sale
    AppCard(onClick = { Nav.go(Screen.SaleEdit(s.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s.isGiftOnly) {
                GiftIcon()
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(v.productName, style = MaterialTheme.typography.titleSmall)
                Text(
                    (v.marketName ?: t("زبون مباشر", "Direct customer")) + " · " + date(s.date),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (s.isGiftOnly) {
                    Text(t("${s.giftQty} قارورة", "${s.giftQty} btl"), style = MaterialTheme.typography.titleSmall, color = Leaf)
                } else {
                    Text(money(s.total), style = MaterialTheme.typography.titleSmall)
                    Text("${s.qty} × ${num(s.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                s.isGiftOnly -> Badge(t("هدية", "Gift"), Leaf)
                v.isPaid -> Badge(t("مدفوع", "Paid"), Good)
                else -> Badge(t("متبقي ", "Owes ") + money(v.owed), Warn)
            }
            if (!s.isGiftOnly && s.giftQty > 0) Badge(t("🎁 هدية ${ltr("+" + s.giftQty)}", "🎁 +${s.giftQty} free"), Leaf)
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

    // "sale" sells bottles (and may add free ones); "gift" gives bottles away with nothing to pay.
    var mode by rememberSaveable { mutableStateOf(if (existing?.isGiftOnly == true) "gift" else "sale") }
    var marketId by rememberSaveable { mutableStateOf(existing?.marketId ?: presetMarket ?: lastMarket()) }
    var productId by rememberSaveable { mutableStateOf(existing?.productId ?: products.firstOrNull()?.id) }
    var qty by rememberSaveable { mutableStateOf(existing?.takeIf { !it.isGiftOnly }?.qty?.toString() ?: "1") }
    var giftQty by rememberSaveable { mutableStateOf(existing?.giftQty?.takeIf { it > 0 }?.toString() ?: "1") }
    var withGift by rememberSaveable { mutableStateOf(existing != null && !existing.isGiftOnly && existing.giftQty > 0) }
    var price by rememberSaveable { mutableStateOf(existing?.takeIf { !it.isGiftOnly }?.unitPrice?.let { num(it) } ?: products.firstOrNull()?.price?.let { num(it) } ?: "") }
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
    var addingProduct by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var savedCount by remember { mutableIntStateOf(0) }

    val gift = mode == "gift"
    val product = products.firstOrNull { it.id == productId }
    val q = if (gift) 0 else parseNum(qty)?.toInt() ?: 0
    val g = if (gift || withGift) parseNum(giftQty)?.toInt() ?: 0 else 0
    val p = if (gift) 0.0 else parseNum(price) ?: 0.0
    val total = q * p
    val paidNow = when {
        gift -> 0.0
        payMode == "full" -> total
        payMode == "part" -> (parseNum(partAmount) ?: 0.0).coerceIn(0.0, total)
        else -> 0.0
    }
    // Stock this sale can draw on: what is recorded now, plus what this sale already took (when editing).
    val available = (product?.stock ?: 0) +
        (existing?.takeIf { it.productId == productId }?.let { it.bottlesOut - it.stockAdded } ?: 0)
    val toAdd = (q + g - available.coerceAtLeast(0)).coerceAtLeast(0)
    val needsMarket = !gift && payMode != "full" && total > 0.005 && marketId == null
    val canSave = productId != null && (if (gift) g > 0 else q > 0) && !needsMarket

    // When the product changes on a new sale, use that product's price.
    LaunchedEffect(productId) {
        if (existing == null || productId != existing.productId) products.firstOrNull { it.id == productId }?.let { price = num(it.price) }
    }

    fun save(): Boolean {
        if (!canSave) return false
        Repo.saveSale(
            Sale(
                id = id, date = day, marketId = marketId, productId = productId!!, batchId = batchId,
                qty = q, unitPrice = p, total = total, notes = notes, giftQty = g,
            ),
            paidNow,
        )
        rememberLastMarket(marketId)
        return true
    }

    AppScreen(
        title = when {
            id == 0L && gift -> t("هدية جديدة", "New gift")
            id == 0L -> t("بيع جديد", "New sale")
            gift -> t("تعديل الهدية", "Edit gift")
            else -> t("تعديل البيع", "Edit sale")
        },
        help = GuideIds.SALES,
        onBack = { Nav.back() },
        actions = {
            if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Ic(R.drawable.ic_delete) }
        },
    ) { pad ->
        FormBody(pad) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !gift, onClick = { mode = "sale" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Ic(R.drawable.ic_cart, Modifier.padding(end = 2.dp)) },
                ) { Text(t("بيع", "Sale")) }
                SegmentedButton(
                    selected = gift, onClick = { mode = "gift" },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Text("🎁") },
                ) { Text(t("هدية", "Gift")) }
            }

            AnimatedVisibility(savedCount > 0, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        key(savedCount) { CheckBurst() }
                        Spacer(Modifier.width(12.dp))
                        Text(t("تم الحفظ. أضف منتجاً آخر لنفس المحل.", "Saved. Add another product for the same market."))
                    }
                }
            }

            PickField(
                if (gift) t("لمن؟ (اختياري)", "Who to? (optional)") else t("المحل", "Market"),
                listOf<Pair<Long?, String>>(null to if (gift) t("بدون محل", "No market") else t("زبون مباشر (بدون محل)", "Direct customer (no market)")) +
                    markets.map { it.id to it.name },
                marketId, { marketId = it },
                footer = { close -> TextButton(onClick = { close(); addingMarket = true }) { Text(t("+ محل جديد", "+ New market")) } },
            )

            if (products.isEmpty()) {
                AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(t("لا توجد منتجات بعد. أضف المنتج هنا وأكمل البيع مباشرة.", "No products yet. Add the product here and carry on with the sale."))
                    Button(onClick = { addingProduct = true }, modifier = Modifier.padding(top = 8.dp)) { Text(t("+ منتج جديد", "+ New product")) }
                }
            } else {
                PickField(
                    t("المنتج", "Product"),
                    products.map { it.id to it.title + "  —  " + t("المتوفر ${it.stock}", "in stock ${it.stock}") },
                    productId, { productId = it },
                    footer = { close -> TextButton(onClick = { close(); addingProduct = true }) { Text(t("+ منتج جديد", "+ New product")) } },
                )
            }

            val bottle = product?.size ?: ProductSize.SMALL
            if (gift) {
                QtyStepper(t("عدد القوارير المهداة", "Bottles given"), giftQty, { giftQty = it }, bottle = bottle, accent = Leaf)
            } else {
                QtyStepper(t("الكمية", "Quantity"), qty, { qty = it }, bottle = bottle)
            }

            // Selling more than is recorded is fine: the missing bottles are put into stock with the sale.
            AnimatedVisibility(product != null && toAdd > 0, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ic(R.drawable.ic_box, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (gift) t(
                                "المسجّل في المخزون ${available.coerceAtLeast(0)}. عند الحفظ يُضاف $toAdd إلى المخزون وتُسجَّل الهدية معاً.",
                                "Recorded stock is ${available.coerceAtLeast(0)}. Saving adds $toAdd to stock and records the gift together.",
                            ) else t(
                                "المسجّل في المخزون ${available.coerceAtLeast(0)}. عند الحفظ يُضاف $toAdd إلى المخزون ويُسجَّل البيع معاً.",
                                "Recorded stock is ${available.coerceAtLeast(0)}. Saving adds $toAdd to stock and records the sale together.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (!gift) {
                Field(t("سعر القارورة (د.ل)", "Price per bottle (LYD)"), price, { price = it }, number = true)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GiftIcon()
                    Spacer(Modifier.width(10.dp))
                    Text(t("قوارير هدية مع البيع", "Gift bottles with this sale"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(withGift, { withGift = it })
                }
                AnimatedVisibility(withGift, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    QtyStepper(t("قوارير مجانية", "Free bottles"), giftQty, { giftQty = it }, bottle = bottle, accent = Leaf)
                }

                AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    KeyValue(t("الإجمالي", "Total"), money(animatedNumber(total)), bold = true)
                    if (g > 0) KeyValue(t("🎁 هدية", "🎁 Free"), t("$g قارورة", "$g btl"), Leaf)
                }

                Text(t("الدفع", "Payment"), style = MaterialTheme.typography.titleSmall)
                ChipRow(listOf("full" to t("مدفوع بالكامل", "Paid in full"), "later" to t("يدفع لاحقاً", "Pays later"), "part" to t("دفع جزء", "Paid part")), payMode) { payMode = it }
                if (payMode == "part") Field(t("المبلغ المدفوع الآن", "Amount paid now"), partAmount, { partAmount = it }, number = true)
                if (payMode != "full") KeyValue(t("المتبقي على المحل", "Left to pay"), money(total - paidNow), valueColor = Warn)
                if (needsMarket) {
                    Text(t("اختر المحل حتى يُسجَّل الدين عليه.", "Choose the market so the debt is recorded against it."), color = Danger, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    t("الهدية لا تُحسب كمبيعات، لكن قواريرها تخرج من المخزون وتظهر تكلفتها في التقارير.", "A gift isn't counted as a sale, but its bottles leave stock and their cost shows in the reports."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                enabled = canSave,
                onClick = { if (save()) Nav.back() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("حفظ", "Save")) }
            if (id == 0L) OutlinedButton(
                enabled = canSave,
                onClick = {
                    if (save()) {
                        savedCount++; qty = "1"; giftQty = "1"; withGift = false; notes = ""; batchId = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("حفظ وإضافة منتج آخر", "Save & add another product"), textAlign = TextAlign.Center) }
        }
    }

    if (addingMarket) QuickMarketDialog(onDone = { addingMarket = false }) { marketId = it }
    if (addingProduct) QuickProductDialog(onDone = { addingProduct = false }) { productId = it }
    if (confirmDelete && existing != null) ConfirmDialog(
        if (existing.isGiftOnly) t("حذف الهدية؟", "Delete gift?") else t("حذف البيع؟", "Delete sale?"),
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
