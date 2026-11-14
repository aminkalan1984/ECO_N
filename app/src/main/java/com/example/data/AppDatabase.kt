package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BillRevisionEntity::class,
        ProfileDraftEntity::class,
        ConsentEntity::class,
        AuditEventEntity::class,
        AssistantMessageEntity::class,
        PlantReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billRevisionDao(): BillRevisionDao
    abstract fun profileDraftDao(): ProfileDraftDao
    abstract fun consentDao(): ConsentDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun assistantMessageDao(): AssistantMessageDao
    abstract fun plantReportDao(): PlantReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ecosolar.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
