package com.mankeu.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mankeu.presentation.components.MankeuTooltip

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mankeu.auth.DriveTokenProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.mankeu.presentation.components.MankeuAppLogo
import com.mankeu.presentation.components.RecordSpeedDial
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mankeu.R
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.guide.HelpCenter
import com.mankeu.presentation.guide.HelpSheet
import com.mankeu.presentation.guide.articlesFor
import com.mankeu.presentation.guide.LocalOpenHelp
import com.mankeu.presentation.screens.ContactsScreen
import com.mankeu.presentation.screens.CustomerScreen
import com.mankeu.presentation.screens.MoneyFormMode
import com.mankeu.presentation.screens.MoneyFormScreen
import com.mankeu.presentation.screens.moneyFormModeFor
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.CategoryRole
import com.mankeu.data.PlanAssembly
import com.mankeu.presentation.screens.DataFocus
import com.mankeu.presentation.screens.ProductionSheet
import com.mankeu.presentation.components.form.PickerPage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Menu
import com.mankeu.presentation.screens.DataScreen
import com.mankeu.presentation.screens.NoteFilter
import com.mankeu.presentation.screens.SaleNoteDetailScreen
import com.mankeu.presentation.screens.SaleNoteEditorScreen
import com.mankeu.presentation.screens.EntryScreen
import com.mankeu.presentation.screens.HomeScreen
import com.mankeu.presentation.screens.ManageScopeSheet
import com.mankeu.presentation.screens.PlanScreen
import com.mankeu.presentation.screens.ReportsScreen
import com.mankeu.presentation.screens.ProductEditorScreen
import com.mankeu.presentation.screens.StockHistoryScreen
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

/** The speed dial lives on Catatan only; Beranda has its own Jual / Belanja / Produksi buttons (U4). */
private val RECORDING_DESTINATIONS = setOf(Destination.ENTRY)

enum class Overlay { NONE, DATA, SETTINGS, HELP, PRODUCT, STOCK_HISTORY, CONTACTS, SALE_EDITOR, SALE_DETAIL, PLAN, PICK_PRODUCT, CUSTOMER, MONEY_FORM }

