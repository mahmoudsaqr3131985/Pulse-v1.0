package com.example.screens.event

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
fun MediaCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContentDetail: (String) -> Unit,
    viewModel: MediaCenterViewModel = viewModel()
) {
    val context = LocalContext.current
    val responsivePadding = ResponsiveUtils.responsivePadding()
    
    val aiContents by viewModel.aiContents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedWorkspaceId by viewModel.selectedWorkspaceId.collectAsStateWithLifecycle()
    val filterFavoritesOnly by viewModel.filterFavoritesOnly.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.media_center),
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
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(id = R.string.sort_by)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sort_newest)) },
                            onClick = {
                                viewModel.setSortOrder(MediaSortOrder.NEWEST)
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sort_oldest)) },
                            onClick = {
                                viewModel.setSortOrder(MediaSortOrder.OLDEST)
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.sort_alphabetical)) },
                            onClick = {
                                viewModel.setSortOrder(MediaSortOrder.ALPHABETICAL)
                                showSortMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = responsivePadding, vertical = 12.dp)
                    .testTag("media_center_search"),
                placeholder = { Text(stringResource(id = R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // 2. Filter Bar (Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = responsivePadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorites Only Chip
                FilterChip(
                    selected = filterFavoritesOnly,
                    onClick = { viewModel.setFilterFavoritesOnly(!filterFavoritesOnly) },
                    label = { Text(stringResource(id = R.string.favorites)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filterFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                // Workspace filter chip
                val activeWorkspaceName = workspaces.find { it.id == selectedWorkspaceId }?.name ?: stringResource(id = R.string.all)
                FilterChip(
                    selected = selectedWorkspaceId != null,
                    onClick = { showWorkspaceDialog = true },
                    label = { Text("${stringResource(id = R.string.workspace_filter)}: $activeWorkspaceName") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Media Items List
            if (aiContents.isEmpty()) {
                EmptyMediaState(searchQuery.isNotEmpty() || selectedWorkspaceId != null || filterFavoritesOnly)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("media_center_list"),
                    contentPadding = PaddingValues(horizontal = responsivePadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(aiContents, key = { it.id }) { item ->
                        MediaCenterItemCard(
                            item = item,
                            onClick = { onNavigateToContentDetail(item.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onDuplicate = {
                                viewModel.duplicateItem(item)
                                Toast.makeText(context, "Content Duplicated", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteItem(item.id)
                                Toast.makeText(context, "Content Deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Workspace Filter Picker Dialog
    if (showWorkspaceDialog) {
        AlertDialog(
            onDismissRequest = { showWorkspaceDialog = false },
            title = { Text(stringResource(id = R.string.workspace_filter)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setWorkspaceFilter(null)
                                showWorkspaceDialog = false
                            }
                            .padding(12.dp),
                        color = if (selectedWorkspaceId == null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Text(
                            text = stringResource(id = R.string.all),
                            fontWeight = if (selectedWorkspaceId == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    workspaces.forEach { ws ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setWorkspaceFilter(ws.id)
                                    showWorkspaceDialog = false
                                }
                                .padding(12.dp),
                            color = if (selectedWorkspaceId == ws.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(
                                text = ws.name,
                                fontWeight = if (selectedWorkspaceId == ws.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkspaceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MediaCenterItemCard(
    item: EventAIContentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.generationTime) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(item.generationTime))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("media_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body Metadata Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(item.provider) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )

                SuggestionChip(
                    onClick = {},
                    label = { Text(item.language) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preview Text Snippet
            Text(
                text = item.headline.ifBlank { item.newsSummary }.take(120) + if (item.headline.length > 120) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Card Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duplicate Action
                IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = stringResource(id = R.string.duplicate),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Action
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // View Details Button
                TextButton(onClick = onClick) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "View")
                }
            }
        }
    }
}

@Composable
fun EmptyMediaState(hasFilters: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasFilters) Icons.Default.SearchOff else Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFilters) "No results match filters" else stringResource(id = R.string.no_generated_content),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilters) "Try adjusting your search query or workspace / favorite filters." else "Generate AI content from the event Details section first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
