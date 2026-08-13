package com.troxzy.trxchess.automation

import com.troxzy.trxchess.core.common.*

data class DeviceSignals(val cpuCores:Int,val ramMb:Int,val batteryPct:Int,val charging:Boolean,val thermal:ThermalLevel)
class ResourceController{
 fun policy(s:DeviceSignals,profile:ComputeProfile):ResourcePolicy{
  val base=when(profile){ComputeProfile.ULTRA_LOW->ResourcePolicy(1,16,8,10,30,2);ComputeProfile.LOW->ResourcePolicy(1,32,12,15,20,3);ComputeProfile.BALANCED->ResourcePolicy(minOf(2,s.cpuCores),64,20,20,15,2);ComputeProfile.HIGH->ResourcePolicy(minOf(4,s.cpuCores),128,30,30,10,2);ComputeProfile.MAX->ResourcePolicy(s.cpuCores.coerceAtLeast(1),256,60,45,5,1);ComputeProfile.CUSTOM->ResourcePolicy(minOf(2,s.cpuCores),64,20,20,15,2)}
  return when(s.thermal){ThermalLevel.SEVERE->base.copy(maxCpuThreads=1,maxAnalysisSeconds=5,maxOverlayHz=10);ThermalLevel.MODERATE->base.copy(maxCpuThreads=minOf(2,base.maxCpuThreads),maxOverlayHz=minOf(20,base.maxOverlayHz));else->base}
 }
}
