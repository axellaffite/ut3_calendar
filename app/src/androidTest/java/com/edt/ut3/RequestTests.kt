package com.edt.ut3

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edt.ut3.backend.requests.RequestsManager
import com.edt.ut3.misc.minus
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
}