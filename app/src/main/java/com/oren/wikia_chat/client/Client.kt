package com.oren.wikia_chat.client

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import java.net.CookieManager

class Client {
    private val httpClient = OkHttpClient.Builder()
        /*.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })*/
        .cookieJar(JavaNetCookieJar(CookieManager()))
        .build()

    lateinit var user: User
        private set

    private val controllers = mutableMapOf<Int, Controller>()

    fun getController(wikId: Int) = controllers[wikId]

    fun addController(wikiId: Int, wikiUrl: String, callback: Controller.Callback<Controller>) {
        controllers[wikiId] = Controller(this).apply {
            init(httpClient, wikiUrl, callback)
        }
    }

    fun login(username: String, password: String, callback: Controller.Callback<Unit>) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://services.fandom.com/")
            .client(httpClient)
            .build()
        val loginApi = retrofit.create(LoginApi::class.java)
        loginApi.login(username, password).enqueue(object : ObjectCallback<Unit>(callback) {
            override fun onObject(obj: JSONObject) {
                user = User(
                    username,
                    "https://services.fandom.com/user-avatar/user/${obj.getString("user_id")}/avatar",
                )
                callback.onSuccess(Unit)
            }

            override fun onFailure(obj: JSONObject) =
                throw Exception(obj.getString("error_description"))
        })
    }

    fun logout() {
        controllers.values.forEach {
            it.logout()
        }
    }
}
