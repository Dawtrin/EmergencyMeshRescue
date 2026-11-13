package com.rescue.mesh.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "emergency_mesh_prefs")

/**
 * Quản lý lưu trữ cấu hình và Hồ sơ Cứu nạn Ngoại tuyến sử dụng Jetpack DataStore.
 *
 * Dữ liệu được lưu cục bộ hoàn toàn KHÔNG cần Internet, phục vụ tình huống
 * mạng viễn thông sập hoàn toàn trong thảm họa thiên nhiên.
 */
class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_FULL_NAME = stringPreferencesKey("full_name")
        val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        val KEY_BLOOD_TYPE = stringPreferencesKey("blood_type")
        val KEY_MEDICAL_NOTES = stringPreferencesKey("medical_notes")
        val KEY_VICTIM_COUNT = intPreferencesKey("victim_count")
        val KEY_NODE_MODE = stringPreferencesKey("node_mode")
        val KEY_BASE_STATION_IP = stringPreferencesKey("base_station_ip")
        val KEY_BASE_STATION_PORT = intPreferencesKey("base_station_port")
        val KEY_RELAY_PORT = intPreferencesKey("relay_port")
        val KEY_ULTRA_BATTERY_SAVER = booleanPreferencesKey("ultra_battery_saver")
    }

    data class UserProfile(
        val isOnboardingComplete: Boolean = false,
        val fullName: String = "N\u1ea1n nh\u00e2n v\u00f4 danh",
        val phoneNumber: String = "",
        val bloodType: String = "O+",
        val medicalNotes: String = "",
        val victimCount: Int = 1,
        val nodeMode: String = "VICTIM",
        val baseStationIp: String = "192.168.1.100",
        val baseStationPort: Int = 8888,
        val relayPort: Int = 8889,
        val ultraBatterySaver: Boolean = false
    )

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            isOnboardingComplete = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            fullName = prefs[KEY_FULL_NAME] ?: "N\u1ea1n nh\u00e2n v\u00f4 danh",
            phoneNumber = prefs[KEY_PHONE_NUMBER] ?: "",
            bloodType = prefs[KEY_BLOOD_TYPE] ?: "O+",
            medicalNotes = prefs[KEY_MEDICAL_NOTES] ?: "",
            victimCount = prefs[KEY_VICTIM_COUNT] ?: 1,
            nodeMode = prefs[KEY_NODE_MODE] ?: "VICTIM",
            baseStationIp = prefs[KEY_BASE_STATION_IP] ?: "192.168.1.100",
            baseStationPort = prefs[KEY_BASE_STATION_PORT] ?: 8888,
            relayPort = prefs[KEY_RELAY_PORT] ?: 8889,
            ultraBatterySaver = prefs[KEY_ULTRA_BATTERY_SAVER] ?: false
        )
    }

    suspend fun saveProfile(
        fullName: String,
        phone: String,
        bloodType: String,
        medicalNotes: String,
        victimCount: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FULL_NAME] = fullName
            prefs[KEY_PHONE_NUMBER] = phone
            prefs[KEY_BLOOD_TYPE] = bloodType
            prefs[KEY_MEDICAL_NOTES] = medicalNotes
            prefs[KEY_VICTIM_COUNT] = victimCount
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun completeOnboarding(skip: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
            if (skip && prefs[KEY_FULL_NAME] == null) {
                prefs[KEY_FULL_NAME] = "N\u1ea1n nh\u00e2n kh\u1ea9n c\u1ea5p"
            }
        }
    }

    suspend fun setNodeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_NODE_MODE] = mode }
    }

    suspend fun updateNetworkConfig(ip: String, port: Int, relayPort: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_STATION_IP] = ip
            prefs[KEY_BASE_STATION_PORT] = port
            prefs[KEY_RELAY_PORT] = relayPort
        }
    }

    suspend fun setUltraBatterySaver(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ULTRA_BATTERY_SAVER] = enabled }
    }
}