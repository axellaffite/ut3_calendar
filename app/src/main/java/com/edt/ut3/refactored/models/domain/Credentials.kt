package com.edt.ut3.refactored.models.domain

data class Credentials(
    val username: String,
    val password: String
) { companion object }

fun Credentials.Companion.from(username: String?, password: String?): Credentials? {
    return if (username != null && password != null) {
        Credentials(username, password)
    } else {
        null
    }
}