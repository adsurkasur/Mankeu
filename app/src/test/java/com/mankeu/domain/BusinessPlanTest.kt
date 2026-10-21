package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

/** DEC-014 plan & actual BEP, pinned to the lecturer's benchmark (`docs/archive/paket_audit_agentic_ai_mankeu.md`). */
class BusinessPlanTest {
    private val month = YearMonth.of(2026, 11)
    private val kopi = PlanProduct(
        id = "kopi",
        name = "Kopi susu",
        unit = "cup",
        priceRupiah = 10_000,
        targetPerMonth = BigDecimal("700"),
        components = listOf(CostComponent("Bubuk kopi/teh + susu", 3_500), CostComponent("Cup + sedotan + tutup", 1_000), CostComponent("Kantong plastik / seal", 500)),
    )
    private val benchmarkFixed = listOf(
        PlanLine("Sewa lapak/booth", CategoryRole.EXPENSE, 1_000_000, Recurrence.MONTHLY, CostNature.FIXED),
        PlanLine("Listrik & air", CategoryRole.EXPENSE, 200_000, Recurrence.MONTHLY, CostNature.FIXED),
        PlanLine("Gaji pembantu", CategoryRole.EXPENSE, 800_000, Recurrence.MONTHLY, CostNature.FIXED),
    )

    @Test
    fun `lecturer benchmark - 400 cup, Rp4 juta, RC 1,27, BC 0,27`() {
        val plan = PlanCalculator.analyse(PlanInputs(listOf(month), listOf(kopi), benchmarkFixed))

        assertEquals(2_000_000L, plan.fixedCost.totalRupiah)
        assertEquals(5_000L, plan.products.single().variableCostPerUnit)
        assertEquals(BigDecimal("400.000"), plan.breakEvenUnits)
        assertEquals(4_000_000L, plan.breakEvenRupiah)
        assertEquals(7_000_000L, plan.revenueRupiah)
        assertEquals(5_500_000L, plan.totalCostRupiah)
        assertEquals(1_500_000L, plan.profitRupiah)
        assertEquals(BigDecimal("1.27"), plan.revenueCostRatio)
        assertEquals(BigDecimal("0.27"), plan.benefitCostRatio)
        assertTrue(plan.feasible!!)
    }

    @Test
    fun `actual month at 700 cups gives the same ratios and no variance when spend matches the recipe`() {
        val actual = ActualBepCalculator.analyse(
            listOf(kopi),
            ActualMonthInputs(month, mapOf("kopi" to BigDecimal("700")), 7_000_000, 0, 2_000_000, 3_500_000, 0),
        )

        assertEquals(4_000_000L, actual.breakEvenRupiah)
        assertTrue(actual.reached!!)
        assertEquals(0L, actual.variableVarianceRupiah)
        assertEquals(BigDecimal("1.27"), actual.revenueCostRatio)
        assertEquals(BigDecimal("0.27"), actual.benefitCostRatio)
    }

    @Test
    fun `overspending on variable costs shows as a positive variance`() {
        val actual = ActualBepCalculator.analyse(
            listOf(kopi),
            ActualMonthInputs(month, mapOf("kopi" to BigDecimal("700")), 7_000_000, 0, 2_000_000, 3_800_000, 0),
        )
        assertEquals(300_000L, actual.variableVarianceRupiah)
    }

    @Test
    fun `two products break even on the weighted margin of their mix`() {
        val teh = PlanProduct("teh", "Teh susu", "cup", 8_000, BigDecimal("300"), listOf(CostComponent("Teh + susu + cup", 3_000)))
        val plan = PlanCalculator.analyse(PlanInputs(listOf(month), listOf(kopi, teh), benchmarkFixed))

        // CM̄ = (700×5.000 + 300×5.000) / 1.000 = 5.000 → 400 units in a 70:30 mix.
        assertEquals(BigDecimal("400.000"), plan.breakEvenUnits)
        assertEquals(BigDecimal("280.000"), plan.products[0].breakEvenUnits)
        assertEquals(BigDecimal("120.000"), plan.products[1].breakEvenUnits)
        assertEquals(2_800_000L + 960_000L, plan.breakEvenRupiah)
    }

