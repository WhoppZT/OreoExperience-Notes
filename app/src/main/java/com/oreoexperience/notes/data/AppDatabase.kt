package com.oreoexperience.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Discurso::class],
    version = 3,
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

        /**
         * v3: agrega [Discurso.pinned] (boolean) y [Discurso.deletedAt]
         * (timestamp nullable) para soportar pin de notas y la papelera de
         * "Eliminadas recientemente".
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE discursos ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE discursos ADD COLUMN deletedAt INTEGER"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
