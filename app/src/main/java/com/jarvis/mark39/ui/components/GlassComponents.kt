package com.jarvis.mark39.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.mark39.ui.theme.Glass
import com.jarvis.mark39.ui.theme.LocalJarvisTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val light = LocalJarvisTheme.current.lightMode
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (light) Glass.panelLight else Glass.panelDark)
            .border(1.dp, if (light) Glass.strokeLight else Glass.strokeDark, shape)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infinite = rememberInfiniteTransition(label = "glow")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Box(
        modifier = modifier.drawBehind {
            val cx = size.width * (0.5f + 0.15f * cos(Math.toRadians(phase.toDouble())).toFloat())
            val cy = size.height * (0.35f + 0.1f * sin(Math.toRadians(phase.toDouble())).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.75f),
                    radius = size.minDimension * 0.4f
                ),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.8f, size.height * 0.75f)
            )
        }
    )
}

@Composable
fun GlassBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val light = LocalJarvisTheme.current.lightMode
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (light) Color(0xF2FFFFFF) else Color(0xD9101620))
            .border(1.dp, if (light) Color(0x18000000) else Color(0x28FFFFFF), shape)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) { content() }
}