    @Test
    fun `an asset adds straight-line depreciation to the monthly fixed cost`() {
        val plan = PlanCalculator.analyse(
            PlanInputs(listOf(month), listOf(kopi), benchmarkFixed + PlanLine("Mesin espresso", CategoryRole.ASSET, 12_000_000, usefulLifeMonths = 24)),
        )
        assertEquals(listOf("Mesin espresso" to 500_000L), plan.fixedCost.depreciation)
        assertEquals(2_500_000L, plan.fixedCost.totalRupiah)
        assertEquals(BigDecimal("500.000"), plan.breakEvenUnits)
        // Buying it is cash out in the plan; depreciation is not.
        assertEquals(-12_000_000L, plan.schedule.single().investingRupiah)
    }

    @Test
    fun `a component priced per pack is divided by its yield`() {
        val gula = CostComponent("Gula 1 kg", 15_000, BigDecimal("50"))
        assertEquals(BigDecimal("300.000000"), gula.perUnit)
    }

    @Test
    fun `without targets each product breaks even alone and no total is claimed`() {
        val plan = PlanCalculator.analyse(PlanInputs(listOf(month), listOf(kopi.copy(targetPerMonth = null)), benchmarkFixed))
        assertFalse(plan.mixDefined)
        assertNull(plan.breakEvenUnits)
        assertEquals(BigDecimal("400.000"), plan.products.single().breakEvenUnits)
        assertEquals(4_000_000L, plan.products.single().breakEvenRupiah)
    }

    @Test
    fun `a product sold below its variable cost is flagged, never divided by zero`() {
        val rugi = kopi.copy(priceRupiah = 4_000)
        val plan = PlanCalculator.analyse(PlanInputs(listOf(month), listOf(rugi), benchmarkFixed))
        assertTrue(plan.products.single().sellsAtALoss)
        assertNull(plan.breakEvenUnits)
    }

