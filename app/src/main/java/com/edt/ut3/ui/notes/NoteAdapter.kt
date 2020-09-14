package com.edt.ut3.ui.notes

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.toDp
import kotlinx.android.synthetic.main.layout_note.view.*
import java.text.SimpleDateFormat

class NoteAdapter(val dataset: MutableList<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val noteView: View): RecyclerView.ViewHolder(noteView)

    companion object {
        fun from(v: RecyclerView) = (v.adapter as NoteAdapter?)
    }

    var onItemClickListener: ((Note) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(R.layout.layout_note, parent, false)

        return NoteViewHolder(rootView)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = dataset[position]

        onItemClickListener?.let { itemListener ->
            holder.noteView.setOnClickListener {
                itemListener(currentNote)
            }
        }

        holder.noteView.apply {
            currentNote.title?.let {
                title.text = it
            } ?: run {
                title.visibility = GONE
            }

            short_desc.text = currentNote.contents

            reminder.text = if (currentNote.reminder) {
                SimpleDateFormat("yyyy/MM/dd - HH:mm ").format(currentNote.date)
            } else {
                context.getString(R.string.no_reminder_set)
            }
        }
    }

    override fun getItemCount() = dataset.size

    class NoteSeparator : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View,
                                    parent: RecyclerView, state: RecyclerView.State)
        {
            val spaceHeight = 16.toDp(parent.context).toInt()

            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }

                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }
}