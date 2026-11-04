package dev.flare.speedtest.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TestResultEntity::class], version = 3, exportSchema = true)
abstract class FlareDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_results ADD COLUMN samples_json TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_results ADD COLUMN loaded_down_ms REAL")
                db.execSQL("ALTER TABLE test_results ADD COLUMN loaded_up_ms REAL")
                db.execSQL("ALTER TABLE test_results ADD COLUMN data_bytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE test_results ADD COLUMN isp TEXT")
                db.execSQL("ALTER TABLE test_results ADD COLUMN server_code TEXT")
                db.execSQL("ALTER TABLE test_results ADD COLUMN server_city TEXT")
            }
        }

        fun create(context: Context): FlareDatabase =
            Room.databaseBuilder(context.applicationContext, FlareDatabase::class.java, "flare.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
