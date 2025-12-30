package com.curbos.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.ui.theme.ElectricLime

@Composable
fun ModifierSelectionDialog(
    menuItem: MenuItem,
    availableModifiers: List<ModifierOption>,
    onDismiss: () -> Unit,
    onConfirm: (List<ModifierOption>) -> Unit
) {
    var selectedModifiers by remember { mutableStateOf<Set<ModifierOption>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = menuItem.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ElectricLime
                )
                Text(
                    text = "Customize your order",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color.DarkGray)

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(availableModifiers) { modifier ->
                        ModifierOptionRow(
                            modifierOption = modifier,
                            isSelected = selectedModifiers.contains(modifier),
                            onToggle = {
                                selectedModifiers = if (selectedModifiers.contains(modifier)) {
                                    selectedModifiers - modifier
                                } else {
                                    selectedModifiers + modifier
                                }
                            }
                        )
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedModifiers.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime)
                    ) {
                        Text("Add to Cart", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierOptionRow(
    modifierOption: ModifierOption,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toggleable(
                value = isSelected,
                onValueChange = { onToggle() },
                role = Role.Checkbox
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null, // Handled by toggleable
                colors = CheckboxDefaults.colors(
                    checkedColor = ElectricLime,
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = modifierOption.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                val typeLabel = if (modifierOption.type == "REMOVAL") "Remove" else "Add"
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (modifierOption.type == "REMOVAL") Color.Red else Color.Gray
                )
            }
        }

        val priceSign = if (modifierOption.priceDelta >= 0) "+" else ""
        Text(
            text = "$priceSign$${String.format("%.2f", modifierOption.priceDelta)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) ElectricLime else Color.Gray
        )
    }
}
