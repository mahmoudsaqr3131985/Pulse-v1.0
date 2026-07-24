package com.example.screens.event.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.EventEntity
import com.example.models.PUBLISH_STATUS_READY
import com.example.models.STORAGE_STATUS_CONNECTED
import com.example.models.STORAGE_STATUS_READY
import com.example.models.STORAGE_TYPE_GOOGLE_DRIVE
import com.example.models.STORAGE_TYPE_LOCAL_STORAGE
import com.example.models.STORAGE_TYPE_UNCONFIGURED
import com.example.models.WorkspaceEntity

enum class ChecklistStatus {
    READY,
    WARNING,
    MISSING,
    NOT_REQUIRED
}

data class ValidationItem(
    val id: String,
    val title: String,
    val description: String,
    val status: ChecklistStatus,
    val missingReason: String,
    val actionLabel: String? = null
)

data class ValidationReport(
    val items: List<ValidationItem>,
    val score: Int, // 0 to 100
    val isReady: Boolean
)

fun validatePublishingReadiness(
    event: EventEntity,
    workspace: WorkspaceEntity?,
    mediaCount: Int,
    isGoogleSignedIn: Boolean
): ValidationReport {
    val items = mutableListOf<ValidationItem>()

    // 1. Workspace
    if (workspace != null) {
        items.add(
            ValidationItem(
                id = "workspace",
                title = "Workspace",
                description = "Active: ${workspace.name}",
                status = ChecklistStatus.READY,
                missingReason = "No active workspace selected.",
                actionLabel = "Select Workspace"
            )
        )
    } else {
        items.add(
            ValidationItem(
                id = "workspace",
                title = "Workspace",
                description = "No active workspace",
                status = ChecklistStatus.MISSING,
                missingReason = "An active workspace must be selected.",
                actionLabel = "Select Workspace"
            )
        )
    }

    // 2. Storage
    if (workspace == null) {
        items.add(
            ValidationItem(
                id = "storage",
                title = "Storage",
                description = "Workspace required first",
                status = ChecklistStatus.MISSING,
                missingReason = "Storage is not configured.",
                actionLabel = "Configure Storage"
            )
        )
    } else {
        when (workspace.storageType) {
            STORAGE_TYPE_GOOGLE_DRIVE -> {
                val isFolderReady = workspace.storageStatus == STORAGE_STATUS_READY ||
                        workspace.storageStatus == STORAGE_STATUS_CONNECTED ||
                        !workspace.driveFolderId.isNullOrBlank() ||
                        !workspace.driveFolderName.isNullOrBlank()
                if (isFolderReady) {
                    items.add(
                        ValidationItem(
                            id = "storage",
                            title = "Storage",
                            description = "Google Drive: ${workspace.driveFolderName ?: "Connected"}",
                            status = ChecklistStatus.READY,
                            missingReason = "Google Drive folder not configured.",
                            actionLabel = "Configure Storage"
                        )
                    )
                } else {
                    items.add(
                        ValidationItem(
                            id = "storage",
                            title = "Storage",
                            description = "Google Drive folder missing",
                            status = ChecklistStatus.MISSING,
                            missingReason = "Google Drive storage folder is not configured.",
                            actionLabel = "Configure Storage"
                        )
                    )
                }
            }
            STORAGE_TYPE_LOCAL_STORAGE -> {
                val isLocalReady = workspace.storageStatus == STORAGE_STATUS_READY ||
                        workspace.storageStatus == STORAGE_STATUS_CONNECTED ||
                        !workspace.localFolderName.isNullOrBlank()
                if (isLocalReady) {
                    items.add(
                        ValidationItem(
                            id = "storage",
                            title = "Storage",
                            description = "Local Storage: ${workspace.localFolderName ?: "Configured"}",
                            status = ChecklistStatus.READY,
                            missingReason = "Local storage folder not configured.",
                            actionLabel = "Configure Storage"
                        )
                    )
                } else {
                    items.add(
                        ValidationItem(
                            id = "storage",
                            title = "Storage",
                            description = "Local folder missing",
                            status = ChecklistStatus.MISSING,
                            missingReason = "Local storage folder is not configured.",
                            actionLabel = "Configure Storage"
                        )
                    )
                }
            }
            else -> {
                items.add(
                    ValidationItem(
                        id = "storage",
                        title = "Storage",
                        description = "Storage not configured",
                        status = ChecklistStatus.MISSING,
                        missingReason = "Storage mode must be configured (Google Drive or Local).",
                        actionLabel = "Configure Storage"
                    )
                )
            }
        }
    }

    // 3. Event Information
    val missingFields = mutableListOf<String>()
    if (event.title.isBlank()) missingFields.add("Title")
    if (event.type.isBlank()) missingFields.add("Type")
    if (event.date.isBlank()) missingFields.add("Date")

    if (missingFields.isEmpty()) {
        items.add(
            ValidationItem(
                id = "event_info",
                title = "Event Information",
                description = "${event.title} (${event.type}, ${event.date})",
                status = ChecklistStatus.READY,
                missingReason = "Event information complete.",
                actionLabel = "Edit Event"
            )
        )
    } else {
        items.add(
            ValidationItem(
                id = "event_info",
                title = "Event Information",
                description = "Missing: ${missingFields.joinToString(", ")}",
                status = ChecklistStatus.MISSING,
                missingReason = "Event details missing: ${missingFields.joinToString(", ")}",
                actionLabel = "Edit Event"
            )
        )
    }

    // 4. Media
    val totalMedia = mediaCount.coerceAtLeast(event.photoCount + event.videoCount + event.documentCount + event.audioCount)
    if (totalMedia > 0) {
        items.add(
            ValidationItem(
                id = "media",
                title = "Media",
                description = "$totalMedia item(s) attached",
                status = ChecklistStatus.READY,
                missingReason = "No media files attached.",
                actionLabel = "Add Media"
            )
        )
    } else {
        items.add(
            ValidationItem(
                id = "media",
                title = "Media",
                description = "No media files added",
                status = ChecklistStatus.MISSING,
                missingReason = "At least one media item (Photo, Video, Document, Audio) is required.",
                actionLabel = "Add Media"
            )
        )
    }

    // 5. Google Account
    if (workspace?.storageType == STORAGE_TYPE_LOCAL_STORAGE) {
        items.add(
            ValidationItem(
                id = "google_account",
                title = "Google Account",
                description = "Not Required for Local Storage",
                status = ChecklistStatus.NOT_REQUIRED,
                missingReason = "Not required.",
                actionLabel = null
            )
        )
    } else {
        val accountConnected = isGoogleSignedIn || !workspace?.googleAccountEmail.isNullOrBlank()
        if (accountConnected) {
            val emailStr = workspace?.googleAccountEmail ?: "Connected"
            items.add(
                ValidationItem(
                    id = "google_account",
                    title = "Google Account",
                    description = "Account: $emailStr",
                    status = ChecklistStatus.READY,
                    missingReason = "Google Account not authenticated.",
                    actionLabel = "Sign In"
                )
            )
        } else {
            items.add(
                ValidationItem(
                    id = "google_account",
                    title = "Google Account",
                    description = "Not authenticated",
                    status = ChecklistStatus.MISSING,
                    missingReason = "Google Drive workspace requires an authenticated Google Account.",
                    actionLabel = "Sign In"
                )
            )
        }
    }

    // Score Calculation: 5 items, each 20%
    val readyCount = items.count { it.status == ChecklistStatus.READY || it.status == ChecklistStatus.NOT_REQUIRED }
    val score = readyCount * 20
    val isReady = readyCount == 5

    return ValidationReport(
        items = items,
        score = score,
        isReady = isReady
    )
}

