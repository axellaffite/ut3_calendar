package com.edt.ut3.backend.requests

import android.util.Log
import com.edt.ut3.misc.plus
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toCelcatDateStr
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.days


class CelcatService {
    @ExperimentalTime
    @Throws(IOException::class)
    fun getEvents(formations: List<String>) : Response {
        val today = Date().timeCleaned()

        val body = RequestsUtils.EventBody().apply {
                add("start", (today).toCelcatDateStr())
                add("end", (today + 365.days).toCelcatDateStr())
                formations.forEach {
                    add("federationIds%5B%5D", it)
                }
            }.build()

        Log.d("CELCAT_SERVICE", "Request body: $body")

        val encodedBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"), body)


        val request = Request.Builder()
            .url("https://edt.univ-tlse3.fr/calendar2/Home/GetCalendarData")
            .addHeader("Accept", "application/json, text/javascript")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Content-Length", encodedBody.contentLength().toString())
            .post(encodedBody)
            .build()

        return OkHttpClient().newCall(request).execute()
    }


    @Throws(IOException::class)
    fun getClasses() : Response {
        val request = Request.Builder()
            .url("https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=1000000&pageNumber=1&resType=102&_=1595177163927")
            .get()
            .build()

        return OkHttpClient().newCall(request).execute()
    }

    @Throws(IOException::class)
    fun getCoursesNames() : Response {
        val request = Request.Builder()
            .url("https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000000&pageNumber=1&resType=100&_=1595183277988")
            .get()
            .build()

        return OkHttpClient().newCall(request).execute()
    }
}