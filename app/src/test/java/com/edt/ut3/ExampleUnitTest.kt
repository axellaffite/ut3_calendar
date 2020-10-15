package com.edt.ut3

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    fun synchronizedTest() {

    }

    suspend fun synchronizedFunction() {
        synchronized(this) {

        }
    }

}