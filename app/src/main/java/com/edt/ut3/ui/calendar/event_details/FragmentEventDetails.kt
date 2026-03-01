package com.edt.ut3.ui.calendar.event_details

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.axellaffite.fastgallery.FastGallery
import com.axellaffite.fastgallery.slider_animations.SlideAnimations
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.maps.MapsUtils
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Note.Reminder.ReminderType
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.extensions.onBackPressed
import com.edt.ut3.misc.extensions.set
import com.edt.ut3.misc.extensions.setTime
import com.edt.ut3.ui.map.MapsViewModel
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.theme.UT3Theme
import com.elzozor.yoda.utils.DateExtensions.get
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FragmentEventDetails : Fragment() {

    private var updateLocationJob: Job? = null
    private lateinit var event: Event

    private val mapsViewModel: MapsViewModel by activityViewModels()

    private var eventNoteLD : LiveData<Note>? = null
    private var canTakePicture = true
    private var firstNoteUpdate = true

    private lateinit var currentNote: Note

    private var pictureFile: File? = null
    private var pictureName: String? = null

    var onReady : (() -> Unit)? = null

    var composeState by mutableStateOf(EventDetailsState())
    val snackbarHostState = SnackbarHostState()

    var listenTo = MutableLiveData<Event>(null)
        set(value) {
            firstNoteUpdate = true
            value.observe(viewLifecycleOwner, Observer {
                it?.let { event ->
                    setupNewEvent(event)
                }
            })

            field = value
        }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            addPictureToNote(pictureName!!, pictureFile!!)
        } else {
            pictureFile?.delete()
        }
    }

    private val grantCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pictureFile?.let {
                takePicture.launch(uriFromFile(it))
            }
        } else {
            pictureFile?.delete()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pictureName", pictureName)
        outState.putSerializable("pictureFile", pictureFile)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.takeIf { it.containsKey("pictureName") && it.containsKey("pictureFile") }?.run {
            pictureName = getString("pictureName")
            pictureFile = getSerializable("pictureFile") as? File
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).also {
            lifecycleScope.launch {
                if (mapsViewModel.getPlaces(requireContext()).value.isNullOrEmpty()) {
                    mapsViewModel.launchDataUpdate(requireContext())
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ComposeView).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                UT3Theme {
                    EventDetailsScreen(
                        state = composeState,
                        callbacks = buildCallbacks(),
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }

        if (listenTo.value == null && isVisible) {
            onBackPressed()
        }

        listenTo.observe(viewLifecycleOwner, Observer { event: Event? ->
            event?.let {
                setupNewEvent(event)
            }
        })
    }

    private fun buildCallbacks() = EventDetailsCallbacks(
        onClose = { requireActivity().onBackPressed() },
        onPlaceClick = ::handlePlaceClick,
        onReminderSelected = ::handleReminderSelection,
        onPictureClick = ::handlePictureClick,
        onAddPictureClick = ::takePicture,
        onNoteTextChanged = ::handleNoteTextChanged
    )

    private fun setupNewEvent(event: Event) {
        this.event = event
        currentNote = Note.generateEmptyNote(event)
        lifecycleScope.launchWhenCreated {

            NotesViewModel(requireContext()).run {
                whenResumed {
                    setupContent()
                    setupListeners()

                    onReady?.invoke()

                    eventNoteLD?.removeObservers(viewLifecycleOwner)
                    firstNoteUpdate = true

                    eventNoteLD = getNoteByEventIDLD(event.id)
                    eventNoteLD?.observe(viewLifecycleOwner, Observer { note: Note? ->
                        updateNoteContents(note)
                    })
                }
            }
        }
    }

    private fun setupContent() {
        composeState = composeState.copy(isLoading = true)

        val indicatorColor = when (PreferencesManager.getInstance(requireContext()).currentTheme()) {
            Theme.LIGHT -> Color(event.lightBackgroundColor(requireContext()))
            Theme.DARK -> Color(event.darkBackgroundColor(requireContext()))
        }

        val descriptionBuilder = StringBuilder()
        event.categoryWithEmotions()?.let { descriptionBuilder.append(it).append("\n") }

        val locations = event.locations.joinToString(", ")
        if (locations.isNotBlank()) {
            descriptionBuilder.append(locations)
        } else {
            descriptionBuilder.append(event.sites.joinToString(", "))
        }
        descriptionBuilder.append("\n")

        event.description?.let { descriptionBuilder.append(it) }

        composeState = composeState.copy(
            title = event.courseOrCategory(requireContext()),
            dateText = generateDateText(),
            descriptionText = descriptionBuilder.toString(),
            indicatorColor = indicatorColor
        )
    }

    private fun updateNoteContents(newNote: Note?) {
        if (firstNoteUpdate) {
            initialNoteSetup(newNote)
        } else {
            newNote?.let {
                if (currentNote.pictures != it.pictures) {
                    currentNote.pictures.clear()
                    currentNote.pictures.addAll(it.pictures)
                    updateComposeStatePictures()
                }

                if (currentNote.reminder != newNote.reminder) {
                    currentNote.reminder.setupFrom(newNote.reminder)
                    composeState = composeState.copy(
                        selectedReminderIndex = ReminderType.values().indexOf(currentNote.reminder.getReminderType())
                    )
                }
            } ?: run {
                clearSpinner()
            }
        }
    }

    private fun clearSpinner() {
        composeState = composeState.copy(selectedReminderIndex = 0)
    }

    private fun initialNoteSetup(newNote: Note?) {
        currentNote = newNote ?: Note.generateEmptyNote(event)

        val reminderOptions = ReminderType.values().map { getString(reminderText(it)) }
        val typeIndex = ReminderType.values().indexOf(currentNote.reminder.getReminderType())

        composeState = composeState.copy(
            isLoading = false,
            noteText = currentNote.contents,
            pictures = currentNote.pictures.toList(),
            selectedReminderIndex = typeIndex,
            reminderOptions = reminderOptions
        )

        firstNoteUpdate = false
    }

    private fun reminderText(type: ReminderType): Int = when (type) {
        ReminderType.NONE -> R.string.reminder_none
        ReminderType.FIFTEEN_MINUTES -> R.string.reminder_fifteen
        ReminderType.THIRTY_MINUTES -> R.string.reminder_thirty
        ReminderType.ONE_HOUR -> R.string.reminder_hour
        ReminderType.CUSTOM -> R.string.reminder_custom
    }

    private fun updateReminderSpinner(newNote: Note?) {
        newNote?.let {
            val typeIndex = ReminderType.values().indexOf(newNote.reminder.getReminderType())
            composeState = composeState.copy(selectedReminderIndex = typeIndex)
        } ?: run {
            composeState = composeState.copy(selectedReminderIndex = 0)
        }
    }

    private fun refreshLocations(places: List<Place>) {
        updateLocationJob?.cancel()
        updateLocationJob = lifecycleScope.launch {
            whenCreated {
                val matchingPlaces = withContext(Default) {
                    val parsedPlaces = places.map { it.apply { title =
                        title.lowercase(Locale.FRENCH)
                    } }

                    computeMatchingLocations(parsedPlaces)
                }

                whenResumed {
                    withContext(Main) {
                        composeState = composeState.copy(matchingPlaces = matchingPlaces)
                    }
                }
            }
        }
    }

    private suspend fun computeMatchingLocations(places: List<Place>) = withContext(Default) {
        val matchingPlaces =
            event.locations.map { location ->
                val lowerCaseLocation = location.lowercase(Locale.FRENCH)
                val correspondingPlace = places.find { place ->
                    lowerCaseLocation.contains(place.title)
                }

                correspondingPlace
            }

        matchingPlaces.filterNotNull()
    }

    private fun setupListeners() {
        mapsViewModel.getPlaces(requireContext()).observe(viewLifecycleOwner, Observer {
            refreshLocations(it)
        })
    }

    private fun handleReminderSelection(index: Int) {
        val type = ReminderType.values()[index]
        when (type) {
            ReminderType.NONE -> currentNote.reminder.disable()
            ReminderType.FIFTEEN_MINUTES -> currentNote.reminder.setFifteenMinutesBefore()
            ReminderType.THIRTY_MINUTES -> currentNote.reminder.setThirtyMinutesBefore()
            ReminderType.ONE_HOUR -> currentNote.reminder.setOneHourBefore()
            ReminderType.CUSTOM -> askUserForDateTime(currentNote.date) { date: Date ->
                currentNote.reminder.setCustomReminder(date)
                lifecycleScope.launch { saveNote() }
            }
        }

        if (type != ReminderType.CUSTOM) {
            lifecycleScope.launch { saveNote() }
        }
    }

    private fun handlePictureClick(picture: Picture) {
        val overlayLayout = ImageOverlayLayout(requireContext())

        val fragment = FastGallery.Builder<Picture>()
            .withBackgroundResource(R.color.backgroundColor)
            .withImages(currentNote.pictures)
            .withInitialPosition(currentNote.pictures.indexOf(picture))
            .withOffscreenLimit(2)
            .withSlideAnimation(SlideAnimations.zoomOutAnimation())
            .withOverlay(overlayLayout)
            .withConverter { displayedPicture, imageLoader ->
                lifecycleScope.launchWhenResumed {
                    imageLoader.fromFile(File(displayedPicture.picture).toUri())
                }
            }.build()

        overlayLayout.onDeleteRequest = {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_image)
                .setPositiveButton(R.string.action_ok) { _, _ ->
                    val viewPager = fragment.getViewPager()
                    viewPager?.run {
                        val itemPosition = currentItem
                        currentNote.removePictureAt(itemPosition)

                        lifecycleScope.launchWhenResumed {
                            saveNote {
                                updateComposeStatePictures()
                                if (currentNote.pictures.isEmpty()) {
                                    fragment.dismiss()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .also {
                    it.show()
                }
        }

        fragment.show(parentFragmentManager, "eventDetailsGallery")
    }

    private fun handleNoteTextChanged(text: String) {
        if (currentNote.contents != text) {
            currentNote.contents = text
            composeState = composeState.copy(noteText = text)
            lifecycleScope.launch { saveNote() }
        }
    }

    private fun handlePlaceClick(place: Place) {
        activity?.let {
            MapsUtils.routeFromTo(it, place.geolocalisation, place.title) {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        getString(R.string.unable_to_launch_googlemaps)
                    )
                }
            }
        }
    }

    private fun updateComposeStatePictures() {
        composeState = composeState.copy(pictures = currentNote.pictures.toList())
    }

    private fun askUserForDateTime(date: Date, callback: (Date) -> Unit) {
        context?.let {
            askUserForDate(it, date, callback)
        }
    }

    private fun askUserForDate(context: Context, date: Date, callback: (Date) -> Unit) {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH)
        val day = date.get(Calendar.DAY_OF_MONTH)

        val dateListener = { _: DatePicker, newYear: Int, newMonth: Int, newDayOfMonth: Int ->
            val newDate = Date().set(newYear, newMonth, newDayOfMonth)
            askUserForTime(context, date, newDate, callback)
        }

        DatePickerDialog(context, dateListener, year, month, day).apply {
            setOnCancelListener { updateReminderSpinner(currentNote) }
        }.show()
    }

    private fun askUserForTime(context: Context, date: Date, newDate: Date, callback: (Date) -> Unit) {
        val hour = date.get(Calendar.HOUR_OF_DAY)
        val minute = date.get(Calendar.MINUTE)

        val timeListener = { _: TimePicker, newHour: Int, newMinute: Int ->
            newDate.setTime(newHour, newMinute)
            callback(newDate)
        }

        TimePickerDialog(context, timeListener, hour, minute, true).apply {
            setOnCancelListener { updateReminderSpinner(currentNote) }
        }.show()
    }

    private fun generateDateText(): String {
        val date =
            SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault()).format(event.start)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val time =
            if (event.allday) {
                getString(R.string.all_day)
            } else {
                val start = "%02dh%02d".format(
                    event.start.get(Calendar.HOUR_OF_DAY),
                    event.start.get(Calendar.MINUTE)
                )
                val end = "%02dh%02d".format(
                    event.end?.get(Calendar.HOUR_OF_DAY),
                    event.end?.get(Calendar.MINUTE)
                )

                val fromToFormat = getString(R.string.from_to_format)

                fromToFormat.format(start, end)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

        return "$date\n$time"
    }

    private fun takePicture() {
        if (!canTakePicture) {
            lifecycleScope.launch {
                snackbarHostState.showSnackbar(getString(R.string.unable_take_while_saving))
            }
            return
        }

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

    private fun uriFromFile(file: File) =
        FileProvider.getUriForFile(requireContext(), "com.edt.ut3.fileprovider", file)

    private fun generateOutputFile(callback: ((name: String, file: File) -> Unit)) {
        lifecycleScope.launch {
            saveNote {
                val name = Picture.generateFilename(it.id.toString())
                val file = Picture.prepareImageFile(
                    requireContext(),
                    name
                )

                callback(name, file)
            }
        }
    }

    private fun addPictureToNote(name: String, file: File) {
        lifecycleScope.launch {
            canTakePicture = false
            Log.d(this@FragmentEventDetails::class.simpleName, "Note saved")

            val generated = Picture.generateFromPictureUri(requireContext(), name, file.absolutePath)
            currentNote.pictures.add(generated)

            saveNote {
                updateComposeStatePictures()
                canTakePicture = true
            }

            Log.d(this@FragmentEventDetails::class.simpleName, "picture added")
        }
    }

    private val save = Mutex()
    private val add = Mutex()
    private val callbackStack = Stack<(Note) -> Unit>()

    private suspend fun saveNote(callback: ((Note) -> Unit)? = null) {
        withContext(Default) {
            add.lock()
            callback?.let { callbackStack.add(it) }
            add.unlock()

            if (save.tryLock()) {

                withContext(IO) {
                    NotesViewModel(requireContext()).run {
                        save(currentNote)
                    }
                }

                add.lock()

                while (callbackStack.isNotEmpty()) {
                    withContext(Main) {
                        callbackStack.pop().invoke(currentNote)
                    }
                }

                save.unlock()
                add.unlock()
            }
        }
    }
}
