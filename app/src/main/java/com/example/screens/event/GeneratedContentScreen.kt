package com.example.screens.event

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.models.GeneratedMediaPackage
import com.example.utils.ResponsiveUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedContentScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: GeneratedContentViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRegeneratingSection by viewModel.isRegeneratingSection.collectAsStateWithLifecycle()

    // Trigger loading or generation on launch
    LaunchedEffect(eventId) {
        viewModel.loadOrGenerate(eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.generated_content_title),
                        fontWeight = FontWeight.Bold
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is AIContentUiState.Idle -> {
                    // Empty state
                }
                is AIContentUiState.Loading -> {
                    LoadingView(
                        onCancel = {
                            viewModel.cancelGeneration()
                            onNavigateBack()
                        },
                        currentContext = viewModel.currentContext
                    )
                }
                is AIContentUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.generateAll(eventId) },
                        onBack = onNavigateBack
                    )
                }
                is AIContentUiState.Success -> {
                    SuccessView(
                        eventId = eventId,
                        mediaPackage = state.mediaPackage,
                        isRegeneratingSection = isRegeneratingSection,
                        viewModel = viewModel,
                        onSaveDraft = { pkg ->
                            viewModel.saveDraft(eventId, pkg)
                            Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
                        },
                        onExportMarkdown = { pkg ->
                            exportPackageAsMarkdown(context, eventId, pkg)
                        },
                        onCopyAll = { pkg ->
                            val fullText = buildString {
                                append("=== ").append(context.getString(R.string.headline_title)).append(" ===\n")
                                append(pkg.headline).append("\n\n")
                                append("=== ").append(context.getString(R.string.facebook_post_title)).append(" ===\n")
                                append(pkg.facebookPost).append("\n\n")
                                append("=== ").append(context.getString(R.string.short_post_title)).append(" ===\n")
                                append(pkg.shortPost).append("\n\n")
                                append("=== ").append(context.getString(R.string.caption_title)).append(" ===\n")
                                append(pkg.caption).append("\n\n")
                                append("=== ").append(context.getString(R.string.hashtags_title)).append(" ===\n")
                                append(pkg.hashtags).append("\n\n")
                                append("=== ").append(context.getString(R.string.news_summary_title)).append(" ===\n")
                                append(pkg.newsSummary).append("\n\n")
                                append("=== ").append(context.getString(R.string.voice_over_script_title)).append(" ===\n")
                                append(pkg.voiceOverScript)
                            }
                            clipboardManager.setText(AnnotatedString(fullText))
                            Toast.makeText(context, "All sections copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView(
    onCancel: () -> Unit,
    currentContext: com.example.models.AIContext?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.generating_content),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Executing multi-channel content generation workflow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (currentContext != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BoxBorder(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Parameters",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Provider: ${currentContext.selectedAIProvider}", style = MaterialTheme.typography.bodySmall)
                    Text("Model: ${currentContext.selectedAIModel}", style = MaterialTheme.typography.bodySmall)
                    Text("Language: ${currentContext.language}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel Request", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Generation Failed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Go Back")
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun SuccessView(
    eventId: String,
    mediaPackage: GeneratedMediaPackage,
    isRegeneratingSection: String?,
    viewModel: GeneratedContentViewModel,
    onSaveDraft: (GeneratedMediaPackage) -> Unit,
    onExportMarkdown: (GeneratedMediaPackage) -> Unit,
    onCopyAll: (GeneratedMediaPackage) -> Unit
) {
    val scrollState = rememberScrollState()
    val responsivePadding = ResponsiveUtils.responsivePadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(responsivePadding)
    ) {
        // Upper section showing generation parameter details
        viewModel.currentContext?.let { ctx ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Model: ${ctx.selectedAIModel} (${ctx.selectedAIProvider})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Language: ${ctx.language}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // List of Cards representing the 7 Sections
        val sections = remember(mediaPackage) {
            listOf(
                GeneratedSectionConfig("headline", R.string.headline_title, mediaPackage.headline, Icons.AutoMirrored.Filled.Article),
                GeneratedSectionConfig("facebookPost", R.string.facebook_post_title, mediaPackage.facebookPost, Icons.Default.Share),
                GeneratedSectionConfig("shortPost", R.string.short_post_title, mediaPackage.shortPost, Icons.Default.Chat),
                GeneratedSectionConfig("caption", R.string.caption_title, mediaPackage.caption, Icons.Default.Image),
                GeneratedSectionConfig("hashtags", R.string.hashtags_title, mediaPackage.hashtags, Icons.Default.Tag),
                GeneratedSectionConfig("newsSummary", R.string.news_summary_title, mediaPackage.newsSummary, Icons.Default.Feed),
                GeneratedSectionConfig("voiceOverScript", R.string.voice_over_script_title, mediaPackage.voiceOverScript, Icons.Default.Mic)
            )
        }

        sections.forEach { section ->
            SectionCard(
                eventId = eventId,
                config = section,
                isRegenerating = isRegeneratingSection == section.key,
                onSave = { updatedText ->
                    val updatedPkg = when (section.key) {
                        "headline" -> mediaPackage.copy(headline = updatedText)
                        "facebookPost" -> mediaPackage.copy(facebookPost = updatedText)
                        "shortPost" -> mediaPackage.copy(shortPost = updatedText)
                        "caption" -> mediaPackage.copy(caption = updatedText)
                        "hashtags" -> mediaPackage.copy(hashtags = updatedText)
                        "newsSummary" -> mediaPackage.copy(newsSummary = updatedText)
                        "voiceOverScript" -> mediaPackage.copy(voiceOverScript = updatedText)
                        else -> mediaPackage
                    }
                    viewModel.updateField(eventId, updatedPkg)
                },
                onRegenerate = {
                    viewModel.regenerateSection(eventId, section.key)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // Global Actions Layout
        Text(
            text = "Global Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Generate Again
                Button(
                    onClick = { viewModel.generateAll(eventId) },
                    modifier = Modifier.weight(1f).testTag("generate_again_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.generate_again), style = MaterialTheme.typography.labelMedium)
                }

                // Copy All
                Button(
                    onClick = { onCopyAll(mediaPackage) },
                    modifier = Modifier.weight(1f).testTag("copy_all_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.copy_all), style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Draft
                OutlinedButton(
                    onClick = { onSaveDraft(mediaPackage) },
                    modifier = Modifier.weight(1f).testTag("save_draft_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.save_draft), style = MaterialTheme.typography.labelMedium)
                }

                // Export Markdown
                OutlinedButton(
                    onClick = { onExportMarkdown(mediaPackage) },
                    modifier = Modifier.weight(1f).testTag("export_markdown_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.TextSnippet, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.export_markdown), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

data class GeneratedSectionConfig(
    val key: String,
    val titleRes: Int,
    val content: String,
    val icon: ImageVector
)

@Composable
fun SectionCard(
    eventId: String,
    config: GeneratedSectionConfig,
    isRegenerating: Boolean,
    onSave: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(config.content) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Sync state when content changes from upstream
    LaunchedEffect(config.content) {
        editedText = config.content
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_card_${config.key}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BoxBorder(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = config.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isRegenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Area (Static content vs. Editable text field)
            if (isEditing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("section_textfield_${config.key}"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        editedText = config.content
                        isEditing = false
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(editedText)
                            isEditing = false
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(id = R.string.save_changes))
                    }
                }
            } else {
                Text(
                    text = config.content.ifBlank { "No content generated." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                // Card Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(config.content))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_button_${config.key}")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(id = R.string.copy_single), style = MaterialTheme.typography.labelMedium)
                    }

                    // Edit
                    TextButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.testTag("edit_button_${config.key}")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(id = R.string.edit_content), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Regenerate
                    IconButton(
                        onClick = onRegenerate,
                        enabled = !isRegenerating,
                        modifier = Modifier.testTag("regenerate_button_${config.key}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Regenerate Section",
                            tint = if (isRegenerating) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Border Helper function for Clean M3 Styling
@Composable
fun BoxBorder(width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}

// Function to compile markdown, write file to packages context and share
fun exportPackageAsMarkdown(context: Context, eventId: String, pkg: GeneratedMediaPackage) {
    val mdText = """
        # ${pkg.headline}
        
        ## Facebook Post
        ${pkg.facebookPost}
        
        ## Short Post (X/Telegram)
        ${pkg.shortPost}
        
        ## Image Caption
        ${pkg.caption}
        
        ## Hashtags
        ${pkg.hashtags}
        
        ## Official News Summary
        ${pkg.newsSummary}
        
        ## Voice-over Script
        ${pkg.voiceOverScript}
    """.trimIndent()

    try {
        val packageDir = File(context.filesDir, "packages/$eventId")
        if (!packageDir.exists()) {
            packageDir.mkdirs()
        }
        val file = File(packageDir, "AI_Generated_Package.md")
        file.writeText(mdText)

        // Launch Share Intent for the Markdown File
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Media Package Markdown"))
        Toast.makeText(context, "Markdown exported and ready for sharing!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Markdown Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
