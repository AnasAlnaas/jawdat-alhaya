package ly.jawdat.alhaya.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.ProductSize
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.BottleOf
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.DateBrown
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.Leaf
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.PopIn
import ly.jawdat.alhaya.ui.Reveal
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.bottleScale
import ly.jawdat.alhaya.ui.ltr
import ly.jawdat.alhaya.ui.shared
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.util.Files

// "Benefits & uses" of date vinegar: cards to read in the app, each one a picture that can be sent
// to WhatsApp, Telegram or anywhere else.

private val Gold = Color(0xFFB8782E)
private val PosterCream = Color(0xFFFFFBF2)
private val PosterWarm = Color(0xFFF8EBD3)
private val Ink = Color(0xFF2A2118)

@Composable
fun BenefitsScreen() {
    var kind by rememberSaveable { mutableStateOf(BenefitKind.BENEFIT) }
    val cards = remember(kind, ly.jawdat.alhaya.ui.Lang.arabic) { benefitCards(kind) }
    AppScreen(t("فوائد الخل واستخداماته", "Benefits & uses"), onBack = { Nav.back() }, help = GuideIds.BENEFITS) { pad ->
        ListBody(pad) {
            item { BenefitsHero() }
            item {
                ChipRow(
                    listOf(BenefitKind.BENEFIT to t("🌿 الفوائد", "🌿 Benefits"), BenefitKind.USE to t("🍽️ الاستخدامات", "🍽️ Uses")),
                    kind,
                ) { kind = it }
            }
            // Two cards per row.
            cards.withIndex().chunked(2).forEach { row ->
                item(key = "$kind-${row.first().index}") {
                    Row(Modifier.animateItem().fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (i, c) -> BenefitTile(kind, i, c, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The three bottles bobbing over a short line about sharing. */
@Composable
private fun BenefitsHero() {
    val tr = rememberInfiniteTransition(label = "hero")
    val bob by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    AppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(t("خل التمر الطبيعي", "Natural date vinegar"), style = MaterialTheme.typography.titleLarge)
                Text(
                    t("اضغط أي بطاقة لعرضها كصورة ومشاركتها على واتساب أو تيليجرام.", "Tap any card to see it as a picture and share it on WhatsApp or Telegram."),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            ProductSize.all.forEachIndexed { i, size ->
                BottleOf(
                    size, 96.dp * bottleScale(size), 0.85f,
                    Modifier.padding(start = 6.dp).graphicsLayer { translationY = -6.dp.toPx() * if (i % 2 == 0) bob else 1f - bob },
                )
            }
        }
    }
}

@Composable
private fun BenefitTile(kind: String, index: Int, c: BenefitCard, modifier: Modifier) {
    AppCard(onClick = { Nav.go(Screen.BenefitDetail(kind, index)) }, modifier = modifier.shared("benefit-$kind-$index")) {
        PopIn(delayMs = index * 60) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(PosterWarm), contentAlignment = Alignment.Center) {
                Text(c.emoji, fontSize = 26.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(c.tag, style = MaterialTheme.typography.labelMedium, color = Gold)
        Text(c.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(c.accent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------- One card as a picture ----------

@Composable
fun BenefitDetailScreen(kind: String, index: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cards = remember(kind, ly.jawdat.alhaya.ui.Lang.arabic) { benefitCards(kind) }
    val pager = rememberPagerState(initialPage = index.coerceIn(0, cards.lastIndex)) { cards.size }
    // Each page records itself into a layer, so "share" can turn the page on screen into a picture.
    val layers = remember { HashMap<Int, GraphicsLayer>() }

    AppScreen(
        if (kind == BenefitKind.USE) t("الاستخدامات", "Uses") else t("الفوائد", "Benefits"),
        onBack = { Nav.back() }, help = GuideIds.BENEFITS,
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            HorizontalPager(pager, Modifier.weight(1f), pageSpacing = 12.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) { page ->
                val layer = rememberGraphicsLayer()
                layers[page] = layer
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(
                        Modifier.shared("benefit-$kind-$page").clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    ) {
                        Box(
                            Modifier.drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                        ) { Poster(cards[page], kind) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            // Page dots: the current one stretches into a pill.
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                cards.indices.forEach { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 22.dp else 7.dp, spring(dampingRatio = 0.6f), label = "dot")
                    Box(
                        Modifier.padding(horizontal = 3.dp).width(w).height(7.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) Gold else MaterialTheme.colorScheme.outlineVariant)
                            .clickable { scope.launch { pager.animateScrollToPage(i) } },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val page = pager.currentPage
                        val layer = layers[page] ?: return@Button
                        scope.launch {
                            val bmp = layer.toImageBitmap().asAndroidBitmap()
                            Files.shareImage(context, bmp, "jawdat_${kind}_${page + 1}")
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Ic(R.drawable.ic_share, Modifier.size(18.dp))
                    Text("  " + t("مشاركة كصورة", "Share as picture"))
                }
                OutlinedButton(onClick = { Files.shareText(context, benefitText(cards[pager.currentPage], kind)) }, modifier = Modifier.height(52.dp)) {
                    Text(t("نص", "Text"))
                }
            }
        }
    }
}

/**
 * The picture itself, in the style of the business's posters: logo and name on top, the topic,
 * a two-tone headline, the points on a white card, and the bottle with how to order at the bottom.
 */
@Composable
private fun Poster(c: BenefitCard, kind: String) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PosterCream, PosterWarm)))) {
        PalmLeaves(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.logo), null, Modifier.size(42.dp)) }
                Text("  " + t("جودة الحياة", "Jawdat Al-Hayah"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    t("خل التمر الطبيعي", "Natural date vinegar"),
                    Modifier.border(BorderStroke(1.5.dp, Gold), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Ink,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(2.dp).background(Gold))
                Text("  " + c.tag, style = MaterialTheme.typography.titleSmall, color = Gold, fontWeight = FontWeight.SemiBold)
            }
            Reveal {
                Column {
                    Text(c.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text(c.accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Gold)
                }
            }
            if (c.recipe.isNotEmpty()) Recipe(c.recipe)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f))
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                c.points.forEachIndexed { i, p ->
                    Reveal(delayMs = 100 + i * 90) {
                        Row {
                            Box(Modifier.padding(top = 7.dp).size(9.dp).clip(CircleShape).background(Gold.copy(alpha = 0.8f)))
                            Text("  $p", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    c.how?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Ink) }
                    if (kind == BenefitKind.BENEFIT) Text(
                        t("محتوى توعوي ولا يغني عن استشارة مختص.", "General information, not a substitute for medical advice."),
                        style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.6f),
                    )
                    Text(
                        t("للطلب والاستفسار: واتساب ", "To order: WhatsApp ") + ltr(Brand.PHONE) +
                            t(" · فيسبوك وتيليجرام: ", " · Facebook & Telegram: ") + ltr(Brand.HANDLE),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DateBrown,
                    )
                }
                BottleOf(ProductSize.SMALL, 120.dp, 0.85f, Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Ingredients joined by "+" and "=", as on the posters; the vinegar is the drawn bottle with two spoons. */
@Composable
private fun Recipe(parts: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        parts.forEachIndexed { i, (art, label) ->
            if (i > 0) PopIn(delayMs = 150 + i * 220) {
                Text(if (i == parts.lastIndex) "=" else "+", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Leaf)
            }
            PopIn(delayMs = i * 220, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) {
                        if (art == BOTTLE) Row(verticalAlignment = Alignment.Bottom) {
                            BottleOf(ProductSize.TINY, 40.dp, 0.85f, wave = false)
                            Text("🥄🥄", fontSize = 20.sp)
                        } else Text(art, fontSize = 40.sp)
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = Ink)
                }
            }
        }
    }
}

/** Soft palm leaves in two corners of the poster. */
@Composable
private fun PalmLeaves(modifier: Modifier) {
    Canvas(modifier) {
        val leaf = Leaf.copy(alpha = 0.16f)
        fun frond(at: Offset, angle: Float, len: Float) = rotate(angle, at) {
            drawLine(leaf, at, Offset(at.x + len, at.y), 3.dp.toPx())
            for (k in 1..9) {
                val x = at.x + len * k / 10f
                val l = len * 0.22f * (1f - k / 12f)
                rotate(-40f, Offset(x, at.y)) { drawOval(leaf, Offset(x, at.y - l * 0.18f), Size(l, l * 0.36f)) }
                rotate(40f, Offset(x, at.y)) { drawOval(leaf, Offset(x, at.y - l * 0.18f), Size(l, l * 0.36f)) }
            }
        }
        frond(Offset(size.width, size.height * 0.12f), 160f, size.width * 0.45f)
        frond(Offset(size.width, size.height * 0.22f), 190f, size.width * 0.35f)
        frond(Offset(0f, size.height * 0.62f), -20f, size.width * 0.35f)
    }
}

private fun benefitText(c: BenefitCard, kind: String): String = buildString {
    appendLine("🌴 " + t("جودة الحياة — خل التمر الطبيعي", "Jawdat Al-Hayah — natural date vinegar"))
    appendLine()
    appendLine("${c.emoji} ${c.title} ${c.accent}")
    c.points.forEach { appendLine("• $it") }
    c.how?.let { appendLine(it) }
    if (kind == BenefitKind.BENEFIT) appendLine(t("محتوى توعوي ولا يغني عن استشارة مختص.", "General information, not a substitute for medical advice."))
    appendLine()
    append(t("للطلب: واتساب ", "To order: WhatsApp ") + Brand.PHONE + " · @" + Brand.HANDLE)
}
