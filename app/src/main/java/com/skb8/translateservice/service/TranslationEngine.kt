package com.skb8.translateservice.service

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Thin coroutine wrapper around ML Kit's on-device Translation and Language
 * Identification APIs.
 */
class TranslationEngine {

    private val languageIdentifier = LanguageIdentification.getClient()
    private val modelManager = RemoteModelManager.getInstance()
    private val translators = mutableMapOf<Pair<String, String>, Translator>()

    /** Returns a BCP-47 language code, or null if the language could not be identified. */
    suspend fun identifyLanguage(text: String): String? {
        val tag = languageIdentifier.identifyLanguage(text).await()
        return if (tag == "und") null else tag
    }

    /** Normalizes an arbitrary BCP-47 tag to an ML Kit [TranslateLanguage] code, or null if unsupported. */
    fun toMlKitLanguage(bcp47: String): String? = TranslateLanguage.fromLanguageTag(bcp47)

    suspend fun isModelDownloaded(mlKitLanguage: String): Boolean {
        val model = TranslateRemoteModel.Builder(mlKitLanguage).build()
        return modelManager.isModelDownloaded(model).await()
    }

    /** Returns the BCP-47 language codes of all translation models currently downloaded on-device. */
    suspend fun getDownloadedLanguages(): List<String> {
        val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        return models.map { it.language }.sorted()
    }

    suspend fun downloadModel(mlKitLanguage: String, allowMobileData: Boolean) {
        val model = TranslateRemoteModel.Builder(mlKitLanguage).build()
        val conditionsBuilder = DownloadConditions.Builder()
        if (!allowMobileData) conditionsBuilder.requireWifi()
        modelManager.download(model, conditionsBuilder.build()).await()
    }

    suspend fun translate(text: String, sourceMlKitLanguage: String, targetMlKitLanguage: String): String {
        val key = sourceMlKitLanguage to targetMlKitLanguage
        val translator = translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceMlKitLanguage)
                .setTargetLanguage(targetMlKitLanguage)
                .build()
            Translation.getClient(options)
        }
        return translator.translate(text).await()
    }

    fun close() {
        languageIdentifier.close()
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
