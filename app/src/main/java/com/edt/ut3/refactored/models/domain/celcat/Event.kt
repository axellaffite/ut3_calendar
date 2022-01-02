package com.edt.ut3.refactored.models.domain.celcat

import android.content.Context
import androidx.room.*
import com.edt.ut3.R
import com.edt.ut3.refactored.models.repositories.database.Converter
import com.edt.ut3.misc.DestructedColor
import com.edt.ut3.misc.Emoji
import java.util.*

@Entity(tableName = "event")
data class Event(
    @PrimaryKey var id: String,
    var category: String?,
    var description: String?,
    var courseName: String?,
    @TypeConverters(Converter::class) var locations: List<String>,
    @TypeConverters(Converter::class) var sites: List<String>,
    @TypeConverters(Converter::class) var start: Date,
    @TypeConverters(Converter::class) var end: Date?,
    var allday: Boolean,
    var backgroundColor: String?,
    var textColor: String?,
    @ColumnInfo(name = "note_id") var noteID: Long?
) {
    @delegate:Ignore
    val categoryWithEmotions: String? by lazy {
        category?.let { category ->
            val examRegex = Regex(".*?(controle|examen).?")
            if (category.lowercase().matches(examRegex)) {
                "$category ${Emoji.sad()}"
            } else {
                category
            }
        }
    }

    fun courseOrCategory(context: Context) = courseName ?: category ?: defaultName(context)

    fun defaultName(context: Context) = context.getString(R.string.default_event_name)

    /**
     * Convert the background color into a darker color.
     * In case where the background color is missing (e.g. null)
     * the primaryColor is returned by the function.
     *
     * @param context Application context
     * @return The darkened color
     */
    fun darkBackgroundColor(context: Context): Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).changeLuminosity().toArgb()
    }

    /**
     * Convert the background color into an Int and returns it.
     * In case of null background color, the primaryColor is returned.
     *
     * @param context Application context
     * @return The converted color
     */
    fun lightBackgroundColor(context: Context): Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).toArgb()
    }

}