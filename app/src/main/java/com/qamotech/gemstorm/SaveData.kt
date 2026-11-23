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

    var musicOn: Boolean
        get() = prefs.getBoolean("musicOn", true)
        set(v) = prefs.edit().putBoolean("musicOn", v).apply()

    var muteAll: Boolean
        get() = prefs.getBoolean("muteAll", false)
        set(v) = prefs.edit().putBoolean("muteAll", v).apply()

    // Batch M (Menus #301-#325)
    var gfxQuality: Int
        get() = prefs.getInt("gfxQuality", 0)
        set(v) = prefs.edit().putInt("gfxQuality", v).apply()

    var shakeOn: Boolean
        get() = prefs.getBoolean("shakeOn", true)
        set(v) = prefs.edit().putBoolean("shakeOn", v).apply()

    var joySize: Float
        get() = prefs.getFloat("joySize", 1.0f)
        set(v) = prefs.edit().putFloat("joySize", v).apply()

    var joyLeft: Boolean
        get() = prefs.getBoolean("joyLeft", false)
        set(v) = prefs.edit().putBoolean("joyLeft", v).apply()

    var totalDamage: Long
        get() = prefs.getLong("totalDamage", 0L)
        set(v) = prefs.edit().putLong("totalDamage", v).apply()

    var totalGemsEarned: Long
        get() = prefs.getLong("totalGemsEarned", 0L)
        set(v) = prefs.edit().putLong("totalGemsEarned", v).apply()

    var bankedRerolls: Int
        get() = prefs.getInt("bankedRerolls", 0)
        set(v) = prefs.edit().putInt("bankedRerolls", v).apply()

    var runSnap: String
        get() = prefs.getString("runSnap", "") ?: ""
        set(v) = prefs.edit().putString("runSnap", v).apply()

    var runSnapWave: Int
        get() = prefs.getInt("runSnapWave", 0)
        set(v) = prefs.edit().putInt("runSnapWave", v).apply()

    fun runsChar(i: Int) = prefs.getInt("runsChar$i", 0)
    fun addRunChar(i: Int) { prefs.edit().putInt("runsChar$i", runsChar(i) + 1).apply() }
    fun runsWeapon(i: Int) = prefs.getInt("runsWeapon$i", 0)
    fun addRunWeapon(i: Int) { prefs.edit().putInt("runsWeapon$i", runsWeapon(i) + 1).apply() }

    var bestRank: Int
        get() = prefs.getInt("bestRank", 0)
        set(v) = prefs.edit().putInt("bestRank", v).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()

    var seenWhatsNew: Boolean
        get() = prefs.getBoolean("seenWhatsNew", false)
        set(v) = prefs.edit().putBoolean("seenWhatsNew", v).apply()

    var shopSort: Boolean
        get() = prefs.getBoolean("shopSort", false)
        set(v) = prefs.edit().putBoolean("shopSort", v).apply()

    var bestEndlessWave: Int
        get() = prefs.getInt("bestEndlessWave", 0)
        set(v) = prefs.edit().putInt("bestEndlessWave", v).apply()

    var manualAim: Boolean
        get() = prefs.getBoolean("manualAim", true) // v2.7 (Nathan): manual aim is the default — you aim your own shots
        set(v) = prefs.edit().putBoolean("manualAim", v).apply()

    var aimAssist: Int
        get() = prefs.getInt("aimAssist", 0) // v2.7 (Nathan): no snap assist by default — pure manual aim
        set(v) = prefs.edit().putInt("aimAssist", v).apply()

    var joySens: Float
        get() = prefs.getFloat("joySens", 1.0f)
        set(v) = prefs.edit().putFloat("joySens", v).apply()

    var pauseGuard: Boolean
        get() = prefs.getBoolean("pauseGuard", false)
        set(v) = prefs.edit().putBoolean("pauseGuard", v).apply()

    var btnOpacity: Float
        get() = prefs.getFloat("btnOpacity", 0.85f)
        set(v) = prefs.edit().putFloat("btnOpacity", v).apply()

    var btnScale: Float
        get() = prefs.getFloat("btnScale", 1.0f)
        set(v) = prefs.edit().putFloat("btnScale", v).apply()

    var oneHanded: Boolean
        get() = prefs.getBoolean("oneHanded", false)
        set(v) = prefs.edit().putBoolean("oneHanded", v).apply()

    fun resetAll() { prefs.edit().clear().apply() }

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

    var permCrit: Int
        get() = prefs.getInt("permCrit", 0)
        set(v) = prefs.edit().putInt("permCrit", v).apply()

    var permMagnet: Int
        get() = prefs.getInt("permMagnet", 0)
        set(v) = prefs.edit().putInt("permMagnet", v).apply()

    var permDash: Int
        get() = prefs.getInt("permDash", 0)
        set(v) = prefs.edit().putInt("permDash", v).apply()

    var reviveTokens: Int
        get() = prefs.getInt("reviveTokens", 0)
        set(v) = prefs.edit().putInt("reviveTokens", v).apply()

    var bossKills: Int
        get() = prefs.getInt("bossKills", 0)
        set(v) = prefs.edit().putInt("bossKills", v).apply()

    var heroDiscount: Boolean
        get() = prefs.getBoolean("heroDiscount", false)
        set(v) = prefs.edit().putBoolean("heroDiscount", v).apply()

    var hardMode: Boolean
        get() = prefs.getBoolean("hardMode", true) // v2.7 (Nathan): hard mode is the default difficulty
        set(v) = prefs.edit().putBoolean("hardMode", v).apply()

    /** v2.7 one-time defaults migration flag (existing saves get the harder defaults once). */
    var defaultsV27: Boolean
        get() = prefs.getBoolean("defaultsV27", false)
        set(v) = prefs.edit().putBoolean("defaultsV27", v).apply()

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
