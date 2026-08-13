package com.troxzy.trxchess.engine.api

data class EngineCapabilities(
    val supportsPonder: Boolean = false,
    val supportsMultiPv: Boolean = true,
    val supportsChess960: Boolean = false,
    val maxThreads: Int = 1,
    val maxHashMb: Int = 128,
)

interface TablebaseProvider {
    suspend fun probe(positionKey: String): TablebaseResult?
}

sealed interface TablebaseResult {
    data class Win(val dtz: Int) : TablebaseResult
    data class Draw(val dtz: Int) : TablebaseResult
    data class Loss(val dtz: Int) : TablebaseResult
}

interface EnginePlugin {
    val id: String
    val capabilities: EngineCapabilities
    fun create(): ChessEngine
}

class EngineRegistry {
    private val plugins = LinkedHashMap<String, EnginePlugin>()

    fun register(plugin: EnginePlugin) {
        require(plugin.id !in plugins) { "duplicate engine id" }
        plugins[plugin.id] = plugin
    }

    fun get(id: String): EnginePlugin? = plugins[id]

    fun all(): List<EnginePlugin> = plugins.values.toList()
}
