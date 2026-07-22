package com.oreoexperience.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Discurso::class, RegistroCampo::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discursoDao(): DiscursoDao
    abstract fun registroCampoDao(): RegistroCampoDao

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

        /**
         * v4: agrega categoría a `discursos` y crea la tabla
         * `registros_campo` para el módulo de Servicio del Campo.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE discursos ADD COLUMN category TEXT NOT NULL DEFAULT 'discurso'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `registros_campo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `hours` REAL NOT NULL DEFAULT 0,
                        `revisits` INTEGER NOT NULL DEFAULT 0,
                        `publications` INTEGER NOT NULL DEFAULT 0,
                        `videos` INTEGER NOT NULL DEFAULT 0,
                        `studies` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_registros_campo_dateMillis` ON `registros_campo` (`dateMillis`)"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
