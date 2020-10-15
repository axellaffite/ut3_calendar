package com.edt.ut3.misc.extensions

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun Any?.isNotNull() : Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }

    return this != null
}

@OptIn(ExperimentalContracts::class)
@SinceKotlin("1.3")
inline fun Any?.isNull() : Boolean {
    contract {
        returns(true) implies (this@isNull == null)
    }

    return this == null
}