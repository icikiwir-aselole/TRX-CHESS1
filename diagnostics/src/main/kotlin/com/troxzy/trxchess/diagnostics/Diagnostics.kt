package com.troxzy.trxchess.diagnostics

data class DiagnosticsSnapshot(val androidApi:Int,val abi:String,val cpuCores:Int,val memoryClassMb:Int,val batteryPct:Int?,val charging:Boolean?,val thermalState:String?,val engineState:String,val engineVersion:String,val threads:Int,val hashMb:Int,val depth:Int,val nodes:Long,val nps:Long,val overlayFps:Double,val uiFrameMs:Double,val restartCount:Int)
object DiagnosticsRedaction{private val secretKeys=setOf("token","authorization","password","secret","key");fun redact(metrics:Map<String,String>)=metrics.mapValues{(k,v)->if(secretKeys.any{k.lowercase().contains(it)})"<redacted>" else v}}
