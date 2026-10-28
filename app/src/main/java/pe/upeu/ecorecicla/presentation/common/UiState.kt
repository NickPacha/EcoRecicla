package pe.upeu.ecorecicla.presentation.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(
        val message: String,
        val validationErrors: Map<String, String> = emptyMap()
    ) : UiState<Nothing>
}
