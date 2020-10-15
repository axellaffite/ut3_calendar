package com.edt.ut3.backend.requests

import com.edt.ut3.misc.extensions.isNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieProvider private constructor(): CookieJar {

    private val savedCookies = mutableMapOf<String, MutableSet<Cookie>>()

    companion object {

        private var instance: CookieProvider? = null

        fun getInstance(): CookieProvider {
            synchronized(this) {
                if (instance.isNull()) {
                    instance = CookieProvider()
                }

                return instance!!
            }
        }

    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(this) {
            return (savedCookies[url.host] ?: setOf()).toList()
        }
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