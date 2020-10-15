package com.edt.ut3.misc.extensions

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment

fun Activity.hideKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE)
    with (imm as InputMethodManager?) {
        this?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
}

fun Fragment.addOnBackPressedListener(enabled: Boolean = true,
                                      onBackPressed: OnBackPressedCallback.() -> Unit) {
    activity
        ?.onBackPressedDispatcher
        ?.addCallback(viewLifecycleOwner, enabled, onBackPressed)
}

fun Fragment.onBackPressed() = activity?.onBackPressed()

fun Fragment.hideKeyboard() {
    val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE)
    with (imm as InputMethodManager?) {
        this?.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}