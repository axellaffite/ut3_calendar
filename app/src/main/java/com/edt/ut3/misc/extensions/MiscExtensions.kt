package com.edt.ut3.misc.extensions

import android.content.Context
import android.util.TypedValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject

fun Number.toDp(context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
)

fun<K,V> MutableMap<K,V>.toHashMap() = HashMap(this)

fun<E> Iterable<E>.toJSONArray(converter: (E) -> Any?) : JSONArray {
    return JSONArray().also { array ->
        forEach { e ->
            array.put(JSONObject.wrap(converter(e)))
        }
    }
}

fun Map<String, String>.toJsonOject() : JsonObject {
    return buildJsonObject {
        for(key in keys){
            put(key, get(key))
        }
    }
}