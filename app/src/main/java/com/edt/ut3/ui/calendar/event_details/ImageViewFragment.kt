package com.edt.ut3.ui.calendar.event_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.edt.ut3.R
import com.edt.ut3.backend.note.Picture
import kotlinx.android.synthetic.main.layout_image_view.*

class ImageViewFragment(private val pictures: List<Picture>, private val current: Picture) : Fragment() {
    private lateinit var demoCollectionPagerAdapter: ImageCollectionPagerAdapter
    private lateinit var viewPager: ViewPager

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        demoCollectionPagerAdapter = ImageCollectionPagerAdapter(childFragmentManager, pictures)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = demoCollectionPagerAdapter
        viewPager.setCurrentItem(pictures.indexOf(current), false)
    }
}


class ImageCollectionPagerAdapter(fm: FragmentManager, private val pictures: List<Picture>) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int  = pictures.size

    override fun getItem(i: Int): Fragment {
        return ImageObjectFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_OBJECT, pictures[i])
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return ""
    }
}

private const val ARG_OBJECT = "object"

class ImageObjectFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_image_view, container, false)
    }

    private lateinit var picture: Picture

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        spinner.visibility = VISIBLE
        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            picture = getParcelable(ARG_OBJECT)!!
            lifecycleScope.launchWhenResumed {
                image.apply {
                    setImageBitmap(picture.loadThumbnail())

                    spinner.visibility = GONE
                }
            }
        }
    }
}