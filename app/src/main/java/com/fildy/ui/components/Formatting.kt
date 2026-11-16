package com.fildy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.fildy.R
import java.text.NumberFormat
import java.util.Locale

/** Locale of the app's strings (R.string.locale_tag), so numbers and dates match the text. */
@Composable
fun rememberAppLocale(): Locale {
    val tag = stringResource(R.string.locale_tag)
    return remember(tag) { Locale.forLanguageTag(tag) }
}

/** "2.247" in es-AR. */
fun Int.formatted(locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(this)

/** "131" or "12,5": at most one decimal, none when it's a whole number. */
fun Double.formatted(locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(this)
