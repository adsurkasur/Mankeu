package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.mankeu.R
import com.mankeu.data.WorkspaceSelection
import java.util.Locale

enum class ThemeMode(val labelRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
}

/**
 * How much decorative animation to run. [REDUCED] keeps meaning-carrying transitions but shortens
 * them; [OFF] removes them entirely. Honoured by [com.mankeu.presentation.components]
 * and the screen-change transition in `MankeuApp`.
 */
enum class MotionMode(val labelRes: Int) {
    FULL(R.string.settings_motion_full),
    REDUCED(R.string.settings_motion_reduced),
    OFF(R.string.settings_motion_off),
}

enum class AppLanguage(val tag: String, val label: String) {
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English"),
}

private const val PREFS_NAME = "mankeu_ui_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_MOTION_MODE = "motion_mode"
private const val KEY_LANGUAGE = "language"
private const val KEY_ACCOUNT_NAME = "account_name"
private const val KEY_HAPTICS = "haptics_enabled"

private const val KEY_SOUND = "sound_enabled"
private const val KEY_BUSINESS = "selected_business"
private const val KEY_PROJECT = "selected_project"

private fun Context.prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

/** The usaha and unit last open, so the app reopens where the owner left it. */
fun Context.readSelection() = WorkspaceSelection(prefs().getString(KEY_BUSINESS, null), prefs().getString(KEY_PROJECT, null))

fun Context.writeSelection(selection: WorkspaceSelection) = prefs().edit {
    putString(KEY_BUSINESS, selection.businessId)
    putString(KEY_PROJECT, selection.projectId)
}

fun Context.readLanguage(): AppLanguage {
    val stored = prefs().getString(KEY_LANGUAGE, null)
    return AppLanguage.entries.firstOrNull { it.tag == stored } ?: AppLanguage.INDONESIAN
}

/**
 * Wraps a base context so every `stringResource` in the app resolves against the owner's chosen
 * language rather than the phone's. Applied in `MainActivity.attachBaseContext`, which is the
 * reliable pre-API-33 route for a per-app language (the framework `LocaleManager` only exists on
 * API 33+, and this app supports API 26+). Changing the language calls `Activity.recreate()`.
 */
fun Context.withAppLanguage(): Context {
    val locale = Locale.forLanguageTag(readLanguage().tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/**
 * Device-local display preferences (theme, motion, language). Deliberately backed by
 * [android.content.SharedPreferences] rather than a Room table: these are per-device UI choices,
 * not accounting data, and must stay out of the audited financial schema, its migrations, and its
 * export/backup contract.
 */
class AppPreferencesState internal constructor(context: Context) {
    private val appContext = context.applicationContext

    var themeMode: ThemeMode by mutableStateOf(
        appContext.prefs().getString(KEY_THEME_MODE, null)
            ?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
    )
        private set

    var motionMode: MotionMode by mutableStateOf(
        appContext.prefs().getString(KEY_MOTION_MODE, null)
            ?.let { raw -> runCatching { MotionMode.valueOf(raw) }.getOrNull() }
            ?: MotionMode.FULL,
    )
        private set

    var language: AppLanguage by mutableStateOf(appContext.readLanguage())
        private set

    /**
     * Touch vibration. On by default because the phone's own "Touch vibration" setting is the outer
     * switch — this one can only turn it off for this app, never on when the phone says no.
     */
    var hapticsEnabled: Boolean by mutableStateOf(appContext.prefs().getBoolean(KEY_HAPTICS, true))
        private set

    /** Touch sounds. Off by default: a bookkeeping app that chirps at every tap is a nuisance. */
    var soundEnabled: Boolean by mutableStateOf(appContext.prefs().getBoolean(KEY_SOUND, false))
        private set

    fun updateHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
        appContext.prefs().edit { putBoolean(KEY_HAPTICS, enabled) }
    }

    fun updateSound(enabled: Boolean) {
        soundEnabled = enabled
        appContext.prefs().edit { putBoolean(KEY_SOUND, enabled) }
    }

    /**
     * Display name for the local (not-yet-synced) account. Stored here rather than in the Room
     * schema for the same reason as the other preferences: it is device-local identity, not
     * audited financial data.
     */
    var accountName: String by mutableStateOf(appContext.prefs().getString(KEY_ACCOUNT_NAME, "").orEmpty())
        private set

    fun updateAccountName(name: String) {
        accountName = name
        appContext.prefs().edit { putString(KEY_ACCOUNT_NAME, name) }
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        appContext.prefs().edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun updateMotion(mode: MotionMode) {
        motionMode = mode
        appContext.prefs().edit { putString(KEY_MOTION_MODE, mode.name) }
    }

    /** Persists the language. The caller must recreate the Activity for it to take effect. */
    fun updateLanguage(newLanguage: AppLanguage) {
        language = newLanguage
        appContext.prefs().edit { putString(KEY_LANGUAGE, newLanguage.tag) }
    }
}

/**
 * The system-wide animator duration scale (Settings > Accessibility > Remove animations, or
 * Developer options). 0 means the user asked for no animation at all.
 */
@Composable
fun rememberSystemAnimationScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
}

@Composable
fun rememberAppPreferencesState(): AppPreferencesState {
    val context = LocalContext.current
    return remember { AppPreferencesState(context) }
}
