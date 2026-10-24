package com.discipline.app.data.repository

import android.content.Context
import com.discipline.app.data.remote.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val email: String,
    val isAnonymous: Boolean = false
)

class AuthRepository(
    private val context: Context,
    private val supabaseService: SupabaseService
) {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        checkCurrentAuth()
    }

    fun checkCurrentAuth() {
        if (supabaseService.isUserAuthenticated) {
            _currentUser.value = AuthUser(
                uid = supabaseService.currentUserId ?: "local_user",
                email = supabaseService.currentUserEmail ?: "",
                isAnonymous = false
            )
        } else {
            // Local guest / offline user session
            _currentUser.value = AuthUser(
                uid = "local_user",
                email = "offline_user@discipline.local",
                isAnonymous = true
            )
        }
    }

    suspend fun signUp(email: String, pass: String): Result<AuthUser> {
        val result = supabaseService.signUp(email, pass)
        return if (result.isSuccess) {
            val (uid, userEmail) = result.getOrThrow()
            val user = AuthUser(uid = uid, email = userEmail, isAnonymous = false)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Sign up failed"))
        }
    }

    suspend fun signIn(email: String, pass: String): Result<AuthUser> {
        val result = supabaseService.signIn(email, pass)
        return if (result.isSuccess) {
            val (uid, userEmail) = result.getOrThrow()
            val user = AuthUser(uid = uid, email = userEmail, isAnonymous = false)
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Sign in failed"))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return supabaseService.sendPasswordReset(email)
    }

    suspend fun signOut(): Result<Unit> {
        supabaseService.signOut()
        _currentUser.value = AuthUser(
            uid = "local_user",
            email = "offline_user@discipline.local",
            isAnonymous = true
        )
        return Result.success(Unit)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val result = supabaseService.deleteUserAccount()
        // Do not change the visible auth state if the server-side deletion failed.
        // The user must remain signed in so they can inspect the error and retry.
        if (result.isSuccess) {
            _currentUser.value = AuthUser(
                uid = "local_user",
                email = "offline_user@discipline.local",
                isAnonymous = true
            )
        }
        return result
    }
}
