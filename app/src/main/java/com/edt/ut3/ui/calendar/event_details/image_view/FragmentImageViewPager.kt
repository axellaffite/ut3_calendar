package com.edt.ut3.ui.calendar.event_details.image_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.edt.ut3.R
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.note.Note
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_image_view_pager.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentImageViewPager: DialogFragment() {

    var position = 0
    lateinit var noteLD: LiveData<Note>

    val viewModel : ImageViewPagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.AppTheme)
        if (this::noteLD.isInitialized) {
            viewModel.noteLD = noteLD
        } else {
            noteLD = viewModel.noteLD
        }
    }

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

        picture_pager.adapter = ImageViewerPager(noteLD)
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
                    noteLD.value?.also { note ->

                        lifecycleScope.launch {
                            note.removePictureAt(position)
                            NotesViewModel(requireContext()).save(note)

                            withContext(Main) {
                                if (note.pictures.isEmpty()) {
                                    dismiss()
                                } else {
                                    picture_pager.adapter?.notifyItemRemoved(position)
                                }
                            }
                        }
                    }
                }
                .show()
        }

        if (view is MotionLayout) {
            picture_pager.setOnClickListener(object: View.OnClickListener {
                var begin = false

                override fun onClick(v: View?) {
                    if (begin) {
                        view.transitionToEnd()
                    } else {
                        view.transitionToStart()
                    }

                    begin = !begin
                }
            })
        }
    }

}