package com.example.examiner.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examiner.data.QuestionEntity
import com.example.examiner.data.AnswerEntity
import com.example.examiner.data.QuestionsDao
import com.example.examiner.data.AnswersDao
import com.example.examiner.logs.EventLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class ExamQuestion(
    val id: Long,
    val text: String,
    val difficulty: Int,
    val studentAnswer: String = "",
    val isAnswered: Boolean = false
)

data class ExamUiState(
    val questions: List<ExamQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val timeRemaining: Long = 0,
    val isExamStarted: Boolean = false,
    val isExamFinished: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val proctoringEnabled: Boolean = true,
    val screenPinningEnabled: Boolean = true
)

class ExamViewModel(
    private val questionsDao: QuestionsDao,
    private val answersDao: AnswersDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()
    
    private var examStartTime: Long = 0
    private var examDuration: Long = 120 * 60 * 1000 // 2 часа по умолчанию
    private var studentId: Long = 0

    fun startExam(studentId: Long, durationMinutes: Int = 120) {
        this.studentId = studentId
        this.examDuration = durationMinutes * 60 * 1000L
        this.examStartTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Загружаем вопросы для экзамена
                val questions = questionsDao.getAll()
                val examQuestions = questions.map { question ->
                    ExamQuestion(
                        id = question.id,
                        text = question.text,
                        difficulty = question.difficulty
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    questions = examQuestions,
                    isExamStarted = true,
                    isLoading = false,
                    timeRemaining = examDuration
                )
                
                EventLog.i("ExamViewModel", "Started exam for student $studentId with ${examQuestions.size} questions")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка запуска экзамена: ${e.message}"
                )
                EventLog.e("ExamViewModel", e)
            }
        }
    }

    fun answerQuestion(questionId: Long, answer: String) {
        val currentQuestions = _uiState.value.questions.toMutableList()
        val questionIndex = currentQuestions.indexOfFirst { it.id == questionId }
        
        if (questionIndex != -1) {
            currentQuestions[questionIndex] = currentQuestions[questionIndex].copy(
                studentAnswer = answer,
                isAnswered = answer.isNotBlank()
            )
            
            _uiState.value = _uiState.value.copy(questions = currentQuestions)
            EventLog.i("ExamViewModel", "Student answered question $questionId")
        }
    }

    fun nextQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex < _uiState.value.questions.size - 1) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = currentIndex - 1)
        }
    }

    fun finishExam() {
        viewModelScope.launch {
            try {
                // Сохраняем все ответы
                val questions = _uiState.value.questions
                val answers = questions.map { question ->
                    AnswerEntity(
                        questionId = question.id,
                        studentId = studentId,
                        answerText = question.studentAnswer,
                        grade = null, // Оценка будет выставлена позже
                        comment = null,
                        createdAt = System.currentTimeMillis()
                    )
                }
                
                answers.forEach { answer ->
                    answersDao.upsert(answer)
                }
                
                _uiState.value = _uiState.value.copy(
                    isExamFinished = true,
                    isExamStarted = false
                )
                
                EventLog.i("ExamViewModel", "Exam finished for student $studentId, saved ${answers.size} answers")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка завершения экзамена: ${e.message}"
                )
                EventLog.e("ExamViewModel", e)
            }
        }
    }

    fun updateTimeRemaining() {
        if (_uiState.value.isExamStarted && !_uiState.value.isExamFinished) {
            val elapsed = System.currentTimeMillis() - examStartTime
            val remaining = examDuration - elapsed
            
            if (remaining <= 0) {
                finishExam()
            } else {
                _uiState.value = _uiState.value.copy(timeRemaining = remaining)
            }
        }
    }

    fun toggleProctoring() {
        _uiState.value = _uiState.value.copy(
            proctoringEnabled = !_uiState.value.proctoringEnabled
        )
        EventLog.i("ExamViewModel", "Proctoring ${if (_uiState.value.proctoringEnabled) "enabled" else "disabled"}")
    }

    fun toggleScreenPinning() {
        _uiState.value = _uiState.value.copy(
            screenPinningEnabled = !_uiState.value.screenPinningEnabled
        )
        EventLog.i("ExamViewModel", "Screen pinning ${if (_uiState.value.screenPinningEnabled) "enabled" else "disabled"}")
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getCurrentQuestion(): ExamQuestion? {
        val questions = _uiState.value.questions
        val currentIndex = _uiState.value.currentQuestionIndex
        return if (currentIndex < questions.size) questions[currentIndex] else null
    }

    fun getProgress(): Float {
        val questions = _uiState.value.questions
        if (questions.isEmpty()) return 0f
        
        val answeredCount = questions.count { it.isAnswered }
        return answeredCount.toFloat() / questions.size
    }

    fun getTimeRemainingFormatted(): String {
        val remaining = _uiState.value.timeRemaining
        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (remaining % (1000 * 60)) / 1000
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
} 