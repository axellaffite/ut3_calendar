package com.edt.ut3.misc.extensions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.json.JSONArray
import org.json.JSONException

@Throws(JSONException::class)
fun <T> JSONArray.toList(): List<T> =
    (0 until length()).map {
        get(it) as T
    }

fun JsonNode.toStringMap(): Map<String, String> {
    return fields().asSequence().associate { (key, value) ->
        key to value.asText()
    }
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
fun <T> JsonNode.realOpt(key: String): T? {
    return if (hasNonNull(key)) {
        get(key) as T
    } else {
        null
    }
}

fun JsonNode.getNotNull(key: String): JsonNode? = when {
    !has(key) || get(key) is NullNode || get(key).isNull -> null
    else -> get(key)
}