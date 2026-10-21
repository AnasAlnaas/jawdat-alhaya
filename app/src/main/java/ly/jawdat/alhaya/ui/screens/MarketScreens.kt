package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ly.jawdat.alhaya.data.Market
import ly.jawdat.alhaya.data.Payment
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.Badge
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
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.parseNum
import ly.jawdat.alhaya.ui.relative
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate

@Composable
fun MarketsScreen() {
    val markets = watch { Repo.markets(true) }
    val balances = watch { Repo.balances() }
    val views = watch { Repo.saleViews() }
    val lastSale = views.groupBy { it.sale.marketId }.mapValues { (_, l) -> l.maxOf { it.sale.date } }
    val bought = views.groupBy { it.sale.marketId }.mapValues { (_, l) -> l.sumOf { it.sale.total } }
    AppScreen(
        title = t("المحلات", "Markets"),
        help = GuideIds.MARKETS,
        onBack = { Nav.back() },
        fab = {
            ExtendedFloatingActionButton(onClick = { Nav.go(Screen.MarketEdit()) }, icon = { Ic(R.drawable.ic_add) }, text = { Text(t("محل جديد", "New market")) })
        },
    ) { pad ->
        ListBody(pad) {
            if (balances.isNotEmpty()) item {
                AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                    KeyValue(t("مجموع الديون على المحلات", "Total owed by markets"), money(balances.values.sum()), bold = true)
                }
            }
            if (markets.isEmpty()) item { EmptyState(t("لا توجد محلات بعد", "No markets yet"), t("أضف المحلات التي تبيع لها.", "Add the markets you sell to.")) }
            items(markets, key = { it.id }) { m ->
                AppCard(modifier = Modifier.animateItem(), onClick = { Nav.go(Screen.MarketDetail(m.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name + if (m.archived) t(" (مؤرشف)", " (archived)") else "", style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    m.phone.takeIf { it.isNotBlank() },
                                    lastSale[m.id]?.let { t("آخر بيع ", "Last sale ") + relative(it) },
                                ).joinToString(" · ").ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(bought[m.id] ?: 0.0), style = MaterialTheme.typography.bodyMedium)
                            balances[m.id]?.let { Badge(t("عليه ", "Owes ") + money(it), Warn) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketDetailScreen(id: Long) {
    val context = LocalContext.current
    val m = watch(id) { Repo.market(id) } ?: return
    val views = watch(id) { Repo.saleViews().filter { it.sale.marketId == id } }
    val payments = watch(id) { Repo.payments().filter { it.marketId == id } }
    val owed = views.sumOf { it.owed }
    var paying by remember { mutableStateOf(false) }
    var removePayment by remember { mutableStateOf<Payment?>(null) }

    AppScreen(
        title = m.name,
        help = GuideIds.MARKETS,
        onBack = { Nav.back() },
        actions = { IconButton(onClick = { Nav.go(Screen.MarketEdit(id)) }) { Ic(R.drawable.ic_edit) } },
        fab = {
            ExtendedFloatingActionButton(onClick = { Nav.go(Screen.SaleEdit(marketId = id)) }, icon = { Ic(R.drawable.ic_cart) }, text = { Text(t("بيع لهذا المحل", "Sell to this market")) })
        },
    ) { pad ->
        ListBody(pad) {
            if (m.address.isNotBlank() || m.phone.isNotBlank() || m.notes.isNotBlank()) item {
                AppCard {
                    if (m.address.isNotBlank()) KeyValue(t("العنوان", "Address"), m.address)
                    if (m.phone.isNotBlank()) {
                        KeyValue(t("الهاتف", "Phone"), m.phone)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            OutlinedButton(onClick = { Files.call(context, m.phone) }) { Ic(R.drawable.ic_phone, Modifier.size(18.dp)); Text("  " + t("اتصال", "Call")) }
                            OutlinedButton(onClick = { Files.whatsapp(context, m.phone) }) { Text(t("واتساب", "WhatsApp")) }
                        }
                    }
                    if (m.notes.isNotBlank()) Text(m.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item {
                StatRow(
                    Triple(t("مجموع المشتريات", "Total bought"), money(views.sumOf { it.sale.total }), null),
                    Triple(t("القوارير", "Bottles"), views.sumOf { it.sale.qty }.toString(), null),
                    Triple(t("عليه الآن", "Owes now"), money(owed), if (owed > 0.005) Warn else Good),
                )
            }
            if (owed > 0.005) item {
                Button(onClick = { paying = true }, modifier = Modifier.fillMaxWidth()) { Text(t("تسجيل دفعة من المحل", "Record a payment from this market")) }
            }
            if (owed > 0.005) item {
                OutlinedButton(onClick = {
                    val lines = views.filter { !it.isPaid }.sortedBy { it.sale.date }.joinToString("\n") {
                        "• ${date(it.sale.date)}: ${it.productName} ${it.sale.qty} × ${num(it.sale.unitPrice)} — ${t("المتبقي", "left")} ${money(it.owed)}"
                    }
                    Files.shareText(
                        context,
                        t("السلام عليكم ${m.name}،\nكشف حساب من جودة الحياة:\n", "Hello ${m.name},\nStatement from Jawdat Al-Hayah:\n") + lines +
                            t("\nالمجموع المتبقي: ", "\nTotal left to pay: ") + money(owed),
                    )
                }, modifier = Modifier.fillMaxWidth()) { Ic(R.drawable.ic_share, Modifier.size(18.dp)); Text("  " + t("إرسال كشف حساب", "Send statement")) }
            }

            item { SectionTitle(t("المبيعات", "Sales")) }
            if (views.isEmpty()) item { Text(t("لا توجد مبيعات لهذا المحل بعد.", "No sales to this market yet."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(views, key = { "s" + it.sale.id }) { Box(Modifier.animateItem()) { SaleCard(it) } }

            if (payments.isNotEmpty()) {
                item { SectionTitle(t("الدفعات المستلمة", "Payments received")) }
                items(payments, key = { "p" + it.id }) { p ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(money(p.amount), style = MaterialTheme.typography.titleSmall, color = Good)
                                Text(
                                    date(p.date) + " · " + if (p.saleId != null) t("مع البيع", "with the sale") else t("سداد دين", "debt payment"),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (p.notes.isNotBlank()) Text(p.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            if (p.saleId == null) IconButton(onClick = { removePayment = p }) { Ic(R.drawable.ic_delete, Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (paying) PaymentDialog(id, owed) { paying = false }
    removePayment?.let { p ->
        ConfirmDialog(t("حذف الدفعة؟", "Delete payment?"), money(p.amount) + " · " + date(p.date), t("حذف", "Delete"), { Repo.deletePayment(p) }, { removePayment = null })
    }
}

@Composable
private fun PaymentDialog(marketId: Long, owed: Double, onDone: () -> Unit) {
    var amount by remember { mutableStateOf(num(owed)) }
    var d by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    val a = parseNum(amount) ?: 0.0
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(t("دفعة من المحل", "Payment from market")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("عليه ", "Owes ") + money(owed))
                Field(t("المبلغ المستلم", "Amount received"), amount, { amount = it }, number = true)
                DateField(t("التاريخ", "Date"), d, { it?.let { x -> d = x } })
                Field(t("ملاحظات", "Notes"), notes, { notes = it })
                Text(t("تُخصم من أقدم المبيعات أولاً.", "It pays off the oldest sales first."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = a > 0, onClick = { Repo.addPayment(Payment(date = d, marketId = marketId, amount = a, notes = notes)); onDone() }) {
                Text(t("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(t("إلغاء", "Cancel")) } },
    )
}

@Composable
fun MarketEditScreen(id: Long) {
    val existing = remember(id) { if (id != 0L) Repo.market(id) else null }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(existing?.phone ?: "") }
    var address by rememberSaveable { mutableStateOf(existing?.address ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var archived by rememberSaveable { mutableStateOf(existing?.archived ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val used = remember(id) { id != 0L && Repo.marketIsUsed(id) }

    AppScreen(if (id == 0L) t("محل جديد", "New market") else t("تعديل المحل", "Edit market"), onBack = { Nav.back() }, help = GuideIds.MARKETS) { pad ->
        FormBody(pad) {
            Field(t("اسم المحل", "Market name"), name, { name = it })
            Field(t("الهاتف", "Phone"), phone, { phone = it }, phone = true)
            Field(t("العنوان / المنطقة", "Address / area"), address, { address = it })
            Field(t("ملاحظات", "Notes"), notes, { notes = it }, singleLine = false)
            if (id != 0L) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("أرشفة (لم يعد يشتري)", "Archive (no longer buying)"), Modifier.weight(1f))
                Switch(archived, { archived = it })
            }
            Button(enabled = name.isNotBlank(), onClick = {
                Repo.saveMarket(Market(id = id, name = name, phone = phone, address = address, notes = notes, archived = archived)); Nav.back()
            }, modifier = Modifier.fillMaxWidth()) { Text(t("حفظ", "Save")) }
            if (id != 0L) {
                if (used) Text(
                    t("لهذا المحل مبيعات مسجّلة، لذا يمكن أرشفته فقط.", "This market has sales, so it can only be archived."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else TextButton(onClick = { confirmDelete = true }) { Text(t("حذف المحل", "Delete market"), color = Danger) }
            }
        }
    }
    if (confirmDelete) ConfirmDialog(t("حذف المحل؟", "Delete market?"), name, t("حذف", "Delete"), {
        Repo.deleteMarket(id); Nav.tab(Screen.More); Nav.go(Screen.Markets)
    }, { confirmDelete = false })
}
