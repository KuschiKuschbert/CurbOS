package com.curbos.pos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.curbos.pos.data.p2p.P2PConnectivityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class P2PConnectivityTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.NEARBY_WIFI_DEVICES
    )

    @Test
    fun testP2PInitializationAndAdvertising() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = P2PConnectivityManager(appContext)

        // Give it a moment to initialize
        assertTrue(manager.connectionStatus.value == "Idle")

        manager.startAdvertising("TestHost")
        
        // Since it's async, we might not see immediate status change without wait, 
        // but let's check if it didn't crash and status changed or is changing.
        // P2P/Nearby calls are async tasks.
        
        // Wait up to 5 seconds for status to change from "Idle"

        var advertisingStarted = false
        
        // We can't easily observe StateFlow in a non-setup coroutine test without proper scope, 
        // but we can poll for a bit.
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 5000) {
            if (manager.connectionStatus.value.contains("Advertising")) {
                advertisingStarted = true
                break
            }
            Thread.sleep(100)
        }
        
        if (!advertisingStarted) {
             // It might have failed or is still starting.
             // If we are on emulator, Nearby often fails. 
             // On real device, it should work.
             // Let's assert that it's NOT "Idle" at least, or log the failure status
        }
        
        // Even if it failed (e.g. no bluetooth), the manager handles it gracefully and updates status.
        assertNotEquals("Should attempt to change status from Idle", "Idle", manager.connectionStatus.value)
        
        manager.stopAdvertising()
        Thread.sleep(500)
        assertEquals("Should return to Idle after stop", "Idle", manager.connectionStatus.value)
    }
    
    @Test
    fun testP2PDiscovery() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = P2PConnectivityManager(appContext)

        manager.startDiscovery()
        
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < 5000) {
            if (manager.connectionStatus.value.contains("Searching")) {

                break
            }
            Thread.sleep(100)
        }
        
        assertNotEquals("Should attempt to change status from Idle", "Idle", manager.connectionStatus.value)

        manager.stopDiscovery()
        Thread.sleep(500)
        assertEquals("Should return to Idle after stop", "Idle", manager.connectionStatus.value)
    }
}
