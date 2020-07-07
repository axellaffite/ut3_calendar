package com.elzozor.ut3calendar.backend.requests

import com.elzozor.ut3calendar.backend.requests.Utils.generateCelcatBody
import com.elzozor.ut3calendar.misc.toCelcatDateStr
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.*

interface CelcatService {
    @Headers(
        "Host: edt.univ-tlse3.fr",
        "Accept: application/json, text/javascript",
        "X-Requested-With: XMLHttpRequest")
    @POST("Home/GetCalendarData")
    suspend fun getEvents(celcatBody: String): Response<String>
}

object Utils {
    fun generateCelcatBody(calStart: Date, calEnd: Date, studentGroups: List<String>) : String {
        val federationIds = studentGroups.joinToString(separator = "&federationIds[]")
        val start = calStart
        val end = calEnd
        val resType = 103
        val calView = "agendaDay"
        val colourScheme = 3

        return arrayOf(
            "start=${start.toCelcatDateStr()}",
            "end=${end.toCelcatDateStr()}",
            "resType=$resType",
            "calView=$calView",
            "colourScheme$colourScheme",
            "federationIds[]=$federationIds"
        ).joinToString(separator = "&")
    }
}