package com.edt.ut3.backend.background_services.updaters

import com.edt.ut3.backend.network.getClient
import io.ktor.client.*


inline fun getUpdater(init: HttpClient.() -> Unit = {}): Updater {
    return CelcatUpdater(getClient().apply(init))
}