package com.rescue.mesh.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.local.UserPreferences
import com.rescue.mesh.android.ui.components.TacticalCard
import com.rescue.mesh.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EmergencyOnboardingScreen(
    userPreferences: UserPreferences,
    onOnboardingComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedBloodType by remember { mutableStateOf("O+") }
    var medicalNotes by remember { mutableStateOf("") }
    var victimCount by remember { mutableIntStateOf(1) }

    val bloodTypes = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    fun requestAppPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    // Spring scale for cards entering
    var appeared by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_spring"
    )
    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CriticalRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "H\u1ed2 S\u01a0 C\u1ee8U N\u1ea0N", style = Typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                    Text(text = "Thi\u1ebft l\u1eadp ngo\u1ea1i tuy\u1ebfn cho t\u00ecnh hu\u1ed1ng kh\u1ea9n c\u1ea5p", style = Typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Th\u00f4ng tin c\u00e1 nh\u00e2n
            TacticalCard(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
                Column {
                    Text("TH\u00d4NG TIN C\u00c1 NH\u00c2N", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HighAmber))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fullName, onValueChange = { fullName = it },
                        label = { Text("H\u1ecd v\u00e0 t\u00ean c\u1ee7a b\u1ea1n", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DispatchBlue, unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("S\u0110T ng\u01b0\u1eddi th\u00e2n li\u00ean h\u1ec7 kh\u1ea9n c\u1ea5p", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DispatchBlue, unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nh\u00f3m m\u00e1u & Y t\u1ebf
            TacticalCard(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
                Column {
                    Text("NH\u00d3M M\u00c1U & Y T\u1ebe \u0110\u1eb6C BI\u1ec6T", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CriticalRed))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        bloodTypes.take(4).forEach { bt ->
                            val sel = selectedBloodType == bt
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) CriticalRed else DarkElevated)
                                    .clickable { selectedBloodType = bt }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(bt, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) Color.White else TextSecondary) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        bloodTypes.takeLast(4).forEach { bt ->
                            val sel = selectedBloodType == bt
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) CriticalRed else DarkElevated)
                                    .clickable { selectedBloodType = bt }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(bt, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) Color.White else TextSecondary) }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = medicalNotes, onValueChange = { medicalNotes = it },
                        label = { Text("B\u1ec7nh n\u1ec1n / D\u1ecb \u1ee9ng thu\u1ed1c / Hen suy\u1ec5n...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = CriticalRed) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CriticalRed, unfocusedBorderColor = CardBorder
                        ),
                        maxLines = 2, modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C\u1ea5p quy\u1ec1n
            TacticalCard(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
                Column {
                    Text("C\u1ea4P QUY\u1ec0N HO\u1ea0T \u0110\u1ed8NG", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ConnectedGreen))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u1ee8ng d\u1ee5ng c\u1ea7n quy\u1ec1n GPS V\u1ec7 tinh, \u0110\u00e8n Flash v\u00e0 Wi-Fi Direct \u0111\u1ec3 t\u1ef1 \u0111\u1ed9ng ph\u00e1t hi\u1ec7n tr\u1ea1m c\u1ee9u n\u1ea1n g\u1ea7n b\u1ea1n.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { requestAppPermissions() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ConnectedGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("C\u1ea5p Quy\u1ec1n Ngay (1-Ch\u1ea1m)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        userPreferences.saveProfile(
                            fullName = fullName.ifBlank { "N\u1ea1n nh\u00e2n" },
                            phone = phone, bloodType = selectedBloodType,
                            medicalNotes = medicalNotes, victimCount = victimCount
                        )
                        onOnboardingComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DispatchBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("L\u01afU H\u1ed2 S\u01a0 & B\u1eaeT \u0110\u1ea6U", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { scope.launch { userPreferences.completeOnboarding(skip = true); onOnboardingComplete() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("\u26a1 B\u1ece QUA V\u00c0 V\u00c0O KH\u1ea8N C\u1ea4P NGAY (SOS)", color = CriticalRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}