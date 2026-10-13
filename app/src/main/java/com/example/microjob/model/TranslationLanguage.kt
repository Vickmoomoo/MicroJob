package com.example.microjob.model

enum class TranslationLanguage(
    val label: String,
    val code: String,
    val localeTag: String,
    val myMemoryCode: String,
) {
    MALAY("Malay", "ms", "ms-MY", "ms"),
    CHINESE("Chinese", "zh", "zh-CN", "zh-CN"),
    ENGLISH("English", "en", "en-US", "en"),
}
