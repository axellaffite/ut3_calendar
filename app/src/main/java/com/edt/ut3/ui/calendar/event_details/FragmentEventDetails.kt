package com.edt.ut3.ui.calendar.event_details

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.edt.ut3.ui.custom_views.image_preview.ImagePreviewAdapter
import com.edt.ut3.ui.preferences.Theme
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FragmentEventDetails(private val event: Event) : Fragment() {

    enum class RequestCode { IMAGE_CAPTURE, CAMERA_PERMISSION }

    private val viewModel: CalendarViewModel by viewModels { defaultViewModelProviderFactory }
    private var note: Note = Note.generateEmptyNote(event.id)
    private var pictureFile : File? = null
    private var pictureName : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().nav_view.visibility = GONE
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().nav_view.visibility = VISIBLE
        viewModel.selectedEvent = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext()).noteDao().run {
                val result = selectByEventIDs(event.id)
                result.forEach {
                    println(it)
                }

                if (result.size == 1) {
                    println("Assigning note")
                    note = result[0]
                }

                setupContent()
            }
        }

        setupListeners()
    }

    private fun setupContent() {
        println(note)
        title.text = event.courseName ?: event.category
        when (PreferencesManager(requireContext()).getTheme()) {
            Theme.LIGHT -> {
                event.lightBackgroundColor(requireContext()).let {
                    title_container.setCardBackgroundColor(it)
                }
            }

            Theme.DARK -> {
                val textButtonColor = Color.WHITE
                title.setTextColor(textButtonColor)
                close_button.setColorFilter(textButtonColor)

                event.darkBackgroundColor(requireContext()).let {
                    title_container.setCardBackgroundColor(it)
                }
            }
        }

        description.text = event.description
        event_note.setText(note.contents)

        pictures.adapter = ImagePreviewAdapter(viewModel.selectedEventPictures).apply {
            onItemClickListener = { v: View, p: Picture ->
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .add(R.id.nav_host_fragment, ImageViewFragment(viewModel.selectedEventPictures, p))
                    .addToBackStack(null)
                    .commit()
            }

            onAddPictureClickListener = {
                requestPicture()
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.selectedEventPictures.addAll(note.pictures)

            pictures.notifyDataSetChanged()
        }
    }

    private fun requestPicture() {
        when (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                        generateOutputFile {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(requireContext(), "com.edt.ut3.fileprovider", pictureFile!!))
                            startActivityForResult(takePictureIntent, RequestCode.IMAGE_CAPTURE.ordinal)
                        }
                    }

                }
            }

            else -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    RequestCode.CAMERA_PERMISSION.ordinal
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode > RequestCode.values().size) {
            return
        }

        val request = RequestCode.values()[requestCode]
        println("ohoh: $request")
        when (request) {
            RequestCode.IMAGE_CAPTURE -> {
                println("$resultCode $RESULT_OK")

                if (resultCode != RESULT_OK) {
                    return
                }

                println(pictureFile?.absolutePath)

                val bitmap = BitmapFactory.decodeStream(pictureFile!!.inputStream())
                println("${bitmap.width} ${bitmap.height}")
                bitmap?.let {
                    addPictureToNote(it)
                }
            }

             else -> {}
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray)
    {
        if (requestCode > RequestCode.values().size) {
            return
        }

        val request = RequestCode.values()[requestCode]
        when (request) {
            RequestCode.CAMERA_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPicture()
                }
            }

            else -> {}
        }
    }

    private fun generateOutputFile(callback: (() -> Unit)) {
        lifecycleScope.launch {
            saveNote(note) {
                pictureName = Picture.generateFilename(it.id.toString())
                pictureFile = Picture.prepareImageFile(
                    requireContext(),
                    pictureName!!
                )

                callback()
            }
        }
    }


    private fun addPictureToNote(picture: Bitmap) {
        try {
            lifecycleScope.launch {
                saveNote(note) {
                    lifecycleScope.launch {
                        val generated = Picture.generateFromPictureUri(requireContext(), pictureName!!, pictureFile!!.absolutePath)
                        note.pictures = note.pictures + generated

                        saveNote(note) {
                            println(it)
                        }

                        viewModel.selectedEventPictures.add(generated)
                        pictures.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        close_button.setOnClickListener {
            requireActivity().onBackPressed()
        }

        event_note.doOnTextChanged { text, start, before, count ->
            note.contents = text.toString()
            lifecycleScope.launch {
                saveNote(note)
            }
        }
    }

    private suspend fun saveNote(newNote: Note, callback: ((Note) -> Unit)? = null) {
        withContext(IO) {
            note = Note.saveNote(newNote, requireContext())
        }

        callback?.invoke(note)
    }
}