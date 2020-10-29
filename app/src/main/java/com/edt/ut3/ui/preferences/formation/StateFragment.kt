package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.edt.ut3.R
import kotlinx.android.synthetic.main.state_fragment.*
import kotlinx.coroutines.Job

abstract class StateFragment: Fragment() {

    private val viewModel: StateViewModel by viewModels()

    private var onRequestBackJob: Job? = null
    private var onRequestNextJob: Job? = null

    private lateinit var builders: List<StateFragmentBuilder>

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.state_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        builders = initFragments()

        setupView()
        setupListeners()
    }

    private fun setupView() {
        pager?.adapter = StateAdapter(this)
        pager?.isUserInputEnabled = false
    }

    private fun setupListeners() {
        viewModel.run {
            position.observe(viewLifecycleOwner) { position: StateViewModel.Position ->
                pager?.currentItem = position.current ?: 0
                setTitle(builders[position.current ?: 0].title)
                setSummary(builders[position.current ?: 0].summary)
            }

            title.observe(viewLifecycleOwner) {
                this@StateFragment.title?.setText(it)
            }

            summary.observe(viewLifecycleOwner) {
                this@StateFragment.summary?.setText(it)
            }
        }

        back.setOnClickListener {
            synchronized(this) {
                onRequestBackJob?.cancel()
                onRequestBackJob = lifecycleScope.launchWhenResumed {
                    val current = viewModel.currentPosition()
                    val builder = builders[current]
                    if (builder.onRequestBack()) {
                        builder.onBack()
                    }
                }
            }
        }

        next.setOnClickListener {
            synchronized(this) {
                onRequestNextJob?.cancel()
                onRequestNextJob = lifecycleScope.launchWhenResumed {
                    val current = viewModel.currentPosition()
                    val builder = builders[current]
                    if (builder.onRequestNext(current)) {
                        builder.onNext()
                    }
                }
            }
        }
    }

    abstract fun initFragments(): List<StateFragmentBuilder>

    fun currentPosition() = viewModel.currentPosition()

    fun back() = viewModel.back()

    fun nextTo(nextPosition: Int?) = viewModel.nextTo(nextPosition)

    abstract fun onFinish()

    abstract fun onCancel()

    fun setTitle(id: Int) = viewModel.setTitle(id)

    fun setSummary(id: Int) = viewModel.setDescription(id)

    fun setBackText(id: Int) = back?.setText(id)
    fun resetBackText() {
        val text = when (pager?.currentItem)  {
            0 -> R.string.step_cancel
            else -> R.string.step_back
        }

        back?.setText(text)
    }

    fun setNextText(id: Int) = next?.setText(id)
    fun resetNextText() {
        val text = when (pager?.currentItem) {
            builders.lastIndex -> {
                R.string.step_finish
            }

            else -> {
                R.string.step_next
            }
        }

        next.setText(text)
    }


    class StateFragmentBuilder(
        val title: Int,
        val summary: Int,
        val builder: () -> Fragment,
        val onRequestNext: suspend (position: Int) -> Boolean,
        val onRequestBack: suspend () -> Boolean,
        val onNext: suspend () -> Unit,
        val onBack: suspend () -> Unit
    )

    class StateAdapter(
        val fragment: StateFragment
    ): FragmentStateAdapter(fragment) {

        override fun getItemCount() = fragment.builders.size

        override fun createFragment(position: Int) = fragment.builders[position].builder()

    }

}