package com.mankeu.presentation

import android.media.AudioManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Touch feedback: a short vibration and, when enabled, a short sound, on the moments that deserve one.
 *
 * Haptics go through Compose's [LocalHapticFeedback] and sound through `View.playSoundEffect`. Both
 * end in the platform's own feedback path, which already obeys the phone's "Touch vibration" and
 * "Touch sounds" settings, the ringer mode and the accessibility switches, and which maps each
 * [HapticFeedbackType] to the closest effect the device and API level support. No `Vibrator`, no
 * waveform of our own and no `VIBRATE` permission: the in-app switches can only make this app
 * quieter than the phone already is (D-085).
 *
 * The taxonomy is deliberately small, so each signature stays recognisable instead of turning into
 * background noise:
 */
enum class FeedbackEvent(internal val haptic: HapticFeedbackType, internal val sound: Int?) {
    /** A discrete choice: a tab, a segment, a filter, a date, a sheet dismissed. The lightest tick. */
    TAP(HapticFeedbackType.SegmentTick, SoundEffectConstants.CLICK),

    /** A long-press reached its threshold and a tooltip opened. */
    LONG_PRESS(HapticFeedbackType.LongPress, null),

    /** A destructive action was started: delete, archive. Heavier than a tap. */
    WARN(HapticFeedbackType.GestureThresholdActivate, null),

    /** Something was written: a transaction saved, an import committed, a backup created. */
    CONFIRM(HapticFeedbackType.Confirm, SoundEffectConstants.CLICK),

    /** The app refused: form validation or a ledger rule. */
    REJECT(HapticFeedbackType.Reject, AudioManager.FX_KEYPRESS_INVALID),
}

class MankeuFeedback(
    private val haptic: HapticFeedback?,
    private val view: View?,
    private val haptics: Boolean,
    private val sound: Boolean,
) {
    operator fun invoke(event: FeedbackEvent) {
        if (haptics) haptic?.performHapticFeedback(event.haptic)
        if (sound) event.sound?.let { view?.playSoundEffect(it) }
    }
}

val LocalFeedback = staticCompositionLocalOf { MankeuFeedback(null, null, haptics = false, sound = false) }

@Composable
fun rememberFeedback(haptics: Boolean, sound: Boolean): MankeuFeedback {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptic, view, haptics, sound) { MankeuFeedback(haptic, view, haptics, sound) }
}
