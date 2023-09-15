package com.edt.ut3.misc.extensions

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun <T> JSONArray.toList(): List<T> =
    (0 until length()).map {
        get(it) as T
    }

@Throws(JSONException::class)
fun  JsonObject.toStringMap(): Map<String, String> {
    val map = HashMap<String, String>()
    for (key in keys) {
        map[key] = get(key).toString()
    }
    return map
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

fun JsonObject.getNotNull(key: String) = when (val value = get(key)) {
    null, is JsonNull -> null
    else -> value
}