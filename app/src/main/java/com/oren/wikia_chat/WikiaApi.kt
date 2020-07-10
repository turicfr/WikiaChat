package com.oren.wikia_chat

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface WikiaApi {
    @GET("/wikia.php?controller=Chat&format=json")
    fun wikiaData(): Call<ResponseBody>

    @GET("/api.php?action=query&meta=siteinfo&siprop=wikidesc&format=json")
    fun siteInfo(): Call<ResponseBody>
}
