package com.edt.ut3.backend.requests

import android.content.Context
import android.util.Log
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.extensions.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*


class CelcatService {

    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun getEvents(context: Context, firstUpdate: Boolean, link: String, formations: List<String>): Response = withContext(IO) {
        val today = Date().timeCleaned()

        val body = RequestsUtils.EventBody().apply {
            val startDate =
                if (firstUpdate) { today.minus(Calendar.YEAR, 1) }
                else { today }

            add("start", (startDate).toCelcatDateStr())
            add("end", (today.add(Calendar.YEAR, 1)).toCelcatDateStr())
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


        return@withContext HttpClientProvider.generateNewClient().withAuthentication(context, request.url) {
            newCall(request).execute()
        }
    }


    @Throws(IOException::class)
    suspend fun getClasses(context: Context, link: String) = withContext(IO) {
        Log.d(this@CelcatService::class.simpleName, "Downloading classes: $link")
        val request = Request.Builder()
            .url(link)
            .get()
            .build()

        HttpClientProvider.generateNewClient().withAuthentication(context, request.url) {
            newCall(request).execute()
        }
    }

    @Throws(IOException::class)
    suspend fun getCoursesNames(context: Context, link: String) = withContext(IO) {
        Log.d(this@CelcatService::class.simpleName, "Downloading courses: $link")
        val request = Request.Builder()
            .url(link)
            .get()
            .build()

        HttpClientProvider.generateNewClient().withAuthentication(context, request.url) {
            newCall(request).execute()
        }
    }

    @Throws(IOException::class, JSONException::class)
    suspend fun getSchoolsURLs(): List<School> = withContext(IO) {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/data/formations/urls.json")
            .get()
            .build()

        val response = HttpClientProvider.generateNewClient().newCall(request).execute()
        response.body?.string()?.let { body ->
            JSONObject(body).getJSONArray("entries").map {
                School.fromJSON(it as JSONObject)
            }
        } ?: throw IOException()
    }

    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class, JSONException::class)
    suspend fun getGroups(context: Context, url: String): List<School.Info.Group> = withContext(IO) {
        Log.i(this@CelcatService::class.simpleName, "Getting groups: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = HttpClientProvider.generateNewClient().withAuthentication(context, request.url) {
            newCall(request).execute()
        }

        response.body?.string()?.let { body ->
            JSONObject(body).getJSONArray("results").map {
                School.Info.Group.fromJSON(it as JSONObject)
            }
        } ?: throw IOException()
    }
}