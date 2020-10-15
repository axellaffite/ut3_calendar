package com.edt.ut3.misc.extensions

import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.children

fun RadioGroup.getSelectedItem() = this.children.find { it is RadioButton && it.isChecked }

fun RadioGroup.getSelectedItemIndex() = this.children.indexOfFirst { it is RadioButton && it.isChecked }