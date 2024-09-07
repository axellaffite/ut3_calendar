package com.edt.ut3.ui.preferences.formation.steps.state_fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class StateViewModel: ViewModel() {

    private val _position = StateStack()
    val position: LiveData<Position>
        get() = _position.getListener()

    private val _title = MutableLiveData<Int>()
    val title : LiveData<Int>
        get() = _title

    private val _summary = MutableLiveData<Int>()
    val summary : LiveData<Int>
        get() = _summary

    init {
        _position.push(0)
    }

    fun currentPosition(): Int = _position.peek()

    fun back() {
        _position.pop()
    }

    fun next() {
        val next = _position.peek() + 1

        val shouldInsert = _position.peek() < next
        if (shouldInsert) {
            _position.push(next)
        }
    }

    fun setTitle(id: Int) {
        _title.value = id
    }

    fun setDescription(id: Int) {
        _summary.value = id
    }


    data class Position(val previous: Int?, val current: Int?)

    private class StateStack(
        private val listener: MutableLiveData<Position> = MutableLiveData()
    ) : Stack<Int>() {
        override fun push(item: Int) : Int = synchronized(this) {
            listener.value = Position(
                if (isEmpty()) null else peek(),
                item
            )

            super.push(item)
        }

        override fun pop(): Int = synchronized(this) {
            listener.value = when (size) {
                1 -> listener.value
                else -> Position(super.pop(), peek())
            }

            return peek()
        }

        fun isNotEmpty() = isEmpty() == false

        fun getListener() : LiveData<Position> = listener
    }
}