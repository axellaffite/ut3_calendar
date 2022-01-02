package com.edt.ut3

import android.app.Application
import com.edt.ut3.refactored.models.modelsModule
import com.edt.ut3.refactored.viewmodels.viewModelsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class EdtUT3Application : Application(){
    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin{

            androidContext(this@EdtUT3Application)
            modules(modelsModule + viewModelsModule)
        }
    }
}