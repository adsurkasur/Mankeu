package com.mankeu.presentation

import androidx.compose.runtime.mutableStateListOf
import com.mankeu.presentation.components.MankeuTooltip

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mankeu.auth.DriveTokenProvider
import com.mankeu.auth.AuthUser
import com.mankeu.backup.cloud.DriveBackupFile
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
import com.mankeu.presentation.components.MankeuAppLogo
import com.mankeu.presentation.components.RecordSpeedDial
import com.mankeu.domain.FinancialEventType
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mankeu.R
import com.mankeu.domain.BookMode
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.guide.HelpCenter
import com.mankeu.presentation.guide.HelpSheet
import com.mankeu.presentation.guide.articlesFor
import com.mankeu.presentation.guide.LocalOpenHelp
import com.mankeu.presentation.screens.DataScreen
import com.mankeu.presentation.screens.EntryScreen
import com.mankeu.presentation.screens.HomeScreen
import com.mankeu.presentation.screens.ManageScopeSheet
import com.mankeu.presentation.screens.PlanScreen
import com.mankeu.presentation.screens.ReportsScreen
import com.mankeu.presentation.screens.ProductEditorScreen
import com.mankeu.presentation.screens.ProductsScreen
import com.mankeu.presentation.screens.RestorePreviewDialog
import com.mankeu.presentation.screens.SettingsScreen
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
    PRODUCTS(R.string.nav_products, Icons.Default.Inventory2),
    REPORTS(R.string.nav_reports, Icons.Default.Assessment),
}

/** Cash is recorded from these; elsewhere the speed dial would sit over Laporan and Produk content. */
private val RECORDING_DESTINATIONS = setOf(Destination.HOME, Destination.ENTRY)

enum class Overlay { NONE, DATA, SETTINGS, HELP, PRODUCT }

