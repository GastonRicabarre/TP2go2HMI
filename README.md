# Go2 HMI - APK con robot simulado

App Android (Kotlin, Jetpack Compose, MVVM, StateFlow, Room) para supervisar y
operar un robot Unitree Go2 **simulado** en rondas de inspección nocturna.
Funciona sin robot, sin servidor y sin internet.

## Cómo compilar
1. Abrir la carpeta con Android Studio (Koala o más nuevo).
2. Esperar el Gradle Sync (necesita internet la primera vez para bajar dependencias).
3. Run en un emulador o celular, o Build > Build APK(s).
   El APK queda en app/build/outputs/apk/debug/.

## Arquitectura
```
 UI (Compose)                 ui/           Pantallas y componentes
   |  observa UiState / manda eventos
   v
 RobotViewModel               viewmodel/    StateFlow<UiState>, guarda eventos en Room
   |  usa la interfaz
   v
 RobotRepository (interfaz)   data/
   |
   +-- SimulatedRobotRepository   simula batería, misiones, alertas, desconexión
   |
 Room (HistoryDao / AppDatabase)  data/    historial de eventos
 Models.kt                    model/        modelos y reglas de habilitación de comandos
```

## Cómo demostrar los 5 escenarios
El panel del simulador se abre con el ícono de llave inglesa (arriba a la derecha de cada pantalla).
1. Misión normal: Misiones > Disponibles > Iniciar (o Control > Iniciar misión). Mirar el progreso en Supervisión hasta 100%.
2. Batería baja: Simulador > Bajar batería a 21% (con misión en curso). Aparece la advertencia; usar Regreso a punto seguro.
3. Desconexión: Simulador > Forzar desconexión. Volver a Supervisión y esperar 5 s (datos antiguos), enviar una orden (queda Sin respuesta), apagar el switch para recuperar.
4. Incidente: con misión en curso, Simulador > Aparece un obstáculo. Reconocer la alerta en Alertas y elegir Reanudar o Regreso.
5. Propio: Simulador > Puerta abierta en el portón (suma al contador y genera alerta).
Extra: "Rechazar todas las órdenes" muestra el estado Rechazada.

## Limitaciones
- Todo es simulado; no usa el SDK de Unitree.
- La detención operativa es software y no reemplaza una parada de emergencia física.