@Composable
fun PublishingStatusCard(
    event: EventEntity,
    validationReport: ValidationReport,
    onPrepareClick: () -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Publish,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Publishing Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Readiness Percentage Badge
                val badgeBgColor = if (validationReport.isReady) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
                val badgeTextColor = if (validationReport.isReady) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBgColor
                ) {
                    Text(
                        text = "${validationReport.score}% Ready",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Readiness Score Progress Indicator
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Publishing Readiness",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${validationReport.score}/100",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { validationReport.score / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = if (validationReport.isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Checklist Items
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                validationReport.items.forEach { item ->
                    ChecklistItemRow(item = item)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Prepare for Publishing Button
                Button(
                    onClick = onPrepareClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Prepare for Publishing",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Publish or Resume Upload Button
                val isUnfinishedUpload = event.uploadStarted && (!event.uploadCompleted || event.failedFileCount > 0)
                val isPublishEnabled = event.publishingReady || validationReport.isReady || isUnfinishedUpload
                
                Button(
                    onClick = onPublishClick,
                    enabled = isPublishEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnfinishedUpload) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(
                        imageVector = if (isUnfinishedUpload) Icons.Default.PlayArrow else Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            event.uploadCompleted -> "Re-Publish / Sync Drive"
                            isUnfinishedUpload -> "Resume Upload"
                            else -> "Publish"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(item: ValidationItem) {
    val (icon, tint, bgTint) = when (item.status) {
        ChecklistStatus.READY -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF2E7D32), // Green
            Color(0xFFE8F5E9)
        )
        ChecklistStatus.WARNING -> Triple(
            Icons.Default.Warning,
            Color(0xFFED6C02), // Orange
            Color(0xFFFFF4E5)
        )
        ChecklistStatus.MISSING -> Triple(
            Icons.Default.Cancel,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
        ChecklistStatus.NOT_REQUIRED -> Triple(
            Icons.Default.CheckCircleOutline,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.status.name,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = bgTint
            ) {
                Text(
                    text = when (item.status) {
                        ChecklistStatus.READY -> "Ready"
                        ChecklistStatus.WARNING -> "Warning"
                        ChecklistStatus.MISSING -> "Missing"
                        ChecklistStatus.NOT_REQUIRED -> "Not Required"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tint,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingRequirementsDialog(
    report: ValidationReport,
    onDismiss: () -> Unit,
    onActionClick: (itemId: String) -> Unit
) {
    val missingItems = remember(report) {
        report.items.filter { it.status == ChecklistStatus.MISSING || it.status == ChecklistStatus.WARNING }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Missing Requirements",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "The following items must be resolved before this event can be published:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    missingItems.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onActionClick(item.id)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = item.missingReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                item.actionLabel?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
fun PublishStatusBadge(
    publishStatus: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (publishStatus) {
        com.example.models.PUBLISH_STATUS_PUBLISHED -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Published"
        )
        PUBLISH_STATUS_READY -> Triple(
            Color(0xFFE0F2FE),
            Color(0xFF0284C7),
            "Ready"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Draft"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor
            )
        }
    }
}
