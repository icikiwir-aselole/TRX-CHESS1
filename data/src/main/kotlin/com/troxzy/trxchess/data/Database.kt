package com.troxzy.trxchess.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="analysis_sessions") data class AnalysisSessionEntity(@PrimaryKey val id:String,val name:String,val initialFen:String,val createdAt:Long,val updatedAt:Long)
@Dao interface AnalysisSessionDao{@Query("SELECT * FROM analysis_sessions ORDER BY updatedAt DESC") fun observeAll():Flow<List<AnalysisSessionEntity>>;@Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(entity:AnalysisSessionEntity);@Query("DELETE FROM analysis_sessions WHERE id=:id") suspend fun delete(id:String)}
@Database(entities=[AnalysisSessionEntity::class],version=1,exportSchema=true) abstract class TrxDatabase:RoomDatabase(){abstract fun sessions():AnalysisSessionDao;companion object{ @Volatile private var instance:TrxDatabase?=null;fun get(c:Context)=instance?:synchronized(this){instance?:Room.databaseBuilder(c.applicationContext,TrxDatabase::class.java,"trx_chess.db").build().also{instance=it}}}}
val Context.trxSettings by preferencesDataStore("trx_settings")
object Keys{
    val profile=stringPreferencesKey("compute_profile")
    val overlay=booleanPreferencesKey("overlay_enabled")
    val hash=intPreferencesKey("hash_mb")
    val threads=intPreferencesKey("threads")
    val multiPv=intPreferencesKey("multipv")
    val defaultDepth=intPreferencesKey("default_depth")
    val themeMode=stringPreferencesKey("theme_mode")
    val boardTheme=stringPreferencesKey("board_theme")
    val animationEnabled=booleanPreferencesKey("animation_enabled")
    val visualQuality=stringPreferencesKey("visual_quality")
    val particlesEnabled=booleanPreferencesKey("particles_enabled")
    val reducedMotion=booleanPreferencesKey("reduced_motion")
    val overlayCompact=booleanPreferencesKey("overlay_compact")
    val overlayOpacity=floatPreferencesKey("overlay_opacity")
    val powerSaver=booleanPreferencesKey("power_saver")
    val lowBatteryStop=booleanPreferencesKey("low_battery_stop")
    val telemetryEnabled=booleanPreferencesKey("telemetry_enabled")
}
