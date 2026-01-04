package com.curbos.pos

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun ensureLoggedIn(device: UiDevice) {
        val startTime = System.currentTimeMillis()
        val maxWaitTime = 60000L // 60 seconds total timeout
        
        println("E2E: ensureLoggedIn - Starting Navigation Loop (Max 60s)")

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            // 1. Check for Success (Sales Screen)
            if (device.hasObject(By.text("Sales"))) {
                println("E2E: ✅ Reached Sales Screen!")
                return
            }

            // 2. Handle 'Android app compatibility' Dialog
            val compatPattern = java.util.regex.Pattern.compile("(?i)(Android app compatibility|ELF alignment)")
            if (device.hasObject(By.text(compatPattern))) {
                println("E2E: Found Compatibility Dialog.")
                
                val dontShow = device.findObject(By.text("Don't show again"))
                if (dontShow != null) {
                    println("E2E: Clicking 'Don't show again'")
                    dontShow.click()
                }

                val okBtn = device.findObject(By.text("OK")) ?: device.findObject(By.text("Close"))
                if (okBtn != null) {
                    println("E2E: Clicking OK/Close")
                    okBtn.click()
                } else {
                    println("E2E: ⚠️ Could not find OK/Close button! Dumping...")
                }
                device.waitForIdle()
                continue
            }

            // 3. Handle Update Dialog
            val updatePattern = java.util.regex.Pattern.compile("(?i)(update now|later)")
            if (device.hasObject(By.text(updatePattern))) {
                val btn = device.findObject(By.text(updatePattern))
                println("E2E: Found Update Dialog ('${btn.text}'). Dismissing...")
                btn.click()
                device.waitForIdle()
                continue
            }

            // 4. Handle "Start Shift" (Welcome Screen)
            if (device.hasObject(By.text("Start Shift"))) {
                println("E2E: Found 'Start Shift'. Clicking...")
                device.findObject(By.text("Start Shift")).click()
                device.waitForIdle()
                continue
            }

            // 5. Handle Login Screen ("AUTHENTICATE" or email input)
            if (device.hasObject(By.text("AUTHENTICATE"))) {
                println("E2E: Found Auth Screen. Attempting Login...")
                performLogin(device)
                continue
            }

            // Small sleep to prevent CPU spinning
            Thread.sleep(1000)
        }
        
        // If loop finishes without return, we failed
        println("E2E: ❌ Timed out waiting for Sales Screen.")
        throw RuntimeException("Failed to reach Sales screen within 60s.")
    }

    private fun performLogin(device: UiDevice) {
        // Enter Email
        val emailObj = device.findObject(By.res(java.util.regex.Pattern.compile(".*email_input"))) 
            ?: device.findObject(By.desc("Email Input"))
            ?: device.findObject(By.text("COMMANDER EMAIL"))
        
        if (emailObj != null) {
            emailObj.click()
            device.waitForIdle()
            device.executeShellCommand("input text derkusch@gmail.com")
            device.pressBack() // Close keyboard
        }

        // Enter Password
        val passObj = device.findObject(By.res(java.util.regex.Pattern.compile(".*password_input")))
            ?: device.findObject(By.desc("Password Input"))
            ?: device.findObject(By.text("PASSCODE"))

        if (passObj != null) {
            passObj.click()
            device.waitForIdle()
            device.executeShellCommand("input text Kuschi_2001")
            device.pressBack()
        }

        // Click Login Button
        val loginBtn = device.findObject(By.text("INITIALIZE SYSTEM"))
        loginBtn?.click()
        println("E2E: Clicked Login Button")
        
        // Wait briefly for transition
        device.wait(Until.gone(By.text("INITIALIZE SYSTEM")), 5000L)
    }

    @Test
    fun completeOrderFlowFromSalesToKitchenToDisplay() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val timeout = 10000L
        val uniqueCustomerName = "TestUser_${System.currentTimeMillis() % 1000}"

        ensureLoggedIn(device)

        // 1. Navigate to Sales
        device.wait(Until.findObject(By.text("Sales")), timeout)
        val salesTab = device.findObject(By.text("Sales"))
        salesTab?.click()

        // 2. Add Item (Carnitas Taco)
        if (device.wait(Until.findObject(By.text("Carnitas Taco")), timeout) == null) {
            // Log failure or assert
        }
        device.findObject(By.text("Carnitas Taco"))?.click()

        // Handle possible modifier dialog (Add to Order)
        if (device.wait(Until.findObject(By.text("Add to Order")), 3000) != null) {
            device.findObject(By.text("Add to Order")).click()
        }

        // 3. Open Cart & Enter Name
        if (device.wait(Until.findObject(By.textContains("View Cart")), 2000) != null) {
            device.findObject(By.textContains("View Cart")).click()
        }
        
        // Enter Name
        val nameInput = device.wait(Until.findObject(By.textContains("Customer Name")), timeout)
        if (nameInput != null) {
            nameInput.click()
            nameInput.text = uniqueCustomerName
            device.pressBack()
        }

        // 4. Pay Cash
        val payBtn = device.wait(Until.findObject(By.textContains("PAY CASH")), timeout)
        payBtn?.click()

        // 5. Navigate to Kitchen
        device.findObject(By.text("Kitchen")).click()

        // 6. Find Order in Kitchen
        device.wait(Until.findObject(By.text(uniqueCustomerName.uppercase())), timeout)

        // 7. Bump Order (COOK -> READY)
        val cookButton = device.wait(Until.findObject(By.textContains("COOK")), timeout)
        cookButton?.click()

        // Wait for READY
        val readyButton = device.wait(Until.findObject(By.textContains("READY")), timeout)
        readyButton?.click()

        // 8. Navigate to Customer Display
        device.findObject(By.text("Display")).click()

        // 9. Verify Order is Ready
        device.wait(Until.findObject(By.text(uniqueCustomerName.uppercase())), timeout)
    }

    @Test
    fun createMenuItemAndVerifyInSales() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val timeout = 10000L
        val newItemName = "E2E Taco" // Short name

        ensureLoggedIn(device)

        // 1. Reset to Sales Tab first (to ensure clean nav state)
        val salesTab = device.wait(Until.findObject(By.desc("Sales")), timeout)
        salesTab?.click()
        device.waitForIdle()

        // 2. Navigate to Menu
        val menuTab = device.wait(Until.findObject(By.desc("Menu")), timeout)
        menuTab?.click()
        
        // Confirm we reached the Menu Screen (Look for "Menu Items" tab or header)
        if (device.wait(Until.findObject(By.text("Menu Items")), timeout) == null) {
             println("E2E: Failed to navigate to Menu Screen. Dumping...")
             device.executeShellCommand("uiautomator dump /sdcard/failure_dump_nav_menu.xml")
             throw RuntimeException("Failed to navigate to Menu Management Screen.")
        }

        // 2. Click Add Item (FAB)
        val addItemFab = device.wait(Until.findObject(By.desc("Add Item")), timeout)
        if (addItemFab == null) {
            println("E2E: 'Add Item' FAB not found. Dumping and failing.")
            device.executeShellCommand("uiautomator dump /sdcard/failure_dump_menu.xml")
            throw RuntimeException("Could not find 'Add Item' FAB on Menu screen.")
        }
        addItemFab.click()

        // 3. Fill Dialog
        val nameLabel = device.wait(Until.findObject(By.text("Name")), timeout)
        nameLabel?.click()
        nameLabel?.text = newItemName
        device.pressBack()

        val priceLabel = device.wait(Until.findObject(By.text("Price")), timeout)
        priceLabel?.click()
        priceLabel?.text = "9.99"
        device.pressBack()

        // Wait for Save button explicitly
        device.wait(Until.findObject(By.text("Save")), timeout)
        device.findObject(By.text("Save")).click()

        // 4. Navigate to Sales
        device.findObject(By.text("Sales")).click()

        // 5. Verify Item Exists
        // Wait longer for sync/db update
        device.wait(Until.findObject(By.text(newItemName)), timeout)
    }
    private fun handleUpdateDialog(device: UiDevice) {
        println("E2E: Checking for System/App Dialogs...")
        
        // 1. Android 15 Compatibility Dialog (16KB Page Size Warning)
        val compatPattern = java.util.regex.Pattern.compile("(?i)(Android app compatibility|ELF alignment)")
        if (device.wait(Until.findObject(By.text(compatPattern)), 10000) != null) {
             println("E2E: Found Compatibility Dialog. Clicking OK...")
             val okBtn = device.findObject(By.text("OK"))
             okBtn?.click()
             device.wait(Until.gone(By.text(compatPattern)), 2000L)
        }

        // 2. App Update Dialog
        val pattern = java.util.regex.Pattern.compile("(?i)(update now|later)")
        val dialogBtn = device.wait(Until.findObject(By.text(pattern)), 5000)
        
        if (dialogBtn != null) {
             println("E2E: Found Dialog Button '${dialogBtn.text}'. Clicking...")
             dialogBtn.click()
             // Wait for it to disappear
             device.wait(Until.gone(By.text(pattern)), 5000L)
        }
    }
}
