package com.example.x_memory

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface UploadService {

    @FormUrlEncoded
    @POST("/app_upload/")
    fun requestUpload(
        @Header("Authorization") token: String?,
        @Field("photo") path:String
    ) : Call<Upload>

}
