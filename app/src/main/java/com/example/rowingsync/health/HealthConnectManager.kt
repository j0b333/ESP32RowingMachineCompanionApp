package com.example.rowingsync.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.example.rowingsync.data.SessionDetail
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Manager for Health Connect integration
 * Handles permissions and writing exercise sessions
 */
class HealthConnectManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HealthConnectManager"
        
        /**
         * Required permissions for the app
         */
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getWritePermission(PowerRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class)
        )
    }
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    /**
     * Check if Health Connect is available on this device
     */
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }
    
    /**
     * Check if all required permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            PERMISSIONS.all { it in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }
    
    /**
     * Sync a rowing session to Health Connect
     * 
     * @param session The session detail from ESP32
     * @return true if sync was successful
     */
    suspend fun syncSession(session: SessionDetail): Boolean {
        // Check if Health Connect is available
        if (!isAvailable()) {
            Log.e(TAG, "Health Connect is not available")
            return false
        }
        
        // Check permissions before attempting to write
        if (!hasAllPermissions()) {
            Log.e(TAG, "Missing required Health Connect permissions")
            return false
        }
        
        return try {
            val startTime = Instant.ofEpochMilli(session.startTime)
            val endTime = startTime.plusSeconds(session.duration.toLong())
            val zoneId = ZoneId.systemDefault()
            
            // Create exercise session record (Rowing)
            val exerciseSession = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneId.rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = zoneId.rules.getOffset(endTime),
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
                title = "Rowing Session"
            )
            
            // Create distance record
            val distanceRecord = DistanceRecord(
                startTime = startTime,
                startZoneOffset = zoneId.rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = zoneId.rules.getOffset(endTime),
                distance = Length.meters(session.distance.toDouble())
            )
            
            // Create calories record
            val caloriesRecord = TotalCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = zoneId.rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = zoneId.rules.getOffset(endTime),
                energy = Energy.kilocalories(session.calories.toDouble())
            )
            
            // Build list of records to insert
            val records = mutableListOf<Record>(exerciseSession, distanceRecord, caloriesRecord)
            
            // Add heart rate records if available
            if (session.heartRateSamples.isNotEmpty()) {
                val heartRateSamples = session.heartRateSamples.map { sample ->
                    HeartRateRecord.Sample(
                        time = Instant.ofEpochMilli(sample.time),
                        beatsPerMinute = sample.bpm.toLong()
                    )
                }
                
                val heartRateRecord = HeartRateRecord(
                    startTime = startTime,
                    startZoneOffset = zoneId.rules.getOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneId.rules.getOffset(endTime),
                    samples = heartRateSamples
                )
                records.add(heartRateRecord)
            }
            
            // Add power records if available
            if (session.powerSamples.isNotEmpty()) {
                val powerSamples = session.powerSamples.map { sample ->
                    PowerRecord.Sample(
                        time = Instant.ofEpochMilli(sample.time),
                        power = androidx.health.connect.client.units.Power.watts(sample.watts.toDouble())
                    )
                }

                val powerRecord = PowerRecord(
                    startTime = startTime,
                    startZoneOffset = zoneId.rules.getOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneId.rules.getOffset(endTime),
                    samples = powerSamples
                )
                records.add(powerRecord)
            }

            // Add speed records if available
            if (session.speedSamples.isNotEmpty()) {
                val speedSamples = session.speedSamples.map { sample ->
                    SpeedRecord.Sample(
                        time = Instant.ofEpochMilli(sample.time),
                        speed = androidx.health.connect.client.units.Velocity.metersPerSecond(sample.metersPerSecond.toDouble())
                    )
                }

                val speedRecord = SpeedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneId.rules.getOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneId.rules.getOffset(endTime),
                    samples = speedSamples
                )
                records.add(speedRecord)
            }

            // Insert all records
            healthConnectClient.insertRecords(records)
            
            Log.i(TAG, "Successfully synced session ${session.id} to Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync session to Health Connect", e)
            false
        }
    }

    /**
     * Data class representing an exercise session stored in Health Connect
     */
    data class HealthConnectExercise(
        val id: String,
        val title: String?,
        val exerciseType: Int,
        val startTime: Instant,
        val endTime: Instant,
        val durationMinutes: Long
    ) {
        fun getExerciseTypeName(): String {
            return when (exerciseType) {
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Rowing Machine"
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "Rowing"
                else -> "Exercise ($exerciseType)"
            }
        }
    }

    /**
     * Read all exercise sessions written by this app from Health Connect
     *
     * @param daysBack Number of days to look back (default 365)
     * @return List of exercise sessions
     */
    suspend fun getExerciseSessions(daysBack: Long = 365): List<HealthConnectExercise> {
        if (!isAvailable()) {
            Log.e(TAG, "Health Connect is not available")
            return emptyList()
        }

        if (!hasAllPermissions()) {
            Log.e(TAG, "Missing required Health Connect permissions")
            return emptyList()
        }

        return try {
            val endTime = Instant.now()
            val startTime = endTime.minus(daysBack, ChronoUnit.DAYS)

            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)

            // Filter to only include rowing sessions (from our app)
            response.records
                .filter { record ->
                    record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE ||
                    record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_ROWING
                }
                .map { record ->
                    HealthConnectExercise(
                        id = record.metadata.id,
                        title = record.title,
                        exerciseType = record.exerciseType,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        durationMinutes = ChronoUnit.MINUTES.between(record.startTime, record.endTime)
                    )
                }
                .sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read exercise sessions from Health Connect", e)
            emptyList()
        }
    }

    /**
     * Delete an exercise session and its associated records from Health Connect
     *
     * @param exerciseId The ID of the exercise session to delete
     * @return true if deletion was successful
     */
    suspend fun deleteExerciseSession(exerciseId: String): Boolean {
        if (!isAvailable()) {
            Log.e(TAG, "Health Connect is not available")
            return false
        }

        if (!hasAllPermissions()) {
            Log.e(TAG, "Missing required Health Connect permissions")
            return false
        }

        return try {
            // First, find the exercise session to get its time range
            val endTime = Instant.now()
            val startTime = endTime.minus(365, ChronoUnit.DAYS)

            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)
            val sessionToDelete = response.records.find { it.metadata.id == exerciseId }

            if (sessionToDelete == null) {
                Log.e(TAG, "Exercise session not found: $exerciseId")
                return false
            }

            // Delete the exercise session record
            healthConnectClient.deleteRecords(
                ExerciseSessionRecord::class,
                recordIdsList = listOf(exerciseId),
                clientRecordIdsList = emptyList()
            )

            // Also try to delete associated records in the same time range
            // These may have been written together with the exercise session
            val sessionStart = sessionToDelete.startTime
            val sessionEnd = sessionToDelete.endTime
            val timeRange = TimeRangeFilter.between(sessionStart, sessionEnd)

            // Delete distance records in this time range
            try {
                healthConnectClient.deleteRecords(
                    DistanceRecord::class,
                    timeRange
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete distance records", e)
            }

            // Delete calories records in this time range
            try {
                healthConnectClient.deleteRecords(
                    TotalCaloriesBurnedRecord::class,
                    timeRange
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete calories records", e)
            }

            // Delete heart rate records in this time range
            try {
                healthConnectClient.deleteRecords(
                    HeartRateRecord::class,
                    timeRange
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete heart rate records", e)
            }

            // Delete power records in this time range
            try {
                healthConnectClient.deleteRecords(
                    PowerRecord::class,
                    timeRange
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete power records", e)
            }

            // Delete speed records in this time range
            try {
                healthConnectClient.deleteRecords(
                    SpeedRecord::class,
                    timeRange
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete speed records", e)
            }

            Log.i(TAG, "Successfully deleted exercise session $exerciseId and associated records")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete exercise session from Health Connect", e)
            false
        }
    }

    /**
     * Delete all rowing exercise sessions from Health Connect
     *
     * @return Number of sessions deleted
     */
    suspend fun deleteAllRowingSessions(): Int {
        val sessions = getExerciseSessions()
        var deletedCount = 0

        for (session in sessions) {
            if (deleteExerciseSession(session.id)) {
                deletedCount++
            }
        }

        Log.i(TAG, "Deleted $deletedCount rowing sessions from Health Connect")
        return deletedCount
    }
}
