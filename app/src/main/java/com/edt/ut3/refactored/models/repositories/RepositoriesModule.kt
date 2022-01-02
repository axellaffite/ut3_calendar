package com.edt.ut3.refactored.models.repositories

import androidx.room.Room
import com.edt.ut3.refactored.models.services.workers.updaters.CelcatUpdaterService
import com.edt.ut3.refactored.models.services.workers.updaters.UpdaterService
import com.edt.ut3.refactored.models.services.notifications.NotificationManagerService
import com.edt.ut3.backend.requests.JsonSerializer
import com.edt.ut3.refactored.models.services.celcat.CelcatService
import com.edt.ut3.refactored.models.repositories.database.AppDatabase
import com.edt.ut3.refactored.models.repositories.preferences.PreferencesManager
import com.edt.ut3.refactored.viewmodels.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.scope.Scope
import org.koin.dsl.module


val repositoriesModule = module {
    single { createDatabase() }
    single { CredentialsRepository(androidContext()) }
    single { CelcatService() }
    single<UpdaterService> { params ->
        CelcatUpdaterService(params.getOrNull() ?: get())
    }

    single { PreferencesManager.getInstance(androidContext()) }

    factory { createClient() }
    factory { NotificationManagerService(androidContext()) }
}


fun createClient() = HttpClient(CIO) {
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }

    install(JsonFeature) {
        serializer = KotlinxSerializer(JsonSerializer)
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }

    install(HttpRedirect) {
        checkHttpMethod = false
    }
}

fun Scope.createDatabase() =
    Room.databaseBuilder(androidContext(), AppDatabase::class.java, "note_event_db")
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()