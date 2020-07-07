package com.elzozor.ut3calendar

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.elzozor.ut3calendar.backend.requests.RequestsManager
import com.elzozor.ut3calendar.misc.minus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.days

@RunWith(AndroidJUnit4::class)
class RequestTests {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @ExperimentalTime
    @Test
    fun downloadFile() {
        RequestsManager(context).celcatService().let {
            GlobalScope.launch {
                assert(
                    it.getEvents(
                        it.generateCelcatBody(
                            Date() - (6.days * 31),
                            Date(),
                            listOf("LINF6TDA1")
                        )
                    ).size > 3
                )
            }
        }
    }
}