package com.mankeu.presentation

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.mankeu.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndonesianOnlyTest {
    @Test fun savedEnglishDoesNotChangeTheCurrentAppLanguage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val old = prefs.getString("language", null)
        try {
            prefs.edit().putString("language", "en").commit()
            assertEquals(java.util.Locale.forLanguageTag("id").language, context.withAppLanguage().resources.configuration.locales[0].language)
            assertEquals("Kembali", context.withAppLanguage().getString(R.string.action_back))
            assertEquals("en", prefs.getString("language", null))
        } finally {
            prefs.edit().apply { if (old == null) remove("language") else putString("language", old) }.commit()
        }
    }
}
