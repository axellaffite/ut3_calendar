package com.edt.ut3.ui.calendar.courses_visibility

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.CourseStatusData
import kotlinx.android.synthetic.main.layout_course_status.view.*

class CourseStatus(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs) {

    /**
     * Used to do an action when the
     * checkbox status changes.
     *
     * The listener will not be triggered when
     * the [setCourse] function is called unless
     * you implicitly specified it.
     */
    var checkChangedListener: ((View, Boolean) -> Unit)? = null
        set(value) {
            isVisible?.setOnCheckedChangeListener(value)
            field = value
        }


    /**
     * Used to change the displayed title value.
     * When updated, this variable automatically
     * change the displayed title value.
     */
    private var _title: String = ""
        set(value) {
            title?.text = value
            field = value
        }


    /**
     * Used to change the displayed remaining lessons value.
     * When updated, this variable automatically
     * change the displayed remaining lessons value.
     */
    private var _remaining : Int = 0
        set(value) {
            remaining?.text = context.getString(R.string.course_status_remaining).format(value)
            updateColor(value)
            field = value
        }



    init {
        inflate(context, R.layout.layout_course_status, this)

        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CourseStatus,
                0, 0
        ).apply {
            try {
                _title = getString(R.styleable.CourseStatus_title) ?: context.getString(R.string.course_status_default_title)
                _remaining = getInteger(R.styleable.CourseStatus_remaining, 0)
            } finally {
                recycle()
            }
        }
    }


    /**
     * Apply the correct style given
     * the given amount of remaining
     * lessons to do.
     *
     * @param remaining The amount of
     * remaining lessons to do (which
     * are in the future / not finished
     * yet)
     */
    private fun updateColor(remaining: Int) {
        when (remaining) {
            0 -> setPastStyle()
            else -> setFutureStyle()
        }
    }


    /**
     * Apply the style that must be used
     * when every events of this courses
     * are already past.
     */
    private fun setPastStyle() {
        title?.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
        remaining?.setTextColor(ContextCompat.getColor(context, R.color.textColorDisabled))
    }


    /**
     * Apply the style that must be displayed
     * when there are remaining events to do.
     * It means when there are events that will occurs
     * in the future.
     */
    private fun setFutureStyle() {
        title?.setTextColor(ContextCompat.getColor(context, R.color.textColorHigh))
        remaining?.setTextColor(ContextCompat.getColor(context, R.color.textColorMedium))
    }


    /**
     * Updates the current view with the provided information.
     * The previously set [checkedChangeListener] will not
     * be triggered by the value change unless you decide by
     * yourself to set the [triggerCheckedListener] parameter
     * to true.
     *
     * @param data The information that must be displayed
     * to the current view.
     *
     * @param triggerCheckedListener If set to true, the
     * function will trigger the [checkChangedListener]
     * set earlier (obviously if there is one).
     *
     * @param checkedChangeListener As you may certainly want
     * to change the listener in order to match the new provided
     * data, this parameter let you update it via this function.
     */
    fun setCourse(
        data: CourseStatusData,
        triggerCheckedListener: Boolean = false,
        checkedChangeListener: ((View, Boolean) -> Unit)? = null
    ) {
        synchronized(this) {
            _title = data.title
            _remaining = data.remaining

            /**
             * Set the listener to null in order to
             * not trigger it when the [isVisible]
             * value is updated.
             */
            if (!triggerCheckedListener) {
                isVisible.setOnCheckedChangeListener(null)
            }

            isVisible?.isChecked = data.visible

            /**
             * Update the [checkChangedListener]
             * if a value has been provided.
             */
            checkedChangeListener?.let { listener ->
                this.checkChangedListener = listener
            }
        }
    }

}