package com.vasudev.ai.master.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.vasudev.ai.master.R
import com.vasudev.ai.master.data.ImageVault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showCameraDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.onImage(ImageVault.saveToVault(context, it)) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImage(ImageVault.copyToVault(context, it)) }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoice()
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "myAi", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = viewModel::toggleTts) {
                        Icon(
                            painter = painterResource(
                                if (ui.ttsEnabled) R.drawable.ic_volume else R.drawable.ic_volume_off
                            ),
                            contentDescription = "Read answers aloud"
                        )
                    }
                    IconButton(onClick = viewModel::clearChat) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Clear chat"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = "About"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = ui.pendingImageUri != null) {
                    ui.pendingImageUri?.let { uri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attached image",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Text(
                                text = "Smart Lens: text will be extracted on-device",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.onImage(null) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_stop),
                                    contentDescription = "Remove image"
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.onInput("Translate to Hindi: ") },
                        label = { Text(stringResource(R.string.chip_translate)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_translate),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = { viewModel.onInput("Summarize this in 5 bullet points: ") },
                        label = { Text(stringResource(R.string.chip_summarize)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_summarize),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = { viewModel.send(forceWebSearch = true) },
                        label = { Text(stringResource(R.string.chip_web)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_web),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                InputBar(
                    value = ui.input,
                    isListening = ui.isListening,
                    isStreaming = ui.streamingId != null,
                    onValueChange = viewModel::onInput,
                    onSend = { viewModel.send() },
                    onStop = {
                        if (ui.isListening) viewModel.startVoice() else viewModel.stopStream()
                    },
                    onMic = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.startVoice() else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onCamera = { showCameraDialog = true }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(padding)
        ) {
            ModelStatusBadge(ui.tier)
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Namaste! Ask me anything - text, voice or photo.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isStreaming = message.id == ui.streamingId
                        )
                    }
                    if (ui.isListening && ui.partialSpeech.isNotEmpty()) {
                        item {
                            Text(
                                text = ui.partialSpeech,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCameraDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDialog = false },
            title = { Text("Smart Lens") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = {
                        showCameraDialog = false
                        cameraLauncher.launch(null)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera")
                    }
                    FilledTonalButton(onClick = {
                        showCameraDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_web),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCameraDialog = false }) { Text("Cancel") }
            }
        )
    }

    ui.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Hmm") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ModelStatusBadge(tier: String) {
    val container = when (tier) {
        "ON-DEVICE" -> Color(0xFF4E342E)
        "OFFLINE" -> Color(0xFF7B1FA2)
        "ONLINE · WEB" -> Color(0xFF0D47A1)
        else -> Color(0xFF1B5E20)
    }
    val content = when (tier) {
        "ON-DEVICE" -> Color(0xFFFFD8B5)
        "OFFLINE" -> Color(0xFFE1BEE7)
        "ONLINE · WEB" -> Color(0xFFB3E5FC)
        else -> Color(0xFFB9F6CA)
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        tonalElevation = 2.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(content)
            )
            Text(text = tier, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MessageBubble(message: com.vasudev.ai.master.core.ChatMessage, isStreaming: Boolean) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = message.content + if (isStreaming) " ..." else "",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!isUser && message.tier.isNotEmpty() && !isStreaming) {
                    Text(
                        text = message.tier,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    isListening: Boolean,
    isStreaming: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit,
    onCamera: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCamera) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Smart Lens"
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.hint_ask)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            when {
                isListening -> IconButton(onClick = onStop) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stop),
                        contentDescription = "Stop listening",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                isStreaming -> IconButton(onClick = onStop) {
                    Icon(
                        painter = painterResource(R.drawable.ic_stop_circle),
                        contentDescription = "Stop generating",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    IconButton(onClick = onMic) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = "Speak"
                        )
                    }
                    IconButton(onClick = onSend, enabled = value.isNotBlank()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
