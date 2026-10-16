package com.skythrew.kattpad.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import com.skythrew.kattpad.domain.attempt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.skythrew.kattpad.R
import com.skythrew.kattpad.api.Wattpad
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen (padding: PaddingValues, navController: NavController, client: Wattpad, store: SharedPreferences, loggedState: MutableState<Boolean>) {
    val coroutineScope = rememberCoroutineScope()

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var isLogging by remember {
        mutableStateOf(false)
    }

    var incorrectCredentials by remember {
        mutableStateOf(false)
    }

    TopAppBar(title = {}, navigationIcon = {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
        }
    })
    Column (
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Text(stringResource(id = R.string.login_short_text), fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize)

            Column (
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                TextField(
                    value = username,
                    singleLine = true,
                    enabled = !isLogging,
                    onValueChange = {username = it},
                    trailingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    label = {Text(stringResource(id = R.string.username))},
                    isError = incorrectCredentials,
                    supportingText = {
                        if (incorrectCredentials)
                            Text(stringResource(id = R.string.incorrect_credentials))
                    }
                )

                TextField(
                    value = password,
                    singleLine = true,
                    enabled = !isLogging,
                    onValueChange = {password = it},
                    trailingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    label = {Text(stringResource(id = R.string.password))},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = incorrectCredentials,
                    supportingText = {
                        if (incorrectCredentials)
                            Text(stringResource(id = R.string.incorrect_credentials))
                    }
                )
            }


            Button(enabled = !isLogging && username.isNotBlank() && password.isNotBlank(), onClick = {
                coroutineScope.launch {
                    isLogging = true
                    incorrectCredentials = false
                    try {
                        attempt {
                            client.login(username.trim(), password)
                            check(client.loggedIn) { "Login rejected" }
                            val token = client.getToken()
                            check(!token.isNullOrBlank()) { "Missing session token" }
                            store.edit { putString("token", token) }
                            loggedState.value = true
                            password = ""
                            navController.popBackStack()
                        }.onFailure { incorrectCredentials = true }
                    } finally { isLogging = false }
                }
            }) {
                Text(if (isLogging) "Connecting…" else stringResource(id = R.string.log_in))
            }
            if (isLogging) CircularProgressIndicator()
            if (incorrectCredentials) Text(
                "Couldn’t connect. Check your details and connection. Wattpad may require verification on its website.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Serializable
object LoginScreen