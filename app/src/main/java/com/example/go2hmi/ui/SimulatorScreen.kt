package com.example.go2hmi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.go2hmi.model.ConnectionState
import com.example.go2hmi.model.OperativeState
import com.example.go2hmi.model.Scenario
import com.example.go2hmi.viewmodel.UiState

@Composable
private fun SwitchRow(title: String, caption: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    RGCard(Modifier.fillMaxWidth(), borderColor = if (checked) RG.Amber else RG.Border) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(caption, color = RG.TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = RG.Amber,
                    uncheckedThumbColor = RG.TextMuted, uncheckedTrackColor = RG.Surface,
                    uncheckedBorderColor = RG.Border
                )
            )
        }
    }
}

@Composable
private fun ScenarioCard(number: String, title: String, caption: String, buttonLabel: String?, onClick: () -> Unit) {
    RGCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ESCENARIO $number", color = RG.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(caption, color = RG.TextSecondary, fontSize = 12.sp)
            }
            if (buttonLabel != null) {
                Spacer(Modifier.width(10.dp))
                RGButton(buttonLabel, onClick, style = RGButtonStyle.Outline, height = 40.dp)
            }
        }
    }
}

@Composable
fun SimulatorScreen(
    state: UiState,
    rejectCommands: Boolean,
    onDisconnect: (Boolean) -> Unit,
    onReject: (Boolean) -> Unit,
    onScenario: (Scenario) -> Unit,
    onRecharge: () -> Unit,
    onResetStop: () -> Unit,
    onBack: () -> Unit
) {
    val t = state.telemetry
    // El switch refleja el estado REAL del robot (no se reinicia al cambiar de pestaña)
    val disconnected = t.connection == ConnectionState.DISCONNECTED

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = RG.Cyan)
            }
            Column {
                Text("Panel del Simulador", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Herramientas de prueba • No forma parte del robot", color = RG.TextSecondary, fontSize = 13.sp)
            }
        }

        SectionTitle("CONECTIVIDAD", RG.Cyan)
        SwitchRow("Forzar desconexión", "Congela la telemetría y deja las órdenes sin respuesta", disconnected, onDisconnect)
        SwitchRow("Rechazar todas las órdenes", "El robot rechaza cualquier comando recibido", rejectCommands, onReject)

        SectionTitle("ESCENARIOS DE PRUEBA", RG.Cyan)
        ScenarioCard("1", "Misión normal", "Iniciá la ronda desde Misiones o Control", null) {}
        ScenarioCard("2", "Batería baja", "Baja la batería a 21% (con misión en curso)", "Aplicar") { onScenario(Scenario.LOW_BATTERY) }
        ScenarioCard("3", "Desconexión", "Usá el switch de Conectividad de arriba", null) {}
        ScenarioCard("4", "Incidente", "Aparece un obstáculo (con misión en curso)", "Aplicar") { onScenario(Scenario.OBSTACLE) }
        ScenarioCard("5", "Escenario propio", "Puerta abierta en el portón de carga", "Aplicar") { onScenario(Scenario.OPEN_DOOR) }

        SectionTitle("BATERÍA Y REARME", RG.Cyan)
        RGCard(Modifier.fillMaxWidth()) {
            Text("Batería actual: ${t.battery}%", color = batteryColor(t.battery), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            RGButton("Recargar batería al 100%", onRecharge, Modifier.fillMaxWidth())
        }
        RGCard(Modifier.fillMaxWidth()) {
            Text("Rearme local", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Simula al vigilador yendo al robot después de una detención operativa.", color = RG.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            RGButton(
                "Rearmar tras detención", onResetStop, Modifier.fillMaxWidth(),
                enabled = t.operative == OperativeState.STOPPED, style = RGButtonStyle.Outline
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
