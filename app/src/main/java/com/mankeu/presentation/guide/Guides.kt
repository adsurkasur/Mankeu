package com.mankeu.presentation.guide

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.UploadFile
import com.mankeu.R
import com.mankeu.domain.BookMode
import com.mankeu.presentation.Destination
import com.mankeu.presentation.Overlay
import com.mankeu.presentation.components.TourStep
import com.mankeu.presentation.components.TourTarget

/**
 * Every page tour. [version] goes up whenever a tour's content changes, so an owner who saw the old
 * one is offered the new one once (and only that one), instead of every tour or none.
 */
enum class Tour(val id: String, val version: Int, val steps: List<TourStep>) {
    HOME(
        "home", 3,
        listOf(
            TourStep(TourTarget.SCOPE_BUSINESS, R.string.tour2_home_scope_title, R.string.tour2_home_scope_desc, Icons.Default.Storefront, helpArticle = HelpArticle.START_SCOPE),
            TourStep(TourTarget.SCOPE_BOOK, R.string.tour2_home_book_title, R.string.tour2_home_book_desc, Icons.Default.SwapHoriz, helpArticle = HelpArticle.START_BOOKS),
            TourStep(TourTarget.HERO_SUMMARY, R.string.tour2_home_cash_title, R.string.tour2_home_cash_desc, Icons.Default.AccountBalanceWallet, helpArticle = HelpArticle.FAQ_CASH_NEGATIVE),
            TourStep(TourTarget.HOME_RECORD, R.string.tour2_home_record_title, R.string.tour2_home_record_desc, Icons.Default.AddCircleOutline, helpArticle = HelpArticle.RECORD_INCOME),
            TourStep(TourTarget.HOME_BEP, R.string.tour2_reports_bep_title, R.string.tour2_home_bep_desc, Icons.Default.Insights, helpArticle = HelpArticle.REPORT_BEP_MONTH),
        ),
    ),
    ENTRY(
        "entry", 3,
        listOf(
            TourStep(TourTarget.ENTRY_HISTORY, R.string.tour2_entry_list_title, R.string.tour2_entry_list_desc, Icons.Default.History, helpArticle = HelpArticle.RECORD_FIX),
            TourStep(TourTarget.HOME_RECORD, R.string.tour2_home_record_title, R.string.tour2_entry_record_desc, Icons.Default.AddCircleOutline, helpArticle = HelpArticle.RECORD_INCOME),
        ),
    ),
    ENTRY_FORM(
        "entry_form", 1,
        listOf(
            TourStep(TourTarget.ENTRY_KINDS, R.string.tour2_form_kind_title, R.string.tour2_form_kind_desc, Icons.AutoMirrored.Filled.ReceiptLong),
            TourStep(TourTarget.ENTRY_AMOUNT, R.string.tour2_form_amount_title, R.string.tour2_form_amount_desc, Icons.Default.Payments),
            TourStep(TourTarget.ENTRY_CATEGORY, R.string.tour2_form_category_title, R.string.tour2_form_category_desc, Icons.Default.Category, helpArticle = HelpArticle.RECORD_CAPITAL),
            TourStep(TourTarget.ENTRY_SAVE, R.string.tour2_form_save_title, R.string.tour2_form_save_desc, Icons.Default.Save),
        ),
    ),
    PLAN(
        "plan", 1,
        listOf(
            TourStep(TourTarget.PLAN_PERIOD, R.string.tour2_plan_period_title, R.string.tour2_plan_period_desc, Icons.Default.CalendarMonth, helpArticle = HelpArticle.PLAN_CREATE),
            TourStep(TourTarget.PLAN_FEASIBILITY, R.string.tour2_plan_feasibility_title, R.string.tour2_plan_feasibility_desc, Icons.Default.Insights, helpArticle = HelpArticle.PLAN_FEASIBILITY),
            TourStep(TourTarget.PLAN_PRODUCTS, R.string.tour2_plan_products_title, R.string.tour2_plan_products_desc, Icons.Default.Storefront, helpArticle = HelpArticle.PLAN_PRODUCTS),
            TourStep(TourTarget.PLAN_COSTS, R.string.tour2_plan_costs_title, R.string.tour2_plan_costs_desc, Icons.Default.Calculate, helpArticle = HelpArticle.PLAN_COSTS),
        ),
    ),
    STOCK(
        "stock", 1,
        listOf(
            TourStep(TourTarget.STOCK_ADD, R.string.tour2_stock_add_title, R.string.tour2_stock_add_desc, Icons.Default.Inventory2, helpArticle = HelpArticle.STOCK_OWNER),
            TourStep(TourTarget.STOCK_LIST, R.string.tour2_stock_list_title, R.string.tour2_stock_list_desc, Icons.Default.SwapHoriz, helpArticle = HelpArticle.STOCK_MOVE),
        ),
    ),
    REPORTS(
        "reports", 2,
        listOf(
            TourStep(TourTarget.REPORTS_TABS, R.string.tour2_reports_tabs_title, R.string.tour2_reports_tabs_desc, Icons.Default.Assessment, helpArticle = HelpArticle.REPORT_VS_TARGET),
            TourStep(TourTarget.REPORTS_PROFIT, R.string.tour2_reports_profit_title, R.string.tour2_reports_profit_desc, Icons.Default.Calculate, helpArticle = HelpArticle.REPORT_PROFIT),
            TourStep(TourTarget.REPORTS_CASHFLOW, R.string.tour2_reports_cash_title, R.string.tour2_reports_cash_desc, Icons.AutoMirrored.Filled.ShowChart, helpArticle = HelpArticle.FAQ_PROFIT_VS_CASH),
        ),
    ),
    DATA(
        "data", 3,
        listOf(
            TourStep(TourTarget.IMPORT_FILE_PICKER, R.string.tour2_data_import_title, R.string.tour2_data_import_desc, Icons.Default.UploadFile, helpArticle = HelpArticle.DATA_IMPORT),
            TourStep(TourTarget.DATA_HISTORY, R.string.tour2_data_history_title, R.string.tour2_data_history_desc, Icons.Default.History, helpArticle = HelpArticle.DATA_UNDO),
            TourStep(TourTarget.DATA_EXPORT, R.string.tour2_data_export_title, R.string.tour2_data_export_desc, Icons.Default.FileDownload, helpArticle = HelpArticle.REPORT_EXPORT),
            TourStep(TourTarget.DATA_BACKUP, R.string.tour2_data_backup_title, R.string.tour2_data_backup_desc, Icons.Default.Security, helpArticle = HelpArticle.DATA_BACKUP),
        ),
    ),
    SCOPE(
        "scope", 2,
        listOf(
            TourStep(TourTarget.SCOPE_ADD_BUSINESS, R.string.tour2_scope_business_title, R.string.tour2_scope_business_desc, Icons.Default.AddBusiness, helpArticle = HelpArticle.START_SCOPE),
            TourStep(TourTarget.SCOPE_ADD_FIELD, R.string.tour2_scope_unit_title, R.string.tour2_scope_unit_desc, Icons.Default.Landscape, helpArticle = HelpArticle.STOCK_OWNER),
        ),
    ),
    SETTINGS(
        "settings", 2,
        listOf(
            TourStep(TourTarget.SETTINGS_GUIDES, R.string.tour2_settings_guides_title, R.string.tour2_settings_guides_desc, Icons.Default.TipsAndUpdates),
            TourStep(TourTarget.SETTINGS_DISPLAY, R.string.tour2_settings_display_title, R.string.tour2_settings_display_desc, Icons.Default.Palette),
            TourStep(TourTarget.SETTINGS_DRIVE, R.string.tour2_settings_drive_title, R.string.tour2_settings_drive_desc, Icons.Default.CloudUpload, helpArticle = HelpArticle.DATA_BACKUP),
        ),
    ),
}

