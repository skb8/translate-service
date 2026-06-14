package com.skb8.translateservice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skb8.translateservice.R
import com.skb8.translateservice.data.TranslationLog
import com.skb8.translateservice.data.TranslationStatus
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    logs: List<TranslationLog>,
    availableLanguages: List<LanguageOption>,
    defaultTargetLanguage: String,
    onTargetLanguageSelected: (String) -> Unit,
    serviceRunning: Boolean,
    installedModels: List<String>,
    onRefreshModels: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    LanguagePicker(
                        availableLanguages = availableLanguages,
                        selectedCode = defaultTargetLanguage,
                        onSelected = onTargetLanguageSelected
                    )
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                StatusCard(serviceRunning, installedModels, onRefreshModels)
                EmptyState(Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    StatusCard(serviceRunning, installedModels, onRefreshModels)
                }
                items(logs, key = { it.id }) { log ->
                    LogCard(log)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusCard(
    serviceRunning: Boolean,
    installedModels: List<String>,
    onRefreshModels: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(if (serviceRunning) R.string.service_running else R.string.service_not_running),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (installedModels.isEmpty()) {
                        stringResource(R.string.ml_models_none)
                    } else {
                        stringResource(R.string.ml_models_installed, installedModels.joinToString(", "))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefreshModels) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.empty_log_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.empty_log_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogCard(log: TranslationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.callerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(log.status)
            }

            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(log.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = log.sourceText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (log.status != TranslationStatus.INFO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${log.sourceLanguage} → ${log.targetLanguage}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (log.status == TranslationStatus.OK) {
                Text(
                    text = log.translatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else if (log.status != TranslationStatus.INFO && log.errorMessage != null) {
                Text(
                    text = log.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(status: TranslationStatus) {
    val (textRes, color) = when (status) {
        TranslationStatus.OK -> R.string.status_ok to MaterialTheme.colorScheme.primaryContainer
        TranslationStatus.PENDING_MODEL -> R.string.status_pending_model to MaterialTheme.colorScheme.secondaryContainer
        TranslationStatus.ERROR -> R.string.status_error to MaterialTheme.colorScheme.errorContainer
        TranslationStatus.INFO -> R.string.status_info to MaterialTheme.colorScheme.secondaryContainer
    }
    SuggestionChip(
        onClick = {},
        label = { Text(stringResource(textRes)) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker(
    availableLanguages: List<LanguageOption>,
    selectedCode: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = availableLanguages.firstOrNull { it.code == selectedCode }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = selected?.displayName ?: selectedCode,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.default_target_language))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableLanguages.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSelected(option.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
