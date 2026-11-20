package com.example.go2hmi.data

import com.example.go2hmi.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Robot simulado. "s" es el estado REAL del robot.
 * Lo que ve la app (telemetry) solo se actualiza si hay conexión.
 */
class SimulatedRobotRepository(scope: CoroutineScope) : RobotRepository {

    private var s = Telemetry()
    private val _telemetry = MutableStateFlow(s)
    override val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _commands = MutableStateFlow<List<CommandRecord>>(emptyList())
    override val commands: StateFlow<List<CommandRecord>> = _commands.asStateFlow()

    private val _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<RobotEvent> = _events.asSharedFlow()

    private var forcedDisconnect = false
    private var rejectAll = false
    private var tick = 0
    private var returnTicks = 0
    private var nextCommandId = 1L
    private var nextAlertId = 1
    private var warned20 = false
    private var warned10 = false

    init {
        scope.launch {
            while (isActive) {
                delay(1000)
                step()
                publish()
            }
        }
    }

    // ---------- Simulación (se ejecuta cada 1 segundo) ----------
    private fun step() {
        tick++
        when (s.operative) {
            OperativeState.PATROLLING -> {
                val p = (s.progress + 2).coerceAtMost(100)
                val angle = p / 100f * 2f * Math.PI.toFloat()
                s = s.copy(
                    progress = p,
                    checkpointsDone = p * s.checkpointsTotal / 100,
                    x = 10f + 8f * cos(angle),
                    y = 6f + 5f * sin(angle),
                    speed = 0.8f,
                    temperature = 18f + (tick % 5) * 0.3f
                )
                if (tick % 4 == 0) drainBattery(1)
                if (p >= 100) {
                    s = s.copy(operative = OperativeState.IDLE, missionState = MissionState.COMPLETED, speed = 0f)
                    emit("MISSION_COMPLETED", "Misión completada: ${s.checkpointsTotal} puntos verificados")
                }
            }
            OperativeState.RETURNING -> {
                returnTicks--
                if (tick % 4 == 0) drainBattery(1)
                if (returnTicks <= 0) {
                    s = s.copy(operative = OperativeState.IDLE, missionState = MissionState.CANCELLED, speed = 0f)
                    emit("RETURNED", "El robot llegó al punto seguro")
                }
            }
            OperativeState.IDLE -> {
                // En la base se recarga lentamente
                if (tick % 3 == 0 && s.battery < 100) {
                    s = s.copy(battery = s.battery + 1)
                    if (s.battery > 30) { warned20 = false; warned10 = false }
                }
            }
            else -> Unit // PAUSED, STOPPED, INCIDENT: el robot no se mueve
        }
        checkBattery()
    }

    private fun drainBattery(n: Int) {
        s = s.copy(battery = (s.battery - n).coerceAtLeast(0))
    }

    private fun checkBattery() {
        if (s.battery <= 20 && !warned20 && s.operative != OperativeState.IDLE) {
            warned20 = true
            addAlert(AlertLevel.WARNING, "Batería baja (${s.battery}%). Se recomienda regresar al punto seguro")
        }
        if (s.battery <= 10 && !warned10 && s.operative != OperativeState.IDLE) {
            warned10 = true
            addAlert(AlertLevel.CRITICAL, "Batería crítica (${s.battery}%). Regresar de inmediato")
        }
    }

    private fun addAlert(level: AlertLevel, message: String) {
        s = s.copy(alerts = s.alerts + Alert(nextAlertId++, level, message))
        emit("ALERT_${level.name}", message)
    }

    private fun emit(type: String, detail: String) {
        _events.tryEmit(RobotEvent(type, detail))
    }

    /** Publica el estado a la app solo si hay conexión. Si no, queda el último dato (que se verá antiguo). */
    private fun publish() {
        _telemetry.value =
            if (forcedDisconnect) {
                _telemetry.value.copy(connection = ConnectionState.DISCONNECTED)
            } else {
                s.copy(connection = ConnectionState.CONNECTED, lastUpdateMs = System.currentTimeMillis())
            }
    }

