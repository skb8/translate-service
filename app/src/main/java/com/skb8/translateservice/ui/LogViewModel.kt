package com.skb8.translateservice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.skb8.translateservice.data.AppDatabase
import com.skb8.translateservice.data.SettingsRepository
import com.skb8.translateservice.data.TranslationLog
import com.skb8.translateservice.service.TranslationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class LanguageOption(val code: String, val displayName: String)

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val logDao = AppDatabase.get(application).logDao()

    val logs: StateFlow<List<TranslationLog>> = logDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultTargetLanguage: StateFlow<String> = settings.defaultTargetLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TranslateLanguage.RUSSIAN)

    val availableLanguages: List<LanguageOption> = TranslateLanguage.getAllLanguages()
        .map { code -> LanguageOption(code, Locale(code).displayName.replaceFirstChar { it.uppercase() }) }
        .sortedBy { it.displayName }

    private val engine = TranslationEngine()

    private val _installedModels = MutableStateFlow<List<String>>(emptyList())
    val installedModels: StateFlow<List<String>> = _installedModels.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    init {
        refreshInstalledModels()
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun refreshInstalledModels() {
        viewModelScope.launch {
            _installedModels.value = engine.getDownloadedLanguages()
        }
    }

    fun setDefaultTargetLanguage(code: String) {
        viewModelScope.launch {
            settings.setDefaultTargetLanguage(code)
        }
    }

    override fun onCleared() {
        engine.close()
        super.onCleared()
    }
}
