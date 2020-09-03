package com.edt.ut3.ui.calendar.event_details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.edt.ut3.R
import com.edt.ut3.backend.note.Picture
import kotlinx.android.synthetic.main.layout_image_view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import kotlin.math.abs

class ImageViewFragment(private val pictures: List<Picture>, private val current: Picture) : Fragment() {
    private lateinit var demoCollectionPagerAdapter: ImageCollectionPagerAdapter
    private lateinit var viewPager: ViewPager

    companion object {
        var zoom = 1f
        fun isZoomed() = zoom != 1f
    }

    private val viewModel: ImageViewModel by viewModels { defaultViewModelProviderFactory }

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

    private val viewModel:ImageViewModel by viewModels { defaultViewModelProviderFactory }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_image_view, container, false)
    }

    private lateinit var picture: Picture
    private val gestureListener = object: GestureDetector.SimpleOnGestureListener() {
//        var origX = 0f
//        var origY = 0f
        var transX = 0f
        var transY = 0f
        var realWidth = 0f
        var realHeight = 0f

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            ImageViewFragment.zoom = if (ImageViewFragment.isZoomed()) 1f else 8f
            if (ImageViewFragment.isZoomed()) {
                realWidth = image.width.toFloat()
                realHeight = image.height.toFloat() * (image.width.toFloat() / image.drawable.intrinsicWidth.toFloat())
                println()
                println(image.drawable)
            } else {
                transX = 0f
                transY = 0f
            }

            image.translationX = transX
            image.translationY = transY
            image.scaleX = ImageViewFragment.zoom
            image.scaleY = ImageViewFragment.zoom

            image.invalidate()
            image.requestLayout()

            return true
        }

        override fun onScroll(e1: MotionEvent?,
                              e2: MotionEvent?,
                              distanceX: Float,
                              distanceY: Float): Boolean
        {
            if (!ImageViewFragment.isZoomed()) {
                return false
            }


            println("$realWidth $realHeight")
            val xOffsetMax = (realWidth * 4f - view!!.width / 2f).coerceAtLeast(0f)
            val yOffsetMax = (realHeight * 4f - view!!.height / 2f).coerceAtLeast(0f)
            println("${image.height} ${view!!.height}")
            println(yOffsetMax)
            if (xOffsetMax > 0) {
                transX = (transX + distanceX).coerceIn(-xOffsetMax, xOffsetMax)
                image.translationX = (image.x - distanceX).coerceIn(-xOffsetMax, xOffsetMax)
            }

            if (yOffsetMax > 0) {
                transY = (transY + distanceY).coerceIn(-yOffsetMax, yOffsetMax)
                image.translationY = (image.y - distanceY).coerceIn(-yOffsetMax, yOffsetMax)
            }

            println("${image.x}, ${image.y}")

            image.invalidate()
            image.requestLayout()

            return true
        }

        override fun onDown(e: MotionEvent?) = true
    }

    private lateinit var gestureDetector : GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        spinner.visibility = VISIBLE
        viewModel.currentImage = image

        gestureDetector = GestureDetector(requireContext(), gestureListener)
        view.setOnTouchListener { view, motionEvent ->
            println("touched")
            return@setOnTouchListener gestureDetector.onTouchEvent(motionEvent)
        }

        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            picture = getParcelable(ARG_OBJECT)!!
            lifecycleScope.launchWhenResumed {
                image.apply {
                    val picture = withContext(IO) { picture.loadPictureForBounds(1080, 1920) }

                    withContext(Main) {
                        setImageBitmap(picture)
                        spinner.visibility = GONE
                    }
                }
            }
        }
    }
}