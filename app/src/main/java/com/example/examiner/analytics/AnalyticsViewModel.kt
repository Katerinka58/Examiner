package com.example.examiner.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examiner.data.AnswersDao
import com.example.examiner.data.GroupsDao
import com.example.examiner.data.StudentsDao
import com.example.examiner.data.CoursesDao
import com.example.examiner.logs.EventLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupAnalytics(
    val groupId: Long,
    val groupName: String, 
    val avgGrade: Double, 
    val answersCount: Int,
    val studentsCount: Int,
    val passRate: Double
)

data class CourseAnalytics(
    val courseId: Long,
    val courseName: String, 
    val avgGrade: Double, 
    val answersCount: Int,
    val studentsCount: Int,
    val passRate: Double
)

data class AnalyticsUiState(
    val groups: List<GroupAnalytics> = emptyList(),
    val courses: List<CourseAnalytics> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTimeRange: TimeRange = TimeRange.ALL_TIME
)

enum class TimeRange(val days: Int) {
    WEEK(7), MONTH(30), QUARTER(90), ALL_TIME(-1)
}

class AnalyticsViewModel(
    private val answersDao: AnswersDao,
    private val groupsDao: GroupsDao,
    private val studentsDao: StudentsDao,
    private val coursesDao: CoursesDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val groups = groupsDao.getAll()
                val courses = coursesDao.getAll()
                
                val groupAnalytics = groups.map { group ->
                    val students = studentsDao.byGroup(group.id)
                    val answers = students.flatMap { student ->
                        // TODO: Реализовать join запрос для получения ответов студента
                        emptyList<Double>()
                    }
                    
                    val avgGrade = if (answers.isNotEmpty()) answers.average() else 0.0
                    val passRate = if (answers.isNotEmpty()) {
                        answers.count { it >= 60.0 }.toDouble() / answers.size * 100
                    } else 0.0
                    
                    GroupAnalytics(
                        groupId = group.id,
                        groupName = group.name,
                        avgGrade = avgGrade,
                        answersCount = answers.size,
                        studentsCount = students.size,
                        passRate = passRate
                    )
                }
                
                val courseAnalytics = courses.map { course ->
                    val students = studentsDao.byCourse(course.id)
                    val answers = students.flatMap { student ->
                        // TODO: Реализовать join запрос для получения ответов студента
                        emptyList<Double>()
                    }
                    
                    val avgGrade = if (answers.isNotEmpty()) answers.average() else 0.0
                    val passRate = if (answers.isNotEmpty()) {
                        answers.count { it >= 60.0 }.toDouble() / answers.size * 100
                    } else 0.0
                    
                    CourseAnalytics(
                        courseId = course.id,
                        courseName = course.name,
                        avgGrade = avgGrade,
                        answersCount = answers.size,
                        studentsCount = students.size,
                        passRate = passRate
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    groups = groupAnalytics,
                    courses = courseAnalytics,
                    isLoading = false
                )
                
                EventLog.i("AnalyticsViewModel", "Loaded analytics for ${groups.size} groups and ${courses.size} courses")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки аналитики: ${e.message}"
                )
                EventLog.e("AnalyticsViewModel", e)
            }
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = timeRange)
        loadAnalytics()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getTopPerformingGroups(limit: Int = 5): List<GroupAnalytics> {
        return _uiState.value.groups
            .sortedByDescending { it.avgGrade }
            .take(limit)
    }

    fun getTopPerformingCourses(limit: Int = 5): List<CourseAnalytics> {
        return _uiState.value.courses
            .sortedByDescending { it.avgGrade }
            .take(limit)
    }

    fun getGroupsWithLowPassRate(threshold: Double = 60.0): List<GroupAnalytics> {
        return _uiState.value.groups
            .filter { it.passRate < threshold }
            .sortedBy { it.passRate }
    }

    fun getCoursesWithLowPassRate(threshold: Double = 60.0): List<CourseAnalytics> {
        return _uiState.value.courses
            .filter { it.passRate < threshold }
            .sortedBy { it.passRate }
    }
} 