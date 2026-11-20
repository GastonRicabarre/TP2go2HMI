package com.example.go2hmi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.*
import com.example.go2hmi.viewmodel.UiState

@Composable
fun AlertsScreen(state: UiState, onCommand: (CommandType) -> Unit, onSimulator: () -> Unit) {
    val all = state.telemetry.alerts
    var filter by rememberSaveable { mutableStateOf(0) } // 0 todas, 1 críticas, 2 advertencias
    val critical = all.count { it.level == AlertLevel.CRITICAL }
    val warnings = all.count { it.level == AlertLevel.WARNING }
    val shown = all.reversed().filter {
        when (filter) {
            1 -> it.level == AlertLevel.CRITICAL
            2 -> it.level == AlertLevel.WARNING
            else -> true
        }
    }
    // El comando "Reconocer alerta" reconoce la alerta sin reconocer más antigua
    val oldestUnacked = all.firstOrNull { !it.acknowledged }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Alertas e Incidentes", "Supervisión de contingencias y seguridad", onSimulator) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RGChip("Todas (${all.size})", filter == 0, RG.Blue) { filter = 0 }
                RGChip("Críticas ($critical)", filter == 1, RG.Red) { filter = 1 }
                RGChip("Advertencias ($warnings)", filter == 2, RG.Amber) { filter = 2 }
            }
        }
        if (shown.isEmpty()) {
            item {
                RGCard(Modifier.fillMaxWidth()) {
                    Text("No hay alertas para mostrar", color = RG.TextSecondary, fontSize = 14.sp)
                }
            }
        }
        items(shown) { a ->
            val c = alertColor(a.level)
            AlertCard(
                levelLabel = alertLabel(a.level), color = c, time = fmt(a.timeMs),
                title = a.message, highlight = !a.acknowledged && a.level != AlertLevel.INFO,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(10.dp))
                if (a.acknowledged) {
                    Text("Reconocida por Operador", color = RG.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado: Pendiente de Reconocimiento", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        if (a.id == oldestUnacked?.id) {
                            RGButton(
                                "Reconocer ✓", { onCommand(CommandType.ACK_ALERT) },
                                enabled = isCommandEnabled(CommandType.ACK_ALERT, state.telemetry),
                                style = if (a.level == AlertLevel.CRITICAL) RGButtonStyle.Danger else RGButtonStyle.Primary,
                                height = 40.dp
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
