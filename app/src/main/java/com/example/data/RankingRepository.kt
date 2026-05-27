package com.example.data

import kotlinx.coroutines.flow.Flow

class RankingRepository(private val rankingDao: RankingDao) {
    val allRankings: Flow<List<RankingEntity>> = rankingDao.getAllRankings()

    suspend fun insertRanking(ranking: RankingEntity): Long {
        return rankingDao.insertRanking(ranking)
    }

    suspend fun clearAll() {
        rankingDao.clearAll()
    }
}
