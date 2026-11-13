package com.rescue.mesh.android.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.local.IncidentEntity
import com.rescue.mesh.android.data.model.MeshPacket
import com.rescue.mesh.android.data.repository.MeshRepository
import com.rescue.mesh.android.ui.components.TacticalCard
import com.rescue.mesh.android.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình Nhật ký Cứu nạn & Lệnh Điều phối (Incident & Dispatch Log).
 *
 * Tính năng:
 *   - Lọc theo Tab: "TẤT CẢ", "SOS ĐÃ PHÁT", "LỆNH CHỈ HUY".
 *   - Hiển thị chi tiết: Thời gian, Loại thảm họa, Tọa độ, Số nạn nhân, Mã gói tin, Checksum.
 *   - Trạng thái truyền tin: ✅ THÀNH CÔNG vs ⚠️ CHƯA GỬI ĐƯỢC.
 *   - Nút "Thử lại ngay" (Instant Retry) phát lại từng gói tin lỗi riêng biệt.
 */
@Composable
fun IncidentHistoryScreen(
    repository: MeshRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tất cả, 1: SOS, 2: Dispatch
    val tabs = listOf("T\u1ea4T C\u1ea2", "SOS \u0110\u00c3 PH\u00c1T", "L\u1ec6NH CH\u1ec8 HUY")

    val allIncidents by repository.incidentDao.getAllIncidents().collectAsState(initial = emptyList())
    val outgoingSos by repository.incidentDao.getOutgoingSos().collectAsState(initial = emptyList())
    val incomingDispatches by repository.incidentDao.getIncomingDispatches().collectAsState(initial = emptyList())

    val displayedIncidents = when (selectedTab) {
        1 -> outgoingSos
        2 -> incomingDispatches
        else -> allIncidents
    }

    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss - dd/MM", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigateBack() },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DarkElevated)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NH\u1eacT K\u00dd C\u1ee8U N\u1ea0N",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "L\u01b0u tr\u1eef ngo\u1ea1i tuy\u1ebfn trong Room Database",
                        style = Typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) DispatchBlue else Color.Transparent,
                        label = "tab_bg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tabBg)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Incident List
            if (displayedIncidents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ch\u01b0a c\u00f3 g\u00f3i tin n\u00e0o \u0111\u01b0\u1ee3c ghi nh\u1eadn",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedIncidents, key = { it.packetId }) { incident ->
                        IncidentItemCard(
                            incident = incident,
                            dateFormatter = dateFormatter,
                            onRetry = {
                                scope.launch {
                                    val res = repository.retrySingleIncident(incident)
                                    if (res.success) {
                                        Toast.makeText(context, "Ph\u00e1t l\u1ea1i g\u00f3i tin th\u00e0nh c\u00f4ng!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Th\u1ea5t b\u1ea1i: ${res.errorMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentItemCard(
    incident: IncidentEntity,
    dateFormatter: SimpleDateFormat,
    onRetry: () -> Unit
) {
    val isSos = incident.packetType == MeshPacket.TYPE_SOS_DATA
    val typeColor = if (isSos) CriticalRed else DispatchBlue
    val statusColor = if (incident.sendSuccess) ConnectedGreen else HighAmber

    TacticalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = typeColor.copy(alpha = 0.3f),
        elevation = 6.dp
    ) {
        Column {
            // Header Row: Type + Status + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isSos) "SOS \u2022 ${incident.alertType}" else "L\u1ec6NH CH\u1ec8 HUY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = typeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${incident.packetId}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = dateFormatter.format(Date(incident.timestamp)),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message text
            Text(
                text = incident.message.ifBlank { "Kh\u00f4ng c\u00f3 l\u1eddi nh\u1eafn k\u00e8m theo" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Victim Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (incident.latitude != 0.0) {
                    Text(
                        text = String.format("\ud83d\udccd %.5f, %.5f", incident.latitude, incident.longitude),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(text = "T\u1ecda \u0111\u1ed9 tr\u1ea1m g\u1ed1c", fontSize = 11.sp, color = TextSecondary)
                }

                if (isSos) {
                    Text(
                        text = "${incident.victimCount} n\u1ea1n nh\u00e2n",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Bar & Retry Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (incident.sendSuccess) "\u0110\u00e3 truy\u1ec1n th\u00e0nh c\u00f4ng" else "Ch\u01b0a g\u1eedi \u0111\u01b0\u1ee3c (Trong h\u00e0ng \u0111\u1ee3i)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (!incident.sendSuccess && incident.isOutgoing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CriticalRed.copy(alpha = 0.25f))
                            .clickable { onRetry() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TH\u1eec L\u1ea0I NGAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CriticalRed
                        )
                    }
                }
            }
        }
    }
}