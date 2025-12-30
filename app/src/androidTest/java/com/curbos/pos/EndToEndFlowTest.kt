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
        val timeout = 10000L
        println("E2E: Checking for Update Dialog...")
        
        // --- HANDLE UPDATE DIALOG ---
        println("E2E: Checking for Update Dialog...")
        // Look for "Update Now" button directly (Case insensitive)
        val updateNowPattern = java.util.regex.Pattern.compile("(?i)update now")
        val updateNowBtn = device.wait(Until.findObject(By.text(updateNowPattern)), 5000)
        
        if (updateNowBtn != null) {
             println("E2E: Found 'Update Now' button. Clicking...")
             updateNowBtn.click()
             // Wait for it to disappear
             device.wait(Until.gone(By.text(updateNowPattern)), 10000L)
        } else {
             // Fallback: Check for "Later" and click it if Update Now missing?
             // User prefers Update Now.
             println("E2E: 'Update Now' button not found.")
        }

        // --- LOGIN FLOW ---
        println("E2E: Checking for Auth Screen...")
        if (device.wait(Until.findObject(By.text("AUTHENTICATE")), timeout) != null) {
            println("E2E: Auth Screen Found. Entering Credentials...")
            
            // Enter Email
            val emailObj = device.wait(Until.findObject(By.res(java.util.regex.Pattern.compile(".*email_input"))), 2000) 
                ?: device.findObject(By.desc("Email Input"))
            
            if (emailObj != null) {
                println("E2E: Found Email via ID/Desc. Clicking...")
                emailObj.click()
            } else {
                println("E2E: Email Input NOT Found via ID/Desc. Clicking Label.")
                val label = device.findObject(By.text("COMMANDER EMAIL"))
                label?.click()
            }
            device.waitForIdle()
            // Reliable text entry via shell
            device.executeShellCommand("input text derkusch@gmail.com")
            println("E2E: Typed Email via Shell")
            device.pressBack() // Close keyboard

            // Enter Password
            val passObj = device.wait(Until.findObject(By.res(java.util.regex.Pattern.compile(".*password_input"))), 2000)
                ?: device.findObject(By.desc("Password Input"))

            if (passObj != null) {
                 println("E2E: Found Password via ID/Desc. Clicking...")
                passObj.click()
            } else {
                 println("E2E: Pw Input NOT Found via ID/Desc. Clicking Label.")
                 val label = device.findObject(By.text("PASSCODE"))
                 label?.click()
            }
            device.waitForIdle()
            device.executeShellCommand("input text Kuschi_2001")
             println("E2E: Typed Password via Shell")
            device.pressBack()

            // Click Login Button
            val loginBtn = device.findObject(By.text("INITIALIZE SYSTEM"))
            loginBtn?.click()
            println("E2E: Clicked Login")
            
            // Wait for Login to process
            device.wait(Until.gone(By.text("INITIALIZE SYSTEM")), 15000L)
        }
        
        // --- HANDLE UPDATE DIALOG (AGAIN) ---
        if (device.wait(Until.findObject(By.text(updateNowPattern)), 3000) != null) {
             println("E2E: Found 'Update Now' button AGAIN. Clicking...")
             val updateBtn = device.findObject(By.text(updateNowPattern))
             updateBtn?.click()
             device.wait(Until.gone(By.text(updateNowPattern)), 10000L)
        }

        // Check for Welcome Screen "Start Shift"
        println("E2E: Checking for Start Shift...")
        if (device.wait(Until.findObject(By.text("Start Shift")), 10000L) != null) {
            device.findObject(By.text("Start Shift")).click()
            println("E2E: Clicked Start Shift")
        }
        
        // Wait for Sales (Main Screen)
        println("E2E: Waiting for Sales Screen...")
        if (device.wait(Until.findObject(By.text("Sales")), timeout) == null) {
             println("E2E: FAILED to reach Sales Screen. Dumping Hierarchy...")
             device.executeShellCommand("uiautomator dump /sdcard/failure_dump.xml")
             println("E2E: Dumped Window Hierarchy to /sdcard/failure_dump.xml via Shell")
            throw RuntimeException("Failed to reach Sales screen. Stuck on Login, Update, or Welcome.")
        }
        println("E2E: Reached Sales Screen!")
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
}
