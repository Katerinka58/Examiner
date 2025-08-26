package com.example.examiner.lms

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LmsApi {
    @GET("api/courses")
    suspend fun getCourses(): Response<ResponseBody>

    @GET("api/groups")
    suspend fun getGroups(): Response<ResponseBody>

    @Multipart
    @POST("api/submissions/{studentId}/upload")
    suspend fun uploadSubmission(
        @Path("studentId") studentId: String,
        @Part file: MultipartBody.Part,
        @Part("type") type: String
    ): Response<ResponseBody>
} 