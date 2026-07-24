package com.example.screens.event

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.EventEntity
import com.example.utils.ResponsiveUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.models.MediaItemEntity
import com.example.screens.camera.CameraCaptureDialog
import com.example.screens.event.components.AddMediaButtonsSection
import com.example.screens.event.components.GalleryConfirmationDialog
import com.example.screens.event.components.MediaDetailsDialog
import com.example.screens.event.components.MediaQueueSection
import com.example.screens.event.components.MediaSummarySection
import com.example.screens.event.components.MissingRequirementsDialog
import com.example.screens.event.components.PublishStatusBadge
import com.example.screens.event.components.PublishingStatusCard
import com.example.screens.event.components.UploadStatusSection
import com.example.screens.event.components.PackageInformationSection
import com.example.screens.event.components.AIContextSection
import com.example.screens.event.components.validatePublishingReadiness
import com.example.services.GoogleAuthManager
import com.example.models.PUBLISH_STATUS_DRAFT
import com.example.models.PUBLISH_STATUS_READY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    viewModel: EventViewModel,
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (eventId: String) -> Unit,
    onNavigateToStorageWizard: ((workspaceId: String) -> Unit)? = null,
    onNavigateToWorkspaces: (() -> Unit)? = null,
    onNavigateToUploadProgress: ((eventId: String) -> Unit)? = null,
    onNavigateToGeneratedContent: ((eventId: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val scrollState = rememberScrollState()

    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val mediaList by viewModel.getMediaForEvent(eventId).collectAsStateWithLifecycle(initialValue = emptyList())

    val googleAuthManager = remember { GoogleAuthManager.getInstance(context) }
    val isGoogleSignedIn by googleAuthManager.isSignedIn.collectAsStateWithLifecycle()

    val currentEvent = events.find { it.id == eventId }

    var isLoading by remember { mutableStateOf(currentEvent == null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMissingDialog by remember { mutableStateOf(false) }

    // Media Dialog States
    var showCameraDialog by remember { mutableStateOf(false) }
    var pendingGalleryUris by remember { mutableStateOf<List<Uri>?>(null) }
    var selectedMediaItem by remember { mutableStateOf<MediaItemEntity?>(null) }

    val validationReport = remember(currentEvent, activeWorkspace, mediaList.size, isGoogleSignedIn) {
        currentEvent?.let { evt ->
            validatePublishingReadiness(
                event = evt,
                workspace = activeWorkspace,
                mediaCount = mediaList.size,
                isGoogleSignedIn = isGoogleSignedIn
            )
        }
    }

    LaunchedEffect(currentEvent) {
        if (currentEvent != null) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentEvent?.title ?: "Event Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        activeWorkspace?.let { workspace ->
                            Text(
                                text = workspace.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    currentEvent?.let { evt ->
                        IconButton(onClick = { onNavigateToEdit(evt.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Event", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading && currentEvent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (currentEvent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Event not found.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val eventItem = currentEvent!!
            val guests = deserializeGuests(eventItem.guestsJson)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = responsivePadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Card
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = eventItem.type,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PublishStatusBadge(publishStatus = eventItem.publishStatus)
                                EventStatusChip(status = eventItem.status)
                            }
                        }

                        Text(
                            text = eventItem.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!eventItem.description.isNullOrBlank()) {
                            Text(
                                text = eventItem.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Date, Time, Location Metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = eventItem.date,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!eventItem.time.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = eventItem.time,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (!eventItem.location.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = eventItem.location,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Section 2: Leadership Attendance Card
                activeWorkspace?.let { workspace ->
                    DetailSectionCard(
                        title = "Leadership Attendance",
                        icon = Icons.Default.Badge
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AttendanceStatusRow(
                                title = workspace.leader1Title,
                                name = workspace.leader1Name,
                                isPresent = eventItem.leader1Present
                            )

                            if (!workspace.leader2Title.isNullOrBlank() && !workspace.leader2Name.isNullOrBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                AttendanceStatusRow(
                                    title = workspace.leader2Title,
                                    name = workspace.leader2Name,
                                    isPresent = eventItem.leader2Present
                                )
                            }
                        }
                    }
                }

                // Section 3: Additional Guests
                DetailSectionCard(
                    title = "Additional Guests (${guests.size})",
                    icon = Icons.Default.Group
                ) {
                    if (guests.isEmpty()) {
                        Text(
                            text = "No guests recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            guests.forEach { guest ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = guest.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (guest.position.isNotBlank() || guest.organization.isNotBlank()) {
                                            Text(
                                                text = listOf(guest.position, guest.organization).filter { it.isNotBlank() }.joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Phase 7: Publishing Status Card
                validationReport?.let { report ->
                    PublishingStatusCard(
                        event = eventItem,
                        validationReport = report,
                        onPrepareClick = {
                            val currentReport = validatePublishingReadiness(
                                event = eventItem,
                                workspace = activeWorkspace,
                                mediaCount = mediaList.size,
                                isGoogleSignedIn = isGoogleSignedIn
                            )
                            if (currentReport.isReady) {
                                viewModel.updatePublishingStatus(
                                    eventId = eventItem.id,
                                    ready = true,
                                    score = 100,
                                    publishStatus = PUBLISH_STATUS_READY
                                ) {
                                    Toast.makeText(context, "Event is ready for publishing.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                viewModel.updatePublishingStatus(
                                    eventId = eventItem.id,
                                    ready = false,
                                    score = currentReport.score,
                                    publishStatus = PUBLISH_STATUS_DRAFT
                                )
                                showMissingDialog = true
                            }
                        },
                        onPublishClick = {
                            val isUnfinished = eventItem.uploadStarted && (!eventItem.uploadCompleted || eventItem.failedFileCount > 0)
                            if (report.isReady || isUnfinished) {
                                viewModel.startOrResumeUpload(eventItem.id)
                                onNavigateToUploadProgress?.invoke(eventItem.id)
                            } else {
                                showMissingDialog = true
                            }
                        }
                    )
                }

                // Phase 8: Upload Status Section
                UploadStatusSection(
                    event = eventItem,
                    mediaList = mediaList,
                    onOpenUploadProgress = {
                        onNavigateToUploadProgress?.invoke(eventItem.id)
                    }
                )

                // Phase 9: Package Information Section
                PackageInformationSection(
                    event = eventItem,
                    onGeneratePackage = {
                        viewModel.generateEventPackage(eventItem.id) { success ->
                            if (success) {
                                Toast.makeText(context, "Event package generated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Metadata generation failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                // Phase 11: AI Context Debug Section
                AIContextSection(
                    event = eventItem,
                    workspace = activeWorkspace,
                    mediaList = mediaList,
                    onNavigateToGeneratedContent = onNavigateToGeneratedContent
                )

                // Phase 6: Media Summary Section
                MediaSummarySection(event = eventItem)

                // Phase 6: Add Media Action Buttons (Camera, Gallery, Files)
                AddMediaButtonsSection(
                    onCameraClick = { showCameraDialog = true },
                    onGallerySelect = { uris -> pendingGalleryUris = uris },
                    onFilesSelect = { uris ->
                        activeWorkspace?.let { workspace ->
                            viewModel.addMediaFromUris(
                                eventId = eventItem.id,
                                workspaceId = workspace.id,
                                workspaceName = workspace.name,
                                eventTitle = eventItem.title,
                                uris = uris
                            ) { added ->
                                Toast.makeText(context, "${added.size} file(s) added.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                // Phase 6: Media Queue Section
                MediaQueueSection(
                    mediaList = mediaList,
                    onItemClick = { item -> selectedMediaItem = item }
                )

                // Metadata Footer
                val lastModifiedStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(eventItem.lastModified))
                Text(
                    text = "Last Updated: $lastModifiedStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Camera Capture Dialog
    if (showCameraDialog && currentEvent != null && activeWorkspace != null) {
        val eventObj = currentEvent!!
        val workspaceObj = activeWorkspace!!
        CameraCaptureDialog(
            onDismiss = { showCameraDialog = false },
            onMediaCaptured = { file, fileType, mimeType ->
                viewModel.addCapturedMedia(
                    eventId = eventObj.id,
                    workspaceId = workspaceObj.id,
                    workspaceName = workspaceObj.name,
                    eventTitle = eventObj.title,
                    tempFile = file,
                    fileType = fileType,
                    mimeType = mimeType
                ) { addedItem ->
                    showCameraDialog = false
                    Toast.makeText(context, "${addedItem.fileType} captured and added to Event.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Gallery Confirmation Dialog
    if (pendingGalleryUris != null && currentEvent != null && activeWorkspace != null) {
        val uris = pendingGalleryUris!!
        val eventObj = currentEvent!!
        val workspaceObj = activeWorkspace!!
        GalleryConfirmationDialog(
            selectedUris = uris,
            onDismiss = { pendingGalleryUris = null },
            onConfirm = {
                viewModel.addMediaFromUris(
                    eventId = eventObj.id,
                    workspaceId = workspaceObj.id,
                    workspaceName = workspaceObj.name,
                    eventTitle = eventObj.title,
                    uris = uris
                ) { added ->
                    pendingGalleryUris = null
                    Toast.makeText(context, "${added.size} media item(s) imported from Gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Media Details Dialog
    selectedMediaItem?.let { item ->
        MediaDetailsDialog(
            mediaItem = item,
            onDismiss = { selectedMediaItem = null },
            onSave = { updatedItem ->
                viewModel.updateMedia(updatedItem) {
                    selectedMediaItem = null
                    Toast.makeText(context, "Media details updated.", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { itemToDelete ->
                viewModel.deleteMedia(itemToDelete) {
                    selectedMediaItem = null
                    Toast.makeText(context, "Media deleted.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Missing Requirements Dialog
    if (showMissingDialog && validationReport != null && currentEvent != null) {
        MissingRequirementsDialog(
            report = validationReport!!,
            onDismiss = { showMissingDialog = false },
            onActionClick = { itemId ->
                when (itemId) {
                    "workspace" -> {
                        onNavigateToWorkspaces?.invoke()
                    }
                    "storage", "google_account" -> {
                        activeWorkspace?.let { ws ->
                            onNavigateToStorageWizard?.invoke(ws.id)
                        } ?: onNavigateToWorkspaces?.invoke()
                    }
                    "event_info" -> {
                        onNavigateToEdit(currentEvent!!.id)
                    }
                    "media" -> {
                        showCameraDialog = true
                    }
                }
            }
        )
    }

    // Delete Event Confirmation Dialog
    if (showDeleteDialog && currentEvent != null) {
        val targetEvent = currentEvent!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Event?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${targetEvent.title}'?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(targetEvent) {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            content()
        }
    }
}

@Composable
private fun AttendanceStatusRow(
    title: String,
    name: String,
    isPresent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isPresent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isPresent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isPresent) "Present" else "Absent",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isPresent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
