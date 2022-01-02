package com.edt.ut3.refactored.models.services.maps

import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.backend.requests.JsonSerializer
import com.edt.ut3.refactored.injected
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import java.io.IOException

class MapsService {

    companion object {
        private const val CROUS_API_LINK =
            "https://data.enseignementsup-recherche.gouv.fr/api/records/1.0/search/?dataset=fr_crous_restauration_france_entiere&q=&rows=-1&facet=type&facet=zone&refine.zone=Toulouse"
        private const val PAUL_SABATIER_PLACES_LINK =
            "https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/maps_data/data.json"
    }

    /**
     * This will retrieve the crous data which contains some information
     * such as the number of founded places,
     * the records (the founded places), their
     * localisation and so on.
     * For further information check the following link :
     * https://data.enseignementsup-recherche.gouv.fr/explore/dataset/fr_crous_restauration_france_entiere/api/?refine.zone=Toulouse
     *
     * @throws IOException This can be thrown when
     * the server can't be reached, when network isn't
     * available or when a timeout is encountered.
     *
     * @return The data as a Response
     */
    @Throws(IOException::class, SerializationException::class)
    suspend fun getCrousPlaces(client: HttpClient = injected()): List<Place> {
        val response = client.get<String>(CROUS_API_LINK) {
            header(HttpHeaders.Accept, ContentType.Text)
        }

        return JsonSerializer.decodeFromString<PlacesRequest>(response).records.map { it.fields }
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
    suspend fun getPaulSabatierPlaces(client: HttpClient = injected()): List<Place> {
        val response =  client.get<String>(PAUL_SABATIER_PLACES_LINK) {
            header(HttpHeaders.Accept, ContentType.Text)
        }

        return JsonSerializer.decodeFromString<PlacesRequest>(response).records.map { it.fields }
    }

}