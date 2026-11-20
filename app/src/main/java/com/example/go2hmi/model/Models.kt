package com.example.go2hmi.model

enum class ConnectionState { CONNECTED, DISCONNECTED }
enum class OperativeState { IDLE, PATROLLING, PAUSED, RETURNING, STOPPED, INCIDENT }
enum class MissionState { NONE, RUNNING, PAUSED, COMPLETED, CANCELLED }
enum class AlertLevel { INFO, WARNING, CRITICAL }
enum class CommandStatus { PENDING, CONFIRMED, REJECTED, NO_RESPONSE }

enum class CommandType(val label: String) {
    START_MISSION("Iniciar misión"),
    PAUSE_MISSION("Pausar misión"),
    RESUME_MISSION("Reanudar misión"),
    CANCEL_MISSION("Cancelar misión"),
    RETURN_SAFE("Regreso a punto seguro"),
    OPERATIONAL_STOP("Detención operativa"),
    ACK_ALERT("Reconocer alerta")
}

enum class Scenario { LOW_BATTERY, OBSTACLE, OPEN_DOOR }

data class Alert(
    val id: Int,
    val level: AlertLevel,
    val message: String,
    val acknowledged: Boolean = false,
    val timeMs: Long = System.currentTimeMillis()
)

data class Telemetry(
    val connection: ConnectionState = ConnectionState.CONNECTED,
    val battery: Int = 100,
    val operative: OperativeState = OperativeState.IDLE,
    val missionState: MissionState = MissionState.NONE,
    val progress: Int = 0,
    val x: Float = 18f,
    val y: Float = 6f,
    val speed: Float = 0f,
    // Variables propias del servicio (ronda de inspección)
    val checkpointsDone: Int = 0,
    val checkpointsTotal: Int = 10,
    val temperature: Float = 18f,
    val doorsOpen: Int = 0,
    val alerts: List<Alert> = emptyList(),
    val lastUpdateMs: Long = System.currentTimeMillis()
)

data class CommandRecord(
    val id: Long,
    val type: CommandType,
    val status: CommandStatus,
    val timeMs: Long
)

data class RobotEvent(val type: String, val detail: String)

/**
 * Condiciones de habilitación de cada comando.
 * La usa la UI (para habilitar/deshabilitar botones) y el simulador (para validar).
 */
fun isCommandEnabled(type: CommandType, t: Telemetry): Boolean {
    val connected = t.connection == ConnectionState.CONNECTED
    val active = t.missionState == MissionState.RUNNING || t.missionState == MissionState.PAUSED
    val unackedCritical = t.alerts.any { it.level == AlertLevel.CRITICAL && !it.acknowledged }
    return when (type) {
        CommandType.START_MISSION ->
            connected && !active && t.operative == OperativeState.IDLE && t.battery >= 30
        CommandType.PAUSE_MISSION ->
            t.missionState == MissionState.RUNNING && t.operative == OperativeState.PATROLLING
        CommandType.RESUME_MISSION ->
            t.missionState == MissionState.PAUSED &&
                (t.operative == OperativeState.PAUSED || t.operative == OperativeState.INCIDENT) &&
                !unackedCritical
        CommandType.CANCEL_MISSION -> active
        CommandType.RETURN_SAFE ->
            t.operative == OperativeState.PATROLLING ||
                t.operative == OperativeState.PAUSED ||
                t.operative == OperativeState.INCIDENT
        CommandType.OPERATIONAL_STOP -> t.operative != OperativeState.STOPPED
        CommandType.ACK_ALERT -> t.alerts.any { !it.acknowledged }
    }
}

/** Texto que explica por qué un comando está deshabilitado. */
fun disabledReason(type: CommandType): String = when (type) {
    CommandType.START_MISSION -> "Requiere conexión, robot quieto, sin misión activa y batería ≥ 30%"
    CommandType.PAUSE_MISSION -> "Requiere misión en curso"
    CommandType.RESUME_MISSION -> "Requiere misión pausada y sin alertas críticas pendientes"
    CommandType.CANCEL_MISSION -> "Requiere misión en curso o pausada"
    CommandType.RETURN_SAFE -> "Requiere robot patrullando, pausado o con incidente"
    CommandType.OPERATIONAL_STOP -> "El robot ya está detenido"
    CommandType.ACK_ALERT -> "No hay alertas sin reconocer"
}
