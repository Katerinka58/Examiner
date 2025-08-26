package com.example.examiner.di

import android.content.Context
import androidx.room.Room
import com.example.examiner.data.AppDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Providers {
    @Volatile private var db: AppDatabase? = null
    fun database(context: Context): AppDatabase = db ?: synchronized(this) {
        db ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "examiner.db"
        ).fallbackToDestructiveMigration().build().also { db = it }
    }

    @Volatile private var retrofit: Retrofit? = null
    fun retrofit(): Retrofit = retrofit ?: synchronized(this) {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl("https://example-lms.local/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .also { retrofit = it }
    }
} 