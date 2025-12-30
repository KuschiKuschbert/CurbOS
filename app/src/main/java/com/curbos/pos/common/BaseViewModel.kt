package com.curbos.pos.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {

    fun launchCatching(
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(
            context = CoroutineExceptionHandler { _, throwable ->
                viewModelScope.launch {
                    SnackbarManager.showError(throwable.localizedMessage ?: "An unknown error occurred")
                }
            },
            block = block
        )
    }
}
