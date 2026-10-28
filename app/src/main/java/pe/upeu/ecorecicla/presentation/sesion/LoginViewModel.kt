package pe.upeu.ecorecicla.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.presentation.common.UiState

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Sesion>>(UiState.Empty)
    val uiState = _uiState.asStateFlow()

    fun login(usuario: String, contrasena: String) {
        if (usuario.isBlank() || contrasena.isBlank()) {
            _uiState.value = UiState.Error(
                message = "Por favor complete todos los campos",
                validationErrors = mapOf(
                    if (usuario.isBlank()) "usuario" to "El usuario no puede estar vacío" else "" to "",
                    if (contrasena.isBlank()) "contrasena" to "La contraseña no puede estar vacía" else "" to ""
                ).filterKeys { it.isNotBlank() }
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.login(usuario, contrasena)
                .onSuccess { sesion ->
                    _uiState.value = UiState.Success(sesion)
                }
                .onFailure { error ->
                    if (error is ErrorApi.BadRequest) {
                        _uiState.value = UiState.Error(error.message, error.validationErrors)
                    } else {
                        _uiState.value = UiState.Error(error.message ?: "Error al iniciar sesión")
                    }
                }
        }
    }
}
