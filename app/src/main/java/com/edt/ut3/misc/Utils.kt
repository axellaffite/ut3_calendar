package com.edt.ut3.misc

import android.text.Html
import android.text.format.DateFormat
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
operator fun Date.plusAssign(duration: Duration) {
    time += duration.inMilliseconds.toLong()
}

@ExperimentalTime
operator fun Date.plus(duration: Duration) = Date(
    time + duration.inMilliseconds.toLong()
)

@ExperimentalTime
operator fun Date.minus(duration: Duration) =
    this.plus(-duration)

@ExperimentalTime
operator fun Date.minusAssign(duration: Duration) =
    this.plusAssign(-duration)

fun Date.toCelcatDateStr() =
    DateFormat.format("yyyy-MM-dd", this).toString()

fun Date.toCelcatDateTimeStr() =
    DateFormat.format("yyyy-MM-dd", this).toString()

@Throws(Exception::class)
fun Date.fromCelcatString(date: String) {
    time =  Html.escapeHtml(date).toString().replace('T', ' ').let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).parse(it)
    }?.time ?: throw Exception()
}

fun Date.cleaned(vararg fields: Int) : Date {
    return Date(time).apply {
        clean(*fields)
    }
}

fun Date.clean(vararg fields: Int) {
    time = Calendar.getInstance().let {
        it.time = this
        for (f in fields) {
            it.set(f, 0)
        }

        it.time.time
    }
}

fun Date.timeCleaned() = Date(time).apply {
    cleanTime()
}

fun Date.cleanTime() = clean(
    Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND
)

fun Date.set(year: Int, month: Int, day: Int): Date = this.apply {
    time = Calendar.getInstance().run {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        time.time
    }

    cleanTime()
}



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

fun String.fromHTML() : String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString();
    } else {
        Html.fromHtml(this).toString();
    }
}