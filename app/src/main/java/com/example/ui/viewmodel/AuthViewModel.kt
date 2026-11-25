package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.auth.AuthState
import com.example.data.auth.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseAuthRepository(application)
    private val _state = MutableStateFlow(repository.initialState)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val authListener: FirebaseAuth.AuthStateListener? = repository.addAuthStateListener {
        _state.value = it
    }

    fun continueWithEmail(email: String, password: String) = submit {
        repository.continueWithEmail(email, password, it)
    }

    fun resetPassword(email: String) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        repository.sendPasswordReset(email) { error ->
            _isSubmitting.value = false
            _message.value = error ?: "Enviamos as instruções de recuperação para seu e-mail."
        }
    }

    fun signOut() = repository.signOut()

    fun clearMessage() {
        _message.value = null
    }

    private fun submit(action: ((String?) -> Unit) -> Unit) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        _message.value = null
        action { error ->
            _isSubmitting.value = false
            _message.value = error
        }
    }

    override fun onCleared() {
        repository.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
