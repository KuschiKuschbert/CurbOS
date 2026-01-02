package com.curbos.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyConstants
import com.curbos.pos.data.model.LoyaltyReward
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange

@Composable
fun RewardsHubDialog(
    customer: Customer,
    rewards: List<LoyaltyReward>,
    onDismiss: () -> Unit,
    onRedeem: (LoyaltyReward) -> Unit,
    onApplyBonus: (LoyaltyConstants.BonusAction) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Passport, 1=Rewards, 2=Bonuses

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E) // Dark Gray Background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Header (Fixed) ---
                RewardsHeader(customer, onDismiss)

                // --- Tabs ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = ElectricLime,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricLime
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("PASSPORT & QUESTS") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("REWARDS STORE") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("BONUS ACTIONS") })
                }
                
                // --- Content ---
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> PassportTabContent(customer)
                        1 -> RewardsStoreContent(customer, rewards, onRedeem)
                        2 -> BonusActionsContent(onApplyBonus)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsHeader(customer: Customer, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = (customer.fullName ?: "Valued Member").uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(customer.currentRank, color = ElectricLime, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(12.dp))
                Text("•   ${customer.redeemableMiles.toInt()} Miles Available", color = Color.Gray)
            }
        }
        
        // Streak Badge
        if (customer.streakCount > 1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafetyOrange.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafetyOrange),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("${customer.streakCount} WEEK STREAK", color = SafetyOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("2x Multiplier Active", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, null, tint = Color.Gray)
        }
    }
}

@Composable
fun PassportTabContent(customer: Customer) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // 1. Quests Check
        item {
            SectionTitle("Active Quests")
            if (customer.activeQuests.isEmpty()) {
                Text("No active quests. Check back soon!", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(customer.activeQuests) { quest ->
                        QuestCard(quest)
                    }
                }
            }
        }
        
        // 2. Stamp Cards
        item {
            SectionTitle("Digital Punch Cards")
            if (customer.stampCards.isEmpty()) {
                Text("Make a purchase to start a Punch Card!", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customer.stampCards.forEach { (category, count) ->
                        StampCardRow(category, count)
                    }
                }
            }
        }

        // 3. Region Unlocks (Existing)
        item {
            SectionTitle("Region Unlocks")
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(120.dp) // Fixed height for grid in column
            ) {
                items(LoyaltyConstants.TacoRegion.ALL_REGIONS) { region ->
                    val isUnlocked = customer.unlockedRegions.contains(region)
                    RegionBadge(region, isUnlocked)
                }
            }
        }
    }
}

@Composable
fun QuestCard(quest: com.curbos.pos.data.model.QuestProgress) {
    Card(
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Quest", style = MaterialTheme.typography.labelSmall, color = ElectricLime)
            Text("Quest ID: ${quest.questId.take(4)}...", color = Color.White, fontWeight = FontWeight.Bold) // Placeholder for Title
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { quest.currentValue.toFloat() / 5f }, // Mock target 5
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = ElectricLime,
                trackColor = Color.DarkGray
            )
            Text("${quest.currentValue} / 5", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun StampCardRow(category: String, count: Int) {
    val maxStamps = 10
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(category, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Buy 10, Get 1 Free", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(maxStamps) { index ->
                val isFilled = index < count
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) ElectricLime else Color.DarkGray)
                        .border(1.dp, if (isFilled) ElectricLime else Color.Gray, CircleShape)
                )
            }
        }
    }
}

@Composable
fun RegionBadge(name: String, isUnlocked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            if (isUnlocked) Icons.Default.Public else Icons.Default.Lock,
            null,
            tint = if (isUnlocked) ElectricLime else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(name, style = MaterialTheme.typography.labelSmall, color = if (isUnlocked) Color.White else Color.Gray, maxLines = 1)
    }
}

@Composable
fun RewardsStoreContent(customer: Customer, rewards: List<LoyaltyReward>, onRedeem: (LoyaltyReward) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rewards.sortedBy { it.costMiles }) { reward ->
             val canAfford = customer.redeemableMiles >= reward.costMiles
             com.curbos.pos.ui.screens.RewardItemRow(reward, canAfford) { onRedeem(reward) }
        }
    }
}

@Composable
fun BonusActionsContent(onApplyBonus: (LoyaltyConstants.BonusAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
         com.curbos.pos.ui.screens.BonusActionsList(onApplyBonus)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = ElectricLime, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}
