package com.edt.ut3.backend.background_services.updater

import com.edt.ut3.backend.background_services.updater.celcat.CelcatUpdater
import java.lang.IllegalStateException

object UpdaterFactory {

    @Throws(IllegalStateException::class)
    fun createUpdater(method: String): Updater {
        return when(method) {
            "celcat" -> CelcatUpdater()
            else -> throw IllegalStateException("Unable to create an Updater for method: $method")
        }
    }

}