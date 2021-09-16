package com.edt.ut3.backend.network.events

import com.edt.ut3.backend.requests.getClient
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.misc.extensions.minus
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class CelcatServiceTest {

    @Test
    fun downloadEvents() {
        runBlocking {
            val result = CelcatService(getClient()).getEvents(
                link = "https://edt.univ-tlse3.fr/calendar2",
                start = Date().minus(Calendar.YEAR, 1),
                formations = listOf("MINDLIHM8TPA22"),
                emptySet(),
                emptyMap()
            )

            println(result)
        }
    }

}