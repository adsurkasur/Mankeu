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
import com.mankeu.presentation.guide.Tour
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
private const val KEY_HAS_SEEN_TOUR = "has_seen_tour"
private const val KEY_HAS_SEEN_CORE_TOUR = "has_seen_core_tour"
private const val KEY_HAS_SEEN_HOME_TOUR = "has_seen_home_tour"
private const val KEY_HAS_SEEN_SETTINGS_TOUR = "has_seen_settings_tour"
private const val KEY_HAS_SEEN_SCOPE_TOUR = "has_seen_scope_tour"
private const val KEY_HAS_SEEN_ENTRY_TOUR = "has_seen_entry_tour"
private const val KEY_HAS_SEEN_REPORTS_TOUR = "has_seen_reports_tour"
private const val KEY_HAS_SEEN_IMPORT_TOUR = "has_seen_import_tour"
private const val KEY_HAPTICS = "haptics_enabled"
private const val KEY_TOUR_SEEN_PREFIX = "tour_seen_"
private const val KEY_TOUR_OFFERS = "tour_offers_enabled"

/** The first tour system's "seen" flags, read once as version 1 of the matching tour. */
private val LEGACY_TOUR_KEYS = mapOf(
    Tour.HOME to listOf(KEY_HAS_SEEN_HOME_TOUR, KEY_HAS_SEEN_CORE_TOUR, KEY_HAS_SEEN_TOUR),
    Tour.SETTINGS to listOf(KEY_HAS_SEEN_SETTINGS_TOUR),
    Tour.SCOPE to listOf(KEY_HAS_SEEN_SCOPE_TOUR),
    Tour.DATA to listOf(KEY_HAS_SEEN_IMPORT_TOUR),
    Tour.ENTRY to listOf(KEY_HAS_SEEN_ENTRY_TOUR),
    Tour.REPORTS to listOf(KEY_HAS_SEEN_REPORTS_TOUR),
)
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
     * Which version of each page tour the owner has seen (0 = none). A tour is offered while this
     * is below [Tour.version], so raising a tour's version offers the changed tour once, to
     * everyone, without touching the others. The boolean flags of the first tour system count as
     * having seen version 1.
     */
    var tourSeen: Map<Tour, Int> by mutableStateOf(Tour.entries.associateWith { readTourSeen(it) })
        private set

    /** The Pengaturan switch for the welcome card on Beranda (DEC-022: the only offer left). */
    var tourOffersEnabled: Boolean by mutableStateOf(appContext.prefs().getBoolean(KEY_TOUR_OFFERS, true))
        private set

    fun shouldOfferTour(tour: Tour): Boolean = tourOffersEnabled && (tourSeen[tour] ?: 0) < tour.version

    fun markTourSeen(tour: Tour) {
        tourSeen = tourSeen + (tour to tour.version)
        appContext.prefs().edit { putInt(KEY_TOUR_SEEN_PREFIX + tour.id, tour.version) }
    }

    /** Switching the welcome on shows it again on Beranda even if it was closed before. */
    fun updateTourOffers(enabled: Boolean) {
        tourOffersEnabled = enabled
        if (enabled) tourSeen = tourSeen + (Tour.HOME to 0)
        appContext.prefs().edit {
            putBoolean(KEY_TOUR_OFFERS, enabled)
            if (enabled) putInt(KEY_TOUR_SEEN_PREFIX + Tour.HOME.id, 0)
        }
    }

    /** Every tour unseen and the welcome on again: Beranda greets on the next visit. */
    fun resetAllTours() {
        tourSeen = Tour.entries.associateWith { 0 }
        tourOffersEnabled = true
        appContext.prefs().edit {
            Tour.entries.forEach { putInt(KEY_TOUR_SEEN_PREFIX + it.id, 0) }
            putBoolean(KEY_TOUR_OFFERS, true)
        }
    }

    private fun readTourSeen(tour: Tour): Int {
        val prefs = appContext.prefs()
        val key = KEY_TOUR_SEEN_PREFIX + tour.id
        if (prefs.contains(key)) return prefs.getInt(key, 0)
        val legacy = LEGACY_TOUR_KEYS[tour].orEmpty().any { prefs.getBoolean(it, false) }
        return if (legacy) 1 else 0
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
