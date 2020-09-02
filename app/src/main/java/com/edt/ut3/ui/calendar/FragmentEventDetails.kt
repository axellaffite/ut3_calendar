package com.edt.ut3.ui.calendar

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
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
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.custom_views.image_preview.ImagePreviewAdapter
import com.edt.ut3.ui.preferences.Theme
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class FragmentEventDetails(private val event: Event) : Fragment() {

    enum class RequestCode { IMAGE_CAPTURE, CAMERA_PERMISSION }

    var saveJob : Job? = null

    private val viewModel: CalendarViewModel by viewModels { defaultViewModelProviderFactory }
    private var note: Note = Note.generateEmptyNote(event.id)
    private var pictureFile : File? = null

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

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
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
                val textButtonColor = event.textColor?.toColorInt() ?: Color.BLACK
                title.setTextColor(textButtonColor)
                close_button.setColorFilter(textButtonColor)

                event.lightBackgroundColor(requireContext()).let {
                    title_container.setBackgroundColor(it)
                    requireActivity().window.statusBarColor = it
                }
            }

            Theme.DARK -> {
                val textButtonColor = Color.WHITE
                title.setTextColor(textButtonColor)
                close_button.setColorFilter(textButtonColor)

                event.darkBackgroundColor(requireContext()).let {
                    title_container.setBackgroundColor(it)
                    requireActivity().window.statusBarColor = it
                }
            }
        }

        description.text = event.description
        event_note.setText(note.contents)

        pictures.adapter = ImagePreviewAdapter(viewModel.selectedEventPictures)
        lifecycleScope.launchWhenResumed {
            viewModel.selectedEventPictures.addAll(withContext(IO) {
                 note.pictures.map {
//                     val f = requireContext().openFileInput(it)
                     val f = File(it)
                     BitmapFactory.decodeFileDescriptor(f.inputStream().fd).also {

                     }
                }.toMutableList()
            })

            pictures.notifyDataSetChanged()
        }


        add_picture.setOnClickListener {
            requestPicture()
        }
    }

    private fun requestPicture() {
        when (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                        generateOutputFile()
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(requireContext(), "com.edt.ut3.fileprovider", pictureFile!!))
                        startActivityForResult(takePictureIntent, RequestCode.IMAGE_CAPTURE.ordinal)
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

    private fun generateOutputFile() {
        saveNote(note) {
            val path = "${it.id}${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}"
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            pictureFile = File.createTempFile(
                path,
                ".jpg",
                storageDir
            )
        }
    }


    private fun addPictureToNote(picture: Bitmap) {
        try {
            saveNote(note) {
//                val path = "${it.id}${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.jpg"
//                requireContext().openFileOutput(path, Context.MODE_PRIVATE).also { outputStream ->
//                    picture.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//                }.close()

                note.pictures = note.pictures + pictureFile!!.absolutePath
                saveNote(note) {
                    println(it)
                }

                viewModel.selectedEventPictures.add(picture)
                pictures.notifyDataSetChanged()

                println("SAVED")
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
            saveNote(note)
        }
    }

    private fun saveNote(newNote: Note, callback: ((Note) -> Unit)? = null) {
        lifecycleScope.launch {
            note = Note.saveNote(newNote, requireContext())
        }

        callback?.invoke(note)
    }
}