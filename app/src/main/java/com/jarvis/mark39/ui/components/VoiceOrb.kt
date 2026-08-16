package com.jarvis.mark39.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.mark39.domain.model.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: VoiceState,
    size: Dp = 240.dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val rotSlow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotSlow"
    )
    val rotFast by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotFast"
    )
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    val core = when (state) {
        VoiceState.LISTENING -> Color(0xFF00F0FF)
        VoiceState.PROCESSING -> Color(0xFFFFC107)
        VoiceState.SPEAKING -> Color(0xFF69F0AE)
        VoiceState.ERROR -> Color(0xFFFF5252)
        VoiceState.IDLE -> Color(0xFF00D4FF)
    }

    Canvas(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            }
    ) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f

        // Outer soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(core.copy(alpha = 0.18f), Color.Transparent),
                center = c,
                radius = r * 1.05f
            ),
            radius = r * 1.05f,
            center = c
        )

        // Expanding rings when listening/processing
        if (state == VoiceState.LISTENING || state == VoiceState.PROCESSING) {
            for (i in 0..2) {
                val t = ((wave + i / 3f) % 1f)
                drawCircle(
                    color = core.copy(alpha = (1f - t) * 0.35f),
                    radius = r * 0.35f + t * r * 0.55f,
                    center = c,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Orbital ellipses (preview A style)
        rotate(rotSlow, c) {
            drawCircle(
                color = core.copy(alpha = 0.35f),
                radius = r * 0.72f,
                center = c,
                style = Stroke(width = 1.5f)
            )
            // tilted ring simulation via offset arcs points
            for (a in 0 until 360 step 8) {
                val rad = Math.toRadians(a.toDouble())
                val x = c.x + cos(rad).toFloat() * r * 0.78f
                val y = c.y + sin(rad).toFloat() * r * 0.42f
                if (a % 16 == 0) {
                    drawCircle(core.copy(alpha = 0.5f), radius = 2f, center = Offset(x, y))
                }
            }
        }
        rotate(rotFast, c) {
            drawCircle(
                color = core.copy(alpha = 0.25f),
                radius = r * 0.58f,
                center = c,
                style = Stroke(width = 1.2f)
            )
        }

        // Core sphere
        val coreR = r * 0.32f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), core, core.copy(alpha = 0.4f)),
                center = Offset(c.x - coreR * 0.25f, c.y - coreR * 0.25f),
                radius = coreR * 1.4f
            ),
            radius = coreR,
            center = c
        )
        // Inner highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = coreR * 0.28f,
            center = Offset(c.x - coreR * 0.25f, c.y - coreR * 0.28f)
        )
    }
}
