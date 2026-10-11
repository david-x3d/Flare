package dev.flare.speedtest.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResultEntity::class], version = 1, exportSchema = true)
abstract class FlareDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao

    companion object {
        fun create(context: Context): FlareDatabase =
            Room.databaseBuilder(context.applicationContext, FlareDatabase::class.java, "flare.db").build()
    }
}
