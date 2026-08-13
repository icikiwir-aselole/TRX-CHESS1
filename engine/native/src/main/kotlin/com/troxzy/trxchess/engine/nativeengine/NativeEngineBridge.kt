package com.troxzy.trxchess.engine.nativeengine
object NativeEngineBridge {
    init { System.loadLibrary("trxengine") }
    @JvmStatic external fun nativeAnalyze(fen:String, depth:Int):String
}
