package com.curbos.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.DarkBackground
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SecondaryText
import com.curbos.pos.ui.theme.SurfaceColor
import com.curbos.pos.ui.viewmodel.SalesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDirectoryScreen(
    viewModel: SalesViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<com.curbos.pos.data.model.Customer?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncAllCustomers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Database", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Share, contentDescription = "Export", tint = ElectricLime)
                    }
                    IconButton(onClick = { viewModel.syncAllCustomers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = ElectricLime)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchAllCustomers(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, phone or zip...", color = SecondaryText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SecondaryText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = CurbOSShapes.large
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Total Customers: ${uiState.allCustomers.size}",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.allCustomers) { customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomer = customer },
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    customer.fullName ?: "Unknown Customer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    customer.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricLime
                                )
                                if (!customer.zipCode.isNullOrEmpty()) {
                                    Text(
                                        "ZIP: ${customer.zipCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${customer.redeemableMiles.toInt()} Miles",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    customer.currentRank,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (selectedCustomer != null) {
        RewardsHubDialog(
            customer = selectedCustomer!!,
            rewards = uiState.loyaltyRewards,
            onDismiss = { selectedCustomer = null },
            onRedeem = { /* View Only in Admin */ },
            onApplyBonus = { /* View Only in Admin */ }
        )
    }
}
