package com.edt.ut3.backend.requests

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

object CookieProvider : CookieJar {

    private val savedCookies = mutableMapOf<String, MutableSet<Cookie>>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(this) {
        (savedCookies[url.host] ?: setOf()).toList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this) {
            val cookieSet = savedCookies.getOrPut(url.host) { mutableSetOf() }
            cookieSet.addAll(cookies)
        }
    }

    fun getCookiesFor(host: String) = synchronized(this) {
        savedCookies[host]
    }

    fun removeCookiesFor(url: HttpUrl) {
        synchronized(this) {
            savedCookies.remove(url.host)
        }
    }

}