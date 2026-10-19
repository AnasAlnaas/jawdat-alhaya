package ly.jawdat.alhaya.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.EmptyState
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.t

/** Where "Go to this screen" leads from each guide section. */
private fun screenFor(id: String): Screen? = when (id) {
    GuideIds.HOME -> Screen.Home
    GuideIds.BATCHES, GuideIds.STAGES, GuideIds.BOTTLING -> Screen.Batches
    GuideIds.SALES -> Screen.Sales
    GuideIds.MARKETS -> Screen.Markets
    GuideIds.PRODUCTS -> Screen.Products
    GuideIds.STOCK -> Screen.Stock
    GuideIds.EXPENSES -> Screen.Expenses
    GuideIds.REPORTS -> Screen.Reports
    GuideIds.BACKUP, GuideIds.SETTINGS -> Screen.Settings
    else -> null
}

private val tabScreens = setOf(Screen.Home, Screen.Batches, Screen.Sales, Screen.Reports, Screen.More)

@Composable
fun GuideScreen(openSection: String?) {
    val sections = remember(Lang.arabic) { guideSections() }
    var query by rememberSaveable { mutableStateOf("") }
    val expanded = remember { mutableStateListOf<String>().apply { openSection?.let { add(it) } } }
    val shown = if (query.isBlank()) sections else sections.filter { s ->
        (listOf(s.title, s.intro) + s.steps + s.tips).any { it.contains(query.trim(), ignoreCase = true) }
    }
    val listState = rememberLazyListState()

    // Opened from a screen's "?" button: scroll to its section (after the 3 header items).
    LaunchedEffect(openSection) {
        val i = sections.indexOfFirst { it.id == openSection }
        if (i >= 0) listState.scrollToItem(i + 3)
    }

    AppScreen(t("دليل الاستخدام", "User guide"), onBack = { Nav.back() }) { pad ->
        ListBody(pad, listState) {
            item {
                Field(t("ابحث في الدليل", "Search the guide"), query, { query = it })
            }
            item {
                OutlinedButton(onClick = { Nav.go(Screen.Welcome) }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("عرض جولة الترحيب مرة أخرى", "Show the welcome tour again"))
                }
            }
            item {
                Text(
                    t("اضغط على أي قسم لفتحه.", "Tap any section to open it."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shown.isEmpty()) item { EmptyState(t("لا توجد نتائج", "Nothing found")) }
            itemsIndexed(shown, key = { _, s -> s.id }) { _, s ->
                val open = query.isNotBlank() || s.id in expanded
                GuideCard(s, open) { if (s.id in expanded) expanded.remove(s.id) else expanded.add(s.id) }
            }
        }
    }
}

@Composable
private fun GuideCard(s: GuideSection, open: Boolean, onToggle: () -> Unit) {
    AppCard(onClick = onToggle, color = if (open) MaterialTheme.colorScheme.primaryContainer else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) { Ic(s.icon, Modifier.size(20.dp), MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Text(s.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Ic(R.drawable.ic_expand, Modifier.size(22.dp).rotate(if (open) 180f else 0f))
        }
        if (open) {
            Spacer(Modifier.height(10.dp))
            Text(s.intro, style = MaterialTheme.typography.bodyLarge)
            s.steps.forEachIndexed { i, step ->
                Row(Modifier.padding(top = 8.dp)) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(step, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (s.tips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(t("نصائح", "Tips"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                s.tips.forEach { tip ->
                    Row(Modifier.padding(top = 6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(tip, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            screenFor(s.id)?.let { target ->
                TextButton(onClick = {
                    if (target in tabScreens) Nav.tab(target) else { Nav.tab(Screen.More); Nav.go(target) }
                }, modifier = Modifier.padding(top = 6.dp)) {
                    Text(t("اذهب إلى هذه الشاشة", "Go to this screen"))
                }
            }
        }
    }
}

// ---------- Welcome tour (first launch) ----------

private data class TourPage(val icon: Int?, val title: String, val text: String)

@Composable
fun WelcomeScreen() {
    val pages = remember(Lang.arabic) {
        listOf(
            TourPage(
                null,
                t("أهلاً بك في جودة الحياة", "Welcome to Jawdat Al-Hayah"),
                t("تطبيق لإدارة إنتاج وبيع خل التمر: من نقع التمر حتى البيع والتقارير.", "An app for running date-vinegar production and sales: from soaking the dates to selling and reports."),
            ),
            TourPage(
                R.drawable.ic_flask,
                t("الإنتاج", "Production"),
                t(
                    "تابع كل دفعة مرحلة بمرحلة: النقع، الغطاء القماشي، التصفية والترقيد، نقع الأعشاب، التعبئة. سجّل الكميات وأسعارها، وضع موعد متابعة ليصلك تذكير.",
                    "Follow each batch stage by stage: soaking, cloth cover, filtering & settling, herb soaking, bottling. Record quantities and prices, and set a check date to get a reminder.",
                ),
            ),
            TourPage(
                R.drawable.ic_cart,
                t("المبيعات والمحلات", "Sales & markets"),
                t(
                    "سجّل كل بيع في ثوانٍ: المحل، المنتج، الكمية، مدفوع أو يدفع لاحقاً. التطبيق يحسب المخزون وديون المحلات تلقائياً.",
                    "Record each sale in seconds: market, product, quantity, paid or later. The app keeps stock and market debts up to date for you.",
                ),
            ),
            TourPage(
                R.drawable.ic_chart,
                t("التقارير", "Reports"),
                t(
                    "ملخص الأسبوع والشهر: المبيعات، الربح، الديون، أفضل المنتجات والمحلات، ومدة كل مرحلة. أرسله PDF أو Excel.",
                    "Weekly and monthly summaries: sales, profit, debts, best products and markets, and how long each stage takes. Send it as PDF or Excel.",
                ),
            ),
            TourPage(
                R.drawable.ic_backup,
                t("احفظ نسخة احتياطية", "Keep a backup"),
                t(
                    "البيانات محفوظة على هذا الهاتف فقط. احفظ نسخة كل أسبوع في Google Drive من: المزيد ← الإعدادات.",
                    "Your data is kept on this phone only. Save a backup every week to Google Drive from More → Settings.",
                ),
            ),
            TourPage(
                R.drawable.ic_help,
                t("الدليل دائماً معك", "The guide is always there"),
                t(
                    "اضغط ؟ أعلى أي شاشة لشرحها، أو افتح المزيد ← دليل الاستخدام.",
                    "Tap ? at the top of any screen for help with it, or open More → User guide.",
                ),
            ),
        )
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    fun finish(openGuide: Boolean) {
        Prefs.seenWelcome = true
        Nav.back()
        if (openGuide) Nav.go(Screen.Guide(GuideIds.START))
    }
    androidx.activity.compose.BackHandler { finish(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = { finish(false) }) { Text(t("تخطي", "Skip")) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (p.icon == null) {
                    Image(painterResource(R.drawable.logo), null, Modifier.height(170.dp))
                } else {
                    Box(
                        Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Ic(p.icon, Modifier.size(56.dp), MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(32.dp))
                Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(p.text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Page dots
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { i ->
                Box(
                    Modifier.padding(horizontal = 4.dp).size(if (i == pager.currentPage) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        .clickable { scope.launch { pager.animateScrollToPage(i) } }
                )
            }
        }
        if (last) {
            Button(onClick = { finish(false) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(t("ابدأ", "Get started")) }
            TextButton(onClick = { finish(true) }, modifier = Modifier.fillMaxWidth()) { Text(t("اقرأ البدء السريع", "Read the quick start")) }
        } else {
            Button(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(t("التالي", "Next")) }
        }
    }
}
