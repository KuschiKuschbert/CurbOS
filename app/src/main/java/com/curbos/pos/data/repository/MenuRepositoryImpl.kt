package com.curbos.pos.data.repository

import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.common.Result
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor() : MenuRepository {
    override suspend fun fetchMenuItems(): Result<List<MenuItem>> {
        return SupabaseManager.fetchMenuItems()
    }

    override suspend fun upsertMenuItem(item: MenuItem): Result<Unit> {
        return SupabaseManager.upsertMenuItem(item)
    }

    override suspend fun deleteMenuItem(id: String): Result<Unit> {
        return SupabaseManager.deleteMenuItem(id)
    }

    override suspend fun fetchModifiers(): Result<List<ModifierOption>> {
        return SupabaseManager.fetchModifiers()
    }

    override suspend fun upsertModifier(modifier: ModifierOption): Result<Unit> {
        return SupabaseManager.upsertModifier(modifier)
    }

    override suspend fun deleteModifier(id: String): Result<Unit> {
        return SupabaseManager.deleteModifier(id)
    }

    override suspend fun subscribeToMenuChanges(onUpdate: () -> Unit) {
        SupabaseManager.subscribeToMenuChanges(onUpdate)
    }

    override suspend fun wipeAllData(): Result<Unit> {
        return SupabaseManager.wipeAllData()
    }

    override suspend fun seedDefaultData(): Result<Unit> {
        return SupabaseManager.seedDefaultData()
    }
}
