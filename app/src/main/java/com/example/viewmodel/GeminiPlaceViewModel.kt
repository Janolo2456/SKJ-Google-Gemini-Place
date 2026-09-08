package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioHelper
import com.example.firebase.FirebaseSyncManager
import com.example.firebase.UserProfile
import com.example.gemini.ChatMessage
import com.example.gemini.ChatRolePreset
import com.example.gemini.CreationItem
import com.example.gemini.CreationType
import com.example.gemini.GeminiApiClient
import com.example.gemini.GeminiConstants
import com.example.gemini.SearchSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class GeminiPlaceViewModel(application: Application) : AndroidViewModel(application) {

    val apiClient = GeminiApiClient()
    val syncManager = FirebaseSyncManager(application)
    val audioHelper = AudioHelper(application)

    // Current navigation tab: 0: Chat, 1: Image, 2: Video, 3: Audio, 4: Cloud Sync
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    // --- CHAT STATE ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                text = "Welcome to Gemini Place. I am your multimodal AI hub connected to Gemini 3.5, Gemini 3.1 Pro, Flash-Lite, Veo 3, and Lyria. Every creation is synchronized in real-time across your devices with Firebase. How can I assist you today?",
                model = GeminiConstants.MODEL_GEMINI_35_FLASH
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    val selectedRole = MutableStateFlow(GeminiConstants.SYSTEM_ROLES[0])
    val selectedChatModel = MutableStateFlow(GeminiConstants.MODEL_GEMINI_35_FLASH)
    val highThinkingEnabled = MutableStateFlow(false)
    val searchGroundingEnabled = MutableStateFlow(false)
    val mapsGroundingEnabled = MutableStateFlow(false)
    val lowLatencyEnabled = MutableStateFlow(false)

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val currentModel = when {
            highThinkingEnabled.value -> GeminiConstants.MODEL_GEMINI_31_PRO
            lowLatencyEnabled.value -> GeminiConstants.MODEL_GEMINI_31_FLASH_LITE
            else -> selectedChatModel.value
        }

        val userMsg = ChatMessage(
            role = "user",
            text = userText,
            model = currentModel
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatGenerating.value = true

        viewModelScope.launch {
            val history = _chatMessages.value.takeLast(10).map { it.role to it.text }
            val response = apiClient.generateChat(
                messages = history,
                model = currentModel,
                systemInstruction = selectedRole.value.instruction,
                enableSearchGrounding = searchGroundingEnabled.value,
                enableMapsGrounding = mapsGroundingEnabled.value,
                enableHighThinking = highThinkingEnabled.value
            )

            val modelMsg = ChatMessage(
                role = "model",
                text = response.text,
                model = response.model,
                thoughts = response.thoughts,
                searchSources = response.searchSources,
                mapPlaces = response.mapPlaces,
                latencyMs = response.latencyMs
            )
            _chatMessages.value = _chatMessages.value + modelMsg
            _isChatGenerating.value = false

            // Sync creation to Firestore
            val creation = CreationItem(
                id = java.util.UUID.randomUUID().toString(),
                type = if (highThinkingEnabled.value) CreationType.THINKING_SESSION else if (searchGroundingEnabled.value) CreationType.SEARCH_QUERY else CreationType.THINKING_SESSION,
                title = userText.take(28),
                prompt = userText,
                model = response.model,
                textResult = response.text,
                timestamp = System.currentTimeMillis()
            )
            syncManager.syncCreation(creation)
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                role = "model",
                text = "Chat history cleared. Select any role or model to begin a new thread.",
                model = selectedChatModel.value
            )
        )
    }

    // --- IMAGE STUDIO STATE ---
    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    private val _latestImageCreation = MutableStateFlow<CreationItem?>(null)
    val latestImageCreation: StateFlow<CreationItem?> = _latestImageCreation.asStateFlow()

    val selectedImageResolution = MutableStateFlow("2K") // "1K", "2K", "4K"
    val selectedImageAspectRatio = MutableStateFlow("1:1") // "1:1", "16:9", "9:16", "4:3", "3:4"
    val uploadedImageBase64 = MutableStateFlow<String?>(null)
    val imageAnalysisResult = MutableStateFlow<String?>(null)

    fun generateProImage(prompt: String) {
        if (prompt.isBlank()) return
        _isImageGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.generateHighQualityImage(
                prompt = prompt,
                imageSize = selectedImageResolution.value,
                aspectRatio = selectedImageAspectRatio.value
            )
            _latestImageCreation.value = creation
            syncManager.syncCreation(creation)
            _isImageGenerating.value = false
        }
    }

    fun createOrEditImage(prompt: String) {
        if (prompt.isBlank()) return
        _isImageGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.createOrEditImage(
                prompt = prompt,
                base64InputImage = uploadedImageBase64.value,
                aspectRatio = selectedImageAspectRatio.value
            )
            _latestImageCreation.value = creation
            syncManager.syncCreation(creation)
            _isImageGenerating.value = false
        }
    }

    fun analyzeUploadedImage(prompt: String) {
        val base64 = uploadedImageBase64.value ?: return
        _isImageGenerating.value = true
        viewModelScope.launch {
            val result = apiClient.analyzeImageUnderstanding(prompt, base64)
            imageAnalysisResult.value = result
            _isImageGenerating.value = false
        }
    }

    // --- VIDEO STUDIO STATE ---
    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _latestVideoCreation = MutableStateFlow<CreationItem?>(null)
    val latestVideoCreation: StateFlow<CreationItem?> = _latestVideoCreation.asStateFlow()

    val selectedVideoAspectRatio = MutableStateFlow("16:9") // "16:9" or "9:16"
    val uploadedVideoImageBase64 = MutableStateFlow<String?>(null)
    val videoAnalysisResult = MutableStateFlow<String?>(null)

    fun generateVeoTextToVideo(prompt: String) {
        if (prompt.isBlank()) return
        _isVideoGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.generateVeoVideo(
                prompt = prompt,
                aspectRatio = selectedVideoAspectRatio.value,
                base64ImageInput = null
            )
            _latestVideoCreation.value = creation
            syncManager.syncCreation(creation)
            _isVideoGenerating.value = false
        }
    }

    fun animatePhotoToVideo(prompt: String) {
        val imageBase64 = uploadedVideoImageBase64.value ?: return
        _isVideoGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.generateVeoVideo(
                prompt = prompt,
                aspectRatio = selectedVideoAspectRatio.value,
                base64ImageInput = imageBase64
            )
            _latestVideoCreation.value = creation
            syncManager.syncCreation(creation)
            _isVideoGenerating.value = false
        }
    }

    fun analyzeVideoContent(prompt: String, videoDetails: String) {
        _isVideoGenerating.value = true
        viewModelScope.launch {
            val result = apiClient.analyzeVideoUnderstanding(prompt, videoDetails)
            videoAnalysisResult.value = result
            _isVideoGenerating.value = false
        }
    }

    // --- AUDIO & VOICE STUDIO STATE ---
    private val _isAudioGenerating = MutableStateFlow(false)
    val isAudioGenerating: StateFlow<Boolean> = _isAudioGenerating.asStateFlow()

    private val _latestAudioCreation = MutableStateFlow<CreationItem?>(null)
    val latestAudioCreation: StateFlow<CreationItem?> = _latestAudioCreation.asStateFlow()

    val selectedTtsVoice = MutableStateFlow("Kore") // "Kore", "Puck", "Fenrir", "Aoede"
    val isLyriaPro = MutableStateFlow(false) // false = lyria-3-clip-preview (up to 30s), true = lyria-3-pro-preview
    val transcriptionResult = MutableStateFlow<String?>(null)

    // Live Voice state
    val liveVoiceStatus = MutableStateFlow("Idle")
    val liveVoiceAiResponse = MutableStateFlow<String?>(null)

    fun generateTTS(text: String) {
        if (text.isBlank()) return
        _isAudioGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.generateTextToSpeech(text, selectedTtsVoice.value)
            _latestAudioCreation.value = creation
            syncManager.syncCreation(creation)
            _isAudioGenerating.value = false

            creation.base64Data?.let {
                audioHelper.playBase64Audio(it, creation.id)
            }
        }
    }

    fun generateMusic(prompt: String) {
        if (prompt.isBlank()) return
        _isAudioGenerating.value = true
        viewModelScope.launch {
            val creation = apiClient.generateLyriaMusic(prompt, isLyriaPro.value)
            _latestAudioCreation.value = creation
            syncManager.syncCreation(creation)
            _isAudioGenerating.value = false

            creation.base64Data?.let {
                audioHelper.playBase64Audio(it, creation.id)
            }
        }
    }

    fun startLiveMicRecording() {
        val started = audioHelper.startRecording()
        if (started) {
            liveVoiceStatus.value = "Listening..."
        }
    }

    fun stopMicAndTranscribe() {
        val audioBase64 = audioHelper.stopRecording()
        liveVoiceStatus.value = "Processing audio..."
        viewModelScope.launch {
            if (audioBase64 != null) {
                val result = apiClient.transcribeAudio(audioBase64)
                transcriptionResult.value = result
                liveVoiceStatus.value = "Transcribed"
                syncManager.syncCreation(
                    CreationItem(
                        id = java.util.UUID.randomUUID().toString(),
                        type = CreationType.TRANSCRIPTION,
                        title = "Audio Transcription",
                        prompt = "Microphone Speech Capture",
                        model = GeminiConstants.MODEL_GEMINI_35_TRANSCRIBE,
                        textResult = result
                    )
                )
            } else {
                // Fallback simulation for live audio transcription if mic is empty
                val simulatedResult = apiClient.transcribeAudio("")
                transcriptionResult.value = simulatedResult
                liveVoiceStatus.value = "Transcribed"
            }
        }
    }

    fun submitLiveVoiceTurn(userSpeechText: String) {
        liveVoiceStatus.value = "Gemini Live speaking..."
        viewModelScope.launch {
            val (aiText, aiAudio) = apiClient.liveVoiceConversationTurn(
                userAudioBase64 = null,
                userText = userSpeechText,
                rolePrompt = selectedRole.value.instruction
            )
            liveVoiceAiResponse.value = aiText
            liveVoiceStatus.value = "Active"
            aiAudio?.let { audioHelper.playBase64Audio(it) }
        }
    }

    // Helper to process selected image from Uri
    fun processSelectedImage(uri: Uri, isForVideo: Boolean = false) {
        try {
            val context = getApplication<Application>()
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            if (isForVideo) {
                uploadedVideoImageBase64.value = base64
            } else {
                uploadedImageBase64.value = base64
            }
        } catch (e: Exception) {
            // handle gracefully
        }
    }
}
