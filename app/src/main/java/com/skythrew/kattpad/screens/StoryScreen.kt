package com.skythrew.kattpad.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.skythrew.kattpad.api.Story
import com.skythrew.kattpad.api.Wattpad
import com.skythrew.kattpad.domain.attempt
import com.skythrew.kattpad.domain.readingProgress
import com.skythrew.kattpad.screens.utils.navigateOnce
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(padding: PaddingValues, navController: NavController, client: Wattpad, storyId: Int) {
    var loading by remember(storyId) { mutableStateOf(true) }
    var story by remember(storyId) { mutableStateOf<Story?>(null) }
    var failed by remember(storyId) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(storyId, retry) {
        loading = true
        failed = false
        attempt {
            story = client.fetchStory(storyId, setOf("title", "description", "cover", "numParts", "readCount", "voteCount", "completed", "firstPartId", "parts(id,title)"))
        }.onFailure { failed = true }
        loading = false
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Story details") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when {
                loading -> item { LoadingState() }
                failed -> item { ErrorState("We couldn’t open this story. Please try again.") { retry++ } }
                else -> story?.data?.let { data ->
                    item {
                        AsyncImage(model = data.cover, contentDescription = "Cover of ${data.title.orEmpty()}", contentScale = ContentScale.Crop, modifier = Modifier.size(144.dp, 216.dp).clip(RoundedCornerShape(16.dp)))
                        Spacer(Modifier.height(20.dp))
                        Text(data.title ?: "Untitled story", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { navController.navigateOnce(ProfileScreen(data.user.username)) }) { Text("by ${data.user.username}") }
                        Text("${data.numParts ?: data.parts.orEmpty().size} chapters · ${if (data.completed == true) "Completed" else "Ongoing"}", color = MaterialTheme.colorScheme.primary)
                        val first = data.readingProgress().partId
                        Button(enabled = first != null, onClick = { first?.let { navController.navigateOnce(PartScreen(it, storyId)) } }, modifier = Modifier.fillMaxWidth()) { Text("Start reading") }
                    }
                    item {
                        Text("About this story", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Text(data.description ?: "No description available.", style = MaterialTheme.typography.bodyLarge)
                    }
                    item { Text("Table of contents", style = MaterialTheme.typography.titleLarge) }
                    if (data.parts.isNullOrEmpty()) item { Text("No available chapters were returned for this story.") }
                    items(data.parts.orEmpty().filter { it.id != null }) { chapter ->
                        ListItem(headlineContent = { Text(chapter.title ?: "Untitled chapter") }, modifier = Modifier.clickable { chapter.id?.let { navController.navigateOnce(PartScreen(it, storyId)) } })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Serializable
data class StoryScreen(val storyId: Int)
