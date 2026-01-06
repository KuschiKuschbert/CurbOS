package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.repository.AuthRepository
import com.curbos.pos.data.prefs.ProfileManager
import com.curbos.pos.common.Result
import com.curbos.pos.data.remote.SupabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.curbos.pos.R
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    object UpgradeRequired : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(context: android.content.Context) {
        _loginState.value = LoginState.Loading
        
        val clientId = try { context.getString(R.string.com_auth0_client_id) } catch (e: Exception) { null }
        val domain = try { context.getString(R.string.com_auth0_domain) } catch (e: Exception) { null }

        if (clientId.isNullOrBlank() || domain.isNullOrBlank()) {
            val errorMsg = "Auth0 Configuration Missing or Invalid resource ID. ClientID: $clientId, Domain: $domain"
            com.curbos.pos.common.Logger.e("LoginViewModel", errorMsg)
            _loginState.value = LoginState.Error("System Configuration Error: Missing Auth0 Keys")
            return
        }

        val auth0 = com.auth0.android.Auth0(clientId, domain)

        com.auth0.android.provider.WebAuthProvider.login(auth0)
            .withScheme("demo") // Changed to demo to bypass assetlinks failure
            .withScope("openid profile email offline_access")
            .start(context, object : com.auth0.android.callback.Callback<com.auth0.android.result.Credentials, com.auth0.android.authentication.AuthenticationException> {
                override fun onSuccess(result: com.auth0.android.result.Credentials) {
                    val userProfile = result.user
                    val email = userProfile.email
                    
                    if (email == null) {
                         _loginState.value = LoginState.Error("No email found in profile")
                         return
                    }

                    viewModelScope.launch {
                        // 2. Perform Supabase Login (Gate check)
                        com.curbos.pos.common.Logger.d("LoginViewModel", "Auth0 Success! ID Token: ${result.idToken.take(10)}...")
                        val supabaseResult = SupabaseManager.signInWithAuth0(result.idToken)
                        
                        if (supabaseResult is com.curbos.pos.common.Result.Error) {
                            com.curbos.pos.common.Logger.e("LoginViewModel", "Supabase Sign-In Failed: ${supabaseResult.message}", supabaseResult.exception)
                            _loginState.value = LoginState.Error(supabaseResult.message ?: "Authentication failed during token exchange")
                            return@launch
                        }

                        com.curbos.pos.common.Logger.d("LoginViewModel", "Supabase Sign-In Success! Proceeding to subscription check for $email")
                        
                        // 2. Check Subscription
                        val subResult = SupabaseManager.checkSubscriptionStatus(email)
                        when (subResult) {
                            is com.curbos.pos.common.Result.Success -> {
                                if (subResult.data) {
                                    // Save User Name to Trigger "isLoggedIn" State
                                    val name = userProfile.name ?: userProfile.nickname ?: email.split("@")[0]
                                    profileManager.saveChefName(name)
                                    
                                    _loginState.value = LoginState.Success
                                } else {
                                    _loginState.value = LoginState.UpgradeRequired
                                }
                            }
                            is com.curbos.pos.common.Result.Error -> {
                                _loginState.value = LoginState.Error(subResult.message ?: "Subscription check failed")
                            }
                            else -> {
                                // Handle Loading or other states if necessary
                            }
                        }
                    }
                }

                override fun onFailure(error: com.auth0.android.authentication.AuthenticationException) {
                    _loginState.value = LoginState.Error(error.localizedMessage ?: "Auth0 Login Failed")
                }
            })
    }
}
