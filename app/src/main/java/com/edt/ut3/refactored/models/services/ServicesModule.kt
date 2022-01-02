package com.edt.ut3.refactored.models.services

import com.edt.ut3.refactored.models.services.authentication.AbstractAuthenticator
import com.edt.ut3.refactored.models.services.authentication.AuthenticatorUT3Service
import com.edt.ut3.refactored.models.services.maps.MapsService
import org.koin.dsl.module

val servicesModule = module {
    single { DayBuilderService() }
    single { MapsService() }

    factory<AbstractAuthenticator> { params ->
        AuthenticatorUT3Service(
            client = params.getOrNull() ?: get(),
            host = params.getOrNull() ?: "https://edt.univ-tlse3.fr/calendar2"
        )
    }
}