package com.fildy.ui.home.header

/** The home screen's greeting and where the farm is. */
data class HomeHeaderUiState(
    val userName: String,
    /** e.g. "La Tranquera"; null until the farm is loaded. */
    val farmName: String? = null,
    /** e.g. "Castelli"; null until the farm is loaded. */
    val locality: String? = null,
)
