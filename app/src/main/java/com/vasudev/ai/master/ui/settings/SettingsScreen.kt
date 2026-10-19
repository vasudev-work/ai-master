package com.vasudev.ai.master.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vasudev.ai.master.R
import com.vasudev.ai.master.data.ondevice.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val model by viewModel.model.collectAsStateWithLifecycle()
    val searchKey by viewModel.searchKey.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val styleExamples by viewModel.styleExamples.collectAsStateWithLifecycle()
    val modelReady by viewModel.modelReady.collectAsStateWithLifecycle()
    val modelSizeMb by viewModel.modelSizeMb.collectAsStateWithLifecycle()
    val downloadState by viewModel.download.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importModel(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(text = "Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Back"
                    )
                }
            }
        )

        Text(
            text = "AI Provider (OpenAI-compatible)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.apiKey.value = it },
            label = { Text("API key (stored encrypted on-device)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { viewModel.baseUrl.value = it },
            label = { Text("Base URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            value = model,
            onValueChange = { viewModel.model.value = it },
            label = { Text("Model name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Web Search Grounding",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = searchKey,
            onValueChange = { viewModel.searchKey.value = it },
            label = { Text("Brave Search API key (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Always search the web for answers", modifier = Modifier.weight(1f))
            Switch(checked = settings.grounded, onCheckedChange = viewModel::setGrounded)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Conversation Language",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = languageName(settings.languageTag),
                onValueChange = {},
                readOnly = true,
                label = { Text("Reply in") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                LANGUAGES.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(languageName(tag)) },
                        onClick = {
                            viewModel.setLanguage(tag)
                            expanded = false
                        }
                    )
                }
            }
        }
        Text(
            text = "Hinglish code-mixing is handled automatically: write however you like.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Private Mode (on-device only, no network)", modifier = Modifier.weight(1f))
            Switch(checked = settings.privateMode, onCheckedChange = viewModel::setPrivate)
        }

        // ---------- On-device story mode ----------
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "On-device story mode (offline)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Write stories on this phone, no internet", modifier = Modifier.weight(1f))
            Switch(checked = settings.onDeviceMode, onCheckedChange = viewModel::setOnDevice)
        }
        Text(
            text = if (modelReady) "Model ready on device ($modelSizeMb MB)"
            else "Model not downloaded yet (~555 MB, one-time)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (!modelReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::downloadModel,
                    enabled = downloadState !is DownloadState.Progress
                ) { Text("Download model") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Text("Import .task file")
                }
            }
            (downloadState as? DownloadState.Progress)?.let { p ->
                LinearProgressIndicator(
                    progress = { p.fraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedButton(onClick = viewModel::deleteModel) { Text("Delete model") }
            }
        }
        (downloadState as? DownloadState.Failed)?.let { f ->
            Text(
                text = "Download failed: ${f.message}. Try again, or import a .task file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ---------- Story style / examples ----------
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Story style / examples",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = styleExamples,
            onValueChange = { viewModel.styleExamples.value = it },
            label = { Text("Paste 2-3 example stories here") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            minLines = 4,
            maxLines = 12
        )
        Text(
            text = "Saved on this device. New stories will follow this style. Tap Save to keep it.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = {
                viewModel.saveAll()
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                onBack()
            }) {
                Text("Save")
            }
            TextButton(onClick = viewModel::clearHistory) {
                Text("Delete all chats")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private val LANGUAGES = listOf(
    "en-IN", "hi-IN", "ta-IN", "te-IN", "mr-IN", "bn-IN"
)

private fun languageName(tag: String): String = when (tag) {
    "hi-IN" -> "Hindi (हिंदी)"
    "ta-IN" -> "Tamil (தமிழ்)"
    "te-IN" -> "Telugu (తెలుగు)"
    "mr-IN" -> "Marathi (मराठी)"
    "bn-IN" -> "Bengali (বাংলা)"
    else -> "English (India)"
}
