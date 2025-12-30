package com.curbos.pos.domain

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent

class SquarePosManager(private val context: Activity) {

    // Using raw Intents as per Square Point of Sale API documentation
    // https://developer.squareup.com/docs/pos-api/build-mobile-web
    
    fun initiatePayment(amountCents: Int, currencyCode: String = "AUD", note: String? = null) {
        val intent = Intent("com.squareup.pos.action.CHARGE")
        intent.putExtra("com.squareup.pos.REQUEST_CODE", REQUEST_CODE_PAYMENT)
        intent.putExtra("com.squareup.pos.CURRENCY_CODE", currencyCode)
        intent.putExtra("com.squareup.pos.TOTAL_AMOUNT", amountCents)
        intent.putExtra("com.squareup.pos.SDK_VERSION", "v2.0")
        note?.let { intent.putExtra("com.squareup.pos.NOTE", it) }
        intent.putExtra("com.squareup.pos.AUTO_RETURN_TIMEOUT_MS", 3000)

        // Verify that the intent can be resolved
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivityForResult(intent, REQUEST_CODE_PAYMENT)
        } else {
             // If Square Point of Sale is not installed, open the Play Store
             try {
                 val playStoreIntent = Intent(Intent.ACTION_VIEW)
                 playStoreIntent.data = android.net.Uri.parse("market://details?id=com.squareup")
                 context.startActivity(playStoreIntent)
             } catch (e: ActivityNotFoundException) {
                 // Play Store not found
             }
        }
    }

    companion object {
        const val REQUEST_CODE_PAYMENT = 1001
    }
}
