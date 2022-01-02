package com.edt.ut3.refactored.extensions

import java.util.*

fun Date.get(field: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(field)
}

val Date.actualYear get() = get(Calendar.YEAR)
val Date.actualMonth get() = get(Calendar.MONTH)
val Date.actualDayOfMonth get() = get(Calendar.DAY_OF_MONTH)
val Date.actualDayOfWeek get() = get(Calendar.DAY_OF_WEEK)
val Date.actualHour get() = get(Calendar.HOUR_OF_DAY)
val Date.actualMinutes get() = get(Calendar.MINUTE)
val Date.actualSeconds get() = get(Calendar.SECOND)
val Date.actualMilliseconds get() = get(Calendar.MILLISECOND)