package com.oren.wikia_chat.client

import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.http.*

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

    // TODO: move to separate interface?
    @GET("/api/v1/Wikis/ByString?expand=1&limit=6&includeDomain=true")
    fun getWikis(
        @Query("string") query: String
    ): Call<ResponseBody>
}
