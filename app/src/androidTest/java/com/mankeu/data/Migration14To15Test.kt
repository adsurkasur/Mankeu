package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration14To15Test {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun oldDatabaseKeepsItsRowsAndGainsTradePurchaseTable() {
        helper.createDatabase("trade-migration", 14).use { db ->
            db.execSQL("INSERT INTO businesses (id,name,currency,archived,createdAtEpochMillis,updatedAtEpochMillis,profitBasis) VALUES ('b','Usaha','IDR',0,1,1,'SIMPLE')")
        }
        helper.runMigrationsAndValidate("trade-migration", 15, true, Migration14To15).use { db ->
            db.query("SELECT COUNT(*) FROM businesses WHERE id='b'").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM trade_purchases").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        }
    }
}
