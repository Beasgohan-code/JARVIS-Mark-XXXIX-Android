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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.mark39.domain.model.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: VoiceState,
    size: Dp = 200.dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val ring by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val coreColor = when (state) {
        VoiceState.LISTENING -> Color(0xFF00E5FF)
        VoiceState.PROCESSING -> Color(0xFFFFB300)
        VoiceState.SPEAKING -> Color(0xFF69F0AE)
        VoiceState.ERROR -> Color(0xFFFF5252)
        VoiceState.IDLE -> Color(0xFF00B0FF)
    }

    Canvas(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseR = this.size.minDimension / 2f
        val active = state == VoiceState.LISTENING || state == VoiceState.SPEAKING || state == VoiceState.PROCESSING
        val r = baseR * if (active) pulse else 1f

        // Outer soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = r * 1.35f
            ),
            radius = r * 1.35f,
            center = center
        )

        // Expanding ring when listening
        if (state == VoiceState.LISTENING) {
            drawCircle(
                color = coreColor.copy(alpha = (1f - ring) * 0.5f),
                radius = r * (0.85f + ring * 0.55f),
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Rotating orbital dots
        val dots = 8
        for (i in 0 until dots) {
            val angle = Math.toRadians((rotation + i * (360.0 / dots)))
            val orbit = r * 0.78f
            val dx = (cos(angle) * orbit).toFloat()
            val dy = (sin(angle) * orbit).toFloat()
            drawCircle(
                color = coreColor.copy(alpha = 0.55f),
                radius = 4.5f,
                center = Offset(center.x + dx, center.y + dy)
            )
        }

        // Core gradient sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    coreColor,
                    coreColor.copy(alpha = 0.75f),
                    Color(0xFF05080F)
                ),
                center = Offset(center.x - r * 0.15f, center.y - r * 0.2f),
                radius = r * 0.72f
            ),
            radius = r * 0.58f,
            center = center
        )

        // Inner ring
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = r * 0.58f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}
