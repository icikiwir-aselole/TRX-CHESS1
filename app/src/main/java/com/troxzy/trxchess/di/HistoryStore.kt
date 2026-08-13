package com.troxzy.trxchess.di

import com.troxzy.trxchess.data.AnalysisSessionEntity
import com.troxzy.trxchess.data.TrxDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Persisted analysis-session history (Room-backed).
 */
class HistoryStore(private val db: TrxDatabase) {

    val sessions: Flow<List<AnalysisSessionEntity>> = db.sessions().observeAll()

    suspend fun save(fen: String, name: String = "Session") {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        db.sessions().upsert(
            AnalysisSessionEntity(
                id = id,
                name = name,
                initialFen = fen,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun delete(id: String) {
        db.sessions().delete(id)
    }
}