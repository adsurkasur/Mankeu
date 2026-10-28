package com.mankeu.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.sync.SyncEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** An install from schema 1-11 is wiped by the destructive fallback (DEC-026) and must still get its sync triggers. */
@RunWith(AndroidJUnit4::class)
class DestructiveFallbackTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "destructive-fallback-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun upgradeFromSchema11RecreatesSyncTriggersAndCapture() = runBlocking {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use {
            it.execSQL("CREATE TABLE old_table (id INTEGER PRIMARY KEY)")
            it.version = 11
        }

        val database = AppDatabase.create(context, name)
        try {
            val db = database.openHelper.writableDatabase
            val triggers = db.query("SELECT name FROM sqlite_master WHERE type = 'trigger' AND substr(name, 1, 5) = 'sync_'").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
            assertTrue("sync triggers missing after destructive fallback", triggers.size >= AppDatabase.DATA_TABLES.size * 3)

            SyncEngine(database).enable()
            FinanceRepository(database).createBusiness("Kedai")
            val queued = db.query("SELECT COUNT(*) FROM sync_outbox").use { c -> c.moveToFirst(); c.getInt(0) }
            assertTrue("no outbox row for a write after upgrade", queued > 0)
            val deviceId = db.query("SELECT COUNT(*) FROM sync_meta WHERE `key` = 'device_id'").use { c -> c.moveToFirst(); c.getInt(0) }
            assertEquals(1, deviceId)
        } finally {
            database.close()
        }
    }
}
