package com.example.gemini

data class CreationItem(
    val id: String = "",
    val type: CreationType = CreationType.IMAGE,
    val title: String = "",
    val prompt: String = "",
    val model: String = "",
    val mediaUrl: String? = null,
    val base64Data: String? = null,
    val textResult: String? = null,
    val audioDurationSec: Int = 0,
    val videoAspectRatio: String = "16:9",
    val imageResolution: String = "1K",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

enum class CreationType {
    IMAGE,
    IMAGE_PRO,
    IMAGE_EDIT,
    VIDEO_VEO,
    VIDEO_ANIMATION,
    TTS_AUDIO,
    MUSIC_CLIP,
    MUSIC_PRO,
    TRANSCRIPTION,
    THINKING_SESSION,
    SEARCH_QUERY,
    MAPS_QUERY
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val model: String = GeminiConstants.MODEL_GEMINI_35_FLASH,
    val timestamp: Long = System.currentTimeMillis(),
    val thoughts: String? = null,
    val searchSources: List<SearchSource> = emptyList(),
    val mapPlaces: List<MapPlace> = emptyList(),
    val latencyMs: Long? = null,
    val isAudio: Boolean = false,
    val audioBase64: String? = null
)

data class SearchSource(
    val title: String,
    val uri: String
)

data class MapPlace(
    val name: String,
    val address: String,
    val rating: String? = null,
    val uri: String? = null
)

object GeminiConstants {
    // Model constants as specified in instructions:
    const val MODEL_GEMINI_31_PRO = "gemini-3.1-pro-preview"
    const val MODEL_GEMINI_35_FLASH = "gemini-3.5-flash"
    const val MODEL_GEMINI_31_FLASH_LITE = "gemini-3.1-flash-lite"
    const val MODEL_GEMINI_31_TTS = "gemini-3.1-flash-tts-preview"
    const val MODEL_LYRIA_CLIP = "lyria-3-clip-preview"
    const val MODEL_LYRIA_PRO = "lyria-3-pro-preview"
    const val MODEL_GEMINI_31_IMAGE = "gemini-3.1-flash-image-preview"
    const val MODEL_GEMINI_3_PRO_IMAGE = "gemini-3-pro-image-preview"
    const val MODEL_GEMINI_31_LIVE = "gemini-3.1-flash-live-preview"
    const val MODEL_VEO_FAST = "veo-3.1-fast-generate-preview"
    const val MODEL_GEMINI_35_TRANSCRIBE = "gemini-3.5-transcribe"

    val SYSTEM_ROLES = listOf(
        ChatRolePreset("AI Architect", "You are an elite systems architect and AI polymath. Provide rigorous, structured, and visionary guidance.", "architecture"),
        ChatRolePreset("Code Sage", "You are a master software engineer. Provide pristine, bug-free, modern idiomatic code solutions.", "code"),
        ChatRolePreset("Creative Muse", "You are an imaginative storyteller, lyrical poet, and creative visionary.", "palette"),
        ChatRolePreset("Research Scientist", "You are an empirical scientific researcher. Ground answers in logic, citations, and critical reasoning.", "science"),
        ChatRolePreset("Executive Copilot", "You are a high-level strategic advisor. Keep answers ultra-concise, actionable, and executive-ready.", "business")
    )
}

data class ChatRolePreset(
    val name: String,
    val instruction: String,
    val iconKey: String
)
