package com.edt.ut3.backend.note

import androidx.room.*
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.misc.minus
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

@Entity(tableName = "note",
    foreignKeys = [
        ForeignKey(entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["event_id"]
        )
    ], indices = [Index("event_id")])
data class Note(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "event_id") var eventID: String?,
    var title: String?,
    var contents: String,
    @TypeConverters(Converter::class) var date: Date,
    var color: String?,
    var textColor: String?,
    @TypeConverters(Converter::class) val reminder: Reminder = Reminder(date),
    @TypeConverters(Converter::class) val pictures: MutableList<Picture> = mutableListOf())
{

    private constructor(id: Long, note: Note): this(
        id = id,
        eventID = note.eventID,
        title = note.title,
        contents = note.contents,
        date = note.date,
        color = note.color,
        textColor = note.textColor,
        reminder = note.reminder,
        pictures = note.pictures
    )

    init {
        reminder.date = date
    }

    companion object {
        fun generateEmptyNote(title: String? = null, eventID: String? = null) = Note(0L, eventID, title, "", Date(), null, null, Reminder(Date()))
    }

    fun clearPictures() {
        while (pictures.isNotEmpty()) {
            removePictureAt(pictures.lastIndex)
        }
    }

    fun removePictureAt(position: Int) {
        if (position in 0 .. pictures.lastIndex) {
            val picture = pictures[position]
            pictures.removeAt(position)
            cleanPictureData(picture)
        }
    }

    private fun removePicture(picture: Picture) {
        pictures.remove(picture)
        cleanPictureData(picture)
    }

    private fun cleanPictureData(picture: Picture) {
        File(picture.picture).delete()
        File(picture.thumbnail).delete()
    }

    /**
     * This class is in charge to determine
     * a reminder for a given date and a given
     * type of reminder.
     *
     * Available types are defined in the ReminderType
     * enum class.
     *
     * @property noteDate The base date which will be used
     * to compute the actual reminder time.
     */
    data class Reminder(
        var date: Date
    ){

        private var type: ReminderType = ReminderType.NONE
        private var customDate: Date? = null

        enum class ReminderType {
            NONE,
            FIFTEEN_MINUTES,
            THIRTY_MINUTES,
            ONE_HOUR,
            CUSTOM
        }

        /**
         * Computes the date depending on the
         * reminder type and the date set.
         *
         * If the reminder is disabled (type == ReminderType.NONE)
         * the resulting date is null.
         */
        fun getReminderDate() = when (type) {
            ReminderType.NONE -> null
            ReminderType.FIFTEEN_MINUTES -> date.minus(Calendar.MINUTE, 15)
            ReminderType.THIRTY_MINUTES -> date.minus(Calendar.MINUTE, 30)
            ReminderType.ONE_HOUR -> date.minus(Calendar.HOUR, 1)
            ReminderType.CUSTOM -> date.minus(Calendar.MINUTE, 15)
        }

        /**
         * Returns whether the current reminder is active.
         */
        fun isActive() = (type != ReminderType.NONE)

        /**
         * Disable the current reminder.
         */
        fun disable() {
            type = ReminderType.NONE
        }

        /**
         * Set the reminder fifteen minutes before
         * the date.
         */
        fun setFifteenMinutesBefore() {
            type = ReminderType.FIFTEEN_MINUTES
        }

        /**
         * Set the reminder thirty minutes before
         * the date.
         */
        fun setThirtyMinutesBefore() {
            type = ReminderType.THIRTY_MINUTES
        }

        /**
         * Set the reminder one hour before
         * the date.
         */
        fun setOneHourBefore() {
            type = ReminderType.ONE_HOUR
        }

        /**
         * Set a custom reminder date not
         * depending on the base date.
         *
         * @param reminderDate The custom date
         */
        fun setCustomReminder(reminderDate: Date) {
            type = ReminderType.CUSTOM
            customDate = reminderDate
        }

        companion object {
            @Throws(JSONException::class)
            fun fromJSON(str: String): Reminder {
                val json = JSONObject(str)
                return Reminder(Date(json.getLong("date"))).apply {
                    type = ReminderType.valueOf(json.getString("type"))
                    customDate =
                        if (json.isNull("custom_date")) { null }
                        else { Date(json.getLong("custom_date")) }
                }
            }
        }

        fun toJSON() = JSONObject().apply {
            put("date", date.time)
            put("type", type.toString())
            put("custom_date", customDate?.time ?: 0L)
        }

        fun getReminderType() = type

    }

}