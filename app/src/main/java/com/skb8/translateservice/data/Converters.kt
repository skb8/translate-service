package com.skb8.translateservice.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun statusToString(status: TranslationStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): TranslationStatus = TranslationStatus.valueOf(value)
}
