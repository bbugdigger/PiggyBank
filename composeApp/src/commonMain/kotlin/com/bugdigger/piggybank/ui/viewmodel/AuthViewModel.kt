package com.bugdigger.piggybank.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugdigger.piggybank.data.api.PiggyBankApi
import com.bugdigger.piggybank.domain.model.LoginRequest
import com.bugdigger.piggybank.domain.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val username: String? = null,
    val error: String? = null
)

class AuthViewModel(
    private val api: PiggyBankApi
) : ViewModel() {
    
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = api.login(LoginRequest(username, password))
            
            result.onSuccess { response ->
                api.setAuthToken(response.token)
                _state.value = AuthState(
                    isAuthenticated = true,
                    userId = response.userId,
                    username = response.username
                )
            }.onError { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
    
    fun register(username: String, password: String, email: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val result = api.register(RegisterRequest(username, password, email))
            
            result.onSuccess { response ->
                api.setAuthToken(response.token)
                _state.value = AuthState(
                    isAuthenticated = true,
                    userId = response.userId,
                    username = response.username
                )
            }.onError { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
    
    fun logout() {
        api.setAuthToken(null)
        _state.value = AuthState()
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
