package dev.flare.speedtest.data.repo

import dev.flare.speedtest.data.db.TestResultDao
import dev.flare.speedtest.data.db.newEntity
import dev.flare.speedtest.data.db.toRecord
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SpeedTestRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHistoryRepository(private val dao: TestResultDao) : HistoryRepository {

    override fun observeAll(): Flow<List<SpeedTestRecord>> =
        dao.observeRecent(MAX_ENTRIES).map { rows -> rows.map { it.toRecord() } }

    override suspend fun save(networkType: NetworkType, measurement: Measurement, timestampMillis: Long) {
        dao.insert(newEntity(networkType, measurement, timestampMillis))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun restore(record: SpeedTestRecord) {
        dao.insert(newEntity(record.networkType, record.measurement, record.timestampMillis).copy(id = record.id))
    }

    override suspend fun clear() = dao.deleteAll()

    private companion object {
        const val MAX_ENTRIES = 500
    }
}
