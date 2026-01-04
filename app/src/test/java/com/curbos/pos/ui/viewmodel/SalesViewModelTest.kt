package com.curbos.pos.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.repository.TransactionRepository
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
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
        
        // Mock ProfileManager
        val profileManager = mockk<com.curbos.pos.data.prefs.ProfileManager>(relaxed = true)
        every { profileManager.getWebBaseUrl() } returns "https://prepflow.org"

        viewModel = SalesViewModel(posDao, transactionRepository, p2pConnectivityManager, profileManager)
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
        io.mockk.coEvery { transactionRepository.createTransaction(any()) } returns com.curbos.pos.common.Result.Success(true)

        // Mock SnackbarManager to prevent blocking
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit

        // Mock Context
        val context = mockk<android.content.Context>(relaxed = true)
        
        viewModel.processPayment("CASH")
        
        // Advance dispatcher to execute the launch block
        advanceUntilIdle()
        
        coVerify { transactionRepository.createTransaction(any()) }
        
        val state = viewModel.uiState.value
        assertTrue("Cart should be empty after payment", state.cartItems.isEmpty())
        assertEquals("Total amount should be 0.0", 0.0, state.totalAmount, 0.0)
    }


    @Test
    fun `applyPromoCode SAVE10 applies 10 percent discount`() = runTest(testDispatcher) {
        // Setup Cart with $100 item
        val menuItem = MenuItem(id = "1", name = "Expensive Steak", price = 100.0, category = "Food", imageUrl = null)
        viewModel.addToCart(menuItem)
        
        // Mock Snackbar
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit
        
        // Apply Code
        viewModel.applyPromoCode("SAVE10")
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Discount should be 10.0", 10.0, state.discountAmount, 0.0)
        assertEquals("Total should be 90.0", 90.0, state.totalAmount, 0.0)
        assertEquals("Promo code should be SAVE10", "SAVE10", state.appliedPromoCode)

        coVerify { com.curbos.pos.common.SnackbarManager.showSuccess(any()) }
    }

    @Test
    fun `applyPromoCode WELCOME applies 5 dollar fixed discount if min spend met`() = runTest(testDispatcher) {
        // Setup Cart with $25 item
        val menuItem = MenuItem(id = "1", name = "Meal", price = 25.0, category = "Food", imageUrl = null)
        viewModel.addToCart(menuItem)
        
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit

        viewModel.applyPromoCode("WELCOME")
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(5.0, state.discountAmount, 0.0)
        assertEquals(20.0, state.totalAmount, 0.0)

        coVerify { com.curbos.pos.common.SnackbarManager.showSuccess(any()) }
    }

    @Test
    fun `applyPromoCode WELCOME fails if min spend not met`() = runTest(testDispatcher) {
        // Setup Cart with $10 item
        val menuItem = MenuItem(id = "1", name = "Snack", price = 10.0, category = "Food", imageUrl = null)
        viewModel.addToCart(menuItem)
        
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showError(any()) } returns Unit

        viewModel.applyPromoCode("WELCOME")
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Discount should be 0", 0.0, state.discountAmount, 0.0)
        assertEquals("Total should remain 10.0", 10.0, state.totalAmount, 0.0)
        
        
        io.mockk.coVerify { com.curbos.pos.common.SnackbarManager.showError(match { it.contains("Invalid Code or Minimum Spend") }) }
    }

    @Test
    fun `searchCustomer attaches customer on success`() = runTest(testDispatcher) {
        val customer = Customer(id="123", phoneNumber="555", fullName="Jane", email=null, redeemableMiles=100.0, lifetimeMiles=100.0, currentRank="Rookie")
        every { transactionRepository.getLoyaltyRewards() } returns flowOf(emptyList()) // avoid flow issues
        io.mockk.coEvery { transactionRepository.syncRewards() } returns Unit
        io.mockk.coEvery { transactionRepository.getCustomerByPhone("555") } returns com.curbos.pos.common.Result.Success(customer)
        
        viewModel.searchCustomer("555")
        advanceUntilIdle()
        
        assertEquals(customer, viewModel.uiState.value.selectedCustomer)
    }

    @Test
    fun `redeemReward applies discount and updates state`() = runTest(testDispatcher) {
        // Setup Customer
        val customer = Customer(id="123", phoneNumber="555", fullName="Jane", email=null, redeemableMiles=100.0, lifetimeMiles=100.0, currentRank="Rookie")
        every { transactionRepository.getLoyaltyRewards() } returns flowOf(emptyList())
        io.mockk.coEvery { transactionRepository.syncRewards() } returns Unit
        viewModel.attachCustomer(customer)
        advanceUntilIdle()
        
        // Setup Cart ($10 item)
        val menuItem = MenuItem(id="1", name="Taco", price=10.0, category="Food", imageUrl=null)
        viewModel.addToCart(menuItem) // Total 10
        
        // Setup Reward (Free Taco, cost 50, val $5)
        val reward = LoyaltyReward(id="r1", description="Free Taco", costMiles=50, isAutoApplied = true, discountAmount = 5.0)
        
        // Mock Snackbar
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit

        viewModel.redeemReward(reward)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        // Debugging prints
        println("DEBUG: Discount=${state.discountAmount}, Redeemed=${state.milesRedeemed}, Total=${state.totalAmount}")
        println("DEBUG: UiState=$state")
        
        assertEquals("Discount should be 5.0", 5.0, state.discountAmount, 0.0)
        assertEquals("Miles Redeemed should be 50.0", 50.0, state.milesRedeemed, 0.0)
        assertEquals("Total should be 5.0", 5.0, state.totalAmount, 0.0)
    }
    
    @Test
    fun `processPayment updates customer points correctly`() = runTest(testDispatcher) {
        // Mock Logs/Snackbar/Repo
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        io.mockk.coEvery { transactionRepository.createTransaction(any()) } returns com.curbos.pos.common.Result.Success(true)
        io.mockk.mockkObject(com.curbos.pos.common.SnackbarManager)
        io.mockk.coEvery { com.curbos.pos.common.SnackbarManager.showSuccess(any()) } returns Unit
        
        
        every { transactionRepository.getLoyaltyRewards() } returns flowOf(emptyList())
        io.mockk.coEvery { transactionRepository.syncRewards() } returns Unit
        
        // 1. Attach Customer (100 redeemable)
        val customer = Customer(id="123", phoneNumber="555", fullName="Jane", email=null, redeemableMiles=100.0, lifetimeMiles=100.0, currentRank="Rookie")
        viewModel.attachCustomer(customer)
        
        
        // 2. Add Item ($60)
        val menuItem = MenuItem(id="1", name="Feast", price=60.0, category="Food", imageUrl=null)
        viewModel.addToCart(menuItem)
        
        // 3. Redeem Reward (50 miles, $5 off)
        val reward = LoyaltyReward(id="r1", description="Free Taco", costMiles=50, isAutoApplied = true, discountAmount = 5.0)
        viewModel.redeemReward(reward)
        
        // State: Total=$55 (60 - 5). Redeemed=50.
        
        // 4. Process Payment
        viewModel.processPayment("CASH")
        advanceUntilIdle()
        
        // 5. Verify Customer Update
        // Earned: 55 (Total paid)
        // Redeemed: 50
        // New Lifetime: 100 + 55 = 155
        // New Redeemable: 100 - 50 + 55 = 105
        
        val slot = io.mockk.slot<Customer>()
        coVerify { transactionRepository.createOrUpdateCustomer(capture(slot)) }
        
        val updated = slot.captured
        assertEquals("Lifetime miles should increase by spent amount", 155.0, updated.lifetimeMiles, 0.01)
        assertEquals("Redeemable miles logic check", 105.0, updated.redeemableMiles, 0.01)
    }
}
