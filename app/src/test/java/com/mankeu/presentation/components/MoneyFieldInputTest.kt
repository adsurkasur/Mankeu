package com.mankeu.presentation.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** U1 fix round (I2): the money field's input rules and the dotted display's cursor mapping. */
class MoneyFieldInputTest {
    private val expected = listOf(
        "" to "", "1" to "1", "12" to "12", "123" to "123", "1234" to "1.234", "12345" to "12.345",
        "123456" to "123.456", "1234567" to "1.234.567", "1250000" to "1.250.000",
        "123456789012345" to "123.456.789.012.345",
    )

    @Test
    fun groupsThousands() {
        expected.forEach { (digits, grouped) -> assertEquals(digits, grouped, groupThousands(digits)) }
    }

    @Test
    fun cursorMappingRoundTripsAndNeverMovesBackwardsForEveryLength() {
        for (n in 0..15) {
            val digits = "123456789012345".take(n)
            val shown = ThousandsTransformation.filter(AnnotatedString(digits))
            val map = shown.offsetMapping
            val len = shown.text.length
            assertEquals("n=$n", groupThousands(digits), shown.text.text)
            assertEquals("start n=$n", 0, map.originalToTransformed(0))
            assertEquals("end n=$n", len, map.originalToTransformed(n))
            assertEquals("start back n=$n", 0, map.transformedToOriginal(0))
            assertEquals("end back n=$n", n, map.transformedToOriginal(len))
            for (i in 0..n) {
                val t = map.originalToTransformed(i)
                assertTrue("in range n=$n i=$i", t in 0..len)
                assertEquals("round trip n=$n i=$i", i, map.transformedToOriginal(t))
                if (i > 0) assertTrue("monotonic n=$n i=$i", t > map.originalToTransformed(i - 1))
            }
            for (t in 1..len) {
                assertTrue("back monotonic n=$n t=$t", map.transformedToOriginal(t) >= map.transformedToOriginal(t - 1))
            }
        }
    }

    @Test
    fun sanitizeKeepsBareDigitsOnly() {
        listOf(
            "Rp 1.250.000" to "1250000",
            "Rp 0012.500" to "12500",
            " 7 500 " to "7500",
            "0000" to "",
            "0" to "",
            "000123" to "123",
            // 20 digits, past Long.MAX_VALUE: cut to 15 so toLong() can never overflow.
            "98765432109876543210" to "987654321098765",
        ).forEach { (raw, clean) ->
            assertEquals(raw, clean, sanitizeRupiah(raw))
            assertTrue(raw, clean.isEmpty() || clean.toLongOrNull() != null)
        }
    }
}
