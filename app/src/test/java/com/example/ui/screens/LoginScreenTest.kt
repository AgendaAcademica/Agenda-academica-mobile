package com.example.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
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
class LoginScreenTest {
    @get:Rule val compose = createComposeRule()
    private val submissions = mutableListOf<Pair<String, String>>()
    private var resetEmail: String? = null

    private fun show(submitting: Boolean = false, configured: Boolean = true, message: String? = null, fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            MyApplicationTheme {
                LoginScreen(submitting, message, !configured,
                    onContinue = { email, password -> submissions.add(email to password) },
                    onResetPassword = { resetEmail = it }, onClearMessage = {})
            }
            }
        }
    }

    private fun fill() {
        compose.onNodeWithContentDescription("E-mail").performTextInput(" student@example.test ")
        compose.onNodeWithContentDescription("Senha").performTextInput("secret-password")
    }

    private fun capture(name: String) {
        File("build/reports/login").mkdirs()
        compose.onRoot().captureRoboImage(filePath = "build/reports/login/$name.png")
    }

    @Test fun `login submits trimmed email and password through unified action`() {
        show()
        fill()
        compose.onAllNodesWithText("Entrar").onLast().performScrollTo().performClick()
        assertEquals(listOf("student@example.test" to "secret-password"), submissions)
    }

    @Test fun `manual registration is not present`() {
        show()
        compose.onNodeWithText("Criar conta").assertDoesNotExist()
    }

    @Test fun `password visibility can be toggled`() {
        show()
        compose.onNodeWithContentDescription("Mostrar senha").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ocultar senha").assertExists()
    }

    @Test fun `reset uses the supplied email`() {
        show()
        compose.onNodeWithContentDescription("E-mail").performTextInput(" student@example.test ")
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().performClick()
        assertEquals("student@example.test", resetEmail)
    }

    @Test fun `empty form cannot submit and has no Google access`() {
        show()
        compose.onAllNodesWithText("Entrar").onLast().assertIsNotEnabled()
        compose.onNodeWithText("Continuar com Google").assertDoesNotExist()
        capture("phone")
    }

    @Test fun `busy state disables controls and shows progress`() {
        show(submitting = true)
        compose.onNodeWithContentDescription("E-mail").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Senha").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Acessando sua conta").assertExists()
        capture("loading")
    }

    @Test fun `configuration message is visible and prevents submission`() {
        show(configured = false)
        compose.onNodeWithText("O acesso ainda não foi configurado para esta versão do aplicativo.").assertExists()
        compose.onAllNodesWithText("Entrar").onLast().assertIsNotEnabled()
        capture("configuration")
    }

    @Test fun `authentication error is displayed`() {
        show(message = "E-mail ou senha incorretos.")
        compose.onNodeWithText("E-mail ou senha incorretos.").assertExists()
        capture("error")
    }

    @Test @Config(qualifiers = "w320dp-h640dp-320dpi")
    fun `narrow screen keeps actions reachable by scrolling`() {
        show()
        val illustration = compose.onNodeWithTag("login-illustration").fetchSemanticsNode().boundsInRoot
        val description = compose.onNode(hasText("Organize seus estudos,", substring = true))
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Illustration must be below the description on narrow screens", illustration.top >= description.bottom)
        capture("narrow-top")
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Entrar").onLast().performScrollTo().assertIsDisplayed()
        capture("narrow")
    }

    @Test fun `large fonts keep the form reachable`() {
        show(fontScale = 1.6f)
        capture("large-font-top")
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Entrar").onLast().performScrollTo().assertIsDisplayed()
        capture("large-font")
    }

    @Test @Config(qualifiers = "w800dp-h1280dp-240dpi")
    fun `tablet preserves centered content`() {
        show()
        capture("tablet")
    }
}
