package com.fildy.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation destinations. Add a new [NavKey] here for each screen. */
@Serializable
data object Home : NavKey

/** Voice mode: listening to / recording the user. */
@Serializable
data object Voice : NavKey
