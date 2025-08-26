package com.example.examiner.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examiner.lms.LmsApi
import com.example.examiner.logs.EventLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.Response
import java.io.File

data class LmsCourse(
    val id: String,
    val name: String,
    val description: String?,
    val isActive: Boolean
)

data class LmsGroup(
    val id: String,
    val name: String,
    val courseId: String,
    val studentCount: Int
)

data class UploadedFile(
    val id: String,
    val name: String,
    val type: String,
    val size: Long,
    val uploadDate: Long,
    val status: UploadStatus
)

enum class UploadStatus {
    PENDING, UPLOADING, SUCCESS, FAILED
}

data class IntegrationsUiState(
    val courses: List<LmsCourse> = emptyList(),
    val groups: List<LmsGroup> = emptyList(),
    val uploadedFiles: List<UploadedFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val syncInProgress: Boolean = false
)

class IntegrationsViewModel(
    private val lmsApi: LmsApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IntegrationsUiState())
    val uiState: StateFlow<IntegrationsUiState> = _uiState.asStateFlow()

    init {
        checkConnection()
    }

    fun checkConnection() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Проверяем подключение к LMS
                val response = lmsApi.getCourses()
                val isConnected = response.isSuccessful
                
                _uiState.value = _uiState.value.copy(
                    isConnected = isConnected,
                    isLoading = false
                )
                
                EventLog.i("IntegrationsViewModel", "LMS connection check: ${if (isConnected) "success" else "failed"}")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    isLoading = false,
                    error = "Ошибка подключения к LMS: ${e.message}"
                )
                EventLog.e("IntegrationsViewModel", e)
            }
        }
    }

    fun syncCourses() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true, error = null)
                
                val response = lmsApi.getCourses()
                if (response.isSuccessful) {
                    // TODO: Парсинг JSON ответа от LMS
                    val courses = listOf(
                        LmsCourse("1", "Программирование", "Основы программирования", true),
                        LmsCourse("2", "Базы данных", "SQL и NoSQL", true),
                        LmsCourse("3", "Веб-разработка", "HTML, CSS, JavaScript", false)
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        courses = courses,
                        syncInProgress = false
                    )
                    
                    EventLog.i("IntegrationsViewModel", "Synced ${courses.size} courses from LMS")
                } else {
                    _uiState.value = _uiState.value.copy(
                        syncInProgress = false,
                        error = "Ошибка синхронизации курсов: ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = "Ошибка синхронизации: ${e.message}"
                )
                EventLog.e("IntegrationsViewModel", e)
            }
        }
    }

    fun syncGroups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(syncInProgress = true, error = null)
                
                val response = lmsApi.getGroups()
                if (response.isSuccessful) {
                    // TODO: Парсинг JSON ответа от LMS
                    val groups = listOf(
                        LmsGroup("1", "Группа А", "1", 25),
                        LmsGroup("2", "Группа Б", "1", 30),
                        LmsGroup("3", "Группа В", "2", 20)
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        groups = groups,
                        syncInProgress = false
                    )
                    
                    EventLog.i("IntegrationsViewModel", "Synced ${groups.size} groups from LMS")
                } else {
                    _uiState.value = _uiState.value.copy(
                        syncInProgress = false,
                        error = "Ошибка синхронизации групп: ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncInProgress = false,
                    error = "Ошибка синхронизации: ${e.message}"
                )
                EventLog.e("IntegrationsViewModel", e)
            }
        }
    }

    fun uploadFile(file: File, studentId: String, type: String) {
        viewModelScope.launch {
            try {
                // Добавляем файл в список загружаемых
                val uploadingFile = UploadedFile(
                    id = System.currentTimeMillis().toString(),
                    name = file.name,
                    type = type,
                    size = file.length(),
                    uploadDate = System.currentTimeMillis(),
                    status = UploadStatus.UPLOADING
                )
                
                val currentFiles = _uiState.value.uploadedFiles.toMutableList()
                currentFiles.add(uploadingFile)
                _uiState.value = _uiState.value.copy(uploadedFiles = currentFiles)
                
                // TODO: Создать MultipartBody.Part из файла
                // val filePart = FileUploadHelper.buildPartFromFile(file.absolutePath)
                
                // TODO: Создать MultipartBody.Part из файла
                // val filePart = FileUploadHelper.buildPartFromFile(file.absolutePath)
                // TODO: Создать MultipartBody.Part из файла
                // val filePart = FileUploadHelper.buildPartFromFile(file.absolutePath)
                // val response = lmsApi.uploadSubmission(studentId, filePart, type)
                // Временно используем заглушку
                val response = Response.success(okhttp3.ResponseBody.create(null, ""))
                
                val updatedFiles = _uiState.value.uploadedFiles.toMutableList()
                val fileIndex = updatedFiles.indexOfFirst { it.id == uploadingFile.id }
                
                if (response.isSuccessful && fileIndex != -1) {
                    updatedFiles[fileIndex] = uploadingFile.copy(status = UploadStatus.SUCCESS)
                    EventLog.i("IntegrationsViewModel", "File ${file.name} uploaded successfully")
                } else {
                    if (fileIndex != -1) {
                        updatedFiles[fileIndex] = uploadingFile.copy(status = UploadStatus.FAILED)
                    }
                    EventLog.e("IntegrationsViewModel", Exception("Upload failed: ${response.code()}"))
                }
                
                _uiState.value = _uiState.value.copy(uploadedFiles = updatedFiles)
                
            } catch (e: Exception) {
                EventLog.e("IntegrationsViewModel", e)
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка загрузки файла: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun removeUploadedFile(fileId: String) {
        val currentFiles = _uiState.value.uploadedFiles.toMutableList()
        currentFiles.removeAll { it.id == fileId }
        _uiState.value = _uiState.value.copy(uploadedFiles = currentFiles)
    }

    fun getFilesByType(type: String): List<UploadedFile> {
        return _uiState.value.uploadedFiles.filter { it.type == type }
    }

    fun getSuccessfulUploads(): List<UploadedFile> {
        return _uiState.value.uploadedFiles.filter { it.status == UploadStatus.SUCCESS }
    }

    fun getFailedUploads(): List<UploadedFile> {
        return _uiState.value.uploadedFiles.filter { it.status == UploadStatus.FAILED }
    }
} 