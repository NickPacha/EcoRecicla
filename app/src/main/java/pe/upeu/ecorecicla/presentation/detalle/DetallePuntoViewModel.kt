package pe.upeu.ecorecicla.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import pe.upeu.ecorecicla.presentation.common.UiState

data class DetallePuntoUiData(
    val punto: PuntoAcopio?,
    val pesajes: List<Pesaje>,
    val sesion: Sesion?
)

class DetallePuntoViewModel(
    private val pesajeRepository: PesajeRepository,
    private val rutaRepository: RutaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _puntoId = MutableStateFlow<Long?>(null)
    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading = _isOperationLoading.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage = _operationMessage.asStateFlow()

    fun initPunto(puntoId: Long) {
        _puntoId.value = puntoId
    }

    val uiState: StateFlow<UiState<DetallePuntoUiData>> = combine(
        _puntoId,
        rutaRepository.getPuntosFlow(),
        authRepository.getSesionFlow()
    ) { id, puntos, sesion ->
        if (id == null) {
            UiState.Loading
        } else {
            val punto = puntos.find { it.id == id }
            UiState.Success(DetallePuntoUiData(punto = punto, pesajes = emptyList(), sesion = sesion))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun getPesajesFlow(puntoId: Long) = pesajeRepository.getPesajesByPuntoFlow(puntoId)

    fun registrarPesaje(
        puntoId: Long,
        material: String,
        pesoBrutoStr: String,
        taraStr: String,
        observacion: String?
    ) {
        val pesoBruto = pesoBrutoStr.toDoubleOrNull() ?: 0.0
        val tara = taraStr.toDoubleOrNull() ?: 0.0

        if (pesoBruto <= 0 || tara < 0) {
            _operationMessage.value = "Ingrese valores numéricos válidos para Peso Bruto y Tara."
            return
        }

        viewModelScope.launch {
            _isOperationLoading.value = true
            _operationMessage.value = null

            pesajeRepository.registrarPesaje(puntoId, material, pesoBruto, tara, observacion)
                .onSuccess { pesaje ->
                    _operationMessage.value = if (pesaje.pendiente) {
                        "Pesaje guardado localmente en modo Offline (ID: ${pesaje.id})"
                    } else {
                        "Pesaje registrado en el servidor (ID: ${pesaje.id})"
                    }
                }
                .onFailure { error ->
                    if (error is ErrorApi.BadRequest) {
                        _operationMessage.value = error.message
                    } else {
                        _operationMessage.value = "Error: ${error.message}"
                    }
                }

            _isOperationLoading.value = false
        }
    }

    fun anularPesaje(pesajeId: Long, motivo: String) {
        if (motivo.isBlank()) {
            _operationMessage.value = "Debe ingresar un motivo para anular el pesaje."
            return
        }

        viewModelScope.launch {
            _isOperationLoading.value = true
            pesajeRepository.anularPesaje(pesajeId, motivo)
                .onSuccess {
                    _operationMessage.value = "Pesaje anulado exitosamente."
                }
                .onFailure { error ->
                    _operationMessage.value = "Error al anular: ${error.message}"
                }
            _isOperationLoading.value = false
        }
    }

    fun clearMessage() {
        _operationMessage.value = null
    }
}
