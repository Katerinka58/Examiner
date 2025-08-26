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
import com.example.examiner.analytics.AnalyticsViewModel
import com.example.examiner.analytics.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTimeRange by remember { mutableStateOf(TimeRange.ALL_TIME) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок и фильтр времени
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Аналитика",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Период:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeRange.values().forEach { timeRange ->
                        FilterChip(
                            selected = selectedTimeRange == timeRange,
                            onClick = {
                                selectedTimeRange = timeRange
                                viewModel.setTimeRange(timeRange)
                            },
                            label = {
                                Text(
                                    when (timeRange) {
                                        TimeRange.WEEK -> "Неделя"
                                        TimeRange.MONTH -> "Месяц"
                                        TimeRange.QUARTER -> "Квартал"
                                        TimeRange.ALL_TIME -> "Все время"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Общая статистика
                item {
                    OverallStatsCard(uiState)
                }

                // Топ групп
                item {
                    TopGroupsCard(
                        groups = viewModel.getTopPerformingGroups(),
                        title = "Лучшие группы"
                    )
                }

                // Топ курсов
                item {
                    TopCoursesCard(
                        courses = viewModel.getTopPerformingCourses(),
                        title = "Лучшие курсы"
                    )
                }

                // Группы с низким процентом сдачи
                item {
                    LowPassRateCard(
                        groups = viewModel.getGroupsWithLowPassRate(),
                        title = "Группы с низким процентом сдачи"
                    )
                }

                // Курсы с низким процентом сдачи
                item {
                    LowPassRateCoursesCard(
                        courses = viewModel.getCoursesWithLowPassRate(),
                        title = "Курсы с низким процентом сдачи"
                    )
                }

                // Детальная аналитика по группам
                item {
                    Text(
                        text = "Аналитика по группам",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.groups) { group ->
                    GroupAnalyticsCard(group = group)
                }

                // Детальная аналитика по курсам
                item {
                    Text(
                        text = "Аналитика по курсам",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.courses) { course ->
                    CourseAnalyticsCard(course = course)
                }
            }
        }
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
private fun OverallStatsCard(uiState: com.example.examiner.analytics.AnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Общая статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Групп",
                    value = uiState.groups.size.toString(),
                    icon = Icons.Default.Person
                )
                StatItem(
                    label = "Курсов",
                    value = uiState.courses.size.toString(),
                    icon = Icons.Default.Person
                )
                StatItem(
                    label = "Ответов",
                    value = uiState.groups.sumOf { it.answersCount }.toString(),
                    icon = Icons.Default.Person
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopGroupsCard(
    groups: List<com.example.examiner.analytics.GroupAnalytics>,
    title: String
) {
    AnalyticsListCard(
        title = title,
        items = groups,
        getValue = { it.avgGrade },
        getLabel = { it.groupName },
        getSubtitle = { "Студентов: ${it.studentsCount}, Ответов: ${it.answersCount}" },
        valueFormatter = { "%.1f".format(it) },
        valueSuffix = " балл"
    )
}

@Composable
private fun TopCoursesCard(
    courses: List<com.example.examiner.analytics.CourseAnalytics>,
    title: String
) {
    AnalyticsListCard(
        title = title,
        items = courses,
        getValue = { it.avgGrade },
        getLabel = { it.courseName },
        getSubtitle = { "Студентов: ${it.studentsCount}, Ответов: ${it.answersCount}" },
        valueFormatter = { "%.1f".format(it) },
        valueSuffix = " балл"
    )
}

@Composable
private fun LowPassRateCard(
    groups: List<com.example.examiner.analytics.GroupAnalytics>,
    title: String
) {
    AnalyticsListCard(
        title = title,
        items = groups,
        getValue = { it.passRate },
        getLabel = { it.groupName },
        getSubtitle = { "Студентов: ${it.studentsCount}, Ответов: ${it.answersCount}" },
        valueFormatter = { "%.1f".format(it) },
        valueSuffix = "%",
        valueColor = Color.Red
    )
}

@Composable
private fun LowPassRateCoursesCard(
    courses: List<com.example.examiner.analytics.CourseAnalytics>,
    title: String
) {
    AnalyticsListCard(
        title = title,
        items = courses,
        getValue = { it.passRate },
        getLabel = { it.courseName },
        getSubtitle = { "Студентов: ${it.studentsCount}, Ответов: ${it.answersCount}" },
        valueFormatter = { "%.1f".format(it) },
        valueSuffix = "%",
        valueColor = Color.Red
    )
}

@Composable
private fun <T> AnalyticsListCard(
    title: String,
    items: List<T>,
    getValue: (T) -> Double,
    getLabel: (T) -> String,
    getSubtitle: (T) -> String,
    valueFormatter: (Double) -> String,
    valueSuffix: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            if (items.isEmpty()) {
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = getLabel(item),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getSubtitle(item),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = valueFormatter(getValue(item)) + valueSuffix,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = valueColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAnalyticsCard(group: com.example.examiner.analytics.GroupAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsMetric(
                    label = "Средний балл",
                    value = "%.1f".format(group.avgGrade),
                    suffix = " балл"
                )
                AnalyticsMetric(
                    label = "Процент сдачи",
                    value = "%.1f".format(group.passRate),
                    suffix = "%",
                    color = if (group.passRate < 60) Color.Red else MaterialTheme.colorScheme.primary
                )
                AnalyticsMetric(
                    label = "Студентов",
                    value = group.studentsCount.toString()
                )
            }
        }
    }
}

@Composable
private fun CourseAnalyticsCard(course: com.example.examiner.analytics.CourseAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsMetric(
                    label = "Средний балл",
                    value = "%.1f".format(course.avgGrade),
                    suffix = " балл"
                )
                AnalyticsMetric(
                    label = "Процент сдачи",
                    value = "%.1f".format(course.passRate),
                    suffix = "%",
                    color = if (course.passRate < 60) Color.Red else MaterialTheme.colorScheme.primary
                )
                AnalyticsMetric(
                    label = "Студентов",
                    value = course.studentsCount.toString()
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMetric(
    label: String,
    value: String,
    suffix: String = "",
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value + suffix,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
} 