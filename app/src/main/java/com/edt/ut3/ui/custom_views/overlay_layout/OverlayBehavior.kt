package com.edt.ut3.ui.custom_views.overlay_layout

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import com.edt.ut3.misc.toDp
import java.io.InvalidClassException
import kotlin.math.abs


/**
 * This class is used to provide a behavior
 * that allows the principal layout
 * to scroll left in order to display
 * an hidden layout.
 * It works almost like the Discord's one
 * in terms of visual effects.
 *
 * @param V
 * @constructor
 *
 * @param context
 * @param attrs
 */
class OverlayBehavior<V : View>(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<V>(context, attrs) {

    companion object {
        @Throws(InvalidClassException::class)
        fun from(v: View) : OverlayBehavior<View> {
            val params = v.layoutParams as CoordinatorLayout.LayoutParams
            if (params.behavior !is OverlayBehavior) {
                throw InvalidClassException("The provided layout does not contain an OverlayBehavior")
            }

            return params.behavior as OverlayBehavior<View>
        }
    }

    enum class Position { IDLE, LEFT, RIGHT }

    private var startX = 0f
    private var offsetX = 0f
    private var startY = 0f
    private var offsetY = 0f
    private var viewX = 0f

    private var status = Position.IDLE
        set(value) {
            onStatusChangeListeners.forEach { it() }
            field = value
        }

    var canAnimate = true

    var canSwipe = false

    /**
     * The onSwipingLeftListeners will be called
     * every time the view's status is equal to IDLE
     * and when the view is moving to the left.
     * They will no be called when the status is equal
     * to RIGHT or LEFT.
     * Triggered are called sequentially in random order.
     * Don't forget to test the nullability
     * because it can be triggered when the view is destroyed.
     */
    val onSwipingLeftListeners = HashSet<() -> Unit>()

    /**
     * The onSwipingRightListeners will be called
     * every time the view's status is equal to IDLE
     * and when the view is moving to the right.
     * They will no be called when the status is equal
     * to RIGHT or LEFT.
     * Triggered are called sequentially in random order.
     * Don't forget to test the nullability
     * because it can be triggered when the view is destroyed.
     */
    val onSwipingRightListeners = HashSet<() -> Unit>()

    /**
     * The onMoveListeners will be called
     * every time the view is moving no matter what
     * is its status.
     * Triggered are called sequentially in random order.
     * Don't forget to test the nullability
     * because it can be triggered when the view is destroyed.
     */
    val onMoveListeners = HashSet<() -> Unit>()

    /**
     * The onStatusChangeListeners will be called
     * every time the view's status is updated no
     * matter is the status is different from
     * the old one or not.
     * Triggered are called sequentially in random order.
     * Don't forget to test the nullability
     * because it can be triggered when the view is destroyed.
     */
    val onStatusChangeListeners = HashSet<() -> Unit>()


    /**
     * Determines whether or not the event must
     * be intercepted.
     * Basically we only intercept the event
     * if the scroll is horizontal and
     * if the scroll amount is greater than
     * a given offset (10px in this case).
     *
     * @param parent
     * @param child
     * @param ev
     * @return
     */
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        println("Action: "+ ev.action + "${ev.action == MotionEvent.ACTION_MOVE}")
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.rawX
                startY = ev.rawY
                viewX = child.x

                return false
            }

            MotionEvent.ACTION_MOVE -> {
                offsetX = ev.rawX - startX
                offsetY = ev.rawY - startY

                println("intercept: $offsetX $offsetY")
                val handled = when (status) {
                    Position.LEFT -> {
                        offsetX > 50f
                    }

                    Position.RIGHT -> {
                        offsetX < -50f
                    }

                    else -> {
                        (canSwipe
                                && canAnimate
                                && abs(offsetX) > abs(offsetY)
                                && abs(offsetX) > 10f)
                    }
                }

                if (handled) {
                    onMoveListeners.forEach { it() }
                }

                handled
            }

            else -> false
        }
    }

    override fun onTouchEvent(qzd: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        val max = maxForView(child)

        return when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                offsetX = ev.rawX - startX
                offsetY = ev.rawY - startY

                val moved = when (status) {
                    Position.IDLE -> {
                        child.translationX = (viewX + offsetX).coerceIn(-max, max)
                        if (abs(offsetX) > abs(offsetY)) {
                            if (child.x < 0f) {
                                onSwipingLeftListeners.forEach { it() }
                            } else if (child.x > 0f) {
                                onSwipingRightListeners.forEach { it() }
                            }
                        }

                        true
                    }

                    Position.LEFT -> {
                        child.translationX = (viewX + offsetX).coerceIn(-max, 0f)
                        offsetX > 0
                    }

                    Position.RIGHT -> {
                        child.translationX = (viewX + offsetX).coerceIn(0f, max)
                        offsetX < 0
                    }
                }

                return moved && abs(offsetX) > abs(offsetY)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (offsetX != 0f) {
                    moveView(child)
                    return true
                }

                return false
            }

            else -> false
        }
    }

    private fun maxForView(view: View): Float {
        val px = 40.toDp(view.context)

        return view.width - px
    }

    private fun shouldGoLeft(child: View): Boolean {
        return (status == Position.IDLE && child.x < -(child.width * (4f/9f)))
                || (status == Position.LEFT && child.x < -(child.width * (5f/9f)))
    }

    private fun shouldGoRight(child: View): Boolean {
        return (status == Position.IDLE && child.x > (child.width * (4f/9f))
                || status == Position.RIGHT && child.x > (child.width * (5f/9f)))
    }

    private fun moveView(view: View) {
        val callback : ((View) -> Unit)

        when {
            shouldGoLeft(view) -> {
                callback = { v -> moveViewLeft(v) { status = Position.LEFT } }
            }
            shouldGoRight(view) -> {
                callback = { v -> moveViewRight(v) { status = Position.RIGHT } }
            }
            else -> {
                callback = { v -> moveViewIdle(v) { status = Position.IDLE } }
            }
        }

        animateIfPossible(callback, view)
    }

    @Synchronized
    private fun animateIfPossible(callback: (View) -> Unit, v: View) {
        if (!canAnimate) {
            return
        }

        canAnimate = false
        callback(v)
    }

    private fun moveViewIdle(v: View, onEnd: (() -> Unit)? = null) {
        val pos = 0f
        val animationDuration = interpolate(maxForView(v), 500f, abs(v.width - abs(-maxForView(v)))).toLong()

        ObjectAnimator.ofFloat(v, "translationX", pos).apply {
            duration = animationDuration
            doOnEnd {
                canAnimate = true
                onEnd?.invoke()
            }

            start()
        }
    }

    private fun moveViewLeft(v: View, onEnd: (() -> Unit)? = null) {
        val pos = maxForView(v)
        val animationDuration = interpolate(pos, 500f, abs(offsetX)).toLong()

        ObjectAnimator.ofFloat(v, "translationX", -pos).apply {
            duration = animationDuration
            doOnEnd {
                canAnimate = true
                onEnd?.invoke()
            }

            start()
        }
    }

    private fun moveViewRight(v: View, onEnd: (() -> Unit)? = null) {
        val pos = maxForView(v)
        val animationDuration = interpolate(pos, 500f, abs(offsetX)).toLong()

        ObjectAnimator.ofFloat(v, "translationX", pos).apply {
            duration = animationDuration
            doOnEnd {
                canAnimate = true
                onEnd?.invoke()
            }

            start()
        }
    }


    private fun interpolate(max: Float, interMax: Float, value: Float): Float {
        return (interMax * (value / max))
    }

}