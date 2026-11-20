package com.example.go2hmi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Cat(val label: String, val color: Color)

private fun category(type: String): Cat = when {
    type == "ALERT_CRITICAL" -> Cat("INCIDENTE CRÍTICO", RG.Red)
    type.startsWith("ALERT") -> Cat("ADVERTENCIA", RG.Amber)
    type == "DISCONNECTED" -> Cat("PÉRDIDA DE SEÑAL", RG.Amber)
    type == "RECONNECTED" -> Cat("RECONEXIÓN", RG.Green)
    type.startsWith("COMMAND") -> Cat("COMANDO OPERADOR", RG.Cyan)
    type.startsWith("MISSION") || type == "RETURNED" -> Cat("MISIÓN", RG.Green)
    else -> Cat("SISTEMA", RG.TextMuted)
}

private fun subline(type: String): Pair<String, Color> = when (type) {
    "COMMAND_SENT" -> "Estado: Pendiente" to RG.Cyan
    "COMMAND_CONFIRMED" -> "Estado: Confirmado por el robot" to RG.Green
    "COMMAND_REJECTED" -> "Estado: Rechazado" to RG.Red
    "COMMAND_NO_RESPONSE" -> "Estado: Sin respuesta" to RG.Amber
    else -> "" to RG.TextMuted
}

@Composable
fun HistoryScreen(state: UiState, onClear: () -> Unit, onSimulator: () -> Unit) {
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            ScreenHeader("Historial y Auditoría", "Registro cronológico de eventos y misiones", onSimulator)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = RG.Surface, border = BorderStroke(1.dp, RG.Border)) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Turno: $today", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClear) { Text("Borrar Log ✕", color = RG.Cyan, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.history.isEmpty()) Text("Todavía no hay eventos registrados.", color = RG.TextSecondary, fontSize = 14.sp)
        }
        items(state.history) { h ->
            val cat = category(h.type)
            val (sub, subColor) = subline(h.type)
            TimelineItem(cat, fmt(h.timeMs), h.detail, sub, subColor, highlight = h.type == "ALERT_CRITICAL")
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TimelineItem(cat: Cat, time: String, title: String, sub: String, subColor: Color, highlight: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(28.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(RG.Border))
            Box(Modifier.padding(top = 18.dp).size(14.dp).background(cat.color, CircleShape))
        }
        RGCard(
            Modifier.weight(1f).padding(bottom = 10.dp),
            borderColor = if (highlight) cat.color else RG.Border, padding = 12.dp
        ) {
            Text("${cat.label} • $time", color = cat.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, color = subColor, fontSize = 12.sp)
        }
    }
}
