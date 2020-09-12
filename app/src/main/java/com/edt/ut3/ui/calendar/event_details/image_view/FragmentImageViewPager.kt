package com.edt.ut3.ui.calendar.event_details.image_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.edt.ut3.R
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_image_view_pager.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentImageViewPager: Fragment() {

    val viewModel : CalendarViewModel by activityViewModels()

    var position = 0

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        sharedElementEnterTransition =
            TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.shared_image)

        return inflater.inflate(R.layout.fragment_image_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            position = getInt("position").coerceAtLeast(0)
        }

        picture_pager.adapter = ImageViewerPager(viewModel)
        picture_pager.setCurrentItem(position, false)
        picture_pager.offscreenPageLimit = 1

        delete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_image)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // Do nothing on dismiss
                }
                .setPositiveButton(R.string.remove) { _, _ ->
                    // Here we remove the picture from the note
                    // at the position where the image viewer is.
                    // As the Picture.removeNoteAt() is safe in the
                    // sense where it does not crash if the index isn't
                    // in the bounds, we can call it with the provided index.
                    //
                    // We then save the note to update its representation
                    // in the database.
                    val position = picture_pager.currentItem
                    viewModel.selectedEventNote?.also { note ->

                        lifecycleScope.launch {
                            note.removePictureAt(position)
                            NotesViewModel(requireContext()).save(note)

                            withContext(Main) {
                                if (note.pictures.isEmpty()) {
                                    findNavController().popBackStack()
                                } else {
                                    picture_pager.adapter?.notifyItemRemoved(position)
                                }
                            }
                        }
                    }
                }
                .show()
        }
    }

}