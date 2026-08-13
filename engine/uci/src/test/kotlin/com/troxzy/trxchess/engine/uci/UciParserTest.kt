package com.troxzy.trxchess.engine.uci
import com.troxzy.trxchess.engine.api.Evaluation
import org.junit.Assert.*
import org.junit.Test
class UciParserTest { @Test fun parsesInfo(){val x=UciParser.parseInfo("info depth 20 nodes 12345 nps 999 score cp 34 multipv 2 pv e2e4 e7e5")!!;assertEquals(20,x.depth);assertEquals(2,x.multiPv);assertEquals(Evaluation.Centipawn(34),x.score);assertEquals(listOf("e2e4","e7e5"),x.pv)} }
