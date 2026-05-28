package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Contact
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.TabState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.text.BasicTextField

// Styling accent colors
val InstaGradient = Brush.sweepGradient(
    listOf(
        Color(0xFF833AB4), // Instagram Indigo/Purple
        Color(0xFFFD1D1D), // Pink/Red
        Color(0xFFF77737), // Orange
        Color(0xFF833AB4)  // Wrap
    )
)

val GeometricPurple = Color(0xFF6750A4)
val GeometricDarkPurple = Color(0xFF21005D)
val GeometricLightPurple = Color(0xFFEADDFF)
val GeometricLightBlue = Color(0xFFD3E3FD)
val GeometricDarkBlue = Color(0xFF041E49)
val GeometricGreyText = Color(0xFF49454F)
val GeometricLavendarBg = Color(0xFFE7E0EC)
val GeometricInputBg = Color(0xFFF3EDF7)
val GeometricCanvasBg = Color(0xFFFEF7FF)

val WhatsappGreen = Color(0xFF6750A4)
val BubbleMeLight = GeometricLightBlue
val BubbleOtherLight = GeometricLightPurple
val BubbleMeDark = Color(0xFF041E49)
val BubbleOtherDark = Color(0xFF21005D)

@Composable
fun InstaChatApp(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val selectedContactId by viewModel.selectedContactId.collectAsState()
    val selectedStoryContactId by viewModel.selectedStoryContactId.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    val currentSelectedContact = contacts.find { it.id == selectedContactId }
    val currentStoryContact = contacts.find { it.id == selectedStoryContactId }

    var activeStoryToView by remember { mutableStateOf<StoryItem?>(null) }
    var showStoryComposer by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main layer
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentSelectedContact == null) {
                // Showing dashboard (Threads, Feed, Profile)
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenStoryComposer = { showStoryComposer = true },
                    onViewCustomStory = { activeStoryToView = it }
                )
            } else {
                // Detail WhatsApp Thread Screen
                ChatDetailScreen(
                    contact = currentSelectedContact,
                    viewModel = viewModel,
                    onBack = { viewModel.selectContact(null) }
                )
            }
        }

        // Full Screen Story Viewer for standard contacts
        AnimatedVisibility(
            visible = currentStoryContact != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            if (currentStoryContact != null) {
                StoryViewerScreen(
                    contact = currentStoryContact,
                    customStory = null,
                    onClose = { viewModel.selectStory(null) }
                )
            }
        }

        // Full Screen Story Viewer for drawn/written stories
        AnimatedVisibility(
            visible = activeStoryToView != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            if (activeStoryToView != null) {
                StoryViewerScreen(
                    contact = null,
                    customStory = activeStoryToView,
                    onClose = { activeStoryToView = null }
                )
            }
        }

        // Story painting and writing composer screen overlay
        AnimatedVisibility(
            visible = showStoryComposer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (showStoryComposer) {
                StoryComposerScreen(
                    onDismiss = { showStoryComposer = false },
                    onPublish = { text, colorHex, drawingJson ->
                        viewModel.publishStory(text, colorHex, drawingJson)
                        showStoryComposer = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ChatViewModel,
    onOpenStoryComposer: () -> Unit,
    onViewCustomStory: (StoryItem) -> Unit
) {
    val activeTab by viewModel.activeTab.collectAsState()
    var showNewContactDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    val contacts by viewModel.contacts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Two",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                brush = InstaGradient
                            )
                        )
                        Text(
                            text = "Chats",
                            fontWeight = FontWeight.ExtraBold,
                            color = WhatsappGreen,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showNewGroupDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Create Group Chat",
                            tint = WhatsappGreen
                        )
                    }
                    IconButton(onClick = { showNewContactDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == TabState.CHATS,
                    onClick = { viewModel.setTab(TabState.CHATS) },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WhatsappGreen,
                        selectedTextColor = WhatsappGreen,
                        indicatorColor = WhatsappGreen.copy(alpha = 0.12f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == TabState.EXPLORE,
                    onClick = { viewModel.setTab(TabState.EXPLORE) },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD62976),
                        selectedTextColor = Color(0xFFD62976),
                        indicatorColor = Color(0xFFD62976).copy(alpha = 0.12f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == TabState.PROFILE,
                    onClick = { viewModel.setTab(TabState.PROFILE) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == TabState.CHATS) {
                FloatingActionButton(
                    onClick = { showNewContactDialog = true },
                    containerColor = WhatsappGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Message, contentDescription = "New Chat")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                TabState.CHATS -> ChatTabContent(
                    viewModel = viewModel,
                    onOpenStoryComposer = onOpenStoryComposer,
                    onViewCustomStory = onViewCustomStory
                )
                TabState.EXPLORE -> ExploreTabContent(viewModel = viewModel)
                TabState.PROFILE -> ProfileTabContent(viewModel = viewModel)
            }
        }
    }

    if (showNewContactDialog) {
        NewContactDialog(
            onDismiss = { showNewContactDialog = false },
            onConfirm = { name, handle, bio, color ->
                viewModel.createNewContact(name, handle, bio, color)
                showNewContactDialog = false
            }
        )
    }

    if (showNewGroupDialog) {
        CreateGroupDialog(
            contacts = contacts,
            onDismiss = { showNewGroupDialog = false },
            onConfirm = { groupName, colorHex, participantsList ->
                viewModel.createGroupChat(groupName, colorHex, participantsList.joinToString(", "))
                showNewGroupDialog = false
            }
        )
    }
}

// ---------------- Chats Tab (WhatsApp Style with Instagram Stories) ----------------

@Composable
fun ChatTabContent(
    viewModel: ChatViewModel,
    onOpenStoryComposer: () -> Unit,
    onViewCustomStory: (StoryItem) -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val typingContactId by viewModel.typingContactId.collectAsState()
    val activeStories by viewModel.activeStories.collectAsState()

    // Filter contacts that have active stories
    val storyContacts = contacts.filter { it.storyMediaUrl != null }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Instagram Stories Section
        item {
            Text(
                text = "Recent Updates (Stories)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Self Story (Placeholder)
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onOpenStoryComposer() }
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarBadge(
                                colorHex = "FF6750A4", // Purple
                                initials = "ME",
                                size = 64.dp,
                                hasStory = activeStories.any { it.userId == 0L },
                                borderColor = WhatsappGreen
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(WhatsappGreen)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add status",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("My Status", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                }

                // Render active users/drawn stories
                items(activeStories) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onViewCustomStory(story) }
                    ) {
                        AvatarBadge(
                            colorHex = story.avatarColorHex,
                            initials = story.userName.take(2).uppercase(),
                            size = 64.dp,
                            hasStory = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = story.userName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active Mock Stories Rows
                items(storyContacts) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.selectStory(contact.id) }
                    ) {
                        AvatarBadge(
                            colorHex = contact.avatarColorHex,
                            initials = contact.name.take(2).uppercase(),
                            size = 64.dp,
                            hasStory = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.name.split(" ").firstOrNull() ?: contact.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // WhatsApp Style Contact Chats
        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WhatsappGreen)
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Conversations",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(contacts) { contact ->
                // Find thread message history details
                val threadMsgs = allMessages.filter { it.threadContactId == contact.id }
                val latestMsg = threadMsgs.maxByOrNull { it.timestamp }
                val unreadCount = if (latestMsg != null && !latestMsg.isFromMe && latestMsg.status != "READ") 1 else 0
                val isTyping = typingContactId == contact.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectContact(contact.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar bubble
                    AvatarBadge(
                        colorHex = contact.avatarColorHex,
                        initials = contact.name.take(2).uppercase(),
                        size = 56.dp,
                        hasStory = contact.storyMediaUrl != null,
                        isOnline = contact.isOnline
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name and last message detail Column
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Timestamp mapping
                            val timeStr = latestMsg?.let {
                                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                sdf.format(Date(it.timestamp))
                            } ?: ""
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (unreadCount > 0) WhatsappGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Status indicators
                            if (isTyping) {
                                Text(
                                    text = "typing...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WhatsappGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                if (latestMsg != null) {
                                    if (latestMsg.isFromMe) {
                                        val tickColor = if (latestMsg.status == "READ") Color(0xFF34B7F1) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        Icon(
                                            imageVector = if (latestMsg.status == "SENT") Icons.Default.Check else Icons.Default.DoneAll,
                                            contentDescription = latestMsg.status,
                                            tint = tickColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    val previewText = when {
                                        latestMsg.audioDurationSec != null -> "🎤 Voice message (${latestMsg.audioDurationSec}s)"
                                        latestMsg.imageUrl != null -> "📷 Photo attachment"
                                        else -> latestMsg.text
                                    }

                                    Text(
                                        text = previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = contact.statusMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Unread badging
                    if (unreadCount > 0 && !isTyping) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsappGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unreadCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 88.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ---------------- Explore Tab (Instagram Style Visual Feed) ----------------

@Composable
fun ExploreTabContent(viewModel: ChatViewModel) {
    val posts by viewModel.posts.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Explore Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF833AB4), Color(0xFFF77737))
                        )
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trending around your visual network",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No posts found. Start sharing in Profile!", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(posts) { post ->
            InstagramPostCard(post = post, onLikeToggle = { viewModel.toggleLikePost(post.id) })
        }
    }
}

@Composable
fun InstagramPostCard(
    post: SocialPost,
    onLikeToggle: () -> Unit
) {
    var animateLike by remember { mutableStateOf(false) }
    val scaleFactor by animateDpAsState(
        targetValue = if (animateLike) 32.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        finishedListener = { animateLike = false }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarBadge(
                    colorHex = post.authorAvatarColorHex,
                    initials = post.authorName.take(2).uppercase(),
                    size = 36.dp,
                    hasStory = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "@${post.authorHandle}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = post.timeAgo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Post Image with loading support and fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.DarkGray)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!post.isLiked) {
                                    onLikeToggle()
                                }
                                animateLike = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { /* showing loader fallback */ },
                    onError = { /* fallback vector check */ }
                )

                // Double tap like heart popup overlay
                if (animateLike) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            // Interactive Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = {
                    animateLike = true
                    onLikeToggle()
                }) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color(0xFFF91B80) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(scaleFactor)
                    )
                }

                IconButton(onClick = { /* Simulated comment sheet */ }) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { /* Share link */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Likes and Captions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${post.likesCount} likes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${post.authorHandle}: ",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ---------------- Profile Tab (Instagram Style Personal Creation Center) ----------------

@Composable
fun ProfileTabContent(viewModel: ChatViewModel) {
    val posts by viewModel.posts.collectAsState()
    var postCaption by remember { mutableStateOf("") }
    var postImageUrl by remember { mutableStateOf("") }
    var expandedNewPost by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val myPosts = posts.filter { it.authorHandle == "dev_artist" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Biography Area
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarBadge(
                    colorHex = "FF4A90E2",
                    initials = "ME",
                    size = 96.dp,
                    hasStory = false,
                    isOnline = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Studio Profile",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "@dev_artist",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileStatItem(count = "${myPosts.size}", label = "Posts")
                    ProfileStatItem(count = "4.8K", label = "Followers")
                    ProfileStatItem(count = "682", label = "Following")
                }

                Text(
                    text = "Creative Artist & Jetpack Compose engineer. Building gorgeous, fully-functional mobile systems with elegant local Room databases! 🚀🖌️",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // New Post Creator Area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Share to Instagram Feed 📸",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { expandedNewPost = !expandedNewPost }) {
                            Icon(
                                imageVector = if (expandedNewPost) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle"
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedNewPost) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = postCaption,
                                onValueChange = { postCaption = it },
                                label = { Text("What is on your mind?") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = postImageUrl,
                                onValueChange = { postImageUrl = it },
                                label = { Text("Image URL (Unsplash/Web) or blank for random") },
                                placeholder = { Text("https://example.com/photo.jpeg") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.createNewPost(postCaption, postImageUrl)
                                    postCaption = ""
                                    postImageUrl = ""
                                    expandedNewPost = false
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD62976)
                                )
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Post to Feed")
                            }
                        }
                    }
                }
            }
        }

        // Display user posts grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "My Gallery Posts",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (myPosts.isEmpty()) {
            item {
                Text(
                    text = "No personal posts yet. Share something above!",
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Group our custom posts into grids of 3 columns
            val chunks = myPosts.chunked(3)
            items(chunks) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until 3) {
                        val item = chunk.getOrNull(i)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.LightGray)
                        ) {
                            if (item != null) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = "Profile post thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Chat Detail View (WhatsApp Styled Conversation Page) ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    contact: Contact,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.activeMessages.collectAsState()
    val isTyping = viewModel.typingContactId.collectAsState().value == contact.id
    val isDecryptionEnabled by viewModel.isDecryptionEnabled.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var simulatedVoiceCount by remember { mutableStateOf(1) }
    var showManageGroupDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to latest when messages modify
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { /* Tap profile */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBadge(
                            colorHex = contact.avatarColorHex,
                            initials = contact.name.take(2).uppercase(),
                            size = 40.dp,
                            hasStory = contact.storyMediaUrl != null,
                            isOnline = contact.isOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val infoText = if (isTyping) {
                                "typing..."
                            } else if (contact.isGroup) {
                                "Participants: ${contact.groupParticipants ?: "None"}"
                            } else if (contact.isOnline) {
                                "online"
                            } else {
                                "offline"
                            }
                            Text(
                                text = infoText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isTyping || contact.isOnline) WhatsappGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (contact.isGroup) {
                        IconButton(onClick = { showManageGroupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Manage Group Participants",
                                tint = WhatsappGreen
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.deleteChat(contact.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Draw continuous WhatsApp subtle custom geometric gradient background
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFE5DDD5), Color(0xFFF7F5F0))
                        )
                    )
                }
        ) {
            // Interactive E2E Encryption Status & Ciphertext Switch indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8DEF8))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isDecryptionEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "E2E Lock Status",
                        tint = WhatsappGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDecryptionEnabled) "🔒 Client E2E Active: Plaintext Decrypted" else "⚠️ intercepted: Raw DB Ciphertext Shown",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF21005D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = { viewModel.toggleDecryptionDisplay() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isDecryptionEnabled) "Show Cipher" else "Decrypt Text",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsappGreen
                    )
                }
            }

            // Conversation streams
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Encryption description notice
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCF5E3))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔐 All messages are protected with state-of-the-art client-side end-to-end symmetric encryption. Keys are never transmitted to any central database server.",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF504838),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubbleItem(message = msg, isDecrypted = isDecryptionEnabled)
                }

                // If typing, draw typing bubble
                if (isTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp))
                                .background(GeometricLightPurple)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .widthIn(max = 100.dp)
                        ) {
                            DotTypingIndicator()
                        }
                    }
                }
            }

            // Bottom control system bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Typing Row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3EDF7))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Emoji placeholder */ }) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = "Emojis",
                            tint = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Message...", color = Color(0xFF49454F)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputMessage.isNotBlank()) {
                                    viewModel.sendMessage(inputMessage)
                                    inputMessage = ""
                                }
                            }
                        )
                    )

                    IconButton(onClick = { showAttachmentSheet = !showAttachmentSheet }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send round key button
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendMessage(inputMessage)
                            inputMessage = ""
                        } else {
                            // Empty input triggers a simulated Voice Message recording instantly for high interactive fidelity!
                            viewModel.sendMessage(
                                text = "",
                                voiceSec = (3..12).random()
                            )
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GeometricLightPurple)
                ) {
                    Icon(
                        imageVector = if (inputMessage.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = GeometricDarkPurple
                    )
                }
            }

            // Animated Visual Attachment Panel
            AnimatedVisibility(visible = showAttachmentSheet) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOptionItem(
                            icon = Icons.Default.Photo,
                            color = Color(0xFFA020F0),
                            label = "Photo attachment",
                            onClick = {
                                viewModel.sendMessage(
                                    text = "Look at this spectacular painting sample! 🌇🎨",
                                    imageUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800"
                                )
                                showAttachmentSheet = false
                            }
                        )
                        AttachmentOptionItem(
                            icon = Icons.Default.LocationOn,
                            color = Color(0xFF1E90FF),
                            label = "Share Location",
                            onClick = {
                                viewModel.sendMessage("📍 Shared Location: Tokyo Digital Tower, Minato, JP")
                                showAttachmentSheet = false
                            }
                        )
                        AttachmentOptionItem(
                            icon = Icons.Default.ContactPage,
                            color = Color(0xFFFF8C00),
                            label = "Contact Detail",
                            onClick = {
                                viewModel.sendMessage("👤 Shared Contact: Sarah Mercer (@sarah_m, Admin)")
                                showAttachmentSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showManageGroupDialog) {
        ManageParticipantsDialog(
            currentParticipants = contact.groupParticipants ?: "",
            onDismiss = { showManageGroupDialog = false },
            onConfirm = { updated ->
                viewModel.addRemoveParticipant(contact.id, updated)
                showManageGroupDialog = false
            }
        )
    }
}

