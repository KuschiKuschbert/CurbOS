package com.curbos.pos.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object SnackbarManager {
    private val _messages = Channel<SnackbarMessage>()
    val messages = _messages.receiveAsFlow()

    suspend fun showMessage(message: String, type: SnackbarMessageType = SnackbarMessageType.Info) {
        _messages.send(SnackbarMessage(message, type))
    }

    suspend fun showError(message: String) {
        showMessage(message, SnackbarMessageType.Error)
    }

    suspend fun showSuccess(message: String) {
        showMessage(message, SnackbarMessageType.Success)
    }
}

data class SnackbarMessage(
    val text: String,
    val type: SnackbarMessageType
)

enum class SnackbarMessageType {
    Info,
    Success,
    Error
}
