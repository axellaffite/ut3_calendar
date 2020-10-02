package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.axellaffite.fastgallery.slider_animations.SlideAnimations
import com.edt.ut3.R
import com.edt.ut3.ui.preferences.formation.choices_fragments.*
import kotlinx.android.synthetic.main.fragment_formation_choice.*

class FragmentFormationChoice: Fragment() {

    private val viewModel: FormationChoiceViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_formation_choice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            pager?.let {
                if (it.currentItem == 0) {
                    isEnabled = false
                    activity?.onBackPressed()
                } else {
                    it.currentItem--
                }
            }
        }

        val adapter = FormationChoiceAdapter(this)
        pager.adapter = adapter
        pager.setPageTransformer(SlideAnimations.depthPageAnimation())


        actionButton?.setOnClickListener {
            pager?.run {
                val currentFragment = adapter.getFragment(currentItem)
                println(currentFragment::class.simpleName)
                if (currentFragment.isChoiceValid()) {
                    viewModel.currentFragment.value = currentItem + 1
                }
            }
        }

        pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.currentFragment.value = position
            }
        })

        viewModel.currentFragment.observe(viewLifecycleOwner) { currentFragment ->
            with(pager.adapter as FormationChoiceAdapter) {
                count = currentFragment + 1
                pager.post {
                    notifyDataSetChanged()
                    pager.currentItem = currentFragment
                }
            }
        }


    }

    class FormationChoiceAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
        var count = 1
        override fun getItemCount() = count

        private val fragments = arrayOf(
            WhichSchoolFragment(),
            WhichLinkFragment(),
            CustomLinkFragment(),
            WhichGroupFragment()
        )

        fun getFragment(position: Int) = fragments[position]

        override fun createFragment(position: Int): ChoiceFragment<*> = fragments[position]

    }


//    class FormationChoiceAdapter : SearchBarAdapter<String, FormationChoiceAdapter.FormationChoiceViewHolder>() {
//        private var dataSet: List<String>? = null
//
//        override fun setDataSet(dataSet: List<String>) {
//            this.dataSet = dataSet
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormationChoiceViewHolder {
//            val v = LayoutInflater.from(parent.context)
//                .inflate(android.R.layout.simple_list_item_1, parent, false)
//
//            return FormationChoiceViewHolder(v)
//        }
//
//        override fun onBindViewHolder(holder: FormationChoiceViewHolder, position: Int) {
//            val item = dataSet?.get(position)
//            item.also {
//                with (holder.v as TextView) {
//                    text = it
//                }
//            }
//        }
//
//        override fun getItemCount() = (dataSet?.size ?: 0) + 1
//
//        class FormationChoiceViewHolder(val v: View): RecyclerView.ViewHolder(v)
//    }

}