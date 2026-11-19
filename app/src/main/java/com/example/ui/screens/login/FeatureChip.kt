package com.example.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FeatureChip(label: String, icon: ImageVector, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Row(modifier
        .shadow(10.dp, shape, clip = false,
            ambientColor = LoginStyle.BlueEnd.copy(alpha = .18f),
            spotColor = LoginStyle.BlueStart.copy(alpha = .24f))
        .background(Brush.verticalGradient(
            0f to LoginStyle.GlassTop.copy(alpha = .92f),
            .22f to LoginStyle.ChipBackground.copy(alpha = .82f),
            1f to LoginStyle.GlassBottom.copy(alpha = .88f)), shape)
        .border(1.dp, Brush.linearGradient(listOf(
            LoginStyle.Accent.copy(alpha = .42f),
            LoginStyle.BlueStart.copy(alpha = .10f),
            LoginStyle.Accent.copy(alpha = .22f))), shape)
        .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).drawBehind {
            drawCircle(Brush.radialGradient(
                listOf(LoginStyle.BlueStart.copy(alpha = .25f), Color.Transparent),
                center = center, radius = size.width * .95f), radius = size.width * .95f)
        }, contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = LoginStyle.Accent)
        }
        Spacer(Modifier.width(6.dp))
        Text(label, color = LoginStyle.Text, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
