package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.LibreTranslateRepository
import com.example.microjob.data.TranslationRepository
import com.example.microjob.model.TranslationLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data object Listening : TranslationUiState
    data object Translating : TranslationUiState
    data class Success(val original: String, val translated: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

class TranslationViewModel(
    application: Application,
    private val repository: TranslationRepository = LibreTranslateRepository(),
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, LibreTranslateRepository())

    val sourceLanguage = MutableStateFlow(TranslationLanguage.ENGLISH)
    val targetLanguage = MutableStateFlow(TranslationLanguage.MALAY)

    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    fun setSourceLanguage(language: TranslationLanguage) {
        if (language == targetLanguage.value) {
            targetLanguage.value = sourceLanguage.value
        }
        sourceLanguage.value = language
        _uiState.value = TranslationUiState.Idle
    }

    fun setTargetLanguage(language: TranslationLanguage) {
        if (language == sourceLanguage.value) {
            sourceLanguage.value = targetLanguage.value
        }
        targetLanguage.value = language
        _uiState.value = TranslationUiState.Idle
    }

    fun swapLanguages() {
        val source = sourceLanguage.value
        sourceLanguage.value = targetLanguage.value
        targetLanguage.value = source
        _uiState.value = TranslationUiState.Idle
    }

    fun beginListening() {
        _uiState.value = TranslationUiState.Listening
    }

    fun translate(text: String) {
        if (text.isBlank()) {
            _uiState.value = TranslationUiState.Error("No speech was detected.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslationUiState.Translating
            try {
                val translated = repository.translate(
                    text = text,
                    source = sourceLanguage.value,
                    target = targetLanguage.value
                )
                _uiState.value = TranslationUiState.Success(text, translated)
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error(
                    e.message ?: "Translation failed. Check your Internet connection."
                )
            }
        }
    }

    fun showError(message: String) {
        _uiState.value = TranslationUiState.Error(message)
    }
}
