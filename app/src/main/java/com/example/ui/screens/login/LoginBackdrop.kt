package com.example.ui.screens.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun LoginBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val blueLight = Offset(w * .64f, 120.dp.toPx())
        val violetLight = Offset(w * .84f, 52.dp.toPx())
        drawRect(LoginStyle.Background)
        drawRect(Brush.radialGradient(
            listOf(LoginStyle.GlowBlue.copy(alpha = .62f), Color.Transparent),
            center = blueLight, radius = w * .72f))
        drawRect(Brush.radialGradient(
            listOf(LoginStyle.GlowViolet.copy(alpha = .40f), Color.Transparent),
            center = violetLight, radius = w * .48f))

        // Smooth, asymmetric sweep: rounded crown, concave waist, broad lower-right tail.
        val organic = Path().apply {
            moveTo(w * 1.08f, 64.dp.toPx())
            cubicTo(w * .85f, 26.dp.toPx(), w * .61f, 30.dp.toPx(), w * .47f, 91.dp.toPx())
            cubicTo(w * .37f, 135.dp.toPx(), w * .33f, 201.dp.toPx(), w * .48f, 222.dp.toPx())
            cubicTo(w * .57f, 236.dp.toPx(), w * .64f, 220.dp.toPx(), w * .74f, 257.dp.toPx())
            cubicTo(w * .84f, 295.dp.toPx(), w * .92f, 307.dp.toPx(), w * 1.08f, 310.dp.toPx())
            close()
        }
        drawPath(organic, Brush.linearGradient(
            0f to LoginStyle.OrganicTop, .42f to LoginStyle.OrganicMiddle,
            .70f to LoginStyle.OrganicTop, 1f to LoginStyle.OrganicBottom,
            start = Offset(w * .55f, 40.dp.toPx()), end = Offset(w, 310.dp.toPx())))
        drawPath(organic, Brush.verticalGradient(
            0f to LoginStyle.GlowViolet.copy(alpha = .80f),
            .25f to LoginStyle.BlueEnd.copy(alpha = .45f),
            .55f to Color.Transparent, 1f to LoginStyle.BlueStart.copy(alpha = .16f),
            startY = 34.dp.toPx(), endY = 310.dp.toPx()), style = Stroke(.7.dp.toPx()))
        // Blue light falls across the concave edge instead of a uniform outline.
        drawRect(Brush.radialGradient(
            listOf(LoginStyle.BlueStart.copy(alpha = .09f), Color.Transparent),
            center = Offset(w * .54f, 196.dp.toPx()), radius = w * .31f))
    }
}
