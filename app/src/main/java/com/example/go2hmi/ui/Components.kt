package com.example.go2hmi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.go2hmi.model.AlertLevel
import com.example.go2hmi.model.CommandStatus
import com.example.go2hmi.model.CommandType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Helpers de formato ----------
private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
fun fmt(ms: Long): String = timeFormat.format(Date(ms))
fun ago(seconds: Long): String = if (seconds < 60) "$seconds s" else "${seconds / 60} min"

fun batteryColor(b: Int): Color = when {
    b >= 50 -> RG.Green
    b > 20 -> RG.Amber
    else -> RG.Red
}

fun alertColor(l: AlertLevel): Color = when (l) {
    AlertLevel.CRITICAL -> RG.Red
    AlertLevel.WARNING -> RG.Amber
    AlertLevel.INFO -> RG.Cyan
}

fun alertLabel(l: AlertLevel): String = when (l) {
    AlertLevel.CRITICAL -> "CRÍTICA"
    AlertLevel.WARNING -> "ADVERTENCIA"
    AlertLevel.INFO -> "INFORMACIÓN"
}

fun commandStatusLabel(s: CommandStatus) = when (s) {
    CommandStatus.PENDING -> "PENDIENTE"
    CommandStatus.CONFIRMED -> "CONFIRMADO"
    CommandStatus.REJECTED -> "RECHAZADO"
    CommandStatus.NO_RESPONSE -> "SIN RESPUESTA"
}

fun commandStatusColor(s: CommandStatus) = when (s) {
    CommandStatus.PENDING -> RG.Cyan
    CommandStatus.CONFIRMED -> RG.Green
    CommandStatus.REJECTED -> RG.Red
    CommandStatus.NO_RESPONSE -> RG.Amber
}

/** Comandos que piden confirmación antes de enviarse (riesgo: orden accidental). */
val CONFIRM_COMMANDS = setOf(CommandType.CANCEL_MISSION, CommandType.OPERATIONAL_STOP, CommandType.RETURN_SAFE)

// ---------- Tarjetas ----------
@Composable
fun RGCard(
    modifier: Modifier = Modifier,
    borderColor: Color = RG.Border,
    container: Color = RG.Surface,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, onSimulator: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = RG.TextSecondary, fontSize = 14.sp)
        }
        if (onSimulator != null) {
            IconButton(onClick = onSimulator) {
                Icon(Icons.Default.Build, contentDescription = "Panel del simulador", tint = RG.Cyan)
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    accent: Color = RG.Green,
    valueColor: Color = accent,
    icon: ImageVector? = null,
    highlight: Boolean = false
) {
    RGCard(modifier, borderColor = if (highlight) accent else RG.Border, padding = 12.dp) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
        }
        Text(label, color = RG.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(caption, color = accent, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = RG.SurfaceAlt, border = BorderStroke(1.dp, RG.Border)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = RG.TextSecondary, fontSize = 10.sp, maxLines = 1)
            Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun KeyValue(label: String, value: String, valueColor: Color = Color.White) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = RG.TextSecondary)) { append("$label ") }
            withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.Bold)) { append(value) }
        },
        fontSize = 13.sp
    )
}

@Composable
fun SectionTitle(text: String, color: Color = Color.White) {
    Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

// ---------- Barra de progreso con "perilla" ----------
@Composable
fun RGProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val cy = size.height / 2f
        val h = 8.dp.toPx()
        val r = 7.dp.toPx()
        drawRoundRect(Color(0xFF1B2538), Offset(0f, cy - h / 2), Size(size.width, h), CornerRadius(h / 2))
        val w = size.width * progress.coerceIn(0f, 1f)
        drawRoundRect(color, Offset(0f, cy - h / 2), Size(w, h), CornerRadius(h / 2))
        drawCircle(color, r, Offset(w.coerceIn(r, size.width - r), cy))
    }
}

