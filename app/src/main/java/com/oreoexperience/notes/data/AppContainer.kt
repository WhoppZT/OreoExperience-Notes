package com.oreoexperience.notes.data

import android.content.Context

/**
 * Manual DI muy ligera. Evitamos Hilt/Koin para no inflar el build de
 * un proyecto pequeño; un container con las dependencias compartidas
 * alcanza y se inyecta vía CompositionLocal en la UI.
 */
interface AppContainer {
    val repository: DiscursoRepository
    val backupManager: BackupManager
    val appContext: Context
}

class AppContainerImpl(context: Context) : AppContainer {
    override val appContext: Context = context.applicationContext
    private val db: AppDatabase = AppDatabase.build(appContext)
    override val repository: DiscursoRepository = DiscursoRepository(db.discursoDao())
    override val backupManager: BackupManager = BackupManager(appContext, repository, db.discursoDao())
}
