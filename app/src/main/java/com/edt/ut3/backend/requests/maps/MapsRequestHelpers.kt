package com.edt.ut3.backend.requests.maps

import com.edt.ut3.backend.maps.Place


data class PlacesRequest(val records: List<Record>)


data class Record(val fields: Place)