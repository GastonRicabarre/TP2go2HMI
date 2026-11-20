package com.example.go2hmi.data

import com.example.go2hmi.model.CommandRecord
import com.example.go2hmi.model.CommandType
import com.example.go2hmi.model.RobotEvent
import com.example.go2hmi.model.Scenario
import com.example.go2hmi.model.Telemetry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato entre la app y el robot. Hoy lo implementa el simulador;
 * mañana podría implementarlo una clase que hable con un robot real.
 */
interface RobotRepository {
    val telemetry: StateFlow<Telemetry>
    val commands: StateFlow<List<CommandRecord>>
    val events: SharedFlow<RobotEvent>

    suspend fun sendCommand(type: CommandType)

    // Controles del simulador (para las demos)
    fun setDisconnected(disconnected: Boolean)
    fun setRejectCommands(reject: Boolean)
    fun triggerScenario(scenario: Scenario)
    fun resetStop()
    fun rechargeFull()
}
