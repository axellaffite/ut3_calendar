package com.edt.ut3.refactored.models.services.maps

import com.edt.ut3.refactored.models.domain.maps.Place
import kotlinx.serialization.Serializable

@Serializable
data class PlacesRequest (val records: List<Record>)

@Serializable
data class Record (val fields: Place)