package com.edt.ut3.ui.calendar.event_details

//import com.squareup.picasso.Picasso
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.edt.ut3.ui.custom_views.image_preview.ImagePreviewAdapter
import com.edt.ut3.ui.preferences.Theme
import com.elzozor.yoda.utils.DateExtensions.get
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FragmentEventDetails() : Fragment() {

    constructor(event: Event): this() {
        this.event = event
    }

    private lateinit var event: Event

    private val viewModel: CalendarViewModel by activityViewModels()

    private lateinit var note: Note

    private var pictureFile: File? = null
    private var pictureName: String? = null

    /**
     * Used to launch an Intent that will take
     * a picture a write the result into the
     * provided file (pictureFile).
     */
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            addPictureToNote(pictureName!!, pictureFile!!)
        } else {
            pictureFile?.delete()
        }
    }

    /**
     * Used to request a permission (in this case a CAMERA permission).
     * If the permission is granted, takePicture is called
     * otherwise, the previously created picture file is deleted.
     */
    private val grantCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pictureFile?.let {
                takePicture.launch(uriFromFile(it))
            }
        } else {
            pictureFile?.delete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        event = viewModel.selectedEvent!!

        // Load asynchronously the attached note.
        // Once it's done, the contents
        lifecycleScope.launch {
            whenCreated {
                note = Note.generateEmptyNote(event.id)

                AppDatabase.getInstance(requireContext()).noteDao().run {
                    val result = selectByEventIDs(event.id)

                    if (result.size == 1) {
                        note = result[0]
                    }

                    view?.post {
                        setupContent()
                        setupListeners()
                    }

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the app bar
        requireActivity().nav_view.visibility = VISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false).also {
            // Hide the app bar
            requireActivity().nav_view.visibility = GONE
        }
    }

    /**
     * Setup the view contents.
     * Called once the event's note is loaded
     *
     */
    private fun setupContent() {
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

        title.text = event.courseName ?: event.category
        from_to.text = generateDateText()
        event_note.setText(note.contents)

        val descriptionBuilder = StringBuilder()
        event.category?.let { descriptionBuilder.append(it).append("\n") }

        val locations = event.locations.joinToString(", ")
        if (locations.isNotBlank()) {
            descriptionBuilder.append(locations)
        } else {
            descriptionBuilder.append(event.sites.joinToString(", "))
        }
        descriptionBuilder.append("\n")

        event.description?.let { descriptionBuilder.append(it) }
        description.text = descriptionBuilder.toString()


        // Setup the adapter contents and the item "onclick" callbacks.
        // Use the StfalconImageViewer library to display a fullscreen
        // image.
        pictures.adapter = ImagePreviewAdapter(note.pictures).apply {
            onItemClickListener = { v: View, p: Picture, dataSet: List<Picture> ->
                val overlayLayout = ImageOverlayLayout(requireContext())

                // This image builder is in charge to load images from
                // the memory into the ImageView of the image viewer.
                val imageBuilder = { view: ImageView, picture: Picture ->
                    Picasso.get().load(File(picture.picture)).fit().centerInside().into(view)

                    // On click we show or hide the overlay layout
                    // to allow the user to perform actions like
                    // deleting pictures.
                    view.setOnClickListener {
                        overlayLayout.showHideOverlay()
                    }
                }

                // This is the view that will display the images
                val imageViewer = StfalconImageViewer.Builder(context, dataSet, imageBuilder)
                    .withStartPosition(dataSet.indexOf(p))
                    .withTransitionFrom(v as ImageView)
                    .withOverlayView(overlayLayout)
                    .build()

                // We set the onDeleteRequest after creating the imageViewer
                // to hold a reference on it and make possible
                // to get the current position of the viewer in
                // order to delete the proper file.
                overlayLayout.apply {
                    onDeleteRequest = {
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
                                lifecycleScope.launch {
                                    note.removePictureAt(imageViewer.currentPosition())
                                    saveNote(note) {
                                        imageViewer.updateImages(it.pictures)
                                        pictures.notifyDataSetChanged()
                                    }
                                }
                            }
                            .show()
                    }
                }

                // Finally we display the imager viewer.
                imageViewer.show()
            }


            // The "take picture" button callback
            onAddPictureClickListener = {
                takePicture()
            }
        }


        // Add all the note event picture to the view model variable
        // It is used into several cases to add and delete pictures
        // to the current note.
        viewModel.selectedEventNote = note
        pictures.notifyDataSetChanged()
    }

    private fun setupListeners() {
        close_button.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Save the current note at each modification.
        event_note.doOnTextChanged { text, _, _, _ ->
            note.contents = text.toString()
            lifecycleScope.launch {
                saveNote(note)
            }
        }
    }

    /**
     * Generate the date text depending on
     * the event status (all day or classic)
     * and its start and end dates.
     */
    @SuppressLint("SimpleDateFormat")
    private fun generateDateText(): String {
        return if (event.allday) {
            getString(R.string.all_day)
        } else {
            val date = SimpleDateFormat("dd/MM/yyyy").format(event.start)
            val start = "${event.start.get(Calendar.HOUR_OF_DAY)}h${event.start.get(Calendar.MINUTE)}"
            val end = "${event.end.get(Calendar.HOUR_OF_DAY)}h${event.end.get(Calendar.MINUTE)}"

            "$date $start-$end"
        }
    }

    /**
     * Generates the output file, assign
     * the picture name and file variables
     * to the result, check the camera permission
     * and depending on the camera permission
     * call the takePicture variable or the
     * grandCameraPermission one.
     */
    private fun takePicture() {
        generateOutputFile { name, file ->
            pictureName = name
            pictureFile = file

            val cameraPermission = Manifest.permission.CAMERA
            val grantStatus = ContextCompat.checkSelfPermission(requireContext(), cameraPermission)
            if (grantStatus != PackageManager.PERMISSION_GRANTED) {
                grantCameraPermission.launch(cameraPermission)
            } else {
                takePicture.launch(uriFromFile(file))
            }
        }
    }

    /**
     * Simply generate an URI from a given file.
     *
     * @param file The concerned file
     */
    private fun uriFromFile(file: File) =
        FileProvider.getUriForFile(requireContext(), "com.edt.ut3.fileprovider", file)

    /**
     * Save the current note and create the
     * file that will store the picture.
     * This function is executed in a separate thread.
     *
     * Once it's done, call the provided callback
     * with these information.
     *
     * @param callback The action to execute once done.
     */
    private fun generateOutputFile(callback: ((name: String, file: File) -> Unit)) {
        lifecycleScope.launch {
            saveNote(note) {
                val name = Picture.generateFilename(it.id.toString())
                val file = Picture.prepareImageFile(
                    requireContext(),
                    name
                )

                callback(name, file)
            }
        }
    }


    /**
     * Generate the thumbnail and add it to the
     * current note.
     * The results are stored into a Picture class.
     *
     * The thumbnail name is generated with the
     * provided name. It just add "_thumbnail" to it.
     *
     * @param name The picture name
     * @param file The picture file
     */
    private fun addPictureToNote(name: String, file: File) {
        lifecycleScope.launch {
            saveNote(note) {
                lifecycleScope.launch {
                    val generated = Picture.generateFromPictureUri(requireContext(), name, file.absolutePath)
                    note.pictures.add(generated)

                    saveNote(note) {
                        pictures.notifyDataSetChanged()
                    }
                }
            }
        }
    }


    /**
     * Save the provided note into the database
     * and assign the old one to the result.
     * Call the callback once done if it's defined.
     *
     * @param newNote The note to save
     * @param callback The action to execute once done
     */
    private suspend fun saveNote(newNote: Note, callback: ((Note) -> Unit)? = null) {
        withContext(IO) {
            Note.saveNote(newNote, requireContext())
        }

        callback?.invoke(note)
    }
}