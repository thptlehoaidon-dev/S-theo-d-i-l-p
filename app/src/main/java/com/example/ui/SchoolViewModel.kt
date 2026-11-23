package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassroomRoom
import com.example.data.SchoolRepository
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchoolViewModel(
    private val repository: SchoolRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _seedError = MutableStateFlow<String?>(null)
    val seedError: StateFlow<String?> = _seedError.asStateFlow()

    private val _selectedFloor = MutableStateFlow<Int?>(null) // null = Tất cả 24 phòng
    val selectedFloor: StateFlow<Int?> = _selectedFloor.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId.asStateFlow()

    val roomsState: StateFlow<UiState<List<ClassroomRoom>>> = repository.observeRooms(currentUserId)
        .map<List<ClassroomRoom>, UiState<List<ClassroomRoom>>> { rooms ->
            UiState.Success(rooms)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing school rooms", rootError)
            emit(UiState.Error(rootError.message ?: "Không thể tải dữ liệu 24 phòng học"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    init {
        ensureSchoolInitialized()
    }

    fun ensureSchoolInitialized() {
        viewModelScope.launch {
            _seedError.value = null
            repository.ensureSchoolJoinedAndSeeded().onFailure { error ->
                _seedError.value = error.localizedMessage ?: "Không thể đồng bộ dữ liệu trường học"
            }
        }
    }

    fun selectFloor(floor: Int?) {
        _selectedFloor.value = floor
    }

    fun openRoomDetail(roomId: String) {
        _selectedRoomId.value = roomId
    }

    fun closeRoomDetail() {
        _selectedRoomId.value = null
    }

    private companion object {
        const val TAG = "SchoolViewModel"
    }
}
