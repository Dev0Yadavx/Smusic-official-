package com.example.ui.common

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppToastData(
    val message: String,
    val isDownload: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object AppToastManager {
    private val _currentToast = MutableStateFlow<AppToastData?>(null)
    val currentToast: StateFlow<AppToastData?> = _currentToast.asStateFlow()

    fun show(message: String, isDownload: Boolean = message.lowercase().contains("download")) {
        if (message.isBlank()) return
        _currentToast.value = AppToastData(message, isDownload)
    }

    fun dismiss() {
        _currentToast.value = null
    }
}

object AppToast {
    fun show(
        context: Context? = null,
        message: String,
        isDownload: Boolean = message.lowercase().contains("download"),
        duration: Int = Toast.LENGTH_SHORT
    ) {
        if (message.isNotBlank()) {
            AppToastManager.show(message, isDownload)
        }
    }
}
