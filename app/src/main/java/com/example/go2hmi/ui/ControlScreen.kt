package com.example.go2hmi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.*
import com.example.go2hmi.viewmodel.UiState

private fun enabledSubtitle(type: CommandType) = when (type) {
    CommandType.START_MISSION -> "Inicia la ronda"
    CommandType.PAUSE_MISSION -> "Frena en posición actual"
    CommandType.RESUME_MISSION -> "Retoma la ronda"
    CommandType.RETURN_SAFE -> "Retorno a Base de Carga"
    CommandType.CANCEL_MISSION -> "Requiere confirmación"
    CommandType.OPERATIONAL_STOP -> "Parada software inmediata"
    CommandType.ACK_ALERT -> ""
}

private fun disabledSubtitle(type: CommandType, t: Telemetry): String = when (type) {
    CommandType.START_MISSION -> when {
        t.missionState == MissionState.RUNNING || t.missionState == MissionState.PAUSED -> "Deshabilitado (En curso)"
        t.connection != ConnectionState.CONNECTED -> "Sin conexión"
        t.battery < 30 -> "Batería menor a 30%"
        else -> "El robot no está quieto"
    }
    CommandType.PAUSE_MISSION -> "No hay misión en curso"
    CommandType.RESUME_MISSION ->
        if (t.alerts.any { it.level == AlertLevel.CRITICAL && !it.acknowledged }) "Hay alertas críticas sin reconocer"
        else "No está en pausa"
    CommandType.RETURN_SAFE -> "El robot ya está en base"
    CommandType.CANCEL_MISSION -> "No hay misión activa"
    CommandType.OPERATIONAL_STOP -> "Ya está detenido"
    CommandType.ACK_ALERT -> ""
}

@Composable
private fun Tile(type: CommandType, t: Telemetry, modifier: Modifier, onPick: (CommandType) -> Unit) {
    val enabled = isCommandEnabled(type, t)
    val (accent, border, container) = when (type) {
        CommandType.CANCEL_MISSION -> Triple(RG.Amber, RG.Amber, RG.AmberDark)
        CommandType.OPERATIONAL_STOP -> Triple(RG.Red, RG.Red, RG.RedDark)
        CommandType.RETURN_SAFE -> Triple(Color.White, RG.Border, RG.SurfaceAlt)
        else -> Triple(RG.Cyan, RG.Blue, RG.Surface)
    }
    val title = when (type) {
        CommandType.START_MISSION -> "▶ Iniciar Misión"
        CommandType.CANCEL_MISSION -> "✖ Cancelar Misión"
        CommandType.RETURN_SAFE -> "Retornar a punto Seguro"
        CommandType.OPERATIONAL_STOP -> "Detención Op."
        else -> type.label
    }
    CommandTile(
        title = title,
        subtitle = if (enabled) enabledSubtitle(type) else disabledSubtitle(type, t),
        enabled = enabled, accent = accent, borderColor = border, container = container,
        onClick = { onPick(type) }, modifier = modifier
    )
}

@Composable
fun ControlScreen(state: UiState, onCommand: (CommandType) -> Unit, onSimulator: () -> Unit) {
    val t = state.telemetry
    var confirming by remember { mutableStateOf<CommandType?>(null) }
    confirming?.let { type ->
        ConfirmDialog(type, onConfirm = { onCommand(type); confirming = null }, onDismiss = { confirming = null })
    }
    val pick: (CommandType) -> Unit = { type -> if (type in CONFIRM_COMMANDS) confirming = type else onCommand(type) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Control Operativo", "Envío de comandos al robot GO2-001", onSimulator) }
        if (state.isStale) {
            item {
                RGCard(Modifier.fillMaxWidth(), borderColor = RG.Red, container = RG.RedDark, padding = 12.dp) {
                    Text("Sin conexión: las órdenes pueden quedar sin respuesta.", color = RG.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(CommandType.START_MISSION, t, Modifier.weight(1f).fillMaxHeight(), pick)
                Tile(CommandType.PAUSE_MISSION, t, Modifier.weight(1f).fillMaxHeight(), pick)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(CommandType.RESUME_MISSION, t, Modifier.weight(1f).fillMaxHeight(), pick)
                Tile(CommandType.RETURN_SAFE, t, Modifier.weight(1f).fillMaxHeight(), pick)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(CommandType.CANCEL_MISSION, t, Modifier.weight(1f).fillMaxHeight(), pick)
                Tile(CommandType.OPERATIONAL_STOP, t, Modifier.weight(1f).fillMaxHeight(), pick)
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            SectionTitle("Estados de Comandos Recientes")
            Text(
                "Una orden enviada no es una orden ejecutada: solo vale cuando figura como CONFIRMADO.",
                color = RG.TextMuted, fontSize = 12.sp
            )
        }
        if (state.commands.isEmpty()) {
            item { Text("Todavía no se enviaron comandos.", color = RG.TextSecondary, fontSize = 13.sp) }
        }
        items(state.commands.take(8)) { c ->
            val detail = when (c.status) {
                CommandStatus.PENDING -> "En cola de despacho"
                CommandStatus.CONFIRMED -> "Telemetría ACK"
                CommandStatus.REJECTED -> "Rechazo: orden no válida"
                CommandStatus.NO_RESPONSE -> "Sin confirmación del robot"
            }
            RGCard(
                Modifier.fillMaxWidth(),
                borderColor = if (c.status == CommandStatus.PENDING) RG.Blue else RG.Border,
                padding = 14.dp
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CMD-%03d: ${c.type.label}".format(c.id), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${fmt(c.timeMs)} • $detail", color = RG.TextMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(commandStatusLabel(c.status), color = commandStatusColor(c.status), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
