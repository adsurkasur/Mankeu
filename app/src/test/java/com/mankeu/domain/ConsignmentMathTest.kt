package com.mankeu.domain

import com.mankeu.domain.ConsignmentMove.Kind
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsignmentMathTest {
    private fun move(contact: String, kind: Kind, qty: String, product: String = "p") =
        ConsignmentMove(contact, product, BigDecimal(qty), kind)

    @Test fun sentSoldReturnedNetsToZero() {
        val b = ConsignmentMath.balances(listOf(move("B", Kind.SENT, "400"), move("B", Kind.SOLD, "240"), move("B", Kind.RETURNED, "160")))
        assertEquals(0, b.getValue("B" to "p").compareTo(BigDecimal.ZERO))
    }

    @Test fun balancesAreKeptPerStoreAndProduct() {
        val b = ConsignmentMath.balances(listOf(
            move("A", Kind.SENT, "10"), move("B", Kind.SENT, "20"), move("B", Kind.SOLD, "5"),
            move("B", Kind.SENT, "7", product = "q"),
        ))
        assertEquals(0, b.getValue("A" to "p").compareTo(BigDecimal("10")))
        assertEquals(0, b.getValue("B" to "p").compareTo(BigDecimal("15")))
        assertEquals(0, b.getValue("B" to "q").compareTo(BigDecimal("7")))
    }
}
