package com.bugdigger.piggybank.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bugdigger.piggybank.domain.model.LoginRequest
import com.bugdigger.piggybank.domain.model.UserResponse
import com.bugdigger.piggybank.ui.components.LoadingOverlay
import com.bugdigger.piggybank.ui.components.PiggyBankPasswordField
import com.bugdigger.piggybank.ui.components.PiggyBankTextField
import com.bugdigger.piggybank.ui.navigation.LocalAppState
import com.bugdigger.piggybank.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            val result = appState.api.login(LoginRequest(username, password))

            result
                .onSuccess { response ->
                    appState.api.setAuthToken(response.token)
                    val userResult = appState.api.getCurrentUser()
                    userResult
                        .onSuccess { user ->
                            appState.setAuthenticated(response.token, user)
                        }
                        .onError {
                            appState.setAuthenticated(
                                response.token,
                                UserResponse(
                                    id = response.userId,
                                    username = response.username,
                                    email = null,
                                    createdAt = ""
                                )
                            )
                        }
                }
                .onError { error ->
                    errorMessage = if (error.contains("401") || error.contains("Unauthorized")) {
                        "Invalid username or password"
                    } else {
                        "Login failed: $error"
                    }
                }

            isLoading = false
        }
    }

    LoadingOverlay(isLoading = isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PiggyBank",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Personal Finance Manager",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Card(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    PiggyBankTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        leadingIcon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !isLoading
                    )

                    PiggyBankPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        imeAction = ImeAction.Done,
                        onImeAction = { login() },
                        enabled = !isLoading
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { login() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !isLoading
                    ) {
                        Text("Sign In")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = { appState.navigateTo(Screen.Register) },
                            enabled = !isLoading
                        ) {
                            Text("Sign Up")
                        }
                    }
                }
            }
        }
    }
}
