package com.edt.ut3.ui.calendar

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class EventView(context: Context, ev: Event.Wrapper): CardView(context) {
    private var startTime = 0L
    private var elevationJob: Job? = null

    init {
        addView(
            TextView(context).apply {
                text = generateCardContents(ev.event)
                setBackgroundColor(Color.parseColor("#FF" + ev.event.backGroundColor?.substring(1)))
                setTextColor(Color.parseColor("#FF" + ev.event.textColor?.substring(1)))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )

                gravity = Gravity.CENTER
            }
        )

        radius = context.resources.getDimension(R.dimen.event_radius)
        cardElevation = 0f
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startTime = Calendar.getInstance().timeInMillis

                elevationJob?.cancel()
                elevationJob = GlobalScope.launch {
                    while (deltaTime() < 1000) {
                        elevation = deltaTime() * 0.1f
                        delay(5)
                    }

                    println("end")
                }

                true
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                elevationJob?.cancel()
                if (startTime > 0L && deltaTime() > 600L) {
                    performLongClick()
                }
                elevation = 0f
                startTime = 0L
                true
            }

            else -> false
        }
    }

    private fun deltaTime() =
        Calendar.getInstance().timeInMillis - startTime

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