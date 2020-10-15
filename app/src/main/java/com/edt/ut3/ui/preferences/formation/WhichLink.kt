package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.text.TextUtils
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.misc.Optional
import kotlinx.android.synthetic.main.fragment_which_link.*

/**
 * ! Actually unused
 */
class WhichLink: StepperElement() {

    val viewModel: FormationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_link, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.school.observe(viewLifecycleOwner) { school ->
            school?.ifInit {
                it?.let {
                    link_group?.removeAllViews()
                    (it.info + null).forEach { info ->
                        link_group?.addView(RadioButton(requireContext()).apply {
                            ellipsize = TextUtils.TruncateAt.END
                            autoLinkMask = Linkify.WEB_URLS

                            info?.run {
                                text = getString(R.string.info_format).format(name, url)
                            } ?: run {
                                text = getString(R.string.not_listed)
                            }

                            setOnClickListener {
                                viewModel.link.value = Optional.of(info)
                            }
                        })
                    }
                }
            }
        }
    }

}