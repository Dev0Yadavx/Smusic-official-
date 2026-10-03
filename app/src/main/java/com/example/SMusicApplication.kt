package com.example

import android.app.Application
import com.example.data.local.SMusicDatabase
import com.example.player.SMusicNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SMusicApplication : Application() {

    companion object {
        @Volatile
        var hasShownSplashThisSession: Boolean = false
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Create system notification channels immediately
        SMusicNotificationHelper.ensureChannelsCreated(this)

        // Pre-initialize database asynchronously off the main thread
        appScope.launch {
            SMusicDatabase.getInstance(this@SMusicApplication)
        }
    }
}
