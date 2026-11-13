package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority

class Converters {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = runCatching { ActivityType.valueOf(value) }.getOrDefault(ActivityType.TRABALHO)

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIA)

    @TypeConverter
    fun fromActivityStatus(value: ActivityStatus): String = value.name

    @TypeConverter
    fun toActivityStatus(value: String): ActivityStatus = runCatching { ActivityStatus.valueOf(value) }.getOrDefault(ActivityStatus.PENDENTE)
}

@Database(
    entities = [
        Discipline::class,
        AcademicActivity::class,
        AcademicDocument::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun disciplineDao(): DisciplineDao
    abstract fun activityDao(): ActivityDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN originalDate TEXT")
                db.execSQL("ALTER TABLE activities ADD COLUMN grade REAL")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE academic_documents ADD COLUMN fileFingerprint TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "academic_agenda_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Version 2 intentionally starts empty, removing the old demo seed.
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
