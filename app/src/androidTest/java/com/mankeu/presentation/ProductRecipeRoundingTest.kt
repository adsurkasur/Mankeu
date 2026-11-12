package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.R
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductRecipeLineEntity
import com.mankeu.data.ProductUnitEntity
import com.mankeu.domain.StockItem
import com.mankeu.presentation.screens.ProductEditorScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ProductRecipeRoundingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun fractionalRowsExplainWhyTheirRoundedSumDiffersFromBatchTotal() {
        val product = ProductRecord(ProductEntity("p", "project", "Produk uji", "pcs", "1", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("u", "p", "pcs", "1", 10)),
            listOf(ProductRecipeLineEntity("l1", "p", "A", "a", "0.3"), ProductRecipeLineEntity("l2", "p", "B", "b", "0.3")))
        val materials = listOf("a", "b").map { StockItem(it, "business", it.uppercase(), "kg", 5, BigDecimal.ZERO, BigDecimal.ZERO) }
        compose.setContent { MankeuTheme { ProductEditorScreen(existing = product, materials = materials, onSave = {}, onDelete = null) } }
        compose.onAllNodesWithText("Rp2", substring = true).assertCountEquals(2)
        compose.onAllNodesWithText("Rp3", substring = true)[0].assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.editor_rounding_explanation)).assertExists()
    }

    @Test fun noExplanationAppearsWhenRoundedRowsMatchTheTotal() {
        val product = ProductRecord(ProductEntity("p", "project", "Produk uji", "pcs", "1", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("u", "p", "pcs", "1", 10)),
            listOf(ProductRecipeLineEntity("l1", "p", "A", "a", "0.3")))
        val material = StockItem("a", "business", "A", "kg", 5, BigDecimal.ZERO, BigDecimal.ZERO)
        compose.setContent { MankeuTheme { ProductEditorScreen(existing = product, materials = listOf(material), onSave = {}, onDelete = null) } }
        compose.onNodeWithText(compose.activity.getString(R.string.editor_rounding_explanation)).assertDoesNotExist()
    }

    @Test fun incompleteRecipeStillExplainsRoundingOfItsKnownLines() {
        val product = ProductRecord(ProductEntity("p", "project", "Produk uji", "pcs", "1", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("u", "p", "pcs", "1", 10)),
            listOf(ProductRecipeLineEntity("l1", "p", "A", "a", "0.3"), ProductRecipeLineEntity("l2", "p", "B", "b", "0.3"), ProductRecipeLineEntity("l3", "p", "C", "c", "1")))
        val materials = listOf("a", "b").map { StockItem(it, "business", it.uppercase(), "kg", 5, BigDecimal.ZERO, BigDecimal.ZERO) }
        compose.setContent { MankeuTheme { ProductEditorScreen(existing = product, materials = materials, onSave = {}, onDelete = null) } }
        compose.onNodeWithText(compose.activity.getString(R.string.editor_rounding_explanation)).assertExists()
    }
}
