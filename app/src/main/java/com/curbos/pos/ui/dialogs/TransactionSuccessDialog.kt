package com.curbos.pos.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SecondaryText

/**
 * Dialog shown after a successful order placement.
 * Displays a QR code for customers to scan and track their order.
 */
@Composable
fun TransactionSuccessDialog(
    transactionId: String,
    webBaseUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CurbOSShapes.xxLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Icon
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Order placed successfully",
                    tint = ElectricLime,
                    modifier = Modifier
                        .size(64.dp)
                        .background(ElectricLime.copy(alpha = 0.2f), CircleShape)
                        .padding(16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Order Placed!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricLime
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Scan to track status",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryText
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // QR Code Generation
                val qrBitmap = remember(transactionId) {
                    generateQrCode("$webBaseUrl/curbos/order/$transactionId")
                }
                
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Order QR Code for tracking",
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, CurbOSShapes.large)
                            .padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CurbOSShapes.medium
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

/**
 * Generates a QR code bitmap from the given content string.
 */
fun generateQrCode(content: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
