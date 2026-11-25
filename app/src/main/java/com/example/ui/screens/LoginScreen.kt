package com.example.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.ui.screens.login.*

@Composable
fun LoginScreen(
    isSubmitting: Boolean,
    message: String?,
    configurationRequired: Boolean,
    onContinue: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onClearMessage: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val enabled = !isSubmitting && !configurationRequired
    val canContinue = enabled && email.isNotBlank() && password.isNotEmpty()
    val submit = {
        if (canContinue) {
            keyboard?.hide()
            focusManager.clearFocus()
            onContinue(email.trim(), password)
        }
    }
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val wasLightStatus = controller?.isAppearanceLightStatusBars
        val wasLightNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                wasLightStatus?.let { controller.isAppearanceLightStatusBars = it }
                wasLightNavigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(LoginStyle.Background)) {
        LoginBackdrop()
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = LoginStyle.PagePadding),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = LoginStyle.MaxWidth).fillMaxWidth()) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.padding(horizontal = 10.dp)) { LoginHero() }
                Spacer(Modifier.height(26.dp))
                Column(Modifier.fillMaxWidth()
                    .background(LoginStyle.PanelGradient, RoundedCornerShape(LoginStyle.PanelRadius))
                    .border(1.dp, LoginStyle.Border, RoundedCornerShape(LoginStyle.PanelRadius))
                    .padding(LoginStyle.PanelPadding)) {
                    AccessHeading()
                    Spacer(Modifier.height(24.dp))
                    LoginField("E-mail", email, { email = it; onClearMessage() },
                        "Seu e-mail institucional ou pessoal", Icons.Outlined.Email, enabled,
                        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }))
                    Spacer(Modifier.height(16.dp))
                    LoginField("Senha", password, { password = it; onClearMessage() },
                        "Sua senha", Icons.Outlined.Lock, enabled,
                        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        KeyboardActions(onDone = { submit() }),
                        if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    if (passwordVisible) "Ocultar senha" else "Mostrar senha", tint = LoginStyle.Muted)
                            }
                        })
                    TextButton(onClick = { keyboard?.hide(); onResetPassword(email.trim()) }, enabled = enabled,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = LoginStyle.Accent,
                            disabledContentColor = LoginStyle.Muted.copy(alpha = .8f))) {
                        Text("Esqueci minha senha", fontSize = LoginStyle.BodySize)
                    }
                    if (message != null || configurationRequired) {
                        Text(message ?: "O acesso ainda não foi configurado para esta versão do aplicativo.",
                            Modifier.fillMaxWidth().padding(bottom = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            color = LoginStyle.Error, fontSize = LoginStyle.BodySize)
                    }
                    Spacer(Modifier.height(8.dp))
                    AccessButton("Entrar", isSubmitting, canContinue, submit)
                    Text(
                        "Se você ainda não tem conta, ela será criada automaticamente ao entrar.",
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = LoginStyle.Muted,
                        fontSize = LoginStyle.BodySize,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(22.dp))
                LoginPrivacyNote()
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
