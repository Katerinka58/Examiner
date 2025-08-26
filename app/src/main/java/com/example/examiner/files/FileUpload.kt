package com.example.examiner.files

import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileUploadHelper {
    fun buildPartFromFile(path: String, formFieldName: String = "file"): MultipartBody.Part {
        val file = File(path)
        val ext = file.extension
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val body = file.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(formFieldName, file.name, body)
    }
} 