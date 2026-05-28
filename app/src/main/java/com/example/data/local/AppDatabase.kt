package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.Contact
import com.example.data.model.ChatMessage
import com.example.data.model.SocialPost
import com.example.data.model.StoryItem

@Database(
    entities = [Contact::class, ChatMessage::class, SocialPost::class, StoryItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
