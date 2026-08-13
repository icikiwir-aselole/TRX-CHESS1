package com.troxzy.trxchess.engine.nativeengine

import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.uci.UciEngine

class NativeEngine : ChessEngine by UciEngine(native = { command ->
    val fen = command.lineSequence().firstOrNull { it.startsWith("position fen ") }
        ?.removePrefix("position fen ")?.trim()
    if (fen == null) null else {
        val depth = Regex("depth (\\d+)").find(command)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 12
        NativeEngineBridge.nativeAnalyze(fen, depth).lineSequence().toList()
    }
})
