package com.edt.ut3

import com.edt.ut3.misc.set
import com.edt.ut3.misc.timeCleaned
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*
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

    @Test
    fun date_build_test() {
        val today = Date().timeCleaned()
        val today_test = Date().set(2020, Calendar.JULY, 18)

        assert(today_test == today)
    }
}