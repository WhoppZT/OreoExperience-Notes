package com.oreoexperience.notes

import android.app.Application
import com.oreoexperience.notes.data.AppContainer
import com.oreoexperience.notes.data.AppContainerImpl
import com.oreoexperience.notes.data.UserPreferences

class OreoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
        UserPreferences.init(this)
    }
}
