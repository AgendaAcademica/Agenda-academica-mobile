package com.example.ui.screens

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.settings.AcademicDataSummary
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-420dpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private var clears = 0
    private var exits = 0
    private var widgets = 0
    private var reminders = 0
    private val dark = mutableStateOf(true)
    private val importing = mutableStateOf(false)
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val preferences get() = context.getSharedPreferences("academic_profile", Context.MODE_PRIVATE)

    @Before fun resetProfile() { preferences.edit().clear().commit() }

    private fun show(clearing: Boolean = false, fontScale: Float = 1f, message: String? = null) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                MyApplicationTheme(darkTheme = dark.value) {
                    SettingsContent(dark.value, "aluno@example.test", AcademicDataSummary(4, 12, 7, 2),
                        clearing = clearing, importing = importing.value, message = message,
                        onToggleTheme = { dark.value = !dark.value },
                        onNavigateToWidget = { widgets++ }, onNavigateToReminders = { reminders++ },
                        onClear = { clears++ }, onSignOut = { exits++ })
                }
            }
        }
    }

    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performClick() }
    private fun capture(name: String) {
        File("build/reports/settings").mkdirs()
        compose.onRoot().captureRoboImage(filePath = "build/reports/settings/$name.png")
    }

    @Test fun `dark and light settings preserve layout and toggle theme`() {
        show()
        capture("dark")
        compose.onNode(isToggleable()).assertIsOn().performClick().assertIsOff()
        assertFalse(dark.value)
        capture("light")
    }

    @Test fun `clear opens consequences and cancel preserves data`() {
        show()
        click("Limpar tudo")
        compose.onNodeWithText("Esta ação é permanente e não pode ser desfeita.").assertIsDisplayed()
        compose.onNodeWithText("Sua conta, seu perfil e suas preferências serão mantidos.").assertIsDisplayed()
        compose.onNode(hasText("Os lembretes agendados serão cancelados.", substring = true)).assertExists()
        assertEquals(0, clears)
        capture("clear-dialog")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Apagar tudo").assertDoesNotExist()
        assertEquals(0, clears)
    }

    @Test fun `clear requires explicit confirmation and fires once`() {
        show()
        click("Limpar tudo")
        assertEquals(0, clears)
        compose.onNodeWithText("Apagar tudo").performClick()
        assertEquals(1, clears)
        compose.onNodeWithText("Apagar tudo").assertDoesNotExist()
    }

    @Test fun `import blocks clearing even when confirmation is already open`() {
        show()
        click("Limpar tudo")
        compose.runOnIdle { importing.value = true }
        compose.onNodeWithText("Apagar tudo").assertIsNotEnabled()
        assertEquals(0, clears)
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Limpar tudo").performScrollTo().assertIsNotEnabled()
    }

    @Test fun `ongoing clearing disables exit and duplicate clearing`() {
        show(clearing = true)
        compose.onNodeWithText("Limpando…").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Sair da conta").performScrollTo().assertIsNotEnabled()
        capture("clearing")
    }

    // Robolectric issue 8460: dialog text fields loop with explicit width/height qualifiers.
    @Config(qualifiers = "w320dp-h640dp-320dpi")
    @Test fun `profile saves trimmed name and cancellation discards edits`() {
        show()
        click("Perfil")
        compose.onNodeWithText("aluno@example.test").assertIsDisplayed()
        capture("profile-dialog")
        compose.onNode(hasSetTextAction()).performTextReplacement("  Ana  ")
        compose.onNodeWithText("Salvar perfil").performClick()
        assertEquals("Ana", preferences.getString("user_name", null))
        click("Perfil")
        compose.onNode(hasSetTextAction()).assertTextContains("Ana")
        compose.onNode(hasSetTextAction()).performTextReplacement("Outra pessoa")
        compose.onNodeWithText("Cancelar").performClick()
        click("Perfil")
        compose.onNode(hasSetTextAction()).assertTextContains("Ana")
    }

    @Config(qualifiers = "w320dp-h640dp-320dpi")
    @Test fun `blank profile cannot save`() {
        show()
        click("Perfil")
        compose.onNode(hasSetTextAction()).performTextReplacement("   ")
        compose.onNodeWithText("Salvar perfil").assertIsNotEnabled()
    }

    @Test fun `academic summary displays separate live counts`() {
        show()
        click("Dados acadêmicos")
        listOf("4", "12", "7", "2").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        capture("data-dialog")
        compose.onNodeWithText("Fechar").performClick()
        compose.onNodeWithText("Dados salvos na sua agenda").assertDoesNotExist()
    }

    @Test fun `widget reminders and sign out call existing actions`() {
        show()
        click("Widgets")
        click("Lembretes")
        click("Sair da conta")
        assertEquals(1, widgets)
        assertEquals(1, reminders)
        assertEquals(1, exits)
        assertEquals(0, clears)
    }

    @Test @Config(qualifiers = "w320dp-h640dp-320dpi")
    fun `narrow display keeps actions reachable`() {
        show()
        capture("narrow-top")
        compose.onNodeWithText("Limpar tudo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sair da conta").performScrollTo().assertIsDisplayed()
        capture("narrow-bottom")
    }

    @Test fun `large font keeps actions and modal controls reachable`() {
        show(fontScale = 1.6f)
        capture("large-font-top")
        click("Limpar tudo")
        compose.onNodeWithText("Apagar tudo").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Sair da conta").performScrollTo().assertIsDisplayed()
        capture("large-font-bottom")
    }

    @Test fun `clearing result is visible`() {
        show(message = "Disciplinas, atividades e documentos removidos.")
        compose.onNodeWithText("Disciplinas, atividades e documentos removidos.").performScrollTo().assertIsDisplayed()
    }
}
