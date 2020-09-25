package com.oren.wikia_chat.client

import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class ObjectCallback<T>(private val callback: Controller.Callback<T>) :
    Callback<ResponseBody> {
    final override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        val body = if (response.isSuccessful) response.body() else response.errorBody()
        val obj = JSONObject(body?.string()!!)
        try {
            if (response.isSuccessful) onObject(obj) else onFailure(obj)
        } catch (e: Throwable) {
            callback.onFailure(e)
            return
        }
    }

    final override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
        callback.onFailure(t)
    }

    protected abstract fun onObject(obj: JSONObject)

    protected open fun onFailure(obj: JSONObject): Unit = throw Exception()
}
