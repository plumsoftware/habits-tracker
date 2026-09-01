package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val body: String,
    val sendAt: Long,
    val resourceUrl: String,
    val type: NotificationType = NotificationType.GENERAL,
    val isRead: Boolean = false
)
