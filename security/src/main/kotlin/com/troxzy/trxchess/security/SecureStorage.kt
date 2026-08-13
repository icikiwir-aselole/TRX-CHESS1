package com.troxzy.trxchess.security
import android.content.Context
import android.security.keystore.*
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
class SecureStorage(context:Context){private val prefs=context.getSharedPreferences("secure_store",Context.MODE_PRIVATE);private val alias="trx_secure_v1";private val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)};private fun key():java.security.Key{ks.getKey(alias,null)?.let{return it};val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey()};fun put(name:String,value:String){val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());prefs.edit().putString(name,Base64.encodeToString(c.iv+c.doFinal(value.toByteArray(StandardCharsets.UTF_8)),Base64.NO_WRAP)).apply()};fun get(name:String):String?=prefs.getString(name,null)?.let{runCatching{val b=Base64.decode(it,Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12)));String(c.doFinal(b.copyOfRange(12,b.size)),StandardCharsets.UTF_8)}.getOrNull()}}
