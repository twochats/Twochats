package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Contact
import com.example.data.model.ChatMessage
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TabState {
    CHATS, EXPLORE, PROFILE
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    // State holding
    val contacts: StateFlow<List<Contact>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<SocialPost>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedContactId = MutableStateFlow<Long?>(null)
    val selectedContactId: StateFlow<Long?> = _selectedContactId.asStateFlow()

    private val _selectedStoryContactId = MutableStateFlow<Long?>(null)
    val selectedStoryContactId: StateFlow<Long?> = _selectedStoryContactId.asStateFlow()

    private val _activeTab = MutableStateFlow(TabState.CHATS)
    val activeTab: StateFlow<TabState> = _activeTab.asStateFlow()

    // Typing simulated indicator holding
    private val _typingContactId = MutableStateFlow<Long?>(null)
    val typingContactId: StateFlow<Long?> = _typingContactId.asStateFlow()

    // Interactive toggle to turn decryption off/on to demonstrate database E2E encryption
    private val _isDecryptionEnabled = MutableStateFlow(true)
    val isDecryptionEnabled: StateFlow<Boolean> = _isDecryptionEnabled.asStateFlow()

    // Query stories posted in the last 24 hours
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeStories: StateFlow<List<StoryItem>> = flow {
        while (true) {
            val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            emitAll(repository.getActiveStories(since))
            delay(10000) // refresh every 10s
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Get active chat messages
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessage>> = _selectedContactId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForContact(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
    }

    // Cryptographic utility: XOR cipher + Base64 symmetrically encrypted
    fun simpleEncrypt(plainText: String): String {
        return try {
            val bytes = plainText.toByteArray(Charsets.UTF_8)
            val encryptedBytes = bytes.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
            android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun simpleDecrypt(cipherText: String): String {
        return try {
            val decodedBytes = android.util.Base64.decode(cipherText, android.util.Base64.NO_WRAP)
            val decryptedBytes = decodedBytes.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }

    fun toggleDecryptionDisplay() {
        _isDecryptionEnabled.value = !_isDecryptionEnabled.value
    }

    fun selectContact(contactId: Long?) {
        _selectedContactId.value = contactId
    }

    fun selectStory(contactId: Long?) {
        _selectedStoryContactId.value = contactId
    }

    fun setTab(tab: TabState) {
        _activeTab.value = tab
    }

    fun toggleLikePost(postId: Long) {
        viewModelScope.launch {
            repository.toggleLikePost(postId)
        }
    }

    fun deleteChat(contactId: Long) {
        viewModelScope.launch {
            repository.clearChatMessages(contactId)
            _selectedContactId.value = null
        }
    }

    fun createNewContact(name: String, userName: String, bio: String, colorHex: String) {
        viewModelScope.launch {
            val contact = Contact(
                name = name,
                userName = userName,
                avatarColorHex = colorHex,
                statusMessage = bio,
                isOnline = true
            )
            repository.addContact(contact)
        }
    }

    // Group chats features
    fun createGroupChat(groupName: String, avatarColorHex: String, participantNames: String) {
        viewModelScope.launch {
            val fullParticipantsList = if (participantNames.contains("You", ignoreCase = true)) {
                participantNames
            } else {
                "$participantNames, You"
            }
            val contact = Contact(
                name = groupName,
                userName = groupName.lowercase().replace(" ", "_"),
                avatarColorHex = avatarColorHex,
                statusMessage = "Group Chat 📌",
                isOnline = true,
                isGroup = true,
                groupParticipants = fullParticipantsList
            )
            val groupId = repository.addContact(contact)

            // Add welcome message
            val text = "Group \"$groupName\" was successfully created! Welcome participants: $fullParticipantsList. All messages sent inside this group are encrypted at the client-side."
            val welcomeMsg = ChatMessage(
                threadContactId = groupId,
                senderName = "System Info",
                text = text,
                timestamp = System.currentTimeMillis() - 500,
                isFromMe = false,
                status = "READ",
                isEncrypted = true,
                encryptedText = simpleEncrypt(text)
            )
            repository.insertMessage(welcomeMsg)
        }
    }

    fun addRemoveParticipant(contactId: Long, newParticipants: String) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.id == contactId } ?: return@launch
            if (contact.isGroup) {
                val updated = contact.copy(groupParticipants = newParticipants)
                repository.updateContact(updated)

                val text = "Group participants list was updated. Active: $newParticipants"
                val systemMsg = ChatMessage(
                    threadContactId = contactId,
                    senderName = "System Info",
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = false,
                    status = "READ",
                    isEncrypted = true,
                    encryptedText = simpleEncrypt(text)
                )
                repository.insertMessage(systemMsg)
            }
        }
    }

    // Story publishing with drawings and overlays
    fun publishStory(textOverlay: String, textOverlayColorHex: String, drawingJson: String, imageUrl: String? = null) {
        viewModelScope.launch {
            val story = StoryItem(
                userId = 0, // 'You'
                userName = "You",
                avatarColorHex = "FF6750A4", // Purple Accent theme
                imageUrl = if (imageUrl.isNullOrBlank()) "drawing_canvas" else imageUrl,
                textOverlay = textOverlay,
                textOverlayColorHex = textOverlayColorHex,
                drawingPointsJson = drawingJson,
                timestamp = System.currentTimeMillis()
            )
            repository.insertStory(story)
        }
    }

    fun createNewPost(caption: String, imageUrl: String) {
        viewModelScope.launch {
            val post = SocialPost(
                authorName = "Your Digital Space",
                authorHandle = "dev_artist",
                authorAvatarColorHex = "FF4A90E2", // custom primary blue
                imageUrl = if (imageUrl.isBlank()) "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800" else imageUrl,
                caption = caption,
                likesCount = 0,
                isLiked = false,
                timeAgo = "Just now"
            )
            repository.addPost(post)
            _activeTab.value = TabState.EXPLORE
        }
    }

    fun sendMessage(text: String, imageUrl: String? = null, voiceSec: Int? = null) {
        val contactId = _selectedContactId.value ?: return
        if (text.isBlank() && imageUrl == null && voiceSec == null) return

        viewModelScope.launch {
            val contact = contacts.value.find { it.id == contactId }
            val isGroup = contact?.isGroup ?: false

            // Symmetric E2E Encryption
            val rawPlainText = text
            val cipherText = simpleEncrypt(rawPlainText)

            val myMsg = ChatMessage(
                threadContactId = contactId,
                text = rawPlainText,
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = "SENT",
                imageUrl = imageUrl,
                audioDurationSec = voiceSec,
                senderName = if (isGroup) "You" else null,
                isEncrypted = true,
                encryptedText = cipherText
            )
            val msgId = repository.insertMessage(myMsg)

            // Simulate delivery and read states
            delay(500)
            repository.updateMessage(myMsg.copy(id = msgId, status = "DELIVERED"))
            delay(500)
            repository.updateMessage(myMsg.copy(id = msgId, status = "READ"))

            // Trigger contact automated response simulation
            simulateAutoReply(contactId, rawPlainText, isGroup, contact)
        }
    }

    private fun simulateAutoReply(contactId: Long, userText: String, isGroup: Boolean, contact: Contact?) {
        viewModelScope.launch {
            // Wait 1.5 seconds before starting to type
            delay(1200)
            _typingContactId.value = contactId

            // Simulate typing duration of 2 seconds
            delay(1800)

            val replyText = getSimulatedReply(contactId, userText)
            val cipherText = simpleEncrypt(replyText)

            // Select a random group member if it is a group chat
            val finalSenderName = if (isGroup) {
                val participantsParts = contact?.groupParticipants?.split(",") ?: listOf("Jane Cooper", "Alex Rivera")
                val others = participantsParts.map { it.trim() }.filter { !it.equals("You", ignoreCase = true) }
                if (others.isNotEmpty()) others.random() else "Jane Cooper"
            } else {
                null
            }

            val contactMsg = ChatMessage(
                threadContactId = contactId,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                isFromMe = false,
                status = "READ",
                senderName = finalSenderName,
                isEncrypted = true,
                encryptedText = cipherText
            )
            repository.insertMessage(contactMsg)
            _typingContactId.value = null
        }
    }

    private fun getSimulatedReply(contactId: Long, userText: String): String {
        val normalized = userText.lowercase().trim()
        val queryWord = normalized.split(" ").firstOrNull() ?: ""

        val standardReplies = mapOf(
            "hello" to "Hey there! How is everything going today? 👋✨",
            "hey" to "Hi! Good to hear from you. What's up?",
            "hi" to "Hey! Hope you're having a gorgeous day.",
            "how" to "I'm doing great! Just working on some balanced design structures here. What about you?",
            "where" to "I'm just relaxing here at home, listening to some visual ambient music. 🎵🏡",
            "nice" to "Absolutely! Truly exciting. 😍",
            "cool" to "Yeah, so cool right? Let's check it out later.",
            "lol" to "Haha, totally! Still laughing about it. 😂🍿",
            "why" to "That is a very good question... Let me think about that for a second!",
            "ok" to "Awesome. Sounds like a solid plan. 👍",
            "bye" to "Talk to you later! Text me when you're free!"
        )

        // Try to match a common keyword
        for ((key, value) in standardReplies) {
            if (normalized.contains(key)) return value
        }

        // Return specific character flavor texts
        return when (contactId) {
            1L -> { // Jane Cooper (Designer)
                if (normalized.contains("design") || normalized.contains("art") || normalized.contains("color")) {
                    "Precisely! Color contrast and generous spacing really elevate user interfaces. We should post that. 🎨👌"
                } else {
                    val pool = listOf(
                        "Ooh, that's interesting! Let me sketch a quick layout of that of yours. 📝",
                        "I love that idea! Meet you at the design studio in a bit?",
                        "Wait, that matches our moodboard perfectly! Check my Instagram story details! 🌸✨",
                        "Design is intelligence made visible! Let me finish this layout and get right back to you! 😊🎨"
                    )
                    pool.random()
                }
            }
            2L -> { // Alex Rivera (Fitness/Tech)
                if (normalized.contains("gym") || normalized.contains("lift") || normalized.contains("work")) {
                    "Exactly! High-volume compound lifts match our developer output 🏋️‍♂️💻 Let's crush this."
                } else {
                    val pool = listOf(
                        "Yo! Gym training session is scheduled for 6:30 today. Be prompt! 🏋️‍♂️💯",
                        "Consistent habits destroy raw intensity every single day of the week. Let's get it!",
                        "Finished my final git push! Getting healthy meals now. Speak in 10!",
                        "Hell yeah! Write nice code and stay athletic! 🧗‍♂️🔥"
                    )
                    pool.random()
                }
            }
            3L -> { // Karthik Nair (Backend engineer)
                val pool = listOf(
                    "Agreed. Let me run compilation checks on our server configurations first.",
                    "Let's review the database schema migration scripts this evening.",
                    "Sounds perfect. Let me log off of my main terminal in 5 minutes.",
                    "Interesting. I am compiling some custom index optimizations right now."
                )
                pool.random()
                }
            4L -> { // Elena Rostova (Artist)
                val pool = listOf(
                    "Paris has such spectacular ambient light today! Missing the atelier. 🎨🗼",
                    "Beautiful textures! Reminds me of Monet's sunrise water studies. 🖌️💧",
                    "Ah, that thought is incredibly poetic. I should sketch that concept.",
                    "Mixing some cobalt blues on my wood palette today. Busy fingers!"
                )
                pool.random()
            }
            5L -> { // TwoChats AI Bot
                if (normalized.contains("help") || normalized.contains("info")) {
                    "I can help explain the app architecture! We use Jetpack Compose, Kotlin Coroutines Flow streams, and a local Room SQLite DB! ⚙️📱"
                } else {
                    val pool = listOf(
                        "Beep Boop! My neural parameters indicate that you are doing fine work. Keep coding! 🤖✨",
                        "I am simulated using client-side Kotlin flows! Fast, private, and offline-compatible. ✨💻",
                        "Fascinating request! Let me write some hypothetical layouts in my digital matrix.",
                        "Welcome to the playground! Type anything and see how fast I reply. ✨🚀"
                    )
                    pool.random()
                }
            }
            6L -> { // TwoChats Design Suite (Group)
                val pool = listOf(
                    "Let's make sure the text overlays fit exactly into the geometric layouts.",
                    "Wait, let's see which story card looks most eye-safe. Pink or Lavender?",
                    "Agreed with Alex. End-to-end local encryption works beautifully on SQLite too!",
                    "Let's build a visual demo. I can upload my sketching story in a minute!"
                )
                pool.random()
            }
            else -> "That is amazing. Keep writing and design things!"
        }
    }
}
