package com.arinahub.arinaagri.calculator.presentation

import android.accounts.Account
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.arinahub.arinaagri.calculator.auth.AuthUser
import com.arinahub.arinaagri.calculator.backup.cloud.DriveBackupFile
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.arinahub.arinaagri.calculator.presentation.components.ArinaAppLogo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.domain.BookMode
import com.arinahub.arinaagri.calculator.presentation.components.SpotlightTour
import com.arinahub.arinaagri.calculator.presentation.components.TourTarget
import com.arinahub.arinaagri.calculator.presentation.components.TourTargetRegistry
import com.arinahub.arinaagri.calculator.presentation.components.LocalTourTargets
import com.arinahub.arinaagri.calculator.presentation.components.tourTarget
import com.arinahub.arinaagri.calculator.presentation.components.CoreTourSteps
import com.arinahub.arinaagri.calculator.presentation.components.SettingsTourSteps
import com.arinahub.arinaagri.calculator.presentation.components.ScopeTourSteps
import com.arinahub.arinaagri.calculator.presentation.components.EntryTourSteps
import com.arinahub.arinaagri.calculator.presentation.components.ImportTourSteps
import com.arinahub.arinaagri.calculator.presentation.components.ReportsTourSteps
import com.arinahub.arinaagri.calculator.presentation.screens.BudgetScreen
import com.arinahub.arinaagri.calculator.presentation.screens.DataScreen
import com.arinahub.arinaagri.calculator.presentation.screens.EntryScreen
import com.arinahub.arinaagri.calculator.presentation.screens.HomeScreen
import com.arinahub.arinaagri.calculator.presentation.screens.ManageScopeSheet
import com.arinahub.arinaagri.calculator.presentation.screens.ReportsScreen
import com.arinahub.arinaagri.calculator.presentation.screens.RestorePreviewDialog
import com.arinahub.arinaagri.calculator.presentation.screens.SettingsScreen
import java.time.LocalDate

/**
 * App shell: destination switching, the farm/field/book scope header, the Berkas dan Data and
 * Pengaturan overlays, and file-picker wiring. Screen content lives in `presentation/screens/`.
 *
 * Navigation model: Material 3 NavigationBar on compact phone viewports, and an adaptive
 * NavigationRail on expanded tablet/desktop viewports.
 */

enum class Destination(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Default.Home),
    ENTRY(R.string.nav_entry, Icons.AutoMirrored.Filled.ReceiptLong),
    BUDGET(R.string.nav_budget, Icons.Default.Calculate),
    REPORTS(R.string.nav_reports, Icons.Default.Assessment),
}

enum class Overlay { NONE, DATA, SETTINGS }