/** Which page tour belongs to what is on screen. Null where the page has none. */
fun tourFor(destination: Destination, bookMode: BookMode, overlay: Overlay): Tour? = when (overlay) {
    Overlay.DATA -> Tour.DATA
    Overlay.SETTINGS -> Tour.SETTINGS
    Overlay.HELP -> null
    Overlay.NONE -> when (destination) {
        Destination.HOME -> Tour.HOME
        Destination.ENTRY -> if (bookMode == BookMode.PROJECTION) Tour.PLAN else Tour.ENTRY
        Destination.STOCK -> Tour.STOCK
        Destination.REPORTS -> Tour.REPORTS
    }
}

enum class HelpGroup(val titleRes: Int) {
    START(R.string.help_group_start),
    RECORD(R.string.help_group_record),
    PLAN(R.string.help_group_plan),
    STOCK(R.string.help_group_stock),
    REPORT(R.string.help_group_report),
    DATA(R.string.help_group_data),
    FAQ(R.string.help_group_faq),
}

/**
 * Where "Tunjukkan di aplikasi" takes the owner: the page (and book mode, or overlay) on which
 * [target] lives. [needsUnit] is false only for pages that make sense before any usaha exists.
 */
data class ShowSpec(
    val target: TourTarget,
    val destination: Destination = Destination.HOME,
    val bookMode: BookMode? = null,
    val overlay: Overlay = Overlay.NONE,
    val needsUnit: Boolean = true,
) {
    /** The screenshot of this place: `res/drawable-nodpi/help_<name>.webp`, made by `HelpScreenshotTest`. */
    val imageName: String
        get() = "help_" + target.name.lowercase() + if (bookMode == BookMode.PROJECTION) "_plan" else ""
}

