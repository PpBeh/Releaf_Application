package com.example.releaf.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun LeafLogo(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    colors: List<Color> = listOf(
        Color(0xFF43A047),
        Color(0xFF66BB6A),
        Color(0xFF81C784)
    )
) {
    val transition = rememberInfiniteTransition()

    val scale1 = if (animated) {
        transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, delayMillis = 0),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else 1f

    val scale2 = if (animated) {
        transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, delayMillis = 250),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else 1f

    val scale3 = if (animated) {
        transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, delayMillis = 500),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else 1f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val leafHeight = size.height * 0.38f
        val leafWidth = size.width * 0.18f

        drawLeaf(center, leafWidth, leafHeight, colors[0], scale1, 0f)
        drawLeaf(center, leafWidth, leafHeight, colors[1], scale2, 120f)
        drawLeaf(center, leafWidth, leafHeight, colors[2], scale3, 240f)

        drawCircle(
            color = Color(0xFF2E7D32),
            radius = size.width * 0.045f,
            center = center
        )
    }
}

private fun DrawScope.drawLeaf(
    center: Offset,
    leafWidth: Float,
    leafHeight: Float,
    color: Color,
    scale: Float,
    rotationDegrees: Float
) {
    if (scale <= 0.05f) return
    rotate(degrees = rotationDegrees, pivot = center) {
        val tip = Offset(center.x, center.y - leafHeight * scale)
        val base = Offset(center.x, center.y)
        val leaf = Path().apply {
            moveTo(tip.x, tip.y)
            cubicTo(
                center.x + leafWidth * scale, center.y - leafHeight * 0.3f,
                center.x + leafWidth * scale, center.y + leafHeight * 0.2f,
                base.x, base.y
            )
            cubicTo(
                center.x - leafWidth * scale, center.y + leafHeight * 0.2f,
                center.x - leafWidth * scale, center.y - leafHeight * 0.3f,
                tip.x, tip.y
            )
            close()
        }
        drawPath(leaf, color)
    }
}
