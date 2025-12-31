package com.curbos.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.ui.theme.ElectricLime
import java.util.UUID

@Composable
fun MenuManagementScreen(
    menuItems: List<MenuItem>,
    onSave: (MenuItem) -> Unit,
    onDelete: (MenuItem) -> Unit,
    // Modifiers Support
    modifiers: List<com.curbos.pos.data.model.ModifierOption> = emptyList(),
    onSaveModifier: (com.curbos.pos.data.model.ModifierOption) -> Unit = {},
    onDeleteModifier: (com.curbos.pos.data.model.ModifierOption) -> Unit = {},
    initialTabIndex: Int = 0
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }
    val tabs = listOf("Menu Items", "Modifiers")

    // Item State
    var showItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MenuItem?>(null) }
    
    // Modifier State
    var showModifierDialog by remember { mutableStateOf(false) }
    var editingModifier by remember { mutableStateOf<com.curbos.pos.data.model.ModifierOption?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ElectricLime
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTabIndex == 0) {
                // --- Menu Items Tab ---
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(menuItems) { item ->
                            MenuRow(
                                item = item,
                                onEdit = { 
                                    editingItem = it
                                    showItemDialog = true 
                                },
                                onDelete = { onDelete(it) }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { 
                            editingItem = null
                            showItemDialog = true 
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = ElectricLime
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            } else {
                // --- Modifiers Tab ---
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(modifiers) { modifier ->
                            ModifierRow(
                                modifier = modifier,
                                onEdit = { 
                                    editingModifier = it
                                    showModifierDialog = true 
                                },
                                onDelete = { onDeleteModifier(it) }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { 
                            editingModifier = null
                            showModifierDialog = true 
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = ElectricLime
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Modifier")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showItemDialog) {
        MenuItemDialog(
            item = editingItem,
            onDismiss = { showItemDialog = false },
            onConfirm = { newItem ->
                onSave(newItem)
                showItemDialog = false
            }
        )
    }
    
    if (showModifierDialog) {
        val categories = menuItems.map { it.category }.distinct()
        ModifierOptionDialog(
            modifier = editingModifier,
            categories = categories,
            onDismiss = { showModifierDialog = false },
            onConfirm = { newModifier ->
                onSaveModifier(newModifier)
                showModifierDialog = false
            }
        )
    }
}

@Composable
fun ModifierRow(
    modifier: com.curbos.pos.data.model.ModifierOption,
    onEdit: (com.curbos.pos.data.model.ModifierOption) -> Unit,
    onDelete: (com.curbos.pos.data.model.ModifierOption) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(modifier.name, style = MaterialTheme.typography.titleMedium)
                val sign = if (modifier.priceDelta >= 0) "+" else ""
                Text("$sign$${modifier.priceDelta}", style = MaterialTheme.typography.bodyMedium, color = ElectricLime)
                Text("Type: ${modifier.type}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = { onEdit(modifier) }) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = { onDelete(modifier) }) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun ModifierOptionDialog(
    modifier: com.curbos.pos.data.model.ModifierOption?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (com.curbos.pos.data.model.ModifierOption) -> Unit
) {
    var name by remember { mutableStateOf(modifier?.name ?: "") }
    var price by remember { mutableStateOf(modifier?.priceDelta?.toString() ?: "0.00") }
    var category by remember { mutableStateOf(modifier?.category) } // Null = All
    
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (modifier == null) "Add Modifier" else "Edit Modifier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g., Extra Cheese)") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price Change (e.g., 0.50 or -0.50)") }
                )
                
                // Category Dropdown
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = category ?: "All Categories")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = { 
                                category = null
                                expanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { 
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text("Select 'All Categories' for global items, or a specific category.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Text("Positive for Add-ons, Negative for Removals/Discounts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    val newModifier = modifier?.copy(
                        name = name,
                        priceDelta = priceDouble,
                        category = category
                    ) ?: com.curbos.pos.data.model.ModifierOption(
                        name = name, 
                        priceDelta = priceDouble,
                        type = if (priceDouble < 0) "REMOVAL" else "ADDON",
                        category = category
                    )
                    onConfirm(newModifier)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MenuRow(
    item: MenuItem,
    onEdit: (MenuItem) -> Unit,
    onDelete: (MenuItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text("$${item.price}", style = MaterialTheme.typography.bodyMedium)
                Text(item.category, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { onEdit(item) }) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun MenuItemDialog(
    item: MenuItem?,
    initialCategory: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (MenuItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(item?.category ?: initialCategory ?: "Tacos") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Item" else "Edit Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") }
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    val newItem = item?.copy(
                        name = name,
                        price = priceDouble,
                        category = category
                    ) ?: MenuItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        price = priceDouble,
                        category = category,
                        imageUrl = null
                    )
                    onConfirm(newItem)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
