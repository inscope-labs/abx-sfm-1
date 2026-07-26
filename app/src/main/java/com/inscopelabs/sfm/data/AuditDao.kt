package com.inscopelabs.sfm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditEntryEntity)

    @Query("SELECT * FROM audit_log ORDER BY id ASC")
    suspend fun getAllEntries(): List<AuditEntryEntity>

    @Query("SELECT * FROM audit_log ORDER BY id DESC LIMIT :count")
    suspend fun getRecentEntries(count: Int): List<AuditEntryEntity>

    @Query("SELECT * FROM audit_log WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSessionEntries(sessionId: String): List<AuditEntryEntity>

    @Query("SELECT * FROM audit_log ORDER BY id DESC LIMIT 1")
    suspend fun getLastEntry(): AuditEntryEntity?

    @Query("DELETE FROM audit_log WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteEntriesOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM audit_log WHERE id NOT IN (SELECT id FROM audit_log ORDER BY id DESC LIMIT :maxEntries)")
    suspend fun pruneOldEntries(maxEntries: Int)
}