@Composable
fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage, isDecrypted: Boolean = true) {
    val isMe = message.isFromMe
    val align = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) {
        if (isSystemInDarkTheme()) BubbleMeDark else BubbleMeLight
    } else {
        if (isSystemInDarkTheme()) BubbleOtherDark else BubbleOtherLight
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    val bubbleTextColor = if (isMe) {
        if (isSystemInDarkTheme()) Color.White else Color(0xFF041E49)
    } else {
        if (isSystemInDarkTheme()) Color.White else Color(0xFF21005D)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 2.dp),
            shadowElevation = 0.5.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // If group chat message and not from me, show sender name
                if (message.senderName != null && !isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = WhatsappGreen,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // If message contains visual image attachments
                if (message.imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Attachment",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // If voice message
                if (message.audioDurationSec != null) {
                    VoiceMessagePlayer(duration = message.audioDurationSec)
                } else if (message.text.isNotEmpty()) {
                    val displayText = if (isDecrypted) {
                        message.text
                    } else {
                        message.encryptedText ?: "[Encrypted Payload]"
                    }
                    val isCustomKey = !isDecrypted && message.isEncrypted
                    
                    Text(
                        text = displayText,
                        style = if (isCustomKey) {
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (isCustomKey) Color.Red else bubbleTextColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Time and tick Row
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2E Protected",
                            tint = WhatsappGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    val formatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                    Text(
                        text = formatted,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.61f)
                    )
                    if (isMe) {
                        val tickColor = if (message.status == "READ") GeometricPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        Icon(
                            imageVector = if (message.status == "SENT") Icons.Default.Check else Icons.Default.DoneAll,
                            contentDescription = message.status,
                            tint = tickColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(duration: Int) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (progress < 1.0f) {
                delay(100)
                progress += (0.1f / duration)
            }
            isPlaying = false
            progress = 0f
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WhatsappGreen.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = WhatsappGreen
            )
        }

        // Custom animated sound waves canvas simulation
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val waveCount = 20
                val barWidth = 4.dp.toPx()
                val gap = 2.dp.toPx()
                val verticalScale = size.height

                for (i in 0 until waveCount) {
                    // Make wave shapes pseudo-random
                    val waveHeight = when (i % 5) {
                        0 -> 0.4f
                        1 -> 0.8f
                        2 -> 0.6f
                        3 -> 0.9f
                        else -> 0.3f
                    } * verticalScale

                    val x = i * (barWidth + gap)
                    val y = (verticalScale - waveHeight) / 2f
                    val animatedProgressIdx = (progress * waveCount).toInt()

                    // Bar colors depend on current playback progress
                    val barColor = if (i <= animatedProgressIdx && isPlaying) WhatsappGreen else Color.LightGray

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, waveHeight)
                    )
                }
            }
            DurationDetails(duration = duration, progress = progress)
        }
    }
}

