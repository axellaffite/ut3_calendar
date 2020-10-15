package com.edt.ut3.ui.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText

class TextInputEditText2(context: Context, attrs: AttributeSet? = null): TextInputEditText(context, attrs) {

    var onPreImeListener: (Int, KeyEvent?) -> Boolean = { _,_ -> false }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        return onPreImeListener(keyCode, event) || super.onKeyPreIme(keyCode, event)
    }

}