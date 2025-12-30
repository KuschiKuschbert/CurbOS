package com.curbos.pos.data.repository

import com.curbos.pos.common.Result

interface AuthRepository {
    suspend fun signIn(email: String, pass: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUserEmail(): String?
}
