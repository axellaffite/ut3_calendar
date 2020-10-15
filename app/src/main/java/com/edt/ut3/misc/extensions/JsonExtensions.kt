package com.edt.ut3.misc.extensions

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun <T> JSONArray.toList(): List<T> =
    (0 until length()).map {
        get(it) as T
    }

fun <T> JSONArray.map(consumer: (Any?) -> T) =
    (0 until length()).map {
        consumer(get(it))
    }

fun JSONArray.forEach(consumer: (Any?) -> Unit) {
    (0 until length()).forEach {
        consumer(get(it))
    }
}

@Suppress("UNCHECKED_CAST")
fun<T> JSONObject.realOpt(key: String): T? {
    return if (isNull(key)) {
        null
    } else {
        get(key) as T
    }
}