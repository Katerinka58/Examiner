package com.example.examiner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.examiner.data.QuestionEntity
import com.example.examiner.questions.QuestionsViewModel
import com.example.examiner.logs.EventLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    viewModel: QuestionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<QuestionEntity?>(null) }
    var showTagsFilter by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Поисковая строка
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onSearch = { },
            active = false,
            onActiveChange = { },
            placeholder = { Text("Поиск вопросов...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) { }

        // Фильтр по тегам
        if (showTagsFilter) {
            TagsFilterSection(
                allTags = viewModel.getAllTags(),
                selectedTags = uiState.selectedTags,
                onTagToggle = viewModel::toggleTag,
                onClose = { showTagsFilter = false }
            )
        }

        // Кнопки действий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { showTagsFilter = !showTagsFilter },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Фильтр")
            }

            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить вопрос")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Список вопросов
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.questions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTags.isNotEmpty()) 
                        "Вопросы не найдены" else "Нет вопросов",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.questions) { index, question ->
                    QuestionCard(
                        question = question,
                        onEdit = { showEditDialog = question },
                        onDelete = {
                            EventLog.i("QuestionsScreen", "Delete question requested: ${question.id}")
                            // TODO: Добавить подтверждение удаления
                        }
                    )
                }
            }
        }
    }

    // Диалог добавления вопроса
    if (showAddDialog) {
        QuestionDialog(
            question = null,
            onDismiss = { showAddDialog = false },
            onSave = { text, tags, difficulty ->
                viewModel.addQuestion(text, tags, difficulty)
                showAddDialog = false
            }
        )
    }

    // Диалог редактирования вопроса
    showEditDialog?.let { question ->
        QuestionDialog(
            question = question,
            onDismiss = { showEditDialog = null },
            onSave = { text, tags, difficulty ->
                viewModel.updateQuestion(question.copy(text = text, tags = tags, difficulty = difficulty))
                showEditDialog = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionCard(
    question: QuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    question.tags?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Text(
                                text = "Теги: $tags",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = "Сложность: ${question.difficulty}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsFilterSection(
    allTags: List<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтр по тегам",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            LazyColumn {
                items(allTags) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTags.contains(tag),
                            onCheckedChange = { onTagToggle(tag) }
                        )
                        Text(
                            text = tag,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionDialog(
    question: QuestionEntity?,
    onDismiss: () -> Unit,
    onSave: (text: String, tags: String?, difficulty: Int) -> Unit
) {
    var text by remember { mutableStateOf(question?.text ?: "") }
    var tags by remember { mutableStateOf(question?.tags ?: "") }
    var difficulty by remember { mutableStateOf(question?.difficulty ?: 3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (question == null) "Добавить вопрос" else "Редактировать вопрос") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст вопроса") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Теги (через запятую)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text("Сложность: $difficulty")
                Slider(
                    value = difficulty.toFloat(),
                    onValueChange = { difficulty = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text, tags.takeIf { it.isNotBlank() }, difficulty)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
} 