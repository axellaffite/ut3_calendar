package com.elzozor.ut3calendar

import com.elzozor.ut3calendar.misc.*
import org.junit.Test

import org.junit.Assert.*
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.hours

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @ExperimentalTime
    @Test
    fun date_test() {
        val d = Date()
        assertEquals(
            d + 6.hours,
            addNHours(d, 6)
        )
    }

    fun addNHours(date: Date, n: Int) =
        Calendar.getInstance().let {
            it.time = date
            it.add(Calendar.HOUR_OF_DAY, n)
            it.time
        }
}