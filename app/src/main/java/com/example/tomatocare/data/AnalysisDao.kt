package com.example.tomatocare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(analysis: AnalysisEntity): Long

    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<AnalysisEntity?>

    @Query("SELECT * FROM analyses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AnalysisEntity?

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun deleteById(id: Long)
}