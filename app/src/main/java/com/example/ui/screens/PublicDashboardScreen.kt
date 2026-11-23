package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ClassroomRoom
import com.example.data.HygieneStatus
import com.example.data.RoomHealthStatus
import com.example.data.SchoolRepository
import com.example.data.UiState
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusErrorFg
import com.example.ui.theme.StatusGoodBg
import com.example.ui.theme.StatusGoodBorder
import com.example.ui.theme.StatusGoodFg
import com.example.ui.theme.StatusWarnBg
import com.example.ui.theme.StatusWarnBorder
import com.example.ui.theme.StatusWarnFg
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PublicDashboardScreen(
    roomsState: UiState<List<ClassroomRoom>>,
    seedError: String?,
    selectedFloor: Int?,
    onSelectFloor: (Int?) -> Unit,
    onRoomClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val calendar = remember(clockMillis) {
        Calendar.getInstance().apply { timeInMillis = clockMillis }
    }
    val todayDate = remember(clockMillis) { SchoolRepository.todayDateString() }
    val dateTimeText = remember(clockMillis) {
        SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.US).format(Date(clockMillis))
    }
    val minutesOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val inDutyWindow = minutesOfDay in 420..515

    when (roomsState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = AcademicBlue)
                    Text(
                        text = "Đang đồng bộ 24 phòng học...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = StatusErrorFg,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = roomsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(onClick = onRetry) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thử tải lại")
                    }
                }
            }
        }

        is UiState.Success -> {
            val rooms = roomsState.data
            if (rooms.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (seedError != null) {
                            Text(
                                text = seedError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusErrorFg,
                            )
                            Button(onClick = onRetry) {
                                Text("Khởi tạo lại dữ liệu 24 phòng")
                            }
                        } else {
                            CircularProgressIndicator(color = AcademicBlue)
                            Text(
                                text = "Đang chuẩn bị sơ đồ 24 phòng học...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                return
            }

            val totalAbsent = rooms.sumOf { it.effectiveAbsences(todayDate).size }
            val totalClean = rooms.count { it.effectiveHygieneStatus(todayDate) == HygieneStatus.SACH_SE }
            val totalBrokenRooms = rooms.count { it.brokenDeviceCount() > 0 }
            val totalConfirmed = rooms.count { it.monitorConfirmed() }

            val floorsToDisplay = if (selectedFloor == null) listOf(3, 2, 1) else listOf(selectedFloor)

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Live date/time + Period 1-2 duty window indicator
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AcademicBlue,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Hôm nay, $dateTimeText",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Tổng hợp 3 tầng • 24 phòng học",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AcademicBlueLight,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (inDutyWindow) StatusGoodFg else Color.White.copy(alpha = 0.16f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = if (inDutyWindow) {
                                            stringResource(R.string.duty_window_active)
                                        } else {
                                            stringResource(R.string.duty_window_badge)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                // 4 Summary KPI cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SummaryStatCard(
                            value = "$totalAbsent",
                            label = "HS Vắng",
                            accentColor = StatusErrorFg,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryStatCard(
                            value = "$totalClean/${rooms.size}",
                            label = "Lớp Sạch",
                            accentColor = StatusGoodFg,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryStatCard(
                            value = "$totalBrokenRooms",
                            label = "Phòng Hư TB",
                            accentColor = StatusWarnFg,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryStatCard(
                            value = "$totalConfirmed/${rooms.size}",
                            label = "LT Xác nhận",
                            accentColor = AcademicBlue,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Floor filter chips + status color legend
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FloorFilterChip(
                                    label = "Tất cả (24)",
                                    selected = selectedFloor == null,
                                    onClick = { onSelectFloor(null) },
                                    tag = "floor_filter_all",
                                )
                                FloorFilterChip(
                                    label = "Tầng 3 (12A)",
                                    selected = selectedFloor == 3,
                                    onClick = { onSelectFloor(3) },
                                    tag = "floor_filter_3",
                                )
                                FloorFilterChip(
                                    label = "Tầng 2 (11A)",
                                    selected = selectedFloor == 2,
                                    onClick = { onSelectFloor(2) },
                                    tag = "floor_filter_2",
                                )
                                FloorFilterChip(
                                    label = "Tầng 1 (10A)",
                                    selected = selectedFloor == 1,
                                    onClick = { onSelectFloor(1) },
                                    tag = "floor_filter_1",
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LegendItem(color = StatusGoodFg, label = "Đạt & Đã XN")
                                LegendItem(color = StatusWarnFg, label = "Chờ XN / Chưa trực")
                                LegendItem(color = StatusErrorFg, label = "Có TB hư / Nhắc nhở")
                            }
                        }
                    }
                }

                // Floor grids (3 floors x 8 classrooms)
                floorsToDisplay.forEach { floorNum ->
                    val floorRooms = rooms.filter { it.floorInt() == floorNum }
                    item(key = "floor_$floorNum") {
                        FloorSectionCard(
                            floorNumber = floorNum,
                            floorRooms = floorRooms,
                            todayDate = todayDate,
                            onRoomClick = onRoomClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FloorFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AcademicBlue,
            selectedLabelColor = Color.White,
        ),
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FloorSectionCard(
    floorNumber: Int,
    floorRooms: List<ClassroomRoom>,
    todayDate: String,
    onRoomClick: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tầng $floorNumber (${floorRooms.size} phòng)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Chạm để xem chi tiết",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 560.dp) 4 else 2
                val rows = floorRooms.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowRooms ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowRooms.forEach { room ->
                                ClassroomGridTile(
                                    room = room,
                                    todayDate = todayDate,
                                    onClick = { onRoomClick(room.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - rowRooms.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassroomGridTile(
    room: ClassroomRoom,
    todayDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val absentCount = room.effectiveAbsences(todayDate).size
    val hygiene = room.effectiveHygieneStatus(todayDate)
    val brokenCount = room.brokenDeviceCount()
    val confirmed = room.monitorConfirmed()
    val health = room.overallHealth(todayDate)

    val (bgColor, borderColor, dotColor) = when (health) {
        RoomHealthStatus.OPTIMAL -> Triple(StatusGoodBg, StatusGoodBorder, StatusGoodFg)
        RoomHealthStatus.PENDING -> Triple(StatusWarnBg, StatusWarnBorder, StatusWarnFg)
        RoomHealthStatus.ISSUE -> Triple(StatusErrorBg, StatusErrorBorder, StatusErrorFg)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.testTag("room_card_${room.className}"),
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = room.className,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = room.roomCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Vắng: $absentCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (absentCount > 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (absentCount > 0) StatusErrorFg else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (hygiene) {
                        HygieneStatus.SACH_SE -> "✓ Sạch"
                        HygieneStatus.CAN_NHAC_NHO -> "✕ Nhắc nhở"
                        HygieneStatus.CHUA_KIEM_TRA -> "⏳ Chưa trực"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (hygiene) {
                        HygieneStatus.SACH_SE -> StatusGoodFg
                        HygieneStatus.CAN_NHAC_NHO -> StatusErrorFg
                        HygieneStatus.CHUA_KIEM_TRA -> StatusWarnFg
                    },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (brokenCount > 0) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = StatusErrorFg,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Text(
                        text = if (brokenCount > 0) "Hư $brokenCount TB" else "TB: Tốt",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (brokenCount > 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (brokenCount > 0) StatusErrorFg else StatusGoodFg,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (confirmed) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AcademicBlue,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = if (confirmed) "LT XN" else "Chờ XN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (confirmed) AcademicBlue else StatusWarnFg,
                    )
                }
            }
        }
    }
}
