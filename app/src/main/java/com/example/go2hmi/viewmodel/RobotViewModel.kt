package com.example.go2hmi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2hmi.data.AppDatabase
import com.example.go2hmi.data.HistoryEntity
import com.example.go2hmi.data.RobotRepository
import com.example.go2hmi.data.SimulatedRobotRepository
import com.example.go2hmi.model.CommandRecord
import com.example.go2hmi.model.CommandType
import com.example.go2hmi.model.Scenario
import com.example.go2hmi.model.Telemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val telemetry: Telemetry,
    val commands: List<CommandRecord>,
    val history: List<HistoryEntity>,
    val nowMs: Long
) {
    /** Los datos se consideran antiguos si pasaron más de 5 segundos sin actualización. */
    val secondsSinceUpdate: Long get() = (nowMs - telemetry.lastUpdateMs) / 1000
    val isStale: Boolean get() = secondsSinceUpdate > 5
}

class RobotViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).historyDao()

    // El ViewModel solo conoce la interfaz, no el simulador en sí
    private val repository: RobotRepository = SimulatedRobotRepository(viewModelScope)

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val uiState: StateFlow<UiState> = combine(
        repository.telemetry, repository.commands, dao.getAll(), ticker
    ) { t, c, h, now -> UiState(t, c, h, now) }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            UiState(Telemetry(), emptyList(), emptyList(), System.currentTimeMillis())
        )

    init {
        // Cada evento del robot se guarda en Room
        viewModelScope.launch {
            repository.events.collect { e ->
                dao.insert(HistoryEntity(timeMs = System.currentTimeMillis(), type = e.type, detail = e.detail))
            }
        }
    }

    // Eventos que envía la UI
    fun onCommand(type: CommandType) {
        viewModelScope.launch { repository.sendCommand(type) }
    }
    fun onSetDisconnected(value: Boolean) = repository.setDisconnected(value)
    fun onSetRejectCommands(value: Boolean) = repository.setRejectCommands(value)
    fun onScenario(s: Scenario) = repository.triggerScenario(s)
    fun onResetStop() = repository.resetStop()
    fun onRechargeFull() = repository.rechargeFull()
    fun onClearHistory() {
        viewModelScope.launch { dao.clear() }
    }
}
