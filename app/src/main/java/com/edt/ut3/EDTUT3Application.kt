package com.edt.ut3

import android.app.Application
import android.util.Log
import com.edt.ut3.ui.room_finder.RoomFinderRepository
import com.edt.ut3.ui.room_finder.RoomFinderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class EDTUT3Application : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.i(this::class.simpleName, "STARTED")

        startKoin {
            // declare used Android context
            androidContext(this@EDTUT3Application)

            // declare modules
            modules(
                roomFinderModule
            )
        }
    }

    val roomFinderModule = module {
        viewModel { RoomFinderViewModel(get()) }
        single { RoomFinderRepository() }
    }

}