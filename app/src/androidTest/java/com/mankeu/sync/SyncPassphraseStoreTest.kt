package com.mankeu.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The sync passphrase survives in Keystore-encrypted form and is gone after "Matikan" (DEC-024). */
@RunWith(AndroidJUnit4::class)
class SyncPassphraseStoreTest {
    private val store = SyncPassphraseStore(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = store.clear()

    @Test
    fun savedPassphraseLoadsBackAndClearRemovesIt() {
        store.save("kata-sandi-sync".toCharArray())

        assertTrue(store.exists())
        assertArrayEquals("kata-sandi-sync".toCharArray(), store.load())

        store.clear()
        assertFalse(store.exists())
        assertNull(store.load())
    }
}
