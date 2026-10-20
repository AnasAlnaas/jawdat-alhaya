package ly.jawdat.alhaya.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.DateBrown
import ly.jawdat.alhaya.ui.Leaf
import ly.jawdat.alhaya.ui.Reveal
import ly.jawdat.alhaya.ui.TourArt
import ly.jawdat.alhaya.ui.TourIllustration
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.EmptyState
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.t
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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

// ---------- Welcome tour (first launch, and once after an update that changes it) ----------

private data class TourPage(val art: TourArt, val tint: Color, val title: String, val text: String)

@Composable
fun WelcomeScreen() {
    val teal = MaterialTheme.colorScheme.primary
    val pages = remember(Lang.arabic) {
        listOf(
            TourPage(
                TourArt.LOGO, teal,
                t("أهلاً بك في جودة الحياة", "Welcome to Jawdat Al-Hayah"),
                t("تطبيق لإدارة إنتاج وبيع خل التمر: من نقع التمر حتى البيع والتقارير.", "An app for running date-vinegar production and sales: from soaking the dates to selling and reports."),
            ),
            TourPage(
                TourArt.PRODUCTION, Leaf,
                t("الإنتاج", "Production"),
                t(
                    "تابع كل دفعة مرحلة بمرحلة: النقع، الغطاء القماشي، التصفية والترقيد، نقع الأعشاب، التعبئة. سجّل الكميات وأسعارها، وضع موعد متابعة ليصلك تذكير.",
                    "Follow each batch stage by stage: soaking, cloth cover, filtering & settling, herb soaking, bottling. Record quantities and prices, and set a check date to get a reminder.",
                ),
            ),
            TourPage(
                TourArt.SALES, DateBrown,
                t("قارورتان، وبيع في ثوانٍ", "Two bottles, a sale in seconds"),
                t(
                    "كل منتج بحجم من اثنين: صغيرة 330 مل أو كبيرة 1 لتر. سجّل البيع: المحل، المنتج، الكمية، مدفوع أو لاحقاً. إن بعت أكثر من المسجّل يُضاف الفرق إلى المخزون تلقائياً.",
                    "Each product comes in one of two sizes: small 330 ml or large 1 L. Record a sale: market, product, quantity, paid or later. Sell more than recorded and the difference is added to stock for you.",
                ),
            ),
            TourPage(
                TourArt.GIFTS, Leaf,
                t("الهدايا محسوبة", "Gifts are counted"),
                t(
                    "أهدِ قوارير وحدها أو مع بيع. تخرج من المخزون وتظهر تكلفتها في التقارير، فيبقى حساب التكاليف والإيرادات صحيحاً.",
                    "Give bottles on their own or with a sale. They leave stock and their cost shows in the reports, so costs and revenue stay right.",
                ),
            ),
            TourPage(
                TourArt.REPORTS, teal,
                t("التقارير", "Reports"),
                t(
                    "ملخص الأسبوع والشهر: المبيعات، الربح، الديون، الهدايا، أفضل المنتجات والمحلات، ومدة كل مرحلة. أرسله PDF أو Excel.",
                    "Weekly and monthly summaries: sales, profit, debts, gifts, best products and markets, and how long each stage takes. Send it as PDF or Excel.",
                ),
            ),
            TourPage(
                TourArt.BACKUP, teal,
                t("احفظ نسخة احتياطية", "Keep a backup"),
                t(
                    "البيانات محفوظة على هذا الهاتف فقط. احفظ نسخة كل أسبوع في Google Drive من: المزيد ← الإعدادات.",
                    "Your data is kept on this phone only. Save a backup every week to Google Drive from More → Settings.",
                ),
            ),
            TourPage(
                TourArt.GUIDE, DateBrown,
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
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val tint by animateColorAsState(pages[pager.currentPage].tint, tween(600), label = "tint")

    fun finish(openGuide: Boolean) {
        Prefs.tourSeen = Prefs.TOUR_VERSION
        Nav.back()
        if (openGuide) Nav.go(Screen.Guide(GuideIds.START))
    }
    androidx.activity.compose.BackHandler { finish(false) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(tint.copy(alpha = 0.22f), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
        )
    ) {
        FloatingSpecks(tint)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
                androidx.compose.animation.AnimatedVisibility(!last, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { finish(false) }) { Text(t("تخطي", "Skip")) }
                }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val p = pages[i]
                // How far this page is from the middle: 0 when centred, ±1 one page away.
                val offset = (pager.currentPage - i) + pager.currentPageOffsetFraction
                val side = if (rtl) -1f else 1f
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Reveal {
                        TourIllustration(
                            p.art,
                            Modifier.graphicsLayer {
                                // The picture lags behind the swipe and shrinks a little: a simple parallax.
                                translationX = offset * size.width * 0.45f * side
                                val k = 1f - (abs(offset) * 0.2f).coerceAtMost(0.2f)
                                scaleX = k
                                scaleY = k
                                alpha = 1f - abs(offset).coerceAtMost(1f) * 0.8f
                            },
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Reveal(delayMs = 120, modifier = Modifier.graphicsLayer { translationX = offset * size.width * 0.15f * side }) {
                        Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(14.dp))
                    Reveal(delayMs = 240, modifier = Modifier.graphicsLayer { translationX = offset * size.width * 0.08f * side }) {
                        Text(p.text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Page dots: the current one stretches into a pill.
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { i ->
                    val on = i == pager.currentPage
                    val w by animateDpAsState(if (on) 26.dp else 8.dp, spring(dampingRatio = 0.6f), label = "dot")
                    val c by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dotc")
                    Box(
                        Modifier.padding(horizontal = 4.dp).width(w).height(8.dp).clip(CircleShape).background(c)
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }
                    )
                }
            }
            Button(
                onClick = { if (last) finish(false) else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                AnimatedContent(last, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() }, label = "btn") { isLast ->
                    Text(if (isLast) t("ابدأ", "Get started") else t("التالي", "Next"))
                }
            }
            Box(Modifier.fillMaxWidth().height(48.dp)) {
                androidx.compose.animation.AnimatedVisibility(last, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
                    TextButton(onClick = { finish(true) }, modifier = Modifier.fillMaxWidth()) { Text(t("اقرأ البدء السريع", "Read the quick start")) }
                }
            }
        }
    }
}

/** Dates and leaves drifting slowly behind the tour. */
@Composable
private fun FloatingSpecks(tint: Color) {
    val tr = rememberInfiniteTransition(label = "specks")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "t")
    val specks = remember { List(14) { i -> Triple((i * 0.137f + 0.05f) % 1f, (i * 0.291f + 0.1f) % 1f, i) } }
    Canvas(Modifier.fillMaxSize()) {
        specks.forEach { (x0, y0, i) ->
            val drift = (t + i / 14f) % 1f
            val x = size.width * x0 + sin(drift * 2f * PI.toFloat() + i) * 18.dp.toPx()
            val y = size.height * ((y0 - drift * 0.25f + 1f) % 1f)
            val color = (if (i % 3 == 0) DateBrown else if (i % 3 == 1) Leaf else tint).copy(alpha = 0.12f)
            val r = (6 + i % 4 * 3).dp.toPx()
            if (i % 2 == 0) drawOval(color, Offset(x - r, y - r * 0.6f), Size(r * 2, r * 1.2f))
            else drawCircle(color, r * 0.7f, Offset(x, y))
        }
    }
}
