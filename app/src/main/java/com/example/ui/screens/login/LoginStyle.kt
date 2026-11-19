package com.example.ui.screens.login

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object LoginStyle {
    val Background = Color(0xFF080F1B)
    val SurfaceTop = Color(0xFF1D2738)
    val SurfaceBottom = Color(0xFF0D1625)
    val Border = Color(0xFF293650)
    val FieldBorder = Color(0xFF586478)
    val Text = Color(0xFFF5F7FC)
    val Muted = Color(0xFFBBC2D0)
    val Accent = Color(0xFF82BEFF)
    val BlueStart = Color(0xFF2699FF)
    val BlueEnd = Color(0xFF194ADD)
    val Violet = Color(0xFF40306C)
    val ChipBackground = Color(0xFF0C1C2E)
    val GlowBlue = Color(0xFF073E87)
    val GlowViolet = Color(0xFF39247D)
    val OrganicTop = Color(0xFF080F20)
    val OrganicMiddle = Color(0xFF101B42)
    val OrganicBottom = Color(0xFF07316A)
    val GlassTop = Color(0xFF102C43)
    val GlassBottom = Color(0xFF0A1524)
    val Error = Color(0xFFFFC1BE)
    val ActionGradient = Brush.linearGradient(listOf(BlueStart, BlueEnd))
    val PanelGradient = Brush.linearGradient(listOf(SurfaceTop, SurfaceBottom))
    val PagePadding = 18.dp
    val PanelPadding = 20.dp
    val PanelRadius = 26.dp
    val FieldRadius = 12.dp
    val ActionRadius = 24.dp
    val ActionHeight = 48.dp
    val MaxWidth = 440.dp
    val TitleSize = 28.sp
    val BodySize = 14.sp
    val CaptionSize = 11.sp
}
