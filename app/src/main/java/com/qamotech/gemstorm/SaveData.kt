package com.qamotech.gemstorm

import android.content.Context
import android.content.SharedPreferences

/** Persistent meta-progression: gem bank, unlocks, records, settings. */
class SaveData(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemstorm_save", Context.MODE_PRIVATE)

    var gemBank: Int
        get() = prefs.getInt("gemBank", 0)
        set(v) = prefs.edit().putInt("gemBank", v).apply()

    var highScore: Int
        get() = prefs.getInt("highScore", 0)
        set(v) = prefs.edit().putInt("highScore", v).apply()

    var bestWave: Int
        get() = prefs.getInt("bestWave", 0)
        set(v) = prefs.edit().putInt("bestWave", v).apply()

    var totalKills: Int
        get() = prefs.getInt("totalKills", 0)
        set(v) = prefs.edit().putInt("totalKills", v).apply()

    var totalRuns: Int
        get() = prefs.getInt("totalRuns", 0)
        set(v) = prefs.edit().putInt("totalRuns", v).apply()

    var bestCombo: Int
        get() = prefs.getInt("bestCombo", 0)
        set(v) = prefs.edit().putInt("bestCombo", v).apply()

    var victories: Int
        get() = prefs.getInt("victories", 0)
        set(v) = prefs.edit().putInt("victories", v).apply()

    /** Bitmask of unlocked characters (bit 0 = Guardian, always owned). */
    var charUnlocks: Int
        get() = prefs.getInt("charUnlocks", 1)
        set(v) = prefs.edit().putInt("charUnlocks", v).apply()

    /** Bitmask of unlocked weapons (bit 0 = Blaster, always owned). */
    var weaponUnlocks: Int
        get() = prefs.getInt("weaponUnlocks", 1)
        set(v) = prefs.edit().putInt("weaponUnlocks", v).apply()

    var selectedChar: Int
        get() = prefs.getInt("selectedChar", 0)
        set(v) = prefs.edit().putInt("selectedChar", v).apply()

    var selectedWeapon: Int
        get() = prefs.getInt("selectedWeapon", 0)
        set(v) = prefs.edit().putInt("selectedWeapon", v).apply()

    var musicVol: Float
        get() = prefs.getFloat("musicVol", 0.8f)
        set(v) = prefs.edit().putFloat("musicVol", v).apply()

    var sfxVol: Float
        get() = prefs.getFloat("sfxVol", 0.9f)
        set(v) = prefs.edit().putFloat("sfxVol", v).apply()

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) = prefs.edit().putBoolean("vibration", v).apply()

    // ---- v2.0 additions (new keys only; all v1.x saves stay valid) ----
    var dmgNumbers: Boolean
        get() = prefs.getBoolean("dmgNumbers", true)
        set(v) = prefs.edit().putBoolean("dmgNumbers", v).apply()

    var showTimer: Boolean
        get() = prefs.getBoolean("showTimer", true)
        set(v) = prefs.edit().putBoolean("showTimer", v).apply()

    var bigHud: Boolean
        get() = prefs.getBoolean("bigHud", false)
        set(v) = prefs.edit().putBoolean("bigHud", v).apply()

    var highContrast: Boolean
        get() = prefs.getBoolean("highContrast", false)
        set(v) = prefs.edit().putBoolean("highContrast", v).apply()

    var elitesSlain: Int
        get() = prefs.getInt("elitesSlain", 0)
        set(v) = prefs.edit().putInt("elitesSlain", v).apply()

    var heartsCollected: Int
        get() = prefs.getInt("heartsCollected", 0)
        set(v) = prefs.edit().putInt("heartsCollected", v).apply()

    var totalPlaytime: Long
        get() = prefs.getLong("totalPlaytime", 0L)
        set(v) = prefs.edit().putLong("totalPlaytime", v).apply()

    var dailyBest: Int
        get() = prefs.getInt("dailyBest", 0)
        set(v) = prefs.edit().putInt("dailyBest", v).apply()

    var dailyDate: String
        get() = prefs.getString("dailyDate", "") ?: ""
        set(v) = prefs.edit().putString("dailyDate", v).apply()

    fun bestWaveChar(i: Int) = prefs.getInt("bestWaveChar$i", 0)
    fun setBestWaveChar(i: Int, v: Int) { prefs.edit().putInt("bestWaveChar$i", v).apply() }

    fun ownsChar(i: Int) = (charUnlocks shr i) and 1 == 1
    fun ownsWeapon(i: Int) = (weaponUnlocks shr i) and 1 == 1
    fun unlockChar(i: Int) { charUnlocks = charUnlocks or (1 shl i) }
    fun unlockWeapon(i: Int) { weaponUnlocks = weaponUnlocks or (1 shl i) }

    /** Bank the gems from a finished run and update records. Returns true if new high score. */
    fun recordRun(score: Int, wave: Int, kills: Int, gemsEarned: Int, victory: Boolean, playSecs: Long = 0L): Boolean {
        gemBank += gemsEarned
        totalPlaytime += playSecs
        totalKills += kills
        totalRuns += 1
        if (victory) victories += 1
        if (wave > bestWave) bestWave = wave
        val isRecord = score > highScore
        if (isRecord) highScore = score
        return isRecord
    }
}
