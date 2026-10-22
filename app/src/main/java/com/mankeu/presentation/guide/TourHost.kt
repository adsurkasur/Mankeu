package com.mankeu.presentation.guide

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.mankeu.presentation.components.SpotlightTour

/** Offer and seen state of page tours, shared by the shell and by sheets that carry a tour of their own. */
class TourController(
    private val shouldOffer: (Tour) -> Boolean,
    private val markSeen: (Tour) -> Unit,
    val openHelp: (HelpArticle) -> Unit,
) {
    /**
     * One welcome on Beranda, nothing on other pages or sheets: offering a guide on every first visit
     * put five cards in front of a new owner before the first record (owner decision after the
     * audit walk, 2026-10-22). Every page keeps its tour behind the ? button.
     */
    fun offer(tour: Tour): Boolean = tour == Tour.HOME && shouldOffer(tour)
    fun seen(tour: Tour) = markSeen(tour)
}

val LocalTourController = staticCompositionLocalOf<TourController?> { null }

/** A tour that lives inside a bottom sheet. [running] is also set by the sheet's own ? button. */
class SheetTourState(val tour: Tour) {
    var running by mutableStateOf(false)
}

@Composable
fun rememberSheetTour(tour: Tour) = remember(tour) { SheetTourState(tour) }

/** Place inside the sheet's root `Box`, after its content: the offer card on top, then the tour. */
@Composable
fun BoxScope.SheetTour(state: SheetTourState) {
    val controller = LocalTourController.current ?: return
    val tour = state.tour
    if (state.running) {
        SpotlightTour(
            steps = tour.steps,
            onDismiss = { state.running = false; controller.seen(tour) },
            modifier = Modifier.matchParentSize(),
            isBottomSheet = true,
            onLearnMore = { article -> state.running = false; controller.seen(tour); controller.openHelp(article) },
        )
    }
}