@Composable
fun MankeuApp(
    viewModel: AppViewModel,
    /** Help screenshots only: show this article's place highlighted, without the card or offers. */
    captureShow: HelpArticle? = null,
) {
    val prefs = rememberAppPreferencesState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val motion = MankeuMotion.resolve(prefs.motionMode, rememberSystemAnimationScale())

    // Theme changes are animated inside MankeuTheme by interpolating the colour scheme, which
    // costs no state at all. Language is different: it goes through Activity.recreate() — see
    // onLanguageChange below for why the in-composition approach was tried and withdrawn.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val recoveryAvailable by viewModel.recoveryAvailable.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val authUser by viewModel.authUser.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    // Set by the speed dial, consumed by EntryScreen, which opens its form on that kind. Kept
    // here rather than inside EntryScreen because the dial can be pressed from Beranda, before
    // EntryScreen exists.
    var requestedEntryKind by rememberSaveable { mutableStateOf<FinancialEventType?>(null) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    // A sheet, not a route: ModalBottomSheet handles its own back press and scrim.
    var manageScope by rememberSaveable { mutableStateOf(false) }
    var helpArticle by rememberSaveable { mutableStateOf<HelpArticle?>(null) }
    var helpSheet by rememberSaveable { mutableStateOf(false) }
    // The product the full-page editor (Overlay.PRODUCT) is open on; null = a new product.
    var productEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    val openProduct: (String?) -> Unit = { id -> productEditorId = id; overlay = Overlay.PRODUCT }
    // Set the instant the owner picks a language, cleared only by the Activity being rebuilt.
    // recreate() is not instantaneous — resources reload and the whole tree recomposes — and
    // without this the screen simply froze on the old language for a beat and then snapped.
    var switchingLanguage by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingRestorePassword by remember { mutableStateOf<CharArray?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            task.getResult(ApiException::class.java)
            viewModel.onGoogleSignedIn()
        }.onFailure { err ->
            val apiEx = err as? ApiException
            val statusCode = apiEx?.statusCode
            // Status 12501 indicates the user voluntarily dismissed the account picker
            if (statusCode != 12501) {
                val errorText = when (statusCode) {
                    10 -> "Gagal masuk Google (Kode 10: DEVELOPER_ERROR). SHA-1 aplikasi belum terdaftar di OAuth client Google Cloud."
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

        val client = GoogleSignIn.getClient(context, gsoBuilder.build())
        googleSignInLauncher.launch(client.signInIntent)
    }

    val driveTokens = remember(context) { DriveTokenProvider(context) }
    val withDriveToken: (suspend (String) -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            val tokenResult: Result<String> = runCatching { driveTokens.token(authUser?.email) }
            tokenResult.fold(
                onSuccess = { token: String -> action(token) },
                onFailure = { err ->
                    viewModel.transferState.value = TransferUiState.Failure(UiText.fromError(err, R.string.msg_drive_backup_failed))
                }
            )
        }
    }

    // "Hapus semua data" (DEC-020). The ViewModel deletes Drive and the database and signs the account out (revoking the Google grant); what
    // only this layer holds (the local name) is cleared once that succeeded.
    val eraseAllData: (Boolean) -> Unit = { includeCloud ->
        val clearUiState = { prefs.updateAccountName("") }
        if (!includeCloud) {
            viewModel.eraseAllData(driveAccessToken = null, onErased = clearUiState)
        } else {
            coroutineScope.launch {
                runCatching { driveTokens.token(authUser?.email) }.fold(
                    onSuccess = { token -> viewModel.eraseAllData(driveAccessToken = token, onErased = clearUiState) },
                    onFailure = { viewModel.message.value = UiText.fromError(it, R.string.msg_erase_failed) },
                )
            }
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
    val templateExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        if (uri != null) viewModel.exportTemplate(context.contentResolver, uri)
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

    // Exports of different units must not share one name: the file says which unit it holds.
    val unitSlug = { state.projects.firstOrNull { it.id == state.selectedProjectId }?.name.orEmpty().fileSlug() }
    val export: () -> Unit = { workbookExporter.launch(listOf("mankeu", unitSlug(), LocalDate.now().toString()).filter(String::isNotEmpty).joinToString("-") + ".xlsx") }
    val exportTemplate: () -> Unit = { templateExporter.launch("template-pencatatan-umkm.xlsx") }
    val exportPdf: () -> Unit = { pdfExporter.launch(listOf("laporan-keuangan", unitSlug(), LocalDate.now().toString()).filter(String::isNotEmpty).joinToString("-") + ".pdf") }
    val createBackup: (String) -> Unit = { password ->
        pendingBackupPassword?.fill(' ')
        pendingBackupPassword = password.toCharArray()
        backupCreator.launch("umkm-mankeu-backup-${LocalDate.now()}.aabk")
    }
    val restoreBackup: (String) -> Unit = { password ->
        pendingRestorePassword?.fill(' ')
        pendingRestorePassword = password.toCharArray()
        backupPicker.launch("*/*")
    }
    val exportRecovery: () -> Unit = { recoveryExporter.launch("umkm-mankeu-sebelum-pemulihan-${LocalDate.now()}.aabk") }

    val openHelp: (HelpArticle) -> Unit = { article ->
        helpSheet = false
        manageScope = false
        helpArticle = article
        overlay = Overlay.HELP
    }
    // "Tunjukkan di aplikasi": go to the page, book mode or overlay the article is about. Without an usaha and unit most pages are an empty card, so the owner is
    // sent to create them first, with the reason.
    val show: (HelpArticle) -> Unit = { article ->
        val spec = article.show
        helpSheet = false
        if (spec.needsUnit && state.selectedProjectId == null) {
            overlay = Overlay.NONE
            manageScope = true
            viewModel.message.value = UiText.of(R.string.help_show_needs_unit)
        } else {
            overlay = spec.overlay
            if (spec.overlay == Overlay.NONE) destination = spec.destination
            spec.bookMode?.let { if (state.selectedBookMode != it) viewModel.selectBookMode(it) }
        }
    }
    LaunchedEffect(captureShow) { captureShow?.let(show) }

    // One handler rather than three stacked ones, so the precedence is stated here instead of
    // depending on composition order: an open overlay, then "not on Beranda".
    // Below that the system default applies and back leaves the app, which is what Android expects
    // from the start destination.
    BackHandler(enabled = overlay != Overlay.NONE || destination != Destination.HOME) {
        when {
            overlay == Overlay.HELP && helpArticle != null -> helpArticle = null
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            else -> destination = Destination.HOME
        }
    }

    val feedback = rememberFeedback(prefs.hapticsEnabled, prefs.soundEnabled)
    CompositionLocalProvider(
        LocalFeedback provides feedback,
        LocalOpenHelp provides openHelp,
    ) {
        MankeuTheme(darkTheme = darkTheme, motion = motion) {
            // Snackbar text arrives from the ViewModel as a UiText (a string id plus args) so the
            // ViewModel needs no Context; it is resolved here, inside composition.
            val messageText = message?.resolve()
            val messageIsError = message?.isError == true
            val transferText = (transferState as? TransferUiState.Success)?.message?.resolve()
                ?: (transferState as? TransferUiState.Failure)?.message?.resolve()
            val transferIsError = transferState is TransferUiState.Failure
            LaunchedEffect(messageText) {
                messageText?.let {
                    // Every write path in the ViewModel ends in a snackbar, so this is the one
                    // place that has to fire rather than every call site remembering to. A refusal
                    // used to buzz CONFIRM like a success; it now gets the REJECT signature.
                    feedback(if (messageIsError) FeedbackEvent.REJECT else FeedbackEvent.CONFIRM)
                    snackbarHost.showSnackbar(it)
                    viewModel.clearMessage()
                }
            }
            LaunchedEffect(transferText) {
                transferText?.let {
                    // Export, backup and restore results: the confirmation a backup was written.
                    feedback(if (transferIsError) FeedbackEvent.REJECT else FeedbackEvent.CONFIRM)
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
                                    onHelp = { helpSheet = true },
                                ) {
                                    DataScreen(
                                        state = state,
                                        importState = importState,
                                        transferState = transferState,
                                        onCreateCategory = viewModel::createCategory,
                                        onArchiveCategory = viewModel::archiveCategory,
                                        onSetCategoryCostNature = viewModel::setCategoryCostNature,
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
                                        onUndoImportBatch = viewModel::undoImportBatch,
                                        onExportWorkbook = export,
                                        onExportPdf = exportPdf,
                                        onExportTemplate = exportTemplate,
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
                                    )
                                }

                                Overlay.SETTINGS -> OverlayScaffold(
                                    title = stringResource(R.string.settings_title),
                                    onBack = { overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                    onHelp = { helpSheet = true },
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
                                        onOpenHelpCenter = {
                                            helpArticle = null
                                            overlay = Overlay.HELP
                                        },
                                        onEraseAllData = eraseAllData,
                                        isErasing = isSaving,
                                        syncState = syncState,
                                        onEnableSync = viewModel::enableSync,
                                        onDisableSync = viewModel::disableSync,
                                        onSyncNow = viewModel::syncNow,
                                    )
                                }

                                Overlay.HELP -> OverlayScaffold(
                                    title = stringResource(R.string.help_center_title),
                                    onBack = { if (helpArticle != null) helpArticle = null else overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                ) {
                                    HelpCenter(article = helpArticle, onOpen = { helpArticle = it }, onShow = show)
                                }

                                Overlay.PRODUCT -> OverlayScaffold(
                                    title = stringResource(if (productEditorId == null) R.string.editor_title_new else R.string.editor_title_edit),
                                    onBack = { overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                ) {
                                    key(productEditorId) {
                                        ProductEditorScreen(
                                            existing = state.products.firstOrNull { it.product.id == productEditorId },
                                            materials = state.stockItems,
                                            // Closed only once saved: a refusal keeps the owner's input and shows why.
                                            onSave = { viewModel.saveProduct(productEditorId, it) { overlay = Overlay.NONE } },
                                            onDelete = productEditorId?.let { id -> { viewModel.archiveProduct(id); overlay = Overlay.NONE } },
                                        )
                                    }
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
                                                onSelectBookMode = viewModel::selectBookMode,
                                                onOpenSettings = { overlay = Overlay.SETTINGS },
                                                onOpenHelpSheet = { helpSheet = true },
                                                onQuickRecord = { kind ->
                                                    requestedEntryKind = kind
                                                    destination = Destination.ENTRY
                                                },
                                                compact = false,
                                            ) { padding ->
                                                ScreenContent(
                                                    modifier = Modifier.padding(padding),
                                                    destination = destination,
                                                    state = state,
                                                    viewModel = viewModel,
                                                    isSaving = isSaving,
                                                    onNavigate = { destination = it },
                                                    onManageScope = { manageScope = true },
                                                    onQuickRecord = { kind -> requestedEntryKind = kind; destination = Destination.ENTRY },
                                                    onOpenProduct = openProduct,
                                                    requestedKind = requestedEntryKind,
                                                    onRequestedKindConsumed = { requestedEntryKind = null },
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
                                            onSelectBookMode = viewModel::selectBookMode,
                                            onOpenSettings = { overlay = Overlay.SETTINGS },
                                            onOpenHelpSheet = { helpSheet = true },
                                            onQuickRecord = { kind ->
                                                requestedEntryKind = kind
                                                destination = Destination.ENTRY
                                            },
                                            compact = true,
                                        ) { padding ->
                                            ScreenContent(
                                                modifier = Modifier.padding(padding),
                                                destination = destination,
                                                state = state,
                                                viewModel = viewModel,
                                                isSaving = isSaving,
                                                onNavigate = { destination = it },
                                                onManageScope = { manageScope = true },
                                                onQuickRecord = { kind -> requestedEntryKind = kind; destination = Destination.ENTRY },
                                                onOpenProduct = openProduct,
                                                requestedKind = requestedEntryKind,
                                                onRequestedKindConsumed = { requestedEntryKind = null },
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

                val firstUnitName = stringResource(R.string.default_unit_name)
                if (manageScope) {
                    ManageScopeSheet(
                        state = state,
                        onDismiss = { manageScope = false },
                        onSelectBusiness = viewModel::selectBusiness,
                        onSelectProject = viewModel::selectProject,
                        onCreateBusiness = { name ->
                            // The first business closes the sheet once saved: it already has its unit.
                            val first = state.businesses.isEmpty()
                            viewModel.createBusiness(name, firstUnitName) { if (first) manageScope = false }
                        },
                        onCreateProject = viewModel::createProject,
                        onRenameBusiness = viewModel::renameBusiness,
                        onRemoveBusiness = viewModel::removeBusiness,
                        onUpdateProject = viewModel::updateProjectDetails,
                        onRemoveProject = viewModel::removeProject,
                    )
                }

                val pageArticles = articlesFor(destination, state.selectedBookMode, overlay)
                if (helpSheet) {
                    HelpSheet(
                        pageArticles = pageArticles,
                        onDismiss = { helpSheet = false },
                        onOpenArticle = openHelp,
                        onOpenCenter = { helpSheet = false; helpArticle = null; overlay = Overlay.HELP },
                    )
                }

                state.priceAlert?.let { alert ->
                    // A short confirmation, not a form (#13): what the new price does to each product (spec #20).
                    AlertDialog(
                        onDismissRequest = viewModel::dismissPriceAlert,
                        title = { Text(stringResource(R.string.price_alert_title, alert.materialName)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.price_alert_body, alert.oldPriceRupiah.toRupiah(), alert.newPriceRupiah.toRupiah(), alert.unit))
                                alert.changes.forEach { change ->
                                    change.before.perSaleUnit.zip(change.after.perSaleUnit).forEach { (before, after) ->
                                        Text(
                                            stringResource(
                                                R.string.price_alert_row,
                                                change.product.name,
                                                before.hppRupiah.toRupiah(),
                                                after.hppRupiah.toRupiah(),
                                                after.unit.name,
                                                before.profitRupiah.toRupiah(),
                                                after.profitRupiah.toRupiah(),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.dismissPriceAlert()
                                overlay = Overlay.NONE
                                manageScope = false
                                destination = Destination.PRODUCTS
                            }) { Text(stringResource(R.string.price_alert_open_products)) }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissPriceAlert) { Text(stringResource(R.string.action_close)) }
                        },
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
    onHelp: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    MankeuTooltip(stringResource(R.string.action_back)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (onHelp != null) {
                        MankeuTooltip(stringResource(R.string.help_sheet_title)) {
                            IconButton(onClick = onHelp) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = stringResource(R.string.help_sheet_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
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
    onSelectBookMode: (BookMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelpSheet: () -> Unit,
    onQuickRecord: (FinancialEventType) -> Unit,
    compact: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // Only where recording is the next thing to do. On Laporan the owner is reading, and a
            // permanent action button there would sit on top of the numbers for no reason. Proyeksi
            // is a plan, not a cash book (DEC-014): it is edited on the Rencana Usaha page instead.
            if (destination in RECORDING_DESTINATIONS && state.selectedProjectId != null &&
                state.selectedBookMode != BookMode.PROJECTION
            ) {
                RecordSpeedDial(onRecord = onQuickRecord)
            }
        },
        topBar = {
            ScopeHeader(
                state = state,
                onSelectBookMode = onSelectBookMode,
                onOpenSettings = onOpenSettings,
                onOpenData = onOpenData,
                onManageScope = onManageScope,
                onOpenHelpSheet = onOpenHelpSheet,
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
    val feedback = LocalFeedback.current
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
                NavigationBarItem(
                    selected = destination == item,
                    onClick = {
                        if (destination != item) feedback(FeedbackEvent.TAP)
                        onDestination(item)
                    },
                    icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                    label = {
                        Text(
                            stringResource(item.labelRes),
                            fontWeight = if (destination == item) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
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
    val feedback = LocalFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp, horizontal = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxHeight(),
            header = {
                MankeuAppLogo(
                    size = 36.dp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            },
        ) {
            Spacer(Modifier.height(8.dp))
            Destination.entries.forEach { item ->
                NavigationRailItem(
                    selected = destination == item,
                    onClick = {
                        if (destination != item) feedback(FeedbackEvent.TAP)
                        onDestination(item)
                    },
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
    onSelectBookMode: (BookMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenData: () -> Unit,
    onManageScope: () -> Unit,
    onOpenHelpSheet: () -> Unit,
    compact: Boolean,
) {
    val motion = LocalMotion.current
    val semantic = LocalMankeuSemanticColors.current
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
            // One row, not three.
            //
            // The header used to stack an app title with a subtitle, then a pair of 56dp dropdowns,
            // then the mode switch — roughly a fifth of a phone screen spent restating the app's
            // own name and two values that change a few times a week. The name is on the launcher
            // icon and in the task switcher; the two scope values are one line of text that opens
            // the sheet that already manages them (D-065).
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MankeuAppLogo(size = 34.dp)
                Spacer(Modifier.width(10.dp))
                ScopeSummary(
                    state = state,
                    onClick = onManageScope,
                    modifier = Modifier.weight(1f),
                )
                // Opens help for whatever destination is open. Placed in the header rather than buried
                // in Pengaturan because the moment someone needs the explanation is the moment they
                // are staring at the screen that confused them.
                MankeuTooltip(stringResource(R.string.help_sheet_title)) {
                    IconButton(onClick = onOpenHelpSheet) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_sheet_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                MankeuTooltip(stringResource(R.string.data_title)) {
                    IconButton(onClick = onOpenData) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.data_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                MankeuTooltip(stringResource(R.string.settings_title)) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            //
            // Round 8 gave Realisasi the semantic success (teal) container so the two books could
            // be told apart. It worked, and it cost more than it bought: teal is the app's money
            // colour — Uang Masuk, a surplus, a positive cash flow — and a permanent teal block in
            // the header competed with the figures below it, which are the thing that is actually
            // supposed to be teal or coral. A book is a scope, not an amount.
            //
            // Both segments come from the generated Material 3 palette now, one step apart on the
            // same tonal family: Realisasi on `primaryContainer`, Proyeksi on `secondaryContainer`.
            // They stay distinguishable because only one of them is filled at a time and the
            // selected one also carries a check icon and bold weight — the selection is legible
            // from shape and state, not from borrowing a colour that means something else.
            val projection = state.selectedBookMode == BookMode.PROJECTION
            val activeContainer by animateColorAsState(
                targetValue = if (projection) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
                label = "modeContainer",
            )
            val activeContent by animateColorAsState(
                targetValue = if (projection) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
                label = "modeContent",
            )
            // Hidden until there is a unit: before that the two books are words without meaning (2026-10-22).
            if (state.selectedProjectId != null) SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth(),
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
 * The active business and unit as one line, opening the sheet that manages them.
 *
 * Two `ExposedDropdownMenuBox`es were the wrong control for this: each carried a floating label
 * and a 56dp minimum height purely to show a value the owner changes a few times a week, and
 * neither could create, rename or archive — for that they went to the same sheet this row opens.
 * A summary that *is* the affordance costs one line and loses nothing.
 */
@Composable
private fun ScopeSummary(
    state: AppUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBusiness = state.businesses.isNotEmpty()
    val hasProject = state.projects.isNotEmpty()
    val businessLabel = state.businesses.firstOrNull { it.id == state.selectedBusinessId }?.name
        ?: stringResource(
            when {
                state.isLoading -> R.string.scope_loading_business
                else -> R.string.scope_empty_business
            },
        )
    val projectLabel = state.projects.firstOrNull { it.id == state.selectedProjectId }?.name
        ?: stringResource(
            when {
                state.isLoading -> R.string.scope_loading_project
                !hasBusiness -> R.string.scope_add_business
                else -> R.string.scope_add_project
            },
        )
    // Nothing set up yet is not a neutral state — it is the one thing the owner has to do next —
    // so the row says so in the primary tone instead of showing two grey placeholders.
    val needsSetup = !state.isLoading && (!hasBusiness || !hasProject)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (needsSetup) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (needsSetup) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    businessLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    projectLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (needsSetup) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                if (needsSetup) Icons.Default.Add else Icons.Default.UnfoldMore,
                contentDescription = stringResource(R.string.scope_manage_open),
                modifier = Modifier.size(18.dp),
                tint = if (needsSetup) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
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
    isSaving: Boolean,
    onNavigate: (Destination) -> Unit,
    /** Opens the Usaha dan Unit sheet: every empty state asking for a business or unit leads there. */
    onManageScope: () -> Unit,
    onQuickRecord: (FinancialEventType) -> Unit,
    onOpenProduct: (String?) -> Unit,
    requestedKind: FinancialEventType? = null,
    onRequestedKindConsumed: () -> Unit = {},
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
                Destination.HOME -> HomeScreen(
                    state = state,
                    onNavigate = onNavigate,
                    onManageScope = onManageScope,
                    onOpenPlan = { onNavigate(Destination.ENTRY) },
                    onQuickRecord = onQuickRecord,
                    expanded = expanded,
                )
                Destination.PRODUCTS -> ProductsScreen(
                    products = state.products,
                    materials = state.stockItems,
                    hasBusiness = state.selectedBusinessId != null,
                    unitName = state.projects.firstOrNull { it.id == state.selectedProjectId }?.name,
                    onManageScope = onManageScope,
                    onOpenProduct = onOpenProduct,
                    onProduce = viewModel::recordProduction,
                    onCountStock = viewModel::countProductStock,
                    onCreateStockItem = viewModel::createStockItem,
                    onRecordStockMovement = viewModel::recordStockMovement,
                    expanded = expanded,
                )
                Destination.ENTRY -> if (state.selectedBookMode == BookMode.PROJECTION) {
                    PlanScreen(
                        state = state,
                        onOpenProduct = onOpenProduct,
                        onSavePlanLine = viewModel::savePlanLine,
                        onArchivePlanLine = viewModel::archivePlanLine,
                        onSetPeriod = viewModel::setPlanPeriod,
                        onManageScope = onManageScope,
                    )
                } else EntryScreen(
                    state = state,
                    isSaving = isSaving,
                    onRecord = viewModel::record,
                    onDelete = viewModel::deleteTransaction,
                    onManageScope = onManageScope,
                    requestedKind = requestedKind,
                    onRequestedKindConsumed = onRequestedKindConsumed,
                    onCreateCategory = viewModel::createCategory,
                )
                Destination.REPORTS -> ReportsScreen(
                    state = state,
                    onOpenPlan = { onNavigate(Destination.ENTRY) },
                    onRecordTransaction = { onNavigate(Destination.ENTRY) },
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

/** "Laundry Kiloan (Cab. 2)" → "laundry-kiloan-cab-2", safe in any file picker. */
internal fun String.fileSlug(): String = lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)
