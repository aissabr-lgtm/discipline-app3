package com.discipline.app.ui.screens.habit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitType
import com.discipline.app.ui.components.DisciplineCard
import com.discipline.app.ui.theme.ScoreColor0
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitFormScreen(
    viewModel: HabitFormViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    val isEditing = uiState.habitId != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = if (isEditing) "Edit Habit" else "New Habit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Habit", tint = ScoreColor0)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Description
            item {
                DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Habit Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.setTitle(it) },
                        label = { Text("Title *") },
                        placeholder = { Text("e.g. Read 10 pages, Drink water, Study") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.errorMessage != null
                    )
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = uiState.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.setDescription(it) },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Why is this habit important to you?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            // Category
            item {
                DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (cat in HabitCategory.entries) {
                            FilterChip(
                                selected = uiState.category == cat,
                                onClick = { viewModel.setCategory(cat) },
                                label = { Text(cat.displayName) }
                            )
                        }
                    }
                    if (uiState.category == HabitCategory.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.customCategory,
                            onValueChange = { viewModel.setCustomCategory(it) },
                            label = { Text("Custom Category Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Habit Type
            item {
                DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Habit Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = uiState.type == HabitType.YES_NO,
                            onClick = { viewModel.setType(HabitType.YES_NO) },
                            label = { Text("Yes / No (Done or not)") }
                        )
                        FilterChip(
                            selected = uiState.type == HabitType.MEASURABLE,
                            onClick = { viewModel.setType(HabitType.MEASURABLE) },
                            label = { Text("Measurable (Numeric target)") }
                        )
                    }

                    if (uiState.type == HabitType.MEASURABLE) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.targetValueText,
                                onValueChange = { viewModel.setTargetValue(it) },
                                label = { Text("Target *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.unit,
                                onValueChange = { viewModel.setUnit(it) },
                                label = { Text("Unit (e.g. pages, min)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Scheduling
            item {
                DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.selectAllDays() }) {
                            Text("Every Day")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayVal in 1..7) {
                            val isSelected = uiState.scheduledDays.contains(dayVal)
                            val dayName = DayOfWeek.of(dayVal).getDisplayName(TextStyle.NARROW, Locale.getDefault())
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.toggleDay(dayVal) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Daily Reminder
            item {
                DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Daily Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = uiState.hasReminder,
                            onCheckedChange = { viewModel.setHasReminder(it) }
                        )
                    }

                    if (uiState.hasReminder) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val formattedTime = String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute)
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute -> viewModel.setReminderTime(hourOfDay, minute) },
                                    uiState.reminderHour,
                                    uiState.reminderMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Reminder Time: $formattedTime")
                        }
                    }
                }
            }

            // Edit-only options: Pause & Archive
            if (isEditing) {
                item {
                    DisciplineCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Habit Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Pause Habit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = "Temporarily hides from today's schedule without breaking streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = uiState.isPaused, onCheckedChange = { viewModel.setPaused(it) })
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Archive Habit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = "Retires habit while preserving all historical records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = uiState.isArchived, onCheckedChange = { viewModel.setArchived(it) })
                        }
                    }
                }
            }

            // Save Action
            item {
                Button(
                    onClick = { viewModel.saveHabit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isSaving
                ) {
                    Text(
                        text = if (uiState.isSaving) "Saving..." else if (isEditing) "Update Habit" else "Create Habit",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete this habit? All associated logs will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteHabit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScoreColor0)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
