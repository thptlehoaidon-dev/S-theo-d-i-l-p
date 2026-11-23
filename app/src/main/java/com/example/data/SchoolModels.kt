package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AbsenceType(val firestoreValue: String, val displayLabel: String) {
    CO_PHEP("CO_PHEP", "Có phép"),
    KHONG_PHEP("KHONG_PHEP", "Không phép"),
    DI_MUON("DI_MUON", "Đi muộn");

    companion object {
        fun fromRaw(raw: String?): AbsenceType =
            entries.firstOrNull { it.firestoreValue == raw } ?: CO_PHEP
    }
}

enum class HygieneStatus(val firestoreValue: String, val displayLabel: String) {
    CHUA_KIEM_TRA("CHUA_KIEM_TRA", "Chưa kiểm tra"),
    SACH_SE("SACH_SE", "Sạch sẽ"),
    CAN_NHAC_NHO("CAN_NHAC_NHO", "Cần nhắc nhở");

    companion object {
        fun fromRaw(raw: String?): HygieneStatus =
            entries.firstOrNull { it.firestoreValue == raw } ?: CHUA_KIEM_TRA
    }
}

enum class DeviceState(val firestoreValue: String, val displayLabel: String) {
    GOOD("GOOD", "Tốt"),
    BROKEN("BROKEN", "Hư");

    companion object {
        fun fromRaw(raw: String?): DeviceState =
            entries.firstOrNull { it.firestoreValue == raw } ?: GOOD
    }
}

enum class SchoolRole(val displayLabel: String) {
    MONITOR("Lớp trưởng"),
    DUTY_TEAM("Nhóm trực nhật"),
}

data class DeviceSpec(
    val key: String,
    val label: String,
)

val ROOM_DEVICE_SPECS = listOf(
    DeviceSpec("lamp1", "Đèn 1"),
    DeviceSpec("lamp2", "Đèn 2"),
    DeviceSpec("lamp3", "Đèn 3"),
    DeviceSpec("lamp4", "Đèn 4"),
    DeviceSpec("fan1", "Quạt 1"),
    DeviceSpec("fan2", "Quạt 2"),
    DeviceSpec("tv", "Màn hình TV"),
    DeviceSpec("wifi", "Mạng Wi-Fi"),
)

@Keep
@IgnoreExtraProperties
data class AbsenceEntry(
    val studentName: String = "",
    val absenceType: String = AbsenceType.CO_PHEP.firestoreValue,
    val reason: String = "",
) {
    fun typeEnum(): AbsenceType = AbsenceType.fromRaw(absenceType)

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "studentName" to studentName.trim().take(80),
        "absenceType" to AbsenceType.fromRaw(absenceType).firestoreValue,
        "reason" to reason.trim().take(200),
    )
}

enum class RoomHealthStatus {
    OPTIMAL,   // Sạch, thiết bị tốt hết, Lớp trưởng đã xác nhận
    PENDING,   // Chưa trực nhật hoặc chưa Lớp trưởng xác nhận thiết bị
    ISSUE,     // Có thiết bị hư hoặc vệ sinh cần nhắc nhở
}

@Keep
@IgnoreExtraProperties
data class ClassroomRoom(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val floor: Long = 1L,
    val className: String = "",
    val roomCode: String = "",
    val memberUids: List<String> = emptyList(),
    val reportDate: String = "",
    val absences: List<AbsenceEntry> = emptyList(),
    val hygieneStatus: String = HygieneStatus.CHUA_KIEM_TRA.firestoreValue,
    val hygieneTimeText: String = "",
    val hygieneOnTime: Boolean = false,
    val hygieneNote: String = "",
    val hygieneChecker: String = "",
    val hygieneFloorClean: Boolean = true,
    val hygieneBoardClean: Boolean = true,
    val hygieneBinEmptied: Boolean = true,
    val devices: Map<String, String> = defaultDevicesMap(),
    val deviceNote: String = "",
    val deviceConfirmedBy: String = "",
    val deviceConfirmedAtText: String = "",
    val rolePin: String = "1234",
    val rolePattern: String = "1-4-7-8-9",
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun floorInt(): Int = floor.toInt()

    fun effectiveAbsences(todayDate: String): List<AbsenceEntry> =
        if (reportDate == todayDate) absences else emptyList()

    fun effectiveHygieneStatus(todayDate: String): HygieneStatus =
        if (reportDate == todayDate) HygieneStatus.fromRaw(hygieneStatus) else HygieneStatus.CHUA_KIEM_TRA

    fun effectiveHygieneTimeText(todayDate: String): String =
        if (reportDate == todayDate) hygieneTimeText else ""

    fun effectiveHygieneOnTime(todayDate: String): Boolean =
        if (reportDate == todayDate) hygieneOnTime else false

    fun effectiveHygieneNote(todayDate: String): String =
        if (reportDate == todayDate) hygieneNote else ""

    fun effectiveHygieneChecker(todayDate: String): String =
        if (reportDate == todayDate) hygieneChecker else ""

    fun deviceState(key: String): DeviceState =
        DeviceState.fromRaw(devices[key])

    fun brokenDeviceCount(): Int =
        ROOM_DEVICE_SPECS.count { spec -> deviceState(spec.key) == DeviceState.BROKEN }

    fun monitorConfirmed(): Boolean = deviceConfirmedBy.isNotBlank()

    fun overallHealth(todayDate: String): RoomHealthStatus {
        val hygiene = effectiveHygieneStatus(todayDate)
        if (brokenDeviceCount() > 0 || hygiene == HygieneStatus.CAN_NHAC_NHO) {
            return RoomHealthStatus.ISSUE
        }
        if (!monitorConfirmed() || hygiene == HygieneStatus.CHUA_KIEM_TRA) {
            return RoomHealthStatus.PENDING
        }
        return RoomHealthStatus.OPTIMAL
    }

    companion object {
        fun defaultDevicesMap(): Map<String, String> =
            ROOM_DEVICE_SPECS.associate { it.key to DeviceState.GOOD.firestoreValue }
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
