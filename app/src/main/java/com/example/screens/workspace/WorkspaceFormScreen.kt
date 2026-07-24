package com.example.screens.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.*
import com.example.utils.ResponsiveUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceFormScreen(
    viewModel: WorkspaceViewModel,
    workspaceId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToStorageWizard: (String) -> Unit
) {
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WORKSPACE_TYPES.first()) }
    var leader1Title by remember { mutableStateOf("") }
    var leader1Name by remember { mutableStateOf("") }
    var leader2Title by remember { mutableStateOf("") }
    var leader2Name by remember { mutableStateOf("") }
    var defaultHashtags by remember { mutableStateOf("") }

    // Phase 10: AI Settings State
    var aiProvider by remember { mutableStateOf(AI_PROVIDER_NONE) }
    var aiApiKey by remember { mutableStateOf("") }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var aiModel by remember { mutableStateOf(AI_MODEL_NONE) }
    var aiTemperature by remember { mutableStateOf(0.4f) }
    var aiMaxTokens by remember { mutableStateOf(2048f) }
    var aiLanguage by remember { mutableStateOf(AI_LANGUAGE_AUTO) }
    var aiConnectionStatus by remember { mutableStateOf(AI_STATUS_NOT_CONFIGURED) }
    var aiLastValidationTime by remember { mutableStateOf(0L) }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }
    var isValidatingConnection by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    var showNameError by remember { mutableStateOf(false) }
    var showLeader1TitleError by remember { mutableStateOf(false) }
    var showLeader1NameError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val isEditing = !workspaceId.isNullOrEmpty()

    LaunchedEffect(workspaceId) {
        if (!workspaceId.isNullOrEmpty()) {
            val existing = viewModel.getWorkspaceById(workspaceId)
            if (existing != null) {
                name = existing.name
                type = existing.type
                leader1Title = existing.leader1Title
                leader1Name = existing.leader1Name
                leader2Title = existing.leader2Title ?: ""
                leader2Name = existing.leader2Name ?: ""
                defaultHashtags = existing.defaultHashtags
                aiProvider = existing.aiProvider
                aiApiKey = viewModel.getDecryptedApiKey(existing.aiApiKey)
                aiModel = existing.aiModel
                aiTemperature = existing.aiTemperature
                aiMaxTokens = existing.aiMaxTokens.toFloat()
                aiLanguage = existing.aiLanguage
                aiConnectionStatus = existing.aiConnectionStatus
                aiLastValidationTime = existing.aiLastValidationTime
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Workspace" else "Create Workspace",
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = responsivePadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Institutional Information",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Workspace Name (Required)
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) showNameError = false
                            },
                            label = { Text("Workspace Name *") },
                            placeholder = { Text("e.g. Faculty of Veterinary Medicine") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            isError = showNameError,
                            supportingText = if (showNameError) {
                                { Text("Workspace Name is required") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 2. Workspace Type (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = type,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Workspace Type *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                WORKSPACE_TYPES.forEach { selectedType ->
                                    DropdownMenuItem(
                                        text = { Text(selectedType) },
                                        onClick = {
                                            type = selectedType
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "Leadership Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 3. Leader 1 Title (Required)
                        OutlinedTextField(
                            value = leader1Title,
                            onValueChange = {
                                leader1Title = it
                                if (it.isNotBlank()) showLeader1TitleError = false
                            },
                            label = { Text("Leader 1 Title *") },
                            placeholder = { Text("e.g. Dean, Director, CEO, President") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            isError = showLeader1TitleError,
                            supportingText = if (showLeader1TitleError) {
                                { Text("Leader 1 Title is required") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4. Leader 1 Name (Required)
                        OutlinedTextField(
                            value = leader1Name,
                            onValueChange = {
                                leader1Name = it
                                if (it.isNotBlank()) showLeader1NameError = false
                            },
                            label = { Text("Leader 1 Name *") },
                            placeholder = { Text("e.g. Prof. Dr. Ahmed Hassan") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            isError = showLeader1NameError,
                            supportingText = if (showLeader1NameError) {
                                { Text("Leader 1 Name is required") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 5. Leader 2 Title (Optional)
                        OutlinedTextField(
                            value = leader2Title,
                            onValueChange = { leader2Title = it },
                            label = { Text("Leader 2 Title (Optional)") },
                            placeholder = { Text("e.g. Vice Dean, Deputy Director") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 6. Leader 2 Name (Optional)
                        OutlinedTextField(
                            value = leader2Name,
                            onValueChange = { leader2Name = it },
                            label = { Text("Leader 2 Name (Optional)") },
                            placeholder = { Text("e.g. Dr. Mahmoud Saqr") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "Default Media Hashtags",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 7. Default Hashtags (Multiline)
                        OutlinedTextField(
                            value = defaultHashtags,
                            onValueChange = { defaultHashtags = it },
                            label = { Text("Default Hashtags") },
                            placeholder = { Text("#Veterinary\n#AlAzhar\n#University") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Artificial Intelligence Section Card
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Artificial Intelligence",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Connection status badge
                            if (aiProvider != AI_PROVIDER_NONE) {
                                val isConn = aiConnectionStatus == AI_STATUS_CONNECTED
                                val badgeColor = if (isConn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                val textCol = if (isConn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = aiConnectionStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textCol,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Configure workspace-specific AI providers and execution parameters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // AI Provider Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isProviderDropdownExpanded,
                            onExpandedChange = { isProviderDropdownExpanded = !isProviderDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = aiProvider,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Provider") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = isProviderDropdownExpanded,
                                onDismissRequest = { isProviderDropdownExpanded = false }
                            ) {
                                AI_PROVIDERS.forEach { providerOption ->
                                    DropdownMenuItem(
                                        text = { Text(providerOption) },
                                        onClick = {
                                            aiProvider = providerOption
                                            isProviderDropdownExpanded = false
                                            val models = getModelsForProvider(providerOption)
                                            aiModel = models.firstOrNull() ?: AI_MODEL_NONE
                                        }
                                    )
                                }
                            }
                        }

                        if (aiProvider != AI_PROVIDER_NONE) {
                            // Dynamic API Key Label
                            val keyLabel = when (aiProvider) {
                                AI_PROVIDER_GEMINI -> "Gemini API Key"
                                AI_PROVIDER_OPENAI -> "OpenAI API Key"
                                AI_PROVIDER_CLAUDE -> "Claude API Key"
                                AI_PROVIDER_OPENROUTER -> "OpenRouter API Key"
                                AI_PROVIDER_CUSTOM -> "Custom API Key"
                                else -> "API Key"
                            }

                            // API Key Field
                            OutlinedTextField(
                                value = aiApiKey,
                                onValueChange = { aiApiKey = it },
                                label = { Text(keyLabel) },
                                placeholder = { Text("Enter your secure API key") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isApiKeyVisible) "Hide key" else "Show key"
                                        )
                                    }
                                },
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                supportingText = { Text("Encrypted locally before storing. Never exposed in UI.") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Dynamic Model Dropdown
                            val currentModels = getModelsForProvider(aiProvider)
                            ExposedDropdownMenuBox(
                                expanded = isModelDropdownExpanded,
                                onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = aiModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Model") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = isModelDropdownExpanded,
                                    onDismissRequest = { isModelDropdownExpanded = false }
                                ) {
                                    currentModels.forEach { modelOption ->
                                        DropdownMenuItem(
                                            text = { Text(modelOption) },
                                            onClick = {
                                                aiModel = modelOption
                                                isModelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Text(
                                text = "AI Parameters",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Temperature Slider
                            Column {
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
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Temperature",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.2f", aiTemperature),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = aiTemperature,
                                    onValueChange = { aiTemperature = it },
                                    valueRange = 0.0f..1.0f,
                                    steps = 19
                                )
                            }

                            // Maximum Output Slider
                            Column {
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
                                            imageVector = Icons.Default.Token,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Maximum Output",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${aiMaxTokens.toInt()} Tokens",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = aiMaxTokens,
                                    onValueChange = { aiMaxTokens = it },
                                    valueRange = 256f..4096f,
                                    steps = 29
                                )
                            }

                            // Language Dropdown
                            ExposedDropdownMenuBox(
                                expanded = isLanguageDropdownExpanded,
                                onExpandedChange = { isLanguageDropdownExpanded = !isLanguageDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = aiLanguage,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Language") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLanguageDropdownExpanded)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = isLanguageDropdownExpanded,
                                    onDismissRequest = { isLanguageDropdownExpanded = false }
                                ) {
                                    AI_LANGUAGES.forEach { langOption ->
                                        DropdownMenuItem(
                                            text = { Text(langOption) },
                                            onClick = {
                                                aiLanguage = langOption
                                                isLanguageDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Last Validation timestamp
                            if (aiLastValidationTime > 0) {
                                val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(aiLastValidationTime))
                                Text(
                                    text = "Last Validated: $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Validate Connection Button
                            OutlinedButton(
                                onClick = {
                                    isValidatingConnection = true
                                    validationMessage = null
                                    viewModel.validateAIConnection(
                                        workspaceId = workspaceId ?: "temp_ws",
                                        providerName = aiProvider,
                                        rawApiKey = aiApiKey,
                                        modelName = aiModel,
                                        temperature = aiTemperature,
                                        maxTokens = aiMaxTokens.toInt(),
                                        language = aiLanguage
                                    ) { success, msg ->
                                        isValidatingConnection = false
                                        aiConnectionStatus = if (success) AI_STATUS_CONNECTED else AI_STATUS_FAILED
                                        aiLastValidationTime = System.currentTimeMillis()
                                        validationMessage = msg
                                    }
                                },
                                enabled = !isValidatingConnection && aiApiKey.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isValidatingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validating Connection...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Validate Connection")
                                }
                            }

                            validationMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (aiConnectionStatus == AI_STATUS_CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Submit Save Button
                Button(
                    onClick = {
                        val isNameValid = name.isNotBlank()
                        val isLeader1TitleValid = leader1Title.isNotBlank()
                        val isLeader1NameValid = leader1Name.isNotBlank()

                        showNameError = !isNameValid
                        showLeader1TitleError = !isLeader1TitleValid
                        showLeader1NameError = !isLeader1NameValid

                        if (isNameValid && isLeader1TitleValid && isLeader1NameValid) {
                            viewModel.saveWorkspace(
                                id = workspaceId,
                                name = name,
                                type = type,
                                leader1Title = leader1Title,
                                leader1Name = leader1Name,
                                leader2Title = leader2Title,
                                leader2Name = leader2Name,
                                defaultHashtags = defaultHashtags,
                                aiProvider = aiProvider,
                                aiApiKeyRaw = aiApiKey,
                                aiModel = aiModel,
                                aiTemperature = aiTemperature,
                                aiMaxTokens = aiMaxTokens.toInt(),
                                aiLanguage = aiLanguage,
                                aiConnectionStatus = aiConnectionStatus,
                                aiLastValidationTime = aiLastValidationTime,
                                onComplete = { savedId ->
                                    if (isEditing) {
                                        onNavigateBack()
                                    } else {
                                        onNavigateToStorageWizard(savedId)
                                    }
                                }
                            )
                        }
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
                        text = if (isEditing) "Update Workspace" else "Save Workspace",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
