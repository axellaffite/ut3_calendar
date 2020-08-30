package com.edt.ut3.backend.requests

import okhttp3.Request
import okhttp3.Response

class MapsServices {

    companion object {
        private const val CROUS_API_LINK = "https://data.enseignementsup-recherche.gouv.fr/api/records/1.0/search/?dataset=fr_crous_restauration_france_entiere&q=&rows=-1&facet=type&facet=zone&refine.zone=Toulouse"
        private const val PAUL_SABATIER_PLACES_LINK = "https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/maps_data/data.json"
    }

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

    /**
     * This will download the Paul Sabatier places from
     * our github repository.
     * We've made this choice to keep up to date
     * the building and amphitheaters along the time
     * without the need to perform application updates.
     *
     * @return A response that contains places
     */
    @Throws(java.io.IOException::class)
    fun getPaulSabatierPlaces() : Response {
        val request = Request.Builder()
            .url(PAUL_SABATIER_PLACES_LINK)
            .get()
            .build()

        return HttpClientProvider.generateNewClient().newCall(request).execute()
    }

}