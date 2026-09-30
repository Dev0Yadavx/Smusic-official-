package com.example

import android.app.Application
import com.example.data.local.SMusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SMusicApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize database asynchronously off the main thread
        appScope.launch {
            SMusicDatabase.getInstance(this@SMusicApplication)
        }
    }
}
