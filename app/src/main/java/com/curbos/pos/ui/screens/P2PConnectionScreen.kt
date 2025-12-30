package com.curbos.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.curbos.pos.data.p2p.P2PConnectivityManager

@Composable
fun P2PConnectionScreen(
    p2pConnectivityManager: P2PConnectivityManager?,
    onNavigateBack: () -> Unit
) {
    if (p2pConnectivityManager == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("P2P Not Supported")
        }
        return
    }

    val status by p2pConnectivityManager.connectionStatus.collectAsState()
    val connectedEndpoints by p2pConnectivityManager.connectedEndpoints.collectAsState()
    val isHosting = p2pConnectivityManager.isHosting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Offline P2P Connection", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        
        if (connectedEndpoints.isNotEmpty()) {
            Text("Connected Devices: ${connectedEndpoints.size}", style = MaterialTheme.typography.titleMedium)
            connectedEndpoints.forEach { endpoint ->
                Text("• $endpoint")
            }
            Spacer(Modifier.height(24.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Host Button
            Button(
                onClick = { 
                    if (isHosting) p2pConnectivityManager.stopAdvertising()
                    else p2pConnectivityManager.startAdvertising()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHosting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isHosting) "Stop Hosting" else "Start Hosting (POS)")
            }

            // Client Button
            Button(
                onClick = {
                    when {
                         connectedEndpoints.isNotEmpty() -> p2pConnectivityManager.disconnectAll()
                         status.contains("Search") || status.contains("Found") -> p2pConnectivityManager.stopDiscovery()
                         else -> p2pConnectivityManager.startDiscovery()
                    }
                },
                 enabled = !isHosting
            ) {
                Text(
                    when {
                        connectedEndpoints.isNotEmpty() -> "Disconnect"
                        status.contains("Search") -> "Stop Searching"
                        else -> "Connect Display"
                    }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(onClick = { p2pConnectivityManager.disconnectAll() }) {
            Text("Disconnect All")
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}
