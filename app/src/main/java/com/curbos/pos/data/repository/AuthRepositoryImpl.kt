package com.curbos.pos.data.repository

import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.common.Result
import javax.inject.Inject

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override suspend fun signIn(email: String, pass: String): Result<Unit> {
        return try {
            SupabaseManager.client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Sign in failed")
        }
    }

    override suspend fun signOut() {
        SupabaseManager.client.auth.signOut()
    }
    
    override fun getCurrentUserEmail(): String? {
        return SupabaseManager.client.auth.currentUserOrNull()?.email
    }
}
