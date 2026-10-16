package com.skythrew.kattpad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.skythrew.kattpad.api.Wattpad
import com.skythrew.kattpad.data.createSharedPreferences
import com.skythrew.kattpad.domain.attempt
import com.skythrew.kattpad.screens.*
import com.skythrew.kattpad.screens.utils.GenericNav
import com.skythrew.kattpad.screens.utils.navigateOnce
import com.skythrew.kattpad.ui.theme.KattpadTheme
import io.ktor.client.plugins.ResponseException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = try {
            createSharedPreferences(applicationContext)
        } catch (_: Exception) {
            // A device-restored encrypted file may no longer match the Keystore key.
            // Never fall back to storing credentials in plaintext.
            setContent {
                KattpadTheme {
                    Surface {
                        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Your saved sign-in couldn’t be unlocked.", style = MaterialTheme.typography.headlineSmall)
                            Text("Reset local sign-in to reconnect. This removes only this device’s saved session, not your Wattpad account or stories.")
                            Button(onClick = { deleteSharedPreferences("credentials"); recreate() }) { Text("Reset local sign-in") }
                        }
                    }
                }
            }
            return
        }
        enableEdgeToEdge()
        setContent {
            KattpadTheme {
                val client = remember { Wattpad() }
                DisposableEffect(client) { onDispose { client.close() } }
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                val showBottomBar = entry?.destination?.route in listOf(
                    HomeScreen.javaClass.name, LibraryScreen.javaClass.name, AccountScreen.javaClass.name
                )
                var isLogging by remember { mutableStateOf(true) }
                val isLogged = remember { mutableStateOf(false) }
                var sessionMessage by remember { mutableStateOf<String?>(null) }
                var restoreAttempt by remember { mutableIntStateOf(0) }
                var canRetrySession by remember { mutableStateOf(false) }

                LaunchedEffect(restoreAttempt) {
                    isLogging = true
                    sessionMessage = null
                    canRetrySession = false
                    try {
                        attempt {
                            val token = store.getString("token", null)
                            if (!token.isNullOrBlank()) client.loginByCookie(token)
                        }.onFailure { error ->
                            val expired = error is ResponseException && error.response.status.value == 401
                            if (expired) store.edit { remove("token") }
                            canRetrySession = !expired
                            sessionMessage = if (expired) "Your session has expired. Please connect Wattpad again."
                                else "We couldn’t reconnect to Wattpad. You can still browse, or retry from Account."
                        }
                        isLogged.value = client.loggedIn
                    } finally { isLogging = false }
                }

                Scaffold(bottomBar = { if (showBottomBar) GenericNav(navController) }) { padding ->
                    NavHost(navController, startDestination = HomeScreen) {
                        composable<HomeScreen> { HomeScreen(padding, navController, client) }
                        composable<LibraryScreen> { LibraryScreen(padding, navController, client, isLogging, isLogged.value) }
                        composable<AccountScreen> {
                            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Text("Your space.", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
                                Text("A quieter way to read Wattpad.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isLogging) LoadingState()
                                sessionMessage?.takeIf { !isLogged.value }?.let { Text(it) }
                                if (isLogged.value) {
                                    Text("Connected as @${client.username}", style = MaterialTheme.typography.titleLarge)
                                    Button(onClick = { client.username?.let { navController.navigateOnce(ProfileScreen(it)) } }) { Text("View my profile") }
                                    OutlinedButton(onClick = {
                                        client.logout()
                                        store.edit { remove("token") }
                                        isLogged.value = false
                                        sessionMessage = null
                                        canRetrySession = false
                                    }) { Text("Sign out on this device") }
                                } else if (!isLogging) {
                                    Button(onClick = { navController.navigateOnce(LoginScreen) }) { Text("Connect Wattpad") }
                                    if (canRetrySession) OutlinedButton(onClick = { restoreAttempt++ }) { Text("Retry connection") }
                                }
                                Text("Independent, open-source client. Not affiliated with Wattpad. Account access and content availability depend on Wattpad’s services.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        composable<StoryScreen> {
                            val args = it.toRoute<StoryScreen>()
                            StoryScreen(padding, navController, client, args.storyId)
                        }
                        composable<PartScreen> {
                            val args = it.toRoute<PartScreen>()
                            PartScreen(navController, client, args.storyId, args.partId)
                        }
                        composable<ProfileScreen> {
                            val args = it.toRoute<ProfileScreen>()
                            ProfileScreen(padding, navController, client, args.username)
                        }
                        composable<LoginScreen> { LoginScreen(padding, navController, client, store, isLogged) }
                    }
                }
            }
        }
    }
}
