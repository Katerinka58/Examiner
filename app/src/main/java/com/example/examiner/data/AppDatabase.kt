package com.example.examiner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val tags: String?,
    val difficulty: Int,
    val lmsId: String?
)

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val studentId: Long,
    val answerText: String?,
    val grade: Double?,
    val comment: String?,
    val createdAt: Long
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long,
    val courseId: Long,
    val externalId: String?
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Dao
interface QuestionsDao {
    @Query("SELECT * FROM questions ORDER BY id DESC")
    suspend fun getAll(): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(question: QuestionEntity): Long
}

@Dao
interface AnswersDao {
    @Query("SELECT * FROM answers WHERE questionId = :questionId ORDER BY createdAt DESC")
    suspend fun byQuestion(questionId: Long): List<AnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(answer: AnswerEntity): Long
}

@Dao
interface StudentsDao {
    @Query("SELECT * FROM students WHERE groupId = :groupId")
    suspend fun byGroup(groupId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE courseId = :courseId")
    suspend fun byCourse(courseId: Long): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: StudentEntity): Long
}

@Dao
interface GroupsDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    suspend fun getAll(): List<GroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupEntity): Long
}

@Dao
interface CoursesDao {
    @Query("SELECT * FROM courses ORDER BY name ASC")
    suspend fun getAll(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity): Long
}

@Database(
    entities = [
        QuestionEntity::class,
        AnswerEntity::class,
        StudentEntity::class,
        GroupEntity::class,
        CourseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionsDao(): QuestionsDao
    abstract fun answersDao(): AnswersDao
    abstract fun studentsDao(): StudentsDao
    abstract fun groupsDao(): GroupsDao
    abstract fun coursesDao(): CoursesDao
} 