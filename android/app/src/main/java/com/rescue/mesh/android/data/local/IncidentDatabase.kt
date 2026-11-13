package com.rescue.mesh.android.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room Database lưu lịch sử gói tin SOS & Dispatch đã nhận.
 *
 * Mục đích:
 *   - Lưu trữ offline hoàn toàn mọi gói tin SOS đã phát đi (để retry khi có mạng).
 *   - Lưu trữ lệnh Dispatch đã nhận từ Base Station (evidence trail).
 *   - Phục vụ chức năng "Lịch sử SOS" trên UI và xuất báo cáo sau thảm họa.
 *
 * Kiến trúc:
 *   - Entity: IncidentEntity chứa đầy đủ thông tin gói tin.
 *   - DAO: IncidentDao với truy vấn Flow reactive.
 *   - Database: Singleton thread-safe.
 */

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val packetId: String,
    val packetType: String,
    val sourceNodeId: String,
    val destinationNodeId: String,
    val senderName: String = "",
    val alertType: String = "",
    val message: String = "",
    val severity: String = "",
    val victimCount: Int = 1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hopCount: Int = 0,
    val ttl: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val routeHistory: String = "",
    val isOutgoing: Boolean = true,
    val sendSuccess: Boolean = false
)

@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE packetType = :type ORDER BY timestamp DESC")
    fun getIncidentsByType(type: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isOutgoing = 1 ORDER BY timestamp DESC")
    fun getOutgoingSos(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isOutgoing = 0 ORDER BY timestamp DESC")
    fun getIncomingDispatches(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE sendSuccess = 0 AND isOutgoing = 1")
    suspend fun getFailedOutgoing(): List<IncidentEntity>

    @Query("UPDATE incidents SET sendSuccess = 1 WHERE packetId = :packetId")
    suspend fun markAsSent(packetId: String)

    @Query("SELECT COUNT(*) FROM incidents")
    fun getTotalCount(): Flow<Int>

    @Query("DELETE FROM incidents WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}

@Database(entities = [IncidentEntity::class], version = 1, exportSchema = false)
abstract class IncidentDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var INSTANCE: IncidentDatabase? = null

        fun getInstance(context: Context): IncidentDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IncidentDatabase::class.java,
                    "incident_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}