package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.CreationType
import com.example.gemini.GeminiConstants
import com.example.ui.components.FilterSelectableChip
import com.example.ui.components.ModelBadge
import com.example.ui.theme.GeminiAccentCyan
import com.example.ui.theme.GeminiAccentRose
import com.example.ui.theme.GeminiPrimary
import com.example.ui.theme.GeminiSecondary
import com.example.viewmodel.GeminiPlaceViewModel

@Composable
fun VideoStudioScreen(viewModel: GeminiPlaceViewModel, modifier: Modifier = Modifier) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isVideoGenerating.collectAsState()
    val latestCreation by viewModel.latestVideoCreation.collectAsState()
    val selectedAspectRatio by viewModel.selectedVideoAspectRatio.collectAsState()
    val uploadedPhotoBase64 by viewModel.uploadedVideoImageBase64.collectAsState()
    val videoAnalysisResult by viewModel.videoAnalysisResult.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.processSelectedImage(it, isForVideo = true) }
    }

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
                text = { Text("Veo 3 Video", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Animate Photo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Understand Video", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
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
                    // --- TAB 0: Veo 3 Text-to-Video (veo-3.1-fast-generate-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_VEO_FAST,
                        accentColor = GeminiAccentRose
                    )

                    Text(
                        text = "Veo 3 Generative Video",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Aspect Ratio (Strict requirement: 16:9 or 9:16)
                    Column {
                        Text(
                            text = "Aspect Ratio (Required: 16:9 or 9:16):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterSelectableChip(
                                text = "16:9 (Landscape)",
                                isSelected = selectedAspectRatio == "16:9",
                                onClick = { viewModel.selectedVideoAspectRatio.value = "16:9" },
                                modifier = Modifier.testTag("video_aspect_16_9")
                            )
                            FilterSelectableChip(
                                text = "9:16 (Portrait)",
                                isSelected = selectedAspectRatio == "9:16",
                                onClick = { viewModel.selectedVideoAspectRatio.value = "9:16" },
                                modifier = Modifier.testTag("video_aspect_9_16")
                            )
                        }
                    }

                    var textPrompt by remember { mutableStateOf("A neon drone flying gracefully through a futuristic cyberpunk metropolis with rain reflections, slow motion") }
                    OutlinedTextField(
                        value = textPrompt,
                        onValueChange = { textPrompt = it },
                        label = { Text("Video Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("veo_text_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiAccentRose,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.generateVeoTextToVideo(textPrompt) },
                        enabled = !isGenerating && textPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_veo_video_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiAccentRose)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rendering Veo 3 Video ($selectedAspectRatio)...")
                        } else {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Veo Video ($selectedAspectRatio)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // --- TAB 1: Animate Photo into Video (veo-3.1-fast-generate-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_VEO_FAST,
                        accentColor = GeminiPrimary
                    )

                    Text(
                        text = "Animate Photo into Veo Video",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Aspect Ratio
                    Column {
                        Text(
                            text = "Aspect Ratio (16:9 or 9:16):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterSelectableChip(
                                text = "16:9 (Landscape)",
                                isSelected = selectedAspectRatio == "16:9",
                                onClick = { viewModel.selectedVideoAspectRatio.value = "16:9" }
                            )
                            FilterSelectableChip(
                                text = "9:16 (Portrait)",
                                isSelected = selectedAspectRatio == "9:16",
                                onClick = { viewModel.selectedVideoAspectRatio.value = "9:16" }
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GeminiPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uploadedPhotoBase64 != null) "Photo Ready to Animate ✓" else "Select Photo to Animate",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Veo will bring this image to life with fluid motion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uploadedPhotoBase64 == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val demoBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(demoBitmap)
                                    canvas.drawColor(android.graphics.Color.MAGENTA)
                                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 20f }
                                    canvas.drawText("VEO", 35f, 65f, paint)
                                    val stream = java.io.ByteArrayOutputStream()
                                    demoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    viewModel.uploadedVideoImageBase64.value = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "💡 Tap to load demo image for instant photo animation",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiPrimary
                            )
                        }
                    }

                    var animationPrompt by remember { mutableStateOf("Animate with gentle wind blowing through the scene, clouds drifting smoothly across the horizon") }
                    OutlinedTextField(
                        value = animationPrompt,
                        onValueChange = { animationPrompt = it },
                        label = { Text("Motion / Animation Instructions") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("animate_photo_prompt_input"),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.animatePhotoToVideo(animationPrompt) },
                        enabled = !isGenerating && uploadedPhotoBase64 != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("animate_photo_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Animating photo via Veo 3...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Animate Photo into Video ($selectedAspectRatio)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // --- TAB 2: Video Understanding (gemini-3.1-pro-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_31_PRO,
                        accentColor = GeminiSecondary
                    )

                    Text(
                        text = "Analyze Video Content with Gemini Pro",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    var videoContext by remember { mutableStateOf("Drone flight sequence capturing historical architecture at sunset with crowd movements and ambient lighting transitions.") }
                    OutlinedTextField(
                        value = videoContext,
                        onValueChange = { videoContext = it },
                        label = { Text("Video Metadata / Scene Context") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    var videoQuestion by remember { mutableStateOf("Analyze key information: breakdown timestamps, summarize camera techniques, and identify important actions.") }
                    OutlinedTextField(
                        value = videoQuestion,
                        onValueChange = { videoQuestion = it },
                        label = { Text("Analysis Query") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_analysis_prompt_input"),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.analyzeVideoContent(videoQuestion, videoContext) },
                        enabled = !isGenerating && videoQuestion.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_video_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiSecondary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing video reasoning with 3.1 Pro...")
                        } else {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Video with 3.1 Pro", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    videoAnalysisResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Gemini Pro Video Understanding", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = GeminiSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(result, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // --- VIDEO RESULT DISPLAY ---
            latestCreation?.let { creation ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiAccentRose.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Veo Video Generation Result",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GeminiAccentRose
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = GeminiAccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synced to Firebase", style = MaterialTheme.typography.labelSmall, color = GeminiAccentCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = creation.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        creation.textResult?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Video Player Placeholder / Frame
                        val isPortrait = creation.videoAspectRatio == "9:16"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isPortrait) 260.dp else 180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                    )
                                )
                                .border(1.dp, GeminiAccentRose.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play Video",
                                    tint = GeminiAccentRose,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Veo 3 [${creation.videoAspectRatio}] • 1080p",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
