package com.mankeu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mankeu.presentation.AppViewModel
import com.mankeu.presentation.MankeuApp
import com.mankeu.presentation.readSelection
import com.mankeu.presentation.writeSelection
import com.mankeu.presentation.withAppLanguage

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        viewModelFactory {
            initializer {
                with(application as MankeuApplication) {
                    AppViewModel(
                        repository, workbookImporter, backupManager, identity, cloudBackupClient,
                        syncController = syncController,
                        rememberedSelection = readSelection(),
                        onSelectionChanged = ::writeSelection,
                    )
                }
            }
        }
    }

    /**
     * Applies the owner's saved language before any resource is resolved. This is the reliable
     * per-app locale route for API 26+ (the framework `LocaleManager` is API 33+). Switching the
     * language in Pengaturan persists the choice and calls `recreate()`, which re-enters here.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() so the system splash is handed over cleanly
        // rather than flashing the app theme first.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MankeuApp(viewModel) }
    }
}
