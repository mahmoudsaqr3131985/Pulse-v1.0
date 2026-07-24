package com.example.screens.event

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.models.EventAIContentEntity
import com.example.utils.ResponsiveUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIContentDetailsScreen(
    contentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNewContent: (String) -> Unit,
    viewModel: AIContentDetailsViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val scrollState = rememberScrollState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(contentId) {
        viewModel.loadContent(contentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.content_details),
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
                actions = {
                    // Export Icon Button
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export"
                        )
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export as Markdown (.md)") },
                            onClick = {
                                (uiState as? ContentDetailUiState.Success)?.let {
                                    viewModel.exportAsMarkdown(context, it.content)
                                }
                                showExportMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Article, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Plain Text (.txt)") },
                            onClick = {
                                (uiState as? ContentDetailUiState.Success)?.let {
                                    viewModel.exportAsTxt(context, it.content)
                                }
                                showExportMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as JSON (.json)") },
                            onClick = {
                                (uiState as? ContentDetailUiState.Success)?.let {
                                    viewModel.exportAsJson(context, it.content)
                                }
                                showExportMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                        )
                    }

                    // Duplicate Button
                    IconButton(onClick = {
                        (uiState as? ContentDetailUiState.Success)?.let {
                            viewModel.duplicateContent(it.content) { newId ->
                                onNavigateToNewContent(newId)
                                Toast.makeText(context, "Content Duplicated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.CopyAll,
                            contentDescription = stringResource(id = R.string.duplicate)
                        )
                    }

                    // Delete Button
                    IconButton(onClick = {
                        viewModel.deleteContent(contentId) {
                            onNavigateBack()
                            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            tint = MaterialTheme.colorScheme.error
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
                is ContentDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ContentDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                is ContentDetailUiState.Success -> {
                    val content = state.content
                    val dateFormatted = remember(content.generationTime) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(content.generationTime))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = responsivePadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title Editor
                        EditableTitleHeader(
                            title = content.title,
                            onSave = { viewModel.updateField(content.id, "title", it) }
                        )

                        // Meta details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("AI Provider: ${content.provider}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Model: ${content.model}", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Lang: ${content.language.uppercase()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(dateFormatted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Copy All and Share All Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val full = buildString {
                                        append("=== ").append(content.title).append(" ===\n\n")
                                        append("Headline:\n").append(content.headline).append("\n\n")
                                        append("Facebook:\n").append(content.facebookPost).append("\n\n")
                                        append("Short Post:\n").append(content.shortPost).append("\n\n")
                                        append("Caption:\n").append(content.caption).append("\n\n")
                                        append("Hashtags:\n").append(content.hashtags).append("\n\n")
                                        append("News Summary:\n").append(content.newsSummary).append("\n\n")
                                        append("Voice-over:\n").append(content.voiceOverScript)
                                    }
                                    clipboardManager.setText(AnnotatedString(full))
                                    Toast.makeText(context, "Copied all sections!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy All")
                            }

                            FilledTonalButton(
                                onClick = {
                                    val full = buildString {
                                        append("=== ").append(content.title).append(" ===\n\n")
                                        append("Headline:\n").append(content.headline).append("\n\n")
                                        append("Facebook:\n").append(content.facebookPost).append("\n\n")
                                        append("Short Post:\n").append(content.shortPost).append("\n\n")
                                        append("Caption:\n").append(content.caption).append("\n\n")
                                        append("Hashtags:\n").append(content.hashtags).append("\n\n")
                                        append("News Summary:\n").append(content.newsSummary).append("\n\n")
                                        append("Voice-over:\n").append(content.voiceOverScript)
                                    }
                                    viewModel.shareSection(context, content.title, full)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share All")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Render each generated segment
                        val sections = listOf(
                            DetailSectionConfig("headline", stringResource(R.string.headline_title), content.headline, Icons.Default.Title),
                            DetailSectionConfig("facebookPost", stringResource(R.string.facebook_post_title), content.facebookPost, Icons.Default.ChatBubbleOutline),
                            DetailSectionConfig("shortPost", stringResource(R.string.short_post_title), content.shortPost, Icons.Default.Send),
                            DetailSectionConfig("caption", stringResource(R.string.caption_title), content.caption, Icons.Default.PhotoCamera),
                            DetailSectionConfig("hashtags", stringResource(R.string.hashtags_title), content.hashtags, Icons.Default.Tag),
                            DetailSectionConfig("newsSummary", stringResource(R.string.news_summary_title), content.newsSummary, Icons.Default.ReceiptLong),
                            DetailSectionConfig("voiceOverScript", stringResource(R.string.voice_over_script_title), content.voiceOverScript, Icons.Default.RecordVoiceOver)
                        )

                        sections.forEach { section ->
                            DetailSectionCard(
                                config = section,
                                onSave = { updated -> viewModel.updateField(content.id, section.key, updated) },
                                onShare = { viewModel.shareSection(context, section.label, section.content) }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditableTitleHeader(
    title: String,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var currentVal by remember(title) { mutableStateOf(title) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = currentVal,
                    onValueChange = { currentVal = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            onSave(currentVal)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save title")
                        }
                    }
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit title")
                }
            }
        }
    }
}

data class DetailSectionConfig(
    val key: String,
    val label: String,
    val content: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSectionCard(
    config: DetailSectionConfig,
    onSave: (String) -> Unit,
    onShare: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var currentVal by remember(config.content) { mutableStateOf(config.content) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = config.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = currentVal,
                    onValueChange = { currentVal = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        currentVal = config.content
                        isEditing = false
                    }) {
                        Text("Cancel")
                    }

                    Button(onClick = {
                        onSave(currentVal)
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                }
            } else {
                Text(
                    text = config.content.ifBlank { "No content generated for this section." },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section Actions Bar: Copy, Edit, Share
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(config.content))
                            Toast.makeText(context, "Copied section text!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit
                    TextButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share Section
                    TextButton(
                        onClick = onShare
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
