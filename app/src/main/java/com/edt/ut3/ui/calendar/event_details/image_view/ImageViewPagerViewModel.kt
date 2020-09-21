package com.edt.ut3.ui.calendar.event_details.image_view

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.note.Note

class ImageViewPagerViewModel: ViewModel() {

    lateinit var noteLD : LiveData<Note>

}