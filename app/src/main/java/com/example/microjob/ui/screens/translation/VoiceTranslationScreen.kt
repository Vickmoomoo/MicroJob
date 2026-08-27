package com.example.microjob.ui.screens.translation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.microjob.model.TranslationLanguage
import com.example.microjob.viewmodel.TranslationUiState
import com.example.microjob.viewmodel.TranslationViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private enum class TranslationMode {
    TEXT,
    VOICE,
}

private val OriginalBlue = Color(0xFFE3F2FD)
private val OriginalBlueText = Color(0xFF0D47A1)
private val TranslatedGreen = Color(0xFFE8F5E9)
private val TranslatedGreenText = Color(0xFF1B5E20)

private const val PREFS_NAME = "voice_translation_prefs"
private const val KEY_SOURCE_LANG = "source_lang"
private const val KEY_TARGET_LANG = "target_lang"
private const val KEY_HISTORY = "history_json"

private fun savePrefs(
    context: Context,
    source: TranslationLanguage,
    target: TranslationLanguage,
    history: List<TranslationRecord>,
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val historyArray = JSONArray()
    history.forEach { record ->
        historyArray.put(
            JSONObject()
                .put("original", record.original)
                .put("translated", record.translated)
        )
    }
    prefs.edit()
        .putString(KEY_SOURCE_LANG, source.name)
        .putString(KEY_TARGET_LANG, target.name)
        .putString(KEY_HISTORY, historyArray.toString())
        .apply()
}

private fun loadSourceLang(context: Context): TranslationLanguage {
    val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SOURCE_LANG, TranslationLanguage.ENGLISH.name)
    return try { TranslationLanguage.valueOf(name!!) } catch (_: Exception) { TranslationLanguage.ENGLISH }
}

private fun loadTargetLang(context: Context): TranslationLanguage {
    val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_TARGET_LANG, TranslationLanguage.MALAY.name)
    return try { TranslationLanguage.valueOf(name!!) } catch (_: Exception) { TranslationLanguage.MALAY }
}

