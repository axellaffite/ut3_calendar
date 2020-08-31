package com.edt.ut3.ui.calendar

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.Theme

class EventView(context: Context, ev: Event.Wrapper): CardView(context) {
//    private var startTime = 0L
//    private var elevationJob: Job? = null

    init {
        addView(
            TextView(context).apply {
                text = generateCardContents(ev.event)
                when (PreferencesManager(context).getTheme()) {
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

                gravity = Gravity.CENTER
            }
        )

        radius = context.resources.getDimension(R.dimen.event_radius)
        cardElevation = 0f
        elevation = 0f
    }


//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        return when (event?.action) {
//            MotionEvent.ACTION_DOWN -> {
//                startTime = Calendar.getInstance().timeInMillis
//
//                elevationJob?.cancel()
//                elevationJob = GlobalScope.launch {
//                    while (deltaTime() < 1000) {
//                        elevation = deltaTime() * 0.1f
//                        delay(5)
//                    }
//
//                    println("end")
//                }
//
//                true
//            }
//
//            MotionEvent.ACTION_UP -> {
//                performClick()
//                elevationJob?.cancel()
//                if (startTime > 0L && deltaTime() > 600L) {
//                    performLongClick()
//                }
//                elevation = 0f
//                startTime = 0L
//                true
//            }
//
//            MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_CANCEL -> {
//                elevationJob?.cancel()
//                elevation = 0f
//                startTime = 0L
//                false
//            }
//
//            else -> false
//        }
//    }
//
//    private fun deltaTime() =
//        Calendar.getInstance().timeInMillis - startTime

    private fun generateCardContents(event: Event) : String {
        val description = StringBuilder()
        if (event.locations.size == 1) {
            description.append(event.locations.first())
        }

        if (event.courseName != null) {
            if (description.isNotEmpty()) {
                description.append("\n")
            }

            description.append(event.courseName)
        }

        if (description.isEmpty()) {
            description.append(event.description)
        }

        return description.toString()
    }
}