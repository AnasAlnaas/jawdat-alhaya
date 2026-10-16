package ly.jawdat.alhaya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.JawdatTheme
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.screens.BatchDetailScreen
import ly.jawdat.alhaya.ui.screens.BatchesScreen
import ly.jawdat.alhaya.ui.screens.ExpenseEditScreen
import ly.jawdat.alhaya.ui.screens.ExpensesScreen
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

@Composable
private fun Root() {
    val screen = Nav.current
    BackHandler(enabled = Nav.stack.size > 1 || Nav.stack[0] != Screen.Home) { Nav.back() }
    val showBar = Nav.stack.size == 1
    Column(Modifier.fillMaxSize()) {
        // With the bottom bar showing, it already keeps clear of the phone's navigation bar.
        Box(Modifier.weight(1f).then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)) {
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
            }
        }
        if (showBar) BottomBar()
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
            NavigationBarItem(
                selected = root == s,
                onClick = { Nav.tab(s) },
                icon = { Ic(icon) },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}
