package com.curbos.pos.data

import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.OfflineTransaction
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.p2p.P2PConnectivityManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSyncManagerTest {

    private lateinit var posDao: PosDao
    private lateinit var p2pManager: P2PConnectivityManager
    private lateinit var syncManager: TransactionSyncManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0

        posDao = mockk(relaxed = true)
        p2pManager = mockk(relaxed = true)
        
        // Mock P2P hosting state
        coEvery { p2pManager.isHosting } returns false

        syncManager = TransactionSyncManager(
            posDao = posDao,
            p2pConnectivityManager = p2pManager,
            externalScope = testScope,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `stageTransaction inserts offline transaction EXACTLY ONCE`() = runTest(testDispatcher) {
        // Arrange
        val transaction = Transaction(
            id = "test-tx-1",
            totalAmount = 10.0,
            taxAmount = 1.0,
            items = emptyList(),
            timestamp = 1234567890L,
            status = "COMPLETED",
            paymentMethod = "CASH",
            orderNumber = 1
        )

        // Act
        syncManager.stageTransaction(transaction)

        // Assert
        coVerify(exactly = 1) { 
            posDao.insertOfflineTransaction(any()) 
        }
    }
}
