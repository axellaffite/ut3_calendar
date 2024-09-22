package com.edt.ut3.ui.preferences.formation.steps.authentication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.backend.requests.authentication_services.Credentials
import com.edt.ut3.backend.requests.authentication_services.getAuthenticator
import com.edt.ut3.databinding.FragmentAuthenticationBinding
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.misc.extensions.isTrue
import com.edt.ut3.misc.extensions.updateIfNecessary
import com.edt.ut3.ui.custom_views.TextInputEditText2
import com.edt.ut3.ui.preferences.formation.FormationSelectionViewModel
import com.edt.ut3.ui.preferences.formation.steps.state_fragment.StateFragment

class FragmentAuthentication: Fragment() {

    private lateinit var binding: FragmentAuthenticationBinding
    private val viewModel: FormationSelectionViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(viewModel.needsAuthentication() == false){
            (parentFragment as? StateFragment)?.requestNext()
            return
        }
        setupListeners(view.context)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    /**
     * Setup the fragment listeners.
     *
     * @param context A valid [Context]
     */
    private fun setupListeners(context: Context) {
        viewModel.run{
            getCredentials(context).observe(viewLifecycleOwner, ::handleCredentialsUpdate)
            authenticationState.observe(viewLifecycleOwner, ::handleStateChange)
            authenticationFailure.observe(viewLifecycleOwner, ::handleFailure)
        }
        setupField(binding.username)
        setupField(binding.password)
        setupDropdown(binding.role)
    }
    private fun setupDropdown(dropdown: Spinner?) = dropdown?.apply {
        val current_role_string = viewModel.getCredentials(context).value?.disambiguationIdentity
        Log.d("FramgentAUthentication", current_role_string ?: "none")
        onItemSelectedListener = object:
        AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                updateViewModelCredentials()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                updateViewModelCredentials()
            }
        }
    }
    /**
     * Setup a credential field in order to
     * handle the changes from it and update
     * the UI depending on them.
     *
     * @param field The text field
     */
    private fun setupField(field: TextInputEditText2?) = field?.apply {
        doOnTextChanged { _, _, _, _ -> updateViewModelCredentials() }

        setOnKeyListener { v, keyCode, event ->
            event.takeIf { it.action == KeyEvent.ACTION_UP }?.let {
                showKeyboardOnEvent(v, keyCode)
            }

            false
        }

        setOnFocusChangeListener { _, _ ->
            showActionButtonsDependingOnFocus()
        }

        onPreImeListener = { code: Int, event: KeyEvent? ->
            val shouldHandle = (code == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP)
            if (shouldHandle) {
                hideKeyboard()
                clearFocus()
                showActionButtonsIfSubFragment()
            }

            shouldHandle
        }
    }

    /**
     * Shows the parent's action buttons
     * depending on the current event.
     *
     * @param v The view
     * @param keyCode The event key code
     * @return Always return false
     */
    private fun showKeyboardOnEvent(v: View? = null, keyCode: Int): Boolean {
        val shouldBeHandled = (keyCode == KeyEvent.KEYCODE_ENTER && v == binding.password)

        if (shouldBeHandled) {
            showActionButtonsIfSubFragment()
        }

        return false
    }

    /**
     * Show the actions button depending
     * on the credentials fields focuses.
     */
    private fun showActionButtonsDependingOnFocus() {
        when {
            binding.username.hasFocus().isTrue() -> hideActionButtonsIfSubFragment()
            binding.password.hasFocus().isTrue() -> hideActionButtonsIfSubFragment()
            else -> {}
        }
    }

    /**
     * Hides the action buttons if the
     * current fragment is a child of
     * a StateFragment.
     */
    private fun hideActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is StateFragment) {
            parent.setActionButtonsVisibility(GONE)
        }
    }

    /**
     * Show the action buttons if the
     * current fragment is a child of
     * a StateFragment.
     */
    private fun showActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is StateFragment) {
            parent.setActionButtonsVisibility(VISIBLE)
        }
    }

    /**
     * Can be set to false, preventing a field update to trigger a credentials update in the backend.
     * Useful during setup, where the fields are set to the starting value.
     */
    private var fieldsReactivityEnabled = true

    private fun withReactivityDisabled(f: () -> Unit){
        fieldsReactivityEnabled =  false
        f()
        fieldsReactivityEnabled = true
    }

    /**
     * Sets the credentials into the [viewModel].
     * If one of the field is null or blank,
     * we just pass null to the [viewModel].
     */
    private fun updateViewModelCredentials() {
        val username = binding.username.text.takeIf { !it.isNullOrBlank() }
        val password = binding.password.text.takeIf { !it.isNullOrBlank() }
        val roleId = binding.role.selectedItemId.toInt()
        val roleString = resources.getStringArray(R.array.roles_values)[roleId]
        if(fieldsReactivityEnabled){
            viewModel.updateCredentials(
                Credentials.from(
                    username?.toString(),
                    password?.toString(),
                    roleString
                )
            )
        }
    }

    /**
     * Update the view depending on the
     * incoming [credentials].
     *
     * If they are the same as the view's ones,
     * they are not updated to avoid an infinite
     * loop.
     *
     * @param credentials The incoming credentials
     */
    private fun handleCredentialsUpdate(credentials: Credentials?) {
        val newUsername = credentials?.username ?: return
        val newPassword = credentials.password
        val newRole = credentials.disambiguationIdentity
        val roleIndex = resources.getStringArray(R.array.roles_values).indexOf(newRole)
        withReactivityDisabled {
            binding.username.updateIfNecessary(newUsername)
            binding.password.updateIfNecessary(newPassword)
            binding.role.setSelection(roleIndex)
        }

    }

    /**
     * Handles an [AuthenticationFailure].
     * Actually it only display a [Toast].
     *
     * @param failure The incoming failure.
     */
    private fun handleFailure(failure: AuthenticationFailure?) = failure?.let {
        context?.let {
            Toast.makeText(it, failure.reason(it), Toast.LENGTH_SHORT).show()
        }

        viewModel.clearFailure(failure)
    }

    /**
     * Update the view depending on
     * incoming [state].
     *
     * @param state The new state.
     */
    private fun handleStateChange(state: AuthenticationState?) {
        when (state) {
            AuthenticationState.Unauthenticated -> {
                binding.username.isEnabled = true
                binding.password.isEnabled = true
                binding.role.isEnabled = true

                context?.let {
                    val parent = parentFragment
                    val credentials = viewModel.getCredentials(it).value
                    if (parent is StateFragment) {
                        when (credentials) {
                            null -> parent.setNextText(R.string.step_skip)
                            else -> parent.setNextText(R.string.step_check_credentials)
                        }
                    }
                }
            }

            AuthenticationState.Authenticating -> {
                val parent = parentFragment
                if (parent is StateFragment) {
                    parent.setNextText(R.string.step_checking_credentials)
                }

                binding.username.isEnabled = false
                binding.password.isEnabled = false
                binding.role.isEnabled = false
            }

            AuthenticationState.Authenticated -> {
                val parent = parentFragment
                if (parent is StateFragment) {
                    parent.resetNextText()
                }

                binding.username.isEnabled = true
                binding.password.isEnabled = true
                binding.role.isEnabled = true

            }

            else -> {}
        }
    }
}
