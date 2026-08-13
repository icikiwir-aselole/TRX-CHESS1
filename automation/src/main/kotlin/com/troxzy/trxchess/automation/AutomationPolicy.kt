package com.troxzy.trxchess.automation
enum class AutomationDecision{ALLOW_ANALYSIS,PAUSE_ANALYSIS,DISALLOW_AUTOMATION,REQUIRE_MANUAL_INPUT,OFFLINE_ONLY}
data class AutomationContext(val positionAvailable:Boolean,val trainingMode:Boolean,val integrationAllowsEngine:Boolean,val batteryPct:Int,val thermalSevere:Boolean,val charging:Boolean)
class AutomationPolicyEngine{fun decide(c:AutomationContext)=when{!c.positionAvailable->AutomationDecision.REQUIRE_MANUAL_INPUT;!c.integrationAllowsEngine&&!c.trainingMode->AutomationDecision.DISALLOW_AUTOMATION;c.thermalSevere->AutomationDecision.PAUSE_ANALYSIS;c.batteryPct<15&&!c.charging->AutomationDecision.PAUSE_ANALYSIS;else->AutomationDecision.ALLOW_ANALYSIS}}
