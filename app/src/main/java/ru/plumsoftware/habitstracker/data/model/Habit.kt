package ru.plumsoftware.habitstracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val periodicity: Int,
    val creationDate: Long,
    val habitTime: Long
)
