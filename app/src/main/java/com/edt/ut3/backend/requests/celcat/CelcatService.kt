package com.edt.ut3.backend.requests.celcat

import android.content.Context
import android.util.Log
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.HttpClientProvider
import com.edt.ut3.backend.requests.JsonWebDeserializer
import com.edt.ut3.backend.requests.RequestsUtils
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.withAuthentication
import com.edt.ut3.misc.extensions.add
import com.edt.ut3.misc.extensions.minus
import com.edt.ut3.misc.extensions.timeCleaned
import com.edt.ut3.misc.extensions.toCelcatDateStr
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*


@Suppress("BlockingMethodInNonBlockingContext")
object CelcatService {

    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun getEvents(
        context: Context,
        firstUpdate: Boolean,
        link: String,
        formations: List<String>
    ): Response = withContext(IO) {
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


        val encodedBody = body.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url("$link/Home/GetCalendarData")
            .addHeader("Accept", "application/json, text/javascript")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Content-Length", encodedBody.contentLength().toString())
            .post(encodedBody)
            .build()


        return@withContext HttpClientProvider.generateNewClient()
            .withAuthentication(
                request.url,
                credentials = CredentialsManager.getInstance(context).getCredentials()
            ) {
                newCall(request).execute()
            }
    }


    @Throws(SocketTimeoutException::class, IOException::class, SerializationException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun getClasses(context: Context, link: String) = withContext(IO) {
        Log.d(this@CelcatService::class.simpleName, "Downloading classes: $link")
        val request = Request.Builder()
            .url(link)
            .get()
            .build()

        val response = HttpClientProvider
            .generateNewClient()
            .withAuthentication(
                request.url,
                credentials = CredentialsManager.getInstance(context).getCredentials()
            ) {
                newCall(request).execute()
            }

        val body = response.body?.string() ?: throw IOException()

        JsonWebDeserializer.decodeFromString<ClassesRequest>(body).results
    }

    @Throws(SocketTimeoutException::class, IOException::class, SerializationException::class, Authenticator.InvalidCredentialsException::class)
    suspend fun getCoursesNames(context: Context, link: String) = withContext(IO) {
        Log.d(this@CelcatService::class.simpleName, "Downloading courses: $link")
        val request = Request.Builder()
            .url(link)
            .get()
            .build()

        val response = HttpClientProvider
            .generateNewClient()
            .withAuthentication(
                request.url,
                credentials = CredentialsManager.getInstance(context).getCredentials()
            ) {
                newCall(request).execute()
            }

        val body = response.body?.string() ?: throw IOException()

        JsonWebDeserializer.decodeFromString<CoursesRequest>(body).results.toMap()
    }

    @Throws(SocketTimeoutException::class, IOException::class, SerializationException::class)
    suspend fun getSchoolsURLs(): List<School> = withContext(IO) {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/data/formations/urls.json")
            .get()
            .build()

        val response = HttpClientProvider
            .generateNewClient()
            .newCall(request)
            .execute()

        val body = response.body?.string() ?: throw IOException()

        JsonWebDeserializer.decodeFromString<SchoolsRequest>(body).entries
    }

    @Throws(SocketTimeoutException::class, IOException::class, Authenticator.InvalidCredentialsException::class, SerializationException::class)
    suspend fun getGroups(context: Context, url: String): List<School.Info.Group> = withContext(IO) {
        Log.i(this@CelcatService::class.simpleName, "Getting groups: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = HttpClientProvider
            .generateNewClient()
            .withAuthentication(
                request.url,
                credentials = CredentialsManager.getInstance(context).getCredentials()
            ) {
                newCall(request).execute()
            }

        val body = response.body?.string() ?: throw IOException()

        JsonWebDeserializer.decodeFromString<GroupsRequest>(body).results
    }

}