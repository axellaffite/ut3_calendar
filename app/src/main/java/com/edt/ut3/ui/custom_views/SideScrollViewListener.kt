package com.edt.ut3.ui.custom_views

import android.view.MotionEvent
import android.view.View

class SideScrollViewListener(private val view: View) {
    
    enum class State {CENTER, UP, RIGHT, DOWN, LEFT}

    var onScrollUpListener : (() -> Boolean) = { false }
    var onScrollDownListener : (() -> Boolean) = { false }
    var onScrollLeftListener : (() -> Boolean) = { false }
    var onScrollRightListener : (() -> Boolean) = { false }

    var onCancelUpListener : (() -> Boolean) = { false }
    var onCancelDownListener : (() -> Boolean) = { false }
    var onCancelLeftListener : (() -> Boolean) = { false }
    var onCancelRightListener : (() -> Boolean) = { false }

    private var state = State.CENTER

    private var offsetX = 0f
    private var offsetY = 0f
    private var startY = 0f
    private var startX = 0f

    private var width = 0f
    private var height = 0f
    private var slideOffset = 0f

    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> handleDownAction(ev)

            MotionEvent.ACTION_MOVE -> handleMoveAction(ev)

            MotionEvent.ACTION_UP -> handleUpAction(ev)

            else -> false
        }
    }

    private fun handleDownAction(ev: MotionEvent): Boolean {
        startY = ev.rawY
        startX = ev.rawX

        return false
    }

    private fun handleMoveAction(ev: MotionEvent): Boolean {
        offsetY = ev.rawY - startY
        offsetX = ev.rawX - startX

        return when(state) {
            State.CENTER -> moveActionAtCenter(ev)

            else -> false
        }
    }

    private fun moveActionAtCenter(ev: MotionEvent): Boolean {
        println("Moving: start_y=$startY startx=$startX y=$offsetY x=$offsetX")
        println("Offset: $slideOffset")
        var handled = false
        when {
            startX < widthOffsetStart() && offsetX > slideOffset -> {
                handled = onScrollLeftListener()
                if (handled) {
                    state = State.LEFT
                }
            }

            startX > widthOffsetEnd() && -offsetX > slideOffset -> {
                handled = onScrollRightListener()
                if (handled) {
                    state = State.RIGHT
                }
            }

            startY < heightOffsetStart() && offsetY > slideOffset -> {
                handled = onScrollUpListener()
                if (handled) {
                    state = State.UP
                }
            }

            startY > heightOffsetEnd() && -offsetY > slideOffset -> {
                handled = onScrollDownListener()
                if (handled) {
                    state = State.DOWN
                }
            }
        }

        return handled
    }

    fun updateDimensions(width: Float, height: Float) {
        this.width = width
        this.height = height

        slideOffset = width.coerceAtMost(height) * 1f/5f
    }

    private fun widthOffset() = view.width * 1f/3f
    private fun widthOffsetStart() =  widthOffset() + view.x
    private fun widthOffsetEnd() = view.right - widthOffset()
    private fun heightOffset() = view.height * 1f/3f
    private fun heightOffsetStart() = view.y + heightOffset()
    private fun heightOffsetEnd() = view.bottom - heightOffset()

    private fun handleUpAction(ev: MotionEvent): Boolean {
        startY = 0f
        startX = 0f

        return false
    }
}