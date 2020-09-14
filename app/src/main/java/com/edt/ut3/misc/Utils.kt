package com.edt.ut3.misc

import android.app.Activity
import android.content.Context
import android.text.Html
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*

fun Date.addAssign(field: Int, amount: Int) {
    time = Calendar.getInstance().run {
        time = this@addAssign
        add(field, amount)
        time.time
    }
}

fun Date.add(field: Int, amount: Int): Date = Calendar.getInstance().run {
    time = this@add
    add(field, amount)
    time
}

fun Date.minus(field: Int, amount: Int) =
    this.add(field, -amount)

fun Date.minusAssign(field: Int, amount: Int) =
    this.addAssign(field, -amount)

fun Date.toCelcatDateStr() =
    DateFormat.format("yyyy-MM-dd", this).toString()

fun Date.toCelcatDateTimeStr() =
    DateFormat.format("yyyy-MM-dd", this).toString()

@Throws(Exception::class)
fun Date.fromCelcatString(date: String) {
    time =  Html.escapeHtml(date).toString().replace('T', ' ').let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).parse(it)
    }?.time ?: throw Exception()
}

fun Date.cleaned(vararg fields: Int) : Date {
    return Date(time).apply {
        clean(*fields)
    }
}

fun Date.clean(vararg fields: Int) {
    time = Calendar.getInstance().let {
        it.time = this
        for (f in fields) {
            it.set(f, 0)
        }

        it.time.time
    }
}

fun Date.timeCleaned() = Date(time).apply {
    cleanTime()
}

fun Date.cleanTime() = clean(
    Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND
)

fun Date.set(year: Int, month: Int, day: Int): Date = this.apply {
    time = Calendar.getInstance().run {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        time.time
    }

    cleanTime()
}

fun Date.toFormattedTime(format: String) = SimpleDateFormat(format).format(this)



@Throws(JSONException::class)
fun <T> JSONArray.toList(): List<T> =
    (0 until length()).map {
        get(it) as T
    }

fun <T> JSONArray.map(consumer: (Any?) -> T) =
    (0 until length()).map {
        consumer(get(it))
    }

fun JSONArray.forEach(consumer: (Any?) -> Unit) {
    (0 until length()).forEach {
        consumer(get(it))
    }
}

fun String.fromHTML() : String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString();
    } else {
        Html.fromHtml(this).toString();
    }
}



fun Number.toDp(context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
)

fun Activity.hideKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE)
    with (imm as InputMethodManager?) {
        this?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
}

fun Fragment.hideKeyboard() {
    val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE)
    with (imm as InputMethodManager?) {
        this?.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}