package com.example.x_memory

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface LoginService{

    @FormUrlEncoded
    @POST("/app_login/")
    fun requestLogin(
//        @Header("Authorization") token: String?,
        @Field("userid") userid:String,
        @Field("userpw") userpw:String
    ) : Call<Login>

}