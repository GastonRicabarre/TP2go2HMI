package com.example.go2hmi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.*
import com.example.go2hmi.viewmodel.UiState

@Composable
fun SupervisionScreen(
    state: UiState,
    onCommand: (CommandType) -> Unit,
    onSimulator: () -> Unit,
    onGoAlerts: () -> Unit
) {
    val offline = state.isStale || state.telemetry.connection == ConnectionState.DISCONNECTED
    if (offline) FailureView(state, onSimulator) else NormalView(state, onCommand, onSimulator, onGoAlerts)
}

private fun operativeSubtitle(o: OperativeState) = when (o) {
    OperativeState.IDLE -> "En base"
    OperativeState.PATROLLING -> "Patrullaje Activo"
    OperativeState.PAUSED -> "Ronda en Pausa"
    OperativeState.RETURNING -> "Regresando a Base"
    OperativeState.STOPPED -> "Detención Operativa"
    OperativeState.INCIDENT -> "Incidente en Curso"
}

private fun missionHeadline(t: Telemetry): Pair<String, Color> = when {
    t.operative == OperativeState.RETURNING -> "Abortada: regresando" to RG.Amber
    t.missionState == MissionState.RUNNING -> "En Ejecución (${t.progress}%)" to RG.Green
    t.missionState == MissionState.PAUSED -> "En Pausa (${t.progress}%)" to RG.Amber
    t.missionState == MissionState.COMPLETED -> "Completada (100%)" to RG.Green
    t.missionState == MissionState.CANCELLED -> "Cancelada (${t.progress}%)" to RG.Amber
    else -> "Sin misión" to RG.TextMuted
}

