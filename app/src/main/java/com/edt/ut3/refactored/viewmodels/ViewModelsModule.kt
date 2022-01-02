package com.edt.ut3.refactored.viewmodels

import com.edt.ut3.refactored.viewmodels.event_details.EventDetailsCalendarSharedViewModel
import com.edt.ut3.refactored.viewmodels.event_details.EventDetailsViewModel
import com.edt.ut3.refactored.viewmodels.event_details.IEventDetailsSharedViewModel
import com.edt.ut3.ui.map.MapsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelsModule = module {
    single { CoursesViewModel(get()) }
    single { EdtChangeViewModel(get()) }
    single { EventViewModel(get()) }
    single { NotesViewModel(get()) }
    single { PlaceViewModel(get()) }
    single { MapsViewModel(get(), get(), get()) }

    viewModel { EventDetailsViewModel(get(), get()) }

    viewModel<IEventDetailsSharedViewModel> { get<EventDetailsCalendarSharedViewModel>() }
    viewModel { EventDetailsCalendarSharedViewModel() }
}