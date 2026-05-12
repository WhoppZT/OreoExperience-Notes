package com.oreoexperience.notes.data

import android.content.Context

/**
 * Manual DI muy ligera. Evitamos Hilt/Koin para no inflar el build de
 * un proyecto pequeño; un container con las dependencias compartidas
 * alcanza y se inyecta vía CompositionLocal en la UI.
 */
interface AppContainer {
    val repository: DiscursoRepository
    val registroCampoRepository: RegistroCampoRepository
    val backupManager: BackupManager
    val mediaStorage: MediaStorage
    val userPreferences: UserPreferences
    val licenseManager: LicenseManager
    val pdfExportManager: PdfExportManager
    val appContext: Context
}

class AppContainerImpl(context: Context) : AppContainer {
    override val appContext: Context = context.applicationContext
    private val db: AppDatabase = AppDatabase.build(appContext)
    override val repository: DiscursoRepository = DiscursoRepository(db.discursoDao())
    override val registroCampoRepository: RegistroCampoRepository =
        RegistroCampoRepository(db.registroCampoDao())
    override val backupManager: BackupManager =
        BackupManager(appContext, repository, registroCampoRepository, db.discursoDao(), db.registroCampoDao())
    override val mediaStorage: MediaStorage = MediaStorage(appContext)
    override val userPreferences: UserPreferences = UserPreferences(appContext)
    override val licenseManager: LicenseManager = LicenseManager(appContext)
    override val pdfExportManager: PdfExportManager = PdfExportManager(appContext)
}