@Composable
fun DurationDetails(duration: Int, progress: Float) {
    val totalSeconds = (duration * (1f - progress)).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Voice Note", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Story Viewer Screen (Instagram Custom Layer Overlay) ----------------

@Composable
fun StoryViewerScreen(
    contact: Contact?,
    customStory: StoryItem?,
    onClose: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    val storyDurationMs = 5000L

    val idKey = remember(contact, customStory) {
        contact?.id ?: customStory?.id ?: 0L
    }

    // Progress bar auto update
    LaunchedEffect(key1 = idKey) {
        val steps = 100
        val delayTime = storyDurationMs / steps
        for (i in 1..steps) {
            delay(delayTime)
            progress = i.toFloat() / steps
        }
        onClose()
    }

    // Restore paint points sequence for custom user story
    val lines = remember(customStory) {
        if (customStory != null) {
            try {
                customStory.drawingPointsJson.split("|").mapNotNull { strokeStr ->
                    if (strokeStr.isBlank()) null else {
                        strokeStr.split(";").mapNotNull { ptStr ->
                            val coords = ptStr.split(",")
                            if (coords.size == 2) {
                                Offset(coords[0].toFloat(), coords[1].toFloat())
                            } else null
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList<List<Offset>>()
            }
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() }
    ) {
        if (customStory != null) {
            // Draw gradient background wall
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6750A4), Color(0xFFD62976))
                        )
                    )
            )

            // Render painting strokes on top
            Canvas(modifier = Modifier.fillMaxSize()) {
                val brushColor = try {
                    Color(android.graphics.Color.parseColor("#${customStory.textOverlayColorHex}"))
                } catch(e: Exception) {
                    Color.White
                }
                for (line in lines) {
                    for (i in 0 until line.size - 1) {
                        try {
                            drawLine(
                                color = brushColor,
                                start = line[i],
                                end = line[i + 1],
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )
                        } catch (e: Exception) {}
                    }
                }
            }

            // Draw centralized text overlay caption
            if (customStory.textOverlay.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val brushColor = try {
                        Color(android.graphics.Color.parseColor("#${customStory.textOverlayColorHex}"))
                    } catch(e: Exception) {
                        Color.White
                    }
                    Text(
                        text = customStory.textOverlay,
                        color = brushColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }
        } else if (contact != null) {
            // Fullscreen Story Wallpaper from Contact URL
            AsyncImage(
                model = contact.storyMediaUrl,
                contentDescription = "Story Media Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark visual vignette top overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        // Dark visual vignette bottom overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // Story Top controls column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Instagram segmented story loading bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Author metadata layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarBadge(
                    colorHex = contact?.avatarColorHex ?: customStory?.avatarColorHex ?: "FF6750A4",
                    initials = (contact?.name ?: customStory?.userName ?: "ME").take(2).uppercase(),
                    size = 36.dp,
                    hasStory = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = contact?.name ?: customStory?.userName ?: "You",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "@${contact?.userName ?: "you_stories"} • Just now",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        // Story Caption details card on absolute bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .imePadding()
                .clickable(enabled = false) {}
        ) {
            if (contact != null) {
                Text(
                    text = contact.storyCaption ?: "Finding beautiful moments inside TwoChats!",
                    color = Color.White,
                    fontSize = 15.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Instagram-style message quick reply bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { /* Tap-typed reply simulation */ },
                    placeholder = { Text("Send reply...", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like story",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onClose() }
                )
            }
        }
    }
}

// ---------------- Dynamic Visual Components ----------------

@Composable
fun AvatarBadge(
    colorHex: String,
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    hasStory: Boolean = false,
    borderColor: Color? = null,
    isOnline: Boolean? = null
) {
    val contextColor = try {
        Color(android.graphics.Color.parseColor("#$colorHex"))
    } catch (e: Exception) {
        Color(0xFF6C5DD3)
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (hasStory) {
                        Modifier
                            .border(3.dp, InstaGradient, CircleShape)
                            .padding(4.dp)
                    } else if (borderColor != null) {
                        Modifier.border(1.dp, borderColor, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(contextColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp
            )
        }

        // Online dot status
        if (isOnline == true) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(WhatsappGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun BoxScope.fillModifierAlign(alignment: Alignment): Modifier {
    return Modifier.align(alignment)
}

@Composable
fun DotTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "Dots")
    val dotCount = 3
    val dotOffsets = List(dotCount) { index ->
        transition.animateValue(
            initialValue = 0.dp,
            targetValue = (-8).dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0.dp at 0
                    (-8).dp at 150
                    0.dp at 300
                    0.dp at 600
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(offsetMillis = index * 100)
            ),
            label = "Offset_$index"
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until dotCount) {
            Box(
                modifier = Modifier
                    .offset(y = dotOffsets[i].value)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.61f))
            )
        }
    }
}

// ---------------- Creating character / New chats sheets ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, handle: String, bio: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val colorsList = listOf("FFDE6B5C", "FF2A9D8F", "FF264653", "FFE76F51", "FF6C5DD3", "FF4A90E2")
    var selectedColorHex by remember { mutableStateOf(colorsList.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chat Character (Contact)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("Sarah Mercer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Aesthetic User handle") },
                    placeholder = { Text("sarah_m") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biography/Status") },
                    placeholder = { Text("Living out loud!") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "Aesthetic profile identity theme",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in colorsList) {
                        val parsed = Color(android.graphics.Color.parseColor("#$c"))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    2.dp,
                                    if (selectedColorHex == c) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorHex = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val realHandle = if (handle.isBlank()) name.replace(" ", "").lowercase() else handle
                        val realBio = if (bio.isBlank()) "Hey there! I am using InstaChat." else bio
                        onConfirm(name, realHandle, realBio, selectedColorHex)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
            ) {
                Text("Start Chatting")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- Create Group Chat Dialog ----------------

@Composable
fun CreateGroupDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, colorHex: String, participants: List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedParticipants = remember { mutableStateListOf<String>() }
    val colorsList = listOf("FF4A90E2", "FFE05252", "FF4CAF50", "FF9C27B0", "FFFF9800", "FF1DA1F2")
    var selectedColorHex by remember { mutableStateOf(colorsList.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group Chat", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g., Tech Team, Family") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "Select Participants",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )

                if (contacts.isEmpty()) {
                    Text(
                        text = "Add some contacts first to invite them to your team!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Box(modifier = Modifier.heightIn(max = 160.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(contacts) { contact ->
                                if (!contact.isGroup) {
                                    val isChecked = selectedParticipants.contains(contact.name)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isChecked) {
                                                    selectedParticipants.remove(contact.name)
                                                } else {
                                                    selectedParticipants.add(contact.name)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarBadge(
                                            colorHex = contact.avatarColorHex,
                                            initials = contact.name.take(2).uppercase(),
                                            size = 32.dp,
                                            hasStory = false
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked == true) {
                                                    selectedParticipants.add(contact.name)
                                                } else {
                                                    selectedParticipants.remove(contact.name)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = WhatsappGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Aesthetic identity theme",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in colorsList) {
                        val parsed = Color(android.graphics.Color.parseColor("#$c"))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    2.dp,
                                    if (selectedColorHex == c) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorHex = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        // Include self in participants always
                        val listWithSelf = mutableListOf("You")
                        listWithSelf.addAll(selectedParticipants)
                        onConfirm(groupName, selectedColorHex, listWithSelf)
                    }
                },
                enabled = groupName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- Manage Group Participants ----------------

@Composable
fun ManageParticipantsDialog(
    currentParticipants: String,
    onDismiss: () -> Unit,
    onConfirm: (updatedParticipants: String) -> Unit
) {
    var textValue by mutableStateOf(currentParticipants)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Group Participants", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Edit participants in your group. Enter comma-separated names of active users.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Participants list") },
                    placeholder = { Text("You, Sarah Mercer, Tony Hawk") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- Interactive Finger Drawing & Story Composer ----------------

@Composable
fun StoryComposerScreen(
    onDismiss: () -> Unit,
    onPublish: (textOverlay: String, colorHex: String, pointsJson: String) -> Unit
) {
    var textOverlay by remember { mutableStateOf("") }
    val brushColors = listOf("FFFFFFFF", "FF4A90E2", "FFFFCC00", "FF1DA1F2", "FFFF4081", "FF4CAF50")
    var selectedColorHex by remember { mutableStateOf("FFFFFFFF") }
    
    // Multi stroke list
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val allStrokes = remember { mutableStateListOf<List<Offset>>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Aesthetic Gradient base wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF6750A4), Color(0xFFD62976))
                    )
                )
        )

        // Capture finger canvas draw touch events
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke.clear()
                            currentStroke.add(offset)
                        },
                        onDragEnd = {
                            allStrokes.add(currentStroke.toList())
                            currentStroke.clear()
                        },
                        onDragCancel = {
                            currentStroke.clear()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentStroke.add(change.position)
                        }
                    )
                }
        ) {
            val drawColor = try {
                Color(android.graphics.Color.parseColor("#$selectedColorHex"))
            } catch (e: Exception) {
                Color.White
            }

            // Draw historical strokes
            for (stroke in allStrokes) {
                for (i in 0 until stroke.size - 1) {
                    drawLine(
                        color = drawColor,
                        start = stroke[i],
                        end = stroke[i + 1],
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw active building stroke
            if (currentStroke.isNotEmpty()) {
                for (i in 0 until currentStroke.size - 1) {
                    drawLine(
                        color = drawColor,
                        start = currentStroke[i],
                        end = currentStroke[i + 1],
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Overlay Interactive typing HUD centered
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val drawColor = try {
                Color(android.graphics.Color.parseColor("#$selectedColorHex"))
            } catch (e: Exception) {
                Color.White
            }
            BasicTextField(
                value = textOverlay,
                onValueChange = { textOverlay = it },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = drawColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (textOverlay.isEmpty()) {
                        Text(
                            text = "Tap to type a message...",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = drawColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Header controls layout containing Close and Undo buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Composer", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (allStrokes.isNotEmpty()) {
                    IconButton(
                        onClick = { allStrokes.removeAt(allStrokes.size - 1) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo stroke", tint = Color.White)
                    }
                }

                Button(
                    onClick = {
                        // Stringify visual coordinates lines back into DB structure
                        val serialized = allStrokes.joinToString("|") { stroke ->
                            stroke.joinToString(";") { "${it.x},${it.y}" }
                        }
                        onPublish(textOverlay, selectedColorHex, serialized)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen)
                ) {
                    Text("Publish 🚀", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }
            }
        }

        // Color Picker bar absolute bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Brush & Overlay Color selector",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (hex in brushColors) {
                    val parsed = Color(android.graphics.Color.parseColor("#$hex"))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .border(
                                3.dp,
                                if (selectedColorHex == hex) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .clickable { selectedColorHex = hex }
                    )
                }
            }
        }
    }
}

