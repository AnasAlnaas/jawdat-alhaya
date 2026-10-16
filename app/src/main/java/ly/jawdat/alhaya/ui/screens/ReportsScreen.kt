package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.logic.Period
import ly.jawdat.alhaya.logic.Report
import ly.jawdat.alhaya.logic.Reports
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.BarRow
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ColumnChart
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.DateField
import ly.jawdat.alhaya.ui.DateBrown
import ly.jawdat.alhaya.ui.Divider
import ly.jawdat.alhaya.ui.Good
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.KeyValue
import ly.jawdat.alhaya.ui.Leaf
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.StatRow
import ly.jawdat.alhaya.ui.Warn
import ly.jawdat.alhaya.ui.date
import ly.jawdat.alhaya.ui.dayName
import ly.jawdat.alhaya.ui.days
import ly.jawdat.alhaya.ui.expenseCategoryName
import ly.jawdat.alhaya.ui.herbName
import ly.jawdat.alhaya.ui.itemKindName
import ly.jawdat.alhaya.ui.money
import ly.jawdat.alhaya.ui.num
import ly.jawdat.alhaya.ui.pct
import ly.jawdat.alhaya.ui.relative
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.watch
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    var range by rememberSaveable { mutableStateOf("week") }
    var customFrom by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var customTo by remember { mutableStateOf(LocalDate.now()) }
    val period = when (range) {
        "week" -> Period.thisWeek()
        "lastWeek" -> Period.lastWeek()
        "month" -> Period.thisMonth()
        "lastMonth" -> Period.lastMonth()
        "year" -> Period.thisYear()
        else -> Period(minOf(customFrom, customTo), maxOf(customFrom, customTo))
    }
    val r = watch(period) { Reports.build(period) }

    AppScreen(title = t("التقارير", "Reports")) { pad ->
        ListBody(pad) {
            item {
                ChipRow(
                    listOf(
                        "week" to t("هذا الأسبوع", "This week"),
                        "lastWeek" to t("الأسبوع الماضي", "Last week"),
                        "month" to t("هذا الشهر", "This month"),
                        "lastMonth" to t("الشهر الماضي", "Last month"),
                        "year" to t("هذه السنة", "This year"),
                        "custom" to t("فترة مخصصة", "Custom"),
                    ),
                    range,
                ) { range = it }
            }
            if (range == "custom") item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(t("من", "From"), customFrom, { it?.let { d -> customFrom = d } }, Modifier.weight(1f))
                    DateField(t("إلى", "To"), customTo, { it?.let { d -> customTo = d } }, Modifier.weight(1f))
                }
            }
            item {
                Text(
                    date(period.from) + " — " + date(period.to) + "  ·  " + days(period.days),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Files.shareReportPdf(context, r, t("جودة الحياة", "Jawdat Al-Hayah")) }) {
                        Ic(R.drawable.ic_share, Modifier.size(18.dp)); Text("  PDF")
                    }
                    OutlinedButton(onClick = { Files.shareSalesCsv(context, period.from, period.to) }) {
                        Ic(R.drawable.ic_share, Modifier.size(18.dp)); Text("  Excel")
                    }
                    OutlinedButton(onClick = { Files.shareText(context, summaryText(r)) }) { Text(t("ملخص نصي", "Text summary")) }
                }
            }

            // ----- Sales -----
            item { SectionTitle(t("المبيعات", "Sales")) }
            item {
                StatRow(
                    Triple(t("الإيرادات", "Revenue"), money(r.revenue), null),
                    Triple(t("القوارير", "Bottles"), r.units.toString(), null),
                )
            }
            item {
                StatRow(
                    Triple(t("عمليات البيع", "Sales made"), r.salesCount.toString(), null),
                    Triple(t("متوسط السعر", "Avg price"), money(r.avgUnitPrice), null),
                )
            }
            item {
                AppCard {
                    ChangeLine(t("الإيرادات", "Revenue"), r.change(r.revenue, r.prevRevenue), money(r.prevRevenue))
                    ChangeLine(t("القوارير", "Bottles"), r.change(r.units.toDouble(), r.prevUnits.toDouble()), r.prevUnits.toString())
                    ChangeLine(t("صافي الربح", "Net profit"), r.change(r.netProfit, r.prevNetProfit), money(r.prevNetProfit))
                    Text(t("مقارنة بالفترة السابقة بنفس الطول", "Compared with the previous period of the same length"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (r.daily.size in 2..62 && r.revenue > 0) item {
                AppCard {
                    Text(t("المبيعات اليومية", "Daily sales"), style = MaterialTheme.typography.titleSmall)
                    val labels = r.daily.mapIndexed { i, (d, _) -> if (r.daily.size <= 10 || i % (r.daily.size / 7 + 1) == 0) d.dayOfMonth.toString() else "" }
                    ColumnChart(r.daily.map { it.second }, labels)
                }
            }

            // ----- Profit -----
            item { SectionTitle(t("الربح والتكاليف", "Profit & costs")) }
            item {
                AppCard {
                    KeyValue(t("الإيرادات", "Revenue"), money(r.revenue))
                    KeyValue(t("− تكاليف الإنتاج في الفترة", "− Production costs in period"), money(r.productionCost))
                    KeyValue(t("− مصاريف أخرى", "− Other expenses"), money(r.expenses))
                    Divider()
                    KeyValue(t("صافي الربح", "Net profit"), money(r.netProfit), if (r.netProfit < 0) Danger else Good, bold = true)
                    if (r.revenue > 0) KeyValue(t("هامش الربح", "Profit margin"), pct(r.netProfit / r.revenue * 100))
                    Divider()
                    KeyValue(t("تكلفة القوارير المباعة (تقديري)", "Cost of bottles sold (est.)"), money(r.costOfGoodsSold))
                    KeyValue(t("الربح على المبيعات", "Profit on sales"), money(r.grossProfit))
                    KeyValue(t("التدفق النقدي (المستلم − المصروف)", "Cash flow (received − spent)"), money(r.cashFlow))
                    Text(
                        t("صافي الربح يقارن ما بعته بما صرفته في الفترة. الربح على المبيعات يستخدم تكلفة القارورة من الدفعات المنتهية.", "Net profit compares what you sold with what you spent in the period. Profit on sales uses the cost per bottle from finished batches."),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (r.productionByKind.isNotEmpty() || r.expensesByCategory.isNotEmpty()) item {
                AppCard {
                    Text(t("أين ذهب المال", "Where the money went"), style = MaterialTheme.typography.titleSmall)
                    val max = (r.productionByKind + r.expensesByCategory).maxOf { it.value }
                    r.productionByKind.forEach { BarRow(itemKindName(it.label), it.value, max, money(it.value), DateBrown) }
                    r.expensesByCategory.forEach { BarRow(expenseCategoryName(it.label), it.value, max, money(it.value), Leaf) }
                }
            }

            // ----- Breakdowns -----
            if (r.byProduct.isNotEmpty()) {
                item { SectionTitle(t("حسب المنتج", "By product")) }
                item {
                    AppCard {
                        val max = r.byProduct.maxOf { it.value }
                        r.byProduct.forEach { BarRow(it.label, it.value, max, "${money(it.value)} · ${it.units}") }
                        if (r.costPerBottle.isNotEmpty()) {
                            Divider()
                            Text(t("الربح التقديري لكل منتج", "Estimated profit per product"), style = MaterialTheme.typography.labelLarge)
                            r.byProduct.forEach { KeyValue(it.label, money(it.extra), if (it.extra < 0) Danger else Good) }
                        }
                    }
                }
            }
            if (r.byHerb.isNotEmpty()) {
                item { SectionTitle(t("حسب النوع (الأعشاب)", "By type (herbs)")) }
                item {
                    AppCard {
                        val max = r.byHerb.maxOf { it.value }
                        r.byHerb.forEach { BarRow(herbName(it.label), it.value, max, "${money(it.value)} · ${it.units}", Leaf) }
                    }
                }
            }
            if (r.byMarket.isNotEmpty()) {
                item { SectionTitle(t("حسب المحل", "By market")) }
                item {
                    AppCard {
                        val max = r.byMarket.maxOf { it.value }
                        r.byMarket.forEach { BarRow(it.label, it.value, max, "${money(it.value)} · ${it.units}", DateBrown) }
                    }
                }
            }
            if (r.revenue > 0) {
                item { SectionTitle(t("حسب يوم الأسبوع", "By day of week")) }
                item {
                    AppCard {
                        val max = r.byWeekday.maxOf { it.second }
                        r.byWeekday.forEach { (d, v) -> BarRow(dayName(d), v, max, money(v)) }
                    }
                }
            }
            if (r.slowProducts.isNotEmpty() || r.quietMarkets.isNotEmpty()) {
                item { SectionTitle(t("يحتاج متابعة", "Worth a look")) }
                item {
                    AppCard {
                        if (r.slowProducts.isNotEmpty()) {
                            Text(t("منتجات لم تُبع في هذه الفترة", "Products not sold in this period"), style = MaterialTheme.typography.labelLarge)
                            r.slowProducts.forEach { Text("• ${it.name} ${it.size}".trim(), style = MaterialTheme.typography.bodyMedium) }
                        }
                        if (r.quietMarkets.isNotEmpty()) {
                            if (r.slowProducts.isNotEmpty()) Divider()
                            Text(t("محلات لم تشترِ منذ أكثر من 30 يوماً", "Markets with no purchase for 30+ days"), style = MaterialTheme.typography.labelLarge)
                            r.quietMarkets.forEach { (m, last) ->
                                KeyValue(m.name, last?.let { relative(it) } ?: t("لم يشترِ بعد", "never bought"))
                            }
                        }
                    }
                }
            }

            // ----- Money owed -----
            item { SectionTitle(t("الديون (الآن)", "Money owed (now)")) }
            item {
                AppCard {
                    KeyValue(t("مجموع الديون", "Total owed"), money(r.owedTotal), if (r.owedTotal > 0.005) Warn else Good, bold = true)
                    KeyValue(t("غير مدفوع من مبيعات الفترة", "Unpaid from this period's sales"), money(r.unpaidFromPeriod))
                    KeyValue(t("المستلم نقداً في الفترة", "Cash received in period"), money(r.cashReceived))
                    r.owedByMarket.forEach { KeyValue("  " + it.label, money(it.value), Warn) }
                    if (r.oldestUnpaid.isNotEmpty()) {
                        Divider()
                        Text(t("أقدم المبيعات غير المدفوعة", "Oldest unpaid sales"), style = MaterialTheme.typography.labelLarge)
                        r.oldestUnpaid.forEach {
                            val age = ChronoUnit.DAYS.between(it.sale.date, LocalDate.now())
                            KeyValue("${it.marketName ?: "—"} · ${date(it.sale.date)} (${days(age)})", money(it.owed), if (age > 30) Danger else Warn)
                        }
                    }
                }
            }

            // ----- Production -----
            item { SectionTitle(t("الإنتاج", "Production")) }
            item {
                StatRow(
                    Triple(t("بدأت", "Started"), r.batchesStarted.toString(), null),
                    Triple(t("انتهت", "Finished"), r.batchesFinished.toString(), null),
                    Triple(t("جارية", "In progress"), r.batchesActive.toString(), null),
                )
            }
            item {
                AppCard {
                    KeyValue(t("قوارير أُنتجت في الفترة", "Bottles made in period"), r.bottlesMade.toString())
                    KeyValue(t("متوسط مدة الدفعة", "Average batch length"), r.avgBatchDays?.let { days(it.toLong()) } ?: "—")
                    KeyValue(t("قوارير لكل كجم تمر", "Bottles per kg of dates"), r.yieldPerKg?.let { num(it) } ?: "—")
                    if (r.stageTimings.isNotEmpty()) {
                        Divider()
                        Text(t("متوسط مدة كل مرحلة (كل الدفعات)", "Average time per stage (all batches)"), style = MaterialTheme.typography.labelLarge)
                        val max = r.stageTimings.maxOf { it.avgDays }
                        r.stageTimings.forEach { BarRow(it.name, it.avgDays, max, t("${num(it.avgDays)} يوم", "${num(it.avgDays)} d"), Leaf) }
                    }
                    if (r.yieldPerKg == null) Text(
                        t("لحساب الإنتاجية: سجّل التمر بصنف \"تمر\" ووحدة \"كجم\"، وسجّل القوارير المنتجة.", "For yield: record dates with kind \"Dates\" and unit \"kg\", and record the bottles made."),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ----- Stock -----
            item { SectionTitle(t("المخزون (الآن)", "Stock (now)")) }
            item {
                AppCard {
                    KeyValue(t("قوارير جاهزة", "Bottles ready"), r.bottlesInStock.toString(), bold = true)
                    KeyValue(t("قيمتها بسعر البيع", "Value at selling price"), money(r.stockValue))
                    r.lowProducts.forEach { KeyValue("⚠ ${it.name} ${it.size}".trim(), it.stock.toString(), Warn) }
                    r.lowMaterials.forEach { KeyValue("⚠ ${it.name}", "${num(it.stock)} ${it.unit}", Warn) }
                }
            }
        }
    }
}

@Composable
private fun ChangeLine(label: String, change: Double?, before: String) {
    val (text, color) = when {
        change == null -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
        change >= 0 -> "▲ ${pct(change)}" to Good
        else -> "▼ ${pct(-change)}" to Danger
    }
    KeyValue("$label  (${t("قبل", "before")}: $before)", text, color)
}

private fun summaryText(r: Report): String = buildString {
    appendLine(t("📊 جودة الحياة — ملخص ${date(r.period.from)} إلى ${date(r.period.to)}", "📊 Jawdat Al-Hayah — summary ${date(r.period.from)} to ${date(r.period.to)}"))
    appendLine(t("المبيعات: ", "Sales: ") + money(r.revenue) + t(" (${r.units} قارورة)", " (${r.units} bottles)"))
    appendLine(t("المستلم نقداً: ", "Cash received: ") + money(r.cashReceived))
    appendLine(t("تكاليف الإنتاج: ", "Production costs: ") + money(r.productionCost))
    appendLine(t("مصاريف أخرى: ", "Other expenses: ") + money(r.expenses))
    appendLine(t("صافي الربح: ", "Net profit: ") + money(r.netProfit))
    appendLine(t("الديون على المحلات: ", "Owed by markets: ") + money(r.owedTotal))
    r.byProduct.firstOrNull()?.let { appendLine(t("الأكثر مبيعاً: ", "Best seller: ") + it.label) }
    r.byMarket.firstOrNull()?.let { appendLine(t("أفضل محل: ", "Top market: ") + it.label) }
    appendLine(t("قوارير جاهزة: ", "Bottles ready: ") + r.bottlesInStock)
    append(t("دفعات قيد التحضير: ", "Batches in progress: ") + r.batchesActive)
}
