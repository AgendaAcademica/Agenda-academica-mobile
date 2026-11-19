package com.example.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoginHero() {
    BoxWithConstraints {
    val expanded = LocalDensity.current.fontScale > 1.25f || maxWidth < 340.dp
    Column {
        Box(Modifier.fillMaxWidth()) {
            if (!expanded) {
                Image(painterResource(R.drawable.login_academic_illustration), contentDescription = null,
                    modifier = Modifier.matchParentSize().wrapContentSize(Alignment.TopEnd, unbounded = true)
                        .offset(x = 24.dp, y = (-12).dp).size(204.dp))
            }
            Column {
                Box(Modifier.size(56.dp).background(LoginStyle.ActionGradient, RoundedCornerShape(20.dp))
                    .border(1.dp, LoginStyle.BlueStart.copy(alpha = .4f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.School, null, Modifier.size(34.dp), tint = LoginStyle.Text)
                }
                Spacer(Modifier.height(6.dp))
                Text("Agenda", fontSize = LoginStyle.TitleSize, fontWeight = FontWeight.Bold,
                    color = LoginStyle.Text, lineHeight = 31.sp)
                Text("Acadêmica", fontSize = LoginStyle.TitleSize, fontWeight = FontWeight.Bold,
                    color = LoginStyle.Accent, lineHeight = 31.sp)
                Spacer(Modifier.height(4.dp))
                Text("Organize seus estudos,\natividades e prazos em um só lugar.",
                    fontSize = LoginStyle.BodySize, lineHeight = 19.sp, color = LoginStyle.Muted)
            }
        }
        if (expanded) Image(painterResource(R.drawable.login_academic_illustration), null,
            Modifier.fillMaxWidth().height(180.dp).testTag("login-illustration"))
        Spacer(Modifier.height(16.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureChip("Prazos", Icons.Outlined.CalendarMonth, Modifier)
            FeatureChip("Tarefas", Icons.Outlined.CheckBox, Modifier)
            FeatureChip("Progresso", Icons.Outlined.BarChart, Modifier)
        }
    }
    }
}

@Composable
internal fun LoginPrivacyNote() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(22.dp), tint = LoginStyle.Accent)
        Spacer(Modifier.width(14.dp))
        Text("Usamos sua conta somente para identificar participantes do teste e medir o consumo dos recursos do aplicativo.",
            color = LoginStyle.Muted, fontSize = LoginStyle.CaptionSize, lineHeight = 16.sp)
    }
}
