package com.curbos.pos.data.repository

import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.common.Result

interface MenuRepository {
    suspend fun fetchMenuItems(): Result<List<MenuItem>>
    suspend fun upsertMenuItem(item: MenuItem): Result<Unit>
    suspend fun upsertMenuItem(item: MenuItem): Result<Unit>
    suspend fun deleteMenuItem(id: String): Result<Unit>
    
    // Category Operations (Batch)
    suspend fun renameCategory(oldName: String, newName: String): Result<Unit>
    suspend fun deleteCategory(categoryName: String): Result<Unit>
    
    suspend fun fetchModifiers(): Result<List<ModifierOption>>
    suspend fun upsertModifier(modifier: ModifierOption): Result<Unit>
    suspend fun deleteModifier(id: String): Result<Unit>

    suspend fun subscribeToMenuChanges(onUpdate: () -> Unit)
    
    // Admin / Demo
    suspend fun wipeAllData(): Result<Unit>
    suspend fun seedDefaultData(): Result<Unit>
}
