package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AbsenceType
import com.example.data.ClassroomRoom
import com.example.data.DeviceState
import com.example.data.HygieneStatus
import com.example.data.ROOM_DEVICE_SPECS
import com.example.data.SchoolRepository
import com.example.data.SchoolRole
import com.example.data.UiState
import com.example.ui.RoleAuthMethod
import com.example.ui.RoleReportUiState
import com.example.ui.RoleReportViewModel
import com.example.ui.RoleWorkspaceTab
import com.example.ui.components.PatternLockView
import com.example.ui.components.PinPadView
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorFg
import com.example.ui.theme.StatusGoodBg
import com.example.ui.theme.StatusGoodBorder
import com.example.ui.theme.StatusGoodFg
import com.example.ui.theme.StatusWarnBg
import com.example.ui.theme.StatusWarnFg

@Composable
fun RoleReportScreen(
    roomsState: UiState<List<ClassroomRoom>>,
    roleUiState: RoleReportUiState,
    viewModel: RoleReportViewModel,
    modifier: Modifier = Modifier,
) {
    val rooms = (roomsState as? UiState.Success)?.data.orEmpty()
    val todayDate = SchoolRepository.todayDateString()
    val activeSession = roleUiState.activeSession

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (activeSession == null) {
                item {
                    RoleLoginCard(
                        rooms = rooms,
                        uiState = roleUiState,
                        viewModel = viewModel,
                    )
                }
            } else {
                val activeRoom = rooms.firstOrNull { it.id == activeSession.roomId }
                if (activeRoom != null) {
                    item {
                        RoleWorkspaceSection(
                            room = activeRoom,
                            role = activeSession.role,
                            uiState = roleUiState,
                            todayDate = todayDate,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleLoginCard(
    rooms: List<ClassroomRoom>,
    uiState: RoleReportUiState,
    viewModel: RoleReportViewModel,
) {
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    val selectedRoom = rooms.firstOrNull { it.id == uiState.selectedRoomId }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = AcademicBlue,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Đăng nhập theo Chức vụ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Chọn lớp, chức vụ và xác thực nhanh để gửi báo cáo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Class & Role selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { classDropdownExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("role_login_class_selector"),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lớp / Phòng",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = selectedRoom?.let { "${it.className} (${it.roomCode})" } ?: uiState.selectedRoomId,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false },
                    ) {
                        val listToShow = rooms.ifEmpty {
                            SchoolRepository.ALL_ROOM_IDS.map { ClassroomRoom(id = it, className = it, roomCode = "") }
                        }
                        listToShow.forEach { room ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (room.roomCode.isNotBlank()) "${room.className} (${room.roomCode})" else room.className
                                    )
                                },
                                onClick = {
                                    viewModel.selectLoginRoom(room.id)
                                    classDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { roleDropdownExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("role_login_role_selector"),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chức vụ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = uiState.selectedRole.displayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                    ) {
                        SchoolRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.displayLabel) },
                                onClick = {
                                    viewModel.selectLoginRole(role)
                                    roleDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Switch Pattern vs PIN
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.authMethod == RoleAuthMethod.PATTERN,
                    onClick = { viewModel.switchAuthMethod(RoleAuthMethod.PATTERN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("auth_mode_pattern_tab"),
                ) {
                    Text("Mẫu hình vẽ (3×3)", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = uiState.authMethod == RoleAuthMethod.PIN,
                    onClick = { viewModel.switchAuthMethod(RoleAuthMethod.PIN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("auth_mode_pin_tab"),
                ) {
                    Text("Mã PIN 4 số", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (uiState.authMethod == RoleAuthMethod.PATTERN) {
                Text(
                    text = "Vẽ mẫu hình để mở khóa (Mặc định chữ L: 1 → 4 → 7 → 8 → 9)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicBlue,
                    textAlign = TextAlign.Center,
                )
                PatternLockView(
                    isError = uiState.authError != null,
                    onPatternComplete = { seq -> viewModel.verifyPattern(seq, rooms) },
                )
            } else {
                val hintPin = selectedRoom?.rolePin?.ifBlank { "1234" } ?: "1234"
                Text(
                    text = "Nhập mã PIN 4 số để mở khóa (Mặc định: $hintPin)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicBlue,
                    textAlign = TextAlign.Center,
                )
                PinPadView(
                    pinLength = uiState.pinInput.length,
                    onDigitClick = { digit -> viewModel.appendPinDigit(digit, rooms) },
                    onBackspace = { viewModel.backspacePin() },
                    onClear = { viewModel.clearPin() },
                )
            }

            uiState.authError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusErrorBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = err,
                        color = StatusErrorFg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleWorkspaceSection(
    room: ClassroomRoom,
    role: SchoolRole,
    uiState: RoleReportUiState,
    todayDate: String,
    viewModel: RoleReportViewModel,
) {
    var showPinDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Active Account Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AcademicBlueSoft,
            border = BorderStroke(1.dp, AcademicBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ĐANG ĐĂNG NHẬP CHỨC VỤ",
                        style = MaterialTheme.typography.labelSmall,
                        color = AcademicBlue,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${role.displayLabel} ${room.className} (${room.roomCode})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (role == SchoolRole.MONITOR) {
                        IconButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.testTag("change_pin_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = "Đổi mã PIN lớp",
                                tint = AcademicBlue,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.logoutRole() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("role_logout_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đổi lớp", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Sub-tabs (Monitor sees all 3; Duty Team sees Hygiene directly)
        if (role == SchoolRole.MONITOR) {
            val tabs = RoleWorkspaceTab.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = uiState.activeSubTab == tab,
                        onClick = { viewModel.switchSubTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("subtab_${tab.name.lowercase()}"),
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        when (uiState.activeSubTab) {
            RoleWorkspaceTab.ABSENCE -> AbsenceFormCard(
                room = room,
                todayDate = todayDate,
                isSubmitting = uiState.isSubmitting,
                onAddAbsence = { name, type, reason ->
                    viewModel.addAbsence(room, name, type, reason, todayDate)
                },
                onRemoveAbsence = { index ->
                    viewModel.removeAbsence(room, index, todayDate)
                },
            )

            RoleWorkspaceTab.DEVICES -> DeviceChecklistCard(
                room = room,
                isSubmitting = uiState.isSubmitting,
                onToggleDevice = { key, state ->
                    viewModel.toggleDeviceState(room, key, state)
                },
                onConfirmDevices = { note ->
                    viewModel.confirmDevices(room, note)
                },
            )

            RoleWorkspaceTab.HYGIENE -> HygieneFormCard(
                room = room,
                todayDate = todayDate,
                isSubmitting = uiState.isSubmitting,
                onSubmitHygiene = { status, note, floorClean, boardClean, binEmptied ->
                    viewModel.submitHygiene(room, status, note, floorClean, boardClean, binEmptied, todayDate)
                },
            )
        }
    }

    if (showPinDialog) {
        ChangePinDialog(
            className = room.className,
            currentPin = room.rolePin,
            onDismiss = { showPinDialog = false },
            onSave = { newPin ->
                viewModel.updateRolePin(room, newPin)
                showPinDialog = false
            },
        )
    }
}

@Composable
private fun AbsenceFormCard(
    room: ClassroomRoom,
    todayDate: String,
    isSubmitting: Boolean,
    onAddAbsence: (String, AbsenceType, String) -> Unit,
    onRemoveAbsence: (Int) -> Unit,
) {
    var studentName by remember(room.id) { mutableStateOf("") }
    var selectedType by remember(room.id) { mutableStateOf(AbsenceType.CO_PHEP) }
    var reason by remember(room.id) { mutableStateOf("") }
    val absences = room.effectiveAbsences(todayDate)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Báo cáo học sinh vắng hôm nay",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = studentName,
                onValueChange = { if (it.length <= 80) studentName = it },
                label = { Text("Họ và tên học sinh vắng") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("absent_name_input"),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AbsenceType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = AbsenceType.entries.size),
                    ) {
                        Text(type.displayLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 200) reason = it },
                label = { Text("Lý do vắng (VD: Bị sốt, việc gia đình...)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("absent_reason_input"),
            )

            Button(
                onClick = {
                    val nameSnapshot = studentName
                    val reasonSnapshot = reason
                    if (nameSnapshot.isNotBlank()) {
                        onAddAbsence(nameSnapshot, selectedType, reasonSnapshot)
                        studentName = ""
                        reason = ""
                    } else {
                        onAddAbsence("", selectedType, reasonSnapshot)
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_absent_button"),
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Thêm vào danh sách vắng")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Danh sách vắng đã ghi nhận (${absences.size}):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (absences.isEmpty()) {
                Text(
                    text = "Chưa có học sinh nào vắng hôm nay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    absences.forEachIndexed { index, entry ->
                        val type = entry.typeEnum()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = entry.studentName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "[${type.displayLabel}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (type == AbsenceType.KHONG_PHEP) StatusErrorFg else AcademicBlue,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    if (entry.reason.isNotBlank()) {
                                        Text(
                                            text = entry.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveAbsence(index) },
                                    enabled = !isSubmitting,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Xóa học sinh vắng",
                                        tint = StatusErrorFg,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceChecklistCard(
    room: ClassroomRoom,
    isSubmitting: Boolean,
    onToggleDevice: (String, DeviceState) -> Unit,
    onConfirmDevices: (String) -> Unit,
) {
    var noteText by remember(room.id) { mutableStateOf(room.deviceNote) }
    LaunchedEffect(room.deviceNote) {
        noteText = room.deviceNote
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Thiết bị sử dụng trong phòng",
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (room.monitorConfirmed()) StatusGoodBg else StatusWarnBg,
                ) {
                    Text(
                        text = if (room.monitorConfirmed()) "Đã XN (${room.deviceConfirmedAtText})" else "Chưa xác nhận",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (room.monitorConfirmed()) StatusGoodFg else StatusWarnFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = "Kiểm tra đủ 4 đèn, 2 quạt, 1 màn hình TV và 1 mạng Wi-Fi. Chọn Tốt/Hư rồi bấm Xác nhận.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ROOM_DEVICE_SPECS.forEach { spec ->
                    val isGood = room.deviceState(spec.key) == DeviceState.GOOD
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = spec.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { onToggleDevice(spec.key, DeviceState.GOOD) },
                                    enabled = !isSubmitting,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isGood) StatusGoodFg else Color.White,
                                        contentColor = if (isGood) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("device_${spec.key}_good"),
                                ) {
                                    Text("Tốt", style = MaterialTheme.typography.labelMedium)
                                }
                                FilledTonalButton(
                                    onClick = { onToggleDevice(spec.key, DeviceState.BROKEN) },
                                    enabled = !isSubmitting,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (!isGood) StatusErrorFg else Color.White,
                                        contentColor = if (!isGood) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("device_${spec.key}_broken"),
                                ) {
                                    Text("Hư", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 300) noteText = it },
                label = { Text("Ghi chú hư hỏng (nếu có)") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_note_input"),
            )

            Button(
                onClick = { onConfirmDevices(noteText) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = StatusGoodFg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_devices_button"),
            ) {
                Icon(imageVector = Icons.Rounded.VerifiedUser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lớp trưởng Xác nhận Tình trạng Thiết bị")
            }
        }
    }
}

@Composable
private fun HygieneFormCard(
    room: ClassroomRoom,
    todayDate: String,
    isSubmitting: Boolean,
    onSubmitHygiene: (HygieneStatus, String, Boolean, Boolean, Boolean) -> Unit,
) {
    val currentStatus = room.effectiveHygieneStatus(todayDate)
    var selectedStatus by remember(room.id) {
        mutableStateOf(
            if (currentStatus == HygieneStatus.CAN_NHAC_NHO) HygieneStatus.CAN_NHAC_NHO else HygieneStatus.SACH_SE
        )
    }
    var floorClean by remember(room.id) { mutableStateOf(room.hygieneFloorClean) }
    var boardClean by remember(room.id) { mutableStateOf(room.hygieneBoardClean) }
    var binEmptied by remember(room.id) { mutableStateOf(room.hygieneBinEmptied) }
    var noteText by remember(room.id) { mutableStateOf(room.effectiveHygieneNote(todayDate)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kiểm tra vệ sinh Tiết 1–2",
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AcademicBlueSoft,
                ) {
                    Text(
                        text = "07:00 - 08:35",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = AcademicBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    HygieneCheckItem(
                        checked = floorClean,
                        onCheckedChange = { floorClean = it },
                        label = "Sàn lớp học & hành lang sạch rác",
                    )
                    HygieneCheckItem(
                        checked = boardClean,
                        onCheckedChange = { boardClean = it },
                        label = "Bảng lau sạch, bàn ghế xếp ngay ngắn",
                    )
                    HygieneCheckItem(
                        checked = binEmptied,
                        onCheckedChange = { binEmptied = it },
                        label = "Đã đổ sọt rác đúng nơi quy định",
                    )
                }
            }

            Text(
                text = "Đánh giá chung của Nhóm trực nhật:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val isClean = selectedStatus == HygieneStatus.SACH_SE
                Surface(
                    onClick = { selectedStatus = HygieneStatus.SACH_SE },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isClean) StatusGoodBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isClean) 2.dp else 1.dp,
                        color = if (isClean) StatusGoodFg else MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hygiene_status_clean"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✓ Sạch sẽ / Đạt",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isClean) StatusGoodFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    onClick = { selectedStatus = HygieneStatus.CAN_NHAC_NHO },
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isClean) StatusErrorBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (!isClean) 2.dp else 1.dp,
                        color = if (!isClean) StatusErrorFg else MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hygiene_status_dirty"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✕ Cần nhắc nhở",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isClean) StatusErrorFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 300) noteText = it },
                label = { Text("Ghi chú trực nhật (nếu có)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hygiene_note_input"),
            )

            Button(
                onClick = {
                    onSubmitHygiene(selectedStatus, noteText, floorClean, boardClean, binEmptied)
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_hygiene_button"),
            ) {
                Icon(imageVector = Icons.Rounded.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi báo cáo vệ sinh")
            }
        }
    }
}

@Composable
private fun HygieneCheckItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = AcademicBlue),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ChangePinDialog(
    className: String,
    currentPin: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var newPin by remember { mutableStateOf(currentPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mã PIN lớp $className") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nhập mã PIN mới gồm đúng 4 chữ số để Ban cán sự và Trực nhật sử dụng.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            newPin = input
                        }
                    },
                    label = { Text("Mã PIN 4 số") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newPin) },
                enabled = newPin.length == 4,
            ) {
                Text("Lưu PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}
