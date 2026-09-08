package com.example.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.GeminiConstants
import com.example.ui.components.AudioPlaybackBar
import com.example.ui.components.FilterSelectableChip
import com.example.ui.components.ModelBadge
import com.example.ui.components.PulsingLiveOrb
import com.example.ui.theme.GeminiAccentAmber
import com.example.ui.theme.GeminiAccentCyan
import com.example.ui.theme.GeminiAccentEmerald
import com.example.ui.theme.GeminiAccentRose
import com.example.ui.theme.GeminiPrimary
import com.example.ui.theme.GeminiSecondary
import com.example.viewmodel.GeminiPlaceViewModel

@Composable
fun AudioStudioScreen(viewModel: GeminiPlaceViewModel, modifier: Modifier = Modifier) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isAudioGenerating.collectAsState()
    val latestCreation by viewModel.latestAudioCreation.collectAsState()
    val isPlaying by viewModel.audioHelper.isPlaying.collectAsState()
    val isRecording by viewModel.audioHelper.isRecording.collectAsState()
    val selectedVoice by viewModel.selectedTtsVoice.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startLiveMicRecording()
        }
    }
    val isLyriaPro by viewModel.isLyriaPro.collectAsState()
    val transcriptionResult by viewModel.transcriptionResult.collectAsState()
    val liveVoiceStatus by viewModel.liveVoiceStatus.collectAsState()
    val liveVoiceAiResponse by viewModel.liveVoiceAiResponse.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GeminiPrimary
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("TTS Voice", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Lyria Music", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Transcribe", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text("Live Voice", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSubTab) {
                0 -> {
                    // --- TAB 0: TTS (gemini-3.1-flash-tts-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_31_TTS,
                        accentColor = GeminiPrimary
                    )

                    Text(
                        text = "Gemini Flash Text-to-Speech",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Voice Selection
                    Column {
                        Text(
                            text = "Select Persona Voice:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Kore", "Puck", "Fenrir", "Aoede").forEach { voice ->
                                FilterSelectableChip(
                                    text = voice,
                                    isSelected = selectedVoice == voice,
                                    onClick = { viewModel.selectedTtsVoice.value = voice }
                                )
                            }
                        }
                    }

                    var ttsText by remember { mutableStateOf("Welcome to Gemini Place. Everything you build, think, or synthesize is synchronized seamlessly across your ecosystem.") }
                    OutlinedTextField(
                        value = ttsText,
                        onValueChange = { ttsText = it },
                        label = { Text("Text to Speak") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_input_field"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.generateTTS(ttsText) },
                        enabled = !isGenerating && ttsText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_tts_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthesizing voice via $selectedVoice...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speak with Gemini Flash TTS", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // --- TAB 1: Lyria 3 Music (lyria-3-clip-preview vs lyria-3-pro-preview) ---
                    val musicModel = if (isLyriaPro) GeminiConstants.MODEL_LYRIA_PRO else GeminiConstants.MODEL_LYRIA_CLIP
                    ModelBadge(
                        modelName = musicModel,
                        accentColor = GeminiAccentAmber
                    )

                    Text(
                        text = "Lyria 3 Music Generation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Mode: Clip vs Pro
                    Column {
                        Text(
                            text = "Model Architecture:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterSelectableChip(
                                text = "lyria-3-clip-preview (Short Clip ≤ 30s)",
                                isSelected = !isLyriaPro,
                                onClick = { viewModel.isLyriaPro.value = false },
                                modifier = Modifier.testTag("lyria_clip_chip")
                            )
                            FilterSelectableChip(
                                text = "lyria-3-pro-preview (Full Track)",
                                isSelected = isLyriaPro,
                                onClick = { viewModel.isLyriaPro.value = true },
                                modifier = Modifier.testTag("lyria_pro_chip")
                            )
                        }
                    }

                    // Genre Quick Pills
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Ambient Space Synth", "Cyberpunk Industrial", "Lo-Fi Study Piano", "Cinematic Epic Strings").forEach { genre ->
                            FilterSelectableChip(
                                text = genre,
                                isSelected = false,
                                onClick = { viewModel.generateMusic(genre) }
                            )
                        }
                    }

                    var musicPrompt by remember { mutableStateOf("Futuristic melodic techno with warm analog bass, ethereal vocal pads, and crisp electronic beats") }
                    OutlinedTextField(
                        value = musicPrompt,
                        onValueChange = { musicPrompt = it },
                        label = { Text("Music Composition Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiAccentAmber,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.generateMusic(musicPrompt) },
                        enabled = !isGenerating && musicPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_music_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiAccentAmber)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Composing with $musicModel...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLyriaPro) "Generate Full Track (Lyria Pro)" else "Generate Short Clip (Lyria Clip)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                2 -> {
                    // --- TAB 2: Audio Transcription (gemini-3.5-transcribe) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_35_TRANSCRIBE,
                        accentColor = GeminiAccentCyan
                    )

                    Text(
                        text = "Record & Transcribe Speech",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) GeminiAccentRose else GeminiAccentCyan)
                                    .clickable {
                                        if (isRecording) {
                                            viewModel.stopMicAndTranscribe()
                                        } else {
                                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                viewModel.startLiveMicRecording()
                                            } else {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .testTag("record_transcribe_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                    tint = if (isRecording) Color.White else Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isRecording) "Recording Voice... Tap to Stop" else "Tap to Record Audio",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Model: gemini-3.5-transcribe will decode speech to text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Button for instant simulated mic recording if hardware mic permissions are not granted
                    Button(
                        onClick = { viewModel.stopMicAndTranscribe() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Transcribe Sample Speech", color = MaterialTheme.colorScheme.onSurface)
                    }

                    transcriptionResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("gemini-3.5-transcribe Result:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = GeminiAccentCyan)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(result, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                3 -> {
                    // --- TAB 3: Live Audio Conversation (gemini-3.1-flash-live-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_31_LIVE,
                        accentColor = GeminiAccentEmerald
                    )

                    Text(
                        text = "Gemini Live Voice Conversations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PulsingLiveOrb(
                            isActive = liveVoiceStatus.contains("Speaking", ignoreCase = true) || liveVoiceStatus.contains("Listening", ignoreCase = true),
                            statusText = liveVoiceStatus
                        )
                    }

                    var voiceInput by remember { mutableStateOf("Hello Gemini, what are the best practices for real-time synchronization?") }
                    OutlinedTextField(
                        value = voiceInput,
                        onValueChange = { voiceInput = it },
                        label = { Text("Speak or Type to Gemini Live") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_voice_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiAccentEmerald,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.submitLiveVoiceTurn(voiceInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("live_voice_turn_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiAccentEmerald)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Live Voice Turn", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    liveVoiceAiResponse?.let { resp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Gemini Live Voice Turn", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = GeminiAccentEmerald)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(resp, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // --- AUDIO PLAYBACK BAR ---
            latestCreation?.let { creation ->
                Spacer(modifier = Modifier.height(8.dp))
                AudioPlaybackBar(
                    isPlaying = isPlaying,
                    onPlayToggle = {
                        if (isPlaying) {
                            viewModel.audioHelper.stopPlayback()
                        } else {
                            creation.base64Data?.let {
                                viewModel.audioHelper.playBase64Audio(it, creation.id)
                            }
                        }
                    },
                    durationText = "${creation.audioDurationSec}s",
                    title = "${creation.model}: ${creation.title.take(24)}"
                )
            }
        }
    }
}
