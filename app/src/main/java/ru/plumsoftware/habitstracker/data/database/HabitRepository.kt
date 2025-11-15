package ru.plumsoftware.habitstracker.data.database

import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.dao.HabitDao

class HabitRepository(private val habitDao: HabitDao) {

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: String): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun deleteHabitById(id: String) = habitDao.deleteHabitById(id)

    fun searchHabits(query: String): Flow<List<Habit>> = habitDao.searchHabits("%$query%")

    fun getHabitsByPeriodicity(periodicity: Int): Flow<List<Habit>> =
        habitDao.getHabitsByPeriodicity(periodicity)

    companion object {
        val repositoryModule = module {
            single<HabitRepository> { HabitRepository(get()) }
        }
    }
}