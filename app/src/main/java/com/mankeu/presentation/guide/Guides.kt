package com.mankeu.presentation.guide

import com.mankeu.R
import com.mankeu.domain.BookMode
import com.mankeu.presentation.Destination
import com.mankeu.presentation.Overlay

enum class HelpGroup(val titleRes: Int) {
    START(R.string.help_group_start),
    RECORD(R.string.help_group_record),
    PLAN(R.string.help_group_plan),
    STOCK(R.string.help_group_stock),
    REPORT(R.string.help_group_report),
    DATA(R.string.help_group_data),
    FAQ(R.string.help_group_faq),
}

/** A place an article is about. Only names the screenshot (`help_<name>`) and groups related articles. */
enum class ShowPlace {
    SCOPE_BUSINESS, SCOPE_BOOK, HERO_SUMMARY, HOME_RECORD, HOME_BEP, ENTRY_HISTORY,
    PLAN_PERIOD, PLAN_PRODUCTS, PLAN_COSTS, PLAN_FEASIBILITY, STOCK_ADD, STOCK_LIST,
    REPORTS_TABS, REPORTS_PROFIT, REPORTS_CASHFLOW,
    IMPORT_FILE_PICKER, DATA_HISTORY, DATA_EXPORT, DATA_BACKUP, SETTINGS_SYNC,
}

/**
 * Where "Tunjukkan di aplikasi" takes the owner: the page (and book mode, or overlay) on which
 * [target] lives. [needsUnit] is false only for pages that make sense before any usaha exists.
 */
data class ShowSpec(
    val target: ShowPlace,
    val destination: Destination = Destination.HOME,
    val bookMode: BookMode? = null,
    val overlay: Overlay = Overlay.NONE,
    val needsUnit: Boolean = true,
) {
    /** The screenshot of this place: `res/drawable-nodpi/help_<name>.webp`, made by `HelpScreenshotTest`. */
    val imageName: String
        get() = "help_" + target.name.lowercase() + if (bookMode == BookMode.PROJECTION) "_plan" else ""
}

private val AT_HOME_SCOPE = ShowSpec(ShowPlace.SCOPE_BUSINESS, bookMode = BookMode.REALIZATION, needsUnit = false)
private val AT_HOME_BOOK = ShowSpec(ShowPlace.SCOPE_BOOK, bookMode = BookMode.REALIZATION)
private val AT_HOME_CASH = ShowSpec(ShowPlace.HERO_SUMMARY, bookMode = BookMode.REALIZATION)
private val AT_RECORD = ShowSpec(ShowPlace.HOME_RECORD, bookMode = BookMode.REALIZATION)
private val AT_LEDGER = ShowSpec(ShowPlace.ENTRY_HISTORY, Destination.ENTRY, BookMode.REALIZATION)
private val AT_PLAN_PERIOD = ShowSpec(ShowPlace.PLAN_PERIOD, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_PRODUCTS = ShowSpec(ShowPlace.PLAN_PRODUCTS, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_COSTS = ShowSpec(ShowPlace.PLAN_COSTS, Destination.ENTRY, BookMode.PROJECTION)
private val AT_PLAN_FEASIBILITY = ShowSpec(ShowPlace.PLAN_FEASIBILITY, Destination.ENTRY, BookMode.PROJECTION)
private val AT_STOCK_ADD = ShowSpec(ShowPlace.STOCK_ADD, Destination.PRODUCTS, BookMode.REALIZATION)
private val AT_STOCK_LIST = ShowSpec(ShowPlace.STOCK_LIST, Destination.PRODUCTS, BookMode.REALIZATION)
private val AT_REPORT_TABS = ShowSpec(ShowPlace.REPORTS_TABS, Destination.REPORTS, BookMode.REALIZATION)
private val AT_REPORT_PROFIT = ShowSpec(ShowPlace.REPORTS_PROFIT, Destination.REPORTS, BookMode.REALIZATION)
private val AT_REPORT_CASH = ShowSpec(ShowPlace.REPORTS_CASHFLOW, Destination.REPORTS, BookMode.REALIZATION)
// On Beranda: in Laporan the same card sits on the second tab, which no scroll reaches.
private val AT_REPORT_BEP = ShowSpec(ShowPlace.HOME_BEP, bookMode = BookMode.REALIZATION)
private val AT_DATA_IMPORT = ShowSpec(ShowPlace.IMPORT_FILE_PICKER, overlay = Overlay.DATA)
private val AT_DATA_HISTORY = ShowSpec(ShowPlace.DATA_HISTORY, overlay = Overlay.DATA)
private val AT_DATA_EXPORT = ShowSpec(ShowPlace.DATA_EXPORT, overlay = Overlay.DATA)
private val AT_DATA_BACKUP = ShowSpec(ShowPlace.DATA_BACKUP, overlay = Overlay.DATA, needsUnit = false)
private val AT_SETTINGS_SYNC = ShowSpec(ShowPlace.SETTINGS_SYNC, overlay = Overlay.SETTINGS, needsUnit = false)

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

/** Articles worth offering from the ? button on a page: those whose "Tunjukkan di aplikasi" lands on it. */
fun articlesFor(destination: Destination, bookMode: BookMode, overlay: Overlay): List<HelpArticle> =
    HelpArticle.entries.filter {
        val spec = it.show
        spec.overlay == overlay && (overlay != Overlay.NONE || (spec.destination == destination && (destination != Destination.ENTRY || spec.bookMode == bookMode)))
    }
