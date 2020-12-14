package com.edt.ut3.backend.requests

import kotlinx.serialization.json.Json

val JsonWebDeserializer = Json { ignoreUnknownKeys = true }
