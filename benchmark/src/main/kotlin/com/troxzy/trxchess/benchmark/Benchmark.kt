package com.troxzy.trxchess.benchmark

enum class BenchmarkPreset{QUICK,STANDARD,EXTENDED,CUSTOM}
data class BenchmarkReport(val preset:BenchmarkPreset,val elapsedMs:Long,val nodes:Long,val nps:Long)
