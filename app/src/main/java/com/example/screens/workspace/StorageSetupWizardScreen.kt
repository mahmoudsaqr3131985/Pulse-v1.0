package com.example.screens.workspace

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.*
import com.example.utils.ResponsiveUtils
import com.example.widgets.PulseLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSetupWizardScreen(
    viewModel: WorkspaceViewModel,
    workspaceId: String,
    onNavigateComplete: () -> Unit
) {
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val scrollState = rememberScrollState()

    var workspace by remember { mutableStateOf<WorkspaceEntity?>(null) }
    var currentStep by remember { mutableIntStateOf(1) } // Step 1 = Confirmation, Step 2 = Option Selection
    var selectedStorageType by remember { mutableStateOf(STORAGE_TYPE_GOOGLE_DRIVE) } // GOOGLE_DRIVE or LOCAL_STORAGE
    var driveChoice by remember { mutableStateOf(DRIVE_CHOICE_CREATE_NEW) } // CREATE_NEW, CHOOSE_EXISTING, SKIP
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(workspaceId) {
        val loaded = viewModel.getWorkspaceById(workspaceId)
        workspace = loaded
        if (loaded != null && loaded.storageType != STORAGE_TYPE_UNCONFIGURED) {
            selectedStorageType = loaded.storageType
            if (loaded.driveFolderChoice != null) {
                driveChoice = loaded.driveFolderChoice
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Storage Setup Wizard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateComplete) {
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
        if (isLoading || workspace == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val currentWorkspace = workspace!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = responsivePadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wizard Step Indicator Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepBadge(stepNumber = 1, title = "Welcome", isActive = currentStep == 1, isDone = currentStep > 1)
                    HorizontalDivider(
                        modifier = Modifier
                            .width(40.dp)
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    StepBadge(stepNumber = 2, title = "Storage Type", isActive = currentStep == 2, isDone = false)
                }

                AnimatedContent(
                    targetState = currentStep,
                    label = "WizardStepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1WorkspaceCreatedContent(
                            workspace = currentWorkspace,
                            onNextStep = { currentStep = 2 }
                        )
                        2 -> Step2SelectStorageTypeContent(
                            workspace = currentWorkspace,
                            selectedStorageType = selectedStorageType,
                            onSelectStorageType = { selectedStorageType = it },
                            driveChoice = driveChoice,
                            onSelectDriveChoice = { driveChoice = it },
                            viewModel = viewModel,
                            onCompleteSetup = { email, choice, folderId, folderName ->
                                if (selectedStorageType == STORAGE_TYPE_GOOGLE_DRIVE) {
                                    viewModel.connectGoogleDrive(
                                        workspaceId = currentWorkspace.id,
                                        googleEmail = email,
                                        driveChoice = choice,
                                        customFolderId = folderId,
                                        customFolderName = folderName,
                                        onComplete = onNavigateComplete
                                    )
                                } else {
                                    val localFolder = "Pulse/${currentWorkspace.name}"
                                    viewModel.updateStorageConfig(
                                        workspaceId = currentWorkspace.id,
                                        storageType = STORAGE_TYPE_LOCAL_STORAGE,
                                        storageStatus = STORAGE_STATUS_READY,
                                        localFolderName = localFolder,
                                        onComplete = onNavigateComplete
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isDone: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive || isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isActive || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Step1WorkspaceCreatedContent(
    workspace: WorkspaceEntity,
    onNextStep: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                    ),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = "Workspace Created Successfully!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = workspace.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${workspace.type} • ${workspace.leader1Title}: ${workspace.leader1Name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Pulse needs a storage location for this Workspace to safely store media assets, documents, press releases, and event archives.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Action Button
        Button(
            onClick = onNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Configure Storage Location",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun Step2SelectStorageTypeContent(
    workspace: WorkspaceEntity,
    selectedStorageType: String,
    onSelectStorageType: (String) -> Unit,
    driveChoice: String,
    onSelectDriveChoice: (String) -> Unit,
    viewModel: WorkspaceViewModel,
    onCompleteSetup: (email: String, choice: String, folderId: String?, folderName: String?) -> Unit
) {
    val authManager = viewModel.authManager
    val driveManager = viewModel.driveManager
    val coroutineScope = rememberCoroutineScope()

    val isSignedIn by authManager.isSignedIn.collectAsStateWithLifecycle()
    val savedEmail by authManager.userEmail.collectAsStateWithLifecycle()

    var inputEmail by remember { mutableStateOf(workspace.googleAccountEmail ?: savedEmail ?: "mahmoudsaqr3131985@gmail.com") }
    var isAuthenticating by remember { mutableStateOf(false) }

    var existingFolders by remember { mutableStateOf<List<com.example.services.DriveFolderItem>>(emptyList()) }
    var selectedExistingFolder by remember { mutableStateOf<com.example.services.DriveFolderItem?>(null) }
    var customFolderNameInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val listResult = driveManager.listExistingDriveFolders()
        if (listResult.isSuccess) {
            val list = listResult.getOrDefault(emptyList())
            existingFolders = list
            if (list.isNotEmpty()) {
                selectedExistingFolder = list.first()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Where would you like to store this Workspace files?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose your preferred primary storage engine for '${workspace.name}'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // OPTION 1: Google Drive
        StorageOptionCard(
            title = "Google Drive",
            description = "Store media and generated content safely in your Google Drive.",
            icon = Icons.Default.Cloud,
            isSelected = selectedStorageType == STORAGE_TYPE_GOOGLE_DRIVE,
            onClick = { onSelectStorageType(STORAGE_TYPE_GOOGLE_DRIVE) }
        )

        // Sub-options if Google Drive selected
        AnimatedVisibility(
            visible = selectedStorageType == STORAGE_TYPE_GOOGLE_DRIVE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Google Drive Authentication & Authorization",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Email Input / Authorization Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text("Google Account Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isSignedIn) "Authenticated & Authorized" else "OAuth Scope Ready",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isSignedIn) {
                                    FilledTonalButton(
                                        onClick = {
                                            isAuthenticating = true
                                            coroutineScope.launch {
                                                authManager.signInWithGoogle(inputEmail)
                                                isAuthenticating = false
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isAuthenticating) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Authorize Scope", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text(
                        text = "Google Drive Folder Options",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Sub Option 1: Create a new folder
                    DriveSubOptionRow(
                        title = "Create a new folder (Pulse/${workspace.name}/)",
                        icon = Icons.Default.CreateNewFolder,
                        isSelected = driveChoice == DRIVE_CHOICE_CREATE_NEW,
                        onClick = { onSelectDriveChoice(DRIVE_CHOICE_CREATE_NEW) }
                    )

                    if (driveChoice == DRIVE_CHOICE_CREATE_NEW) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Will automatically create:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "📁 Pulse / ${workspace.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Sub Option 2: Choose an existing folder
                    DriveSubOptionRow(
                        title = "Choose an existing folder",
                        icon = Icons.Default.FolderOpen,
                        isSelected = driveChoice == DRIVE_CHOICE_CHOOSE_EXISTING,
                        onClick = { onSelectDriveChoice(DRIVE_CHOICE_CHOOSE_EXISTING) }
                    )

                    if (driveChoice == DRIVE_CHOICE_CHOOSE_EXISTING) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select from Google Drive:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                existingFolders.forEach { folderItem ->
                                    val isFolderSelected = selectedExistingFolder?.folderId == folderItem.folderId
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFolderSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedExistingFolder = folderItem }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = folderItem.folderName,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = folderItem.parentPath,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isFolderSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customFolderNameInput,
                                    onValueChange = { customFolderNameInput = it },
                                    label = { Text("Or enter custom Drive folder path/name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Sub Option 3: Skip for now
                    DriveSubOptionRow(
                        title = "Skip for now",
                        icon = Icons.Default.SkipNext,
                        isSelected = driveChoice == DRIVE_CHOICE_SKIP,
                        onClick = { onSelectDriveChoice(DRIVE_CHOICE_SKIP) }
                    )
                }
            }
        }

        // OPTION 2: Local Storage
        StorageOptionCard(
            title = "Local Storage",
            description = "Store everything only on this Android device.",
            icon = Icons.Default.Phonelink,
            isSelected = selectedStorageType == STORAGE_TYPE_LOCAL_STORAGE,
            onClick = { onSelectStorageType(STORAGE_TYPE_LOCAL_STORAGE) }
        )

        // Information banner if Local Storage selected
        AnimatedVisibility(
            visible = selectedStorageType == STORAGE_TYPE_LOCAL_STORAGE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Local Storage Prepared",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = "Your files will be stored locally on this device.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Dedicated Folder Path: Pulse/${workspace.name}/",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Complete Button
        Button(
            onClick = {
                val fId = if (customFolderNameInput.isNotBlank()) "hdrv_custom_${customFolderNameInput.hashCode()}" else selectedExistingFolder?.folderId
                val fName = if (customFolderNameInput.isNotBlank()) customFolderNameInput else selectedExistingFolder?.folderName
                onCompleteSetup(inputEmail, driveChoice, fId, fName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Storage Configuration",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun StorageOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DriveSubOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
