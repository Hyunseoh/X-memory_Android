package com.example.x_memory

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface UploadDetailService {

    @FormUrlEncoded
    @POST("/app_detail/")
    fun requestUploadDetail(
        @Header("Authorization") token: String?,
        @Field("latitude") latitude:String,
        @Field("longitude") longitude:String?,
        @Field("time") time: String?,
        @Field("photo") photo_id:Int,

    ) : Call<UploadDetail>

}
