package com.curbos.pos.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.curbos.pos.data.repository.TransactionRepository
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.p2p.P2PConnectivityManager
import com.curbos.pos.data.prefs.ProfileManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class KitchenViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val profileManager = mockk<ProfileManager>(relaxed = true)
    private val p2pConnectivityManager = mockk<P2PConnectivityManager>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)

    private lateinit var viewModel: KitchenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Log
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        
        // Mock Repository flows
        every { transactionRepository.getActiveTransactions() } returns flowOf(emptyList())
        // Mock Suspend functions
        coEvery { transactionRepository.fetchActiveTransactions() } returns com.curbos.pos.common.Result.Success(emptyList())
        coEvery { transactionRepository.subscribeToTransactionChanges(any()) } returns Unit
        
        // Mock P2P
        every { p2pConnectivityManager.receivedMessages } returns kotlinx.coroutines.flow.MutableStateFlow(null)
        every { p2pConnectivityManager.connectedEndpoints } returns kotlinx.coroutines.flow.MutableStateFlow(emptySet())
        every { p2pConnectivityManager.isHosting } returns true // Simulate Host/Online

        viewModel = KitchenViewModel(profileManager, p2pConnectivityManager, transactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
         io.mockk.unmockkAll()
    }

    @Test
    fun `bumpOrder updates transaction via repository`() = runTest(testDispatcher) {
        // Setup initial state with one transaction
        val transaction = Transaction(
            id = "tx1",
            timestamp = System.currentTimeMillis(),
            totalAmount = 10.0,
            taxAmount = 1.0,
            items = emptyList(),
            status = "COMPLETED",
            paymentMethod = "CASH",
            fulfillmentStatus = "PENDING",
            orderNumber = 1,
            customerName = "Daniel"
        )
        
        // Mock update success
        coEvery { transactionRepository.updateTransaction(any()) } returns com.curbos.pos.common.Result.Success(Unit)

        // Trigger bump
        viewModel.bumpOrder(transaction)
        
        advanceUntilIdle()
        
        // Verify Repository called
        coVerify { transactionRepository.updateTransaction(any()) }
    }
}
