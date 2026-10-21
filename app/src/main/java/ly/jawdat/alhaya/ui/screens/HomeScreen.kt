package ly.jawdat.alhaya.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import ly.jawdat.alhaya.data.Product
import ly.jawdat.alhaya.data.ProductSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.shared
import ly.jawdat.alhaya.ui.BottleOf
import ly.jawdat.alhaya.ui.bottleScale
import ly.jawdat.alhaya.ui.Reveal
import ly.jawdat.alhaya.ui.animatedNumber
import ly.jawdat.alhaya.ui.sizeName
import java.time.LocalTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.BatchStatus
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.logic.Period
import ly.jawdat.alhaya.logic.Reports
import ly.jawdat.alhaya.reminders.Reminders
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.Danger
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
import ly.jawdat.alhaya.ui.pct
import ly.jawdat.alhaya.ui.relative
import ly.jawdat.alhaya.ui.label
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.ui.title
import ly.jawdat.alhaya.ui.watch
import java.time.LocalDate

@Composable
fun HomeScreen() {
    val week = watch { Reports.build(Period.thisWeek()) }
    val due = watch { Reminders.dueStages() }
    val summaries = watch { Repo.batchSummaries() }
    val products = watch { Repo.products() }
    val active = summaries.filter { it.batch.status == BatchStatus.ACTIVE }
    val upcoming = active.mapNotNull { s -> s.current?.checkDate?.let { s to it } }
        .filter { it.second.isAfter(LocalDate.now()) }.sortedBy { it.second }.take(3)

    AppScreen(
        title = t("جودة الحياة", "Jawdat Al-Hayah"),
        help = GuideIds.HOME,
        actions = {
            Image(painterResource(R.drawable.logo), null, Modifier.padding(end = 12.dp).height(44.dp))
        },
    ) { pad ->
        ListBody(pad) {
            item { StockHero(products) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { Nav.go(Screen.SaleEdit()) }, modifier = Modifier.weight(1f).height(56.dp)) {
                        Ic(R.drawable.ic_cart, Modifier.size(20.dp)); Text("  " + t("بيع جديد", "New sale"))
                    }
                    FilledTonalButton(onClick = { Nav.go(Screen.NewBatch) }, modifier = Modifier.weight(1f).height(56.dp)) {
                        Ic(R.drawable.ic_flask, Modifier.size(20.dp)); Text("  " + t("دفعة جديدة", "New batch"))
                    }
                }
            }

            if (due.isNotEmpty() || week.lowProducts.isNotEmpty() || week.lowMaterials.isNotEmpty()) {
                item { SectionTitle(t("يحتاج انتباهك", "Needs your attention")) }
                due.forEach { (batch, stage) ->
                    item(key = "due${stage.id}") {
                        AppCard(onClick = { Nav.go(Screen.StageDetail(stage.id)) }, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(batch.label + ": " + stage.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                t("موعد المتابعة ", "Check date ") + date(stage.checkDate) + " (" + relative(stage.checkDate!!) + ")",
                                color = if (stage.checkDate.isBefore(LocalDate.now())) Danger else Warn,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                if (week.lowProducts.isNotEmpty() || week.lowMaterials.isNotEmpty()) item {
                    AppCard(onClick = { Nav.go(Screen.Stock) }) {
                        Text(t("مخزون منخفض", "Low stock"), style = MaterialTheme.typography.titleSmall, color = Warn)
                        week.lowProducts.forEach { KeyValue(it.title, it.stock.toString(), Warn) }
                        week.lowMaterials.forEach { KeyValue(it.name, "${num(it.stock)} ${it.unit}", Warn) }
                    }
                }
            }

            item { SectionTitle(t("هذا الأسبوع", "This week")) }
            item {
                StatRow(
                    Triple(t("المبيعات", "Sales"), money(week.revenue), null),
                    Triple(t("القوارير", "Bottles"), week.units.toString(), null),
                )
            }
            item {
                StatRow(
                    Triple(t("صافي الربح", "Net profit"), money(week.netProfit), if (week.netProfit < 0) Danger else null),
                    Triple(t("المستلم نقداً", "Cash received"), money(week.cashReceived), null),
                )
            }
            week.change(week.revenue, week.prevRevenue)?.let { c ->
                item {
                    Text(
                        t("المبيعات ${if (c >= 0) "أعلى" else "أقل"} بـ ${pct(kotlin.math.abs(c))} من الأسبوع الماضي", "Sales are ${pct(kotlin.math.abs(c))} ${if (c >= 0) "up" else "down"} on last week"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { TipOfTheDay() }

            item { SectionTitle(t("الوضع الآن", "Right now")) }
            item {
                AppCard {
                    KeyValue(t("قوارير جاهزة للبيع", "Bottles ready to sell"), week.bottlesInStock.toString(), bold = true)
                    KeyValue(t("دفعات قيد التحضير", "Batches in progress"), active.size.toString())
                    KeyValue(t("ديون على المحلات", "Owed by markets"), money(week.owedTotal), if (week.owedTotal > 0.005) Warn else MaterialTheme.colorScheme.onSurface)
                }
            }
            if (week.owedTotal > 0.005) item {
                AppCard(onClick = { Nav.go(Screen.Markets) }) {
                    Text(t("أكبر الديون", "Largest debts"), style = MaterialTheme.typography.titleSmall)
                    week.owedByMarket.take(3).forEach { KeyValue(it.label, money(it.value), Warn) }
                }
            }
            if (upcoming.isNotEmpty()) {
                item { SectionTitle(t("المراحل القادمة", "Coming up")) }
                upcoming.forEach { (s, d) ->
                    item(key = "up${s.batch.id}") {
                        AppCard(onClick = { Nav.go(Screen.BatchDetail(s.batch.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.batch.label + " · " + (s.current?.name ?: ""), style = MaterialTheme.typography.titleSmall)
                                    Text(date(d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(relative(d), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            if (summaries.isEmpty()) item {
                AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(t("أهلاً بك! للبدء:", "Welcome! To get started:"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        t(
                            "1. أضف منتجاتك وأسعارها (المزيد ← المنتجات)\n2. أضف المحلات التي تبيع لها\n3. ابدأ دفعة جديدة عند نقع التمر\n4. سجّل كل بيع من زر \"بيع جديد\"",
                            "1. Add your products and prices (More → Products)\n2. Add the markets you sell to\n3. Start a new batch when you soak the dates\n4. Record every sale with \"New sale\"",
                        ),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
                    )
                    androidx.compose.material3.TextButton(onClick = { Nav.go(Screen.Guide(GuideIds.START)) }) {
                        Text(t("اقرأ دليل الاستخدام", "Read the user guide"))
                    }
                }
            }
        }
    }
}

/**
 * The top of Home: a greeting and the bottles ready to sell, one drawn bottle per size whose level
 * follows how many there are.
 */
@Composable
private fun StockHero(products: List<Product>) {
    val bySize = ProductSize.all.associateWith { size -> products.filter { it.size == size }.sumOf { it.stock }.coerceAtLeast(0) }
    val other = products.filter { it.size !in ProductSize.all }.sumOf { it.stock }.coerceAtLeast(0)
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 4..11 -> t("صباح الخير", "Good morning")
        in 12..16 -> t("نهارك سعيد", "Good afternoon")
        else -> t("مساء الخير", "Good evening")
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))
            ).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Reveal { Text(greeting + " 🌿", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Reveal(delayMs = 120) {
                        Text(
                            t("قوارير جاهزة للبيع", "Bottles ready to sell"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        animatedNumber((bySize.values.sum() + other).toDouble()).toInt().toString(),
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ProductSize.all.forEach { size ->
                    HeroBottle(bySize[size] ?: 0, size)
                    if (size != ProductSize.all.last()) Spacer(Modifier.width(10.dp))
                }
            }
        }
    }
}

/** One benefit or use a day, ready to open and send to customers. */
@Composable
private fun TipOfTheDay() {
    val all = remember(Lang.arabic) {
        benefitCards(BenefitKind.BENEFIT).mapIndexed { i, c -> Triple(BenefitKind.BENEFIT, i, c) } +
            benefitCards(BenefitKind.USE).mapIndexed { i, c -> Triple(BenefitKind.USE, i, c) }
    }
    val (kind, i, c) = all[LocalDate.now().dayOfYear % all.size]
    val wobble by rememberInfiniteTransition(label = "tip").animateFloat(
        -6f, 6f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w",
    )
    AppCard(onClick = { Nav.go(Screen.BenefitDetail(kind, i)) }, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.shared("benefit-$kind-$i")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji, fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = wobble })
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(t("فائدة اليوم", "Tip of the day"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(c.title + " " + c.accent, style = MaterialTheme.typography.titleSmall)
                Text(t("اضغط لعرضها ومشاركتها كصورة", "Tap to see it and share it as a picture"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Ic(R.drawable.ic_share, Modifier.size(20.dp), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun HeroBottle(count: Int, size: String) {
    // 24 or more fills the bottle; fewer shows it lower, so a glance says when to bottle again.
    val fill by animateFloatAsState((count / 24f).coerceIn(0.06f, 0.9f), tween(1200, easing = FastOutSlowInEasing), label = "fill")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(86.dp), contentAlignment = Alignment.BottomCenter) {
            BottleOf(size, 84.dp * bottleScale(size), fill)
        }
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(sizeName(size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
