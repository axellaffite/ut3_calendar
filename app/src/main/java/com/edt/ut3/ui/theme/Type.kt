package com.edt.ut3.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.edt.ut3.R

val Roboto = FontFamily(Font(R.font.roboto))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontSize = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Roboto,
        fontSize = 12.sp
    )
)
