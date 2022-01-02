package com.edt.ut3.refactored.ui

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import com.axellaffite.fastgallery.FastGallery
import com.axellaffite.fastgallery.slider_animations.SlideAnimations
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Note.Reminder.ReminderType
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.misc.extensions.set
import com.edt.ut3.misc.extensions.setTime
import com.edt.ut3.misc.extensions.updateIfNecessary
import com.edt.ut3.refactored.extensions.*
import com.edt.ut3.refactored.models.domain.EventWithNote
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.viewmodels.EventViewModel
import com.edt.ut3.refactored.viewmodels.event_details.EventDetailsViewModel
import com.edt.ut3.refactored.viewmodels.event_details.IEventDetailsSharedViewModel
import com.edt.ut3.ui.calendar.BottomSheetFragment
import com.edt.ut3.ui.calendar.event_details.ImageOverlayLayout
import com.edt.ut3.ui.custom_views.image_preview.ImagePreviewAdapter
import com.edt.ut3.ui.map.MapsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val layoutId = R.layout.fragment_event_details
class FragmentEventDetails : Fragment(layoutId), AdapterView.OnItemSelectedListener {

    private val sharedViewModel: IEventDetailsSharedViewModel by lazy { requireParentFragment().getViewModel() }
    private val eventViewModel: EventViewModel by viewModel()
    private val mapsViewModel: MapsViewModel by viewModel()
    private val eventDetailsViewModel: EventDetailsViewModel by viewModel()

    private var previousNote: Note? = null
    private var previousEvent: Event? = null