    @Test
    fun `capital need is the deepest cumulative deficit of the plan`() {
        val months = listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12))
        val plan = PlanCalculator.analyse(
            PlanInputs(months, listOf(kopi), benchmarkFixed + PlanLine("Mesin", CategoryRole.ASSET, 12_000_000, month = months[0], usefulLifeMonths = 24)),
        )
        // Month 1: +1,5 jt operating − 12 jt asset = −10,5 jt; month 2: +1,5 jt → −9 jt.
        assertEquals(10_500_000L, plan.capitalNeedRupiah)
        assertEquals(10_500_000L, plan.capitalShortfallRupiah)
    }

    @Test
    fun `planned capital does not hide the need, it reduces the shortfall`() {
        val months = listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12))
        val plan = PlanCalculator.analyse(
            PlanInputs(
                months,
                listOf(kopi),
                benchmarkFixed + listOf(
                    PlanLine("Mesin", CategoryRole.ASSET, 12_000_000, month = months[0], usefulLifeMonths = 24),
                    PlanLine("Setoran", CategoryRole.CAPITAL_IN, 8_000_000, month = months[0]),
                ),
            ),
        )
        assertEquals(10_500_000L, plan.capitalNeedRupiah)
        assertEquals(8_000_000L, plan.plannedCapitalRupiah)
        assertEquals(2_500_000L, plan.capitalShortfallRupiah)
    }

    @Test
    fun `profit is zero at break-even even with variable costs outside recipes and other income`() {
        // Seasonal farm: seed and fertiliser are variable but not per kg; a subsidy is other income.
        val months = (11..12).map { YearMonth.of(2026, it) } + (1..2).map { YearMonth.of(2027, it) }
        val cabai = PlanProduct("cabai", "Cabai", "kg", 35_000, BigDecimal("250"), listOf(CostComponent("Panen & angkut", 3_000)))
        val plan = PlanCalculator.analyse(
            PlanInputs(
                months,
                listOf(cabai),
                listOf(
                    PlanLine("Upah", CategoryRole.EXPENSE, 2_000_000, Recurrence.MONTHLY, CostNature.FIXED),
                    PlanLine("Sewa lahan", CategoryRole.EXPENSE, 4_000_000, Recurrence.ONCE, CostNature.FIXED, months[0]),
                    PlanLine("Bibit & pupuk", CategoryRole.EXPENSE, 8_000_000, Recurrence.ONCE, CostNature.VARIABLE, months[0]),
                    PlanLine("Subsidi", CategoryRole.REVENUE, 2_000_000, Recurrence.ONCE, month = months[1]),
                ),
            ),
        )
        // Base = 2 jt + 1 jt (lahan/4) + 2 jt (bibit/4) − 0,5 jt (subsidi/4) = 4,5 jt; CM 32.000.
        assertEquals(4_500_000L, plan.breakEvenBaseRupiah)
        assertEquals(BigDecimal("140.625"), plan.breakEvenUnits)
        val profitAtBep = plan.breakEvenUnits!!.multiply(BigDecimal.valueOf(32_000)).toLong() - plan.breakEvenBaseRupiah
        assertEquals(0L, profitAtBep)
        assertEquals(4_921_875L, plan.breakEvenRupiah)
    }

    /**
     * Chilli over four months, harvest from month 3 (DEC-017). Hand-computed: fixed Rp1.000.000 a
     * month, seed Rp2.000.000 once (variable, spread Rp500.000), so the margin must cover
     * Rp1.500.000 × 4 = Rp6.000.000 in two selling months at Rp18.000/kg → 166,667 kg per selling
     * month. Cash: −3,0 jt, −1,0 jt, +4,4 jt, +4,4 jt → capital need Rp4.000.000 (Rp0 if it sold from
     * month 1), period profit Rp4.800.000 = 4 × Rp1.200.000 per month.
     */
    @Test
    fun `a seasonal product sells only from its start month and the waiting months need capital`() {
        val months = (0L..3L).map { YearMonth.of(2026, 11).plusMonths(it) }
        val cabai = PlanProduct("cabai", "Cabai", "kg", 20_000, BigDecimal("300"), listOf(CostComponent("Panen & kemas", 2_000)), sellFromMonth = 3)
        val lines = listOf(
            PlanLine("Sewa lahan", CategoryRole.EXPENSE, 1_000_000, Recurrence.MONTHLY, CostNature.FIXED),
            PlanLine("Bibit & pupuk", CategoryRole.EXPENSE, 2_000_000, Recurrence.ONCE, CostNature.VARIABLE),
        )

        val plan = PlanCalculator.analyse(PlanInputs(months, listOf(cabai), lines))

        assertEquals(listOf(-3_000_000L, -1_000_000L, 4_400_000L, 4_400_000L), plan.schedule.map { it.operatingRupiah })
        assertEquals(4_000_000L, plan.capitalNeedRupiah)
        assertEquals(3_000_000L, plan.revenueRupiah)
        assertEquals(1_200_000L, plan.profitRupiah)
        assertEquals(4_800_000L, plan.summary.profitRupiah)
        assertEquals(BigDecimal("166.667"), plan.products.single().breakEvenUnits)
        assertEquals(1_666_667L, plan.breakEvenRupiah)
        assertEquals(0L, PlanCalculator.analyse(PlanInputs(months, listOf(cabai.copy(sellFromMonth = 1)), lines)).capitalNeedRupiah)
    }

    @Test
    fun `a product starting after the period ends sells nothing and has no break-even`() {
        val late = kopi.copy(sellFromMonth = 2)

        val plan = PlanCalculator.analyse(PlanInputs(listOf(month), listOf(late), benchmarkFixed))

        assertEquals(0L, plan.revenueRupiah)
        assertNull(plan.products.single().breakEvenUnits)
        assertNull(plan.breakEvenRupiah)
    }

    @Test
    fun `mix break-even is exact to the rupiah`() {
        // Laundry simulation: found as a Rp2 drift on the emulator when share and CM were rounded first.
        val recipe = listOf(CostComponent("Deterjen", 45_000, BigDecimal("30")), CostComponent("Plastik", 500), CostComponent("Listrik", 700))
        val plan = PlanCalculator.analyse(
            PlanInputs(
                listOf(month),
                listOf(
                    PlanProduct("a", "Cuci setrika", "kg", 8_000, BigDecimal("1500"), recipe),
                    PlanProduct("b", "Cuci kilat", "kg", 12_000, BigDecimal("300"), recipe),
                ),
                listOf(PlanLine("Tetap", CategoryRole.EXPENSE, 6_100_000, Recurrence.MONTHLY, CostNature.FIXED)),
            ),
        )
        // 6,1 jt × 1500 × 8000 / 10,74 jt = 6.815.642,46; 6,1 jt × 300 × 12000 / 10,74 jt = 2.044.692,74.
        assertEquals(listOf(6_815_642L, 2_044_693L), plan.products.map { it.breakEvenRupiah })
        assertEquals(8_860_335L, plan.breakEvenRupiah)
        assertEquals(listOf(BigDecimal("851.955"), BigDecimal("170.391")), plan.products.map { it.breakEvenUnits })
    }
}
