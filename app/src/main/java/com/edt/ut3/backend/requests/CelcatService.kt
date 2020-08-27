package com.edt.ut3.backend.requests

import android.util.Log
import com.edt.ut3.misc.set
import com.edt.ut3.misc.toCelcatDateStr
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.time.ExperimentalTime


//interface CelcatService {
//    @Headers(
//        "Accept: application/json, text/javascript",
//        "X-Requested-With: XMLHttpRequest",
//        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
//        "Connection: keep-alive")
//    @POST("/calendar2/Home/GetCalendarData")
//    fun getEvents(@Body celcatBody: String): Call<JsonObject>
//}


class CelcatService {
    @ExperimentalTime
    @Throws(IOException::class)
    fun getEvents(formations: List<String>) : Response {
        //TODO Changer la date pour quelque chose de dynamique !
        val body = RequestsUtils.EventBody().apply {
                add("start", (Date().set(2020, Calendar.JANUARY, 1)).toCelcatDateStr())
                add("end", (Date().set(2024, 7, 1)).toCelcatDateStr())
                formations.map {
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