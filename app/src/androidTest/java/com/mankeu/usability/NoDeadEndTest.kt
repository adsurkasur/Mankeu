package com.mankeu.usability

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.AppViewModel
import com.mankeu.presentation.MankeuApp
import com.mankeu.presentation.MotionMode
import com.mankeu.presentation.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * Appendix C §1.3 acceptance contracts, not a claim that the future destinations exist today.
 * Future Indonesian copy comes from Appendix A §§4–8, 11–13; no invented production routes/tags.
 * @Ignore is method-level so each missing destination remains visible in runner reports.
 */
@RunWith(AndroidJUnit4::class)
class NoDeadEndTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var projectId: String
    private lateinit var savedPrefs: Map<String, *>
    private val savedLocale = Locale.getDefault()
    private val viewModels = ViewModelStore()
    private val counter = TapCounter()

    @Before fun setUp() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        savedPrefs = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name)
            .putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        runBlocking { projectId = repository.createBusinessWithUnit("Usaha uji sintetis", "Utama").second }
        // Only a business and its required unit/books/categories. No products, notes, contacts or financial data.
        val vm = AppViewModel(repository, FinanceWorkbookImporter(),
            EncryptedBackupManager(database, File(context.cacheDir, "no-dead-end-test.aabk")),
            GoogleIdentity(context), GoogleDriveBackupClient())
        viewModels.put("no-dead-end", vm)
        setLocale(Locale.forLanguageTag("id"))
        compose.setContent { MankeuApp(vm) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Usaha uji sintetis").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    @After fun tearDown() {
        compose.runOnUiThread { viewModels.clear() }
        database.close()
        setLocale(savedLocale)
        context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE).edit().clear().apply {
            savedPrefs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }.commit()
    }

    @Ignore("PK-2.3 + PK-2.4 + PK-2.5: Beranda has no empty_action Aku jual/Aku beli or quick forms yet")
    @Test fun emptyHomeOpensBothSolvingForms() {
        openBothMoneyForms()
    }

    @Ignore("PK-2.7 + PK-2.4 + PK-2.5: Catatan has one legacy empty CTA, no two empty_action quick forms")
    @Test fun emptyCatatanOpensBothSolvingForms() {
        click(R.string.nav_entry)
        openBothMoneyForms()
    }

    @Ignore("PK-2.8: Produk has no empty_action leading to the three-field Barang editor")
    @Test fun emptyItemsOpensThreeFieldEditor() {
        click(R.string.nav_products)
        emptyAction("Tambah")
        field("Namanya").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(2) // name + price; unit is a picker
        compose.onNode(hasText("Dijual per") and hasClickAction()).assertIsDisplayed()
        field("Harganya").assertIsDisplayed()
    }

    @Ignore("PK-2.4 (requires PK-1.3): legacy sale requires products; free-name sale cannot be saved")
    @Test fun saleWithoutItemsSavesTypedNameWithoutCreatingAProduct() {
        emptyAction("Aku jual")
        counter.performTextInput(field("Jual apa?"), "Jualan sintetis")
        counter.performTextInput(field("Harganya (satu)"), "10000")
        click(R.string.action_save)
        compose.waitUntil(5_000) { runBlocking { repository.sales(projectId).first().size == 1 } }
        runBlocking {
            val sale = repository.sales(projectId).first().single()
            assertEquals(10_000L, sale.total)
            assertEquals(10_000L, sale.paid)
            assertEquals(0, repository.products(projectId).first().size)
        }
        click(R.string.nav_entry)
        compose.onNode(hasText("Jualan sintetis", substring = true)).assertIsDisplayed()
    }

    @Ignore("PK-3.3: standalone recipe calculator and empty_action ingredient/cost sheet do not exist")
    @Test fun recipeWithoutMaterialsOpensIngredientOrCostForm() {
        click("Hitung modal resep")
        emptyAction("+ Tambah bahan atau biaya")
        field("Namanya").assertIsDisplayed()
        field("Dipakai berapa").assertIsDisplayed()
        field("Harga belinya").assertIsDisplayed()
    }

    @Ignore("PK-3.5 (requires PK-3.4/K-14): legacy plan has no empty_action simple feasibility item form")
    @Test fun feasibilityWithoutItemsOpensSimpleItemForm() {
        click("Layak nggak usahaku?")
        emptyAction("+ Tambah barang")
        field("Namanya").assertIsDisplayed()
        field("Harganya").assertIsDisplayed()
        field("Modal per satuan").assertIsDisplayed()
    }

    @Ignore("PK-2.6 + PK-2.2: legacy Kontak has no empty_action Tambah pembeli contract")
    @Test fun emptyBuyersOpensBuyerForm() {
        click(R.string.header_more)
        click(R.string.contacts_title)
        emptyAction("Tambah pembeli")
        field(compose.activity.getString(R.string.contact_name)).assertIsDisplayed()
        compose.onNode(hasText(compose.activity.getString(R.string.action_save)) and hasClickAction()).assertIsDisplayed()
    }

    @Ignore("PK-2.2 + PK-2.4: Laporan legacy empty CTA navigates to Catatan instead of opening Aku jual")
    @Test fun emptyReportsOpensSaleForm() {
        click(R.string.nav_reports)
        emptyAction("Aku jual")
        field("Jual apa?").assertIsDisplayed()
        field("Harganya (satu)").assertIsDisplayed()
    }

    private fun openBothMoneyForms() {
        emptyAction("Aku jual")
        field("Jual apa?").assertIsDisplayed()
        field("Harganya (satu)").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        emptyAction("Aku beli")
        field("Beli apa?").assertIsDisplayed()
        field("Bayar berapa?").assertIsDisplayed()
    }

    private fun emptyAction(text: String) {
        val node = compose.onNode(hasTestTag("empty_action") and hasText(text))
        node.assertIsDisplayed().assertHasClickAction()
        counter.performClick(node)
        compose.waitForIdle()
    }

    private fun click(resId: Int) = click(compose.activity.getString(resId))
    private fun click(text: String) {
        counter.performClick(compose.onNode(hasText(text) and hasClickAction()))
        compose.waitForIdle()
    }
    private fun field(label: String) = compose.onNode(hasText(label) and hasSetTextAction())
    private fun setLocale(locale: Locale) {
        Locale.setDefault(locale)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(locale) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
    }
}
