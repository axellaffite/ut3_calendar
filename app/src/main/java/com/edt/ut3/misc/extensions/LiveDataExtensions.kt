package com.edt.ut3.misc.extensions

import androidx.lifecycle.MutableLiveData


fun MutableLiveData<*>.trigger() = synchronized(this) {
    this.value = this.value
}