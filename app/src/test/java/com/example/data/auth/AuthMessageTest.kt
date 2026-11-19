package com.example.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthMessageTest {
    @Test fun `network errors explain how to retry`() {
        assertEquals("Não foi possível conectar. Verifique sua internet e tente novamente.",
            FirebaseAuthRepository.userMessage(FirebaseNetworkException("offline")))
    }

    @Test fun `rate limit tells user to wait`() {
        assertEquals("Muitas tentativas foram realizadas. Aguarde um pouco e tente novamente.",
            FirebaseAuthRepository.userMessage(FirebaseTooManyRequestsException("limited")))
    }
}