// ---------- Dibujo simple del robot ----------
@Composable
fun RobotIllustration(modifier: Modifier = Modifier, accent: Color = RG.Cyan) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = Color(0xFF3B4A63)
        val thin = Stroke(width = 2.dp.toPx())
        // sombra
        drawOval(Color(0x44000000), Offset(w * 0.15f, h * 0.88f), Size(w * 0.7f, h * 0.09f))
        // patas
        listOf(0.27f, 0.40f, 0.60f, 0.72f).forEach { x ->
            drawLine(line, Offset(w * x, h * 0.55f), Offset(w * (x - 0.04f), h * 0.88f), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(accent, 4.dp.toPx(), Offset(w * (x - 0.02f), h * 0.70f), style = thin)
        }
        // cuerpo
        drawRoundRect(Color(0xFF1E2A3F), Offset(w * 0.18f, h * 0.25f), Size(w * 0.58f, h * 0.32f), CornerRadius(12.dp.toPx()))
        drawRoundRect(line, Offset(w * 0.18f, h * 0.25f), Size(w * 0.58f, h * 0.32f), CornerRadius(12.dp.toPx()), style = thin)
        drawRoundRect(RG.Blue, Offset(w * 0.28f, h * 0.34f), Size(w * 0.38f, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
        // cabeza
        drawRoundRect(Color(0xFF16324A), Offset(w * 0.72f, h * 0.14f), Size(w * 0.20f, h * 0.28f), CornerRadius(6.dp.toPx()))
        drawRoundRect(accent, Offset(w * 0.72f, h * 0.14f), Size(w * 0.20f, h * 0.28f), CornerRadius(6.dp.toPx()), style = thin)
        drawCircle(accent, 5.dp.toPx(), Offset(w * 0.80f, h * 0.24f))
    }
}

// ---------- Botones ----------
enum class RGButtonStyle { Primary, Outline, Danger }

@Composable
fun RGButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: RGButtonStyle = RGButtonStyle.Primary,
    height: Dp = 52.dp
) {
    val shape = RoundedCornerShape(14.dp)
    val m = modifier.height(height)
    when (style) {
        RGButtonStyle.Outline -> OutlinedButton(
            onClick = onClick, modifier = m, enabled = enabled, shape = shape,
            border = BorderStroke(1.dp, if (enabled) RG.Blue else RG.Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RG.Cyan, disabledContentColor = RG.TextMuted)
        ) { Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }

        else -> Button(
            onClick = onClick, modifier = m, enabled = enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (style == RGButtonStyle.Danger) RG.Red else RG.Blue,
                contentColor = Color.White,
                disabledContainerColor = RG.Surface,
                disabledContentColor = RG.TextMuted
            )
        ) { Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
    }
}

/** Botón grande de la pantalla de Control, con título y subtítulo. */
@Composable
fun CommandTile(
    title: String,
    subtitle: String,
    enabled: Boolean,
    accent: Color,
    borderColor: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) container else RG.Surface,
        border = BorderStroke(1.dp, if (enabled) borderColor else RG.Border)
    ) {
        Column(Modifier.padding(14.dp).heightIn(min = 60.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = if (enabled) accent else RG.TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = if (enabled) accent.copy(alpha = 0.75f) else RG.TextMuted, fontSize = 12.sp)
        }
    }
}

// ---------- Pestañas y chips ----------
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = RG.Surface, border = BorderStroke(1.dp, RG.Border)) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, text ->
                val sel = i == selected
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) RG.Blue else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text, color = if (sel) Color.White else RG.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun RGChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) color else RG.Surface,
        border = BorderStroke(1.dp, if (selected) color else color.copy(alpha = 0.6f))
    ) {
        Text(
            text,
            color = if (selected) Color.White else color,
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

// ---------- Tarjeta de alerta ----------
@Composable
fun AlertCard(
    levelLabel: String,
    color: Color,
    time: String,
    title: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    RGCard(
        modifier,
        borderColor = if (highlight) color else RG.Border,
        container = if (highlight && color == RG.Red) RG.RedDark else RG.Surface
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.18f)) {
                Text(levelLabel, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Text(time, color = RG.TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        footer()
    }
}

// ---------- Diálogo de confirmación ----------
@Composable
fun ConfirmDialog(type: CommandType, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val isStop = type == CommandType.OPERATIONAL_STOP
    val accent = if (isStop) RG.Red else RG.Amber
    val title = when (type) {
        CommandType.OPERATIONAL_STOP -> "¿Solicitar Detención Operativa?"
        CommandType.CANCEL_MISSION -> "¿Cancelar la misión?"
        CommandType.RETURN_SAFE -> "¿Regresar a punto seguro?"
        else -> "¿Enviar ${type.label}?"
    }
    val body = when (type) {
        CommandType.OPERATIONAL_STOP -> "Se enviará una orden de software al robot GO2-001 para frenar su marcha e inmovilizar actuadores."
        CommandType.CANCEL_MISSION -> "Se enviará la orden de cancelar la misión en curso. El progreso se perderá."
        CommandType.RETURN_SAFE -> "Se enviará la orden de abandonar la ronda y volver a la base de carga."
        else -> "Se enviará la orden al robot GO2-001."
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = RG.Surface, border = BorderStroke(1.5.dp, accent)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isStop) "🛑" else "⚠", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(body, color = RG.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                if (isStop) {
                    Spacer(Modifier.height(14.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = RG.RedDark, border = BorderStroke(1.dp, RG.Red)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("⚠ AVISO CRÍTICO DE SEGURIDAD", color = RG.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Esta acción no reemplaza la parada de emergencia física.", color = RG.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RGButton("Cancelar", onDismiss, Modifier.weight(1f), style = RGButtonStyle.Outline)
                    RGButton(
                        if (isStop) "Confirmar Parada" else "Enviar",
                        onConfirm, Modifier.weight(1f),
                        style = if (isStop) RGButtonStyle.Danger else RGButtonStyle.Primary
                    )
                }
            }
        }
    }
}
