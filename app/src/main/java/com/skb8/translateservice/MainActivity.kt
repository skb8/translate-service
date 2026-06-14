package com.skb8.translateservice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.skb8.translateservice.service.TranslationBinderService
import com.skb8.translateservice.ui.LogScreen
import com.skb8.translateservice.ui.LogViewModel
import com.skb8.translateservice.ui.theme.TranslateServiceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the translation Binder service alive in the background so other apps
        // can bind to it even when this activity is not visible.
        val serviceIntent = Intent(this, TranslationBinderService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        setContent {
            TranslateServiceTheme {
                val logs by viewModel.logs.collectAsState()
                val defaultTarget by viewModel.defaultTargetLanguage.collectAsState()

                LogScreen(
                    logs = logs,
                    availableLanguages = viewModel.availableLanguages,
                    defaultTargetLanguage = defaultTarget,
                    onTargetLanguageSelected = viewModel::setDefaultTargetLanguage
                )
            }
        }
    }
}
