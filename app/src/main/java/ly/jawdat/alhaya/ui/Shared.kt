package ly.jawdat.alhaya.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

// Shared-element transitions: a card in a list and the top of the screen it opens carry the same
// key, so opening it grows the card into place instead of swapping screens.

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The screen animation the current screen is part of (set by MainActivity for each screen). */
val LocalScreenAnimation = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks this element as the same thing as the element with [key] on the screen before or after. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shared(key: Any): Modifier {
    val scope = LocalSharedTransition.current ?: return this
    val anim = LocalScreenAnimation.current ?: return this
    return with(scope) {
        this@shared.sharedBounds(
            rememberSharedContentState(key), anim,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
