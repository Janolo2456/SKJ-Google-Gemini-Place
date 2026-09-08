package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.gemini.CreationItem
import com.example.gemini.CreationType
import com.example.ui.components.FilterSelectableChip
import com.example.ui.components.ModelBadge
import com.example.ui.theme.GeminiAccentCyan
import com.example.ui.theme.GeminiAccentEmerald
import com.example.ui.theme.GeminiAccentRose
import com.example.ui.theme.GeminiPrimary
import com.example.viewmodel.GeminiPlaceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncScreen(viewModel: GeminiPlaceViewModel, modifier: Modifier = Modifier) {
    val currentUser by viewModel.syncManager.currentUser.collectAsState()
    val syncStatus by viewModel.syncManager.syncStatus.collectAsState()
    val creations by viewModel.syncManager.creations.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredCreations = remember(creations, selectedFilter) {
        when (selectedFilter) {
            "Images" -> creations.filter { it.type == CreationType.IMAGE || it.type == CreationType.IMAGE_PRO || it.type == CreationType.IMAGE_EDIT }
            "Videos" -> creations.filter { it.type == CreationType.VIDEO_VEO || it.type == CreationType.VIDEO_ANIMATION }
            "Audio" -> creations.filter { it.type == CreationType.TTS_AUDIO || it.type == CreationType.MUSIC_CLIP || it.type == CreationType.MUSIC_PRO || it.type == CreationType.TRANSCRIPTION }
            "Thinking" -> creations.filter { it.type == CreationType.THINKING_SESSION || it.type == CreationType.SEARCH_QUERY }
            else -> creations
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- AUTH CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GeminiPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Avatar",
                                tint = GeminiPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentUser?.isAnonymous == false) (currentUser?.displayName ?: "User") else "Guest Explorer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser?.isAnonymous == false && currentUser?.email != null) currentUser?.email!! else "Anonymous Guest Session • Ready to sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentUser?.isAnonymous == false) {
                            OutlinedButton(
                                onClick = { viewModel.syncManager.signOut() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sign Out", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { showAuthDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("google_firebase_signin_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync status pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = GeminiAccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firestore Real-Time DB: $syncStatus",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = GeminiAccentCyan
                            )
                        }
                    }
                }
            }
        }

        // --- FILTER PILLS ---
        item {
            Column {
                Text(
                    text = "Real-Time Synchronized Creations (${creations.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Images", "Videos", "Audio", "Thinking").forEach { filter ->
                        FilterSelectableChip(
                            text = filter,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }
        }

        // --- EMPTY STATE ---
        if (filteredCreations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No $selectedFilter creations yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generate chats, images, videos, or audio to watch them synchronize instantly via Firestore!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- CREATION ITEMS STREAM ---
        items(filteredCreations, key = { it.id }) { item ->
            CreationSyncCard(
                item = item,
                onDelete = { viewModel.syncManager.deleteCreation(item.id) }
            )
        }
    }

    // --- AUTH DIALOG ---
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Sign In or Register", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your personal email and password to sync your creations across devices via Firebase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            authError = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. your.email@gmail.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            authError = null
                        },
                        label = { Text("Password (min 6 characters)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        singleLine = true
                    )

                    authError?.let { err ->
                        Text(
                            text = err,
                            color = GeminiAccentRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedEmail = emailInput.trim()
                        val trimmedPass = passwordInput.trim()
                        when {
                            trimmedEmail.isBlank() -> {
                                authError = "Please enter your email address."
                            }
                            !trimmedEmail.contains("@") -> {
                                authError = "Please enter a valid email address."
                            }
                            trimmedPass.length < 6 -> {
                                authError = "Password must be at least 6 characters."
                            }
                            else -> {
                                viewModel.syncManager.signInWithEmail(
                                    email = trimmedEmail,
                                    pass = trimmedPass,
                                    onSuccess = {
                                        showAuthDialog = false
                                        emailInput = ""
                                        passwordInput = ""
                                    },
                                    onError = { authError = it }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiPrimary),
                    modifier = Modifier.testTag("auth_confirm_button")
                ) {
                    Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAuthDialog = false
                    authError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreationSyncCard(
    item: CreationItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true }
            .testTag("creation_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (item.type) {
                            CreationType.IMAGE, CreationType.IMAGE_PRO, CreationType.IMAGE_EDIT -> GeminiPrimary.copy(alpha = 0.15f)
                            CreationType.VIDEO_VEO, CreationType.VIDEO_ANIMATION -> GeminiAccentRose.copy(alpha = 0.15f)
                            CreationType.TTS_AUDIO, CreationType.MUSIC_CLIP, CreationType.MUSIC_PRO, CreationType.TRANSCRIPTION -> GeminiAccentCyan.copy(alpha = 0.15f)
                            else -> GeminiAccentEmerald.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.type) {
                        CreationType.IMAGE, CreationType.IMAGE_PRO, CreationType.IMAGE_EDIT -> Icons.Default.Image
                        CreationType.VIDEO_VEO, CreationType.VIDEO_ANIMATION -> Icons.Default.Movie
                        CreationType.TTS_AUDIO, CreationType.MUSIC_CLIP, CreationType.MUSIC_PRO, CreationType.TRANSCRIPTION -> Icons.Default.MusicNote
                        else -> Icons.Default.Psychology
                    },
                    contentDescription = null,
                    tint = when (item.type) {
                        CreationType.IMAGE, CreationType.IMAGE_PRO, CreationType.IMAGE_EDIT -> GeminiPrimary
                        CreationType.VIDEO_VEO, CreationType.VIDEO_ANIMATION -> GeminiAccentRose
                        CreationType.TTS_AUDIO, CreationType.MUSIC_CLIP, CreationType.MUSIC_PRO, CreationType.TRANSCRIPTION -> GeminiAccentCyan
                        else -> GeminiAccentEmerald
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title.ifEmpty { item.type.name },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Synced",
                        tint = GeminiAccentCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.model} • $dateStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete from Firestore",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text(text = item.title.ifEmpty { item.type.name }, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Model: ${item.model}", style = MaterialTheme.typography.labelMedium)
                    Text(text = "Type: ${item.type.name}", style = MaterialTheme.typography.labelMedium)
                    Text(text = "Date: $dateStr", style = MaterialTheme.typography.labelMedium)
                    if (item.prompt.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Prompt:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = item.prompt, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!item.textResult.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Result:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = item.textResult!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
