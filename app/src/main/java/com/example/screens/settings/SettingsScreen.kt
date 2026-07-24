package com.example.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppConstants
import com.example.models.*
import com.example.screens.workspace.WorkspaceViewModel
import com.example.utils.ResponsiveUtils
import com.example.widgets.PhaseBadge
import com.example.widgets.PulseLogo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStorageWizard: (workspaceId: String) -> Unit
) {
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val scrollState = rememberScrollState()

    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val currentTheme by PreferencesManager.themeFlow.collectAsStateWithLifecycle()
    val currentLanguage by PreferencesManager.languageFlow.collectAsStateWithLifecycle()
    var showDisconnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Storage",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = responsivePadding, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PulseLogo(size = 64.dp, animated = false)

                    Text(
                        text = AppConstants.APP_NAME,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    PhaseBadge()
                }
            }

            // Application Preferences Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.app_preferences),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Theme Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.theme),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val themeOptions = listOf(
                                PreferencesManager.THEME_SYSTEM to stringResource(id = R.string.theme_system),
                                PreferencesManager.THEME_LIGHT to stringResource(id = R.string.theme_light),
                                PreferencesManager.THEME_DARK to stringResource(id = R.string.theme_dark)
                            )
                            themeOptions.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = currentTheme == key,
                                    onClick = { PreferencesManager.setTheme(key) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Language Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val langOptions = listOf(
                                PreferencesManager.LANG_SYSTEM to stringResource(id = R.string.language_system),
                                PreferencesManager.LANG_ARABIC to stringResource(id = R.string.language_arabic),
                                PreferencesManager.LANG_ENGLISH to stringResource(id = R.string.language_english)
                            )
                            langOptions.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = currentLanguage == key,
                                    onClick = { PreferencesManager.setLanguage(key) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = langOptions.size)
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Storage Configuration Card for Active Workspace
            activeWorkspace?.let { workspace ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Workspace Storage",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = workspace.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status Chip
                            val isConnected = workspace.storageType == STORAGE_TYPE_GOOGLE_DRIVE && workspace.storageStatus == STORAGE_STATUS_CONNECTED
                            val isLocal = workspace.storageType == STORAGE_TYPE_LOCAL_STORAGE
                            val chipColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                                           else if (isLocal) MaterialTheme.colorScheme.secondaryContainer
                                           else MaterialTheme.colorScheme.errorContainer

                            val textColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else if (isLocal) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onErrorContainer

                            val statusText = if (isConnected) "Connected"
                                             else if (isLocal) "Local Storage"
                                             else "Not Configured"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textColor
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Storage Type
                        SettingMetadataItem(
                            label = "Storage Type",
                            value = when (workspace.storageType) {
                                STORAGE_TYPE_GOOGLE_DRIVE -> "Google Drive"
                                STORAGE_TYPE_LOCAL_STORAGE -> "Local Storage"
                                else -> "Not Configured"
                            },
                            icon = Icons.Default.Cloud
                        )

                        // Google Account Email (if Google Drive)
                        if (workspace.storageType == STORAGE_TYPE_GOOGLE_DRIVE) {
                            SettingMetadataItem(
                                label = "Google Account",
                                value = workspace.googleAccountEmail ?: "mahmoudsaqr3131985@gmail.com",
                                icon = Icons.Default.Person
                            )

                            SettingMetadataItem(
                                label = "Drive Folder",
                                value = workspace.driveFolderName ?: "Pulse/${workspace.name}",
                                icon = Icons.Default.Folder
                            )

                            workspace.lastConnectionTime?.let { timeMs ->
                                val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(timeMs))
                                SettingMetadataItem(
                                    label = "Last Connection",
                                    value = dateStr,
                                    icon = Icons.Default.Storage
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onNavigateToStorageWizard(workspace.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Change Folder",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Folder", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = { showDisconnectDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = "Disconnect Drive",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onNavigateToStorageWizard(workspace.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Connect Google Drive",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Configure Google Drive Storage")
                            }
                        }
                    }
                }
            }

            // AI Status Section Card for Active Workspace
            activeWorkspace?.let { workspace ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AI Status",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = workspace.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // AI Connection Status Chip
                            val isConnected = workspace.aiConnectionStatus == AI_STATUS_CONNECTED
                            val isNotConfigured = workspace.aiProvider == AI_PROVIDER_NONE || workspace.aiConnectionStatus == AI_STATUS_NOT_CONFIGURED
                            val chipColor = when {
                                isConnected -> MaterialTheme.colorScheme.primaryContainer
                                isNotConfigured -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                            val textColor = when {
                                isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isNotConfigured -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = workspace.aiConnectionStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textColor
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingMetadataItem(
                            label = "Current Provider",
                            value = workspace.aiProvider,
                            icon = Icons.Default.Psychology
                        )

                        SettingMetadataItem(
                            label = "Current Model",
                            value = workspace.aiModel,
                            icon = Icons.Default.AutoAwesome
                        )

                        SettingMetadataItem(
                            label = "Connection Status",
                            value = workspace.aiConnectionStatus,
                            icon = Icons.Default.CheckCircle
                        )

                        val lastValText = if (workspace.aiLastValidationTime > 0) {
                            SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(workspace.aiLastValidationTime))
                        } else {
                            "Never"
                        }

                        SettingMetadataItem(
                            label = "Last Validation",
                            value = lastValText,
                            icon = Icons.Default.Storage
                        )
                    }
                }
            }

            // Info Details Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Application Metadata",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingMetadataItem(
                        label = "Application Name",
                        value = AppConstants.APP_NAME,
                        icon = Icons.Default.Settings
                    )

                    SettingMetadataItem(
                        label = "Version",
                        value = AppConstants.VERSION_NAME,
                        icon = Icons.Default.Info
                    )

                    SettingMetadataItem(
                        label = "Project Codename",
                        value = AppConstants.PROJECT_CODENAME,
                        icon = Icons.Default.Code
                    )
                }
            }
        }
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectDialog && activeWorkspace != null) {
        val workspace = activeWorkspace!!
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(
                    text = "Disconnect Google Drive?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This will remove Google Drive credentials and folder links for '${workspace.name}'. The workspace storage status will return to Not Configured.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disconnectGoogleDrive(workspace.id) {
                            showDisconnectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingMetadataItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
