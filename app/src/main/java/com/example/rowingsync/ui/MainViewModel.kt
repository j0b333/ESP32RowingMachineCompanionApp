package com.example.rowingsync.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rowingsync.data.ApiClient
import com.example.rowingsync.data.SessionSummary
import com.example.rowingsync.health.HealthConnectManager
import com.example.rowingsync.health.HealthConnectManager.HealthConnectExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the main screen
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val sessions: List<SessionSummary> = emptyList(),
    val error: String? = null,
    val esp32Address: String = "rower.local",
    val syncingSessionId: Int? = null,
    val deletingEsp32SessionId: Int? = null,
    val healthConnectAvailable: Boolean = false,
    val healthConnectPermissionsGranted: Boolean = false,
    val healthConnectSdkStatus: Int = 0,
    // Health Connect workout management
    val healthConnectWorkouts: List<HealthConnectExercise> = emptyList(),
    val isLoadingHealthConnectWorkouts: Boolean = false,
    val deletingWorkoutId: String? = null,
    val showHealthConnectWorkouts: Boolean = false
)

/**
 * ViewModel for the main activity
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val healthConnectManager = HealthConnectManager(application)
    private var permissionLauncher: ((Set<String>) -> Unit)? = null
    private var openHealthConnectSettingsLauncher: (() -> Unit)? = null

    init {
        // Check Health Connect availability
        val sdkStatus = androidx.health.connect.client.HealthConnectClient.getSdkStatus(application)
        Log.d(TAG, "Health Connect SDK Status in ViewModel: $sdkStatus")
        _uiState.value = _uiState.value.copy(
            healthConnectAvailable = healthConnectManager.isAvailable(),
            healthConnectSdkStatus = sdkStatus
        )
    }

    /**
     * Get a human-readable description of the Health Connect status
     */
    fun getHealthConnectStatusMessage(): String {
        return when (_uiState.value.healthConnectSdkStatus) {
            androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE ->
                "Health Connect is not installed. Please install it from the Play Store."
            androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                "Health Connect needs to be updated. Please update it from the Play Store."
            androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE ->
                "Health Connect is available"
            else ->
                "Unknown Health Connect status"
        }
    }

    /**
     * Set the permission launcher from the Activity
     */
    fun setPermissionLauncher(launcher: (Set<String>) -> Unit) {
        permissionLauncher = launcher
    }

    /**
     * Set the Health Connect settings launcher from the Activity
     */
    fun setOpenHealthConnectSettingsLauncher(launcher: () -> Unit) {
        openHealthConnectSettingsLauncher = launcher
    }

    /**
     * Open Health Connect settings directly
     */
    fun openHealthConnectSettings() {
        Log.d(TAG, "Opening Health Connect settings directly")
        openHealthConnectSettingsLauncher?.invoke()
    }

    /**
     * Check if Health Connect permissions are granted
     */
    fun checkHealthConnectPermissions() {
        if (!_uiState.value.healthConnectAvailable) {
            return
        }

        viewModelScope.launch {
            val hasPermissions = healthConnectManager.hasAllPermissions()
            _uiState.value = _uiState.value.copy(
                healthConnectPermissionsGranted = hasPermissions
            )
        }
    }

    /**
     * Request Health Connect permissions
     */
    fun requestHealthConnectPermissions() {
        Log.d(TAG, "requestHealthConnectPermissions called")
        Log.d(TAG, "Permission launcher: ${if (permissionLauncher != null) "initialized" else "NULL!"}")
        Log.d(TAG, "Requesting ${HealthConnectManager.PERMISSIONS.size} permissions")
        permissionLauncher?.invoke(HealthConnectManager.PERMISSIONS)
    }

    /**
     * Update ESP32 address
     */
    fun setEsp32Address(address: String) {
        _uiState.value = _uiState.value.copy(esp32Address = address)
        ApiClient.reset()
    }
    
    /**
     * Connect to ESP32 and load sessions
     */
    fun refreshSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val api = ApiClient.getApi(_uiState.value.esp32Address)
                val sessionsResponse = api.getSessions()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = true,
                    sessions = sessionsResponse.sessions,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to ESP32", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = false,
                    error = "Failed to connect: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Sync a single session to Health Connect
     */
    fun syncSession(sessionId: Int) {
        // Check if Health Connect is available before attempting sync
        if (!_uiState.value.healthConnectAvailable) {
            _uiState.value = _uiState.value.copy(
                error = "Health Connect is not available on this device"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncingSessionId = sessionId)
            
            try {
                val api = ApiClient.getApi(_uiState.value.esp32Address)
                val sessionDetail = api.getSession(sessionId)
                
                val success = healthConnectManager.syncSession(sessionDetail)
                
                if (success) {
                    // Mark as synced on ESP32
                    try {
                        Log.d(TAG, "Marking session $sessionId as synced on ESP32 at ${_uiState.value.esp32Address}")
                        val markResponse = api.markSynced(sessionId)
                        Log.d(TAG, "Mark synced response: status=${markResponse.status}, success=${markResponse.success}, error=${markResponse.error}")
                        if (markResponse.error != null) {
                            Log.w(TAG, "Failed to mark session as synced on ESP32: ${markResponse.error}")
                            _uiState.value = _uiState.value.copy(
                                error = "Synced to Health Connect, but ESP32 marking failed: ${markResponse.error}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception marking session as synced on ESP32", e)
                        _uiState.value = _uiState.value.copy(
                            error = "Synced to Health Connect, but couldn't mark on ESP32: ${e.message}"
                        )
                    }
                    
                    // Refresh session list
                    refreshSessions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to sync to Health Connect. Please check permissions."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync session", e)
                _uiState.value = _uiState.value.copy(
                    error = "Sync failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(syncingSessionId = null)
            }
        }
    }
    
    /**
     * Sync all unsynced sessions
     */
    fun syncAllSessions() {
        viewModelScope.launch {
            val unsyncedSessions = _uiState.value.sessions.filter { !it.synced }
            
            for (session in unsyncedSessions) {
                syncSession(session.id)
            }
        }
    }
    
    /**
     * Delete a session from ESP32
     * Only synced sessions can be deleted to prevent data loss
     */
    fun deleteEsp32Session(sessionId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingEsp32SessionId = sessionId)
            
            try {
                val api = ApiClient.getApi(_uiState.value.esp32Address)
                val response = api.deleteSession(sessionId)
                Log.d(TAG, "Delete session response: status=${response.status}, success=${response.success}, error=${response.error}")
                
                if (response.success == true || response.status == "ok") {
                    // Refresh session list to reflect the deletion
                    refreshSessions()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete session: ${response.error ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete ESP32 session", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete session: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(deletingEsp32SessionId = null)
            }
        }
    }
    
    /**
     * Delete all synced sessions from ESP32
     */
    fun deleteAllSyncedEsp32Sessions() {
        viewModelScope.launch {
            val syncedSessions = _uiState.value.sessions.filter { it.synced }
            
            if (syncedSessions.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "No synced sessions to delete"
                )
                return@launch
            }
            
            var deletedCount = 0
            var failedCount = 0
            
            for (session in syncedSessions) {
                try {
                    _uiState.value = _uiState.value.copy(deletingEsp32SessionId = session.id)
                    val api = ApiClient.getApi(_uiState.value.esp32Address)
                    val response = api.deleteSession(session.id)
                    
                    if (response.success == true || response.status == "ok") {
                        deletedCount++
                    } else {
                        failedCount++
                        Log.w(TAG, "Failed to delete session ${session.id}: ${response.error}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    Log.e(TAG, "Exception deleting session ${session.id}", e)
                }
            }
            
            _uiState.value = _uiState.value.copy(deletingEsp32SessionId = null)
            
            if (failedCount > 0) {
                _uiState.value = _uiState.value.copy(
                    error = "Deleted $deletedCount sessions, $failedCount failed"
                )
            }
            
            // Refresh session list
            refreshSessions()
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Toggle display of Health Connect workouts section
     */
    fun toggleHealthConnectWorkouts() {
        val newShowState = !_uiState.value.showHealthConnectWorkouts
        _uiState.value = _uiState.value.copy(showHealthConnectWorkouts = newShowState)
        if (newShowState) {
            loadHealthConnectWorkouts()
        }
    }

    /**
     * Load all rowing workouts from Health Connect
     */
    fun loadHealthConnectWorkouts() {
        if (!_uiState.value.healthConnectAvailable || !_uiState.value.healthConnectPermissionsGranted) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHealthConnectWorkouts = true)

            try {
                val workouts = healthConnectManager.getExerciseSessions()
                _uiState.value = _uiState.value.copy(
                    healthConnectWorkouts = workouts,
                    isLoadingHealthConnectWorkouts = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Health Connect workouts", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingHealthConnectWorkouts = false,
                    error = "Failed to load Health Connect workouts: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a single workout from Health Connect
     */
    fun deleteHealthConnectWorkout(workoutId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingWorkoutId = workoutId)

            try {
                val success = healthConnectManager.deleteExerciseSession(workoutId)

                if (success) {
                    // Refresh the list
                    loadHealthConnectWorkouts()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete workout from Health Connect"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete Health Connect workout", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete workout: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(deletingWorkoutId = null)
            }
        }
    }

    /**
     * Delete all rowing workouts from Health Connect
     */
    fun deleteAllHealthConnectWorkouts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHealthConnectWorkouts = true)

            try {
                val deletedCount = healthConnectManager.deleteAllRowingSessions()

                // Refresh the list
                loadHealthConnectWorkouts()

                if (deletedCount > 0) {
                    Log.i(TAG, "Deleted $deletedCount workouts from Health Connect")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete all Health Connect workouts", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingHealthConnectWorkouts = false,
                    error = "Failed to delete workouts: ${e.message}"
                )
            }
        }
    }
}
