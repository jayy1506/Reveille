package com.example.reveille.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.reveille.ui.theme.GoldRankAccent
import com.example.reveille.ui.theme.SignalRedVibrant
import com.example.reveille.ui.theme.TacticalForestGreen

@Composable
fun SergeantEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Shield / Crest Outline
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.6f)
            cubicTo(w * 0.9f, h * 0.85f, w * 0.5f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.5f, h * 0.98f, w * 0.1f, h * 0.85f, w * 0.1f, h * 0.6f)
            lineTo(w * 0.1f, h * 0.2f)
            close()
        }

        // Fill shield background with tactical forest green & signal red border
        drawPath(shieldPath, color = TacticalForestGreen)
        drawPath(shieldPath, color = SignalRedVibrant, style = Stroke(width = w * 0.05f))

        // Military Chevrons (V-stripes)
        val strokeWidth = w * 0.07f
        for (i in 0..2) {
            val yOffset = h * (0.32f + i * 0.12f)
            val chevronPath = Path().apply {
                moveTo(w * 0.28f, yOffset)
                lineTo(w * 0.5f, yOffset + h * 0.1f)
                lineTo(w * 0.72f, yOffset)
            }
            drawPath(
                path = chevronPath,
                color = GoldRankAccent,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}