package ru.plumsoftware.habitstracker.data.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.plumsoftware.habitstracker.data.model.Notification

@Dao
interface NotificationsDao {

    @Insert
    suspend fun insert(notification: Notification)

    @Delete
    suspend fun delete(notification: Notification)

    @Query("SELECT * FROM notifications ORDER BY sendAt DESC")
    fun getAll(): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun unreadCount(): Flow<Int>
}
