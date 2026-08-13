package com.troxzy.trxchess.engine.uci

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

interface UciTransport { suspend fun start(); suspend fun send(command:String); fun lines():Flow<String>; suspend fun stop() }

class ProcessUciTransport(private val command:List<String>, private val scope:CoroutineScope = CoroutineScope(Dispatchers.IO)) : UciTransport {
    private val channel=Channel<String>(capacity=Channel.BUFFERED)
    private var process:Process?=null
    private var readerJob:Job?=null
    override suspend fun start(){
        check(process==null) { "transport already started" }
        process=ProcessBuilder(command).redirectErrorStream(false).start()
        val p=process!!
        readerJob=scope.launch { BufferedReader(InputStreamReader(p.inputStream)).useLines { seq -> seq.forEach { channel.trySend(it) } } }
        scope.launch { BufferedReader(InputStreamReader(p.errorStream)).useLines { seq -> seq.forEach { channel.trySend("info string [stderr] $it") } } }
    }
    override suspend fun send(command:String){ val p=process?:error("transport not started");BufferedWriter(OutputStreamWriter(p.outputStream)).apply{write(command);write("\n");flush()} }
    override fun lines():Flow<String> = channel.receiveAsFlow()
    override suspend fun stop(){ runCatching { process?.outputStream?.close() }; readerJob?.cancel(); runCatching { process?.destroy() }; process=null }
}
