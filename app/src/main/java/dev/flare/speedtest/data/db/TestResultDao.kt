package dev.flare.speedtest.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    /** Newest first; the history screen only ever needs a bounded window. */
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TestResultEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestResultEntity): Long

    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM test_results")
    suspend fun deleteAll()
}
