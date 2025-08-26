package com.example.examiner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.examiner.exam.ExamViewModel
import com.example.examiner.exam.ExamQuestion
import com.example.examiner.exam.ExamIsolation
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: ExamViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scope = rememberCoroutineScope()
    
    var showStartDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf("") }

    // Блокируем кнопку "Назад" во время экзамена
    LaunchedEffect(uiState.isExamStarted) {
        if (uiState.isExamStarted) {
            // TODO: Реализовать блокировку кнопки "Назад"
            // backDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            //     override fun handleOnBackPressed() {
            //         // Игнорируем нажатие кнопки "Назад"
            //     }
            // })
        }
    }

    // Обновляем время каждую секунду
    LaunchedEffect(uiState.isExamStarted) {
        if (uiState.isExamStarted) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                viewModel.updateTimeRemaining()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок экзамена
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Экзамен",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Статус экзамена
                    Text(
                        text = if (uiState.isExamStarted) "В процессе" else "Не начат",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (uiState.isExamStarted) Color.Green else Color.Gray
                    )
                    
                    // Оставшееся время
                    if (uiState.isExamStarted) {
                        Text(
                            text = viewModel.getTimeRemainingFormatted(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.timeRemaining < 300000) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Прогресс
                if (uiState.isExamStarted) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = viewModel.getProgress(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Прогресс: ${(viewModel.getProgress() * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!uiState.isExamStarted && !uiState.isExamFinished) {
            // Экран перед началом экзамена
            PreExamScreen(
                onStartExam = { showStartDialog = true },
                onToggleProctoring = viewModel::toggleProctoring,
                onToggleScreenPinning = viewModel::toggleScreenPinning,
                proctoringEnabled = uiState.proctoringEnabled,
                screenPinningEnabled = uiState.screenPinningEnabled
            )
        } else if (uiState.isExamStarted) {
            // Экран экзамена
            ExamInProgressScreen(
                uiState = uiState,
                onAnswerQuestion = viewModel::answerQuestion,
                onNextQuestion = viewModel::nextQuestion,
                onPreviousQuestion = viewModel::previousQuestion,
                onFinishExam = { showFinishDialog = true },
                getCurrentQuestion = viewModel::getCurrentQuestion
            )
        } else if (uiState.isExamFinished) {
            // Экран завершения экзамена
            ExamFinishedScreen(
                onBackToMain = {
                    // TODO: Навигация обратно к главному экрану
                }
            )
        }
    }

    // Диалог начала экзамена
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Начать экзамен") },
            text = {
                Column {
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("ID студента") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Внимание! После начала экзамена:\n" +
                                "• Приложение будет заблокировано\n" +
                                "• Кнопка \"Назад\" будет отключена\n" +
                                "• Включится прокторинг (если разрешено)\n" +
                                "• Экран будет закреплен (если разрешено)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (studentId.isNotBlank()) {
                            scope.launch {
                                // Включаем изоляцию экрана
                                if (uiState.screenPinningEnabled) {
                                    // TODO: ExamIsolation.startScreenPinning(activity)
                                }
                                viewModel.startExam(studentId.toLongOrNull() ?: 1L)
                                showStartDialog = false
                            }
                        }
                    }
                ) {
                    Text("Начать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог завершения экзамена
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Завершить экзамен") },
            text = {
                Text(
                    text = "Вы уверены, что хотите завершить экзамен? " +
                            "Это действие нельзя отменить.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Отключаем изоляцию экрана
                            if (uiState.screenPinningEnabled) {
                                // TODO: ExamIsolation.stopScreenPinning(activity)
                            }
                            viewModel.finishExam()
                            showFinishDialog = false
                        }
                    }
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Показ ошибок
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Показать Snackbar с ошибкой
            viewModel.clearError()
        }
    }
}

@Composable
private fun PreExamScreen(
    onStartExam: () -> Unit,
    onToggleProctoring: () -> Unit,
    onToggleScreenPinning: () -> Unit,
    proctoringEnabled: Boolean,
    screenPinningEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Настройки экзамена",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Настройки безопасности
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Прокторинг",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = proctoringEnabled,
                    onCheckedChange = { onToggleProctoring() }
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Закрепление экрана",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = screenPinningEnabled,
                    onCheckedChange = { onToggleScreenPinning() }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onStartExam,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Начать экзамен")
            }
        }
    }
}

@Composable
private fun ExamInProgressScreen(
    uiState: com.example.examiner.exam.ExamUiState,
    onAnswerQuestion: (Long, String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onFinishExam: () -> Unit,
    getCurrentQuestion: () -> ExamQuestion?
) {
    val currentQuestion = getCurrentQuestion()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Навигация по вопросам
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onPreviousQuestion,
                enabled = uiState.currentQuestionIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Text("Предыдущий")
            }
            
            Text(
                text = "Вопрос ${uiState.currentQuestionIndex + 1} из ${uiState.questions.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onNextQuestion,
                enabled = uiState.currentQuestionIndex < uiState.questions.size - 1
            ) {
                Text("Следующий")
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Текущий вопрос
        currentQuestion?.let { question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = question.studentAnswer,
                        onValueChange = { onAnswerQuestion(question.id, it) },
                        label = { Text("Ваш ответ") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 10
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Сложность: ${question.difficulty}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // Кнопка завершения
        Button(
            onClick = onFinishExam,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Завершить экзамен")
        }
    }
}

@Composable
private fun ExamFinishedScreen(
    onBackToMain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Экзамен завершен",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Ваши ответы сохранены и будут проверены преподавателем.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onBackToMain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Вернуться к главному экрану")
            }
        }
    }
} 