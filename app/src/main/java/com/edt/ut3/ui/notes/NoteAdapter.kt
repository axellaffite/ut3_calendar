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
import com.edt.ut3.databinding.LayoutNoteBinding
import com.edt.ut3.misc.extensions.toDp
import java.text.SimpleDateFormat

class NoteAdapter(val dataset: MutableList<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val noteView: View): RecyclerView.ViewHolder(noteView)

    companion object {
        fun from(v: RecyclerView) = (v.adapter as NoteAdapter?)
    }

    var onItemClickListener: ((Note) -> Unit)? = null

    private lateinit var binding: LayoutNoteBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        binding = LayoutNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding.root)
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
                binding.title.text = it
            } ?: run {
                binding.title.visibility = GONE
            }

            binding.shortDesc.text = currentNote.contents

            val reminderText = if (currentNote.reminder.isActive()) {
                SimpleDateFormat("dd/MM/yyyy - HH:mm ").format(currentNote.reminder.getReminderDate()!!)
            } else {
                context.getString(R.string.no_reminder_set)
            }

            binding.reminder.text = context.getString(R.string.note_reminder_pictures)
                .format(reminderText, currentNote.pictures.size)
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