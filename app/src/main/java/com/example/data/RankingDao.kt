package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {
    @Query("SELECT * FROM rankings ORDER BY duration DESC, timestamp DESC")
    fun getAllRankings(): Flow<List<RankingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRanking(ranking: RankingEntity): Long

    @Query("DELETE FROM rankings")
    suspend fun clearAll()
}
