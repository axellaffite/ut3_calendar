package com.edt.ut3.backend.background_services


import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.edt.ut3.R
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.background_services.updater.Updater
import com.edt.ut3.backend.background_services.updater.UpdaterFactory
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


/**
 * Used to update the Calendar data in background.
 *
 * @param appContext The application context
 * @param workerParams The worker's parameters
 */
@Suppress("BlockingMethodInNonBlockingContext")
class UpdateScheduler(
    appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    companion object {
        // Used to provide a progress value outside
        // of the Worker.
        const val Progress = "progress"

        /**
         * Schedule a periodic update.
         * If the periodic update is already launched
         * it is not replaced.
         *
         * @param context A valid context
         */
        fun scheduleUpdate(context: Context) {
            val worker = PeriodicWorkRequestBuilder<UpdateScheduler>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "event_update",
                ExistingPeriodicWorkPolicy.KEEP,
                worker
            )
        }

        /**
         * Force an update that will execute
         * almost directly after the function call.
         *
         * @param context A valid context
         * @param viewLifecycleOwner The fragment's viewLifeCycleOwner
         * which will observe the Worker *optional*
         * @param observer An observer *optional*
         */
        fun launchUpdate(
            context: Context,
            firstUpdate : Boolean = false,
            viewLifecycleOwner: LifecycleOwner? = null,
            observer: Observer<WorkInfo>? = null
        ) {
            val inputData = Data.Builder().putBoolean("firstUpdate", firstUpdate).build()

            val worker = OneTimeWorkRequestBuilder<UpdateScheduler>().setInputData(inputData).build()
            WorkManager.getInstance(context).run {
                enqueueUniqueWork("event_update_force", ExistingWorkPolicy.KEEP, worker)

                if (viewLifecycleOwner != null && observer != null) {
                    getWorkInfoByIdLiveData(worker.id).observe(viewLifecycleOwner, observer)
                }
            }
        }
    }

    private val preferences = PreferencesManager.getInstance(applicationContext)
    private val observer = SchedulerObserver()

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val firstUpdate = inputData.getBoolean("firstUpdate", false)
            val updater = UpdaterFactory.createUpdater(preferences.update_method)

            try {
                withContext(Main) {
                    updater.getProgression().observeForever(observer)
                }

                updater.doUpdate(Updater.Parameters(
                    firstUpdate,
                    applicationContext,
                    PreferencesManager.getInstance(applicationContext)
                ))
            } finally {
                withContext(Main) {
                    updater.getProgression().removeObserver(observer)
                }
            }

        } catch (e: Updater.Failure) {
            return@coroutineScope Result.failure(
                Data.Builder()
                    .putString("error", e.getReasonMessage(applicationContext))
                    .build()
            )
        } catch (e: Exception) {
            return@coroutineScope Result.failure(
                Data.Builder()
                    .putString("error", applicationContext.getString(R.string.error_updater_unknown))
                    .build()
            )
        }

        Result.success()
    }

    private inner class SchedulerObserver: Observer<Int> {

        override fun onChanged(progression: Int) {
            GlobalScope.launch {
                setProgress(workDataOf(Progress to progression))
            }
        }

    }

}