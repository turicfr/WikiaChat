package com.oren.wikia_chat

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginApi {
    @POST("/api.php?action=login&format=json")
    fun login(
        @Query("lgname") lgname: String,
        @Query("lgpassword") lgpassword: String
    ): Call<ResponseBody>

    @POST("/api.php?action=login&format=json")
    fun login(
        @Query("lgname") lgname: String,
        @Query("lgpassword") lgpassword: String,
        @Query("lgtoken") lgtoken: String
    ): Call<ResponseBody>

    @GET("/wikia.php?controller=Chat&format=json")
    fun wikiaData(): Call<ResponseBody>

    @GET("/api.php?action=query&meta=siteinfo&siprop=wikidesc&format=json")
    fun siteInfo(): Call<ResponseBody>
}
