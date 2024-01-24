package com.flixclusive.core.ui.film

import com.flixclusive.core.util.common.ui.UiText

data class FilmUiState(
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val isFilmInWatchlist: Boolean = false,
)