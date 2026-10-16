package com.skythrew.kattpad.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.skythrew.kattpad.api.Story
import com.skythrew.kattpad.api.User
import com.skythrew.kattpad.api.Wattpad
import com.skythrew.kattpad.domain.attempt
import com.skythrew.kattpad.screens.utils.navigateOnce
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(padding: PaddingValues, navController: NavController, client: Wattpad, username: String) {
    var user by remember(username) { mutableStateOf<User?>(null) }
    var stories by remember(username) { mutableStateOf<List<Story>>(emptyList()) }
    var people by remember(username) { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var tab by remember(username) { mutableStateOf("Stories") }
    var following by remember(username) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var offset by remember(username, tab) { mutableIntStateOf(0) }
    var more by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(username, tab, retry, offset) {
        loading = true
        failed = false
        attempt {
            val profile = user ?: client.fetchUser(username, setOf("username", "description", "numFollowers", "numFollowing", "following"))
            if (user == null) following = profile.data.following == true
            user = profile
            if (tab == "Stories") {
                val page = profile.fetchStories(setOf("title", "cover", "description", "completed"), limit = 20, offset = offset)
                stories = if (offset == 0) page else (stories + page).distinctBy { it.data.id }
                more = page.size >= 20
            } else {
                val page = if (tab == "Followers") profile.fetchFollowers(setOf("avatar"), 20, offset) else profile.fetchFollowing(setOf("avatar"), 20, offset)
                people = if (offset == 0) page else (people + page).distinctBy { it.data.username }
                more = page.size >= 20
            }
        }.onFailure { failed = true }
        loading = false
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text("@$username") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(username, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
                Text(user?.data?.description.orEmpty())
                Text("${user?.data?.numFollowers ?: 0} followers · ${user?.data?.numFollowing ?: 0} following", style = MaterialTheme.typography.labelLarge)
                if (client.loggedIn && client.username != username) Button(enabled = user != null && !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            attempt {
                                val result = if (following) user?.unfollow() else user?.follow()
                                check(result == true)
                                following = !following
                            }.onFailure { snackbar.showSnackbar("Couldn’t update following. Try again.") }
                        } finally { busy = false }
                    }
                }) { Text(if (following) "Unfollow" else "Follow") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Stories", "Followers", "Following").forEach { name -> FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name) }) }
                }
            }
            if (tab == "Stories") {
                items(stories) { story -> StoryRow(story.data, story.data.description.orEmpty()) { navController.navigateOnce(StoryScreen(story.data.id)) } }
            } else {
                items(people) { person -> ListItem(headlineContent = { Text(person.data.username) }, modifier = Modifier.clickable { navController.navigateOnce(ProfileScreen(person.data.username)) }) }
            }
            when {
                loading -> item { LoadingState() }
                failed -> item { ErrorState("We couldn’t load this profile section.") { retry++ } }
                more -> item { OutlinedButton(onClick = { offset += 20 }) { Text("Load more") } }
                stories.isEmpty() && tab == "Stories" -> item { Text("No published stories to show.") }
                people.isEmpty() && tab != "Stories" -> item { Text("No readers to show.") }
            }
        }
    }
}

@Serializable
data class ProfileScreen(val username: String)
