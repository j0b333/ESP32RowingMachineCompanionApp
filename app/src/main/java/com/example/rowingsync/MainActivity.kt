package com.example.rowingsync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.rowingsync.ui.MainViewModel
import com.example.rowingsync.ui.screens.SessionListScreen
import com.example.rowingsync.ui.theme.RowingSyncTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    companion object {
        private const val TAG = "MainActivity"
        private const val HEALTH_CONNECT_SETTINGS_ACTION = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    }

    // Health Connect permission launcher
    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d(TAG, "Permission request completed. Granted: $granted")

        // If no permissions were granted and the dialog didn't show, try opening Health Connect directly
        if (granted.isEmpty()) {
            Log.w(TAG, "No permissions granted - may need to open Health Connect manually")
        }

        // Check permissions after user returns from Health Connect settings
        viewModel.checkHealthConnectPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate: Setting up permission launcher")

        // Check Health Connect SDK status
        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        Log.d(TAG, "Health Connect SDK Status: $sdkStatus")
        when (sdkStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.e(TAG, "Health Connect SDK is UNAVAILABLE - not installed?")
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.e(TAG, "Health Connect SDK requires provider update")
            }
            HealthConnectClient.SDK_AVAILABLE -> {
                Log.d(TAG, "Health Connect SDK is AVAILABLE")
            }
            else -> {
                Log.w(TAG, "Unknown SDK status: $sdkStatus")
            }
        }

        // Set the permission launcher in the view model
        viewModel.setPermissionLauncher { permissions ->
            Log.d(TAG, "Launching permission request for ${permissions.size} permissions")
            permissions.forEach { Log.d(TAG, "  - $it") }
            try {
                requestPermissions.launch(permissions)
                Log.d(TAG, "Permission request launch call completed")
            } catch (e: Exception) {
                Log.e(TAG, "Exception launching permission request, trying fallback", e)
                openHealthConnectSettings()
            }
        }

        // Set fallback launcher for opening Health Connect settings directly
        viewModel.setOpenHealthConnectSettingsLauncher {
            openHealthConnectSettings()
        }

        setContent {
            RowingSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SessionListScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    /**
     * Open Health Connect app settings directly
     */
    private fun openHealthConnectSettings() {
        try {
            val intent = Intent(HEALTH_CONNECT_SETTINGS_ACTION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Health Connect settings", e)
            // Try to open the Health Connect app directly
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Log.e(TAG, "Health Connect app not found")
                    // Open Play Store as last resort
                    val playStoreIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                    startActivity(playStoreIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open Health Connect app", e2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Checking permissions")
        viewModel.checkHealthConnectPermissions()
        viewModel.refreshSessions()
    }
}
