package com.example

import android.app.Application
import com.example.data.billing.BillingManager
import com.example.data.db.SalimDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.PhotoRepository
import com.example.data.repository.ProjectRepository
import com.example.util.LocationHelper

class SalimApplication : Application() {

    lateinit var database: SalimDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var photoRepository: PhotoRepository
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var locationHelper: LocationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = SalimDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        projectRepository = ProjectRepository(database.projectDao())
        photoRepository = PhotoRepository(database.photoDao(), database.projectDao())
        billingManager = BillingManager(this)
        locationHelper = LocationHelper(this)

        // Initialize billing connection and entitlement check on launch
        billingManager.startConnection()
    }

    companion object {
        lateinit var instance: SalimApplication
            private set
    }
}
