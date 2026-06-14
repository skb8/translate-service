package com.skb8.translateservice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TranslationStatus {
    OK,
    PENDING_MODEL,
    ERROR
}

@Entity(tableName = "translation_log")
data class TranslationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val callerPackage: String,
    val callerLabel: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceText: String,
    val translatedText: String,
    val status: TranslationStatus,
    val errorMessage: String? = null
)
