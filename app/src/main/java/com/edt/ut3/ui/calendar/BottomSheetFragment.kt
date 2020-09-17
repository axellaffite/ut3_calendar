package com.edt.ut3.ui.calendar

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import java.lang.ref.WeakReference


open class BottomSheetFragment: Fragment() {

    val bottomSheetManager = BottomSheetManager()

    override fun onDestroy() {
        super.onDestroy()

        bottomSheetManager.clear()
    }

    class BottomSheetManager {
        private val bottomSheets = mutableSetOf<WeakReference<View>>()

        fun hasVisibleSheet() : Boolean {
            return bottomSheets.map { ref ->
                ref.get()?.let { v ->
                    BottomSheetBehavior.from(v).state
                }
            }.contains(STATE_EXPANDED)
        }

        /**
         * Setup the bottom sheets in order to
         * handle them when the setVisibleSheet()
         * function is called.
         *
         * @param sheets
         */
        fun add(vararg sheets: View) {
            bottomSheets.addAll(sheets.map { WeakReference(it) })
        }

        fun setVisibleSheet(bottomSheet: View?) {
            bottomSheets.forEach {
                it.get()?.let { view ->
                    val behavior = BottomSheetBehavior.from(view)
                    if (view === bottomSheet) {
                        behavior.state = STATE_EXPANDED
                    } else {
                        behavior.state = STATE_COLLAPSED
                    }
                }
            }
        }

        fun clear() = bottomSheets.clear()
    }

}