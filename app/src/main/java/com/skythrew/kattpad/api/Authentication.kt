package com.skythrew.kattpad.api

import com.skythrew.kattpad.api.requests.UserData
import io.ktor.client.request.cookie
import io.ktor.client.statement.bodyAsText
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.parameters
import io.ktor.http.setCookie

open class Authentication : com.skythrew.kattpad.api.config.Request() {
    var username: String? = null
        private set
    var loggedIn = false
        private set

    fun logout() {
        loggedIn = false
        username = null
        updateHttpClient()
    }

    suspend fun login(username: String, password: String) {
        logout()
        val response = simplePost("https://www.wattpad.com/login", parameters {
            append("username", username.trim())
            append("password", password)
        })
        val token = response.setCookie().firstOrNull { it.name == "token" }?.value
            ?.decodeURLQueryComponent() ?: getToken()
        if (token.isNullOrBlank()) return
        // Do not mark an account authenticated until the server validates the session.
        loginByCookie(token)
    }

    suspend fun loginByCookie(cookie: String) {
        val response = getAPI("v3", "internal/current_user") {
            cookie(name = "token", value = cookie)
            url { parameters.append("fields", "username") }
        }
        val user = jsonDecoder.decodeFromString<UserData>(response.bodyAsText())
        require(user.username.isNotBlank()) { "Missing account username" }
        updateHttpClient(cookie)
        username = user.username
        loggedIn = true
    }
}
