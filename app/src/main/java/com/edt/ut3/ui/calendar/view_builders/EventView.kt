package com.edt.ut3.ui.calendar.view_builders

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.preferences.Theme

class EventView(context: Context, private var ev: Event.Wrapper): CardView(context) {

    var padding: Int? = null
    var event: Event

    init {
        event = ev.event
        setEvent(ev)

        radius = context.resources.getDimension(R.dimen.event_radius)
        cardElevation = 0f
        elevation = 0f
    }

    fun setEvent(newEvent: Event.Wrapper) {
        event = newEvent.event

        removeAllViews()

        addView(
            TextView(context).apply {
                text = generateCardContents(ev.event)
                when (PreferencesManager(context).currentTheme()) {
                    Theme.LIGHT -> {
                        setBackgroundColor(ev.event.lightBackgroundColor(context))
                        setTextColor(Color.parseColor("#FF" + ev.event.textColor?.substring(1)))
                    }

                    Theme.DARK -> {
                        setBackgroundColor(ev.event.darkBackgroundColor(context))
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )

                isVerticalScrollBarEnabled = false

                padding?.let {
                    setPadding(it, it, it, it)
                }

                gravity = Gravity.CENTER

                ellipsize = TextUtils.TruncateAt.END
            }
        )
    }


    private fun generateCardContents(event: Event) : String {
        val description = StringBuilder()
        if (event.locations.size == 1) {
            description.append(event.locations.first()).append("\n")
        }

        description.append(event.courseOrCategory(context)).append("\n")

        if (description.isEmpty()) {
            description.append(event.description)
        }

        return description.toString().removeSuffix("\n")
    }
}