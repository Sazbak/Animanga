package org.example.common

import java.util.*

fun String.capitalizeFirstLetter() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }