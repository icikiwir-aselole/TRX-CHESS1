package com.troxzy.trxchess

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.troxzy.trxchess.ui.BoardView

class MainActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,24,24,24);setBackgroundColor(0xff101418.toInt())};root.addView(TextView(this).apply{text="TRX-CHESS";textSize=28f;setTextColor(0xfff2f5f7.toInt())});root.addView(TextView(this).apply{text="By Troxzy\nt.me/SoloBanNoTrash\nOffline-first • native engine architecture";setTextColor(0xffb8c2cc.toInt());textSize=15f});val board=BoardView(this);root.addView(board,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)}}
