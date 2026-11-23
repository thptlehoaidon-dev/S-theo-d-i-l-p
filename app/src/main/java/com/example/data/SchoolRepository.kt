package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    companion object {
        const val SCHOOL_ID = "thpt_main"
        const val DEFAULT_SCHOOL_NAME = "Trường THPT - Dãy phòng học"

        val ALL_ROOM_IDS: List<String> = buildList {
            for (i in 1..8) add("10A$i")
            for (i in 1..8) add("11A$i")
            for (i in 1..8) add("12A$i")
        }

        fun todayDateString(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập Google trước khi truy cập dữ liệu.")

    suspend fun ensureSchoolJoinedAndSeeded(): Result<Unit> {
        val schoolPath = "schools/$SCHOOL_ID"
        return try {
            val uid = requireUserId()
            val schoolRef = db.collection("schools").document(SCHOOL_ID)
            val schoolSnap = schoolRef.get().await()

            if (!schoolSnap.exists()) {
                val batch = db.batch()
                val schoolPayload = mapOf(
                    "name" to DEFAULT_SCHOOL_NAME,
                    "memberUids" to listOf(uid),
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                batch.set(schoolRef, schoolPayload)

                val today = todayDateString()
                buildInitialRooms(uid, today).forEach { (roomId, payload) ->
                    val roomRef = schoolRef.collection("rooms").document(roomId)
                    batch.set(roomRef, payload)
                }
                batch.commit().await()
            } else {
                val existingMembers = (schoolSnap.get("memberUids") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()
                if (uid !in existingMembers) {
                    schoolRef.update(
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(uid),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()

                    val batch = db.batch()
                    ALL_ROOM_IDS.forEach { roomId ->
                        val roomRef = schoolRef.collection("rooms").document(roomId)
                        batch.update(
                            roomRef,
                            mapOf(
                                "memberUids" to FieldValue.arrayUnion(uid),
                                "updatedByUid" to uid,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                    batch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, schoolPath)
            Result.failure(e)
        }
    }

    fun observeRooms(userId: String): Flow<List<ClassroomRoom>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val roomsPath = "schools/$SCHOOL_ID/rooms"
        emitAll(
            db.collection("schools")
                .document(SCHOOL_ID)
                .collection("rooms")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClassroomRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareByDescending<ClassroomRoom> { it.floor }.thenBy { it.className })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, roomsPath)
                    }
                    throw error
                }
        )
    }

    suspend fun updateAbsences(
        roomId: String,
        absences: List<AbsenceEntry>,
        reportDate: String = todayDateString(),
    ): Result<Unit> {
        val path = "schools/$SCHOOL_ID/rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank" }
            require(absences.size <= 60) { "Danh sách vắng tối đa 60 học sinh" }
            val validAbsences = absences.map { it.toFirestoreMap() }

            db.collection("schools").document(SCHOOL_ID)
                .collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "reportDate" to reportDate,
                        "absences" to validAbsences,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitHygiene(
        roomId: String,
        status: HygieneStatus,
        timeText: String,
        onTime: Boolean,
        note: String,
        checker: String,
        floorClean: Boolean,
        boardClean: Boolean,
        binEmptied: Boolean,
        reportDate: String = todayDateString(),
    ): Result<Unit> {
        val path = "schools/$SCHOOL_ID/rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank" }

            db.collection("schools").document(SCHOOL_ID)
                .collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "reportDate" to reportDate,
                        "hygieneStatus" to status.firestoreValue,
                        "hygieneTimeText" to timeText.take(10),
                        "hygieneOnTime" to onTime,
                        "hygieneNote" to note.trim().take(300),
                        "hygieneChecker" to checker.trim().take(80),
                        "hygieneFloorClean" to floorClean,
                        "hygieneBoardClean" to boardClean,
                        "hygieneBinEmptied" to binEmptied,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDevices(
        roomId: String,
        devices: Map<String, String>,
        deviceNote: String,
        confirmedBy: String,
        confirmedAtText: String,
    ): Result<Unit> {
        val path = "schools/$SCHOOL_ID/rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(roomId.isNotBlank()) { "Room ID must not be blank" }
            val normalizedDevices = ROOM_DEVICE_SPECS.associate { spec ->
                spec.key to DeviceState.fromRaw(devices[spec.key]).firestoreValue
            }

            db.collection("schools").document(SCHOOL_ID)
                .collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "devices" to normalizedDevices,
                        "deviceNote" to deviceNote.trim().take(300),
                        "deviceConfirmedBy" to confirmedBy.trim().take(80),
                        "deviceConfirmedAtText" to confirmedAtText.take(10),
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoleCredentials(
        roomId: String,
        newPin: String,
        newPattern: String,
    ): Result<Unit> {
        val path = "schools/$SCHOOL_ID/rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(newPin.matches(Regex("^[0-9]{4}$"))) { "Mã PIN phải gồm đúng 4 chữ số" }
            require(newPattern.length in 3..30) { "Mẫu hình vẽ không hợp lệ" }

            db.collection("schools").document(SCHOOL_ID)
                .collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "rolePin" to newPin,
                        "rolePattern" to newPattern,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun buildInitialRooms(uid: String, today: String): Map<String, Map<String, Any>> {
        val result = linkedMapOf<String, Map<String, Any>>()
        val gradeConfigs = listOf(
            Triple(1L, "10A", 100),
            Triple(2L, "11A", 200),
            Triple(3L, "12A", 300),
        )

        gradeConfigs.forEach { (floor, prefix, baseRoom) ->
            for (i in 1..8) {
                val className = "$prefix$i"
                val roomCode = "P.${baseRoom + i}"
                val devices = ClassroomRoom.defaultDevicesMap().toMutableMap()
                var absences = emptyList<Map<String, Any>>()
                var hygieneStatus = HygieneStatus.SACH_SE.firestoreValue
                var hygieneTimeText = "07:35"
                var hygieneOnTime = true
                var hygieneNote = ""
                var hygieneChecker = "Nhóm trực nhật $className"
                var confirmedBy = "Lớp trưởng $className"
                var confirmedAtText = "07:20"
                var deviceNote = ""

                when (className) {
                    "12A1" -> {
                        absences = listOf(
                            AbsenceEntry("Nguyễn Văn An", AbsenceType.CO_PHEP.firestoreValue, "Bị sốt cao").toFirestoreMap()
                        )
                        devices["lamp2"] = DeviceState.BROKEN.firestoreValue
                        deviceNote = "Đèn 2 bị cháy bóng, đã báo bảo trì"
                    }
                    "11A3" -> {
                        absences = listOf(
                            AbsenceEntry("Trần Thị Bình", AbsenceType.CO_PHEP.firestoreValue, "Việc gia đình").toFirestoreMap(),
                            AbsenceEntry("Lê Minh Khang", AbsenceType.KHONG_PHEP.firestoreValue, "Chưa liên hệ được").toFirestoreMap(),
                        )
                        hygieneStatus = HygieneStatus.CAN_NHAC_NHO.firestoreValue
                        hygieneTimeText = "08:15"
                        hygieneNote = "Còn giấy vụn dưới gầm bàn cuối"
                    }
                    "10A5" -> {
                        devices["wifi"] = DeviceState.BROKEN.firestoreValue
                        deviceNote = "Mạng Wi-Fi phòng bị mất tín hiệu"
                        confirmedBy = ""
                        confirmedAtText = ""
                    }
                    "10A2", "12A6" -> {
                        hygieneStatus = HygieneStatus.CHUA_KIEM_TRA.firestoreValue
                        hygieneTimeText = ""
                        hygieneOnTime = false
                        hygieneChecker = ""
                        confirmedBy = ""
                        confirmedAtText = ""
                    }
                }

                result[className] = mapOf(
                    "schoolId" to SCHOOL_ID,
                    "floor" to floor,
                    "className" to className,
                    "roomCode" to roomCode,
                    "memberUids" to listOf(uid),
                    "reportDate" to today,
                    "absences" to absences,
                    "hygieneStatus" to hygieneStatus,
                    "hygieneTimeText" to hygieneTimeText,
                    "hygieneOnTime" to hygieneOnTime,
                    "hygieneNote" to hygieneNote,
                    "hygieneChecker" to hygieneChecker,
                    "hygieneFloorClean" to true,
                    "hygieneBoardClean" to true,
                    "hygieneBinEmptied" to true,
                    "devices" to devices,
                    "deviceNote" to deviceNote,
                    "deviceConfirmedBy" to confirmedBy,
                    "deviceConfirmedAtText" to confirmedAtText,
                    "rolePin" to "1234",
                    "rolePattern" to "1-4-7-8-9",
                    "updatedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            }
        }
        return result
    }
}
