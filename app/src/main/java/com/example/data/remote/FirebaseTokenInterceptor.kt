package com.example.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class SessionAuthenticationException : IOException("Não foi possível verificar sua sessão. Entre novamente.")

/** Runs on OkHttp's worker thread. Firebase refreshes expired tokens before returning them. */
internal class FirebaseTokenInterceptor(
    private val tokenProvider: () -> String = {
        try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: throw SessionAuthenticationException()
            Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).token
                ?.takeIf { it.isNotBlank() }
                ?: throw SessionAuthenticationException()
        } catch (error: IOException) {
            throw error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("A verificação da sessão foi interrompida.")
        } catch (error: Exception) {
            throw SessionAuthenticationException()
        }
    }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        if (token.isBlank()) throw SessionAuthenticationException()
        return chain.proceed(chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build())
    }
}
