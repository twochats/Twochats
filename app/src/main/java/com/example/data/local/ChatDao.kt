package com.example.data.local

import androidx.room.*
import com.example.data.model.Contact
import com.example.data.model.ChatMessage
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Contacts queries
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    suspend fun getContactById(contactId: Long): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Update
    suspend fun updateContact(contact: Contact)

    // Messages queries
    @Query("SELECT * FROM messages WHERE threadContactId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContactFlow(contactId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("DELETE FROM messages WHERE threadContactId = :contactId")
    suspend fun deleteMessagesForContact(contactId: Long)

    // Social Posts queries
    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getAllPostsFlow(): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<SocialPost>)

    @Update
    suspend fun updatePost(post: SocialPost)

    // Stories queries
    @Query("SELECT * FROM stories WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getActiveStoriesFlow(sinceTimestamp: Long): Flow<List<StoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryItem): Long
}
