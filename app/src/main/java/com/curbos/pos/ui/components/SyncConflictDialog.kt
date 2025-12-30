package com.curbos.pos.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun SyncConflictDialog(
    onConfirmSync: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available", color = Color(0xFFC0FF02)) },
        text = { 
            Text("Newer menu data was found in the cloud. Would you like to update your local menu now? This will overwrite local changes.") 
        },
        confirmButton = {
            TextButton(onClick = onConfirmSync) {
                Text("UPDATE NOW", color = Color(0xFFC0FF02))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("LATER", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color(0xFFC0FF02),
        textContentColor = Color.White
    )
}
