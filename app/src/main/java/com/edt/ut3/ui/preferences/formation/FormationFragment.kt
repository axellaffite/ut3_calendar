package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.Updater
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.authentication_services.CelcatAuthenticator
import com.edt.ut3.misc.extensions.*
import kotlinx.android.synthetic.main.fragment_formation.*
import kotlinx.coroutines.Job
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class FormationFragment: Fragment() {

    val viewModel: FormationViewModel by activityViewModels()
    private var authJob: Job? = null

    private val positionHistory = Stack<Int>().apply { push(0) }

    private val layoutChangeListener = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position > positionHistory.peek()) {
                positionHistory.push(position)
            }

            onNext(position)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("title", title?.text.toString())
        outState.putString("summary", summary?.text.toString())
        val adapter = pager?.adapter
        if (adapter is FormationAdapter) {
            outState.putSerializable("fragmentClasses", adapter.mFragments.toHashMap())
        }
        outState.putIntegerArrayList("history", ArrayList<Int>().apply {
            addAll(positionHistory.toTypedArray())
        })

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        pager.registerOnPageChangeCallback(layoutChangeListener)
        super.onResume()
    }

    override fun onPause() {
        pager.unregisterOnPageChangeCallback(layoutChangeListener)
        super.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_formation, container, false)
        .also {
            viewModel.credentials.run {
                if (value.isNull()) {
                    value = CredentialsManager.getInstance(inflater.context).getCredentials()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.run {
            title.text = getString("title")
            summary.text = getString("summary")

            positionHistory.clear()
            with (getSerializable("history") as? List<Int>) {
                this?.run {
                    this.sorted().forEach {
                        positionHistory.push(it)
                    }
                }
            }
        }

        pager.registerOnPageChangeCallback(layoutChangeListener)

        // We disable the user input to
        // ensure that he's unable to swipe
        // between fragments.
        pager.isUserInputEnabled = false
        pager.offscreenPageLimit = 4
        pager.adapter = FormationAdapter(this).apply {
            savedInstanceState?.run {
                val saved = getSerializable("fragmentClasses") as? HashMap<*, *>
                saved?.entries
                    ?.filterIsInstance<Map.Entry<Int, Class<StepperElement>>>() // Ensure type safety
                    ?.map { Pair(it.key, it.value) } // Transform them into map entries
                    ?.toMap(mFragments) // Put the result into the map
            }
        }

        next.setOnClickListener {
            onRequestNext(pager.currentItem)
        }

        back.setOnClickListener {
            onRequestBack()
        }

        addOnBackPressedListener {
            positionHistory.pop()
            if (positionHistory.isEmpty()) {
                isEnabled = false
                viewModel.reset()
                activity?.onBackPressed()
            } else {
                pager.currentItem = positionHistory.peek()
            }
        }

        setTitle(pager.currentItem)

    }

    private fun onRequestNext(step: Int) = when (step) {
        0 -> nextAuthentication()
        1 -> nextGroup()
        else -> throw IllegalStateException()
    }

    private fun onNext(position: Int) {
        when (position) {
            0 -> {
                next?.setText(R.string.step_next)
                back?.setText(R.string.step_cancel)
            }

            else -> {
                next?.setText(R.string.step_finish)
                back?.setText(R.string.step_back)
            }
        }

        setTitle(position)
        setSummary(position)
        hideKeyboard()
    }

    private fun onRequestBack() = onBackPressed()


    private fun nextAuthentication() {
        fun goToNext() {
            pager?.currentItem = 1
        }

        val credentialsManager = CredentialsManager.getInstance(requireContext())
        val credentials = viewModel.credentials.value
        if (credentials.isNotNull()) {
            authJob = lifecycleScope.launchWhenResumed {
                try {
                    context?.let {
                        val toast = Toast.makeText(context, R.string.check_credentials, Toast.LENGTH_SHORT)
                        toast.show()
                        CelcatAuthenticator().checkCredentials(it, credentials)
                        credentialsManager.saveCredentials(credentials)
                        goToNext().also { toast.cancel() }
                    }
                } catch (e: Exception) {
                    val err = when (e) {
                        is Authenticator.InvalidCredentialsException -> R.string.error_wrong_credentials
                        is IOException -> R.string.error_check_internet
                        else -> R.string.unknown_error
                    }

                    context?.let {
                        Toast.makeText(it, err, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            credentialsManager.clearCredentials()
            goToNext()
        }
    }

    private fun nextGroup() {
        if (viewModel.groups.value?.isNotEmpty() == true) {
            onCompleted()
        }
    }

    private fun onCompleted() {
        val ctx = context ?: return
        val preferences = PreferencesManager.getInstance(ctx)

        viewModel.link.value?.ifInit { nullableInfo ->
            nullableInfo?.let { info ->
                preferences.link = info.toJSON().toString()
                preferences.groups = viewModel.groups.value?.toJSONArray { it.id }.toString()
            }
        }

        Updater.forceUpdate(ctx, firstUpdate = true)
        findNavController().popBackStack()
    }

    private fun setTitle(position: Int) {
        val adapter = pager.adapter
        if (adapter is FormationAdapter) {
            adapter.getFragmentClass(position)?.let {
                title?.setText(StepperElement.titleOf(it))
            }
        }
    }

    private fun setSummary(position: Int) {
        val adapter = pager.adapter
        if (adapter is FormationAdapter) {
            adapter.getFragmentClass(position)?.let {
                summary?.setText(StepperElement.summaryOf(it))
            }
        }
    }

    class FormationAdapter(val fragment: FormationFragment): FragmentStateAdapter(fragment) {

        val mFragments = mutableMapOf<Int, Class<out StepperElement>>()

        fun getFragmentClass(position: Int) = mFragments[position]

        override fun getItemCount() = 2

        override fun createFragment(position: Int): StepperElement {
            return when (position) {
                0 -> FormationAuthentication()
                1 -> WhichGroups()
                else -> throw IllegalStateException("Fragment creation isn't set for position $position")
            }.also {
                mFragments[position] = it.javaClass
            }
        }

    }

}