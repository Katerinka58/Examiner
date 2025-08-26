package com.example.examiner.logs

import android.util.Log

object EventLog {
    fun i(event: String, message: String) {
        Log.i("Examiner", "$event: $message")
    }
    fun e(event: String, error: Throwable) {
        Log.e("Examiner", "$event: ${error.message}", error)
    }
} 