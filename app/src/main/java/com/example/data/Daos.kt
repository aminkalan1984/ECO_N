package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillRevisionDao {
    @Query("SELECT * FROM bill_revisions ORDER BY version ASC")
    fun observeAll(): Flow<List<BillRevisionEntity>>

    @Query("SELECT * FROM bill_revisions ORDER BY version DESC LIMIT 1")
    suspend fun getLatest(): BillRevisionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BillRevisionEntity): Long
}

@Dao
interface ProfileDraftDao {
    @Query("SELECT * FROM profile_drafts WHERE phone = :phone LIMIT 1")
    fun observeByPhone(phone: String): Flow<ProfileDraftEntity?>

    @Query("SELECT * FROM profile_drafts WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): ProfileDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileDraftEntity)
}

@Dao
interface ConsentDao {
    @Query("SELECT * FROM data_consents ORDER BY grantedAt DESC")
    fun observeAll(): Flow<List<ConsentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConsentEntity)

    @Query("UPDATE data_consents SET revokedAt = :revokedAt WHERE id = :id")
    suspend fun revoke(id: String, revokedAt: Long)
}

@Dao
interface AuditEventDao {
    @Query("SELECT * FROM audit_events ORDER BY at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AuditEventEntity)
}

@Dao
interface AssistantMessageDao {
    @Query("SELECT * FROM assistant_messages ORDER BY at ASC")
    fun observeAll(): Flow<List<AssistantMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssistantMessageEntity)

    @Query("DELETE FROM assistant_messages")
    suspend fun clearAll()
}

@Dao
interface PlantReportDao {
    @Query("SELECT * FROM plant_reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlantReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlantReportEntity)
}
