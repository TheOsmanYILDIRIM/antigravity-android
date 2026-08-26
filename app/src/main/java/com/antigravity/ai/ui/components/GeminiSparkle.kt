package com.antigravity.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antigravity.ai.ui.theme.*

@Composable
fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx, cy * 0.55f, cx * 0.55f, cy, 0f, cy)
            cubicTo(cx * 0.55f, cy, cx, cy * 1.45f, cx, h)
            cubicTo(cx, cy * 1.45f, cx * 1.45f, cy, w, cy)
            cubicTo(cx * 1.45f, cy, cx, cy * 0.55f, cx, 0f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(GeminiBlue, GeminiPurple, GeminiPink, GeminiAmber)
            )
        )
    }
}
