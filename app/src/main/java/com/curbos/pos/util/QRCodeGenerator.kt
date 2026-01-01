package com.curbos.pos.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

object QRCodeGenerator {
    fun generateQRCode(text: String, size: Int = 512): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}
