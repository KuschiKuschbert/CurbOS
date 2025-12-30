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
        
        val auth0 = com.auth0.android.Auth0(
            context.getString(com.curbos.pos.R.string.com_auth0_client_id),
            context.getString(com.curbos.pos.R.string.com_auth0_domain)
        )

        com.auth0.android.provider.WebAuthProvider.login(auth0)
            .withScheme("demo") // Ensure this matches build.gradle manifestPlaceholder
            .withScope("openid profile email offline_access")
            .start(context, object : com.auth0.android.callback.Callback<com.auth0.android.result.Credentials, com.auth0.android.authentication.AuthenticationException> {
                override fun onSuccess(result: com.auth0.android.result.Credentials) {
                    val idToken = result.idToken // JWT
                    val userProfile = result.user
                    val email = userProfile.email
                    
                    if (email == null) {
                         _loginState.value = LoginState.Error("No email found in profile")
                         return
                    }

                    viewModelScope.launch {
                        // 1. "Sign In" (For now, just acknowledge token)
                        SupabaseManager.signInWithAuth0(idToken)
                        
                        // 2. Check Subscription
                        val subResult = SupabaseManager.checkSubscriptionStatus(email)
                        when (subResult) {
                            is com.curbos.pos.common.Result.Success -> {
                                if (subResult.data) {
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
