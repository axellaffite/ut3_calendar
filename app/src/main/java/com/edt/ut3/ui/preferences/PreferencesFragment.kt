package com.edt.ut3.ui.preferences

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.edt.ut3.R
import com.edt.ut3.backend.preferences.PreferencesManager

class PreferencesFragment: PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        findPreference<ListPreference>("theme")?.run {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference, any: Any ->
                try {
                    val pref = PreferencesManager.getInstance(context)
                    pref.theme = ThemePreference.valueOf(any as String)

                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        findPreference<Preference>("section")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(R.id.action_preferencesFragment_to_fragmentFormationChoice)
                true
            }
        }

        findPreference<Preference>("about_us")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(R.id.action_preferencesFragment_to_aboutUsFragment)
                true
            }
        }
    }

}