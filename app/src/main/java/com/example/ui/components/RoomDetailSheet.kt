package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AbsenceType
import com.example.data.ClassroomRoom
import com.example.data.DeviceState
import com.example.data.HygieneStatus
import com.example.data.ROOM_DEVICE_SPECS
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusErrorFg
import com.example.ui.theme.StatusGoodBg
import com.example.ui.theme.StatusGoodBorder
import com.example.ui.theme.StatusGoodFg
import com.example.ui.theme.StatusWarnBg
import com.example.ui.theme.StatusWarnFg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailSheet(
    room: ClassroomRoom,
    todayDate: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val absences = room.effectiveAbsences(todayDate)
    val hygieneStatus = room.effectiveHygieneStatus(todayDate)
    val hygieneTime = room.effectiveHygieneTimeText(todayDate)
    val hygieneOnTime = room.effectiveHygieneOnTime(todayDate)
    val hygieneNote = room.effectiveHygieneNote(todayDate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Lớp ${room.className} (${room.roomCode})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Dãy phòng học • Tầng ${room.floorInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Đóng chi tiết phòng",
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 1. Học sinh vắng
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. TÌNH HÌNH HỌC SINH VẮNG HÔM NAY (${absences.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (absences.isEmpty()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = StatusGoodFg,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Sĩ số đầy đủ (0 học sinh vắng)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusGoodFg,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            absences.forEachIndexed { idx, entry ->
                                val type = entry.typeEnum()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.studentName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (entry.reason.isNotBlank()) {
                                            Text(
                                                text = "Lý do: ${entry.reason}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (type == AbsenceType.KHONG_PHEP) StatusErrorBg else AcademicBlue.copy(alpha = 0.12f),
                                    ) {
                                        Text(
                                            text = type.displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (type == AbsenceType.KHONG_PHEP) StatusErrorFg else AcademicBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                if (idx < absences.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Vệ sinh Tiết 1-2 (7h00 - 8h35)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. VỆ SINH LỚP HỌC (TIẾT 1–2: 07:00–08:35)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when (hygieneStatus) {
                        HygieneStatus.SACH_SE -> StatusGoodBg
                        HygieneStatus.CAN_NHAC_NHO -> StatusErrorBg
                        HygieneStatus.CHUA_KIEM_TRA -> StatusWarnBg
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = hygieneStatus.displayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = when (hygieneStatus) {
                                    HygieneStatus.SACH_SE -> StatusGoodFg
                                    HygieneStatus.CAN_NHAC_NHO -> StatusErrorFg
                                    HygieneStatus.CHUA_KIEM_TRA -> StatusWarnFg
                                },
                            )
                            if (hygieneTime.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "$hygieneTime (${if (hygieneOnTime) "Đúng giờ" else "Ngoài giờ"})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (hygieneStatus == HygieneStatus.CHUA_KIEM_TRA) {
                            Text(
                                text = "Nhóm trực nhật chưa gửi phiếu kiểm tra hôm nay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusWarnFg,
                            )
                        } else if (hygieneNote.isNotBlank()) {
                            Text(
                                text = "Ghi chú: $hygieneNote",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // 3. Thiết bị sử dụng trong phòng
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "3. THIẾT BỊ PHÒNG HỌC (8 MỤC)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (room.monitorConfirmed()) {
                            "✓ ${room.deviceConfirmedBy} (${room.deviceConfirmedAtText})"
                        } else {
                            "⏳ Chờ Lớp trưởng xác nhận"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (room.monitorConfirmed()) AcademicBlue else StatusWarnFg,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // 4 rows x 2 columns grid for the 8 devices
                val chunked = ROOM_DEVICE_SPECS.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { spec ->
                                val isGood = room.deviceState(spec.key) == DeviceState.GOOD
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGood) StatusGoodBg else StatusErrorBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isGood) StatusGoodBorder else StatusErrorBorder,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = spec.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (isGood) "Tốt" else "Hư",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGood) StatusGoodFg else StatusErrorFg,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (room.deviceNote.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = StatusErrorFg,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Ghi chú thiết bị: ${room.deviceNote}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = StatusErrorFg,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