// ------------------------------------------------------------------
@Composable
private fun NormalView(
    state: UiState,
    onCommand: (CommandType) -> Unit,
    onSimulator: () -> Unit,
    onGoAlerts: () -> Unit
) {
    val t = state.telemetry
    var confirming by remember { mutableStateOf<CommandType?>(null) }
    confirming?.let { type ->
        ConfirmDialog(type, onConfirm = { onCommand(type); confirming = null }, onDismiss = { confirming = null })
    }

    val (headline, headlineColor) = missionHeadline(t)
    val barColor = if (t.missionState == MissionState.PAUSED || t.operative == OperativeState.RETURNING) RG.Amber else RG.Blue
    val elapsed = t.progress * 35 / 100
    val gait = when (t.operative) {
        OperativeState.PATROLLING -> "Trote Normal"
        OperativeState.RETURNING -> "Marcha de regreso"
        else -> "En reposo"
    }
    val (posture, postureColor) = when (t.operative) {
        OperativeState.STOPPED -> "Detenido" to RG.Red
        OperativeState.INCIDENT -> "En alerta" to RG.Amber
        else -> "Estable" to RG.Green
    }
    val pending = t.alerts.lastOrNull { !it.acknowledged }
    val pauseOrResume = if (t.missionState == MissionState.PAUSED) CommandType.RESUME_MISSION else CommandType.PAUSE_MISSION

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Supervisión", "Robot GO2-001 • ${operativeSubtitle(t.operative)}", onSimulator)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusBadge("ONLINE", RG.Green)
            Text("${t.battery}%", color = batteryColor(t.battery), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (t.battery <= 20) Text("⚠ Batería baja", color = batteryColor(t.battery), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        if (t.operative == OperativeState.RETURNING) {
            RGCard(Modifier.fillMaxWidth(), borderColor = RG.Amber, container = RG.AmberDark) {
                Text("REGRESANDO A BASE", color = RG.Amber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("El robot abandonó la ronda y vuelve al punto seguro.", color = RG.TextSecondary, fontSize = 13.sp)
            }
        }

        // Misión actual
        RGCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MISIÓN ACTUAL", color = RG.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(headline, color = headlineColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text("Ronda Nocturna Completa", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Depósito Central • Sector B • Pasillo 4 (CP ${t.checkpointsDone}/${t.checkpointsTotal})",
                color = RG.TextSecondary, fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            RGProgressBar(t.progress / 100f, barColor)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tiempo estimado: $elapsed min / 35 min est.", color = RG.TextMuted, fontSize = 11.sp)
                Text("Última sinc: Hace ${state.secondsSinceUpdate}s", color = RG.TextMuted, fontSize = 11.sp)
            }
        }

        // Cinemática y telemetría
        RGCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
                    RobotIllustration(Modifier.fillMaxWidth().height(90.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(RG.Green, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("UNITREE GO2", color = RG.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Cinemática y Telemetría", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    KeyValue("Velocidad:", "%.1f m/s".format(t.speed))
                    KeyValue("Modo de marcha:", gait, RG.Cyan)
                    KeyValue("Temp. ambiente:", "%.1f °C".format(t.temperature), RG.Green)
                    KeyValue("Ubicación:", "%.1f, %.1f m".format(t.x, t.y))
                    KeyValue("Postura:", posture, postureColor)
                }
            }
        }

        // Variables del servicio
        SectionTitle("Variables de Depósito en Tiempo Real")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "PUNTOS", "${t.checkpointsDone}/${t.checkpointsTotal}", "de control verificados",
                Modifier.weight(1f).fillMaxHeight(), accent = RG.Green, icon = Icons.Default.LocationOn
            )
            MetricCard(
                "TEMPERATURA", "%.1f°C".format(t.temperature), "Ambiente",
                Modifier.weight(1f).fillMaxHeight(), accent = RG.Green, icon = Icons.Default.Thermostat
            )
            MetricCard(
                "PUERTAS", "${t.doorsOpen}", if (t.doorsOpen > 0) "Portón de carga abierto" else "Todas cerradas",
                Modifier.weight(1f).fillMaxHeight(),
                accent = if (t.doorsOpen > 0) RG.Amber else RG.Green,
                highlight = t.doorsOpen > 0, icon = Icons.Default.MeetingRoom
            )
        }

        // Alerta reciente
        SectionTitle("Alerta Reciente en Curso")
        if (pending != null) {
            val c = alertColor(pending.level)
            AlertCard(
                levelLabel = alertLabel(pending.level), color = c, time = fmt(pending.timeMs),
                title = pending.message, highlight = true, modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RGButton("Ver en Alertas →", onGoAlerts, style = RGButtonStyle.Danger, height = 38.dp)
                }
            }
        } else {
            RGCard(Modifier.fillMaxWidth()) {
                Text("Sin alertas pendientes", color = RG.Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("El robot no reportó eventos sin reconocer.", color = RG.TextSecondary, fontSize = 13.sp)
            }
        }

        // Acciones rápidas
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RGButton(
                if (pauseOrResume == CommandType.PAUSE_MISSION) "Pausar Ronda" else "Reanudar Ronda",
                { onCommand(pauseOrResume) }, Modifier.weight(1f),
                enabled = isCommandEnabled(pauseOrResume, t), style = RGButtonStyle.Outline
            )
            RGButton(
                "Detención Op.", { confirming = CommandType.OPERATIONAL_STOP }, Modifier.weight(1f),
                enabled = isCommandEnabled(CommandType.OPERATIONAL_STOP, t), style = RGButtonStyle.Danger
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ------------------------------------------------------------------
@Composable
private fun FailureView(state: UiState, onSimulator: () -> Unit) {
    val t = state.telemetry
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Supervisión • Modo Falla", "Robot GO2-001", onSimulator)

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = RG.RedDark, border = BorderStroke(1.5.dp, RG.Red)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).background(RG.Red, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ROBOT DESCONECTADO (OFFLINE)", color = RG.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Telemetría desactualizada • Último enlace: hace ${ago(state.secondsSinceUpdate)}",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Las órdenes enviadas pueden quedar sin respuesta", color = RG.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        RGCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Última Misión Conocida:", color = RG.TextSecondary, fontSize = 14.sp)
                StatusBadge("DATOS OBSOLETOS", RG.Amber)
            }
            Spacer(Modifier.height(4.dp))
            Text("Ronda Nocturna Completa", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Última ubicación registrada: %.1f, %.1f m".format(t.x, t.y), color = RG.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            RGProgressBar(t.progress / 100f, RG.TextMuted)
            Spacer(Modifier.height(4.dp))
            Text("Último reporte: ${fmt(t.lastUpdateMs)} • Batería informada: ${t.battery}%", color = RG.TextMuted, fontSize = 12.sp)
        }

        Text("Variables en Momento de Desconexión", color = RG.TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("PUNTOS", "${t.checkpointsDone}/${t.checkpointsTotal}", "Sin confirmar",
                Modifier.weight(1f).fillMaxHeight(), accent = RG.Red, valueColor = RG.TextMuted)
            MetricCard("PUERTAS", "${t.doorsOpen}", "Estado incierto",
                Modifier.weight(1f).fillMaxHeight(), accent = RG.Amber, valueColor = RG.TextMuted)
            MetricCard("ENLACE RF", "0 dBm", "Sin señal",
                Modifier.weight(1f).fillMaxHeight(), accent = RG.Red)
        }

        Text("Acciones de Recuperación", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        RGButton(
            "Forzar Reintento de Sincronización",
            { message = "Reintentando sincronización... el robot no responde." },
            Modifier.fillMaxWidth()
        )
        RGButton(
            "Activar Baliza de Búsqueda Acústica",
            { message = "La baliza acústica no está disponible en la simulación." },
            Modifier.fillMaxWidth(), style = RGButtonStyle.Outline
        )
        message?.let { Text(it, color = RG.Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        RGCard(Modifier.fillMaxWidth(), container = Color(0xFF0F1626)) {
            Text("Protocolo de Seguridad Física", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Si el robot no retorna en 10 min, enviar personal de seguridad al Sector B.", color = RG.TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}
