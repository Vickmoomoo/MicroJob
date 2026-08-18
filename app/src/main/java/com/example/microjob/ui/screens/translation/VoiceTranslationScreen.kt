package com.example.microjob.ui.screens.translation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.util.Locale

private enum class TranslationMode {
    TEXT,
    VOICE,
}

private val OriginalBlue = Color(0xFFE3F2FD)
private val OriginalBlueText = Color(0xFF0D47A1)
private val TranslatedGreen = Color(0xFFE8F5E9)
private val TranslatedGreenText = Color(0xFF1B5E20)

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

    LaunchedEffect(uiState, ttsReady, targetLanguage, mode) {
        val success = uiState as? TranslationUiState.Success ?: return@LaunchedEffect
        if (!ttsReady || mode != TranslationMode.VOICE) return@LaunchedEffect

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

    fun startListening() {
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
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
            override fun onError(error: Int) {
                vm.showError(
                    when (error) {
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Speech recognition needs an Internet connection."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was detected. Try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Microphone permission is required."
                        else -> "Could not recognize speech. Try again."
                    }
                )
            }

            override fun onResults(results: android.os.Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                vm.translate(text.orEmpty())
            }
        })

        recognizer.cancel()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguage.localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sourceLanguage.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        vm.beginListening()
        recognizer.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Translation") },
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Speak across languages",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose two languages, then speak or type your message.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
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

            TranslationMessages(uiState)

            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ModeIconButton(
                            selected = mode == TranslationMode.TEXT,
                            contentDescription = "Text mode",
                            onClick = { mode = TranslationMode.TEXT }
                        ) {
                            Icon(Icons.Filled.Keyboard, contentDescription = null)
                        }
                        ModeIconButton(
                            selected = mode == TranslationMode.VOICE,
                            contentDescription = "Voice mode",
                            onClick = { mode = TranslationMode.VOICE }
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                        }
                    }

                    if (mode == TranslationMode.TEXT) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") },
                                maxLines = 4
                            )
                            ModeIconButton(
                                selected = false,
                                contentDescription = "Translate typed message",
                                enabled = textInput.isNotBlank() &&
                                    uiState !is TranslationUiState.Translating,
                                onClick = { vm.translate(textInput) }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            }
                        }
                    } else {
                        ModeIconButton(
                            selected = uiState is TranslationUiState.Listening,
                            contentDescription = "Speak now",
                            enabled = uiState !is TranslationUiState.Translating,
                            onClick = ::startListening,
                            large = true
                        ) {
                            Icon(
                                imageVector = if (uiState is TranslationUiState.Listening) {
                                    Icons.Filled.Stop
                                } else {
                                    Icons.Filled.Mic
                                },
                                contentDescription = null
                            )
                        }
                        Text(
                            text = when (uiState) {
                                TranslationUiState.Listening -> "Listening..."
                                TranslationUiState.Translating -> "Translating..."
                                else -> "Tap the microphone to speak"
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState is TranslationUiState.Error) {
                Text(
                    text = (uiState as TranslationUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            ttsWarning?.let { warning ->
                Text(warning, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TranslationMessages(uiState: TranslationUiState) {
    val success = uiState as? TranslationUiState.Success ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MessageBubble(
            label = "Original",
            text = success.original,
            background = OriginalBlue,
            contentColor = OriginalBlueText,
            alignment = Alignment.End
        )
        MessageBubble(
            label = "Translated",
            text = success.translated,
            background = TranslatedGreen,
            contentColor = TranslatedGreenText,
            alignment = Alignment.Start
        )
    }
}

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
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
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

@Composable
private fun ModeIconButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    large: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = if (large) Modifier.padding(6.dp) else Modifier
        ) {
            content()
        }
    }
}
