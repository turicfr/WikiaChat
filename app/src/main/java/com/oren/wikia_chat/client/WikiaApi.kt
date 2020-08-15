package com.oren.wikia_chat.client

import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface WikiaApi {
    @GET("/wikia.php?controller=Chat&format=json")
    fun wikiaData(): Call<ResponseBody>

    @GET("/api.php?action=query&meta=siteinfo&siprop=wikidesc&prop=info&titles=titles&intoken=edit&format=json")
    fun siteInfo(): Call<ResponseBody>

    @FormUrlEncoded
    @POST("/index.php?action=ajax&rs=ChatAjax&method=getPrivateRoomID")
    fun getPrivateRoomId(
        @Field("users") users: JSONArray,
        @Field("token") token: String
    ): Call<ResponseBody>
}
