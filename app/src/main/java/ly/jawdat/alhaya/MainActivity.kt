package ly.jawdat.alhaya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.jawdat.alhaya.ui.LocalScreenAnimation
import ly.jawdat.alhaya.ui.LocalSharedTransition
import ly.jawdat.alhaya.ui.screens.BenefitDetailScreen
import ly.jawdat.alhaya.ui.screens.BenefitsScreen
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.JawdatTheme
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.screens.BatchDetailScreen
import ly.jawdat.alhaya.ui.screens.BatchesScreen
import ly.jawdat.alhaya.ui.screens.ExpenseEditScreen
import ly.jawdat.alhaya.ui.screens.ExpensesScreen
import ly.jawdat.alhaya.ui.screens.GuideScreen
import ly.jawdat.alhaya.ui.screens.WelcomeScreen
import ly.jawdat.alhaya.ui.screens.HomeScreen
import ly.jawdat.alhaya.ui.screens.MarketDetailScreen
import ly.jawdat.alhaya.ui.screens.MarketEditScreen
import ly.jawdat.alhaya.ui.screens.MarketsScreen
import ly.jawdat.alhaya.ui.screens.MoreScreen
import ly.jawdat.alhaya.ui.screens.NewBatchScreen
import ly.jawdat.alhaya.ui.screens.ProductEditScreen
import ly.jawdat.alhaya.ui.screens.ProductsScreen
import ly.jawdat.alhaya.ui.screens.ReportsScreen
import ly.jawdat.alhaya.ui.screens.SaleEditScreen
import ly.jawdat.alhaya.ui.screens.SalesScreen
import ly.jawdat.alhaya.ui.screens.SettingsScreen
import ly.jawdat.alhaya.ui.screens.StageDetailScreen
import ly.jawdat.alhaya.ui.screens.StageTemplateScreen
import ly.jawdat.alhaya.ui.screens.StockScreen
import ly.jawdat.alhaya.ui.t

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && Prefs.tourSeen < Prefs.TOUR_VERSION && Nav.current != Screen.Welcome) Nav.go(Screen.Welcome)
        openFromIntent(intent)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            JawdatTheme {
                CompositionLocalProvider(LocalLayoutDirection provides if (Lang.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Root() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromIntent(intent)
    }

    /** A tap on a stage reminder opens that batch. */
    private fun openFromIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_BATCH, 0L) ?: 0L
        if (id != 0L) {
            Nav.tab(Screen.Batches)
            Nav.go(Screen.BatchDetail(id))
        }
    }

    companion object {
        const val EXTRA_BATCH = "batch_id"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Root() {
    val screen = Nav.current
    val showBar = Nav.stack.size == 1
    val scope = rememberCoroutineScope()

    // Swiping back from the screen edge shrinks the screen under the finger (Android's predictive
    // back); letting go finishes the move, sliding back cancels it.
    var dragged by remember { mutableStateOf<Screen?>(null) }
    val drag = remember { Animatable(0f) }
    var fromLeft by remember { mutableStateOf(true) }
    PredictiveBackHandler(enabled = Nav.stack.size > 1 || Nav.stack[0] != Screen.Home) { events ->
        dragged = Nav.current
        drag.snapTo(0f)
        try {
            events.collect { e ->
                fromLeft = e.swipeEdge == BackEventCompat.EDGE_LEFT
                drag.snapTo(e.progress)
            }
            Nav.back(gesture = true)
        } catch (e: CancellationException) {
            scope.launch { drag.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
            throw e
        }
    }
    LaunchedEffect(screen) {
        delay(500)
        if (dragged != null && dragged != screen) {
            dragged = null
            drag.snapTo(0f)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // With the bottom bar showing, it already keeps clear of the phone's navigation bar.
        Box(Modifier.weight(1f).then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)) {
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            SharedTransitionLayout {
                val shared = this
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        // Opening a screen slides it in from the reading side; going back slides it away.
                        val dir = Nav.direction * (if (rtl) -1 else 1)
                        when {
                            Nav.direction == -2 ->
                                (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                                    (fadeOut(tween(200)) + scaleOut(tween(260), targetScale = 0.85f))
                            dir == 0 ->
                                (fadeIn(tween(240)) + scaleIn(tween(300), initialScale = 0.98f)) togetherWith fadeOut(tween(160))
                            else ->
                                (slideInHorizontally(tween(340, easing = EaseOutCubic)) { w -> dir * w / 3 } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally(tween(340, easing = EaseOutCubic)) { w -> -dir * w / 5 } + fadeOut(tween(220)))
                        }
                    },
                    label = "screen",
                ) { s ->
                    val anim = this
                    CompositionLocalProvider(LocalSharedTransition provides shared, LocalScreenAnimation provides anim) {
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                if (s == dragged) {
                                    val p = drag.value
                                    val k = 1f - 0.1f * p
                                    scaleX = k
                                    scaleY = k
                                    translationX = (if (fromLeft) 1f else -1f) * p * 28.dp.toPx()
                                    shape = RoundedCornerShape((32 * p).dp)
                                    clip = p > 0f
                                }
                            },
                        ) { ScreenContent(s) }
                    }
                }
            }
        }
        AnimatedVisibility(showBar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { BottomBar() }
    }
}

@Composable
private fun ScreenContent(screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen()
        Screen.Batches -> BatchesScreen()
        Screen.Sales -> SalesScreen()
        Screen.Reports -> ReportsScreen()
        Screen.More -> MoreScreen()
        Screen.NewBatch -> NewBatchScreen()
        is Screen.BatchDetail -> BatchDetailScreen(screen.id)
        is Screen.StageDetail -> StageDetailScreen(screen.id)
        is Screen.SaleEdit -> SaleEditScreen(screen.id, screen.marketId)
        Screen.Markets -> MarketsScreen()
        is Screen.MarketDetail -> MarketDetailScreen(screen.id)
        is Screen.MarketEdit -> MarketEditScreen(screen.id)
        Screen.Products -> ProductsScreen()
        is Screen.ProductEdit -> ProductEditScreen(screen.id)
        Screen.Stock -> StockScreen()
        Screen.Expenses -> ExpensesScreen()
        is Screen.ExpenseEdit -> ExpenseEditScreen(screen.id)
        Screen.Settings -> SettingsScreen()
        Screen.StageTemplate -> StageTemplateScreen()
        is Screen.Guide -> GuideScreen(screen.section)
        Screen.Welcome -> WelcomeScreen()
        Screen.Benefits -> BenefitsScreen()
        is Screen.BenefitDetail -> BenefitDetailScreen(screen.kind, screen.index)
    }
}

@Composable
private fun BottomBar() {
    val root = Nav.stack[0]
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        listOf(
            Triple(Screen.Home, R.drawable.ic_home, t("الرئيسية", "Home")),
            Triple(Screen.Batches, R.drawable.ic_flask, t("الإنتاج", "Production")),
            Triple(Screen.Sales, R.drawable.ic_cart, t("المبيعات", "Sales")),
            Triple(Screen.Reports, R.drawable.ic_chart, t("التقارير", "Reports")),
            Triple(Screen.More, R.drawable.ic_menu, t("المزيد", "More")),
        ).forEach { (s, icon, label) ->
            val on = root == s
            // The chosen tab's icon hops up a little.
            val lift by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow), label = "tab")
            NavigationBarItem(
                selected = on,
                onClick = { Nav.tab(s) },
                icon = {
                    Ic(icon, Modifier.size(22.dp).graphicsLayer {
                        val k = 1f + 0.15f * lift
                        scaleX = k
                        scaleY = k
                        translationY = -3.dp.toPx() * lift
                    })
                },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}
