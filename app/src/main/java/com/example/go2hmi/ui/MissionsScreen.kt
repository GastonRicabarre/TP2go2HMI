package com.example.go2hmi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.*
import com.example.go2hmi.viewmodel.UiState

private data class MissionInfo(val name: String, val description: String, val minutes: Int)

private val availableMissions = listOf(
    MissionInfo("Ronda Nocturna Completa", "Patrullaje perimetral integral: Sectores A, B, C y muelles", 35),
    MissionInfo("Inspección Sector A (Racks de Alta Densidad)", "Pasillos 1 a 6 • Detección de cajas caídas y focos de calor", 15),
    MissionInfo("Inspección Sector B (Carga y Descarga)", "Rampa de pallets • Verificación de portones y autoelevadores", 20),
    MissionInfo("Verificación de Accesos Perimetrales", "Puertas de emergencia B1, B2, C1 y portón norte", 12)
)

@Composable
fun MissionsScreen(
    state: UiState,
    onCommand: (CommandType) -> Unit,
    onSimulator: () -> Unit,
    onGoSupervision: () -> Unit
) {
    val t = state.telemetry
    var tab by rememberSaveable { mutableStateOf(0) }
    val active = t.missionState == MissionState.RUNNING || t.missionState == MissionState.PAUSED
    val canStart = isCommandEnabled(CommandType.START_MISSION, t)
    val missionEvents = state.history.filter { it.type.startsWith("MISSION_") || it.type == "RETURNED" }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Misiones de Vigilancia", "Gestión y despacho de rondas autónomas", onSimulator) }
        item {
            SegmentedTabs(
                listOf("En Curso (${if (active) 1 else 0})", "Disponibles (${availableMissions.size})", "Historial"),
                tab, { tab = it }
            )
        }

        when (tab) {
            0 -> item {
                if (active) {
                    val elapsed = t.progress * 35 / 100
                    RGCard(Modifier.fillMaxWidth(), borderColor = RG.Blue) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val c = if (t.missionState == MissionState.PAUSED) RG.Amber else RG.Green
                                Box(Modifier.size(8.dp).background(c, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(if (t.missionState == MissionState.PAUSED) "EN PAUSA" else "EN CURSO", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Est: 35 min (${elapsed}m transcurridos)", color = RG.TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Ronda Nocturna Completa", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Patrullaje perimetral integral: Sectores A, B, C y muelles", color = RG.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        RGProgressBar(t.progress / 100f, if (t.missionState == MissionState.PAUSED) RG.Amber else RG.Blue)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Checkpoints: ${t.checkpointsDone} de ${t.checkpointsTotal} validados", color = RG.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            RGButton("Detalles", onGoSupervision, style = RGButtonStyle.Outline, height = 38.dp)
                        }
                    }
                } else {
                    RGCard(Modifier.fillMaxWidth()) {
                        Text("No hay misiones en curso", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Iniciá una ronda desde la pestaña Disponibles.", color = RG.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            1 -> {
                item { SectionTitle("Misiones Preconfiguradas") }
                items(availableMissions) { m ->
                    RGCard(Modifier.fillMaxWidth()) {
                        Text(m.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(m.description, color = RG.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⏱ ${m.minutes} min", color = RG.TextSecondary, fontSize = 13.sp)
                                Text(
                                    if (canStart) "DISPONIBLE" else "NO DISPONIBLE",
                                    color = if (canStart) RG.Green else RG.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            RGButton("Iniciar", { onCommand(CommandType.START_MISSION) }, enabled = canStart, height = 40.dp)
                        }
                    }
                }
                item {
                    Text(
                        "Nota: en esta simulación todas las misiones ejecutan la misma ronda.",
                        color = RG.TextMuted, fontSize = 12.sp
                    )
                }
            }

            else -> {
                if (missionEvents.isEmpty()) {
                    item {
                        RGCard(Modifier.fillMaxWidth()) {
                            Text("Todavía no hay misiones registradas", color = RG.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                items(missionEvents) { e ->
                    RGCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                        Text("${fmt(e.timeMs)}", color = RG.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(e.detail, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
