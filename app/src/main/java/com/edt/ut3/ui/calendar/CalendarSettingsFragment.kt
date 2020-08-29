package com.edt.ut3.ui.calendar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.edt.ut3.R
import org.json.JSONArray

class CalendarSettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.calendar_preferences, rootKey)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        findPreference<ListPreference>("theme")?.let { theme ->
            theme.setOnPreferenceChangeListener { _, newValue -> themeSelector(theme, newValue) }
        }

        findPreference<EditTextPreference>("section")?.let { editText ->
            editText.setOnBindEditTextListener(EditTextListener(requireContext()))
            editText.setOnPreferenceChangeListener { _, link -> setSections(link as String) }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun themeSelector(preference: ListPreference, value: Any) : Boolean {
        val index = (value as String).toInt()

        requireActivity().run {
            when (index) {
                0 -> theme.applyStyle(R.style.AppTheme, true)
                1 -> theme.applyStyle(R.style.DarkTheme, true)
            }

            recreate()
        }

        return true
    }

    private fun setSections(link: String) : Boolean {
        val finder = Regex("&fid[\\d]+=[\\w\\d]+")
        val sections = finder.findAll(link).map { it.value.split('=').last() }

        preferenceManager.sharedPreferences.edit().putString("groups", JSONArray(sections.toList().toTypedArray()).toString()).apply()


        return true
    }

    private class EditTextListener(private var context: Context): EditTextPreference.OnBindEditTextListener {
        override fun onBindEditText(editText: EditText) {
            editText.doOnTextChanged { text, _, _, _ ->
                val valid = text?.matches(Regex(".*edt.univ-tlse3.fr/calendar2/.*(&fid[\\d]+=[\\w\\d]+)")) ?: false
                if (!valid) {
                    editText.error = context.resources.getString(R.string.not_valid_link)
                }
            }
        }

    }
}