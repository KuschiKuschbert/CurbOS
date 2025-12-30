package com.curbos.pos.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.repository.TransactionRepository
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.p2p.P2PConnectivityManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    // Mocks
    private val posDao = mockk<PosDao>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val p2pConnectivityManager = mockk<P2PConnectivityManager>(relaxed = true)

    private lateinit var viewModel: SalesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock DAO flows
        every { posDao.getAllMenuItems() } returns flowOf(emptyList())
        every { posDao.getAllModifiers() } returns flowOf(emptyList())
        every { posDao.getUnsyncedCount() } returns flowOf(0)
        
        viewModel = SalesViewModel(posDao, transactionRepository, p2pConnectivityManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.unmockkAll()
    }

    @Test
    fun `addToCart adds item to state`() = runTest(testDispatcher) {
        val menuItem = MenuItem(id = "1", name = "Taco", price = 5.0, category = "Food", imageUrl = null)
        
        viewModel.addToCart(menuItem)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals("Taco", state.cartItems[0].menuItem.name)
        assertEquals(5.0, state.totalAmount, 0.0)
    }

    @Test
    fun `calculate total amount correctly with modifiers`() = runTest(testDispatcher) {
        val menuItem = MenuItem(id = "1", name = "Taco", price = 5.0, category = "Food", imageUrl = null)
        val modifiers = listOf(
            com.curbos.pos.data.model.ModifierOption(id = "m1", name = "Cheese", priceDelta = 1.0)
        )
        
        viewModel.addToCart(menuItem, modifiers)
        
        val state = viewModel.uiState.value
        assertEquals(6.0, state.totalAmount, 0.0)
    }

    @Test
    fun `clearCart removes all items`() = runTest(testDispatcher) {
        val menuItem = MenuItem(id = "1", name = "Taco", price = 5.0, category = "Food", imageUrl = null)
        viewModel.addToCart(menuItem)
        
        viewModel.clearCart()
        
        val state = viewModel.uiState.value
        assertTrue(state.cartItems.isEmpty())
        assertEquals(0.0, state.totalAmount, 0.0)
    }

    @Test
    fun `processPayment creates transaction via repository`() = runTest(testDispatcher) {
        // Mock Log
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0

        val menuItem = MenuItem(id = "1", name = "Taco", price = 5.0, category = "Food", imageUrl = null)
        viewModel.addToCart(menuItem)
        
        // Mock Repository result explicitly
        io.mockk.coEvery { transactionRepository.createTransaction(any()) } returns com.curbos.pos.common.Result.Success(Unit)

        // Mock SnackbarManager to prevent blocking
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit

        // Mock Context
        val context = mockk<android.content.Context>(relaxed = true)
        
        viewModel.processPayment(context, "CASH")
        
        // Advance dispatcher to execute the launch block
        advanceUntilIdle()
        
        coVerify { transactionRepository.createTransaction(any()) }
        
        val state = viewModel.uiState.value
        assertTrue("Cart should be empty after payment", state.cartItems.isEmpty())
        assertEquals("Total amount should be 0.0", 0.0, state.totalAmount, 0.0)
    }
}
