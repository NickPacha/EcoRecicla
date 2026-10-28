package pe.upeu.ecorecicla.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaDia
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.common.UiState

data class RutaUiData(
    val ruta: RutaDia?,
    val puntos: List<PuntoAcopio>,
    val sesion: Sesion?
)

class RutaDiaViewModel(
    private val rutaRepository: RutaRepository,
    private val authRepository: AuthRepository,
    private val sincronizarUseCase: SincronizarUseCase
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    val uiState: StateFlow<UiState<RutaUiData>> = combine(
        rutaRepository.getRutaHoyFlow(),
        rutaRepository.getPuntosFlow(),
        authRepository.getSesionFlow()
    ) { ruta, puntos, sesion ->
        if (puntos.isEmpty() && ruta == null) {
            UiState.Empty
        } else {
            UiState.Success(RutaUiData(ruta, puntos, sesion))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun sincronizar() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            sincronizarUseCase()
                .onSuccess {
                    _syncMessage.value = "Sincronización completada con éxito."
                }
                .onFailure { error ->
                    _syncMessage.value = "Modo Offline: ${error.message}"
                }
            _isSyncing.value = false
        }
    }

    fun cerrarRuta(rutaId: Long) {
        viewModelScope.launch {
            _isSyncing.value = true
            rutaRepository.cerrarRuta(rutaId)
                .onSuccess {
                    _syncMessage.value = "La ruta se ha cerrado correctamente."
                }
                .onFailure { error ->
                    _syncMessage.value = "Error al cerrar ruta: ${error.message}"
                }
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
