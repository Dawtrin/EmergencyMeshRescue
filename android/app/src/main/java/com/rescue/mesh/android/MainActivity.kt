package com.rescue.mesh.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rescue.mesh.android.data.local.UserPreferences
import com.rescue.mesh.android.data.repository.MeshRepository
import com.rescue.mesh.android.hardware.FlashlightManager
import com.rescue.mesh.android.hardware.SirenSoundPlayer
import com.rescue.mesh.android.ui.screens.*
import com.rescue.mesh.android.ui.theme.EmergencyMeshAndroidTheme
import com.rescue.mesh.android.ui.theme.OledBlack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var repository: MeshRepository
    private lateinit var flashlightManager: FlashlightManager
    private lateinit var sirenPlayer: SirenSoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MeshRepository.getInstance(applicationContext)
        flashlightManager = FlashlightManager(this)
        sirenPlayer = SirenSoundPlayer(this)

        val startRoute = runBlocking {
            val profile = repository.userPreferences.userProfileFlow.first()
            if (profile.isOnboardingComplete) "sos_home" else "onboarding"
        }

        setContent {
            EmergencyMeshAndroidTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = Modifier.fillMaxSize().background(OledBlack)
                ) {
                    composable("onboarding") {
                        EmergencyOnboardingScreen(
                            userPreferences = repository.userPreferences,
                            onOnboardingComplete = {
                                navController.navigate("sos_home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("sos_home") {
                        SosHomeScreen(
                            repository = repository,
                            flashlightManager = flashlightManager,
                            sirenPlayer = sirenPlayer,
                            onNavigateToCompass = { navController.navigate("compass") },
                            onNavigateToRelay = { navController.navigate("relay_monitor") },
                            onNavigateToHistory = { navController.navigate("incident_history") }
                        )
                    }

                    composable("compass") {
                        SurvivalCompassScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("relay_monitor") {
                        RelayMonitorScreen(
                            repository = repository,
                            onNavigateToSos = { navController.popBackStack() },
                            onNavigateToHistory = { navController.navigate("incident_history") }
                        )
                    }

                    composable("incident_history") {
                        IncidentHistoryScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.registerP2p()
    }

    override fun onDestroy() {
        flashlightManager.shutdown()
        sirenPlayer.release()
        repository.cleanup()
        super.onDestroy()
    }
}