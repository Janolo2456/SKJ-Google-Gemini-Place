package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class AudioHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _activeAudioId = MutableStateFlow<String?>(null)
    val activeAudioId: StateFlow<String?> = _activeAudioId.asStateFlow()

    fun playBase64Audio(base64Data: String, id: String = java.util.UUID.randomUUID().toString()) {
        try {
            stopPlayback()
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = File.createTempFile("gemini_audio_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _activeAudioId.value = null
                }
                start()
            }
            _isPlaying.value = true
            _activeAudioId.value = id
        } catch (e: Exception) {
            Log.e("AudioHelper", "Play error: ${e.message}")
            _isPlaying.value = false
            _activeAudioId.value = null
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // ignore
        } finally {
            _isPlaying.value = false
            _activeAudioId.value = null
        }
    }

    fun startRecording(): Boolean {
        return try {
            recordFile = File.createTempFile("gemini_rec_", ".mp4", context.cacheDir)
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordFile?.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            true
        } catch (e: Exception) {
            Log.e("AudioHelper", "Start record error: ${e.message}")
            _isRecording.value = false
            false
        }
    }

    fun stopRecording(): String? {
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecording.value = false

            val file = recordFile
            if (file != null && file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Stop record error: ${e.message}")
            _isRecording.value = false
            null
        }
    }
}
