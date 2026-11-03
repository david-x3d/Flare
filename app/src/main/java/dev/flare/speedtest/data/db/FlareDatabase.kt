package dev.flare.speedtest.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResultEntity::class], version = 2, exportSchema = true)
abstract class FlareDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao

    companion object {
        fun create(context: Context): FlareDatabase =
            Room.databaseBuilder(context.applicationContext, FlareDatabase::class.java, "flare.db").addMigrations(object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE test_results ADD COLUMN samples_json TEXT NOT NULL DEFAULT '[]'")
                }
            }).build()
    }
}
