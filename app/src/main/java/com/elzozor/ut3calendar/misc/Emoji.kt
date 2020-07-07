package com.elzozor.ut3calendar.misc

object Emoji {
     private val happy = arrayOf(
        "\uD83D\uDE03", "\uD83D\uDE42",
        "\uD83D\uDE0A", "\uD83D\uDE4B",
        "\uD83D\uDE4B\uD83C\uDFFF", "\uD83D\uDE0F",
        "\uD83D\uDE2C", "\uD83C\uDF49",
        "\uD83C\uDF46"
    )

    private val sad = arrayOf(
        "\uD83D\uDE10", "\uD83D\uDE25",
        "\uD83D\uDE1F", "\uD83D\uDE13",
        "\uD83D\uDE22", "\uD83D\uDE41",
        "\uD83D\uDE2A", "\uD83D\uDE15",
        "\uD83D\uDE29", "\uD83D\uDE30"
    )

    fun happy() = happy.random()
    fun sad() = sad.random()
    fun whatever() = arrayOf(happy(), sad()).random()
}