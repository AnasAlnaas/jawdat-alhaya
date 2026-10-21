package ly.jawdat.alhaya.ui

import androidx.compose.runtime.mutableStateListOf

sealed interface Screen {
    // Bottom-bar tabs
    data object Home : Screen
    data object Batches : Screen
    data object Sales : Screen
    data object Reports : Screen
    data object More : Screen

    // Production
    data object NewBatch : Screen
    data class BatchDetail(val id: Long) : Screen
    data class StageDetail(val id: Long) : Screen

    // Sales & markets
    data class SaleEdit(val id: Long = 0, val marketId: Long? = null) : Screen
    data object Markets : Screen
    data class MarketDetail(val id: Long) : Screen
    data class MarketEdit(val id: Long = 0) : Screen

    // Other lists
    data object Products : Screen
    data class ProductEdit(val id: Long = 0) : Screen
    data object Stock : Screen
    data object Expenses : Screen
    data class ExpenseEdit(val id: Long = 0) : Screen
    data object Settings : Screen
    data object StageTemplate : Screen

    // Help
    data class Guide(val section: String? = null) : Screen
    data object Welcome : Screen

    // Benefits & uses of the vinegar, to read and share as pictures
    data object Benefits : Screen
    data class BenefitDetail(val kind: String, val index: Int) : Screen
}

val tabs = listOf(Screen.Home, Screen.Batches, Screen.Sales, Screen.Reports, Screen.More)

/** A simple back stack: the first entry is always a tab. */
object Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)

    val current: Screen get() = stack.last()

    /**
     * Which way the last move went, for the screen animation: 1 forward, -1 back, 0 a tab switch,
     * -2 back by the swipe gesture (the screen has already shrunk under the finger).
     */
    var direction = 0
        private set

    /** A batch just created: its page opens with a celebration once. */
    var pendingCelebration: Long? = null

    fun go(s: Screen) {
        direction = 1
        stack.add(s)
    }

    fun back(gesture: Boolean = false): Boolean {
        direction = if (gesture) -2 else -1
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (stack[0] != Screen.Home) {
            stack[0] = Screen.Home
            return true
        }
        return false
    }

    fun tab(s: Screen) {
        direction = 0
        stack.clear()
        stack.add(s)
    }

    /** Replace the current screen (e.g. after creating a batch, open it instead of the form). */
    fun replace(s: Screen) {
        direction = 0
        stack[stack.lastIndex] = s
    }
}
