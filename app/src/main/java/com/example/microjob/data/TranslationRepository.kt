package com.example.microjob.data

import com.example.microjob.model.TranslationLanguage

/** Online translation data source. */
interface TranslationRepository {
    suspend fun translate(
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ): String
}
