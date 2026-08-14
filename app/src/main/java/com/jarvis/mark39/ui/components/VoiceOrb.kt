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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.mark39.domain.model.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: VoiceState,
    size: Dp = 180.dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    val coreColor = when (state) {
        VoiceState.LISTENING -> Color(0xFF00E5FF)
        VoiceState.PROCESSING -> Color(0xFFFFAB00)
        VoiceState.SPEAKING -> Color(0xFF76FF03)
        VoiceState.ERROR -> Color(0xFFFF5252)
        VoiceState.IDLE -> Color(0xFF00B8D4)
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
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 2 * if (state == VoiceState.LISTENING || state == VoiceState.SPEAKING) pulse else 1f

        // Outer glow rings
        for (i in 3 downTo 1) {
            drawCircle(
                color = coreColor.copy(alpha = 0.08f * i),
                radius = radius * (1f + i * 0.18f),
                center = center
            )
        }

        // Rotating arc accents
        val arcCount = 6
        for (i in 0 until arcCount) {
            val angle = Math.toRadians((rotation + i * (360.0 / arcCount)))
            val x = center.x + cos(angle).toFloat() * radius * 0.7f
            val y = center.y + sin(angle).toFloat() * radius * 0.7f
            drawCircle(
                color = coreColor.copy(alpha = 0.35f),
                radius = 6f,
                center = Offset(x, y)
            )
        }

        // Core gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor, coreColor.copy(alpha = 0.4f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Inner solid
        drawCircle(
            color = coreColor.copy(alpha = 0.9f),
            radius = radius * 0.35f,
            center = center
        )
    }
}
