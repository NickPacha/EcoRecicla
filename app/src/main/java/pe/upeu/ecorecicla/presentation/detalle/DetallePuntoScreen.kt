package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.presentation.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePuntoScreen(
    puntoId: Long,
    viewModel: DetallePuntoViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(puntoId) {
        viewModel.initPunto(puntoId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val pesajesList by viewModel.getPesajesFlow(puntoId).collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }

    var material by remember { mutableStateOf("Plástico PET") }
    var pesoBrutoStr by remember { mutableStateOf("") }
    var taraStr by remember { mutableStateOf("") }
    var observacion by remember { mutableStateOf("") }

    var pesajeAAnular by remember { mutableStateOf<Pesaje?>(null) }
    var motivoAnulacion by remember { mutableStateOf("") }

    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Punto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                is UiState.Success, is UiState.Empty -> {
                    val data = (state as? UiState.Success)?.data
                    val punto = data?.punto
                    val privilegios = data?.sesion?.privilegios ?: emptySet()
                    val puedeCrearPesaje = privilegios.contains("PESAJE_CREAR")
                    val puedeAnularPesaje = privilegios.contains("PESAJE_ANULAR")

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Punto
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = punto?.razonSocial ?: "Punto #$puntoId",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(text = "Dirección: ${punto?.direccion ?: "N/A"}")
                                    Text(text = "Zona: ${punto?.zona ?: "N/A"} | Material Habitual: ${punto?.materialHabitual ?: "N/A"}")
                                }
                            }
                        }

                        // Formulario de Registro de Pesaje (RF-A05, RN-03)
                        if (puedeCrearPesaje) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "Registrar Nuevo Pesaje", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = material,
                                            onValueChange = { material = it },
                                            label = { Text("Tipo de Material") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = pesoBrutoStr,
                                                onValueChange = { pesoBrutoStr = it },
                                                label = { Text("Peso Bruto (kg)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedTextField(
                                                value = taraStr,
                                                onValueChange = { taraStr = it },
                                                label = { Text("Tara (kg)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        val brutoVal = pesoBrutoStr.toDoubleOrNull() ?: 0.0
                                        val taraVal = taraStr.toDoubleOrNull() ?: 0.0
                                        val netoVal = brutoVal - taraVal

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Peso Neto Calculado: ${if (netoVal > 0) String.format(java.util.Locale.getDefault(), "%.2f kg", netoVal) else "0.00 kg (debe ser > 0)"}",
                                            color = if (netoVal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = observacion,
                                            onValueChange = { observacion = it },
                                            label = { Text("Observación (Opcional)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                viewModel.registrarPesaje(puntoId, material, pesoBrutoStr, taraStr, observacion)
                                                pesoBrutoStr = ""
                                                taraStr = ""
                                                observacion = ""
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Guardar Pesaje (Offline/Online)")
                                        }
                                    }
                                }
                            }
                        }

                        // Lista de Pesajes
                        item {
                            Text(
                                text = "Pesajes Registrados (${pesajesList.size}):",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (pesajesList.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay pesajes registrados para este punto de acopio.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(pesajesList, key = { it.id }) { pesaje ->
                                ItemPesaje(
                                    pesaje = pesaje,
                                    puedeAnular = puedeAnularPesaje,
                                    onAnularClick = { pesajeAAnular = pesaje }
                                )
                            }
                        }
                    }
                }
            }

            if (isOperationLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Dialog para Anular Pesaje
            pesajeAAnular?.let { pesaje ->
                AlertDialog(
                    onDismissRequest = { pesajeAAnular = null },
                    title = { Text("Anular Pesaje #${pesaje.id}") },
                    text = {
                        Column {
                            Text("Ingrese el motivo explícito de anulación:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = motivoAnulacion,
                                onValueChange = { motivoAnulacion = it },
                                label = { Text("Motivo de Anulación") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.anularPesaje(pesaje.id, motivoAnulacion)
                                pesajeAAnular = null
                                motivoAnulacion = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Anular")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pesajeAAnular = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ItemPesaje(
    pesaje: Pesaje,
    puedeAnular: Boolean,
    onAnularClick: () -> Unit
) {
    val esAnulado = pesaje.estado is EstadoPesaje.Anulado

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esAnulado) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${pesaje.id} | ${pesaje.material}",
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    if (pesaje.pendiente) {
                        Badge(containerColor = Color(0.9f, 0.5f, 0f, 1f)) {
                            Text("Pendiente Subida", color = Color.White, modifier = Modifier.padding(4.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Badge(
                        containerColor = if (esAnulado) Color.Red else Color(0F, 0.6f, 0.2f, 1f)
                    ) {
                        Text(
                            text = if (esAnulado) "Anulado" else "Registrado",
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Bruto: ${pesaje.pesoBruto} kg | Tara: ${pesaje.tara} kg | NETO: ${pesaje.pesoNeto} kg", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Operario: ${pesaje.operario} | Hora: ${pesaje.fechaHora}", style = MaterialTheme.typography.bodySmall)

            if (esAnulado) {
                val motivo = (pesaje.estado as EstadoPesaje.Anulado).motivo
                Text(text = "Motivo Anulación: $motivo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // UX-03: Botón Anular adaptativo por privilegio
            if (puedeAnular && !esAnulado) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = onAnularClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Anular Pesaje", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
