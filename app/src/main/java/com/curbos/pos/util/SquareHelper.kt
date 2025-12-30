package com.curbos.pos.util

import android.content.Intent
import android.net.Uri

object SquareHelper {
    private const val CHARGE_ACTION = "com.squareup.pos.action.CHARGE"
    
    // ID provided by user - TODO: Move to a secure config if possible
    const val SQUARE_APPLICATION_ID = "sq0idp-iNOJeR33afNgE1g3EqHmhw" 

    // Known Square POS packages
    private val SQUARE_PACKAGES = listOf(
        "com.squareup",             // Square Point of Sale
        "com.squareup.subero",      // Square for Restaurants
        "com.squareup.retail"       // Square for Retail
    )

    fun findSquarePackage(context: android.content.Context): String? {
        val packageManager = context.packageManager
        for (pkg in SQUARE_PACKAGES) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                // Continue checking other packages
            }
        }
        return null
    }

    fun createChargeIntent(
        amountCents: Int,
        currencyCode: String = "AUD",
        note: String,
        metadata: String,
        squarePackageName: String // Force caller to provide the resolved package
    ): Intent {
        val intent = Intent(CHARGE_ACTION)
        intent.setPackage(squarePackageName)
        
        intent.putExtra("com.squareup.pos.NOTE", note)
        
        // Correct Tender Types Constants
        val tenderTypes = ArrayList<String>()
        tenderTypes.add("com.squareup.pos.TENDER_CARD")
        tenderTypes.add("com.squareup.pos.TENDER_CASH")
        tenderTypes.add("com.squareup.pos.TENDER_OTHER")
        tenderTypes.add("com.squareup.pos.TENDER_SQUARE_GIFT_CARD")
        tenderTypes.add("com.squareup.pos.TENDER_CARD_ON_FILE")
        intent.putStringArrayListExtra("com.squareup.pos.TENDER_TYPES", tenderTypes)
        
        // Amount logic
        intent.putExtra("com.squareup.pos.TOTAL_AMOUNT", amountCents)
        intent.putExtra("com.squareup.pos.CURRENCY_CODE", currencyCode)
        
        // Metadata to track this transaction back to our system
        intent.putExtra("com.squareup.pos.REQUEST_METADATA", metadata)
        
        // Standard Request Code
        intent.putExtra("com.squareup.pos.REQUEST_CODE", 1337)
        intent.putExtra("com.squareup.pos.API_VERSION", "v2.0")
        intent.putExtra("com.squareup.pos.CLIENT_ID", SQUARE_APPLICATION_ID)


        return intent
    }

    fun openPlayStoreForSquare(context: android.content.Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.squareup"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.squareup"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
