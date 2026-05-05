package com.oreoexperience.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Discurso::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discursoDao(): DiscursoDao

    companion object {
        private const val DB_NAME = "oreo_notes.db"

        /**
         * v2: agrega la columna [Discurso.targetDurationSec] (duración objetivo
         * del discurso en segundos). Se rellena con 0 para todos los registros
         * existentes — equivale a "sin objetivo".
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE discursos ADD COLUMN targetDurationSec INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