private fun loadHistory(context: Context): List<TranslationRecord> {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_HISTORY, "[]") ?: "[]"
    val array = JSONArray(json)
    val result = mutableListOf<TranslationRecord>()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        result.add(TranslationRecord(obj.getString("original"), obj.getString("translated")))
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTranslationScreen(
    onBack: () -> Unit,
    vm: TranslationViewModel = viewModel(),
) {
    val context = LocalContext.current
    val sourceLanguage by vm.sourceLanguage.collectAsStateWithLifecycle()
    val targetLanguage by vm.targetLanguage.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(TranslationMode.VOICE) }
    var textInput by remember { mutableStateOf("") }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsWarning by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(loadHistory(context)) }
    var isListening by remember { mutableStateOf(false) }

    // Restore saved languages on first load
    LaunchedEffect(Unit) {
        vm.setSourceLanguage(loadSourceLang(context))
        vm.setTargetLanguage(loadTargetLang(context))
    }

    val textToSpeech = remember(context) {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(textToSpeech, speechRecognizer) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            speechRecognizer?.destroy()
        }
    }

    // Save prefs whenever language or history changes
    LaunchedEffect(sourceLanguage, targetLanguage, history) {
        savePrefs(context, sourceLanguage, targetLanguage, history)
    }

    LaunchedEffect(uiState) {
        val success = uiState as? TranslationUiState.Success ?: return@LaunchedEffect
        if (history.none { it.original == success.original && it.translated == success.translated }) {
            history = history + TranslationRecord(success.original, success.translated)
        }
    }

    LaunchedEffect(uiState, ttsReady, targetLanguage) {
        val success = uiState as? TranslationUiState.Success ?: return@LaunchedEffect
        if (!ttsReady) return@LaunchedEffect

        val result = textToSpeech.setLanguage(Locale.forLanguageTag(targetLanguage.localeTag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            ttsWarning = "The ${targetLanguage.label} voice is not installed on this device."
        } else {
            ttsWarning = null
            textToSpeech.speak(
                success.translated,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "microjob-translation"
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) vm.showError("Microphone permission is required for voice mode.")
    }

    fun toggleListening() {
        if (isListening) {
            // Stop listening
            speechRecognizer?.stopListening()
            isListening = false
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val recognizer = speechRecognizer
        if (recognizer == null) {
            vm.showError("Speech recognition is not available on this device.")
            return
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

            override fun onEndOfSpeech() {
                // Don't auto-stop — let the user control when to stop.
                // If the user hasn't manually stopped, keep listening.
                if (!isListening) {
                    // User already clicked stop, so this is expected.
                    // onResults will fire next.
                }
                // If isListening is still true, the speech engine stopped on its own
                // (silence detected). We still wait for onResults.
            }

            override fun onError(error: Int) {
                isListening = false
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    // No speech detected — don't show error, just reset silently.
                    return
                }
                vm.showError(
                    when (error) {
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Speech recognition needs an Internet connection."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Microphone permission is required."
                        else -> "Could not recognize speech. Try again."
                    }
                )
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    vm.translate(text)
                }
            }
        })

        recognizer.cancel()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguage.localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sourceLanguage.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        isListening = true
        vm.beginListening()
        recognizer.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),title = { Text("Voice Translation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // ---- Scrollable messages area ----
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(history.size) {
                if (history.isNotEmpty()) {
                    listState.animateScrollToItem(history.size * 2 - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) {
                item {
                    Text(
                        text = "Speak across languages",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Choose two languages, then speak or type your message.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(history.size) { index ->
                    val record = history[index]
                    MessageBubble(
                        label = "Original",
                        text = record.original,
                        background = OriginalBlue,
                        contentColor = OriginalBlueText,
                        alignment = Alignment.End
                    )
                    Spacer(Modifier.height(6.dp))
                    MessageBubble(
                        label = "Translated",
                        text = record.translated,
                        background = TranslatedGreen,
                        contentColor = TranslatedGreenText,
                        alignment = Alignment.Start
                    )
                }

                if (uiState is TranslationUiState.Error) {
                    item {
                        Text(
                            text = (uiState as TranslationUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (ttsWarning != null) {
                    item {
                        Text(ttsWarning!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ---- Language selector row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageDropdown(
                    label = "Original",
                    current = sourceLanguage,
                    modifier = Modifier.weight(1f),
                    onSelect = vm::setSourceLanguage
                )
                IconButton(onClick = vm::swapLanguages) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap languages")
                }
                LanguageDropdown(
                    label = "Translate to",
                    current = targetLanguage,
                    modifier = Modifier.weight(1f),
                    onSelect = vm::setTargetLanguage
                )
            }

            HorizontalDivider()

            // ---- Bottom input bar (WeChat style) ----
            if (mode == TranslationMode.TEXT) {
                TextModeBar(
                    text = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            vm.translate(textInput)
                            textInput = ""
                        }
                    },
                    onSwitchMode = { mode = TranslationMode.VOICE },
                    isTranslating = uiState is TranslationUiState.Translating
                )
            } else {
                VoiceModeBar(
                    isListening = isListening,
                    isTranslating = uiState is TranslationUiState.Translating,
                    onSwitchMode = { mode = TranslationMode.TEXT },
                    onToggle = ::toggleListening
                )
            }
        }
    }
}

// ---------- WeChat style input bars ----------

@Composable
private fun TextModeBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onSwitchMode: () -> Unit,
    isTranslating: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Switch mode button (circle, shows mic to switch to voice)
        IconButton(
            onClick = onSwitchMode,
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.Bottom)
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Switch to voice",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Text field (rounded rectangle, fills space)
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = RoundedCornerShape(22.dp),
            maxLines = 4
        )

        // Send button (circle)
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isTranslating,
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.Bottom)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank() && !isTranslating) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}

@Composable
private fun VoiceModeBar(
    isListening: Boolean,
    isTranslating: Boolean,
    onSwitchMode: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Switch mode button (circle, shows keyboard to switch to text)
        IconButton(
            onClick = onSwitchMode,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Filled.Keyboard,
                contentDescription = "Switch to text",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Press and speak button (rounded rectangle, fills remaining space)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = if (isListening) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isListening) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onToggle
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isListening -> "Listening..."
                            isTranslating -> "Translating..."
                            else -> "Press and speak"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---------- Messages ----------

private data class TranslationRecord(
    val original: String,
    val translated: String,
)

@Composable
private fun MessageBubble(
    label: String,
    text: String,
    background: Color,
    contentColor: Color,
    alignment: Alignment.Horizontal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignment == Alignment.End) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(18.dp),
            color = background
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium)
                Text(text, color = contentColor, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ---------- Language dropdown ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    current: TranslationLanguage,
    modifier: Modifier,
    onSelect: (TranslationLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = current.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.Keyboard else Icons.Filled.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TranslationLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.label) },
                    onClick = {
                        onSelect(language)
                        expanded = false
                    }
                )
            }
        }
    }
}
