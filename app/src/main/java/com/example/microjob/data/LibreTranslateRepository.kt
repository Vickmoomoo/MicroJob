package com.example.microjob.data

import com.example.microjob.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Small online-only LibreTranslate client. Keep the endpoint in one place so
 * it can be replaced with another public instance or a school backend later.
 */
class LibreTranslateRepository : TranslationRepository {

    override suspend fun translate(
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ): String = withContext(Dispatchers.IO) {
        try {
            translateWithLibreTranslate(text, source, target)
        } catch (_: Exception) {
            // Public LibreTranslate instances can be unavailable or require a
            // key. Keep the demo usable with a second public endpoint.
            translateWithMyMemory(text, source, target)
        }
    }

    private fun translateWithLibreTranslate(
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ): String {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            val request = JSONObject()
                .put("q", text)
                .put("source", source.code)
                .put("target", target.code)
                .put("format", "text")

            connection.outputStream.use { output ->
                output.write(request.toString().toByteArray(Charsets.UTF_8))
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Translation service unavailable. Try again.")
            }

            val raw = JSONObject(responseText).optString("translatedText").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The translation service returned no text.")
            return cleanTranslation(raw)
        } finally {
            connection.disconnect()
        }
    }

    private fun translateWithMyMemory(
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
    ): String {
        val query = URLEncoder.encode(text, Charsets.UTF_8.name())
        val languagePair = "${source.myMemoryCode}|${target.myMemoryCode}"
        val url = "$MY_MEMORY_ENDPOINT?q=$query&langpair=$languagePair"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Translation service unavailable. Try again.")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val translated = JSONObject(response)
                .optJSONObject("responseData")
                ?.optString("translatedText")
                .orEmpty()
            if (translated.isBlank()) {
                throw IllegalStateException("The translation service returned no text.")
            }
            return cleanTranslation(translated)
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanTranslation(raw: String): String {
        // MyMemory often wraps segments in <x id="1"/> or <x>…</x> placeholders.
        // Strip any XML-like tags and decode common entities, then trim.
        var cleaned = raw.replace(Regex("<[^>]+>"), "")
        cleaned = cleaned
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        return cleaned.trim()
    }

    private companion object {
        const val ENDPOINT = "https://translate.argosopentech.com/translate"
        const val MY_MEMORY_ENDPOINT = "https://api.mymemory.translated.net/get"
    }
}
