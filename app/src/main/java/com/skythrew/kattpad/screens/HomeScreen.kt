package com.skythrew.kattpad.screens

import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.skythrew.kattpad.api.Wattpad
import com.skythrew.kattpad.api.requests.StoryData
import com.skythrew.kattpad.api.User
import com.skythrew.kattpad.domain.attempt
import com.skythrew.kattpad.domain.readingProgress
import com.skythrew.kattpad.screens.utils.navigateOnce
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Composable
fun HomeScreen(padding: PaddingValues, navController: NavController, client: Wattpad) {
    var query by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("Fantasy") }
    var profiles by rememberSaveable { mutableStateOf(false) }
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var offset by remember(query, genre, profiles) { mutableIntStateOf(0) }
    var nextOffset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }

    LaunchedEffect(query, genre, profiles, retry, offset) {
        loading = true
        failed = false
        if (offset == 0) {
            stories = emptyList()
            users = emptyList()
        }
        delay(400)
        attempt {
            if (profiles) {
                val page = if (query.isNotBlank()) client.searchUser(query.trim(), offset) else emptyList()
                val previousCount = users.size
                users = (users + page).distinctBy { it.data.username }
                hasMore = page.isNotEmpty() && users.size > previousCount
                nextOffset = offset + page.size
            } else {
                val page = client.searchStory(query.trim().ifBlank { genre }, offset).map { it.data }
                val previousCount = stories.size
                stories = (stories + page).distinctBy { it.id }
                hasMore = page.isNotEmpty() && stories.size > previousCount
                nextOffset = offset + page.size
            }
        }.onFailure { failed = true }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("KATTPAD / DISCOVER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Your next obsession\nstarts here.", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Fresh worlds. Unforgettable voices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                placeholder = { Text("Search stories or writers") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !profiles, onClick = { profiles = false }, label = { Text("Stories") })
                FilterChip(selected = profiles, onClick = { profiles = true }, label = { Text("Writers") })
            }
        }
        if (!profiles && query.isBlank()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Fantasy", "Romance", "Mystery", "Teen Fiction", "Science Fiction", "Horror")) { name ->
                        FilterChip(selected = name == genre, onClick = { genre = name }, label = { Text(name) })
                    }
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("FIND YOUR NEXT WORLD", style = MaterialTheme.typography.labelSmall)
                        Text("A little escape.\nA whole new perspective.", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                        Text("Explore $genre stories on Wattpad.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Text(if (query.isBlank()) if (profiles) "Find a writer" else "Explore $genre" else "Search results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        when {
            profiles -> {
                if (users.isEmpty() && !loading && !failed) item { Text(if (query.isBlank()) "Search for a Wattpad username to discover their stories." else "No writers found. Try another name.") }
                items(users) { user ->
                    ListItem(
                        headlineContent = { Text(user.data.username) },
                        leadingContent = { AsyncImage(model = user.data.avatar, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))) },
                        modifier = Modifier.clickable { navController.navigateOnce(ProfileScreen(user.data.username)) }
                    )
                }
            }
            else -> {
                if (stories.isEmpty() && !loading && !failed) item { Text("No stories found. Try a different title or genre.") }
                items(stories) { story -> StoryRow(story = story, subtitle = story.description.orEmpty()) { navController.navigateOnce(StoryScreen(story.id)) } }
            }
        }
        when {
            loading -> item { LoadingState() }
            failed -> item { ErrorState("We couldn’t load discoveries. Check your connection and try again.") { retry++ } }
            hasMore -> item { OutlinedButton(onClick = { offset = nextOffset }) { Text("Load more") } }
        }
    }
}

@Composable
fun LibraryScreen(padding: PaddingValues, navController: NavController, client: Wattpad, isLogging: Boolean, isLogged: Boolean) {
    var stories by remember { mutableStateOf<List<StoryData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(isLogged, retry) {
        if (!isLogged) { loading = false; stories = emptyList(); return@LaunchedEffect }
        loading = true
        failed = false
        attempt { stories = client.fetchLibrary()?.data?.stories.orEmpty().filter { it.deleted != true } }
            .onFailure { failed = true }
        loading = false
    }
    val visible = stories.filter {
        it.title.orEmpty().contains(query, ignoreCase = true) && when (filter) {
            "Reading" -> it.readingProgress().chapter > 0
            "Completed" -> it.completed == true
            else -> true
        }
    }.sortedByDescending { it.readingPosition?.lastReadDate?.time ?: 0L }

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("YOUR PERSONAL BOOKSHELF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("One more chapter.", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Text("Pick up where your imagination left off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            isLogging || loading -> item { LoadingState() }
            !isLogged -> item {
                Text("Your stories, all in one place. Connect your Wattpad account to see your library.")
                Button(onClick = { navController.navigateOnce(LoginScreen) }) { Text("Connect Wattpad") }
            }
            failed -> item { ErrorState("Your library couldn’t be loaded. Your stories haven’t been changed.") { retry++ } }
            else -> {
                item {
                    OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Search your library") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Reading", "Completed").forEach { name ->
                            FilterChip(selected = filter == name, onClick = { filter = name }, label = { Text(name) })
                        }
                    }
                    Text("${visible.size} stories", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { retry++ }) { Text("Refresh library") }
                }
                if (visible.isEmpty()) item { Text(if (stories.isEmpty()) "A new chapter awaits. Add stories to your library on Wattpad, then refresh here." else "No stories match this filter.") }
                items(visible) { story ->
                    val progress = story.readingProgress()
                    StoryRow(story, if (progress.chapter > 0) "Chapter ${progress.chapter} of ${progress.total} · Continue reading" else "Not started · Open story", progress.fraction) {
                        val part = progress.partId
                        if (part != null) navController.navigateOnce(PartScreen(part, story.id))
                        else navController.navigateOnce(StoryScreen(story.id))
                    }
                }
            }
        }
    }
}

@Composable
fun StoryRow(story: StoryData, subtitle: String, progress: Float? = null, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(80.dp, 120.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text("K", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
                AsyncImage(model = story.cover, contentDescription = "Cover of ${story.title.orEmpty()}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(story.title ?: "Untitled story", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("by ${story.user.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else if (story.completed == true) Text("COMPLETED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(message: String, retry: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = retry) { Text("Try again") }
    }
}

@Keep
@Serializable
object HomeScreen

@Keep
@Serializable
object LibraryScreen

@Keep
@Serializable
object AccountScreen
