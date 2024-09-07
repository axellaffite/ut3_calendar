package com.edt.ut3.ui.preferences.formation.steps.state_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.edt.ut3.R
import com.edt.ut3.databinding.StateFragmentBinding
import com.edt.ut3.misc.extensions.addOnBackPressedListener
import kotlinx.coroutines.Job

abstract class StateFragment: Fragment() {

    private val viewModel: StateViewModel by viewModels()
    private lateinit var binding: StateFragmentBinding

    private var onRequestBackJob: Job? = null
    private var onRequestNextJob: Job? = null

    private lateinit var builders: List<StateFragmentBuilder>

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?{
        binding = StateFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        builders = initFragments()

        setupView()
        setupListeners()
    }

    private fun setupView() {
        binding.pager?.adapter = StateAdapter(this)
        binding.pager?.isUserInputEnabled = false
    }

    private fun setupListeners() {
        viewModel.run {
            position.observe(viewLifecycleOwner) { position: StateViewModel.Position ->
                binding.pager?.currentItem = position.current ?: 0
                resetBackText()
                resetNextText()
                setTitle(builders[position.current ?: 0].title)
                setSummary(builders[position.current ?: 0].summary)
                setActionButtonsVisibility(if (builders[position.current ?: 0].actionButtonsVisible) View.VISIBLE else View.GONE)
            }

            title.observe(viewLifecycleOwner) {
                binding.title?.setText(it)
            }

            summary.observe(viewLifecycleOwner) {
                binding.summary?.setText(it)
            }
        }

        addOnBackPressedListener { requestBack() }
        binding.back.setOnClickListener { requestBack() }
        binding.next.setOnClickListener { requestNext() }
    }

    fun requestBack() = synchronized(this) {
        onRequestBackJob?.cancel()
        onRequestBackJob = lifecycleScope.launchWhenResumed {
            val current = viewModel.currentPosition()
            val builder = builders[current]
            if (builder.onRequestBack()) {
                builder.onBack()
            }
        }
    }

    fun requestNext() = synchronized(this) {
        onRequestNextJob?.cancel()
        onRequestNextJob = lifecycleScope.launchWhenResumed {
            val current = viewModel.currentPosition()
            val builder = builders[current]
            if (builder.onRequestNext(current)) {
                builder.onNext()
            }
        }
    }

    public fun setActionButtonsVisibility(visibility: Int){
        binding.actionButtons?.visibility = visibility;
    }
    abstract fun initFragments(): List<StateFragmentBuilder>

    fun currentPosition() = viewModel.currentPosition()

    fun back() = viewModel.back()

    fun next() = viewModel.next()

    abstract fun onFinish()

    abstract fun onCancel()

    fun setTitle(id: Int) = viewModel.setTitle(id)

    fun setSummary(id: Int) = viewModel.setDescription(id)

    fun setBackText(id: Int) = binding.back?.setText(id)
    fun resetBackText() {
        val text = when (binding.pager?.currentItem)  {
            0 -> R.string.step_cancel
            else -> R.string.step_back
        }

        binding.back?.setText(text)
    }

    fun setNextText(id: Int) = binding.next?.setText(id)
    fun resetNextText() {
        val text = when (binding.pager?.currentItem) {
            builders.lastIndex -> {
                R.string.step_finish
            }

            else -> {
                R.string.step_next
            }
        }

        binding.next.setText(text)
    }


    class StateFragmentBuilder(
        val title: Int,
        val summary: Int,
        val builder: () -> Fragment,
        val onRequestNext: suspend (position: Int) -> Boolean,
        val onRequestBack: suspend () -> Boolean,
        val onNext: suspend () -> Unit,
        val onBack: suspend () -> Unit,
        val actionButtonsVisible: Boolean = true
    )

    class StateAdapter(
        val fragment: StateFragment
    ): FragmentStateAdapter(fragment) {

        override fun getItemCount() = fragment.builders.size

        override fun createFragment(position: Int) = fragment.builders[position].builder()

    }

}