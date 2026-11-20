package com.example.go2hmi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.go2hmi.viewmodel.RobotViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    SUPERVISION("Supervisión", Icons.Default.Home),
    MISSIONS("Misiones", Icons.Default.CheckBox),
    CONTROL("Control", Icons.Default.Gamepad),
    ALERTS("Alertas", Icons.Default.Notifications),
    HISTORY("Historial", Icons.Default.History)
}

@Composable
fun RobotApp(vm: RobotViewModel) {
    // La UI solo observa el estado y manda eventos al ViewModel
    val state by vm.uiState.collectAsStateWithLifecycle()

    var entered by rememberSaveable { mutableStateOf(false) }
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var showSimulator by rememberSaveable { mutableStateOf(false) }
    var rejectCommands by rememberSaveable { mutableStateOf(false) }
    val tab = Tab.values()[tabIndex]

    BackHandler(enabled = showSimulator) { showSimulator = false }

    Scaffold(
        containerColor = RG.Bg,
        bottomBar = {
            if (entered) {
                NavigationBar(containerColor = Color(0xFF0C1220)) {
                    Tab.values().forEach { t ->
                        NavigationBarItem(
                            selected = !showSimulator && tab == t,
                            onClick = { tabIndex = t.ordinal; showSimulator = false },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RG.Cyan, selectedTextColor = RG.Cyan,
                                indicatorColor = Color(0xFF14284A),
                                unselectedIconColor = RG.TextMuted, unselectedTextColor = RG.TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val openSimulator = { showSimulator = true }
            when {
                !entered -> FleetScreen(state) { entered = true }

                showSimulator -> SimulatorScreen(
                    state = state,
                    rejectCommands = rejectCommands,
                    onDisconnect = vm::onSetDisconnected,
                    onReject = { rejectCommands = it; vm.onSetRejectCommands(it) },
                    onScenario = vm::onScenario,
                    onRecharge = vm::onRechargeFull,
                    onResetStop = vm::onResetStop,
                    onBack = { showSimulator = false }
                )

                else -> when (tab) {
                    Tab.SUPERVISION -> SupervisionScreen(state, vm::onCommand, openSimulator) { tabIndex = Tab.ALERTS.ordinal }
                    Tab.MISSIONS -> MissionsScreen(state, vm::onCommand, openSimulator) { tabIndex = Tab.SUPERVISION.ordinal }
                    Tab.CONTROL -> ControlScreen(state, vm::onCommand, openSimulator)
                    Tab.ALERTS -> AlertsScreen(state, vm::onCommand, openSimulator)
                    Tab.HISTORY -> HistoryScreen(state, vm::onClearHistory, openSimulator)
                }
            }
        }
    }
}
