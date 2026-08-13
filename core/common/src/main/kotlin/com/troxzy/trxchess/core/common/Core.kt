package com.troxzy.trxchess.core.common
import kotlinx.coroutines.*
data class ResourcePolicy(val maxCpuThreads:Int,val maxHashMb:Int,val maxAnalysisSeconds:Int,val maxOverlayHz:Int,val batteryThreshold:Int,val thermalThreshold:Int)
enum class ComputeProfile{ULTRA_LOW,LOW,BALANCED,HIGH,MAX,CUSTOM}
enum class ThermalLevel{NONE,LIGHT,MODERATE,SEVERE}
class AppDispatchers(val main:CoroutineDispatcher=Dispatchers.Main.immediate,val default:CoroutineDispatcher=Dispatchers.Default,val io:CoroutineDispatcher=Dispatchers.IO,val engine:CoroutineDispatcher=Dispatchers.Default)
object FeatureFlags{ @Volatile var overlay=true; @Volatile var automation=true; @Volatile var cloudAnalysis=false; @Volatile var aiExplanation=false; @Volatile var tablebase=false; @Volatile var diagnostics=true; @Volatile var experimental=false }
