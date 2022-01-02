package com.edt.ut3.refactored.models.repositories.preferences.simple_preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlin.reflect.KProperty

/**
 * Used to substitute the classic preferences.
 *
 * @param context Used to get the default shared preferences.
 * @property preferences The preferences from which the values will be get from.
 */
class SimplePreference(
    context: Context,
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {

    /**
     * Used to delegate a preference value.
     * When the value is assigned, it is directly put into the
     * [SharedPreferences].
     * When the value is accessed, it is directly get from the
     * [SharedPreferences].
     *
     * @param Origin The type of the preference (non nullable)
     * @property key The key where to store the preference
     * @property defValue The default value to retrieve if the preference is unset. If the
     * specified type is nullable, the [default value][defValue] should be equal to null
     * as some parsers are able to detect that the deserialized object is in fact null. But this
     * is only an advice as the final decision remains on the development context.
     * @property getter The way to get the preference
     */
    inner class Delegate <Origin, Converted> (
        private val key: String,
        private val defValue: Converted,
        private val converter: Converter<Origin, Converted>,
        private val manager: GetSetManager<Converted>,
    ) {
        /**
         * How to get the value from the preferences.
         * It uses the [getter] provided in the constructor.
         * By default it only get the value as a string.
         *
         * @param thisRef The reference to the current variable
         * @param property The properties of the current variable
         *
         * @return The property get from the preferences with the getter
         * provided in the constructor. By default if the value is not present,
         * the [defValue] is returned.
         */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Origin = synchronized(this) {
            return converter.deserialize(manager.get(key, defValue, preferences))
        }


        /**
         * Sets the value into the [SharedPreferences].
         * The value is set as a [String].
         *
         * @param thisRef The reference to the current variable
         * @param property The properties of the current variable
         * @param value The value to set into the [SharedPreferences]
         */
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Origin) = synchronized(this) {
            preferences.edit(commit = true) { manager.set(key, converter.serialize(value), this) }
        }
    }

    /**
     * Used to specify how to convert a certain type.
     *
     * @param T The type the the object to convert.
     */
    abstract class Converter <Origin, Converted> {
        /**
         * This function describes how the object should
         * be deserialized from a [String].
         *
         * @param value The string from which the value
         * will be converted. Note that the string may be
         * equal to "null" if the incoming value is null.
         *
         * @return T The converted value as a [T] object
         */
        abstract fun deserialize(value: Converted): Origin

        /**
         * This function describes how the object should
         * be serialized to a [String].
         *
         * @param value The value to serialize.
         * If the type is nullable, the returned value
         * should be equal to "null" as some parsers are
         * able to guess that "null" should be converted
         * as a null object.
         *
         * @return The serialized object as a [String]
         */
        abstract fun serialize(value: Origin): Converted
    }

    interface GetSetManager<Converted> {
        fun get(key: String, defValue: Converted, edit: SharedPreferences) : Converted
        fun set(key: String, value: Converted, edit: SharedPreferences.Editor)
    }

}