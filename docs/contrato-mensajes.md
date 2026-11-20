# Contrato de mensajes propuesto (propio del TP)

> Es una propuesta del grupo. NO es el protocolo oficial de Unitree.

## Telemetría (robot -> app, cada 1 segundo)
```json
{
  "type": "telemetry",
  "timestamp": 1731700000000,
  "connection": "CONNECTED",
  "battery": 78,
  "operative": "PATROLLING",
  "mission": { "state": "RUNNING", "progress": 40 },
  "position": { "x": 12.3, "y": 7.1 },
  "speed": 0.8,
  "service": { "checkpointsDone": 4, "checkpointsTotal": 10, "temperature": 18.6, "doorsOpen": 0 },
  "alerts": [
    { "id": 1, "level": "WARNING", "message": "Batería baja (20%)", "acknowledged": false }
  ]
}
```

## Comando (app -> robot)
```json
{ "type": "command", "id": 12, "command": "PAUSE_MISSION", "timestamp": 1731700005000 }
```

## Respuesta (robot -> app)
```json
{ "type": "response", "commandId": 12, "status": "CONFIRMED", "timestamp": 1731700006000 }
```
Valores posibles de status: `CONFIRMED`, `REJECTED`. Si no llega respuesta
antes del tiempo de espera, la app marca la orden como `NO_RESPONSE`.

## Respuesta rechazada
```json
{ "type": "response", "commandId": 13, "status": "REJECTED", "reason": "Misión no está en curso" }
```

## Evento (robot -> app)
```json
{ "type": "event", "event": "MISSION_COMPLETED", "detail": "10 puntos verificados", "timestamp": 1731700100000 }
```
