package com.edt.ut3.misc.extensions

import android.widget.EditText

fun EditText.updateIfNecessary(text: String?) = synchronized(this) {
    val isNullAndFieldNotEmpty = (text == null) && (this.text.isNotEmpty())
    val different = (text != null) && (this.text.toString() != text)
    val shouldUpdate = isNullAndFieldNotEmpty || different

    if (shouldUpdate) {
        setText(text)
    }
}