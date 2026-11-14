package com.mankeu.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.data.*
import com.mankeu.domain.PlanCalculator
import com.mankeu.exporter.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PlanPdfEvidenceTest {
    @Test fun exportIncompletePlanForInspection() {
        val product = ProductRecord(ProductEntity("p", "u", "Keripik", "bungkus", batchYieldText = "1", targetPerMonthText = "10", createdAtEpochMillis = 0), listOf(ProductUnitEntity("pu", "p", "bungkus", "1", 8_000)), emptyList())
        val project = ProjectEntity("u", "b", "Utama", true, createdAtEpochMillis = 0)
        val plan = PlanCalculator.analyse(PlanAssembly.planInputs(project, listOf(product), emptyList(), emptyList(), LocalDate.of(2026, 11, 14), emptyMap()))
        val projection = FinanceExportBook(BookEntity("book", "u", "PROJECTION", 0), emptyList(), emptyList(), plan.summary)
        val bundle = FinanceExportBundle(BusinessEntity("b", "Usaha", createdAtEpochMillis = 0), project, emptyList(), listOf(projection), emptyList(), emptyList(), emptyList(), products = listOf(product), plan = plan)
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "plan-incomplete-evidence.pdf")
        file.writeBytes(FinancePdfExporter().export(bundle))
        assertTrue(file.length() > 0)
    }
}
