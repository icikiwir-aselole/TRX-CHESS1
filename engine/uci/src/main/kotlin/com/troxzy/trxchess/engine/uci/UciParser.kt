package com.troxzy.trxchess.engine.uci

import com.troxzy.trxchess.engine.api.Evaluation

data class UciInfo(val depth:Int?=null,val selDepth:Int?=null,val timeMs:Long?=null,val nodes:Long?=null,val nps:Long?=null,val hashFull:Int?=null,val multiPv:Int=1,val score:Evaluation?=null,val pv:List<String> = emptyList())
object UciParser {
    fun parseInfo(line:String):UciInfo? {
        if(!line.startsWith("info"))return null
        val t=line.trim().split(Regex("\\s+")); var i=1; var d:Int?=null;var sd:Int?=null;var tm:Long?=null;var n:Long?=null;var np:Long?=null;var hf:Int?=null;var mpv=1;var sc:Evaluation?=null;var pv=emptyList<String>()
        while(i<t.size){ when(t[i]){
            "depth"->d=t.getOrNull(++i)?.toIntOrNull(); "seldepth"->sd=t.getOrNull(++i)?.toIntOrNull(); "time"->tm=t.getOrNull(++i)?.toLongOrNull(); "nodes"->n=t.getOrNull(++i)?.toLongOrNull(); "nps"->np=t.getOrNull(++i)?.toLongOrNull(); "hashfull"->hf=t.getOrNull(++i)?.toIntOrNull(); "multipv"->mpv=t.getOrNull(++i)?.toIntOrNull()?:1
            "score"->{ val typ=t.getOrNull(++i); val v=t.getOrNull(++i)?.toIntOrNull(); if(typ!=null&&v!=null) sc=when(typ){"cp"->Evaluation.Centipawn(v);"mate"->Evaluation.Mate(v);else->null}; if(t.getOrNull(i+1)=="lowerbound"){sc=sc?.let{Evaluation.LowerBound(it)};i++};if(t.getOrNull(i+1)=="upperbound"){sc=sc?.let{Evaluation.UpperBound(it)};i++} }
            "pv"->{pv=t.drop(i+1).takeWhile{it !in listOf("string","currmove","currmovenumber")};break}
        };i++ }
        return UciInfo(d,sd,tm,n,np,hf,mpv,sc,pv)
    }
    fun isBestMove(line:String)=line.startsWith("bestmove ")
    fun bestMove(line:String)=line.removePrefix("bestmove ").trim().split(Regex("\\s+"))[0].ifBlank{null}
}
