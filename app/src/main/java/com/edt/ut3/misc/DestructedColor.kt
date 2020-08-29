package com.edt.ut3.misc

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.edt.ut3.R

/**
 * Destructed color is used to perform operations on colors.
 * The ability to access inner members such as R, G, B makes
 * it easier to perform transformation.
 * It also provides function to get the color luminosity
 * and to darker (or lighter) the color.
 *
 * @property r The red amount (0.0 -> 255.0)
 * @property g The green amount (0.0 -> 255.0)
 * @property b The blue amount (0.0 -> 255.0)
 */
class DestructedColor(var r: Double = 0.0, var g: Double = 0.0, var b: Double = 0.0) {

    companion object {
        /**
         * Parse a celcat color from the string into
         * a DestructedColor.
         *
         * @param context A valid context in case of null string
         * @param color A color null or not
         * @return The parsed color
         */
        fun fromCelcatColor(context: Context, color: String?) : DestructedColor {
            return if (color == null) {
                val default = ContextCompat.getColor(context, R.color.colorPrimary)
                DestructedColor(default)
            } else {
                DestructedColor(Color.parseColor(color))
            }
        }
    }

    constructor(color: Int) : this(
        ((color shr 16) and 0xFF).toDouble(),
        ((color shr 8) and 0xFF).toDouble(),
        (color and 0xFF).toDouble()
    )

    constructor(color: DestructedColor) : this(color.r, color.g, color.b)

    init {
        r = r.coerceIn(0.0, 255.0)
        g = g.coerceIn(0.0, 255.0)
        b = b.coerceIn(0.0, 255.0)
    }

    operator fun times(x: Double) = DestructedColor(r*x, g*x, b*x)

    /**
     * Returns the color luminosity computed
     * with the following formula :
     * (0.2125*r + 0.7152*g + 0.0722*b)
     */
    fun luminosity() = (0.2125*r + 0.7152*g + 0.0722*b)

    /**
     * Create a new color with the given luminosity.
     * It can be darker or lighter than the current one.
     *
     * @param desired The luminosity desired.
     * @return The new color.
     */
    fun changeLuminosity(desired: Double = 90.0): DestructedColor {
        val coeff = desired / luminosity()
        return DestructedColor(this) * coeff
    }

    fun toArgb() = Color.argb(255, r.toInt(), g.toInt(), b.toInt())
}