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

    @Query(
        "SELECT * FROM notifications"
    )
    fun getAll(): Flow<List<Notification>>
}