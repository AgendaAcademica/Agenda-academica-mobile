package com.example.data.auth

import com.google.firebase.auth.FirebaseAuthException

internal data class EmailCredentials(val email: String, val password: String)

internal interface EmailAccountService {
    fun signIn(credentials: EmailCredentials, onResult: (Result<Unit>) -> Unit)
    fun createAccount(credentials: EmailCredentials, onResult: (Result<Unit>) -> Unit)
}

/** Authentication is the account directory; private Firestore profiles are not public lookup data. */
internal class EmailAccountAccess(private val service: EmailAccountService) {
    fun continueWithEmail(credentials: EmailCredentials, onResult: (Result<Unit>) -> Unit) {
        val normalized = credentials.copy(email = credentials.email.trim())
        service.signIn(normalized) { signInResult ->
            val error = signInResult.exceptionOrNull()
            val code = (error as? FirebaseAuthException)?.errorCode
            if (code !in setOf("ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS")) {
                onResult(signInResult)
            } else {
                // With enumeration protection, a missing account and a wrong password share a code.
                // Creation is atomic: a collision must never overwrite an existing account/password.
                service.createAccount(normalized) { creationResult ->
                    val creationCode = (creationResult.exceptionOrNull() as? FirebaseAuthException)?.errorCode
                    onResult(if (creationCode == "ERROR_EMAIL_ALREADY_IN_USE") signInResult else creationResult)
                }
            }
        }
    }
}