private val AT_HOME_SCOPE = ShowSpec(TourTarget.SCOPE_BUSINESS, bookMode = BookMode.REALIZATION, needsUnit = false)
private val AT_HOME_BOOK = ShowSpec(TourTarget.SCOPE_BOOK, bookMode = BookMode.REALIZATION)
private val AT_HOME_CASH = ShowSpec(TourTarget.HERO_SUMMARY, bookMode = BookMode.REALIZATION)
private val AT_RECORD = ShowSpec(TourTarget.HOME_RECORD, bookMode = BookMode.REALIZATION)
private val AT_LEDGER = ShowSpec(TourTarget.ENTRY_HISTORY, Destination.ENTRY, BookMode.REALIZATION)
private val AT_PLAN_PERIOD = ShowSpec(TourTarget.PLAN_PERIOD, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_PRODUCTS = ShowSpec(TourTarget.PLAN_PRODUCTS, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_COSTS = ShowSpec(TourTarget.PLAN_COSTS, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_FEASIBILITY = ShowSpec(TourTarget.PLAN_FEASIBILITY, Destination.ENTRY, BookMode.PROJECTION)
private val AT_STOCK_ADD = ShowSpec(TourTarget.STOCK_ADD, Destination.STOCK, BookMode.REALIZATION)
private val AT_STOCK_LIST = ShowSpec(TourTarget.STOCK_LIST, Destination.STOCK, BookMode.REALIZATION)
private val AT_REPORT_TABS = ShowSpec(TourTarget.REPORTS_TABS, Destination.REPORTS, BookMode.REALIZATION)
private val AT_REPORT_PROFIT = ShowSpec(TourTarget.REPORTS_PROFIT, Destination.REPORTS, BookMode.REALIZATION)
private val AT_REPORT_CASH = ShowSpec(TourTarget.REPORTS_CASHFLOW, Destination.REPORTS, BookMode.REALIZATION)
// On Beranda: in Laporan the same card sits on the second tab, which no scroll reaches.
private val AT_REPORT_BEP = ShowSpec(TourTarget.HOME_BEP, bookMode = BookMode.REALIZATION)
private val AT_DATA_IMPORT = ShowSpec(TourTarget.IMPORT_FILE_PICKER, overlay = Overlay.DATA)
private val AT_DATA_HISTORY = ShowSpec(TourTarget.DATA_HISTORY, overlay = Overlay.DATA)
private val AT_DATA_EXPORT = ShowSpec(TourTarget.DATA_EXPORT, overlay = Overlay.DATA)
private val AT_DATA_BACKUP = ShowSpec(TourTarget.DATA_BACKUP, overlay = Overlay.DATA, needsUnit = false)
private val AT_SETTINGS_SYNC = ShowSpec(TourTarget.SETTINGS_SYNC, overlay = Overlay.SETTINGS, needsUnit = false)

/** Every help article: task-first titles, a short body, numbered steps (one per line), and where to show it. */
enum class HelpArticle(
    val group: HelpGroup,
    val titleRes: Int,
    val bodyRes: Int,
    val stepsRes: Int,
    val show: ShowSpec,
) {
    START_SCOPE(HelpGroup.START, R.string.help_start_scope_title, R.string.help_start_scope_body, R.string.help_start_scope_steps, AT_HOME_SCOPE),
    START_BOOKS(HelpGroup.START, R.string.help_start_books_title, R.string.help_start_books_body, R.string.help_start_books_steps, AT_HOME_BOOK),
    START_HOME(HelpGroup.START, R.string.help_start_home_title, R.string.help_start_home_body, R.string.help_start_home_steps, AT_HOME_CASH),

    RECORD_INCOME(HelpGroup.RECORD, R.string.help_record_income_title, R.string.help_record_income_body, R.string.help_record_income_steps, AT_RECORD),
    RECORD_EXPENSE(HelpGroup.RECORD, R.string.help_record_expense_title, R.string.help_record_expense_body, R.string.help_record_expense_steps, AT_RECORD),
    RECORD_CAPITAL(HelpGroup.RECORD, R.string.help_record_capital_title, R.string.help_record_capital_body, R.string.help_record_capital_steps, AT_RECORD),
    RECORD_FIX(HelpGroup.RECORD, R.string.help_record_fix_title, R.string.help_record_fix_body, R.string.help_record_fix_steps, AT_LEDGER),

    PLAN_CREATE(HelpGroup.PLAN, R.string.help_plan_create_title, R.string.help_plan_create_body, R.string.help_plan_create_steps, AT_PLAN_PERIOD),
    PLAN_PRODUCTS(HelpGroup.PLAN, R.string.help_plan_products_title, R.string.help_plan_products_body, R.string.help_plan_products_steps, AT_PLAN_PRODUCTS),
    PLAN_SEASONAL(HelpGroup.PLAN, R.string.help_plan_seasonal_title, R.string.help_plan_seasonal_body, R.string.help_plan_seasonal_steps, AT_PLAN_PRODUCTS),
    PLAN_COSTS(HelpGroup.PLAN, R.string.help_plan_costs_title, R.string.help_plan_costs_body, R.string.help_plan_costs_steps, AT_PLAN_COSTS),
    PLAN_FEASIBILITY(HelpGroup.PLAN, R.string.help_plan_feasibility_title, R.string.help_plan_feasibility_body, R.string.help_plan_feasibility_steps, AT_PLAN_FEASIBILITY),

    STOCK_OWNER(HelpGroup.STOCK, R.string.help_stock_owner_title, R.string.help_stock_owner_body, R.string.help_stock_owner_steps, AT_STOCK_ADD),
    STOCK_MOVE(HelpGroup.STOCK, R.string.help_stock_move_title, R.string.help_stock_move_body, R.string.help_stock_move_steps, AT_STOCK_LIST),

    REPORT_PROFIT(HelpGroup.REPORT, R.string.help_report_profit_title, R.string.help_report_profit_body, R.string.help_report_profit_steps, AT_REPORT_PROFIT),
    REPORT_BEP_MONTH(HelpGroup.REPORT, R.string.help_report_bep_title, R.string.help_report_bep_body, R.string.help_report_bep_steps, AT_REPORT_BEP),
    REPORT_VS_TARGET(HelpGroup.REPORT, R.string.help_report_target_title, R.string.help_report_target_body, R.string.help_report_target_steps, AT_REPORT_TABS),
    REPORT_EXPORT(HelpGroup.REPORT, R.string.help_report_export_title, R.string.help_report_export_body, R.string.help_report_export_steps, AT_DATA_EXPORT),

    DATA_IMPORT(HelpGroup.DATA, R.string.help_data_import_title, R.string.help_data_import_body, R.string.help_data_import_steps, AT_DATA_IMPORT),
    DATA_UNDO(HelpGroup.DATA, R.string.help_data_undo_title, R.string.help_data_undo_body, R.string.help_data_undo_steps, AT_DATA_HISTORY),
    DATA_BACKUP(HelpGroup.DATA, R.string.help_data_backup_title, R.string.help_data_backup_body, R.string.help_data_backup_steps, AT_DATA_BACKUP),
    DATA_RESTORE(HelpGroup.DATA, R.string.help_data_restore_title, R.string.help_data_restore_body, R.string.help_data_restore_steps, AT_DATA_BACKUP),
    DATA_SYNC(HelpGroup.DATA, R.string.help_data_sync_title, R.string.help_data_sync_body, R.string.help_data_sync_steps, AT_SETTINGS_SYNC),

    FAQ_CASH_NEGATIVE(HelpGroup.FAQ, R.string.help_faq_cash_title, R.string.help_faq_cash_body, R.string.help_faq_cash_steps, AT_HOME_CASH),
    FAQ_NO_BEP(HelpGroup.FAQ, R.string.help_faq_bep_title, R.string.help_faq_bep_body, R.string.help_faq_bep_steps, AT_PLAN_PRODUCTS),
    FAQ_IMPORT_REJECTED(HelpGroup.FAQ, R.string.help_faq_import_title, R.string.help_faq_import_body, R.string.help_faq_import_steps, AT_DATA_IMPORT),
    FAQ_STOCK_MISSING(HelpGroup.FAQ, R.string.help_faq_stock_title, R.string.help_faq_stock_body, R.string.help_faq_stock_steps, AT_STOCK_ADD),
    FAQ_PROFIT_VS_CASH(HelpGroup.FAQ, R.string.help_faq_profit_title, R.string.help_faq_profit_body, R.string.help_faq_profit_steps, AT_REPORT_CASH),
}

/** Articles worth offering from the ? button on a page: those that show something on it, first. */
fun articlesFor(tour: Tour?): List<HelpArticle> {
    val targets = tour?.steps?.map { it.target }?.toSet().orEmpty()
    return HelpArticle.entries.filter { it.show.target in targets }
}