@Composable
fun ArinaCalculatorApp(viewModel: AppViewModel) {
    val prefs = rememberAppPreferencesState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val motion = ArinaMotion.resolve(prefs.motionMode, rememberSystemAnimationScale())

    // Theme changes are animated inside ArinaTheme by interpolating the colour scheme, which
    // costs no state at all. Language is different: it goes through Activity.recreate() — see
    // onLanguageChange below for why the in-composition approach was tried and withdrawn.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val recoveryAvailable by viewModel.recoveryAvailable.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val authUser by viewModel.authUser.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    // A sheet, not a route: ModalBottomSheet handles its own back press and scrim.
    var manageScope by rememberSaveable { mutableStateOf(false) }
    var activeTourType by rememberSaveable { mutableStateOf<String?>(if (!prefs.hasSeenCoreTour) "CORE" else null) }
    // Set the instant the owner picks a language, cleared only by the Activity being rebuilt.
    // recreate() is not instantaneous — resources reload and the whole tree recomposes — and
    // without this the screen simply froze on the old language for a beat and then snapped.
    var switchingLanguage by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingRestorePassword by remember { mutableStateOf<CharArray?>(null) }
    val tourTargets = remember { TourTargetRegistry() }

    LaunchedEffect(overlay) {
        if (overlay == Overlay.SETTINGS && !prefs.hasSeenSettingsTour && activeTourType == null) {
            activeTourType = "SETTINGS"
        } else if (overlay == Overlay.DATA && !prefs.hasSeenImportTour && activeTourType == null) {
            activeTourType = "IMPORT"
        }
    }

    LaunchedEffect(manageScope) {
        if (manageScope && !prefs.hasSeenScopeTour && activeTourType == null) {
            activeTourType = "SCOPE"
        }
    }

    // Each destination teaches itself once, the first time it is opened. Gated on the core tour
    // being finished so a first run is not two overlapping tours, and on no overlay being open so
    // the spotlight never points at a control that is currently behind a sheet.
    LaunchedEffect(destination, overlay, manageScope) {
        if (activeTourType != null || overlay != Overlay.NONE || manageScope || !prefs.hasSeenCoreTour) {
            return@LaunchedEffect
        }
        when (destination) {
            Destination.ENTRY -> if (!prefs.hasSeenEntryTour) activeTourType = "ENTRY"
            Destination.REPORTS -> if (!prefs.hasSeenReportsTour) activeTourType = "REPORTS"
            else -> Unit
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogleIdToken(idToken)
            } else {
                viewModel.setAuthenticatedUser(
                    com.arinahub.arinaagri.calculator.auth.AuthUser(
                        uid = account.id ?: account.email ?: "google-user",
                        email = account.email,
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString(),
                    )
                )
            }
        }.onFailure { err ->
            val apiEx = err as? ApiException
            val statusCode = apiEx?.statusCode
            // Status 12501 indicates the user voluntarily dismissed the account picker
            if (statusCode != 12501) {
                val errorText = when (statusCode) {
                    10 -> "Gagal masuk Google (Kode 10: DEVELOPER_ERROR). SHA-1/OAuth Client ID belum didaftarkan di Firebase Console."
                    12500 -> "Gagal masuk Google (Kode 12500: SIGN_IN_FAILED). Play Services atau akun Google bermasalah."
                    7 -> "Gagal masuk Google (Kode 7: NETWORK_ERROR). Periksa koneksi internet."
                    else -> "Gagal masuk Google: ${err.localizedMessage ?: err.message ?: "Terjadi kesalahan."}"
                }
                viewModel.message.value = UiText.Raw(errorText)
            }
        }
    }

    val launchGoogleSignIn: () -> Unit = {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))

        val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientIdRes != 0) {
            @Suppress("LocalContextGetResourceValueCall")
            val webClientId = context.resources.getString(webClientIdRes)
            if (webClientId.isNotBlank()) {
                gsoBuilder.requestIdToken(webClientId)
            }
        }
        val client = GoogleSignIn.getClient(context, gsoBuilder.build())
        googleSignInLauncher.launch(client.signInIntent)
    }

    val withDriveToken: (suspend (String) -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            val user = authUser
            val tokenResult: Result<String> = runCatching {
                withContext(Dispatchers.IO) {
                    val account: Account = GoogleSignIn.getLastSignedInAccount(context)?.account
                        ?: (user?.email?.let { Account(it, "com.google") })
                        ?: error("Belum masuk akun Google.")
                    GoogleAuthUtil.getToken(context, account, "oauth2:https://www.googleapis.com/auth/drive.appdata")
                }
            }
            tokenResult.fold(
                onSuccess = { token: String -> action(token) },
                onFailure = { err ->
                    viewModel.transferState.value = TransferUiState.Failure(UiText.fromError(err, R.string.msg_drive_backup_failed))
                }
            )
        }
    }

    val workbookPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.previewWorkbook(context.contentResolver, uri)
    }
    val workbookExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        if (uri != null) viewModel.exportWorkbook(context.contentResolver, uri)
    }
    val pdfExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) viewModel.exportPdf(context.contentResolver, uri)
    }
    val backupCreator = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = pendingBackupPassword
        pendingBackupPassword = null
        if (uri != null && password != null) viewModel.createEncryptedBackup(context.contentResolver, uri, password)
        else password?.fill(' ')
    }
    val recoveryExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) viewModel.exportLatestRecovery(context.contentResolver, uri)
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val password = pendingRestorePassword
        pendingRestorePassword = null
        if (uri != null && password != null) viewModel.prepareEncryptedBackupRestore(context.contentResolver, uri, password)
        else password?.fill(' ')
    }

    val export: () -> Unit = { workbookExporter.launch("mankeu-${LocalDate.now()}.xlsx") }
    val exportPdf: () -> Unit = { pdfExporter.launch("laporan-mankeu-${LocalDate.now()}.pdf") }
    val createBackup: (String) -> Unit = { password ->
        pendingBackupPassword?.fill(' ')
        pendingBackupPassword = password.toCharArray()
        backupCreator.launch("mankeu-backup-${LocalDate.now()}.aabk")
    }
    val restoreBackup: (String) -> Unit = { password ->
        pendingRestorePassword?.fill(' ')
        pendingRestorePassword = password.toCharArray()
        backupPicker.launch("*/*")
    }
    val exportRecovery: () -> Unit = { recoveryExporter.launch("mankeu-sebelum-pemulihan-${LocalDate.now()}.aabk") }

    val dismissTour: () -> Unit = {
        when (activeTourType) {
            "CORE" -> {
                prefs.updateHasSeenCoreTour(true)
                prefs.updateHasSeenTour(true)
                destination = Destination.HOME
                overlay = Overlay.NONE
                manageScope = false
            }
            "SETTINGS" -> prefs.updateHasSeenSettingsTour(true)
            "SCOPE" -> prefs.updateHasSeenScopeTour(true)
            "IMPORT" -> prefs.updateHasSeenImportTour(true)
            "ENTRY" -> prefs.updateHasSeenEntryTour(true)
            "REPORTS" -> prefs.updateHasSeenReportsTour(true)
        }
        activeTourType = null
    }

    // One handler rather than three stacked ones, so the precedence is stated here instead of
    // depending on composition order: the tour is modal, then an open overlay, then "not on Beranda".
    // Below that the system default applies and back leaves the app, which is what Android expects
    // from the start destination.
    BackHandler(enabled = activeTourType != null || overlay != Overlay.NONE || destination != Destination.HOME) {
        when {
            activeTourType != null -> dismissTour()
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            else -> destination = Destination.HOME
        }
    }

    val feedback = rememberFeedback(prefs.hapticsEnabled, prefs.soundEnabled)
    CompositionLocalProvider(
        LocalTourTargets provides tourTargets,
        LocalFeedback provides feedback,
    ) {
        ArinaTheme(darkTheme = darkTheme, motion = motion) {
            // Snackbar text arrives from the ViewModel as a UiText (a string id plus args) so the
            // ViewModel needs no Context; it is resolved here, inside composition.
            val messageText = message?.resolve()
            val transferText = (transferState as? TransferUiState.Success)?.message?.resolve()
                ?: (transferState as? TransferUiState.Failure)?.message?.resolve()
            LaunchedEffect(messageText) {
                messageText?.let {
                    // Every write path in the ViewModel ends in a snackbar, so this is the one
                    // place that has to fire rather than every call site remembering to.
                    feedback(FeedbackEvent.CONFIRM)
                    snackbarHost.showSnackbar(it)
                    viewModel.clearMessage()
                }
            }
            LaunchedEffect(transferText) {
                transferText?.let {
                    snackbarHost.showSnackbar(it)
                    viewModel.clearTransferState()
                }
            }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        AnimatedContent(
                            targetState = overlay,
                            transitionSpec = {
                                val opening = targetState != Overlay.NONE
                                val enterMillis = maxOf(motion.mediumMillis, 1)
                                val exitMillis = maxOf(motion.fastMillis, 1)
                                if (opening) {
                                    // Shared-axis Z: the overlay rises over the shell, which recedes.
                                    (slideInVertically(tween(enterMillis, easing = FastOutSlowInEasing)) { full -> full / 5 } +
                                        fadeIn(tween(enterMillis)))
                                        .togetherWith(fadeOut(tween(exitMillis)) + scaleOut(tween(exitMillis), targetScale = 0.97f))
                                } else {
                                    (fadeIn(tween(enterMillis)) + scaleIn(tween(enterMillis), initialScale = 0.97f))
                                        .togetherWith(
                                            slideOutVertically(tween(exitMillis, easing = FastOutSlowInEasing)) { full -> full / 5 } +
                                                fadeOut(tween(exitMillis)),
                                        )
                                }.using(SizeTransform(clip = false))
                            },
                            label = "overlay",
                        ) { currentOverlay ->
                            when (currentOverlay) {
                                Overlay.DATA -> OverlayScaffold(
                                    title = stringResource(R.string.data_title),
                                    onBack = { overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                ) {
                                    DataScreen(
                                        state = state,
                                        importState = importState,
                                        transferState = transferState,
                                        onCreateCategory = viewModel::createCategory,
                                        onArchiveCategory = viewModel::archiveCategory,
                                        onCreateUnit = viewModel::createUnit,
                                        onArchiveUnit = viewModel::archiveUnit,
                                        onChooseWorkbook = {
                                            workbookPicker.launch(
                                                arrayOf(
                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                    "application/vnd.ms-excel",
                                                    "application/octet-stream",
                                                    "*/*",
                                                ),
                                            )
                                        },
                                        onResetImport = viewModel::resetImportState,
                                        onCommitImport = viewModel::commitImport,
                                        onReverseImportBatch = viewModel::reverseImportBatch,
                                        onExportWorkbook = export,
                                        onExportPdf = exportPdf,
                                        onCreateBackup = createBackup,
                                        onRestoreBackup = restoreBackup,
                                        recoveryAvailable = recoveryAvailable,
                                        onExportRecovery = exportRecovery,
                                        onManageScope = { manageScope = true },
                                        authUser = authUser,
                                        driveBackups = driveBackups,
                                        isDriveLoading = isDriveLoading,
                                        onFetchDriveBackups = { withDriveToken { token -> viewModel.fetchDriveBackups(token) } },
                                        onBackupToDrive = { password -> withDriveToken { token -> viewModel.backupToDrive(password.toCharArray(), token) } },
                                        onRestoreDriveBackup = { fileId, password -> withDriveToken { token -> viewModel.prepareDriveBackupRestore(fileId, password.toCharArray(), token) } },
                                        onSignInGoogle = launchGoogleSignIn,
                                        onStartTour = { activeTourType = "IMPORT" },
                                    )
                                }

                                Overlay.SETTINGS -> OverlayScaffold(
                                    title = stringResource(R.string.settings_title),
                                    onBack = { overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                    ) {
                                    SettingsScreen(
                                        themeMode = prefs.themeMode,
                                        onThemeModeChange = prefs::updateTheme,
                                        motionMode = prefs.motionMode,
                                        onMotionModeChange = prefs::updateMotion,
                                        hapticsEnabled = prefs.hapticsEnabled,
                                        onHapticsChange = prefs::updateHaptics,
                                        soundEnabled = prefs.soundEnabled,
                                        onSoundChange = prefs::updateSound,
                                        language = prefs.language,
                                        // Activity.recreate(), deliberately, after the alternative was
                                        // built and withdrawn. Providing a locale-configured
                                        // LocalContext/LocalConfiguration localises the screens but NOT
                                        // dialogs, dropdown menus, bottom sheets or the date picker:
                                        // Compose composes those in their own subcomposition, which
                                        // re-provides both locals from the Activity and drops the
                                        // override. Verified on device — the screen read English while
                                        // its own Save/Cancel buttons stayed Indonesian, and every
                                        // future dialog would have regressed the same way silently.
                                        // Recreating the Activity is what the platform itself does for
                                        // a locale change and cannot leak. See D-017.
                                        onLanguageChange = { newLanguage ->
                                            if (newLanguage != prefs.language) {
                                                switchingLanguage = true
                                                prefs.updateLanguage(newLanguage)
                                                (context as? Activity)?.recreate()
                                            }
                                        },
                                        accountName = prefs.accountName,
                                        onAccountNameChange = prefs::updateAccountName,
                                        authUser = authUser,
                                        onSignInGoogle = launchGoogleSignIn,
                                        onSignOut = viewModel::signOut,
                                        onStartTour = {
                                            overlay = Overlay.NONE
                                            activeTourType = "CORE"
                                        },
                                        onResetTours = {
                                            prefs.resetAllTours()
                                            overlay = Overlay.NONE
                                            activeTourType = "CORE"
                                        },
                                    )
                                }

                                Overlay.NONE -> BoxWithConstraints(Modifier.fillMaxSize()) {
                                    val expanded = maxWidth >= 840.dp
                                    if (expanded) {
                                        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                            AppRail(
                                                destination = destination,
                                                onDestination = { destination = it },
                                                onOpenSettings = { overlay = Overlay.SETTINGS },
                                            )
                                            MainScaffold(
                                                state = state,
                                                snackbarHost = snackbarHost,
                                                destination = destination,
                                                onDestination = { destination = it },
                                                onOpenData = { overlay = Overlay.DATA },
                                                onManageScope = { manageScope = true },
                                                onSelectBusiness = viewModel::selectBusiness,
                                                onSelectProject = viewModel::selectProject,
                                                onSelectBookMode = viewModel::selectBookMode,
                                                onOpenSettings = { overlay = Overlay.SETTINGS },
                                                onReplayTour = {
                                                    activeTourType = when (destination) {
                                                        Destination.ENTRY -> "ENTRY"
                                                        Destination.REPORTS -> "REPORTS"
                                                        else -> "CORE"
                                                    }
                                                },
                                                compact = false,
                                            ) { padding ->
                                                ScreenContent(
                                                    modifier = Modifier.padding(padding),
                                                    destination = destination,
                                                    state = state,
                                                    viewModel = viewModel,
                                                    onNavigate = { destination = it },
                                                    onOpenData = { overlay = Overlay.DATA },
                                                    onStartTour = { activeTourType = it },
                                                    expanded = true,
                                                )
                                            }
                                        }
                                    } else {
                                        MainScaffold(
                                            state = state,
                                            snackbarHost = snackbarHost,
                                            destination = destination,
                                            onDestination = { destination = it },
                                            onOpenData = { overlay = Overlay.DATA },
                                            onManageScope = { manageScope = true },
                                            onSelectBusiness = viewModel::selectBusiness,
                                            onSelectProject = viewModel::selectProject,
                                            onSelectBookMode = viewModel::selectBookMode,
                                            onOpenSettings = { overlay = Overlay.SETTINGS },
                                            onReplayTour = {
                                                activeTourType = when (destination) {
                                                    Destination.ENTRY -> "ENTRY"
                                                    Destination.REPORTS -> "REPORTS"
                                                    else -> "CORE"
                                                }
                                            },
                                            compact = true,
                                        ) { padding ->
                                            ScreenContent(
                                                modifier = Modifier.padding(padding),
                                                destination = destination,
                                                state = state,
                                                viewModel = viewModel,
                                                onNavigate = { destination = it },
                                                onOpenData = { overlay = Overlay.DATA },
                                                onStartTour = { activeTourType = it },
                                                expanded = false,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        (restoreState as? RestoreUiState.Ready)?.let { ready ->
                            RestorePreviewDialog(
                                preview = ready.preview,
                                onDismiss = viewModel::cancelPreparedRestore,
                                onConfirm = viewModel::confirmPreparedRestore,
                            )
                        }

                if (manageScope) {
                    ManageScopeSheet(
                        state = state,
                        onDismiss = {
                            manageScope = false
                            if (activeTourType == "SCOPE") dismissTour()
                        },
                        onSelectBusiness = viewModel::selectBusiness,
                        onSelectProject = viewModel::selectProject,
                        onCreateBusiness = { name, sector -> viewModel.createBusiness(name, sector) },
                        onCreateProject = viewModel::createProject,
                        onRenameBusiness = viewModel::renameBusiness,
                        onRemoveBusiness = viewModel::removeBusiness,
                        onUpdateProject = viewModel::updateProjectDetails,
                        onRemoveProject = viewModel::removeProject,
                        activeTour = activeTourType == "SCOPE",
                        onStartTour = { activeTourType = "SCOPE" },
                        onDismissTour = dismissTour,
                    )
                }

                val currentTourSteps = when (activeTourType) {
                    "SETTINGS" -> SettingsTourSteps
                    "IMPORT" -> ImportTourSteps
                    "ENTRY" -> EntryTourSteps
                    "REPORTS" -> ReportsTourSteps
                    "CORE" -> CoreTourSteps
                    else -> null
                }
                if (currentTourSteps != null) {
                    SpotlightTour(
                        steps = currentTourSteps,
                        onStepChange = { step ->
                            if (activeTourType == "CORE") {
                                step.destination?.let {
                                    destination = it
                                    overlay = Overlay.NONE
                                    manageScope = false
                                }
                                step.overlay?.let {
                                    overlay = it
                                    manageScope = false
                                }
                                if (step.manageScope) {
                                    manageScope = true
                                }
                            }
                        },
                        onDismiss = dismissTour,
                        modifier = Modifier.zIndex(100f),
                    )
                }

                LanguageSwitchOverlay(visible = switchingLanguage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHost: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

@Composable
private fun MainScaffold(
    state: AppUiState,
    snackbarHost: SnackbarHostState,
    destination: Destination,
    onDestination: (Destination) -> Unit,
    onOpenData: () -> Unit,
    onManageScope: () -> Unit,
    onSelectBusiness: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onSelectBookMode: (BookMode) -> Unit,
    onOpenSettings: () -> Unit,
    onReplayTour: () -> Unit,
    compact: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScopeHeader(
                state = state,
                onSelectBusiness = onSelectBusiness,
                onSelectProject = onSelectProject,
                onSelectBookMode = onSelectBookMode,
                onOpenSettings = onOpenSettings,
                onOpenData = onOpenData,
                onManageScope = onManageScope,
                onReplayTour = onReplayTour,
                compact = compact,
            )
        },
        bottomBar = {
            if (compact) {
                AppBottomBar(
                    destination = destination,
                    onDestination = onDestination,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        content = content,
    )
}

@Composable
private fun AppBottomBar(
    destination: Destination,
    onDestination: (Destination) -> Unit,
) {
    // Material 3 puts the navigation bar on `surfaceContainer`, one step above the page. In light
    // mode a shadow adds the last bit of lift; in dark mode a black shadow is either invisible or a
    // hard rim, so the tone difference carries it alone.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = if (isDarkScheme()) 0.dp else 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            Destination.entries.forEach { item ->
                val targetModifier = when (item) {
                    Destination.ENTRY -> Modifier.tourTarget(TourTarget.NAV_ENTRY)
                    Destination.REPORTS -> Modifier.tourTarget(TourTarget.NAV_REPORTS)
                    else -> Modifier
                }
                NavigationBarItem(
                    selected = destination == item,
                    onClick = { onDestination(item) },
                    icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                    label = {
                        Text(
                            stringResource(item.labelRes),
                            fontWeight = if (destination == item) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    modifier = targetModifier,
                )
            }
        }
    }
}

@Composable
private fun AppRail(
    destination: Destination,
    onDestination: (Destination) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .tourTarget(TourTarget.NAV_REPORTS),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxHeight(),
            header = {
                ArinaAppLogo(
                    size = 36.dp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            },
        ) {
            Spacer(Modifier.height(8.dp))
            Destination.entries.forEach { item ->
                NavigationRailItem(
                    selected = destination == item,
                    onClick = { onDestination(item) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(stringResource(item.labelRes)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            NavigationRailItem(
                selected = false,
                onClick = onOpenSettings,
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.settings_title)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopeHeader(
    state: AppUiState,
    onSelectBusiness: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onSelectBookMode: (BookMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenData: () -> Unit,
    onManageScope: () -> Unit,
    onReplayTour: () -> Unit,
    compact: Boolean,
) {
    val motion = LocalMotion.current
    val semantic = LocalArinaSemanticColors.current
    val feedback = LocalFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (isDarkScheme()) 0.dp else 2.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ArinaAppLogo(
                    size = 34.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.app_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Replays the tour for whatever destination is open. Placed in the header rather
                // than buried in Pengaturan because the moment someone needs the explanation is the
                // moment they are staring at the screen that confused them.
                IconButton(onClick = onReplayTour) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.tour_replay),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenData) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = stringResource(R.string.data_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Scope selectors. When the database is genuinely empty the app does not invent a
            // business or a field on the owner's behalf — it asks for one, explicitly. See
            // AppViewModel: the old ensureStarterWorkspace() silently wrote "Usaha Saya"/"Umum".
            Row(
                Modifier.fillMaxWidth().tourTarget(TourTarget.SCOPE_BUSINESS),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.businesses.isEmpty() && !state.isLoading) {
                    FilledTonalButton(
                        onClick = onManageScope,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scope_add_business), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    ChoiceMenu(
                        modifier = Modifier.weight(1f),
                        fieldLabel = stringResource(R.string.manage_business),
                        label = state.businesses.firstOrNull { it.id == state.selectedBusinessId }?.name
                            ?: stringResource(if (state.isLoading) R.string.scope_loading_business else R.string.scope_empty_business),
                        options = state.businesses.map { it.id to it.name },
                        onSelected = onSelectBusiness,
                        addLabel = stringResource(R.string.scope_manage_open),
                        onAddAction = onManageScope,
                    )
                }

                if (state.projects.isEmpty() && !state.isLoading) {
                    FilledTonalButton(
                        onClick = onManageScope,
                        enabled = state.selectedBusinessId != null,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scope_add_project), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    ChoiceMenu(
                        modifier = Modifier.weight(1f),
                        fieldLabel = stringResource(R.string.manage_project),
                        label = state.projects.firstOrNull { it.id == state.selectedProjectId }?.name
                            ?: stringResource(if (state.isLoading) R.string.scope_loading_project else R.string.scope_empty_project),
                        options = state.projects.map { it.id to it.name },
                        onSelected = onSelectProject,
                        addLabel = stringResource(R.string.scope_manage_open),
                        onAddAction = onManageScope,
                    )
                }
            }

            // Material 3's own segmented button, replacing the hand-rolled Surface/Row pill. The
            // sliding selection and its ripple are the component's, not ours; the only thing this
            // code adds is the plan/actual colour identity the web dashboard also uses (info-toned
            // chip for Rencana, success-toned for Aktual).
            // Proyeksi and Realisasi must not merely animate, they must *look* different, or the
            // owner cannot tell at a glance which book a figure belongs to. The first attempt used
            // secondaryContainer vs successContainer, two near-identical pale greens, and the
            // "colour change" was invisible. The pairing below is semantic rather than decorative:
            // a plan is provisional, so it wears the warning container; realisation is confirmed,
            // so it wears the success container. Each carries its own `on…Container` tone, which is
            // what the generated palette guarantees at 13:1 (see docs/UX_AND_ACCESSIBILITY.md).
            val projection = state.selectedBookMode == BookMode.PROJECTION
            val activeContainer by animateColorAsState(
                targetValue = if (projection) semantic.warningContainer else semantic.successContainer,
                animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
                label = "modeContainer",
            )
            val activeContent by animateColorAsState(
                targetValue = if (projection) semantic.onWarningContainer else semantic.onSuccessContainer,
                animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
                label = "modeContent",
            )
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().tourTarget(TourTarget.SCOPE_BOOK),
            ) {
                BookMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.selectedBookMode == mode,
                        onClick = { feedback(FeedbackEvent.TAP); onSelectBookMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = BookMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = activeContainer,
                            activeContentColor = activeContent,
                            activeBorderColor = MaterialTheme.colorScheme.outline,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        label = {
                            Text(
                                stringResource(
                                    if (mode == BookMode.PROJECTION) R.string.scope_mode_plan else R.string.scope_mode_actual,
                                ),
                                fontWeight = if (state.selectedBookMode == mode) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * A Material 3 exposed dropdown (read-only text field plus menu), replacing the previous
 * `OutlinedButton` + `DropdownMenu` pair. Besides being the platform's own select control, the
 * floating label finally says *what* each of the two header dropdowns selects — unlabelled, they
 * were distinguishable only by whatever value happened to be in them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceMenu(
    modifier: Modifier = Modifier,
    label: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    fieldLabel: String? = null,
    addLabel: String? = null,
    onAddAction: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = fieldLabel?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.scope_no_options), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    enabled = false,
                    onClick = {},
                )
            } else {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onSelected(id)
                        },
                    )
                }
            }
            if (addLabel != null && onAddAction != null) {
                if (options.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                }
                DropdownMenuItem(
                    text = { Text(addLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    onClick = {
                        expanded = false
                        onAddAction()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier,
    destination: Destination,
    state: AppUiState,
    viewModel: AppViewModel,
    onNavigate: (Destination) -> Unit,
    onOpenData: () -> Unit,
    onStartTour: ((String) -> Unit)? = null,
    expanded: Boolean,
) {
    val motion = LocalMotion.current
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Screen and book mode changes cross-fade with directional glide so the switch reads as
        // deliberate and responsive rather than an instant swap. Honours the Gerakan setting: at OFF the
        // durations are zero, which makes this equivalent to the previous hard cut.
        AnimatedContent(
            targetState = Pair(destination, state.selectedBookMode),
            transitionSpec = {
                val isBookModeChange = initialState.first == targetState.first && initialState.second != targetState.second
                if (isBookModeChange) {
                    val slideLeft = targetState.second == BookMode.REALIZATION
                    val enterMillis = maxOf(motion.mediumMillis, 1)
                    val exitMillis = maxOf(motion.fastMillis, 1)
                    (slideInHorizontally(tween(enterMillis, easing = FastOutSlowInEasing)) { fullWidth ->
                        if (slideLeft) fullWidth / 5 else -fullWidth / 5
                    } + fadeIn(tween(enterMillis)))
                        .togetherWith(
                            slideOutHorizontally(tween(exitMillis, easing = FastOutSlowInEasing)) { fullWidth ->
                                if (slideLeft) -fullWidth / 5 else fullWidth / 5
                            } + fadeOut(tween(exitMillis)),
                        )
                } else {
                    (fadeIn(tween(maxOf(motion.mediumMillis, 1))) +
                        slideInVertically(tween(maxOf(motion.mediumMillis, 1))) { it / 12 })
                        .togetherWith(fadeOut(tween(maxOf(motion.fastMillis, 1))))
                }
            },
            label = "screenAndBookMode",
        ) { (currentDest, _) ->
            when (currentDest) {
                Destination.HOME -> HomeScreen(state, onNavigate, viewModel::reverseEvent, onOpenData, expanded)
                Destination.ENTRY -> EntryScreen(
                    state = state,
                    onRecord = viewModel::record,
                    onReverse = viewModel::reverseEvent,
                    onDelete = viewModel::deleteTransaction,
                    onOpenData = onOpenData,
                    onStartTour = onStartTour?.let { { it("ENTRY") } },
                )
                Destination.BUDGET -> BudgetScreen(state, viewModel::createBudgetItem, viewModel::updateBudgetItem, viewModel::archiveBudgetItem)
                Destination.REPORTS -> ReportsScreen(
                    state = state,
                    onSettle = viewModel::settleObligation,
                    onSaveAssumptions = viewModel::saveAssumptions,
                    onStartTour = onStartTour?.let { { it("REPORTS") } },
                )
            }
        }
    }
}

/**
 * Covers the screen while the Activity is rebuilt for a new language.
 *
 * Changing locale is the one appearance change that cannot be animated in place — every string is
 * replaced and the layout reflows, and the platform's own answer is to recreate the Activity (see
 * D-017). A scrim with a spinner turns that unavoidable rebuild into something that reads as the
 * app working rather than as a stutter. It is never dismissed here: the Activity it belongs to
 * stops existing.
 */
@Composable
private fun LanguageSwitchOverlay(visible: Boolean) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(maxOf(motion.fastMillis, 1))),
        exit = fadeOut(tween(maxOf(motion.fastMillis, 1))),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.language_switching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
