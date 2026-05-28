package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.ChatDao
import com.example.data.model.Contact
import com.example.data.model.ChatMessage
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ChatRepository(private val context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "instachat_database"
    ).fallbackToDestructiveMigration().build()

    private val chatDao: ChatDao = db.chatDao()

    val contacts: Flow<List<Contact>> = chatDao.getAllContactsFlow().flowOn(Dispatchers.IO)
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessagesFlow().flowOn(Dispatchers.IO)
    val posts: Flow<List<SocialPost>> = chatDao.getAllPostsFlow().flowOn(Dispatchers.IO)

    fun getMessagesForContact(contactId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForContactFlow(contactId).flowOn(Dispatchers.IO)
    }

    suspend fun insertMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(message)
    }

    suspend fun toggleLikePost(postId: Long) = withContext(Dispatchers.IO) {
        val allPosts = chatDao.getAllPostsFlow().first()
        val post = allPosts.find { it.id == postId }
        if (post != null) {
            val updated = post.copy(
                isLiked = !post.isLiked,
                likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
            )
            chatDao.updatePost(updated)
        }
    }

    suspend fun addPost(post: SocialPost) = withContext(Dispatchers.IO) {
        chatDao.insertPost(post)
    }

    suspend fun addContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        chatDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        chatDao.updateContact(contact)
    }

    suspend fun clearChatMessages(contactId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForContact(contactId)
    }

    fun getActiveStories(since: Long): Flow<List<StoryItem>> {
        return chatDao.getActiveStoriesFlow(since).flowOn(Dispatchers.IO)
    }

    suspend fun insertStory(story: StoryItem): Long = withContext(Dispatchers.IO) {
        chatDao.insertStory(story)
    }

    // Populate data if empty
    suspend fun prepopulateIfNeeded() = withContext(Dispatchers.IO) {
        val existingContacts = chatDao.getAllContactsFlow().first()
        if (existingContacts.isEmpty()) {
            val contactList = listOf(
                Contact(
                    id = 1,
                    name = "Jane Cooper",
                    userName = "jane_coop",
                    avatarColorHex = "FFDE6B5C", // Terracotta
                    statusMessage = "Design is how it works 🎨✨",
                    isOnline = true,
                    storyMediaUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=500",
                    storyCaption = "Wandering around Kyoto under the cherry blossoms! Pink as far as the eye can see 🌸🇯🇵"
                ),
                Contact(
                    id = 2,
                    name = "Alex Rivera",
                    userName = "alex_r",
                    avatarColorHex = "FF2A9D8F", // Teal
                    statusMessage = "Code, compound, and lift. 🏋️‍♂️💻",
                    isOnline = true,
                    storyMediaUrl = "https://images.unsplash.com/photo-1548690312-e3b507d8c110?w=500",
                    storyCaption = "Early morning climb. Grind matches the elevation! 🧗‍♂️🌄"
                ),
                Contact(
                    id = 3,
                    name = "Karthik Nair",
                    userName = "k_nair",
                    avatarColorHex = "FF264653", // Dark slate
                    statusMessage = "Quiet minds solve grand challenges. ⚙️🧠",
                    isOnline = false
                ),
                Contact(
                    id = 4,
                    name = "Elena Rostova",
                    userName = "elena_r",
                    avatarColorHex = "FFE76F51", // Coral Orange
                    statusMessage = "Chasing paint strokes in Paris 🖌️🎨",
                    isOnline = true,
                    storyMediaUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=500",
                    storyCaption = "Studio view. Finding sanity in chaotic strokes today. 🎨✨"
                ),
                Contact(
                    id = 5,
                    name = "TwoChats AI Bot",
                    userName = "twochats_bot",
                    avatarColorHex = "FF6C5DD3", // Interactive Blue-Violet
                    statusMessage = "AI assistant at your service ✨🤖",
                    isOnline = true
                ),
                Contact(
                    id = 6,
                    name = "TwoChats Design Suite",
                    userName = "group_design",
                    avatarColorHex = "FF6750A4", // Purple Accent
                    statusMessage = "Designing beautiful interfaces with balance 🎨📐",
                    isOnline = true,
                    isGroup = true,
                    groupParticipants = "Jane Cooper, Alex Rivera, Elena Rostova, You"
                )
            )
            chatDao.insertContacts(contactList)

            // Setup welcome messages
            val messages = listOf(
                ChatMessage(
                    threadContactId = 5,
                    text = "Welcome to TwoChats! This app merges the gorgeous social features of Instagram Stories & Feed with fast, client-side WhatsApp Direct Messages. 📩💬",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 5,
                    text = "You can chat with me, tap stories, swipe the Instagram Explore Feed, check profiles, and start custom conversations with new contacts! Ask me anything.",
                    timestamp = System.currentTimeMillis() - 1800000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 1,
                    text = "Hey! Did you take a look at the custom Compose layouts we discussed yesterday? The visual depth is crazy!",
                    timestamp = System.currentTimeMillis() - 7200000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 1,
                    text = "Absolutely, they look super smooth! I'll test them soon.",
                    timestamp = System.currentTimeMillis() - 6000000,
                    isFromMe = true,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 1,
                    text = "Let me know when you run them on a high refresh device. Have a look at my status today, Kyoto is unreal! 🌸",
                    timestamp = System.currentTimeMillis() - 5000000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 2,
                    text = "Yo bro, you down for gym/workout at 6:30 PM?",
                    timestamp = System.currentTimeMillis() - 10800000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 6,
                    senderName = "Jane Cooper",
                    text = "Hey everyone! Let's align on the new slate-colored minimalist styles. 🎨📐",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isFromMe = false,
                    status = "READ"
                ),
                ChatMessage(
                    threadContactId = 6,
                    senderName = "Alex Rivera",
                    text = "I'm loving the Geometric Balance theme. The contrast is perfect for late-night review sessions! 🏋️‍♂️💻",
                    timestamp = System.currentTimeMillis() - 1800000,
                    isFromMe = false,
                    status = "READ"
                )
            )
            for (msg in messages) {
                chatDao.insertMessage(msg)
            }

            // Setup Instagram-style visual feed posts
            val initialPosts = listOf(
                SocialPost(
                    id = 1,
                    authorName = "Jane Cooper",
                    authorHandle = "jane_coop",
                    authorAvatarColorHex = "FFDE6B5C",
                    imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800",
                    caption = "Lost in the golden autumn leaves of Kyoto 🍁 Kyoto's visual palette is so pure. Can't wait to sketch this landscape!",
                    likesCount = 124,
                    isLiked = true,
                    timeAgo = "2 hours ago"
                ),
                SocialPost(
                    id = 2,
                    authorName = "Alex Rivera",
                    authorHandle = "alex_r",
                    authorAvatarColorHex = "FF2A9D8F",
                    imageUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
                    caption = "Sunday morning session. Push hard when nobody is monitoring! Consistency always beats raw intensity. Let's get it! 🔥💪 #fitness #motivation",
                    likesCount = 56,
                    isLiked = false,
                    timeAgo = "5 hours ago"
                ),
                SocialPost(
                    id = 3,
                    authorName = "Elena Rostova",
                    authorHandle = "elena_r",
                    authorAvatarColorHex = "FFE76F51",
                    imageUrl = "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800",
                    caption = "Mixed acrylic studies from the workshop in Montmartre. Captured the contrast of raw light on textured canvas. 🎨🖌️🗼",
                    likesCount = 89,
                    isLiked = false,
                    timeAgo = "1 day ago"
                )
            )
            chatDao.insertPosts(initialPosts)
        }
    }
}
