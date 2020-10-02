package com.edt.ut3.ui.custom_views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.misc.toDp
import com.google.android.material.button.MaterialButton


class UniqueChoiceItem<Data>(context: Context, attributeSet: AttributeSet? = null): MaterialButton(
    context,
    attributeSet
) {

    constructor(context: Context, data: Data, converter: (Data) -> String): this(context) {
        this.data = data
        text = converter(data)

        setBackgroundColor(ContextCompat.getColor(context, R.color.foregroundColor))
        setTextColor(ContextCompat.getColor(context, R.color.textColor))

        val iconColor = ContextCompat.getColor(context, R.color.iconTint)
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled), // disabled
            intArrayOf(-android.R.attr.state_enabled), // disabled
            intArrayOf(-android.R.attr.state_checked), // unchecked
            intArrayOf(-android.R.attr.state_pressed)  // unpressed
        )
        iconTint = ColorStateList(
            states,
            (0..3).map { iconColor }.toIntArray()
        )
    }

    var data: Data? = null

    init {
        isCheckable = true

        val dps = 16.toDp(context).toInt()
        cornerRadius = dps
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(dps, dps, dps, dps)
        }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)

        val drawable: Drawable? =
            if (isChecked) {
                ContextCompat.getDrawable(context, R.drawable.ic_checked)?.apply {
                    val iconColor = ContextCompat.getColor(
                        context,
                        R.color.iconTint
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(iconColor, BlendMode.SRC_ATOP)
                    } else {
                        setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
            } else { null }

        setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }

}