package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_revisions")
data class BillRevisionEntity(
    @PrimaryKey(autoGenerate = true) val version: Int = 0,
    val phone: String,
    val period: String,
    val peakKwh: Int,
    val midKwh: Int,
    val lowKwh: Int,
    val amountRial: Long,
    val source: String, // "ai", "offline_ocr", "manual"
    val confidence: String, // "high", "low"
    val issuesText: String,
    val confirmed: Boolean,
    val createdAt: Long
)

@Entity(tableName = "profile_drafts")
data class ProfileDraftEntity(
    @PrimaryKey val phone: String,
    val step: Int,
    val sectorRaw: String?,
    val city: String,
    val meterSerial: String,
    val tariffRaw: String,
    val demandKw: Double,
    val bidirectional: Boolean,
    val appliancesJson: String,
    val roofArea: Double,
    val roofTilt: Int,
    val roofAzimuth: String,
    val roofShadowPct: Int,
    val sharedOwnership: Boolean,
    val ownersTotal: Int,
    val ownersGranted: Int,
    val updatedAt: Long,
    val completedAt: Long?
)

@Entity(tableName = "data_consents")
data class ConsentEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val partner: String,
    val scope: String,
    val documentVersion: String,
    val grantedAt: Long,
    val revokedAt: Long? = null
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val action: String,
    val at: Long
)

@Entity(tableName = "assistant_messages")
data class AssistantMessageEntity(
    @PrimaryKey val id: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val at: Long
)

@Entity(tableName = "plant_reports")
data class PlantReportEntity(
    @PrimaryKey val id: String,
    val monthKey: String,
    val text: String,
    val createdAt: Long
)
