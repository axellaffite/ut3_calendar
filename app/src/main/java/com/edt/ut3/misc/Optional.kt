package com.edt.ut3.misc

class Optional<T> private constructor() {

    private var isInit = false
    private var value : T? = null

    private constructor(v: T): this() {
        value = v
        isInit = true
    }

    companion object {
        fun<T> empty() = Optional<T>()
        fun<T> of(v: T) = Optional(v)
    }

    fun ifInit(callback: (T?) -> Unit) =
        callback
            .takeIf { isInit }
            ?.invoke(value)
}