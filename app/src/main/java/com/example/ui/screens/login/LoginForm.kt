package com.example.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AccessHeading() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Person, null, Modifier.size(24.dp), tint = LoginStyle.Accent)
        Spacer(Modifier.width(10.dp))
        Text("Entrar", color = LoginStyle.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun LoginField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    icon: ImageVector, enabled: Boolean, keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions, transformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(label, Modifier.padding(start = 5.dp, bottom = 5.dp), color = LoginStyle.Text,
            fontSize = LoginStyle.BodySize, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = label },
            placeholder = { Text(placeholder, fontSize = LoginStyle.BodySize) },
            textStyle = LocalTextStyle.current.copy(fontSize = LoginStyle.BodySize),
            leadingIcon = { Icon(icon, null, Modifier.size(22.dp)) }, trailingIcon = trailingIcon,
            enabled = enabled, singleLine = true, visualTransformation = transformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            shape = RoundedCornerShape(LoginStyle.FieldRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LoginStyle.Text, unfocusedTextColor = LoginStyle.Text,
                disabledTextColor = LoginStyle.Muted, cursorColor = LoginStyle.Accent,
                focusedBorderColor = LoginStyle.Accent, unfocusedBorderColor = LoginStyle.FieldBorder,
                disabledBorderColor = LoginStyle.Border,
                disabledPlaceholderColor = LoginStyle.Muted.copy(alpha = .8f),
                disabledLeadingIconColor = LoginStyle.Muted.copy(alpha = .8f),
                disabledContainerColor = Color.Transparent,
                focusedLeadingIconColor = LoginStyle.Muted, unfocusedLeadingIconColor = LoginStyle.Muted,
                focusedPlaceholderColor = LoginStyle.Muted, unfocusedPlaceholderColor = LoginStyle.Muted,
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
            ))
    }
}

@Composable
internal fun AccessButton(label: String, isSubmitting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(LoginStyle.ActionRadius)
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = LoginStyle.ActionHeight)
        .background(LoginStyle.ActionGradient, shape)
        .border(1.dp, LoginStyle.Accent.copy(alpha = .65f), shape),
        shape = shape, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            disabledContainerColor = LoginStyle.Background.copy(alpha = .25f),
            contentColor = LoginStyle.Text, disabledContentColor = LoginStyle.Muted)) {
        if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp).semantics { contentDescription = "Acessando sua conta" },
            color = LoginStyle.Text, strokeWidth = 2.dp)
        else {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(22.dp))
        }
    }
}
