package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.GeminiConstants
import com.example.ui.components.FilterSelectableChip
import com.example.ui.components.ModelBadge
import com.example.ui.theme.GeminiAccentCyan
import com.example.ui.theme.GeminiAccentRose
import com.example.ui.theme.GeminiPrimary
import com.example.ui.theme.GeminiSecondary
import com.example.viewmodel.GeminiPlaceViewModel

@Composable
fun ImageStudioScreen(viewModel: GeminiPlaceViewModel, modifier: Modifier = Modifier) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isImageGenerating.collectAsState()
    val latestCreation by viewModel.latestImageCreation.collectAsState()
    val selectedResolution by viewModel.selectedImageResolution.collectAsState()
    val selectedAspectRatio by viewModel.selectedImageAspectRatio.collectAsState()
    val uploadedBase64 by viewModel.uploadedImageBase64.collectAsState()
    val visionAnalysisResult by viewModel.imageAnalysisResult.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.processSelectedImage(it, isForVideo = false) }
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
                text = { Text("HQ Pro Image", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Create & Edit", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Vision Analysis", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
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
                    // --- TAB 0: High-Quality Pro Images (gemini-3-pro-image-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_3_PRO_IMAGE,
                        accentColor = GeminiPrimary
                    )

                    Text(
                        text = "Generate High-Quality Imagery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Resolution affordance: 1K, 2K, 4K
                    Column {
                        Text(
                            text = "Target Resolution (Mandatory Affordance):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1K", "2K", "4K").forEach { res ->
                                FilterSelectableChip(
                                    text = res,
                                    isSelected = selectedResolution == res,
                                    onClick = { viewModel.selectedImageResolution.value = res }
                                )
                            }
                        }
                    }

                    // Aspect Ratio
                    Column {
                        Text(
                            text = "Aspect Ratio:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1:1", "16:9", "9:16", "4:3", "3:4").forEach { ratio ->
                                FilterSelectableChip(
                                    text = ratio,
                                    isSelected = selectedAspectRatio == ratio,
                                    onClick = { viewModel.selectedImageAspectRatio.value = ratio }
                                )
                            }
                        }
                    }

                    var proPrompt by remember { mutableStateOf("A hyper-realistic iridescent glass sculpture floating in deep cosmic nebula, cinematic lighting, 8k render") }
                    OutlinedTextField(
                        value = proPrompt,
                        onValueChange = { proPrompt = it },
                        label = { Text("Prompt for Pro Image") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_pro_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.generateProImage(proPrompt) },
                        enabled = !isGenerating && proPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_pro_image_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rendering $selectedResolution image...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate $selectedResolution Image", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // --- TAB 1: Create & Edit Images (gemini-3.1-flash-image-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_31_IMAGE,
                        accentColor = GeminiSecondary
                    )

                    Text(
                        text = "Create & Edit Images with Text",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Photo selector or upload
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
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GeminiSecondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uploadedBase64 != null) "Photo Selected for Editing ✓" else "Select Photo to Edit (Optional)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uploadedBase64 != null) "Tap to change image or leave blank to create anew" else "Zero-permission Android Photo Picker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Preset demo image toggle if device has no photos
                    if (uploadedBase64 == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Generate a sample 64x64 solid/gradient bitmap in Base64 for instant demo testing
                                    val demoBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(demoBitmap)
                                    canvas.drawColor(android.graphics.Color.DKGRAY)
                                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.CYAN; textSize = 24f }
                                    canvas.drawText("SOURCE", 12f, 65f, paint)
                                    val stream = java.io.ByteArrayOutputStream()
                                    demoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    viewModel.uploadedImageBase64.value = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiSecondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "💡 Tap to load demo sample image for instant edit testing",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiSecondary
                            )
                        }
                    }

                    var editPrompt by remember { mutableStateOf("Transform this image with futuristic cybernetic enhancements and neon violet volumetric lighting") }
                    OutlinedTextField(
                        value = editPrompt,
                        onValueChange = { editPrompt = it },
                        label = { Text("Editing / Creation Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_edit_prompt_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.createOrEditImage(editPrompt) },
                        enabled = !isGenerating && editPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_or_edit_image_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiSecondary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing via gemini-3.1-flash-image-preview...")
                        } else {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uploadedBase64 != null) "Apply AI Edit to Photo" else "Create Image with Prompt",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                2 -> {
                    // --- TAB 2: Image Understanding (gemini-3.1-pro-preview) ---
                    ModelBadge(
                        modelName = GeminiConstants.MODEL_GEMINI_31_PRO,
                        accentColor = GeminiAccentCyan
                    )

                    Text(
                        text = "Analyze Images & Visual Documents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

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
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = GeminiAccentCyan, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uploadedBase64 != null) "Photo Ready for Vision Inspection ✓" else "Upload Photo to Analyze",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Inspect objects, text, diagrams, and artistic composition",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uploadedBase64 == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val demoBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(demoBitmap)
                                    canvas.drawColor(android.graphics.Color.BLUE)
                                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 20f }
                                    canvas.drawText("GEMINI", 20f, 65f, paint)
                                    val stream = java.io.ByteArrayOutputStream()
                                    demoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    viewModel.uploadedImageBase64.value = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiAccentCyan.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "💡 Tap to load demo image for instant vision reasoning",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiAccentCyan
                            )
                        }
                    }

                    var visionPrompt by remember { mutableStateOf("Explain what is in this image, identify any key subjects, color harmony, and extract any text.") }
                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        label = { Text("Analysis Question / Instruction") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vision_prompt_input"),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiAccentCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.analyzeUploadedImage(visionPrompt) },
                        enabled = !isGenerating && uploadedBase64 != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("analyze_image_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiAccentCyan)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing with gemini-3.1-pro-preview...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze with Gemini 3.1 Pro", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    visionAnalysisResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Vision Understanding Result", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = GeminiAccentCyan)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(result, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // --- RESULT DISPLAY CARD ---
            latestCreation?.let { creation ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latest Image Result",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GeminiPrimary
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

                        // Display Base64 bitmap if present
                        if (!creation.base64Data.isNullOrBlank()) {
                            val decoded = remember(creation.base64Data) {
                                try {
                                    val bytes = Base64.decode(creation.base64Data, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) { null }
                            }
                            decoded?.let { bmp ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Generated Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
