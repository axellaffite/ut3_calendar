package com.edt.ut3.backend.background_services


import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.updater.Updater
import com.edt.ut3.backend.background_services.updater.UpdaterFactory
import com.edt.ut3.backend.notification.NotificationManager
import com.edt.ut3.backend.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random


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
    private val notificationManager = NotificationManager.getInstance(applicationContext)
    private val notificationID = synchronized(Random::class) {
        Random(System.currentTimeMillis()).nextInt()
    }

    private lateinit var observer: SchedulerObserver

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val firstUpdate = inputData.getBoolean("firstUpdate", false)
            val updater = UpdaterFactory.createUpdater(preferences.update_method)

            observer = SchedulerObserver(
                notificationTitle = updater.getUpdateNotificationTitle(applicationContext)
            )

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
            displayUpdateError(e)

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
        } finally {
            removeNotificationProgress()
        }

        Result.success()
    }

    /**
     * Used to display the notification progress.
     * There is no need to create the notification channel
     * as the [NotificationManager] will do it for us
     * when created.
     *
     * @param notificationTitle The title to display for
     * the notification. This title is usually provided
     * by the [Updater] itself.
     *
     * @param progression The progression to display. It
     * MUST be between 0 and 100 (included).
     */
    private fun displayNotificationProgress(notificationTitle: String, progression: Int) {
        notificationManager.displayUpdateProgressBar(notificationTitle, progression, notificationID)
    }


    /**
     * Used to remove the notification
     * that displays the progress bar.
     *
     * This function MUST be called at the end of the
     * doWork() function of the scheduler.
     *
     * For example, a good practice is to place
     * the function call into a finally catch
     * when a try-catch block is used (which
     * is probably the case here).
     */
    private fun removeNotificationProgress() {
        notificationManager.removeUpdateProgressBar(notificationID)
    }


    /**
     * Used to display an update error.
     * The
     *
     * @param reason
     */
    private fun displayUpdateError(exception: Updater.Failure) {
        if (exception.shouldBeNotified) {
            notificationManager.displayUpdateError(
                exception.getReasonMessage(applicationContext)
            )
        }
    }


    private inner class SchedulerObserver(
        val notificationTitle: String,
    ): Observer<Int> {

        override fun onChanged(progression: Int) {
            GlobalScope.launch {
                setProgress(workDataOf(Progress to progression))
            }

            displayNotificationProgress(notificationTitle, progression)
        }

    }

}