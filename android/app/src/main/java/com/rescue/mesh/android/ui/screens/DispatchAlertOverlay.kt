package com.rescue.mesh.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.model.MeshPacket
import com.rescue.mesh.android.ui.theme.*

@Composable
fun DispatchAlertOverlay(
    dispatchPacket: MeshPacket?,
    onAcknowledge: (MeshPacket) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = dispatchPacket != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (dispatchPacket != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = DispatchBlue)
                        .clip(RoundedCornerShape(24.dp)).background(DarkSurface)
                        .border(2.dp, DispatchBlue, RoundedCornerShape(24.dp)).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(DispatchBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DispatchBlue, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("L\u1ec6NH T\u1eea TR\u1ea0M CH\u1ec8 HUY", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = DispatchBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(dispatchPacket.payload?.senderName ?: "Tr\u01b0\u1edfng Tr\u1ea1m C\u1ee9u H\u1ed9", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkElevated).padding(16.dp)) {
                            Text(dispatchPacket.payload?.message ?: "\u0110\u1ed9i c\u1ee9u h\u1ed9 \u0111ang tr\u00ean \u0111\u01b0\u1eddng t\u1edbi v\u1ecb tr\u00ed c\u1ee7a b\u1ea1n.", fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { onAcknowledge(dispatchPacket) }, colors = ButtonDefaults.buttonColors(containerColor = ConnectedGreen), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("\u0110\u00c3 NH\u1eacN & PH\u1ea2N H\u1ed2I (ACK)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}