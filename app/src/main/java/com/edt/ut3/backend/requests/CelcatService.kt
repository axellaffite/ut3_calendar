package com.edt.ut3.backend.requests

import com.edt.ut3.misc.toCelcatDateStr
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.*

interface CelcatService {
    @Headers(
        "Host: edt.univ-tlse3.fr",
        "Accept: application/json, text/javascript",
        "X-Requested-With: XMLHttpRequest")
    @POST("Home/GetCalendarData")
    suspend fun getEvents(celcatBody: String): String
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