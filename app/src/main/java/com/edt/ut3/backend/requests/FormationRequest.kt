package com.edt.ut3.backend.requests

import com.google.gson.JsonElement
import retrofit2.http.GET

interface FormationRequest {

    @GET
    fun getFormations(): JsonElement

}