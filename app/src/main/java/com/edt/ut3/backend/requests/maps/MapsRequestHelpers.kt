package com.edt.ut3.backend.requests.maps

import com.edt.ut3.backend.maps.Place
import kotlinx.serialization.Serializable

@Serializable
data class PlacesRequest (val records: List<Record>)

@Serializable
data class Record (val fields: Place)