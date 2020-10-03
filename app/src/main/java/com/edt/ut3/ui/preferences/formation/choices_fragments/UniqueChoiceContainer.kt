package com.edt.ut3.ui.preferences.formation.choices_fragments

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.view.children
import com.edt.ut3.ui.custom_views.UniqueChoiceItem
import com.google.android.material.button.MaterialButton

class UniqueChoiceContainer<Data>(context: Context, attributeSet: AttributeSet? = null): LinearLayout(context, attributeSet) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    var onChoiceDone: (() -> Unit)? = null
    private var choice: Data? = null

    fun setDataSet(dataSet: Array<Data>, converter: (Data) -> String) {
        removeAllViews()
        dataSet.forEach { data ->
            addView(UniqueChoiceItem(context, data, converter).apply {
                setOnClickListener {
                    select(this)
                    choice = this.data
                }
            })
        }
    }

    fun select(view: UniqueChoiceItem<Data>) {
        children.forEach {
            with (it as MaterialButton) {
                isChecked = (it == view)
            }
        }

        choice = view.data
        if (choice != null) {
            onChoiceDone?.invoke()
        }
    }

    @Throws(IllegalStateException::class)
    fun getChoice() =
        choice ?:
        throw IllegalStateException("Choice isn't valid, this function should not be called")

}