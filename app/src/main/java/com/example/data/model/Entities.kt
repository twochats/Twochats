package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userName: String,
    val avatarColorHex: String,
    val statusMessage: String,
    val isOnline: Boolean,
    val storyMediaUrl: String? = null, // Mock story if not null
    val storyCaption: String? = null,
    val isGroup: Boolean = false,
    val groupParticipants: String = "" // List of participants, e.g., "Jane, Alex, You"
) : Serializable

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadContactId: Long,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: String, // "SENT", "DELIVERED", "READ"
    val imageUrl: String? = null, // for media messages
    val audioDurationSec: Int? = null, // for voice note simulation
    val isAudioPlaying: Boolean = false,
    val senderName: String? = null, // Name of the group sender
    val isEncrypted: Boolean = false,
    val encryptedText: String? = null // Holds raw ciphertext in database to show end-to-end mechanism
) : Serializable

@Entity(tableName = "posts")
data class SocialPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarColorHex: String,
    val imageUrl: String,
    val caption: String,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val timeAgo: String
) : Serializable

@Entity(tableName = "stories")
data class StoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long, // 0 if self, otherwise contactId
    val userName: String,
    val avatarColorHex: String,
    val imageUrl: String? = null, // Image URL or "drawing_canvas"
    val textOverlay: String = "",
    val textOverlayColorHex: String = "FFFFFF",
    val drawingPointsJson: String = "", // serialized drawing actions
    val timestamp: Long,
    val isVideo: Boolean = false
) : Serializable
