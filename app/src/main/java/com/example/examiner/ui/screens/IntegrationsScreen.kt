package com.example.examiner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.examiner.integrations.IntegrationsViewModel
import com.example.examiner.integrations.LmsCourse
import com.example.examiner.integrations.LmsGroup
import com.example.examiner.integrations.UploadedFile
import com.example.examiner.integrations.UploadStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    viewModel: IntegrationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFileUploadDialog by remember { mutableStateOf(false) }
    var selectedFileType by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Статус подключения
        ConnectionStatusCard(uiState.isConnected, uiState.isLoading)
        
        Spacer(Modifier.height(16.dp))
        
        // Кнопки синхронизации
        SyncButtonsCard(
            onSyncCourses = viewModel::syncCourses,
            onSyncGroups = viewModel::syncGroups,
            syncInProgress = uiState.syncInProgress
        )
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Курсы из LMS
            item {
                Text(
                    text = "Курсы из LMS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.courses) { course ->
                CourseCard(course = course)
            }
            
            // Группы из LMS
            item {
                Text(
                    text = "Группы из LMS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.groups) { group ->
                GroupCard(group = group)
            }
            
            // Загруженные файлы
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Загруженные файлы",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = { showFileUploadDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Загрузить файл")
                    }
                }
            }
            
            items(uiState.uploadedFiles) { file ->
                UploadedFileCard(
                    file = file,
                    onRemove = { viewModel.removeUploadedFile(file.id) }
                )
            }
        }
    }

    // Диалог загрузки файла
    if (showFileUploadDialog) {
        FileUploadDialog(
            onDismiss = { showFileUploadDialog = false },
            onUpload = { file, type, studentId ->
                viewModel.uploadFile(file, studentId, type)
                showFileUploadDialog = false
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
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isConnected) Color.Green else Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Подключение к LMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isLoading) "Проверка подключения..." 
                           else if (isConnected) "Подключено" 
                           else "Не подключено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) Color.Green else Color.Red
                )
            }
        }
    }
}

@Composable
private fun SyncButtonsCard(
    onSyncCourses: () -> Unit,
    onSyncGroups: () -> Unit,
    syncInProgress: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Синхронизация",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncCourses,
                    enabled = !syncInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Курсы")
                }
                
                Button(
                    onClick = onSyncGroups,
                    enabled = !syncInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Группы")
                }
            }
            
            if (syncInProgress) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CourseCard(course: LmsCourse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                course.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = if (course.isActive) "Активный" else "Неактивный",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (course.isActive) Color.Green else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun GroupCard(group: LmsGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Студентов: ${group.studentCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UploadedFileCard(
    file: UploadedFile,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (file.status) {
                    UploadStatus.SUCCESS -> Icons.Default.CheckCircle
                    UploadStatus.FAILED -> Icons.Default.Close
                    UploadStatus.UPLOADING -> Icons.Default.Refresh
                    UploadStatus.PENDING -> Icons.Default.Person
                },
                contentDescription = null,
                tint = when (file.status) {
                    UploadStatus.SUCCESS -> Color.Green
                    UploadStatus.FAILED -> Color.Red
                    UploadStatus.UPLOADING -> MaterialTheme.colorScheme.primary
                    UploadStatus.PENDING -> Color.Gray
                }
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Тип: ${file.type}, Размер: ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Загружен: ${formatDate(file.uploadDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = when (file.status) {
                        UploadStatus.SUCCESS -> "Загружено успешно"
                        UploadStatus.FAILED -> "Ошибка загрузки"
                        UploadStatus.UPLOADING -> "Загружается..."
                        UploadStatus.PENDING -> "Ожидает загрузки"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (file.status) {
                        UploadStatus.SUCCESS -> Color.Green
                        UploadStatus.FAILED -> Color.Red
                        UploadStatus.UPLOADING -> MaterialTheme.colorScheme.primary
                        UploadStatus.PENDING -> Color.Gray
                    }
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
private fun FileUploadDialog(
    onDismiss: () -> Unit,
    onUpload: (java.io.File, String, String) -> Unit
) {
    var selectedFile by remember { mutableStateOf<java.io.File?>(null) }
    var fileType by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Загрузить файл") },
        text = {
            Column {
                // TODO: Добавить выбор файла
                Text(
                    text = "Выбор файла будет реализован позже",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = fileType,
                    onValueChange = { fileType = it },
                    label = { Text("Тип файла (код/текст/PDF)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("ID студента") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // TODO: Реализовать загрузку файла
                    onDismiss()
                }
            ) {
                Text("Загрузить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size Б"
        size < 1024 * 1024 -> "${size / 1024} КБ"
        else -> "${size / (1024 * 1024)} МБ"
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
} 