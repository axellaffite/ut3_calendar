package com.edt.ut3.ui.preferences

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.preferences.PreferencesManager
import org.json.JSONArray

class PreferencesFragment: PreferenceFragmentCompat() {

    companion object {

        private fun extractFids(uri: Uri): List<String> {
            fun extract(index: Int): List<String> {
                val fid = uri.getQueryParameter("fid$index")
                if (fid.isNullOrBlank()) {
                    return listOf()
                }

                return extract(index + 1) + fid
            }

            return extract(0)
        }

    }

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
                    pref.theme = any as String

                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        findPreference<EditTextPreference>("section")?.let { editText ->
            editText.setOnBindEditTextListener(EditTextListener(requireContext()))
            editText.setOnPreferenceChangeListener { _, link -> setSections(link as String) }
        }

        findPreference<Preference>("about_us")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(R.id.action_preferencesFragment_to_aboutUsFragment)
                true
            }
        }
    }

    private fun setSections(link: String) : Boolean {
        val baseLinkFinder = Regex("(.*)/cal.*")
        val baseLink = baseLinkFinder.find(link)?.groups?.get(1)?.value!!

        Log.d(this::class.simpleName, "setSections: $baseLink")

        val fids = extractFids(link.toUri())

        PreferencesManager.getInstance(requireContext()).apply {
            this.groups = JSONArray(fids).toString()
            this.link = baseLink
        }

        Updater.forceUpdate(requireContext(), true)

        return true
    }

    private class EditTextListener(private var context: Context): EditTextPreference.OnBindEditTextListener {
        override fun onBindEditText(editText: EditText) {
            editText.doOnTextChanged { text, _, _, _ ->
                val valid: Boolean
                valid = try {
                    val baseLinkFinder = Regex("(.*)/cal.*")
                    val baseLink = baseLinkFinder.find(text.toString())?.value
                    val fids = extractFids(text.toString().toUri())

                    (!baseLink.isNullOrBlank()) && fids.isNotEmpty()
                } catch (e: UnsupportedOperationException) {
                    false
                }

                if (!valid) {
                    editText.error = context.resources.getString(R.string.not_valid_link)
                }
            }
        }
    }

}