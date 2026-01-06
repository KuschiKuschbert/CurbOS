package com.curbos.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curbos.pos.data.prefs.ProfileManager
import com.curbos.pos.ui.theme.DarkBackground
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.theme.SecondaryText
import com.curbos.pos.ui.theme.CurbOSShapes
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: com.curbos.pos.ui.viewmodel.LoginViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    var showUpgradeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loginState) {
        when (loginState) {
            is com.curbos.pos.ui.viewmodel.LoginState.Success -> onLoginSuccess()
            is com.curbos.pos.ui.viewmodel.LoginState.UpgradeRequired -> showUpgradeDialog = true
            else -> {}
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss? Or allow retry */ showUpgradeDialog = false },
            title = { Text(text = "Subscription Required", color = ElectricLime) },
            text = { Text("CurbOS requires a Top Tier (Business) subscription on PrepFlow. Please upgrade to continue.", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://prepflow.org/pricing"))
                    context.startActivity(intent)
                }) {
                    Text("Upgrade Now", color = ElectricLime)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkBackground,
            titleContentColor = ElectricLime,
            textContentColor = Color.White
        )
    }
    
    com.curbos.pos.ui.components.PulsatingBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 400.dp)
            ) {
                Text(
                    text = "CURBSIDE OPERATING SYSTEM",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "AUTHENTICATE",
                    color = ElectricLime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CURBOS SECURE ACCESS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                if (loginState is com.curbos.pos.ui.viewmodel.LoginState.Error) {
                    Text(
                        text = (loginState as com.curbos.pos.ui.viewmodel.LoginState.Error).message,
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = { 
                        viewModel.login(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        disabledContainerColor = Color.DarkGray
                    ),
                    enabled = loginState !is com.curbos.pos.ui.viewmodel.LoginState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CurbOSShapes.medium
                ) {
                    if (loginState is com.curbos.pos.ui.viewmodel.LoginState.Loading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "INITIALIZE SYSTEM",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            // Version Display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "v${com.curbos.pos.BuildConfig.VERSION_NAME}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
