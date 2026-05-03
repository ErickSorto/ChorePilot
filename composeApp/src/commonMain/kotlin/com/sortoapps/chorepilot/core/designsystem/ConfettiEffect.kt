package com.sortoapps.chorepilot.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

sealed class ConfettiShape {
    data object Circle : ConfettiShape()
    data object Square : ConfettiShape()
    data class Rectangle(val heightRatio: Float) : ConfettiShape()
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val shape: ConfettiShape,
    var alpha: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    confettiCount: Int = 90,
    durationMillis: Int = 1400,
    burstOriginX: Float? = null,
    burstOriginY: Float = 0f,
    onAnimationEnd: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    val particles = remember(confettiCount, burstOriginX, burstOriginY) {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(confettiCount) {
                add(createRandomParticle(burstOriginX ?: 500f, burstOriginY))
            }
        }
    }

    val animationProgress = progress.value

    LaunchedEffect(confettiCount, durationMillis, burstOriginX, burstOriginY) {
        particles.clear()
        repeat(confettiCount) {
            particles += createRandomParticle(burstOriginX ?: 500f, burstOriginY)
        }

        progress.snapTo(0f)
        val animationJob = launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
        }
        var previousFrameNanos = withFrameNanos { it }
        while (animationJob.isActive) {
            val frameNanos = withFrameNanos { it }
            val frameDelta = ((frameNanos - previousFrameNanos) / 16_666_667f).coerceIn(0.75f, 2.2f)
            previousFrameNanos = frameNanos
            particles.forEach { particle ->
                particle.x += particle.velocityX * frameDelta
                particle.y += particle.velocityY * frameDelta
                particle.velocityY += 0.18f * frameDelta
                particle.alpha = (particle.alpha - 0.012f * frameDelta).coerceAtLeast(0f)
                particle.rotation += particle.rotationSpeed * frameDelta
            }
        }
        animationJob.join()
        onAnimationEnd()
    }

    Canvas(modifier = modifier) {
        if (animationProgress >= 0f) {
            particles.forEach { particle ->
                if (particle.alpha > 0f) {
                    rotate(degrees = particle.rotation, pivot = Offset(particle.x, particle.y)) {
                        drawConfettiParticle(particle)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawConfettiParticle(particle: ConfettiParticle) {
    when (particle.shape) {
        ConfettiShape.Circle -> {
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.width / 2f,
                center = Offset(particle.x, particle.y),
            )
        }

        ConfettiShape.Square -> {
            drawRect(
                color = particle.color.copy(alpha = particle.alpha),
                topLeft = Offset(particle.x, particle.y),
                size = Size(particle.width, particle.width),
            )
        }

        is ConfettiShape.Rectangle -> {
            drawRect(
                color = particle.color.copy(alpha = particle.alpha),
                topLeft = Offset(particle.x, particle.y),
                size = Size(particle.width, particle.height * particle.shape.heightRatio),
            )
        }
    }
}

private fun createRandomParticle(originX: Float, originY: Float): ConfettiParticle {
    val shapes = listOf(
        ConfettiShape.Circle,
        ConfettiShape.Square,
        ConfettiShape.Rectangle(heightRatio = 1.6f),
    )

    val colors = listOf(
        Color(0xFFFF5C7A),
        Color(0xFFFFC247),
        Color(0xFF59CFFF),
        Color(0xFF8B5CFF),
        Color(0xFF33C9A5),
        Color(0xFFFF9FB7),
    )

    val angle = Random.nextDouble(212.0 * PI / 180.0, 328.0 * PI / 180.0)
    val speed = Random.nextDouble(5.4, 12.8)

    return ConfettiParticle(
        x = originX + Random.nextInt(-36, 36),
        y = originY + Random.nextInt(-18, 18),
        width = Random.nextInt(8, 15).toFloat(),
        height = Random.nextInt(8, 15).toFloat(),
        color = colors.random(),
        shape = shapes.random(),
        alpha = 1f,
        velocityX = (speed * cos(angle)).toFloat(),
        velocityY = (speed * sin(angle)).toFloat(),
        rotation = Random.nextInt(0, 360).toFloat(),
        rotationSpeed = Random.nextFloat() * 18f - 9f,
    )
}

@Preview
@Composable
private fun ConfettiEffectPreview() {
    ChorePilotPreviewTheme {
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 160.dp)
                .background(Color(0xFFF7F1EC)),
        ) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize(),
                confettiCount = 45,
                burstOriginX = 96f,
                burstOriginY = 24f,
            )
        }
    }
}
