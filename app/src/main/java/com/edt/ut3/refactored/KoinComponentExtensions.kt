package com.edt.ut3.refactored

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

inline fun <reified T> injected(vararg parameters: Any?) =
    object: KoinComponent {
        val value: T by inject { parametersOf(parameters) }
    }.value