package com.example.screens.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.*
import com.example.utils.ResponsiveUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    viewModel: EventViewModel,
    eventId: String? = null,
    onNavigateBack: () -> Unit,
    onSavedSuccessfully: () -> Unit
) {
    val responsivePadding = ResponsiveUtils.responsivePadding()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(eventId != null) }

    // Section 1: Basic Info
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var dateStr by remember { mutableStateOf(todayStr) }
    var dateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var timeStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Section 2: Leadership Attendance
    var leader1Present by remember { mutableStateOf(true) }
    var leader2Present by remember { mutableStateOf(true) }

    // Section 3: Additional Guests
    var guestList by remember { mutableStateOf<List<GuestItem>>(emptyList()) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var editingGuest by remember { mutableStateOf<GuestItem?>(null) }

    // Section 4: Event Status
    var selectedStatus by remember { mutableStateOf(EVENT_STATUS_PLANNED) }
    var isStatusMenuExpanded by remember { mutableStateOf(false) }

    // Validation State
    var showValidationErrors by remember { mutableStateOf(false) }

    // Load existing event if editing
    LaunchedEffect(eventId) {
        if (!eventId.isNullOrBlank()) {
            val existing = viewModel.getEventById(eventId)
            if (existing != null) {
                isEditing = true
                title = existing.title
                selectedType = existing.type
                dateStr = existing.date
                dateTimestamp = existing.dateTimestamp
                timeStr = existing.time ?: ""
                location = existing.location ?: ""
                description = existing.description ?: ""
                leader1Present = existing.leader1Present
                leader2Present = existing.leader2Present
                guestList = deserializeGuests(existing.guestsJson)
                selectedStatus = existing.status
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Event" else "Create New Event",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        activeWorkspace?.let { workspace ->
                            Text(
                                text = "Workspace: ${workspace.name}",
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
                CircularProgressIndicator()
            }
        } else if (activeWorkspace == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please select an active Workspace first.",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = responsivePadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // SECTION 1: Basic Information
                FormSectionCard(
                    title = "SECTION 1: Basic Information",
                    icon = Icons.Default.Event
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Event Title (Required)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title *") },
                            placeholder = { Text("Enter event name or subject") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = showValidationErrors && title.isBlank(),
                            supportingText = {
                                if (showValidationErrors && title.isBlank()) {
                                    Text("Event title is required", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Event Type (Required Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = isTypeMenuExpanded,
                            onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Event Type *") },
                                placeholder = { Text("Select event type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                isError = showValidationErrors && selectedType.isBlank(),
                                supportingText = {
                                    if (showValidationErrors && selectedType.isBlank()) {
                                        Text("Event type is required", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = isTypeMenuExpanded,
                                onDismissRequest = { isTypeMenuExpanded = false }
                            ) {
                                EVENT_TYPES.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedType = type
                                            isTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date & Time Picker Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Event Date (Picker)
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Event Date") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCal = Calendar.getInstance()
                                                selectedCal.set(year, month, dayOfMonth)
                                                dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                                                dateTimestamp = selectedCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Pick Date",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCal = Calendar.getInstance()
                                                selectedCal.set(year, month, dayOfMonth)
                                                dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                                                dateTimestamp = selectedCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Event Time (Picker)
                            OutlinedTextField(
                                value = timeStr,
                                onValueChange = { timeStr = it },
                                label = { Text("Event Time (Optional)") },
                                placeholder = { Text("e.g. 10:00 AM") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance()
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Pick Time",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Location
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location (Optional)") },
                            placeholder = { Text("e.g. Main Hall / Conference Room B") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Short Description (Optional)") },
                            placeholder = { Text("Brief overview or agenda for this event...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // SECTION 2: Leadership Attendance
                activeWorkspace?.let { workspace ->
                    FormSectionCard(
                        title = "SECTION 2: Leadership Attendance",
                        icon = Icons.Default.Badge
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Leader 1
                            LeadershipAttendanceRow(
                                title = workspace.leader1Title,
                                name = workspace.leader1Name,
                                isPresent = leader1Present,
                                onToggle = { leader1Present = it }
                            )

                            // Leader 2 if present in workspace
                            if (!workspace.leader2Name.isNullOrBlank() && !workspace.leader2Title.isNullOrBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                LeadershipAttendanceRow(
                                    title = workspace.leader2Title,
                                    name = workspace.leader2Name,
                                    isPresent = leader2Present,
                                    onToggle = { leader2Present = it }
                                )
                            }
                        }
                    }
                }

                // SECTION 3: Additional Guests
                FormSectionCard(
                    title = "SECTION 3: Additional Guests",
                    icon = Icons.Default.Group
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Guests List (${guestList.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedButton(
                                onClick = {
                                    editingGuest = null
                                    showGuestDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Guest", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (guestList.isEmpty()) {
                            Text(
                                text = "No additional guests added yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            guestList.forEach { guest ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
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

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editingGuest = guest
                                                    showGuestDialog = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Guest",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    guestList = guestList.filter { it.id != guest.id }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Guest",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: Event Status
                FormSectionCard(
                    title = "SECTION 4: Event Status",
                    icon = Icons.Default.CheckCircle
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isStatusMenuExpanded,
                        onExpandedChange = { isStatusMenuExpanded = !isStatusMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Event Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isStatusMenuExpanded,
                            onDismissRequest = { isStatusMenuExpanded = false }
                        ) {
                            EVENT_STATUSES.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        selectedStatus = status
                                        isStatusMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Save Action Button
                Button(
                    onClick = {
                        if (title.isBlank() || selectedType.isBlank()) {
                            showValidationErrors = true
                        } else {
                            activeWorkspace?.let { workspace ->
                                viewModel.saveEvent(
                                    id = eventId,
                                    workspaceId = workspace.id,
                                    title = title,
                                    type = selectedType,
                                    date = dateStr,
                                    dateTimestamp = dateTimestamp,
                                    time = timeStr,
                                    location = location,
                                    description = description,
                                    leader1Present = leader1Present,
                                    leader2Present = leader2Present,
                                    guestsJson = serializeGuests(guestList),
                                    status = selectedStatus,
                                    onComplete = {
                                        onSavedSuccessfully()
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Update Event" else "Save Event",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Add / Edit Guest Dialog
    if (showGuestDialog) {
        GuestInputDialog(
            guestToEdit = editingGuest,
            onDismiss = { showGuestDialog = false },
            onSaveGuest = { newGuest ->
                if (editingGuest != null) {
                    guestList = guestList.map { if (it.id == newGuest.id) newGuest else it }
                } else {
                    guestList = guestList + newGuest
                }
                showGuestDialog = false
            }
        )
    }
}

@Composable
private fun FormSectionCard(
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
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            content()
        }
    }
}

@Composable
private fun LeadershipAttendanceRow(
    title: String,
    name: String,
    isPresent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isPresent,
                onClick = { onToggle(true) },
                label = { Text("Present") },
                leadingIcon = if (isPresent) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = !isPresent,
                onClick = { onToggle(false) },
                label = { Text("Absent") },
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun GuestInputDialog(
    guestToEdit: GuestItem?,
    onDismiss: () -> Unit,
    onSaveGuest: (GuestItem) -> Unit
) {
    var guestName by remember { mutableStateOf(guestToEdit?.name ?: "") }
    var guestPosition by remember { mutableStateOf(guestToEdit?.position ?: "") }
    var guestOrganization by remember { mutableStateOf(guestToEdit?.organization ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (guestToEdit != null) "Edit Guest" else "Add Guest",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    label = { Text("Guest Name *") },
                    placeholder = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isError && guestName.isBlank(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = guestPosition,
                    onValueChange = { guestPosition = it },
                    label = { Text("Position / Title") },
                    placeholder = { Text("e.g. Vice President / Director") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = guestOrganization,
                    onValueChange = { guestOrganization = it },
                    label = { Text("Organization / Institution") },
                    placeholder = { Text("e.g. Ministry of Higher Education") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (guestName.isBlank()) {
                        isError = true
                    } else {
                        onSaveGuest(
                            GuestItem(
                                id = guestToEdit?.id ?: UUID.randomUUID().toString(),
                                name = guestName.trim(),
                                position = guestPosition.trim(),
                                organization = guestOrganization.trim()
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Guest JSON Helpers
fun serializeGuests(guests: List<GuestItem>): String {
    val jsonArray = JSONArray()
    for (guest in guests) {
        val obj = JSONObject()
        obj.put("id", guest.id)
        obj.put("name", guest.name)
        obj.put("position", guest.position)
        obj.put("organization", guest.organization)
        jsonArray.put(obj)
    }
    return jsonArray.toString()
}

fun deserializeGuests(jsonStr: String): List<GuestItem> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<GuestItem>()
    try {
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                GuestItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    position = obj.optString("position", ""),
                    organization = obj.optString("organization", "")
                )
            )
        }
    } catch (e: Exception) {
        // Fallback gracefully
    }
    return list
}
