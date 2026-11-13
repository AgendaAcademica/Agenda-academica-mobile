package com.example.data.auth

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object ConfigurationRequired : AuthState
    data class SignedIn(val user: FirebaseUser) : AuthState
}

class FirebaseAuthRepository(context: Context) {
    private val configured = FirebaseApp.getApps(context).isNotEmpty()
    private val auth = if (configured) FirebaseAuth.getInstance() else null
    private val firestore = if (configured) FirebaseFirestore.getInstance() else null

    val initialState: AuthState
        get() = when {
            !configured -> AuthState.ConfigurationRequired
            auth?.currentUser != null -> AuthState.SignedIn(auth.currentUser!!)
            else -> AuthState.SignedOut
        }

    fun addAuthStateListener(listener: (AuthState) -> Unit): FirebaseAuth.AuthStateListener? {
        val currentAuth = auth ?: return null
        return FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) recordLogin(user)
            listener(if (user == null) AuthState.SignedOut else AuthState.SignedIn(user))
        }.also(currentAuth::addAuthStateListener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener?) {
        if (listener != null) auth?.removeAuthStateListener(listener)
    }

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        val currentAuth = auth ?: return onResult(CONFIGURATION_MESSAGE)
        currentAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let(::recordLogin)
                onResult(null)
            }
            .addOnFailureListener { onResult(userMessage(it)) }
    }

    fun register(email: String, password: String, onResult: (String?) -> Unit) {
        val currentAuth = auth ?: return onResult(CONFIGURATION_MESSAGE)
        currentAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let(::createUserRecord)
                onResult(null)
            }
            .addOnFailureListener { onResult(userMessage(it)) }
    }

    fun sendPasswordReset(email: String, onResult: (String?) -> Unit) {
        val currentAuth = auth ?: return onResult(CONFIGURATION_MESSAGE)
        if (email.isBlank()) return onResult("Informe seu e-mail para recuperar a senha.")
        currentAuth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(userMessage(it)) }
    }

    fun signOut() = auth?.signOut()

    private fun createUserRecord(user: FirebaseUser) {
        val data = mapOf(
            "email" to user.email,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastAccessAt" to FieldValue.serverTimestamp(),
            "appVersion" to BuildConfig.VERSION_NAME,
            "platform" to "android"
        )
        firestore?.collection("users")?.document(user.uid)?.set(data, SetOptions.merge())
    }

    private fun recordLogin(user: FirebaseUser) {
        val data = mapOf(
            "email" to user.email,
            "lastAccessAt" to FieldValue.serverTimestamp(),
            "appVersion" to BuildConfig.VERSION_NAME,
            "platform" to "android"
        )
        firestore?.collection("users")?.document(user.uid)?.set(data, SetOptions.merge())
    }

    companion object {
        const val CONFIGURATION_MESSAGE = "O acesso ainda não foi configurado para esta versão do aplicativo."

        internal fun userMessage(error: Exception): String = when ((error as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Informe um endereço de e-mail válido."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" ->
                "E-mail ou senha incorretos."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já possui uma conta."
            "ERROR_WEAK_PASSWORD" -> "Use uma senha com pelo menos 6 caracteres."
            "ERROR_USER_DISABLED" -> "Esta conta está desativada."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Não foi possível conectar. Verifique sua internet e tente novamente."
            "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas foram realizadas. Aguarde um pouco e tente novamente."
            else -> "Não foi possível concluir o acesso. Tente novamente."
        }
    }
}

class FirebaseUsageTracker(context: Context) {
    private val configured = FirebaseApp.getApps(context).isNotEmpty()
    private val auth = if (configured) FirebaseAuth.getInstance() else null
    private val firestore = if (configured) FirebaseFirestore.getInstance() else null

    fun recordDocumentAnalysis(
        fileCount: Int,
        totalBytes: Long,
        durationMs: Long,
        success: Boolean,
        resultCount: Int = 0,
        errorCategory: String? = null
    ) {
        val user = auth?.currentUser ?: return
        val data = mutableMapOf<String, Any?>(
            "createdAt" to FieldValue.serverTimestamp(),
            "appVersion" to BuildConfig.VERSION_NAME,
            "fileCount" to fileCount,
            "totalBytes" to totalBytes,
            "durationMs" to durationMs,
            "success" to success,
            "resultCount" to resultCount
        )
        if (errorCategory != null) data["errorCategory"] = errorCategory
        firestore?.collection("users")?.document(user.uid)
            ?.collection("aiUsage")?.add(data)
    }
}
