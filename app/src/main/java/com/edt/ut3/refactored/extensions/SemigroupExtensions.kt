package com.edt.ut3.refactored.extensions

import arrow.typeclasses.Semigroup
import java.lang.Exception

class MultipleExceptions(vararg val causes: Throwable): Exception()

fun Semigroup.Companion.throwable() = object: Semigroup<Throwable> {
    override fun Throwable.combine(b: Throwable): Throwable {
        return if (this is MultipleExceptions) {
            MultipleExceptions(*causes, b)
        } else {
            MultipleExceptions(this, b)
        }
    }
}