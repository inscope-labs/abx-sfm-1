package com.inscopelabs.sfm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: String?,
    val eventType: String,
    val operation: String,
    val resource: String,
    val result: String,
    val userApprovalState: String?,
    val elapsedMs: Long,
    val detailsJson: String?,
    val method: String?,
    val signature: String?,
    val previousSignature: String?
)
