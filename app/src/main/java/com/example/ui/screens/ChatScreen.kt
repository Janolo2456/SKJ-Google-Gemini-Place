package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.ChatMessage
import com.example.gemini.GeminiConstants
import com.example.ui.components.FilterSelectableChip
import com.example.ui.components.ModelBadge
import com.example.ui.theme.GeminiAccentAmber
import com.example.ui.theme.GeminiAccentCyan
import com.example.ui.theme.GeminiAccentEmerald
import com.example.ui.theme.GeminiAccentRose
import com.example.ui.theme.GeminiPrimary
import com.example.ui.theme.GeminiSecondary
import com.example.viewmodel.GeminiPlaceViewModel

@Composable
fun ChatScreen(viewModel: GeminiPlaceViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isChatGenerating.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val selectedModel by viewModel.selectedChatModel.collectAsState()
    val highThinking by viewModel.highThinkingEnabled.collectAsState()
    val searchGrounding by viewModel.searchGroundingEnabled.collectAsState()
    val mapsGrounding by viewModel.mapsGroundingEnabled.collectAsState()
    val lowLatency by viewModel.lowLatencyEnabled.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- CONTROL STRIP: Roles & Mode Filters ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Roles horizontal row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GeminiConstants.SYSTEM_ROLES.forEach { role ->
                        FilterSelectableChip(
                            text = role.name,
                            isSelected = selectedRole == role,
                            onClick = { viewModel.selectedRole.value = role }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Models & Capabilities Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Models
                    FilterSelectableChip(
                        text = "3.5 Flash",
                        isSelected = selectedModel == GeminiConstants.MODEL_GEMINI_35_FLASH && !highThinking && !lowLatency,
                        onClick = {
                            viewModel.selectedChatModel.value = GeminiConstants.MODEL_GEMINI_35_FLASH
                            viewModel.highThinkingEnabled.value = false
                            viewModel.lowLatencyEnabled.value = false
                        }
                    )

                    FilterSelectableChip(
                        text = "3.1 Pro",
                        isSelected = selectedModel == GeminiConstants.MODEL_GEMINI_31_PRO && !highThinking && !lowLatency,
                        onClick = {
                            viewModel.selectedChatModel.value = GeminiConstants.MODEL_GEMINI_31_PRO
                            viewModel.highThinkingEnabled.value = false
                            viewModel.lowLatencyEnabled.value = false
                        }
                    )

                    // Special Feature Toggles
                    Surface(
                        modifier = Modifier
                            .clickable {
                                viewModel.highThinkingEnabled.value = !highThinking
                                if (!highThinking) {
                                    viewModel.lowLatencyEnabled.value = false
                                }
                            }
                            .testTag("toggle_thinking_mode"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (highThinking) GeminiAccentAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (highThinking) GeminiAccentAmber else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = if (highThinking) GeminiAccentAmber else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("High Thinking", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (highThinking) GeminiAccentAmber else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clickable {
                                viewModel.lowLatencyEnabled.value = !lowLatency
                                if (!lowLatency) {
                                    viewModel.highThinkingEnabled.value = false
                                }
                            }
                            .testTag("toggle_low_latency"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (lowLatency) GeminiAccentEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (lowLatency) GeminiAccentEmerald else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = if (lowLatency) GeminiAccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Lite", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (lowLatency) GeminiAccentEmerald else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.searchGroundingEnabled.value = !searchGrounding }
                            .testTag("toggle_search_grounding"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (searchGrounding) GeminiAccentCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (searchGrounding) GeminiAccentCyan else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = if (searchGrounding) GeminiAccentCyan else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Search", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (searchGrounding) GeminiAccentCyan else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.mapsGroundingEnabled.value = !mapsGrounding }
                            .testTag("toggle_maps_grounding"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (mapsGrounding) GeminiAccentRose.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (mapsGrounding) GeminiAccentRose else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = if (mapsGrounding) GeminiAccentRose else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Maps", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (mapsGrounding) GeminiAccentRose else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(32.dp).testTag("clear_chat_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- CHAT MESSAGES LIST ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onSpeak = { text -> viewModel.generateTTS(text) }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = GeminiPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when {
                                highThinking -> "Deep thinking step with gemini-3.1-pro-preview..."
                                searchGrounding -> "Searching Google live indexes..."
                                mapsGrounding -> "Querying Google Maps grounding..."
                                lowLatency -> "Streaming ultra-fast from gemini-3.1-flash-lite..."
                                else -> "Generating response from ${selectedModel}..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // --- INPUT BAR ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = when {
                                highThinking -> "Ask a complex reasoning or math query..."
                                searchGrounding -> "Search current news or facts..."
                                mapsGrounding -> "Find places or directions..."
                                lowLatency -> "Ask anything (low-latency mode)..."
                                else -> "Message ${selectedRole.name}..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeminiPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank() && !isGenerating) {
                            val text = inputPrompt
                            inputPrompt = ""
                            viewModel.sendChatMessage(text)
                        }
                    },
                    enabled = inputPrompt.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputPrompt.isNotBlank() && !isGenerating) GeminiPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputPrompt.isNotBlank() && !isGenerating) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var thoughtsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Model / role header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (!isUser) {
                ModelBadge(modelName = message.model)
                message.latencyMs?.let { ms ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GeminiAccentEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚡ ${ms}ms",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = GeminiAccentEmerald
                        )
                    }
                }
            } else {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Thinking accordion if present
        if (!message.thoughts.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 6.dp)
                    .clickable { thoughtsExpanded = !thoughtsExpanded },
                shape = RoundedCornerShape(12.dp),
                color = GeminiAccentAmber.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiAccentAmber.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = GeminiAccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thinking Process (ThinkingLevel.HIGH)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GeminiAccentAmber
                            )
                        }
                        Icon(
                            imageVector = if (thoughtsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = GeminiAccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = thoughtsExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = message.thoughts,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Message body
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                GeminiPrimary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface
                )

                // Search Grounding sources
                if (message.searchSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Google Search Sources:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GeminiAccentCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.searchSources.take(3).forEach { source ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.uri))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = GeminiAccentCyan.copy(alpha = 0.1f)
                        ) {
                            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = GeminiAccentCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = source.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = GeminiAccentCyan,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Maps Grounding places
                if (message.mapPlaces.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Google Maps Locations:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GeminiAccentRose
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.mapPlaces.forEach { place ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = GeminiAccentRose.copy(alpha = 0.1f)
                        ) {
                            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, tint = GeminiAccentRose, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = place.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = place.address, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // TTS Read-Aloud Action
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(28.dp).testTag("tts_speak_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud via Gemini TTS",
                                tint = GeminiPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
