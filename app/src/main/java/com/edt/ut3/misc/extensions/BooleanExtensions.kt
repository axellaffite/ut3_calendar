package com.edt.ut3.misc.extensions

fun Boolean?.isTrue() = this == true
fun Boolean?.isNullOrFalse() = this == null || this == false