    // ---------- Comandos ----------
    override suspend fun sendCommand(type: CommandType) {
        val id = nextCommandId++
        val now = System.currentTimeMillis()
        _commands.value = (listOf(CommandRecord(id, type, CommandStatus.PENDING, now)) + _commands.value).take(20)
        emit("COMMAND_SENT", type.label)

        delay(1000) // tiempo de respuesta simulado

        val status = when {
            forcedDisconnect -> CommandStatus.NO_RESPONSE
            rejectAll || !isCommandEnabled(type, s) -> CommandStatus.REJECTED
            else -> {
                apply(type)
                CommandStatus.CONFIRMED
            }
        }
        _commands.value = _commands.value.map { if (it.id == id) it.copy(status = status) else it }
        emit("COMMAND_" + status.name, type.label)
        publish()
    }

    private fun apply(type: CommandType) {
        when (type) {
            CommandType.START_MISSION -> {
                s = s.copy(
                    operative = OperativeState.PATROLLING, missionState = MissionState.RUNNING,
                    progress = 0, checkpointsDone = 0, doorsOpen = 0
                )
                emit("MISSION_STARTED", "Misión iniciada")
            }
            CommandType.PAUSE_MISSION ->
                s = s.copy(operative = OperativeState.PAUSED, missionState = MissionState.PAUSED, speed = 0f)
            CommandType.RESUME_MISSION ->
                s = s.copy(operative = OperativeState.PATROLLING, missionState = MissionState.RUNNING)
            CommandType.CANCEL_MISSION -> {
                s = s.copy(operative = OperativeState.IDLE, missionState = MissionState.CANCELLED, speed = 0f)
                emit("MISSION_CANCELLED", "Misión cancelada por el operador")
            }
            CommandType.RETURN_SAFE -> {
                returnTicks = 5
                s = s.copy(operative = OperativeState.RETURNING, speed = 0.6f)
            }
            CommandType.OPERATIONAL_STOP ->
                s = s.copy(operative = OperativeState.STOPPED, speed = 0f)
            CommandType.ACK_ALERT -> {
                val first = s.alerts.firstOrNull { !it.acknowledged }
                if (first != null) {
                    s = s.copy(alerts = s.alerts.map { if (it.id == first.id) it.copy(acknowledged = true) else it })
                }
            }
        }
    }

    // ---------- Controles del simulador ----------
    override fun setDisconnected(disconnected: Boolean) {
        forcedDisconnect = disconnected
        emit(if (disconnected) "DISCONNECTED" else "RECONNECTED",
            if (disconnected) "Se perdió la conexión con el robot" else "Conexión recuperada")
        publish()
    }

    override fun setRejectCommands(reject: Boolean) {
        rejectAll = reject
    }

    override fun triggerScenario(scenario: Scenario) {
        when (scenario) {
            Scenario.LOW_BATTERY -> {
                s = s.copy(battery = 21)
                warned20 = false
                warned10 = false
            }
            Scenario.OBSTACLE -> {
                if (s.operative == OperativeState.PATROLLING) {
                    s = s.copy(operative = OperativeState.INCIDENT, missionState = MissionState.PAUSED, speed = 0f)
                    addAlert(AlertLevel.CRITICAL, "Obstáculo detectado en el pasillo 3. El robot se detuvo")
                }
            }
            Scenario.OPEN_DOOR -> {
                s = s.copy(doorsOpen = s.doorsOpen + 1)
                addAlert(AlertLevel.WARNING, "Puerta abierta detectada en el portón de carga")
            }
        }
        publish()
    }

    override fun rechargeFull() {
        s = s.copy(battery = 100)
        warned20 = false
        warned10 = false
        emit("BATTERY_RECHARGED", "Batería recargada al 100% (simulador)")
        publish()
    }

    override fun resetStop() {
        if (s.operative == OperativeState.STOPPED) {
            s = s.copy(operative = OperativeState.IDLE, missionState = MissionState.CANCELLED)
            emit("RESET", "Rearme local del robot tras detención")
            publish()
        }
    }
}
