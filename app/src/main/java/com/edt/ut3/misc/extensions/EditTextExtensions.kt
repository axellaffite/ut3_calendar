package com.edt.ut3.misc.extensions

import android.util.Log
import android.widget.EditText

fun EditText.updateIfNecessary(text: String?) = synchronized(this) {
    val shouldUpdate = this.text.toString() != text

    if (shouldUpdate) {
        println("UPDATE")
        setText(text)
    }
}