package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.AcademicApp
import com.example.MainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AcademicViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class SettingsNativeTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.cacheDir, "settings-native").apply { mkdirs() }
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    @Test fun settingsWithRealNavigationAndDialogs() {
        val app = ApplicationProvider.getApplicationContext<AcademicApp>()
        val viewModel = AcademicViewModel(app)
        var dark by mutableStateOf(true)
        var exits = 0
        compose.setContent {
            MyApplicationTheme(darkTheme = dark) {
                MainApp(viewModel, dark, { dark = !dark }, onSignOut = { exits++ },
                    accountEmail = "aluno@example.test")
            }
        }
        compose.onNodeWithTag("nav_item_settings").performClick()
        compose.onNodeWithText("PREFERÊNCIAS").assertIsDisplayed()
        capture("dark")
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("aluno@example.test").assertIsDisplayed()
        capture("profile")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Dados acadêmicos").performScrollTo().performClick()
        compose.onNodeWithText("Dados salvos na sua agenda").assertIsDisplayed()
        capture("data")
        compose.onNodeWithText("Fechar").performClick()
        compose.onNodeWithText("Limpar tudo").performScrollTo().performClick()
        compose.onNodeWithText("Esta ação é permanente e não pode ser desfeita.").assertIsDisplayed()
        capture("clear")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Sair da conta").performScrollTo().performClick()
        assertEquals(1, exits)
        compose.onNodeWithText("Tema escuro").performScrollTo()
        compose.onNode(isToggleable()).performClick().assertIsOff()
        capture("light")
        compose.onNodeWithText("Widgets").performClick()
        compose.onNodeWithContentDescription("Voltar às configurações").assertIsDisplayed().performClick()
        compose.onNodeWithText("Lembretes").performScrollTo().performClick()
        compose.onNodeWithText("Voltar às configurações").assertIsDisplayed().performClick()
        compose.onNodeWithText("PREFERÊNCIAS").assertIsDisplayed()
    }
}
