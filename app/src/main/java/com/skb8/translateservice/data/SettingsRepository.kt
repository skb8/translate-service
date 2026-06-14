package com.skb8.translateservice.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val defaultTargetLanguageKey = stringPreferencesKey("default_target_language")
    private val mobileDataApprovedLanguagesKey = stringSetPreferencesKey("mobile_data_approved_languages")

    val defaultTargetLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[defaultTargetLanguageKey] ?: TranslateLanguage.RUSSIAN
    }

    suspend fun setDefaultTargetLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[defaultTargetLanguageKey] = languageCode
        }
    }

    suspend fun isMobileDataApproved(languageCode: String): Boolean {
        val approved = context.dataStore.data.map { it[mobileDataApprovedLanguagesKey] ?: emptySet() }.first()
        return languageCode in approved
    }

    suspend fun approveMobileDataDownload(languageCode: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[mobileDataApprovedLanguagesKey] ?: emptySet()
            prefs[mobileDataApprovedLanguagesKey] = current + languageCode
        }
    }
}
