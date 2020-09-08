package com.edt.ut3.backend.requests

import android.util.Log
import com.edt.ut3.misc.plus
import com.edt.ut3.misc.timeCleaned
import com.edt.ut3.misc.toCelcatDateStr
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.days


class CelcatService {
    @ExperimentalTime
    @Throws(IOException::class)
    suspend fun getEvents(link: String, formations: List<String>): Response = withContext(IO) {
        val today = Date().timeCleaned()

        val body = RequestsUtils.EventBody().apply {
                add("start", (today).toCelcatDateStr())
                add("end", (today + 365.days).toCelcatDateStr())
                formations.forEach {
                    add("federationIds%5B%5D", it)
                }
            }.build()

        Log.d("CELCAT_SERVICE", "Request body: $body")

        val encodedBody = body.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url("$link/Home/GetCalendarData")
            .addHeader("Accept", "application/json, text/javascript")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Content-Length", encodedBody.contentLength().toString())
            .post(encodedBody)
            .build()

        HttpClientProvider.generateNewClient().newCall(request).execute()
    }


    @Throws(IOException::class)
    suspend fun getClasses(link: String) = withContext(IO) {
        println(link)
        val search =
            if (link.contains("calendar2")) { "___" }
            else { "__" }

        val request = Request.Builder()
            .url("$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=102&_=1595177163927")
            .get()
            .build()

        HttpClientProvider.generateNewClient().newCall(request).execute()
    }

    @Throws(IOException::class)
    suspend fun getCoursesNames(link: String) = withContext(IO) {
        val search =
            if (link.contains("calendar2")) { "___" }
            else { "__" }

        val request = Request.Builder()
            .url("$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=10000000&pageNumber=1&resType=100&_=1595183277988")
            .get()
            .build()

        HttpClientProvider.generateNewClient().newCall(request).execute()
    }
}