package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.ui.custom_views.TextInputEditText2
import kotlinx.android.synthetic.main.fragment_authentication.*
import kotlinx.android.synthetic.main.fragment_formation.*

class FormationAuthentication: StepperElement() {

    val viewModel: FormationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_authentication, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupField(username, viewModel.credentials.value?.username)
        setupField(password, viewModel.credentials.value?.password)
    }

    private fun setupField(field: TextInputEditText2?, value: String?) = field?.apply {
        setText(value)

        doOnTextChanged { _, _, _, _ -> updateViewModelCredentials() }

        setOnKeyListener { v, keyCode, event ->
            event.takeIf { it.action == KeyEvent.ACTION_UP }?.let {
                showKeyboardOnEvent(v, keyCode)
            }

            false
        }

        setOnFocusChangeListener { _, hasFocus ->
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

    private fun showKeyboardOnEvent(v: View? = null, keyCode: Int): Boolean {
        val shouldBeHandled = (keyCode  == KeyEvent.KEYCODE_ENTER && v == password)

        if (shouldBeHandled) {
            showActionButtonsIfSubFragment()
        }

        return false
    }

    private fun showActionButtonsDependingOnFocus() {
        when {
            username?.hasFocus() == true -> hideActionButtonsIfSubFragment()
            password?.hasFocus() == true -> hideActionButtonsIfSubFragment()
            else -> {}
        }
    }

    private fun hideActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is FormationFragment) {
            parent.action_buttons?.visibility = GONE
        }
    }

    private fun showActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is FormationFragment) {
            parent.action_buttons?.visibility = VISIBLE
        }
    }

    private fun updateViewModelCredentials() {
        val username = username?.text.takeIf { !it.isNullOrBlank() }
        val password = password?.text.takeIf { !it.isNullOrBlank() }

        viewModel.credentials.value = Authenticator.Credentials.from(
            username?.toString(),
            password?.toString()
        )
    }

}