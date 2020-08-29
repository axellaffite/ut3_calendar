package com.edt.ut3.backend.requests

import okhttp3.Request
import okhttp3.Response

class CrousService {

    private val CROUS_API_LINK = "https://data.enseignementsup-recherche.gouv.fr/api/records/1.0/search/?dataset=fr_crous_restauration_france_entiere&q=&facet=type&facet=zone&refine.zone=Toulouse"

    /**
     * This will retrieve the crous data which contains some information
     * such as the number of founded places,
     * the records (the founded places), their
     * localisation and so on.
     * For further information check the following link :
     * https://data.enseignementsup-recherche.gouv.fr/explore/dataset/fr_crous_restauration_france_entiere/api/?refine.zone=Toulouse
     *
     * @throws java.io.IOException This can be thrown when
     * the server can't be reached, when network isn't
     * available or when a timeout is encountered.
     *
     * @return The data as a Response
     */
    @Throws(java.io.IOException::class)
    fun getCrousPlaces(): Response {
        val request = Request.Builder()
            .url(CROUS_API_LINK)
            .get()
            .build()

        return HttpClientProvider.generateNewClient().newCall(request).execute()
    }

}