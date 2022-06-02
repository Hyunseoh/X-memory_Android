package com.example.x_memory

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface ProfileService {
    @FormUrlEncoded
    @POST("/app_profile/")
    fun requestProfile(
        @Header("Authorization") token: String?,
        @Field("tags") tag: String?,
    ) : Call<Profile>
}