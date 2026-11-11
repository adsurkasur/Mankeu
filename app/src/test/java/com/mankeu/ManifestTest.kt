package com.mankeu

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Drive backup and sync need INTERNET. It used to arrive through the Firebase libraries and vanished with
 * them in phase 0; no test noticed because Drive is faked in tests (audit 2026-11-12, T0).
 */
class ManifestTest {
    @Test
    fun `manifest declares INTERNET`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "AndroidManifest.xml must declare android.permission.INTERNET itself",
            Regex("""<uses-permission\s+android:name="android\.permission\.INTERNET"\s*/>""").containsMatchIn(manifest),
        )
    }
}
