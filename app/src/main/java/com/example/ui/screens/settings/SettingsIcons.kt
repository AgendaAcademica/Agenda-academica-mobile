package com.example.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val AcademicDataIcon: ImageVector = ImageVector.Builder(
    name = "AcademicData", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3f, 6f)
        curveTo(3f, 3.8f, 7f, 2.5f, 12f, 2.5f)
        curveTo(17f, 2.5f, 21f, 3.8f, 21f, 6f)
        curveTo(21f, 8.2f, 17f, 9.5f, 12f, 9.5f)
        curveTo(7f, 9.5f, 3f, 8.2f, 3f, 6f)
        lineTo(3f, 18f)
        curveTo(3f, 20.2f, 7f, 21.5f, 12f, 21.5f)
        curveTo(17f, 21.5f, 21f, 20.2f, 21f, 18f)
        lineTo(21f, 6f)
        moveTo(3f, 12f)
        curveTo(3f, 14.2f, 7f, 15.5f, 12f, 15.5f)
        curveTo(17f, 15.5f, 21f, 14.2f, 21f, 12f)
    }
}.build()
