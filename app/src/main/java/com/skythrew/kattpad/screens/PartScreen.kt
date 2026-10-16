package com.skythrew.kattpad.screens

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skythrew.kattpad.domain.attempt
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.skythrew.kattpad.R
import com.skythrew.kattpad.api.Comment
import com.skythrew.kattpad.api.Part
import com.skythrew.kattpad.api.Wattpad
import com.skythrew.kattpad.screens.utils.getFormattedNumber
import com.skythrew.kattpad.screens.utils.navigateOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartScreen(navController: NavController, client: Wattpad, storyId: Int, id: Int) {
    BackHandler {
        if (!navController.popBackStack(StoryScreen(storyId), inclusive = false))
            navController.popBackStack()
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("reader", android.content.Context.MODE_PRIVATE) }
    var fontSize by remember { mutableIntStateOf(preferences.getInt("font_size", 18).coerceIn(14, 30)) }
    var appearance by remember { mutableStateOf(preferences.getString("appearance", "Paper") ?: "Paper") }
    var settings by remember { mutableStateOf(false) }
    var loading by remember(id) { mutableStateOf(true) }
    var failed by remember(id) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var part by remember(id) { mutableStateOf<Part?>(null) }
    var text by remember(id) { mutableStateOf("") }
    var voted by remember(id) { mutableStateOf(false) }
    var voteCount by remember(id) { mutableStateOf<Int?>(null) }
    var previous by remember(id) { mutableStateOf<Int?>(null) }
    var next by remember(id) { mutableStateOf<Int?>(null) }
    var voting by remember { mutableStateOf(false) }
    val showComments = remember { mutableStateOf(false) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(fontSize, appearance) {
        preferences.edit().putInt("font_size", fontSize).putString("appearance", appearance).apply()
    }
    LaunchedEffect(id, retry) {
        loading = true
        failed = false
        attempt {
            val loaded = client.fetchStoryPart(id, setOf("title", "group", "readCount", "voteCount", "commentCount", "voted"))
            text = HtmlCompat.fromHtml(loaded.fetchText(), HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            val chapters = loaded.data.story?.parts.orEmpty()
            val index = chapters.indexOfFirst { it.id == id }
            previous = if (index > 0) chapters.getOrNull(index - 1)?.id else null
            next = if (index >= 0) chapters.getOrNull(index + 1)?.id else null
            part = loaded
            voted = loaded.data.voted == true
            voteCount = loaded.data.voteCount
        }.onFailure { failed = true }
        loading = false
        // Sync is best-effort; reading must remain available if the sync endpoint fails.
        if (!failed && client.loggedIn) attempt { part?.syncReadingPosition() }
    }
    val paper = when (appearance) { "Night" -> Color(0xFF191D1B); "Sepia" -> Color(0xFFF3E8D2); else -> Color(0xFFFCFAF5) }
    val ink = if (appearance == "Night") Color(0xFFE4E5DC) else Color(0xFF262C28)
    val paragraphs = remember(text) { text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() } }
    val listState = androidx.compose.runtime.key(id) { rememberLazyListState() }

    Scaffold(
        containerColor = paper,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(part?.data?.title ?: "Reading", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = ink) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = paper),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ink) } },
                actions = { TextButton(onClick = { settings = true }) { Text("Aa", color = ink) } }
            )
        },
        bottomBar = {
            if (!loading && !failed) BottomAppBar(containerColor = paper) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = previous != null, onClick = {
                        previous?.let { navController.navigateOnce(PartScreen(it, storyId)) { popUpTo<PartScreen> { inclusive = true } } }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous chapter", tint = ink.copy(alpha = if (previous != null) 1f else 0.3f)) }
                    TextButton(enabled = client.loggedIn && !voting, onClick = {
                        scope.launch {
                            voting = true
                            try {
                                attempt {
                                    val result = part?.toggleVote()
                                    if (result == null) error("Vote not accepted")
                                    voted = part?.data?.voted == true
                                    voteCount = result.votes
                                }.onFailure { snackbar.showSnackbar("Couldn’t update your vote. Try again.") }
                            } finally { voting = false }
                        }
                    }) { Text("${if (voted) "♥" else "♡"} ${voteCount ?: 0}", color = ink) }
                    TextButton(onClick = { showComments.value = true }) { Text("Comments", color = ink) }
                    IconButton(enabled = next != null, onClick = {
                        next?.let { navController.navigateOnce(PartScreen(it, storyId)) { popUpTo<PartScreen> { inclusive = true } } }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next chapter", tint = ink.copy(alpha = if (next != null) 1f else 0.3f)) }
                }
            }
        }
    ) { padding ->
        when {
            loading -> Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.Center) { LoadingState() }
            failed -> Column(Modifier.padding(padding)) { ErrorState("This chapter couldn’t be opened. It may be unavailable, or your connection was interrupted.") { retry++ } }
            else -> LazyColumn(state = listState, modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item { Text(part?.data?.title ?: "Untitled chapter", color = ink, style = MaterialTheme.typography.headlineLarge, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif) }
                items(paragraphs) { paragraph ->
                    Text(paragraph.trim(), color = ink, fontSize = fontSize.sp, lineHeight = (fontSize * 1.7f).sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                }
                item { Text(if (next == null) "You’ve reached the end of this chapter." else "Another chapter awaits →", color = ink, modifier = Modifier.padding(vertical = 24.dp)) }
            }
        }
    }
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Make yourself comfortable", style = MaterialTheme.typography.titleLarge)
            Text("Text size · $fontSize")
            androidx.compose.material3.Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() }, valueRange = 14f..30f, steps = 15)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Paper", "Sepia", "Night").forEach { name ->
                    androidx.compose.material3.FilterChip(selected = appearance == name, onClick = { appearance = name }, label = { Text(name) })
                }
            }
        }
    }
    if (showComments.value) part?.let { CommentsModal(showComments, it, client, navController) }
}

