package com.curbos.pos.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.curbos.pos.data.local.PosDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import com.curbos.pos.data.model.TransactionItem

class CsvExportManager(
    private val context: Context,
    private val posDao: PosDao
) {

    suspend fun exportDailySales() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Determine Start and End of today
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfDay = calendar.timeInMillis

                // 2. Fetch Transactions
                val transactions = posDao.getTransactionsForDay(startOfDay, endOfDay)

                if (transactions.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No sales to export (Kitchen is quiet!)", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                // 3. Build CSV Content
                val csvHeader = "Transaction ID,Time,Items (JSON),Total Amount,GST Component,Payment Method,Status\n"
                val stringBuilder = StringBuilder(csvHeader)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val jsonInstance = Json { ignoreUnknownKeys = true }

                transactions.forEach { transaction ->
                    val date = dateFormat.format(Date(transaction.timestamp))
                    
                    // Convert structured items list back to JSON for the CSV column
                    // Using explicit serializer for the list to avoid inference issues
                    val itemsRawJson = jsonInstance.encodeToString(ListSerializer(TransactionItem.serializer()), transaction.items)
                    val itemsCsvSafe = itemsRawJson.replace("\"", "\"\"") // Escape quotes in JSON
                    
                    val line = listOf(
                        transaction.id,
                        date,
                        "\"$itemsCsvSafe\"", // Wrap JSON in quotes
                        "%.2f".format(transaction.totalAmount),
                        "%.2f".format(transaction.taxAmount),
                        transaction.paymentMethod,
                        transaction.status
                    ).joinToString(",")
                    stringBuilder.append(line).append("\n")
                }

                // 4. Save to File
                val dateFileName = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fileName = "Sales_Summary_$dateFileName.csv"
                // Save to 'files/reports' or 'cache/reports' mapped in file_paths.xml
                // We used cache-path name="reports" path="." -> which maps to context.cacheDir
                val file = File(context.cacheDir, fileName)
                
                val writer = FileWriter(file)
                writer.append(stringBuilder.toString())
                writer.flush()
                writer.close()
                
                // 5. Share File (Switch to Main thread for UI intent)
                withContext(Dispatchers.Main) {
                    shareCsvFile(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareCsvFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "CurbOS Daily Sales Report")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export Daily Sales CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Sharing Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