/** The labelled Lainnya menu in the header (U4, plan ruling 5); replaces the three unlabelled icons. */
enum class MoreItem(val labelRes: Int, val icon: ImageVector) {
    PLAN(R.string.home_plan_row, Icons.Default.Calculate),
    CONTACTS(R.string.contacts_title, Icons.Default.Contacts),
    CATEGORIES(R.string.more_categories, Icons.Default.Category),
    HELP(R.string.help_sheet_title, Icons.AutoMirrored.Filled.HelpOutline),
    BACKUP(R.string.more_backup, Icons.Default.Backup),
    EXCEL(R.string.more_excel, Icons.Default.FolderOpen),
    SETTINGS(R.string.settings_title, Icons.Default.Settings),
}

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
    // The money page (U5): Belanja / Uang masuk lain / Uang keluar lain / Modal & ambil pribadi, the
    // record it corrects, and the material and quantity "Catat juga uangnya?" fills Belanja with (U3).
    var moneyMode by rememberSaveable { mutableStateOf(MoneyFormMode.PURCHASE) }
    var moneyEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var moneyPrefill by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    // A sheet, not a route: ModalBottomSheet handles its own back press and scrim.
    var manageScope by rememberSaveable { mutableStateOf(false) }
    var helpArticle by rememberSaveable { mutableStateOf<HelpArticle?>(null) }
    var helpSheet by rememberSaveable { mutableStateOf(false) }
    // The product the full-page editor (Overlay.PRODUCT) is open on; null = a new product.
    var productEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    // Where the product editor and Kontak return to: Rencana usaha / Berkas dan Data, or the shell.
    var productFrom by rememberSaveable { mutableStateOf(Overlay.NONE) }
    val openProduct: (String?) -> Unit = { id -> productEditorId = id; productFrom = overlay; overlay = Overlay.PRODUCT }
    var contactsFrom by rememberSaveable { mutableStateOf(Overlay.NONE) }
    var contactsSearch by rememberSaveable { mutableStateOf("") }
    val openContacts: (String) -> Unit = { search -> contactsSearch = search; contactsFrom = overlay; overlay = Overlay.CONTACTS }
    var dataFocus by rememberSaveable { mutableStateOf(DataFocus.NONE) }
    // Beranda's Produksi: the product picked in Overlay.PICK_PRODUCT, whose production sheet is open.
    var produceId by rememberSaveable { mutableStateOf<String?>(null) }
    // The product whose Riwayat stok (Overlay.STOCK_HISTORY) is open.
    var stockHistoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val openStockHistory: (String) -> Unit = { id -> stockHistoryId = id; overlay = Overlay.STOCK_HISTORY }
    // Fase 2: the note the editor is open on (null = a new note), the note whose detail is open,
    // and the Catatan note filter, which Beranda's Tagihan card sets.
    var saleEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var saleDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteFilter by rememberSaveable { mutableStateOf(NoteFilter.OPEN) }
    val openSaleEditor: (String?) -> Unit = { id -> saleEditorId = id; overlay = Overlay.SALE_EDITOR }
    // A note opened from a customer's page returns there.
    var saleDetailFrom by rememberSaveable { mutableStateOf(Overlay.NONE) }
    val openSaleDetail: (String) -> Unit = { id ->
        saleDetailId = id
        saleDetailFrom = if (overlay == Overlay.CUSTOMER) Overlay.CUSTOMER else Overlay.NONE
        overlay = Overlay.SALE_DETAIL
    }
    val openBills: (NoteFilter) -> Unit = { filter -> noteFilter = filter; destination = Destination.ENTRY }
    val openMoneyForm: (MoneyFormMode, String?, Pair<String, String>?) -> Unit = { mode, editId, prefill ->
        moneyMode = mode
        moneyEditId = editId
        moneyPrefill = prefill
        overlay = Overlay.MONEY_FORM
    }
    val openMoney: (MoneyFormMode) -> Unit = { openMoneyForm(it, null, null) }
    val editMoney: (MoneyEvent) -> Unit = { openMoneyForm(moneyFormModeFor(it, state.categories), it.id, null) }
    val buyMaterial: (String, java.math.BigDecimal) -> Unit = { id, quantity ->
        openMoneyForm(MoneyFormMode.PURCHASE, null, id to quantity.stripTrailingZeros().toPlainString())
    }
    val businessName = state.businesses.firstOrNull { it.id == state.selectedBusinessId }?.name.orEmpty()
    // Kirim (U-3): the screens hand over text and, with a phone number, a wa.me link; only this layer builds Intents.
    val sendText: (String, String?) -> Unit = { text, link -> shareText(context, text, link) }
    // The customer page (U5): sisa tagihan, notes, Terima bayar, Kirim pengingat.
    var customerId by rememberSaveable { mutableStateOf<String?>(null) }
    var customerFrom by rememberSaveable { mutableStateOf(Overlay.NONE) }
    val openCustomer: (String) -> Unit = { id -> customerId = id; customerFrom = overlay; overlay = Overlay.CUSTOMER }
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
            // Ubah from a note's detail returns to that detail; Kontak returns to Berkas dan Data.
            overlay == Overlay.SALE_EDITOR && saleEditorId != null -> overlay = Overlay.SALE_DETAIL
            overlay == Overlay.CONTACTS -> overlay = contactsFrom
            overlay == Overlay.CUSTOMER -> overlay = customerFrom
            overlay == Overlay.SALE_DETAIL -> overlay = saleDetailFrom
            overlay == Overlay.PRODUCT -> overlay = productFrom
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            else -> destination = Destination.HOME
        }
    }

    val onMore: (MoreItem) -> Unit = { item ->
        when (item) {
            MoreItem.PLAN -> overlay = Overlay.PLAN
            MoreItem.CONTACTS -> openContacts("")
            MoreItem.CATEGORIES -> { dataFocus = DataFocus.CATEGORIES; overlay = Overlay.DATA }
            MoreItem.HELP -> helpSheet = true
            MoreItem.BACKUP -> { dataFocus = DataFocus.BACKUP; overlay = Overlay.DATA }
            MoreItem.EXCEL -> { dataFocus = DataFocus.NONE; overlay = Overlay.DATA }
            MoreItem.SETTINGS -> overlay = Overlay.SETTINGS
        }
    }
    // No product yet: say why the product editor opens instead of the picker (U4 review M-4).
    val onProduce: () -> Unit = {
        if (state.products.isEmpty()) {
            viewModel.message.value = UiText.of(R.string.produce_need_product)
            openProduct(null)
        } else {
            overlay = Overlay.PICK_PRODUCT
        }
    }
    // A Kategori/Cadangan focus belongs to one visit of Berkas dan Data, never a later one (U4 review I-2).
    LaunchedEffect(overlay) { if (overlay != Overlay.DATA) dataFocus = DataFocus.NONE }

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
                                        onSetCategoryIncludedInHpp = viewModel::setCategoryIncludedInHpp,
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
                                        onOpenContacts = { openContacts("") },
                                        focus = dataFocus,
                                        onFocusDone = { dataFocus = DataFocus.NONE; overlay = Overlay.NONE },
                                    )
                                }

                                Overlay.CONTACTS -> OverlayScaffold(
                                    title = stringResource(R.string.contacts_title),
                                    onBack = { overlay = contactsFrom },
                                    snackbarHost = snackbarHost,
                                ) {
                                    key(contactsSearch) {
                                        ContactsScreen(
                                            contacts = state.contacts,
                                            sales = state.businessSales,
                                            payables = state.businessPayables,
                                            onSave = viewModel::saveContact,
                                            onArchive = viewModel::archiveContact,
                                            initialSearch = contactsSearch,
                                            onOpen = openCustomer,
                                        )
                                    }
                                }

                                // Temporary full page until U7 rebuilds Rencana Usaha: the plan content, no switch.
                                Overlay.PLAN -> OverlayScaffold(
                                    title = stringResource(R.string.home_plan_row),
                                    onBack = { overlay = Overlay.NONE },
                                    snackbarHost = snackbarHost,
                                    onHelp = { helpSheet = true },
                                ) {
                                    PlanScreen(
                                        state = state,
                                        onOpenProduct = openProduct,
                                        onSavePlanLine = viewModel::savePlanLine,
                                        onArchivePlanLine = viewModel::archivePlanLine,
                                        onSetPeriod = viewModel::setPlanPeriod,
                                        onManageScope = { manageScope = true },
                                    )
                                }

                                Overlay.PICK_PRODUCT -> PickerPage(
                                    title = stringResource(R.string.produce_pick_title),
                                    items = state.products,
                                    itemLabel = { it.product.name },
                                    onPick = { produceId = it.product.id; overlay = Overlay.NONE },
                                    onClose = { overlay = Overlay.NONE },
                                )

                                Overlay.SALE_EDITOR -> key(saleEditorId) {
                                    val closeEditor = { overlay = if (saleEditorId != null) Overlay.SALE_DETAIL else Overlay.NONE }
                                    SaleNoteEditorScreen(
                                        title = stringResource(if (saleEditorId == null) R.string.sale_title_new else R.string.sale_title_edit),
                                        existing = state.sales.firstOrNull { it.sale.id == saleEditorId },
                                        products = state.products,
                                        materials = state.stockItems,
                                        contacts = state.contacts,
                                        sales = state.sales,
                                        // Closed only once saved: a refusal keeps the owner's input and shows why.
                                        onSave = { draft ->
                                            viewModel.saveSale(saleEditorId, draft) { id ->
                                                if (saleEditorId != null) openSaleDetail(id) else overlay = Overlay.NONE
                                            }
                                        },
                                        onSaveContact = viewModel::saveContact,
                                        onClose = closeEditor,
                                        snackbarHost = snackbarHost,
                                    )
                                }

                                Overlay.SALE_DETAIL -> {
                                    val record = state.sales.firstOrNull { it.sale.id == saleDetailId }
                                    // Deleted here or synced away while open.
                                    LaunchedEffect(record == null) { if (record == null) overlay = Overlay.NONE }
                                    OverlayScaffold(
                                        title = stringResource(R.string.sale_detail_title),
                                        onBack = { overlay = saleDetailFrom },
                                        snackbarHost = snackbarHost,
                                    ) {
                                        record?.let {
                                            SaleNoteDetailScreen(
                                                record = it,
                                                products = state.products,
                                                onPay = { amount, date, onDone, onError -> viewModel.paySale(it.sale.id, amount, date, onDone, onError) },
                                                onEdit = { openSaleEditor(it.sale.id) },
                                                onDelete = { viewModel.deleteSale(it.sale.id) },
                                                businessName = businessName,
                                                onSend = sendText,
                                                onOpenCustomer = openCustomer,
                                            )
                                        }
                                    }
                                }

                                Overlay.CUSTOMER -> {
                                    val contact = state.contacts.firstOrNull { it.id == customerId }
                                    // Archived here or synced away while open.
                                    LaunchedEffect(contact == null) { if (contact == null) overlay = customerFrom }
                                    OverlayScaffold(
                                        title = contact?.name.orEmpty(),
                                        onBack = { overlay = customerFrom },
                                        snackbarHost = snackbarHost,
                                    ) {
                                        contact?.let { c ->
                                            CustomerScreen(
                                                contact = c,
                                                sales = state.businessSales.filter { it.sale.contactId == c.id },
                                                products = state.products,
                                                others = state.contacts,
                                                businessName = businessName,
                                                onOpenNote = openSaleDetail,
                                                onPay = viewModel::payCustomer,
                                                onSend = sendText,
                                                onSaveContact = viewModel::saveContact,
                                                onArchiveContact = viewModel::archiveContact,
                                            )
                                        }
                                    }
                                }

                                Overlay.MONEY_FORM -> key(moneyMode, moneyEditId, moneyPrefill) {
                                    val existing = state.events.firstOrNull { it.id == moneyEditId }
                                    MoneyFormScreen(
                                        mode = moneyMode,
                                        state = state,
                                        existing = existing,
                                        prefill = moneyPrefill,
                                        onSave = { entry ->
                                            val material = entry.stockItemId?.let { id -> state.stockItems.firstOrNull { it.id == id } }
                                            val role = CategoryRole.parse(entry.category.role)
                                            viewModel.record(
                                                entry.type, entry.amountRupiah, entry.description, entry.category.id, entry.category.name, entry.date,
                                                // What the page does not show is kept as it was (DEC-019 correction in place).
                                                reference = existing?.reference,
                                                quantityText = entry.quantityText,
                                                unit = entry.unit,
                                                unitPriceRupiah = entry.unitPriceRupiah,
                                                budgetItemId = existing?.budgetItemId,
                                                costNature = existing?.costNature.takeIf { role == CategoryRole.EXPENSE },
                                                productId = existing?.productId.takeIf { role == CategoryRole.REVENUE },
                                                stockItemId = entry.stockItemId,
                                                usefulLifeMonths = (existing?.usefulLifeMonths ?: PlanAssembly.DEFAULT_ASSET_LIFE_MONTHS).takeIf { role == CategoryRole.ASSET },
                                                editingEventId = existing?.id,
                                                savedMessage = material?.let { UiText.of(R.string.purchase_saved, it.name, entry.quantityText.orEmpty(), it.unit) },
                                                onSuccess = { overlay = Overlay.NONE },
                                            )
                                        },
                                        onClose = { overlay = Overlay.NONE },
                                        onCreateCategory = viewModel::createCategory,
                                        snackbarHost = snackbarHost,
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

                                Overlay.PRODUCT -> key(productEditorId) {
                                    ProductEditorScreen(
                                        title = stringResource(if (productEditorId == null) R.string.editor_title_new else R.string.editor_title_edit),
                                        existing = state.products.firstOrNull { it.product.id == productEditorId },
                                        materials = state.stockItems,
                                        // Closed only once saved: a refusal keeps the owner's input and shows why.
                                        onSave = { viewModel.saveProduct(productEditorId, it) { overlay = productFrom } },
                                        onDelete = productEditorId?.let { id -> { viewModel.archiveProduct(id); overlay = productFrom } },
                                        onClose = { overlay = productFrom },
                                        // Target and sell-from month belong to Rencana usaha (U5): shown only when opened from there.
                                        showPlan = productFrom == Overlay.PLAN,
                                        snackbarHost = snackbarHost,
                                    )
                                }

                                Overlay.STOCK_HISTORY -> {
                                    val record = state.products.firstOrNull { it.product.id == stockHistoryId }
                                    // The product can disappear while the page is open (archived, unit changed, synced away).
                                    LaunchedEffect(record == null) { if (record == null) overlay = Overlay.NONE }
                                    OverlayScaffold(
                                        title = stringResource(R.string.stock_history_title, record?.product?.name.orEmpty()),
                                        onBack = { overlay = Overlay.NONE },
                                        snackbarHost = snackbarHost,
                                    ) {
                                        record?.let { StockHistoryScreen(record = it, onDelete = viewModel::deleteProductMovement) }
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
                                                onManageScope = { manageScope = true },
                                                onMore = onMore,
                                                onQuickRecord = openMoney,
                                                onSaleNote = { openSaleEditor(null) },
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
                                                    onQuickRecord = openMoney,
                                                    onOpenProduct = openProduct,
                                                    onOpenStockHistory = openStockHistory,
                                                    onNewSale = { openSaleEditor(null) },
                                                    onOpenSale = openSaleDetail,
                                                    noteFilter = noteFilter,
                                                    onNoteFilter = { noteFilter = it },
                                                    onOpenBills = openBills,
                                                    onOpenPlan = { overlay = Overlay.PLAN },
                                                    onProduce = onProduce,
                                                    onOpenContact = openContacts,
                                                    onOpenCustomer = openCustomer,
                                                    onEditMoney = editMoney,
                                                    onBuyMaterial = buyMaterial,
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
                                            onManageScope = { manageScope = true },
                                            onMore = onMore,
                                            onQuickRecord = openMoney,
                                            onSaleNote = { openSaleEditor(null) },
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
                                                onQuickRecord = openMoney,
                                                onOpenProduct = openProduct,
                                                    onOpenStockHistory = openStockHistory,
                                                onNewSale = { openSaleEditor(null) },
                                                onOpenSale = openSaleDetail,
                                                noteFilter = noteFilter,
                                                onNoteFilter = { noteFilter = it },
                                                onOpenBills = openBills,
                                                onOpenPlan = { overlay = Overlay.PLAN },
                                                onProduce = onProduce,
                                                onOpenContact = openContacts,
                                                onOpenCustomer = openCustomer,
                                                onEditMoney = editMoney,
                                                onBuyMaterial = buyMaterial,
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

                val pageArticles = articlesFor(destination, overlay)
                if (helpSheet) {
                    HelpSheet(
                        pageArticles = pageArticles,
                        onDismiss = { helpSheet = false },
                        onOpenArticle = openHelp,
                        onOpenCenter = { helpSheet = false; helpArticle = null; overlay = Overlay.HELP },
                    )
                }

                state.products.firstOrNull { it.product.id == produceId }?.let { record ->
                    ProductionSheet(record, state.stockItems, onDismiss = { produceId = null }) { batches, date ->
                        viewModel.recordProduction(record.product.id, batches, date)
                        produceId = null
                    }
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
    onManageScope: () -> Unit,
    onMore: (MoreItem) -> Unit,
    onQuickRecord: (MoneyFormMode) -> Unit,
    onSaleNote: () -> Unit,
    compact: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // Only where recording is the next thing to do. On Laporan the owner is reading, and a
            // permanent action button there would sit on top of the numbers for no reason.
            if (destination in RECORDING_DESTINATIONS && state.selectedProjectId != null) {
                RecordSpeedDial(onRecord = onQuickRecord, onSaleNote = onSaleNote)
            }
        },
        topBar = {
            ScopeHeader(
                state = state,
                onManageScope = onManageScope,
                onMore = onMore,
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

/**
 * The header (U4): the business name, opening the sheet that manages it, and one labelled
 * "Lainnya" menu. The Rencana/Realisasi switch is gone (#4) and the three unlabelled icons
 * (?, folder, person) are entries of that menu, so every door says what it opens (audit P5).
 */
@Composable
private fun ScopeHeader(
    state: AppUiState,
    onManageScope: () -> Unit,
    onMore: (MoreItem) -> Unit,
    compact: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = if (compact) Space.Small else Space.Section, end = Space.Small, top = Space.Small, bottom = Space.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScopeSummary(state = state, onClick = onManageScope, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.Small))
                    Text(stringResource(R.string.header_more), style = MankeuType.Body, fontWeight = FontWeight.SemiBold)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MoreItem.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(stringResource(item.labelRes), style = MankeuType.Body) },
                            leadingIcon = { Icon(item.icon, contentDescription = null) },
                            onClick = { menuOpen = false; onMore(item) },
                        )
                    }
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
    // Units stay hidden until a second one exists (#15).
    val projectLabel = state.projects.firstOrNull { it.id == state.selectedProjectId }?.name?.takeIf { state.projects.size >= 2 }
        ?: if (hasProject && !state.isLoading) null else stringResource(
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
                    style = MankeuType.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (projectLabel != null) Text(
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
    onQuickRecord: (MoneyFormMode) -> Unit,
    onOpenProduct: (String?) -> Unit,
    onOpenStockHistory: (String) -> Unit,
    onNewSale: () -> Unit,
    onOpenSale: (String) -> Unit,
    noteFilter: NoteFilter,
    onNoteFilter: (NoteFilter) -> Unit,
    onOpenBills: (NoteFilter) -> Unit,
    onOpenPlan: () -> Unit,
    onProduce: () -> Unit,
    onOpenContact: (String) -> Unit,
    onOpenCustomer: (String) -> Unit,
    onEditMoney: (MoneyEvent) -> Unit,
    onBuyMaterial: (String, java.math.BigDecimal) -> Unit = { _, _ -> },
    expanded: Boolean,
) {
    val motion = LocalMotion.current
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Screen changes cross-fade with a short glide so the switch reads as deliberate rather than
        // an instant swap. Honours the Gerakan setting: at OFF the durations are zero (a hard cut).
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                (fadeIn(tween(maxOf(motion.mediumMillis, 1))) +
                    slideInVertically(tween(maxOf(motion.mediumMillis, 1))) { it / 12 })
                    .togetherWith(fadeOut(tween(maxOf(motion.fastMillis, 1))))
            },
            label = "screen",
        ) { currentDest ->
            when (currentDest) {
                Destination.HOME -> HomeScreen(
                    state = state,
                    onNavigate = onNavigate,
                    onManageScope = onManageScope,
                    onOpenPlan = onOpenPlan,
                    onQuickRecord = onQuickRecord,
                    onOpenBills = onOpenBills,
                    onNewSale = onNewSale,
                    onProduce = onProduce,
                    onNewProduct = { onOpenProduct(null) },
                    onOpenContact = onOpenContact,
                    onOpenCustomer = onOpenCustomer,
                    expanded = expanded,
                )
                Destination.PRODUCTS -> ProductsScreen(
                    products = state.products,
                    materials = state.stockItems,
                    hasBusiness = state.selectedBusinessId != null,
                    unitName = state.projects.firstOrNull { it.id == state.selectedProjectId }?.name,
                    onManageScope = onManageScope,
                    onOpenProduct = onOpenProduct,
                    onOpenHistory = onOpenStockHistory,
                    onProduce = viewModel::recordProduction,
                    onCountStock = viewModel::countProductStock,
                    onCreateStockItem = viewModel::createStockItem,
                    onRecordStockMovement = viewModel::recordStockMovement,
                    onBuyMaterial = onBuyMaterial,
                    expanded = expanded,
                    multiUnit = state.projects.size >= 2,
                )
                Destination.ENTRY -> EntryScreen(
                    state = state,
                    onEdit = onEditMoney,
                    onDelete = viewModel::deleteTransaction,
                    onManageScope = onManageScope,
                    onNew = onQuickRecord,
                    onOpenSale = onOpenSale,
                    noteFilter = noteFilter,
                    onNoteFilter = onNoteFilter,
                )
                Destination.REPORTS -> ReportsScreen(
                    state = state,
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

/**
 * Kirim (U-3, DEC-030 §8): WhatsApp straight to [link] when there is one, falling back to the share
 * sheet when WhatsApp is not installed; otherwise the share sheet with plain [text].
 */
private fun shareText(context: android.content.Context, text: String, link: String?) {
    val chooser = android.content.Intent.createChooser(
        android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text),
        null,
    )
    if (link != null) {
        try {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link)))
            return
        } catch (_: android.content.ActivityNotFoundException) {
            // No WhatsApp: any other app instead.
        }
    }
    context.startActivity(chooser)
}

/** "Laundry Kiloan (Cab. 2)" → "laundry-kiloan-cab-2", safe in any file picker. */
internal fun String.fileSlug(): String = lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)
