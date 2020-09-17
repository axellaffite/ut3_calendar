package com.edt.ut3.ui.calendar.event_details

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.lifecycle.whenResumed
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.database.viewmodels.NotesViewModel
import com.edt.ut3.backend.maps.MapsUtils
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Note.Reminder.ReminderType
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.set
import com.edt.ut3.misc.setTime
import com.edt.ut3.ui.calendar.CalendarViewModel
import com.edt.ut3.ui.custom_views.image_preview.ImagePreviewAdapter
import com.edt.ut3.ui.map.MapsViewModel
import com.edt.ut3.ui.preferences.Theme
import com.elzozor.yoda.utils.DateExtensions.get
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlinx.android.synthetic.main.fragment_event_details.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FragmentEventDetails : Fragment() {

    private var updateLocationJob: Job? = null
    private lateinit var event: Event

    private val calendarViewModel: CalendarViewModel by activityViewModels()
    private val mapsViewModel: MapsViewModel by activityViewModels()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false).also { root ->
            // Load asynchronously the attached note.
            // Once it's done, the contents
            lifecycleScope.launch {
                if (mapsViewModel.getPlaces(root.context).value.isNullOrEmpty()) {
                    mapsViewModel.launchDataUpdate(root.context)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarViewModel.selectedEvent.observe(viewLifecycleOwner) {
            it?.let { event ->
                setupNewEvent(event)
            }
        }
    }

    private fun setupNewEvent(event: Event) {
        this.event = event
        lifecycleScope.launchWhenCreated {
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(event.start)
            note = Note.generateEmptyNote("$date - ${event.courseName}", event.id)

            AppDatabase.getInstance(requireContext()).noteDao().run {
                val result = selectByEventIDs(event.id)

                if (result.size == 1) {
                    note = result[0]
                }

                whenResumed {
                    setupContent()
                    setupListeners()
                }
            }
        }
    }



    /**
     * Setup the view contents.
     * Called once the event's note is loaded
     *
     */
    private fun setupContent() {
        when (PreferencesManager(requireContext()).currentTheme()) {
            Theme.LIGHT -> {
                event.lightBackgroundColor(requireContext()).let {
                    title_container.setCardBackgroundColor(it)
                }
            }

            Theme.DARK -> {
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
            onItemClickListener = { imgView, picture, pictures ->
                val extras = FragmentNavigatorExtras(
                    imgView to "end_transition"
                )

                imgView.transitionName = "start_transition"

                findNavController()
                    .navigate(
                        R.id.action_navigation_calendar_to_fragmentImageViewPager,
                        Bundle().apply { putInt("position", pictures.indexOf(picture)) },
                        null,
                        extras
                    )
            }


            // The "take picture" button callback
            onAddPictureClickListener = {
                takePicture()
            }
        }


        // Add all the note event picture to the view model variable
        // It is used into several cases to add and delete pictures
        // to the current note.
        calendarViewModel.selectedEventNote = note
        pictures.notifyDataSetChanged()

        // The adapter is an extension of the ArrayAdapter class.
        // It's made like this to override the getView() function
        // in order to remove the left and right padding of the
        // spinner.
        val adapterValues = ReminderType.values().map { getString(reminderText(it)) }
        reminder_spinner.adapter = object: ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, adapterValues) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getView(position, convertView, parent).apply {
                    setPadding(0, paddingTop, 0, paddingBottom)
                }
            }
        }

        updateReminderSpinner()
    }

    /**
     * Returns the textual representation
     * (from strings.xml, not .toString())
     * of the given ReminderType.
     *
     * @param type The reminder type to convert
     * @return Its textual representation
     */
    private fun reminderText(type: ReminderType): Int = when (type) {
        ReminderType.NONE -> R.string.reminder_none
        ReminderType.FIFTEEN_MINUTES -> R.string.reminder_fifteen
        ReminderType.THIRTY_MINUTES -> R.string.reminder_thirty
        ReminderType.ONE_HOUR -> R.string.reminder_hour
        ReminderType.CUSTOM -> R.string.reminder_custom
    }

    private fun updateReminderSpinner() {
        val typeIndex = ReminderType.values().indexOf(note.reminder.getReminderType())
        reminder_spinner?.setSelection(typeIndex)
    }

    private fun refreshLocations(places: List<Place>) {
        updateLocationJob?.cancel()
        updateLocationJob = lifecycleScope.launch {
            whenCreated {
                val matchingPlaces = withContext(Default) {
                    val parsedPlaces = places.map { it.apply { title = title.toLowerCase(Locale.FRENCH) } }

                    computeMatchingLocations(parsedPlaces)
                }

                whenResumed {
                    refreshLocations(requireContext(), matchingPlaces)
                }
            }
        }
    }

    private fun refreshLocations(context: Context, places: List<Place>) {
        if (places.isNotEmpty()) {
            locations_container.removeAllViews()

            places.forEach {
                locations_container.addView(PlaceChip(context, it))
            }

            locations_container.visibility = VISIBLE
            locations_not_found_label.visibility = GONE
        }
    }

    private suspend fun computeMatchingLocations(places: List<Place>) = withContext(Default) {
        val matchingPlaces =
            event.locations.map { location ->
                val lowerCaseLocation = location.toLowerCase(Locale.FRENCH)
                val correspondingPlace = places.find { place ->
                    lowerCaseLocation.contains(place.title)
                }

                correspondingPlace
            }

        matchingPlaces.filterNotNull()
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

        reminder_spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            var isFirstTrigger = true

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, index: Int, id: Long) {
                if (isFirstTrigger) {
                    isFirstTrigger = false
                    return
                }

                val type = ReminderType.values()[index]
                when (type) {
                    ReminderType.NONE -> note.reminder.disable()
                    ReminderType.FIFTEEN_MINUTES -> note.reminder.setFifteenMinutesBefore()
                    ReminderType.THIRTY_MINUTES -> note.reminder.setThirtyMinutesBefore()
                    ReminderType.ONE_HOUR -> note.reminder.setOneHourBefore()
                    ReminderType.CUSTOM -> askUserForDateTime(note.date) { date: Date ->
                        note.reminder.setCustomReminder(date)
                        lifecycleScope.launch { saveNote(note) }
                    }
                }

                // We do not want to save the note
                // 2 times, especially if the dialog is
                // actually shown which is almost always the
                // case at this point.
                if (type != ReminderType.CUSTOM) {
                    lifecycleScope.launch { saveNote(note) }
                }
            }

            override fun onNothingSelected(adapter: AdapterView<*>?) {
                // Nothing to do here
            }

        }

        mapsViewModel.getPlaces(requireContext()).observe(viewLifecycleOwner) {
            refreshLocations(it)
        }
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
            setOnCancelListener { updateReminderSpinner() }
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
            setOnCancelListener { updateReminderSpinner() }
        }.show()
    }

    /**
     * Generate the date text depending on
     * the event status (all day or classic)
     * and its start and end dates.
     */
    private fun generateDateText(): String {
        val date =
            SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault()).format(event.start)
                .capitalize(Locale.getDefault())

        val time =
            if (event.allday) {
                getString(R.string.all_day)
            } else {
                val start = "%02dh%02d".format(event.start.get(Calendar.HOUR_OF_DAY), event.start.get(Calendar.MINUTE))
                val end = "%02dh%02d".format(event.end?.get(Calendar.HOUR_OF_DAY), event.end?.get(Calendar.MINUTE))

                val fromToFormat = getString(R.string.from_to_format)

                fromToFormat.format(start, end).capitalize(Locale.getDefault())
            }

        return "$date\n$time"
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
            NotesViewModel(requireContext()).run {
                save(newNote)
            }
        }

        callback?.invoke(note)
    }


    private inner class PlaceChip(context: Context, val place: Place) : Chip(context) {
        init {
            setChipDrawable(
                ChipDrawable.createFromAttributes(
                    context,
                    null,
                    0,
                    R.style.Widget_MaterialComponents_Chip_Action
                )
            )

            setOnClickListener {
                activity?.let {
                    MapsUtils.routeFromTo(it, null, place.geolocalisation, place.title) {
                        view?.event_details_main?.let { mainView ->
                            Snackbar.make(mainView, R.string.unable_to_launch_googlemaps, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }

            setChipBackgroundColorResource(R.color.foregroundColor)

            text = place.title.toUpperCase(Locale.FRENCH)
        }
    }
}