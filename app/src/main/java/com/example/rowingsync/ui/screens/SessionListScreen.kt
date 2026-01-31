package com.example.rowingsync.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rowingsync.data.SessionSummary
import com.example.rowingsync.health.HealthConnectManager.HealthConnectExercise
import com.example.rowingsync.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var addressInput by remember { mutableStateOf(uiState.esp32Address) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RowingSync") },
                actions = {
                    IconButton(onClick = { viewModel.refreshSessions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Connection settings
            ConnectionCard(
                address = addressInput,
                onAddressChange = { addressInput = it },
                onConnect = { 
                    viewModel.setEsp32Address(addressInput)
                    viewModel.refreshSessions()
                },
                isConnected = uiState.isConnected,
                isLoading = uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Health Connect status
            if (!uiState.healthConnectAvailable) {
                val statusMessage = viewModel.getHealthConnectStatusMessage()
                val needsInstall = uiState.healthConnectSdkStatus ==
                    androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsInstall)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = if (needsInstall)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                statusMessage,
                                color = if (needsInstall)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        if (needsInstall) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    // Open Health Connect in Play Store
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to web browser
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                        )
                                        context.startActivity(webIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install Health Connect")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!uiState.healthConnectPermissionsGranted) {
                // Show permission request card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Health Connect Permissions Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Grant permissions to sync your rowing workouts to Health Connect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.requestHealthConnectPermissions() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Permissions")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.openHealthConnectSettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Health Connect Settings")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sessions list (ESP32 workouts) - shown first
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.sessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DirectionsBoat,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No sessions on ESP32",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Connect to your rowing machine to see workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                var showDeleteAllSyncedDialog by remember { mutableStateOf(false) }
                val unsyncedCount = uiState.sessions.count { !it.synced }
                val syncedCount = uiState.sessions.count { it.synced }
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ESP32 Workouts",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Bulk action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sync all unsynced button
                            if (unsyncedCount > 0 && uiState.healthConnectAvailable) {
                                Button(
                                    onClick = { viewModel.syncAllSessions() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.syncingSessionId == null
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync All ($unsyncedCount)")
                                }
                            }
                            
                            // Delete all synced button
                            if (syncedCount > 0) {
                                OutlinedButton(
                                    onClick = { showDeleteAllSyncedDialog = true },
                                    modifier = if (unsyncedCount > 0 && uiState.healthConnectAvailable) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                    enabled = uiState.deletingEsp32SessionId == null,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Synced ($syncedCount)")
                                }
                            }
                        }
                        
                        if (unsyncedCount > 0 || syncedCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        uiState.sessions.forEach { session ->
                            SessionCard(
                                session = session,
                                isSyncing = uiState.syncingSessionId == session.id,
                                isDeleting = uiState.deletingEsp32SessionId == session.id,
                                onSync = { viewModel.syncSession(session.id) },
                                onDelete = { viewModel.deleteEsp32Session(session.id) },
                                healthConnectAvailable = uiState.healthConnectAvailable
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                // Delete All Synced Confirmation Dialog
                if (showDeleteAllSyncedDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAllSyncedDialog = false },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                        title = { Text("Delete All Synced Workouts?") },
                        text = {
                            Text("This will permanently delete $syncedCount synced workout${if (syncedCount != 1) "s" else ""} from the ESP32. The data has already been synced to Health Connect.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteAllSyncedDialog = false
                                    viewModel.deleteAllSyncedEsp32Sessions()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete All")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAllSyncedDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health Connect Workouts Management Section - shown after ESP32 sessions
            if (uiState.healthConnectAvailable && uiState.healthConnectPermissionsGranted) {
                HealthConnectWorkoutsSection(
                    showWorkouts = uiState.showHealthConnectWorkouts,
                    workouts = uiState.healthConnectWorkouts,
                    isLoading = uiState.isLoadingHealthConnectWorkouts,
                    deletingWorkoutId = uiState.deletingWorkoutId,
                    onToggle = { viewModel.toggleHealthConnectWorkouts() },
                    onRefresh = { viewModel.loadHealthConnectWorkouts() },
                    onDelete = { workoutId -> viewModel.deleteHealthConnectWorkout(workoutId) },
                    onDeleteAll = { viewModel.deleteAllHealthConnectWorkouts() }
                )
            }
        }
    }
}

@Composable
fun ConnectionCard(
    address: String,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    isConnected: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isConnected) "Connected" else "Not Connected",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("ESP32 Address") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConnect() }
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onConnect,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: SessionSummary,
    isSyncing: Boolean,
    isDeleting: Boolean,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    healthConnectAvailable: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(session.startTime)),
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Sync status indicator - always shown
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.synced) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Not Synced",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Not Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    icon = Icons.Default.Straighten,
                    value = String.format("%.0f m", session.distance),
                    label = "Distance"
                )
                MetricItem(
                    icon = Icons.Default.Timer,
                    value = formatDuration(session.duration),
                    label = "Duration"
                )
                MetricItem(
                    icon = Icons.Default.Rowing,
                    value = "${session.strokes}",
                    label = "Strokes"
                )
                MetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${session.calories}",
                    label = "Calories"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sync button - shown when not synced and Health Connect available
                if (!session.synced && healthConnectAvailable) {
                    Button(
                        onClick = onSync,
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSyncing) "Syncing..." else "Sync")
                    }
                }
                
                // Delete button - enabled only when synced
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = if (!session.synced && healthConnectAvailable) Modifier else Modifier.fillMaxWidth(),
                    enabled = session.synced && !isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (session.synced) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (session.synced && !isDeleting) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDeleting) "Deleting..." else "Delete from ESP32")
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Workout?") },
            text = {
                Text("This will permanently delete this workout from the ESP32. The data has already been synced to Health Connect.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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
fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

@Composable
fun HealthConnectWorkoutsSection(
    showWorkouts: Boolean,
    workouts: List<HealthConnectExercise>,
    isLoading: Boolean,
    deletingWorkoutId: String?,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Health Connect Workouts",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    if (showWorkouts) {
                        IconButton(onClick = onRefresh, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (showWorkouts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showWorkouts) "Collapse" else "Expand"
                        )
                    }
                }
            }

            if (showWorkouts) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (workouts.isEmpty()) {
                    Text(
                        "No rowing workouts found in Health Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        "${workouts.size} rowing workout${if (workouts.size != 1) "s" else ""} stored",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Delete All button
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Rowing Workouts")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of workouts
                    workouts.forEach { workout ->
                        HealthConnectWorkoutCard(
                            workout = workout,
                            isDeleting = deletingWorkoutId == workout.id,
                            onDelete = { onDelete(workout.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete All Workouts?") },
            text = {
                Text("This will permanently delete all ${workouts.size} rowing workout${if (workouts.size != 1) "s" else ""} from Health Connect. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        onDeleteAll()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HealthConnectWorkoutCard(
    workout: HealthConnectExercise,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        workout.title ?: workout.getExerciseTypeName(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        dateFormat.format(Date.from(workout.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "${workout.durationMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Workout?") },
            text = {
                Text("This will permanently delete this workout and all associated data (heart rate, calories, etc.) from Health Connect.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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

