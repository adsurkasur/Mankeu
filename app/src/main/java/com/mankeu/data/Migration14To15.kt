package com.mankeu.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.sync.SyncSchema

/** Keep trade purchase quantities attached to their cash events and products. */
object Migration14To15 : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `trade_purchases` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `quantityText` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`id`) REFERENCES `financial_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trade_purchases_productId` ON `trade_purchases` (`productId`)")
        SyncSchema.createTriggers(db)
        db.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES ('${SyncSchema.REQUIRES_FULL_SNAPSHOT}', '1')")
    }
}