@Composable
fun NumberIconButton(icon: ImageVector, number: Int?, contentDescription: String = "", onClick: () -> Unit = {}) {
    val iconBtnColors = IconButtonDefaults.iconButtonColors()

    val colors = ButtonColors(
        containerColor = iconBtnColors.containerColor,
        contentColor = iconBtnColors.contentColor,
        disabledContentColor = iconBtnColors.disabledContentColor,
        disabledContainerColor = iconBtnColors.disabledContainerColor
    )

    TextButton(colors = colors, onClick = onClick) {
        Column (verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = contentDescription)
            if (number != null)
                Text(getFormattedNumber(number))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommentsModal(showModal: MutableState<Boolean>, part: Part, client: Wattpad, navController: NavController) {
    val modalState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    val lazyColState = rememberLazyListState()

    var commentText by remember {
        mutableStateOf("")
    }

    var commentSent by remember {
        mutableStateOf(true)
    }

    val commentsState: MutableState<List<Comment>> = remember {
        mutableStateOf(listOf())
    }

    val comments = commentsState.value

    var commentsLeft by remember {
        mutableStateOf(false)
    }

    var after: String? by remember {
        mutableStateOf(null)
    }

    var commentsLoading by remember {
        mutableStateOf(false)
    }

    var commentsFailed by remember { mutableStateOf(false) }
    var commentsRetry by remember { mutableIntStateOf(0) }
    var sending by remember { mutableStateOf(false) }

    ModalBottomSheet(sheetState = modalState, onDismissRequest = { showModal.value = false }) {
        LaunchedEffect(after, commentsRetry) {
            commentsLoading = true
            commentsFailed = false
            attempt {
                val (commentsAfter, fetchedComments) = part.fetchComments(after = after)
                commentsLeft = commentsAfter
                commentsState.value = (commentsState.value + fetchedComments).distinctBy { it.data.commentId.resourceId }
            }.onFailure { commentsFailed = true }
            commentsLoading = false
        }
        if (commentsFailed) ErrorState("Comments couldn’t be loaded.") { commentsRetry++ }

        if (commentsLoading)
            Row (
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
            }
        else
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth(),
                state = lazyColState
            ) {
                stickyHeader {
                    TextField(
                        value = commentText,
                        enabled = client.loggedIn,
                        onValueChange = { commentText = it },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = {
                            Text(stringResource(id = R.string.write_comment))
                        },
                        keyboardActions = KeyboardActions(onSend = {}),
                        trailingIcon = {
                            IconButton(
                                enabled = client.loggedIn && commentText.isNotBlank() && !sending,
                                onClick = {
                                    coroutineScope.launch {
                                        sending = true
                                        try {
                                            val sentComment = attempt { part.comment(commentText.trim()) }.getOrNull()
                                            commentSent = sentComment != null
                                            if (sentComment != null) {
                                                commentsState.value = listOf(sentComment) + commentsState.value
                                                commentText = ""
                                            }
                                        } finally { sending = false }
                                    }

                                    focusManager.clearFocus(force = true)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                            }
                        },
                        supportingText = {
                            if (!commentSent)
                                Text(stringResource(id = R.string.comment_error))
                        },
                        isError = !commentSent
                    )
                }
                if (commentsLeft && !commentsFailed) item {
                    TextButton(onClick = { after = comments.lastOrNull()?.data?.commentId?.resourceId }) { Text("Load more comments") }
                }
                items(comments) {
                    CommentRow(navController = navController, comment = it, client = client, commentsState = commentsState)
                }
            }
    }
}

@Composable
fun CommentRow(navController: NavController, comment: Comment, commentsState: MutableState<List<Comment>>, modifier: Modifier = Modifier, client: Wattpad) {
    val coroutineScope = rememberCoroutineScope()

    var showReplies by remember {
        mutableStateOf(false)
    }

    var repliesLoading by remember {
        mutableStateOf(true)
    }

    var replies: List<Comment> by remember {
        mutableStateOf(listOf())
    }

    val showDeleteDialogState = remember {
        mutableStateOf(false)
    }

    val deletionErrState = remember {
        mutableStateOf(false)
    }

    var commentLiked by remember {
        mutableStateOf(comment.data.sentiments.like?.interaction != null)
    }

    var likeCount by remember {
        mutableIntStateOf(comment.data.sentiments.like?.count ?: 0)
    }

    LaunchedEffect(key1 = showReplies) {
        if (showReplies && replies.isEmpty()) {
            repliesLoading = true
            attempt { replies = comment.fetchReplies() }.onFailure { deletionErrState.value = true }
            repliesLoading = false
        }
    }

    Row (
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            AsyncImage(
                model = comment.data.user.avatar,
                contentDescription = stringResource(id = R.string.profile_picture),
                modifier = Modifier
                    .clip(CircleShape)
                    .height(40.dp)
                    .clickable { navController.navigateOnce(ProfileScreen(comment.data.user.username)) }
            )
        }
        Column (modifier = Modifier.weight(1F)){
            Row  (modifier = Modifier.fillMaxWidth()){
                Column (modifier = Modifier.weight(1F)) {
                    Text(
                        comment.data.user.username,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        comment.data.text
                    )
                    Text(
                        DateFormat.getDateFormat(LocalContext.current).format(comment.data.created),
                        fontWeight = FontWeight.Thin
                    )

                    if (comment.data.replyCount > 0)
                        TextButton(
                            onClick = { showReplies = !showReplies }
                        ) {
                            Text("${if (showReplies) stringResource(R.string.hide) else stringResource(R.string.show)} ${comment.data.replyCount} ${stringResource(id = R.string.replies)}")
                        }
                }
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NumberIconButton(icon = ImageVector.vectorResource(id = if (commentLiked) R.drawable.baseline_favorite_24 else R.drawable.outline_favorite_24), number = likeCount) {
                        if (client.loggedIn)
                            coroutineScope.launch {
                                if (commentLiked) {
                                    if(attempt { comment.unlike() }.getOrNull() == true) {
                                        commentLiked = false
                                        likeCount -= 1
                                    }
                                } else {
                                    if(attempt { comment.like() }.getOrNull() == true) {
                                        commentLiked = true
                                        likeCount += 1
                                    }
                                }
                            }
                    }
                    if (comment.data.user.username == client.username)
                        IconButton(onClick = {
                            showDeleteDialogState.value = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_icon))
                        }
                }
            }
            if (deletionErrState.value)
                Text(stringResource(id = R.string.comment_delete_err), color = MaterialTheme.colorScheme.error)
            if (showReplies) {
                if (repliesLoading)
                    CircularProgressIndicator()
                else {
                    for (reply in replies) {
                        CommentRow(navController = navController, comment = reply, client = client, commentsState = commentsState)
                    }
                }
            }
        }
    }

    if (showDeleteDialogState.value)
        DeleteCommentDialog(showDialogState = showDeleteDialogState, commentToDel = comment, errState = deletionErrState, commentsState = commentsState, coroutineScope)
}

@Composable
fun DeleteCommentDialog(showDialogState: MutableState<Boolean>, commentToDel: Comment, errState: MutableState<Boolean>, commentsState: MutableState<List<Comment>>, coroutineScope: CoroutineScope) {
    AlertDialog(onDismissRequest = { showDialogState.value = false },
        confirmButton = {
            TextButton(onClick = {
                coroutineScope.launch {
                    showDialogState.value = false
                    val res = attempt { commentToDel.delete() }.getOrNull()

                    if (res != true) {
                        errState.value = true
                    }
                    else {
                        errState.value = false
                        commentsState.value = commentsState.value.minus(commentToDel)
                    }
                }
            }) {
                Text(stringResource(id = R.string.delete))
            }
        },
        text = {Text(stringResource(id = R.string.comment_deletion_confirm))}
    )
}

@Serializable
data class PartScreen (
    val partId: Int,
    val storyId: Int
)