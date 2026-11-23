package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AbsenceEntry
import com.example.data.AbsenceType
import com.example.data.ClassroomRoom
import com.example.data.DeviceState
import com.example.data.HygieneStatus
import com.example.data.SchoolRepository
import com.example.data.SchoolRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RoleAuthMethod {
    PATTERN,
    PIN,
}

enum class RoleWorkspaceTab(val label: String) {
    ABSENCE("1. Báo vắng"),
    DEVICES("2. Thiết bị"),
    HYGIENE("3. Vệ sinh"),
}

data class RoleSession(
    val roomId: String,
    val role: SchoolRole,
)

data class RoleReportUiState(
    val selectedRoomId: String = "12A1",
    val selectedRole: SchoolRole = SchoolRole.MONITOR,
    val authMethod: RoleAuthMethod = RoleAuthMethod.PATTERN,
    val pinInput: String = "",
    val authError: String? = null,
    val activeSession: RoleSession? = null,
    val activeSubTab: RoleWorkspaceTab = RoleWorkspaceTab.ABSENCE,
    val isSubmitting: Boolean = false,
    val feedbackMessage: String? = null,
)

class RoleReportViewModel(
    private val repository: SchoolRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleReportUiState())
    val uiState: StateFlow<RoleReportUiState> = _uiState.asStateFlow()

    fun selectLoginRoom(roomId: String) {
        _uiState.update { it.copy(selectedRoomId = roomId, authError = null) }
    }

    fun selectLoginRole(role: SchoolRole) {
        _uiState.update { it.copy(selectedRole = role, authError = null) }
    }

    fun switchAuthMethod(method: RoleAuthMethod) {
        _uiState.update { it.copy(authMethod = method, pinInput = "", authError = null) }
    }

    fun appendPinDigit(digit: String, rooms: List<ClassroomRoom>) {
        val current = _uiState.value
        if (current.pinInput.length >= 4) return
        val nextPin = current.pinInput + digit
        _uiState.update { it.copy(pinInput = nextPin, authError = null) }
        if (nextPin.length == 4) {
            verifyPin(nextPin, rooms)
        }
    }

    fun backspacePin() {
        _uiState.update { it.copy(pinInput = it.pinInput.dropLast(1), authError = null) }
    }

    fun clearPin() {
        _uiState.update { it.copy(pinInput = "", authError = null) }
    }

    private fun verifyPin(pin: String, rooms: List<ClassroomRoom>) {
        val state = _uiState.value
        val room = rooms.firstOrNull { it.id == state.selectedRoomId }
        val expectedPin = room?.rolePin?.ifBlank { "1234" } ?: "1234"
        if (pin == expectedPin) {
            unlockWorkspace(state.selectedRoomId, state.selectedRole)
        } else {
            _uiState.update {
                it.copy(
                    pinInput = "",
                    authError = "Sai mã PIN! Thử lại (mặc định: $expectedPin)",
                )
            }
        }
    }

    fun verifyPattern(patternSequence: List<Int>, rooms: List<ClassroomRoom>) {
        if (patternSequence.isEmpty()) return
        val patternString = patternSequence.joinToString("-")
        val state = _uiState.value
        val room = rooms.firstOrNull { it.id == state.selectedRoomId }
        val expectedPattern = room?.rolePattern?.ifBlank { "1-4-7-8-9" } ?: "1-4-7-8-9"
        if (patternString == expectedPattern) {
            unlockWorkspace(state.selectedRoomId, state.selectedRole)
        } else {
            _uiState.update {
                it.copy(
                    authError = "Mẫu hình chưa đúng! Hãy vẽ chữ L (1→4→7→8→9)",
                )
            }
        }
    }

    private fun unlockWorkspace(roomId: String, role: SchoolRole) {
        val initialTab = if (role == SchoolRole.DUTY_TEAM) {
            RoleWorkspaceTab.HYGIENE
        } else {
            RoleWorkspaceTab.ABSENCE
        }
        _uiState.update {
            it.copy(
                pinInput = "",
                authError = null,
                activeSession = RoleSession(roomId = roomId, role = role),
                activeSubTab = initialTab,
                feedbackMessage = "Đã đăng nhập ${role.displayLabel} $roomId",
            )
        }
    }

    fun logoutRole() {
        _uiState.update {
            it.copy(
                activeSession = null,
                pinInput = "",
                authError = null,
            )
        }
    }

    fun switchSubTab(tab: RoleWorkspaceTab) {
        _uiState.update { it.copy(activeSubTab = tab) }
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun addAbsence(
        room: ClassroomRoom,
        studentName: String,
        absenceType: AbsenceType,
        reason: String,
        todayDate: String,
    ) {
        val trimmedName = studentName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng nhập họ tên học sinh vắng") }
            return
        }
        val currentList = room.effectiveAbsences(todayDate)
        if (currentList.size >= 60) {
            _uiState.update { it.copy(feedbackMessage = "Đã đạt giới hạn 60 học sinh vắng trong ngày") }
            return
        }
        val updatedList = currentList + AbsenceEntry(
            studentName = trimmedName.take(80),
            absenceType = absenceType.firestoreValue,
            reason = reason.trim().take(200),
        )
        runMutation("Đã thêm $trimmedName vào danh sách vắng") {
            repository.updateAbsences(room.id, updatedList, todayDate)
        }
    }

    fun removeAbsence(room: ClassroomRoom, index: Int, todayDate: String) {
        val currentList = room.effectiveAbsences(todayDate).toMutableList()
        if (index !in currentList.indices) return
        currentList.removeAt(index)
        runMutation("Đã xóa khỏi danh sách vắng") {
            repository.updateAbsences(room.id, currentList, todayDate)
        }
    }

    fun toggleDeviceState(room: ClassroomRoom, deviceKey: String, newState: DeviceState) {
        val updatedDevices = room.devices.toMutableMap().apply {
            this[deviceKey] = newState.firestoreValue
        }
        runMutation("Đã cập nhật thiết bị — Vui lòng bấm Xác nhận lại") {
            repository.updateDevices(
                roomId = room.id,
                devices = updatedDevices,
                deviceNote = room.deviceNote,
                confirmedBy = "",
                confirmedAtText = "",
            )
        }
    }

    fun confirmDevices(room: ClassroomRoom, note: String) {
        val session = _uiState.value.activeSession ?: return
        val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val confirmerLabel = "${session.role.displayLabel} ${room.className}"
        runMutation("$confirmerLabel đã xác nhận thiết bị lúc $nowTime") {
            repository.updateDevices(
                roomId = room.id,
                devices = room.devices,
                deviceNote = note,
                confirmedBy = confirmerLabel,
                confirmedAtText = nowTime,
            )
        }
    }

    fun submitHygiene(
        room: ClassroomRoom,
        status: HygieneStatus,
        note: String,
        floorClean: Boolean,
        boardClean: Boolean,
        binEmptied: Boolean,
        todayDate: String,
    ) {
        val session = _uiState.value.activeSession ?: return
        val cal = Calendar.getInstance()
        val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isOnTime = minutesOfDay in 420..515 // 07:00 - 08:35 (Tiết 1, 2)
        val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
        val checkerLabel = "${session.role.displayLabel} ${room.className}"
        val timingDesc = if (isOnTime) "Đúng giờ Tiết 1–2" else "Ngoài giờ quy định"

        runMutation("Đã gửi báo cáo vệ sinh ($nowTime - $timingDesc)") {
            repository.submitHygiene(
                roomId = room.id,
                status = status,
                timeText = nowTime,
                onTime = isOnTime,
                note = note,
                checker = checkerLabel,
                floorClean = floorClean,
                boardClean = boardClean,
                binEmptied = binEmptied,
                reportDate = todayDate,
            )
        }
    }

    fun updateRolePin(room: ClassroomRoom, newPin: String) {
        if (!newPin.matches(Regex("^[0-9]{4}$"))) {
            _uiState.update { it.copy(feedbackMessage = "Mã PIN mới phải gồm đúng 4 chữ số") }
            return
        }
        runMutation("Đã đổi mã PIN lớp ${room.className}") {
            repository.updateRoleCredentials(room.id, newPin, room.rolePattern.ifBlank { "1-4-7-8-9" })
        }
    }

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = block()
            _uiState.update { state ->
                state.copy(
                    isSubmitting = false,
                    feedbackMessage = result.fold(
                        onSuccess = { successMessage },
                        onFailure = { err -> err.localizedMessage ?: "Thao tác thất bại, vui lòng thử lại" },
                    ),
                )
            }
        }
    }
}