    /**
     * Used to launch an Intent that will take
     * a picture and write the result into the
     * provided file (pictureFile).
     */
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            eventDetailsViewModel.pictureFile?.let {
                if (success) {
                    Log.d("NOTE", previousNote.toString())
                    val previousNote = previousNote ?: return@let
                    val context = context ?: return@let

                    eventDetailsViewModel.addPictureToNote(context, previousNote)
                } else {
                    it.second.delete()
                }
            }
        }

    /**
     * Used to request a permission (in this case a CAMERA permission).
     * If the permission is granted, takePicture is called
     * otherwise, the previously created picture file is deleted.
     */
    private val grantCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            eventDetailsViewModel.pictureFile?.let {
                if (granted) {
                    takePicture.launch(it.second.androidUri)
                } else {
                    it.second.delete()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            mapsViewModel.launchDataUpdate()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapsViewModel.places.observe(viewLifecycleOwner) { setupPlaces() }

        sharedViewModel.event
            .switchMap(eventViewModel::listenToEventWithNote)
            .observe(viewLifecycleOwner, ::setupView)

        event_note.doOnTextChanged { text, start, before, count ->
            if (eventDetailsViewModel.saving.value == true) {
                previousNote?.let {
                    it.contents = text.toString()
                    eventDetailsViewModel.saveNote(it)
                }
            }
        }
    }

    private fun setupView(eventWithNote: EventWithNote?) {
        val event = eventWithNote?.event
        val note = eventWithNote?.note

        // Event isn't available anymore,
        // close the current view
        if (event == null) {
            val parent = parentFragment
            if (parent as? BottomSheetFragment != null) {
                parent.bottomSheetManager.setVisibleSheet(null)
                sharedViewModel.event.value = null
            }

            return
        }

        val previousNote = previousNote
        setupEventView(requireContext(), event)
        setupNoteView(
            event = event,
            note = note,
            sameNote = previousNote != null && (note?.id == previousNote.id)
        )
    }

    private fun setupEventView(context: Context, event: Event) {
        previousEvent = event
        title_container.setCardBackgroundColor(
            eventDetailsViewModel.getEventCardBackgroundColor(context, event)
        )

        title.text = event.courseOrCategory(context)
        from_to.text = eventDetailsViewModel.generateDateText(context, event)
        description.text = eventDetailsViewModel.buildDescription(event)
        setupPlaces(event)
    }

    private fun setupPlaces(event: Event? = null) {
        val event = event ?: previousEvent ?: return
        val context = context ?: return
        val places: List<Place> = mapsViewModel.matchingPlaces(event.sitesAndLocations)

        if (places.isNotEmpty()) {
            locations_container.removeAllViews()

            places.forEach { place ->
                locations_container.addView(
                    PlaceChip(context = context, place = place, onClickListener = {
                        showGoogleMaps(place)
                    })
                )
            }

            locations_container.visibility = VISIBLE
            locations_not_found_label.visibility = GONE
        } else {
            locations_container.visibility = GONE
            locations_not_found_label.visibility = VISIBLE
        }
    }

    private fun showGoogleMaps(place: Place) {
        activity?.let {
            mapsViewModel.routeFromTo(it, place.geolocalisation, place.title) {
                event_details_main?.let { mainView ->
                    Snackbar.make(
                        mainView,
                        R.string.unable_to_launch_googlemaps,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupNoteView(event: Event, note: Note?, sameNote: Boolean) {
        val noteToDisplay = note ?: Note.generateEmptyNote(event)
        previousNote = noteToDisplay

        val reminderTypes = ReminderType.values()
        if (!sameNote) {
            val adapterValues = reminderTypes.map { getString(it.resId) }
            reminder_spinner.adapter = buildSpinnerAdapter(adapterValues)
            reminder_spinner.onItemSelectedListener = this
        }

        event_note.updateIfNecessary(noteToDisplay.contents)
        pictures.adapter = createImagePreviewAdapter(noteToDisplay)
        pictures.notifyDataSetChanged()
        reminder_spinner.setSelection(noteToDisplay.reminder.getReminderType().ordinal)
    }

    private fun createImagePreviewAdapter(noteToDisplay: Note) = ImagePreviewAdapter(
        dataset = noteToDisplay.pictures,
        onAddPictureClickListener = { takePicture() },
        onItemClickListener = { _, picture, pictures ->
            val overlayLayout = ImageOverlayLayout(requireContext())
            val fragment = buildImageViewer(overlayLayout, noteToDisplay, picture, pictures)
            overlayLayout.onDeleteRequest = {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_image)
                    .setPositiveButton(R.string.action_ok) { _, _ ->
                        val viewPager = fragment.getViewPager()
                        viewPager?.run {
                            val itemPosition = currentItem
                            noteToDisplay.removePictureAt(itemPosition)
                            eventDetailsViewModel.saveNote(noteToDisplay, delayMs = 0)

                            if (noteToDisplay.pictures.isEmpty()) {
                                fragment.dismiss()
                            } else {
                                adapter?.notifyItemRemoved(itemPosition)
                            }
                        }
                    }
                    .setNegativeButton(R.string.action_cancel) { _, _ -> }
                    .also(AlertDialog.Builder::show)
            }

            fragment.show(parentFragmentManager, "eventDetailsGallery")
        }
    )

    private fun buildImageViewer(
        overlayLayout: ImageOverlayLayout,
        noteToDisplay: Note,
        picture: Picture,
        pictures: List<Picture>
    ) = FastGallery.Builder<Picture>()
        .withBackgroundResource(R.color.backgroundColor)
        .withImages(noteToDisplay.pictures)
        .withInitialPosition(pictures.indexOf(picture))
        .withOffscreenLimit(2)
        .withSlideAnimation(SlideAnimations.zoomOutAnimation())
        .withOverlay(overlayLayout)
        .withConverter { displayedPicture, imageLoader ->
            lifecycleScope.launchWhenResumed {
                imageLoader.fromFile(File(displayedPicture.picture).toUri())
            }
        }.build()

    private fun buildSpinnerAdapter(adapterValues: List<String>) =
        object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            adapterValues
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getView(position, convertView, parent).apply {
                    setPadding(0, paddingTop, 0, paddingBottom)
                }
            }
        }

    private fun takePicture() {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss")
        val name = Picture.generateFilename(dateFormat.format(Date()))
        val file = Picture.prepareImageFile(
            requireContext(),
            name
        )

        eventDetailsViewModel.pictureFile = name to file
        val cameraPermission = Manifest.permission.CAMERA
        val grantStatus = ContextCompat.checkSelfPermission(requireContext(), cameraPermission)
        if (grantStatus != PackageManager.PERMISSION_GRANTED) {
            grantCameraPermission.launch(cameraPermission)
        } else {
            takePicture.launch(file.androidUri)
        }
    }

    val File.androidUri
        get() = FileProvider.getUriForFile(
            requireContext(),
            "com.edt.ut3.fileprovider",
            this
        )

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        eventDetailsViewModel.handleReminderChoice(previousNote, position) { note ->
            promptForDateTime(
                context = requireContext(),
                date = note.reminder.date,
                onCancel = { reminder_spinner.setSelection(note.reminder.getReminderType().ordinal) },
                onConfirm = { date ->
                    note.reminder.setCustomReminder(date)
                    eventDetailsViewModel.saveNote(note)
                }
            )
        }
    }

    private fun promptForDateTime(
        context: Context,
        date: Date,
        onCancel: () -> Unit,
        onConfirm: (Date) -> Unit
    ) {
        val newDate = Date()
        val year = date.actualYear
        val month = date.actualMonth
        val day = date.actualDayOfMonth
        val hour = date.actualHour
        val minute = date.actualMinutes

        val timeListener = { _: TimePicker, newHour: Int, newMinute: Int ->
            newDate.setTime(newHour, newMinute)
            onConfirm(newDate)
        }

        val dateListener = { _: DatePicker, newYear: Int, newMonth: Int, newDayOfMonth: Int ->
            newDate.set(newYear, newMonth, newDayOfMonth)
            TimePickerDialog(context, timeListener, hour, minute, true).apply {
                setOnCancelListener { onCancel() }
            }.show()
        }

        DatePickerDialog(context, dateListener, year, month, day).apply {
            setOnCancelListener { onCancel() }
        }.show()
    }

}