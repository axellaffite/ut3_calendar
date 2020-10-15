package com.edt.ut3.misc.extensions

import android.text.Html
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun Date.addAssign(field: Int, amount: Int) {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(field, amount)

    time = calendar.timeInMillis
}

fun Date.add(field: Int, amount: Int): Date = Calendar.getInstance().run {
    time = this@add
    add(field, amount)
    time
}

fun Date.minus(field: Int, amount: Int) =
    this.add(field, -amount)

fun Date.minusAssign(field: Int, amount: Int) =
    this.addAssign(field, -amount)

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
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.DAY_OF_MONTH, day)

    time = calendar.timeInMillis
    cleanTime()
}

fun Date.setTime(hour: Int, minute: Int, second: Int = 0, milliSecond: Int = 0) = this.apply {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, second)
    calendar.set(Calendar.MILLISECOND, milliSecond)

    time = calendar.time.time
}

fun Date.toFormattedTime(format: String) = SimpleDateFormat(format).format(this)