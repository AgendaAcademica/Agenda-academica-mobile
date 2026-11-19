package com.example.data.auth

import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailAccountAccessTest {
    private class Accounts : EmailAccountService {
        val passwords = mutableMapOf("existing@example.test" to "correct-password")
        var signInFailure: String? = null
        var creationFailure: String? = null
        var createCalls = 0
        var signedIn: String? = null

        override fun signIn(credentials: EmailCredentials, onResult: (Result<Unit>) -> Unit) {
            val forcedCode = signInFailure
            if (forcedCode != null) onResult(failure(forcedCode))
            else if (passwords[credentials.email] == credentials.password) {
                signedIn = credentials.email
                onResult(Result.success(Unit))
            } else onResult(failure("ERROR_INVALID_CREDENTIAL"))
        }

        override fun createAccount(credentials: EmailCredentials, onResult: (Result<Unit>) -> Unit) {
            createCalls++
            when {
                creationFailure != null -> onResult(failure(requireNotNull(creationFailure)))
                passwords.containsKey(credentials.email) -> onResult(failure("ERROR_EMAIL_ALREADY_IN_USE"))
                credentials.password.length < 6 -> onResult(failure("ERROR_WEAK_PASSWORD"))
                else -> {
                    passwords[credentials.email] = credentials.password
                    signedIn = credentials.email
                    onResult(Result.success(Unit))
                }
            }
        }

        private fun failure(code: String) = Result.failure<Unit>(FirebaseAuthException(code, code))
    }

    @Test fun `existing email signs in without creating an account`() {
        val accounts = Accounts()
        var result: Result<Unit>? = null
        EmailAccountAccess(accounts).continueWithEmail(EmailCredentials(" existing@example.test ", "correct-password")) { result = it }
        assertTrue(requireNotNull(result).isSuccess)
        assertEquals("existing@example.test", accounts.signedIn)
        assertEquals(0, accounts.createCalls)
    }

    @Test fun `unknown email creates account and signs in`() {
        val accounts = Accounts()
        var result: Result<Unit>? = null
        EmailAccountAccess(accounts).continueWithEmail(EmailCredentials("new@example.test", "new-password")) { result = it }
        assertTrue(requireNotNull(result).isSuccess)
        assertEquals("new@example.test", accounts.signedIn)
        assertEquals("new-password", accounts.passwords["new@example.test"])
    }

    @Test fun `wrong password never changes existing credentials or signs in`() {
        val accounts = Accounts()
        var result: Result<Unit>? = null
        EmailAccountAccess(accounts).continueWithEmail(EmailCredentials("existing@example.test", "wrong-password")) { result = it }
        assertEquals("ERROR_INVALID_CREDENTIAL", (result?.exceptionOrNull() as? FirebaseAuthException)?.errorCode)
        assertNull(accounts.signedIn)
        assertEquals("correct-password", accounts.passwords["existing@example.test"])
        assertEquals(1, accounts.passwords.size)
    }

    @Test fun `network disabled and rate limit failures do not try to create accounts`() {
        for (code in listOf("ERROR_NETWORK_REQUEST_FAILED", "ERROR_USER_DISABLED", "ERROR_TOO_MANY_REQUESTS", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_EMAIL")) {
            val accounts = Accounts().apply { signInFailure = code }
            var result: Result<Unit>? = null
            EmailAccountAccess(accounts).continueWithEmail(EmailCredentials("existing@example.test", "password")) { result = it }
            assertEquals(code, (result?.exceptionOrNull() as? FirebaseAuthException)?.errorCode)
            assertEquals(0, accounts.createCalls)
        }
    }

    @Test fun `new account with weak password returns registration error`() {
        val accounts = Accounts()
        var result: Result<Unit>? = null
        EmailAccountAccess(accounts).continueWithEmail(EmailCredentials("new@example.test", "123")) { result = it }
        assertEquals("ERROR_WEAK_PASSWORD", (result?.exceptionOrNull() as? FirebaseAuthException)?.errorCode)
        assertNull(accounts.signedIn)
    }

    @Test fun `creation failure reaches caller without signing in`() {
        val accounts = Accounts().apply { creationFailure = "ERROR_NETWORK_REQUEST_FAILED" }
        var result: Result<Unit>? = null
        EmailAccountAccess(accounts).continueWithEmail(EmailCredentials("new@example.test", "password")) { result = it }
        assertEquals("ERROR_NETWORK_REQUEST_FAILED", (result?.exceptionOrNull() as? FirebaseAuthException)?.errorCode)
        assertNull(accounts.signedIn)
    }
}
