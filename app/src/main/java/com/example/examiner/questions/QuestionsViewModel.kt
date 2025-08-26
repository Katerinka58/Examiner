package com.example.examiner.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examiner.data.QuestionEntity
import com.example.examiner.data.QuestionsDao
import com.example.examiner.logs.EventLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuestionUiState(
    val questions: List<QuestionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val error: String? = null
)

class QuestionsViewModel(
    private val questionsDao: QuestionsDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QuestionUiState())
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val questions = questionsDao.getAll()
                _uiState.value = _uiState.value.copy(
                    questions = questions,
                    isLoading = false
                )
                EventLog.i("QuestionsViewModel", "Loaded ${questions.size} questions")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки вопросов: ${e.message}"
                )
                EventLog.e("QuestionsViewModel", e)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterQuestions()
    }

    fun toggleTag(tag: String) {
        val currentTags = _uiState.value.selectedTags.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _uiState.value = _uiState.value.copy(selectedTags = currentTags)
        filterQuestions()
    }

    private fun filterQuestions() {
        viewModelScope.launch {
            val allQuestions = questionsDao.getAll()
            val query = _uiState.value.searchQuery.lowercase()
            val selectedTags = _uiState.value.selectedTags

            val filtered = allQuestions.filter { question ->
                val matchesQuery = query.isEmpty() || 
                    question.text.lowercase().contains(query) ||
                    (question.tags?.lowercase()?.contains(query) == true)
                
                val matchesTags = selectedTags.isEmpty() ||
                    selectedTags.any { tag ->
                        question.tags?.lowercase()?.contains(tag.lowercase()) == true
                    }
                
                matchesQuery && matchesTags
            }

            _uiState.value = _uiState.value.copy(questions = filtered)
        }
    }

    fun addQuestion(text: String, tags: String?, difficulty: Int) {
        viewModelScope.launch {
            try {
                val question = QuestionEntity(
                    text = text,
                    tags = tags,
                    difficulty = difficulty,
                    lmsId = null
                )
                questionsDao.upsert(question)
                loadQuestions()
                EventLog.i("QuestionsViewModel", "Added new question: $text")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка добавления вопроса: ${e.message}"
                )
                EventLog.e("QuestionsViewModel", e)
            }
        }
    }

    fun updateQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            try {
                questionsDao.upsert(question)
                loadQuestions()
                EventLog.i("QuestionsViewModel", "Updated question: ${question.text}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка обновления вопроса: ${e.message}"
                )
                EventLog.e("QuestionsViewModel", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getAllTags(): List<String> {
        return _uiState.value.questions
            .flatMap { question ->
                question.tags?.split(",")?.map { it.trim() } ?: emptyList()
            }
            .distinct()
            .sorted()
    }
} 