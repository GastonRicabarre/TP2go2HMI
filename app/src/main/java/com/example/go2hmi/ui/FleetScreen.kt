package com.example.go2hmi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.ConnectionState
import com.example.go2hmi.model.MissionState
import com.example.go2hmi.viewmodel.UiState

@Composable
fun FleetScreen(state: UiState, onEnter: () -> Unit) {
    val t = state.telemetry
    val connected = t.connection == ConnectionState.CONNECTED && !state.isStale
    val statusColor = if (connected) RG.Green else RG.Red
    val totalMin = t.battery * 195 / 100

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            Text("ROBOGUARD", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(28.dp))
            Text("Flota Disponible", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Seleccione la unidad robótica para iniciar supervisión", color = RG.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            RGCard(Modifier.fillMaxWidth(), borderColor = RG.Blue) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(if (connected) "CONECTADO" else "SIN CONEXIÓN", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("UNITREE GO2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${t.battery}% BAT", color = batteryColor(t.battery), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF0C1322), RoundedCornerShape(12.dp))) {
                    RobotIllustration(Modifier.fillMaxSize().padding(12.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Unidad GO2-001", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Depósito Central • Sector B • Pasillo 4", color = RG.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTile("AUTONOMÍA", "${totalMin / 60}h ${totalMin % 60}m", Modifier.weight(1f))
                    InfoTile(
                        "MISIÓN ACTUAL",
                        if (t.missionState == MissionState.RUNNING) "Ronda Noct." else "Sin misión",
                        Modifier.weight(1f), RG.Cyan
                    )
                    InfoTile("ENLACE", if (connected) "-58 dBm" else "0 dBm", Modifier.weight(1f), statusColor)
                }
                Spacer(Modifier.height(10.dp))
                Text("IP: 192.168.10.45 • FW: v1.4.2 • LiDAR 4D 360° Activo", color = RG.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
        RGButton("Ingresar al Panel de Supervisión →", onEnter, Modifier.fillMaxWidth())
    }
}
