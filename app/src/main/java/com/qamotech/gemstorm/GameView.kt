package com.qamotech.gemstorm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * GEMSTORM 💎 — Neon Abyss
 * A wave-survival arena ARPG: 20 waves, boss every 5, XP gems, roguelite
 * level-up choices, meta shop (characters + weapons), full save system.
 * Entire game (engine + UI) renders on Canvas — no layouts, no assets
 * beyond the composed music/SFX in res/raw.
 */
class GameView(context: Context) : View(context) {

    enum class State { MENU, HOWTO, SHOP, SETTINGS, STATS, PLAYING, LEVELUP, PAUSED, GAMEOVER, VICTORY }

    // ---------- catalog ----------
    data class CharDef(val name: String, val icon: String, val hp: Float, val spd: Float,
                       val dmg: Float, val cost: Int, val color: Int, val desc: String)
    data class WeaponDef(val name: String, val icon: String, val dmg: Float, val rate: Float,
                         val projSpeed: Float, val pellets: Int, val pierce: Int,
                         val cost: Int, val color: Int, val desc: String)

    companion object {
        val CHARS = listOf(
            CharDef("Guardian", "💎", 120f, 1.00f, 1.00f, 0, Color.rgb(34, 211, 238), "Balanced gem knight. Solid at everything."),
            CharDef("Swift", "⚡", 85f, 1.35f, 0.92f, 400, Color.rgb(250, 204, 21), "Fragile but blazing fast. Kite everything."),
            CharDef("Bulwark", "🛡️", 190f, 0.82f, 1.18f, 900, Color.rgb(251, 146, 60), "A walking fortress. Slow, hits like a truck.")
        )
        val WEAPONS = listOf(
            WeaponDef("Gem Blaster", "🔫", 12f, 4.0f, 950f, 1, 0, 0, Color.rgb(34, 211, 238), "Reliable single shots."),
            WeaponDef("Scatter Nova", "🌟", 9f, 3.4f, 900f, 3, 0, 300, Color.rgb(244, 114, 182), "Three-way spread. Shreds crowds up close."),
            WeaponDef("Pulse Lance", "⚡", 10f, 7.0f, 1250f, 1, 3, 750, Color.rgb(167, 139, 250), "Piercing bolts at machine-gun pace."),
            WeaponDef("Orbit Blades", "🌀", 15f, 3.6f, 950f, 1, 0, 1500, Color.rgb(52, 211, 153), "Twin blades orbit you + a blaster. Area denial.")
        )
        val UPGRADE_NAMES = arrayOf("💥 Power Core", "🔥 Rapid Cycler", "👟 Swift Boots", "❤️ Vitality Plating",
            "🧲 Magnet Core", "🎯 Crit Lens", "🔱 Split Shot", "💚 Regen Weave", "💨 Phase Dash", "🛡️ Hull Plating",
            "🩸 Vampire Core", "❄️ Frost Rounds",
            "⚡ Tesla Coil", "☄️ Meteor Call", "🧪 Poison Vials", "🌵 Thorn Aura",
            "🧲 Gem Magnet+", "🏃 Adrenaline", "🔪 Glass Cannon", "💫 Second Wind",
            "💨 Dash Charge+", "🎯 Crit Damage+", "💰 XP Greed", "🛡️ Shield Generator")
        val UPGRADE_DESCS = arrayOf("+15% damage", "+15% fire rate", "+10% move speed", "+25 max HP, heal 40",
            "+45% gem magnet range", "+10% crit chance (crits deal 2×)", "+1 projectile per shot", "+0.9 HP/s regeneration",
            "Dash recharges 22% faster", "+1 armor (blocks 1 dmg per hit)",
            "Heal 1.5 HP on every kill", "Hits chill enemies: −22% speed for 1.5s (stacks)",
            "Hits chain lightning to nearby foes (40% dmg)", "A meteor smashes a random foe every ~6s",
            "Kills may leave a toxic pool (9 dps)", "Attackers take 30% of contact dmg back",
            "+35% gem magnet range (stacks)", "Combo 10+: +18% move speed",
            "+25% damage, −15 max HP", "Revive once at 50% HP when slain",
            "+1 dash charge", "Crits deal +50% more damage",
            "+25% XP from gems, foes 10% faster", "Regenerating energy shield (20 HP)")
        const val MAX_WAVE = 20
    }

    // ---------- entities ----------
    class Enemy(val type: Int, var x: Float, var y: Float, var hp: Float, var maxHp: Float,
                val speed: Float, var radius: Float, var damage: Float, var xp: Int, var score: Int) {
        var slowT = 0f; var elite = false; var burnT = 0f
        var flash = 0f; var shootT = 1.5f + Random.nextFloat() * 1.5f; var bladeT = 0f
        var wob = Random.nextFloat() * 6.28f; var chargeT = 0f; var vx = 0f; var vy = 0f
        var state = 0; var stateT = 0f; var dir = if (Random.nextBoolean()) 1f else -1f
    }
    class Proj(var x: Float, var y: Float, var vx: Float, var vy: Float, val dmg: Float,
               var pierce: Int, val fromPlayer: Boolean, val radius: Float, val color: Int) { var life = 1.6f }
    class GemDrop(var x: Float, var y: Float, val value: Int) { var vx = 0f; var vy = 0f; var mag = false }
    class HeartDrop(var x: Float, var y: Float) { var vx = 0f; var vy = 0f; var mag = false }
    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
                   var life: Float, val maxLife: Float, val color: Int, val size: Float,
                   val shape: Int = 0, var rot: Float = 0f)
    class Decal(var x: Float, var y: Float, val r: Float, var life: Float)
    class PoisonPool(var x: Float, var y: Float, val r: Float, var life: Float)
    class Spark(var x: Float, var y: Float, var life: Float, val color: Int, val size: Float)
    class Bolt(var x1: Float, var y1: Float, var x2: Float, var y2: Float, var life: Float)
    class Meteor(var x: Float, var y: Float, var t: Float)
    class FloatText(var x: Float, var y: Float, val text: String, var life: Float, val color: Int, val size: Float)
    class Star(val x: Float, val y: Float, val size: Float, val phase: Float)
    class Ghost(var x: Float, var y: Float, var life: Float, val color: Int)
    class Ring(var x: Float, var y: Float, var r: Float, var life: Float, val maxLife: Float, val color: Int)
    class HitBtn(val rect: RectF, val id: String)

    // ---------- core ----------
    private val save = SaveData(context)
    val audio = AudioManager(context, save)
    private var state = State.MENU
    private var w = 0f; private var h = 0f
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hits = mutableListOf<HitBtn>()
    private var time = 0f
    private var loopActive = false
    private var lastFrame = 0L
    private val choreo = Choreographer.getInstance()
    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!loopActive) return
            val dt = if (lastFrame == 0L) 0.016f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0.001f, 0.033f)
            lastFrame = frameTimeNanos
            tick(dt)
            invalidate()
            choreo.postFrameCallback(this)
        }
    }

    // ---------- run state ----------
    private var px = 0f; private var py = 0f; private var pvx = 0f; private var pvy = 0f
    private var hp = 100f; private var maxHp = 100f
    private var level = 1; private var xp = 0; private var xpNext = 6
    private var dmgMul = 1f; private var rateMul = 1f; private var speedMul = 1f
    private var magnetR = 110f; private var critCh = 0.05f; private var regen = 0f
    private var projBonus = 0; private var armor = 0; private var dashCdMax = 2.2f
    private var lifesteal = 0f; private var frostLvl = 0
    private var dashCd = 0f; private var dashT = 0f; private var invulnT = 0f
    private var fireT = 0f; private var orbitAngle = 0f
    private var wave = 0; private var toSpawn = 0; private var spawnT = 0f; private var spawnInterval = 1f
    private var waveBannerT = 0f; private var bossWave = false
    private var score = 0; private var kills = 0; private var gemsRun = 0; private var runTime = 0f
    private var shake = 0f; private var hurtFlash = 0f
    private var runRecorded = false; private var newRecord = false
    private var offers = listOf<Int>()
    private var lastShootSfx = 0f; private var lastPickupSfx = 0f
    private var combo = 0; private var comboT = 0f; private var comboBest = 0
    private var lastAim = 0f; private var muzzleT = 0f
    private var comboPop = 0f; private var killsPop = 0f; private var gemsPop = 0f
    private var lastKillsSeen = 0; private var lastGemsSeen = 0
    private val upgradeCounts = IntArray(24)
    private var bossIntroT = 0f
    private var eliteBannerT = 0f; private var waveClearT = 0f
    private var waveClearBonus = 0; private var waveClearedNum = 0; private var waveTotal = 1
    private var dashCharges = 1; private var dashMaxCharges = 1; private var dashRechargeT = 0f
    private var toastMsg = ""; private var toastT = 0f
    private var pendingConfirm = ""; private var confirmT = 0f
    private var teslaLvl = 0; private var meteorLvl = 0; private var poisonLvl = 0; private var thornLvl = 0
    private var adrenalineLvl = 0; private var secondWind = 0; private var xpGreedLvl = 0
    private var shieldLvl = 0; private var shieldHp = 0f; private var lastHurtAt = -10f
    private var critMul = 2f; private var meteorT = 5f
    private var mutator = 0
    private var bossBurst66 = false; private var bossBurst33 = false; private var bossSummoned = false
    private var bossEnraged = false; private var bossAliveT = 0f
    private var dailyMode = false; private var dailyRng = Random(1)
    private val meteors = mutableListOf<Meteor>()
    private val pendingSpawns = mutableListOf<Enemy>()

    private fun pickRng(): Random = if (dailyMode) dailyRng else Random
    private fun mutatorTitle(): String = when (mutator) {
        1 -> "💎 GEM RUSH — double gems, faster spawns!"
        2 -> "🌑 DARKNESS — stay in the light!"
        3 -> "⭐ ELITE SURGE — every foe is elite!"
        else -> ""
    }

    private val enemies = mutableListOf<Enemy>()
    private val projs = mutableListOf<Proj>()
    private val gemDrops = mutableListOf<GemDrop>()
    private val hearts = mutableListOf<HeartDrop>()
    private val particles = mutableListOf<Particle>()
    private val texts = mutableListOf<FloatText>()
    private val stars = mutableListOf<Star>()
    private val ghosts = mutableListOf<Ghost>()
    private val rings = mutableListOf<Ring>()
    private val decals = mutableListOf<Decal>()
    private val pools = mutableListOf<PoisonPool>()
    private val sparks = mutableListOf<Spark>()
    private val bolts = mutableListOf<Bolt>()

    // ---------- input ----------
    private var joyPointer = -1; private var joyOx = 0f; private var joyOy = 0f
    private var joyDx = 0f; private var joyDy = 0f
    private val dashBtnRect = RectF()
    private val pauseBtnRect = RectF()

    init {
        isFocusable = true
        contentDescription = "GEMSTORM game canvas" 
        audio.playMusic(R.raw.music_menu)
    }

    override fun onSizeChanged(nw: Int, nh: Int, ow: Int, oh: Int) {
        super.onSizeChanged(nw, nh, ow, oh)
        w = nw.toFloat(); h = nh.toFloat()
        stars.clear()
        for (i in 0 until 90) stars.add(Star(Random.nextFloat() * w, Random.nextFloat() * h,
            1f + Random.nextFloat() * 2.2f, Random.nextFloat() * 6.28f))
        if (px == 0f) { px = w / 2; py = h / 2 }
    }

    fun onHostResume() {
        startLoop()
        // Music was stopped on host pause; restore it for ambient states.
        when (state) {
            State.MENU, State.HOWTO, State.SHOP, State.SETTINGS, State.STATS,
            State.GAMEOVER, State.VICTORY, State.PAUSED -> audio.playMusic(R.raw.music_menu)
            else -> { }
        }
    }
    fun onHostPause() {
        if (state == State.PLAYING) state = State.PAUSED
        stopLoop()
        audio.stopMusic()
    }
    fun shutdown() { stopLoop(); audio.release() }
    private fun startLoop() { if (!loopActive) { loopActive = true; lastFrame = 0L; choreo.postFrameCallback(frameCb) } }
    private fun stopLoop() { loopActive = false; choreo.removeFrameCallback(frameCb) }

    /** Returns true if the game consumed the back press. */
    fun handleBack(): Boolean = when (state) {
        State.PLAYING -> { pauseGame(); true }
        State.PAUSED -> { resumeGame(); true }
        State.HOWTO, State.SHOP, State.SETTINGS, State.STATS -> { toMenu(); true }
        State.LEVELUP -> true // must choose
        else -> false
    }

    // ---------- state transitions ----------
    private fun toast(msg: String) { toastMsg = msg; toastT = 2.2f }

    private fun toMenu() {
        state = State.MENU
        audio.playMusic(R.raw.music_menu)
    }

    private fun startRun(daily: Boolean = false) {
        val ch = CHARS[save.selectedChar]
        val wp = WEAPONS[save.selectedWeapon]
        maxHp = ch.hp; hp = maxHp
        level = 1; xp = 0; xpNext = 6
        dmgMul = ch.dmg; rateMul = 1f; speedMul = ch.spd
        magnetR = 110f; critCh = 0.05f; regen = 0f; projBonus = 0; armor = 0; dashCdMax = 2.2f
        lifesteal = 0f; frostLvl = 0
        teslaLvl = 0; meteorLvl = 0; poisonLvl = 0; thornLvl = 0; adrenalineLvl = 0
        secondWind = 0; xpGreedLvl = 0; shieldLvl = 0; shieldHp = 0f; critMul = 2f; meteorT = 5f
        mutator = 0; dailyMode = daily
        if (daily) dailyRng = Random(java.time.LocalDate.now().toEpochDay())
        dashCd = 0f; dashT = 0f; invulnT = 0f; fireT = 0f
        dashMaxCharges = 1; dashCharges = 1; dashRechargeT = 0f
        waveClearT = 0f; eliteBannerT = 0f; pendingConfirm = ""
        wave = 0; score = 0; kills = 0; gemsRun = 0; runTime = 0f
        combo = 0; comboT = 0f; comboBest = 0; muzzleT = 0f
        upgradeCounts.fill(0); lastKillsSeen = 0; lastGemsSeen = 0; bossIntroT = 0f
        shake = 0f; hurtFlash = 0f; runRecorded = false; newRecord = false
        enemies.clear(); projs.clear(); gemDrops.clear(); hearts.clear(); particles.clear(); texts.clear()
        ghosts.clear(); rings.clear(); decals.clear(); pools.clear(); sparks.clear(); bolts.clear()
        meteors.clear(); pendingSpawns.clear()
        px = w / 2; py = h / 2; pvx = 0f; pvy = 0f
        joyPointer = -1; joyDx = 0f; joyDy = 0f
        state = State.PLAYING
        nextWave()
        if (daily) texts.add(FloatText(w / 2, h * 0.3f, "📅 DAILY SEED RUN", 2f, Color.rgb(250, 204, 21), dp(22f)))
        audio.playMusic(R.raw.music_battle)
        audio.sfx("click")
    }

    private fun pauseGame() { if (state == State.PLAYING) { state = State.PAUSED; audio.playMusic(R.raw.music_menu) } }
    private fun resumeGame() { if (state == State.PAUSED) { state = State.PLAYING; audio.playMusic(R.raw.music_battle) } }

    private fun endRun(victory: Boolean) {
        if (!runRecorded) {
            newRecord = save.recordRun(score, wave, kills, gemsRun, victory, runTime.toLong())
            if (wave > save.bestWaveChar(save.selectedChar)) save.setBestWaveChar(save.selectedChar, wave)
            if (comboBest > save.bestCombo) save.bestCombo = comboBest
            if (dailyMode && score > save.dailyBest) { save.dailyBest = score; save.dailyDate = java.time.LocalDate.now().toString() }
            runRecorded = true
        }
        burst(px, py, CHARS[save.selectedChar].color, 60, 520f)
        if (victory) { // #77 confetti bursts
            burst(px - dp(60f), py - dp(40f), Color.rgb(244, 114, 182), 40, 460f)
            burst(px + dp(60f), py - dp(40f), Color.rgb(250, 204, 21), 40, 460f)
            rings.add(Ring(px, py, dp(14f), 0.8f, 0.8f, Color.rgb(250, 204, 21)))
        }
        shake = 14f
        if (victory) { audio.sfx("victory"); state = State.VICTORY }
        else { audio.sfx("explosion"); audio.sfx("gameover"); state = State.GAMEOVER }
        audio.playMusic(R.raw.music_menu)
        joyPointer = -1
    }

    private fun nextWave() {
        wave++
        bossWave = wave % 5 == 0
        waveBannerT = 2.0f
        mutator = 0
        if (!bossWave && wave >= 2 && pickRng().nextFloat() < 0.22f) mutator = pickRng().nextInt(1, 4)
        if (bossWave) {
            toSpawn = 3 + wave / 2
            spawnInterval = 2.2f
            spawnBoss()
            audio.sfx("boss")
            vibrate(120)
        } else {
            toSpawn = 6 + wave * 2
            spawnInterval = max(0.35f, 1.15f - wave * 0.045f)
        }
        if (mutator == 1) spawnInterval *= 0.65f // #119 Gem Rush: faster spawns
        waveTotal = toSpawn + enemies.size
        spawnT = 0.5f
    }

    // ---------- spawning ----------
    private fun spawnPos(): Pair<Float, Float> {
        val m = 60f
        return when (Random.nextInt(4)) {
            0 -> Pair(Random.nextFloat() * w, -m)
            1 -> Pair(Random.nextFloat() * w, h + m)
            2 -> Pair(-m, Random.nextFloat() * h)
            else -> Pair(w + m, Random.nextFloat() * h)
        }
    }

    private fun spawnEnemy(forceType: Int = -1) {
        val hpMul = 1f + (wave - 1) * 0.24f
        val spMul = (1f + (wave - 1) * 0.012f) * (1f + 0.1f * xpGreedLvl)
        val type = if (forceType >= 0) forceType else {
            val roll = pickRng().nextFloat()
            when {
                wave >= 4 && roll < 0.012f && !enemies.any { it.type == 11 } -> 11 // #118 Treasure Gnome
                wave >= 6 && roll < 0.07f -> 10 // #117 Bulwark Brute
                wave >= 5 && roll < 0.16f -> 9  // #116 Necroshard
                wave >= 3 && roll < 0.26f -> 2  // brute
                wave >= 4 && roll < 0.38f -> 7  // #114 Lancer
                wave >= 5 && roll < 0.48f -> 8  // #115 Orbiter
                wave >= 2 && roll < 0.60f -> 3  // splitter
                wave >= 3 && roll < 0.72f -> 6  // #113 Weaver
                roll < 0.86f -> 1               // dart
                else -> 0                       // chaser
            }
        }
        val (sx, sy) = spawnPos()
        val e = when (type) {
            1 -> Enemy(1, sx, sy, 14f * hpMul, 14f * hpMul, 265f * spMul, dp(13f), 8f, 1, 15)
            2 -> Enemy(2, sx, sy, 95f * hpMul, 95f * hpMul, 92f * spMul, dp(30f), 22f, 3, 60)
            3 -> Enemy(3, sx, sy, 30f * hpMul, 30f * hpMul, 150f * spMul, dp(20f), 12f, 2, 30)
            4 -> Enemy(4, sx, sy, 9f * hpMul, 9f * hpMul, 235f * spMul, dp(10f), 6f, 1, 8)
            6 -> Enemy(6, sx, sy, 20f * hpMul, 20f * hpMul, 190f * spMul, dp(15f), 10f, 2, 35)
            7 -> Enemy(7, sx, sy, 34f * hpMul, 34f * hpMul, 152f * spMul, dp(16f), 16f, 2, 45)
            8 -> Enemy(8, sx, sy, 24f * hpMul, 24f * hpMul, 205f * spMul, dp(15f), 10f, 3, 50)
            9 -> Enemy(9, sx, sy, 30f * hpMul, 30f * hpMul, 142f * spMul, dp(19f), 12f, 2, 35)
            10 -> Enemy(10, sx, sy, 125f * hpMul, 125f * hpMul, 82f * spMul, dp(31f), 24f, 4, 80)
            11 -> Enemy(11, sx, sy, 42f * hpMul, 42f * hpMul, 305f * spMul, dp(14f), 0f, 0, 250)
            else -> Enemy(0, sx, sy, 26f * hpMul, 26f * hpMul, 168f * spMul, dp(17f), 12f, 1, 20)
        }
        // ⭐ ELITE: rare, huge, gold-ringed, drops triple loot
        if ((mutator == 3 || (wave >= 3 && pickRng().nextFloat() < 0.09f)) && type != 11) {
            e.elite = true
            e.hp *= 2.6f; e.maxHp *= 2.6f; e.radius *= 1.35f
            e.xp *= 3; e.score *= 3; e.damage *= 1.3f
        }
        enemies.add(e)
        // #71/#89 spawn portal ring + swirl at the arena edge
        val cxp = sx.coerceIn(dp(16f), w - dp(16f)); val cyp = sy.coerceIn(dp(80f), h - dp(16f))
        rings.add(Ring(cxp, cyp, dp(8f), 0.4f, 0.4f, enemyColor(e.type)))
        if (particles.size < 300) for (i in 0 until 5) {
            val a = Random.nextFloat() * 6.283f
            particles.add(Particle(cxp + cos(a) * dp(20f), cyp + sin(a) * dp(20f), -cos(a) * 60f, -sin(a) * 60f, 0.35f, 0.35f, enemyColor(e.type), dp(2.4f)))
        }
    }

    private fun spawnBoss() {
        val hpMul = 1f + (wave - 1) * 0.24f
        val bossHp = 650f * hpMul * (1f + wave / 10f)
        val e = Enemy(5, w / 2, -80f, bossHp, bossHp, 105f, dp(46f), 30f, 25, 1000)
        enemies.add(e)
        bossBurst66 = false; bossBurst33 = false; bossSummoned = false; bossEnraged = false; bossAliveT = 0f
        bossIntroT = 2f
        texts.add(FloatText(w / 2, h * 0.3f, "⚠ ABYSS WARDEN ⚠", 2.2f, Color.rgb(244, 114, 182), dp(26f)))
    }

    private fun radialBurst(e: Enemy, n: Int, spd: Float) {
        for (i in 0 until n) {
            val a = (2 * Math.PI * i / n).toFloat() + e.wob
            projs.add(Proj(e.x, e.y, cos(a) * spd, sin(a) * spd, if (bossEnraged) 18f else 14f, 0, false, dp(7f), Color.rgb(244, 114, 182)))
        }
        audio.sfx("boss", 1.2f)
    }

    // ---------- main tick ----------
    private fun tick(dt: Float) {
        time += dt
        if (toastT > 0) toastT -= dt
        if (confirmT > 0) { confirmT -= dt; if (confirmT <= 0) pendingConfirm = "" }
        if (state != State.PLAYING) {
            updateFx(dt)
            return
        }
        runTime += dt
        if (muzzleT > 0) muzzleT -= dt
        if (comboT > 0) { comboT -= dt; if (comboT <= 0) combo = 0 }
        if (comboPop > 0) comboPop -= dt * 3f
        if (killsPop > 0) killsPop -= dt * 3f
        if (gemsPop > 0) gemsPop -= dt * 3f
        if (bossIntroT > 0) bossIntroT -= dt
        if (eliteBannerT > 0) eliteBannerT -= dt
        if (waveClearT > 0) waveClearT -= dt
        if (kills != lastKillsSeen) { lastKillsSeen = kills; killsPop = 1f }
        if (gemsRun != lastGemsSeen) { lastGemsSeen = gemsRun; gemsPop = 1f }
        if (waveBannerT > 0) waveBannerT -= dt
        if (shake > 0) shake = max(0f, shake - dt * 30f)
        if (hurtFlash > 0) hurtFlash = max(0f, hurtFlash - dt * 2.2f)
        if (dashCharges < dashMaxCharges) {
            dashRechargeT -= dt
            if (dashRechargeT <= 0) { dashCharges++; if (dashCharges < dashMaxCharges) dashRechargeT = dashCdMax }
        }
        if (dashT > 0) dashT -= dt
        if (invulnT > 0) invulnT -= dt
        if (regen > 0) hp = min(maxHp, hp + regen * dt)
        if (shieldLvl > 0 && shieldHp < shieldLvl * 20f && runTime - lastHurtAt > 3f)
            shieldHp = min(shieldLvl * 20f, shieldHp + 7f * dt) // #112 shield regen
        if (meteorLvl > 0 && enemies.isNotEmpty()) { // #102 Meteor Call timer
            meteorT -= dt
            if (meteorT <= 0f) {
                meteorT = max(2.5f, 6.5f - 0.7f * meteorLvl)
                val t = enemies[pickRng().nextInt(enemies.size)]
                meteors.add(Meteor(t.x, t.y, 0.8f))
            }
        }
        orbitAngle += dt * 3.4f

        // --- movement ---
        val spd = 335f * speedMul * (if (dashT > 0) 3.1f else 1f) *
            (if (adrenalineLvl > 0 && combo >= 10) 1f + 0.18f * adrenalineLvl else 1f) // #106 Adrenaline
        pvx = joyDx * spd; pvy = joyDy * spd
        px = (px + pvx * dt).coerceIn(dp(20f), w - dp(20f))
        py = (py + pvy * dt).coerceIn(dp(90f), h - dp(20f))
        if (hyp(pvx, pvy) > 200f && particles.size < 340)
            particles.add(Particle(px, py, -pvx * 0.06f, -pvy * 0.06f, 0.3f, 0.3f, CHARS[save.selectedChar].color, dp(3.2f)))
        if (dashT > 0) ghosts.add(Ghost(px, py, 0.34f, CHARS[save.selectedChar].color))

        // --- spawning / wave flow ---
        if (toSpawn > 0) {
            spawnT -= dt
            if (spawnT <= 0) { spawnEnemy(); toSpawn--; spawnT = spawnInterval * (0.7f + Random.nextFloat() * 0.6f) }
        } else if (enemies.isEmpty()) {
            waveClearBonus = 50 * wave; waveClearedNum = wave; waveClearT = 1.6f
            score += waveClearBonus
            hearts.add(HeartDrop(px + dp(46f), py - dp(20f))) // #133 wave-clear heart
            if (wave >= MAX_WAVE) { endRun(true); return }
            nextWave()
        }

        updateCombat(dt)
        updateFx(dt)

        if (hp <= 0f) {
            if (secondWind > 0) { // #108 Second Wind revive
                secondWind--; hp = maxHp * 0.5f; invulnT = 1.6f
                rings.add(Ring(px, py, dp(20f), 0.6f, 0.6f, Color.rgb(250, 204, 21)))
                burst(px, py, Color.rgb(250, 204, 21), 36, 460f)
                texts.add(FloatText(px, py - dp(56f), "💫 SECOND WIND!", 1.2f, Color.rgb(250, 204, 21), dp(19f)))
                for (e2 in enemies) {
                    val ddx = e2.x - px; val ddy = e2.y - py; val dd = max(1f, hyp(ddx, ddy))
                    if (dd < dp(170f)) { e2.vx = ddx / dd * 620f; e2.vy = ddy / dd * 620f }
                }
                audio.sfx("levelup"); vibrate(80)
            } else { hp = 0f; endRun(false) }
        }
    }

    private fun updateCombat(dt: Float) {
        val wp = WEAPONS[save.selectedWeapon]

        // --- auto-fire at nearest enemy ---
        fireT -= dt
        val target = enemies.minByOrNull { hyp(it.x - px, it.y - py) }
        if (target != null && fireT <= 0f) {
            fireT = 1f / (wp.rate * rateMul)
            val baseAng = atan2(target.y - py, target.x - px)
            lastAim = baseAng; muzzleT = 0.055f
            val count = wp.pellets + projBonus
            for (i in 0 until count) {
                val spread = when {
                    count == 1 -> 0f
                    wp.pellets > 1 -> (i - (count - 1) / 2f) * 0.16f
                    else -> (i - (count - 1) / 2f) * 0.07f
                }
                val a = baseAng + spread
                val pcol = if (wp.pellets > 1) { // #64 per-pellet tint ramp
                    val boost = i * 26
                    Color.rgb(min(255, Color.red(wp.color) + boost), min(255, Color.green(wp.color) + boost), min(255, Color.blue(wp.color) + boost))
                } else wp.color
                projs.add(Proj(px + cos(a) * dp(20f), py + sin(a) * dp(20f),
                    cos(a) * wp.projSpeed, sin(a) * wp.projSpeed,
                    wp.dmg * dmgMul, wp.pierce, true, dp(6f), pcol))
            }
            if (time - lastShootSfx > 0.07f) { audio.sfx("shoot", 0.9f + Random.nextFloat() * 0.25f); lastShootSfx = time }
        }

        // --- orbit blades ---
        val hasOrbit = save.selectedWeapon == 3
        val bladeR = dp(64f)
        val blades = if (hasOrbit) listOf(
            Pair(px + cos(orbitAngle) * bladeR, py + sin(orbitAngle) * bladeR),
            Pair(px + cos(orbitAngle + 3.14159f) * bladeR, py + sin(orbitAngle + 3.14159f) * bladeR)
        ) else emptyList()

        // --- enemies ---
        val dead = mutableListOf<Enemy>()
        // #102 meteor impacts (telegraph drawn in drawGame — that's #86)
        val deadMeteors = mutableListOf<Meteor>()
        for (m in meteors) {
            m.t -= dt
            if (m.t <= 0f) {
                deadMeteors.add(m)
                rings.add(Ring(m.x, m.y, dp(10f), 0.4f, 0.4f, Color.rgb(249, 115, 22)))
                burst(m.x, m.y, Color.rgb(249, 115, 22), 22, 420f)
                decals.add(Decal(m.x, m.y, dp(52f), 6f))
                audio.sfx("explosion", 0.9f)
                val mdmg = 42f * dmgMul * (1f + 0.3f * (meteorLvl - 1))
                for (e2 in enemies) if (e2.hp > 0 && hyp(e2.x - m.x, e2.y - m.y) < dp(72f) + e2.radius * 0.5f) damageEnemy(e2, mdmg, dead)
            }
        }
        meteors.removeAll(deadMeteors)
        for (e in enemies) {
            if (e.flash > 0) e.flash -= dt
            if (e.slowT > 0) e.slowT -= dt
            if (e.burnT > 0) e.burnT -= dt
            if (e.bladeT > 0) e.bladeT -= dt
            e.wob += dt * 3f
            val dx = px - e.x; val dy = py - e.y
            val dist = max(1f, hyp(dx, dy))
            var mvx = dx / dist; var mvy = dy / dist
            if (e.type == 1) { // dart weaves
                val perp = sin(e.wob) * 0.55f
                val nx = mvx - mvy * perp; val ny = mvy + mvx * perp
                val nl = max(1f, hyp(nx, ny)); mvx = nx / nl; mvy = ny / nl
            }
            if (e.type == 6) { // #113 Weaver: hard sine weave
                val perp = sin(e.wob * 2.3f) * 0.9f
                val nx = mvx - mvy * perp; val ny = mvy + mvx * perp
                val nl = max(1f, hyp(nx, ny)); mvx = nx / nl; mvy = ny / nl
            }
            if (e.type == 11) { mvx = -mvx; mvy = -mvy } // #118 Gnome flees the player
            if (e.type == 8) { // #115 Orbiter: strafes at range, fires orbs
                e.shootT -= dt
                if (e.shootT <= 0 && dist < dp(360f)) {
                    e.shootT = 2.6f
                    projs.add(Proj(e.x, e.y, dx / dist * 320f, dy / dist * 320f, 10f, 0, false, dp(6f), Color.rgb(165, 243, 252)))
                }
                val radial = when { dist > dp(215f) -> 1f; dist < dp(165f) -> -1f; else -> 0f }
                val nx = dx / dist * radial + (-dy / dist) * 0.95f * e.dir
                val ny = dy / dist * radial + (dx / dist) * 0.95f * e.dir
                val nl = max(1f, hyp(nx, ny)); mvx = nx / nl; mvy = ny / nl
            }
            if (e.type == 5) { // boss: advance, charge, radial bursts
                e.shootT -= dt
                if (e.shootT <= 0) {
                    e.shootT = if (e.hp < e.maxHp / 2) 1.5f else 2.2f
                    val n = 12
                    for (i in 0 until n) {
                        val a = (2 * Math.PI * i / n).toFloat() + e.wob
                        projs.add(Proj(e.x, e.y, cos(a) * 265f, sin(a) * 265f, 14f, 0, false, dp(7f), Color.rgb(244, 114, 182)))
                    }
                    audio.sfx("boss", 1.3f)
                }
                e.chargeT -= dt
                if (e.chargeT <= 0 && dist > dp(120f)) { e.vx = mvx * 620f; e.vy = mvy * 620f; e.chargeT = 2.8f }
            }
            if (e.type == 5) { // #122-124 boss abilities
                bossAliveT += dt
                val frac = e.hp / e.maxHp
                if (!bossBurst66 && frac <= 0.66f) { bossBurst66 = true; radialBurst(e, 16, 300f); texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "💥 WARDEN SURGE!", 1f, Color.rgb(244, 114, 182), dp(16f))) }
                if (!bossSummoned && frac <= 0.5f) { // #123 summon minions
                    bossSummoned = true
                    for (i in 0 until 4) {
                        val mt = if (i % 2 == 0) 0 else 1
                        val mh = if (mt == 0) 26f else 14f
                        val mhp = mh * (1f + (wave - 1) * 0.24f)
                        pendingSpawns.add(Enemy(mt, e.x + (i - 1.5f) * dp(38f), e.y + dp(30f), mhp, mhp,
                            if (mt == 0) 168f else 265f, if (mt == 0) dp(17f) else dp(13f), 12f, 1, 20))
                    }
                    texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "👹 MINIONS, RISE!", 1f, Color.rgb(244, 114, 182), dp(16f)))
                }
                if (!bossBurst33 && frac <= 0.33f) { bossBurst33 = true; radialBurst(e, 20, 330f); texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "💥 DESPERATE SURGE!", 1f, Color.rgb(248, 113, 113), dp(16f))) }
                if (!bossEnraged && bossAliveT > 90f) { bossEnraged = true; texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "🔥 WARDEN ENRAGED!", 1.4f, Color.rgb(248, 113, 113), dp(18f))); audio.sfx("boss", 0.8f) }
            }
            if (e.type == 7) { // #114 Lancer state machine
                e.stateT -= dt
                when (e.state) {
                    0 -> if (dist < dp(330f) && e.stateT <= 0f) { e.state = 1; e.stateT = 0.55f }
                    1 -> if (e.stateT <= 0f) { e.state = 2; e.stateT = 1.5f; e.vx = mvx * 900f; e.vy = mvy * 900f; audio.sfx("dash", 0.7f) }
                    2 -> if (e.stateT <= 0f) { e.state = 0; e.stateT = 1.4f }
                }
            }
            val slowMul = if (e.slowT > 0) 1f - min(0.55f, 0.22f * frostLvl) else 1f
            if (abs2(e.vx, e.vy) > 1f) {
                e.x += e.vx * dt; e.y += e.vy * dt
                e.vx *= (1f - 3.2f * dt); e.vy *= (1f - 3.2f * dt)
            } else {
                val spdMulE = (if (e.type == 5 && bossEnraged) 1.5f else 1f) * (if (e.type == 7 && e.state == 1) 0.15f else 1f)
                e.x += mvx * e.speed * slowMul * spdMulE * dt; e.y += mvy * e.speed * slowMul * spdMulE * dt
            }
            // orbit blade contact
            if (hasOrbit && e.bladeT <= 0) {
                for ((bx, by) in blades) {
                    if (hyp(bx - e.x, by - e.y) < e.radius + dp(15f)) {
                        damageEnemy(e, wp.dmg * dmgMul * 0.9f, dead)
                        e.bladeT = 0.35f
                        break
                    }
                }
            }
            // contact with player
            if (dist < e.radius + dp(17f) && invulnT <= 0 && dashT <= 0 && e.damage > 0) {
                hurtPlayer(e.damage)
                if (thornLvl > 0) damageEnemy(e, e.damage * 0.3f * thornLvl, dead) // #104 Thorn Aura
            }
        }

        if (pendingSpawns.isNotEmpty()) { enemies.addAll(pendingSpawns); pendingSpawns.clear() }

        // --- projectiles ---
        val deadProj = mutableListOf<Proj>()
        for (pr in projs) {
            pr.life -= dt
            pr.x += pr.vx * dt; pr.y += pr.vy * dt
            if (pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.65f)
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.16f, 0.16f, pr.color, dp(2.6f)))
            if (pr.life <= 0 || pr.x < -80 || pr.x > w + 80 || pr.y < -80 || pr.y > h + 80) { deadProj.add(pr); continue }
            if (pr.fromPlayer) {
                for (e in enemies) {
                    if (e.hp > 0 && hyp(pr.x - e.x, pr.y - e.y) < e.radius + pr.radius) {
                        damageEnemy(e, pr.dmg, dead, pr.x, pr.y)
                        if (pr.pierce > 0) pr.pierce-- else { deadProj.add(pr); break }
                    }
                }
            } else if (invulnT <= 0 && dashT <= 0 && hyp(pr.x - px, pr.y - py) < dp(16f) + pr.radius) {
                hurtPlayer(pr.dmg)
                deadProj.add(pr)
            }
        }
        projs.removeAll(deadProj)

        // --- deaths ---
        for (e in dead) {
            if (!enemies.remove(e)) continue
            kills++
            combo++; comboT = 2.5f
            if (combo > comboBest) comboBest = combo
            val mult = 1 + min(combo / 10, 4)
            score += e.score * mult
            if (save.dmgNumbers) texts.add(FloatText(e.x, e.y, "+${e.score * mult}", 0.6f,
                when { mult >= 4 -> Color.rgb(250, 204, 21); mult >= 2 -> Color.rgb(251, 146, 60); else -> Color.rgb(165, 243, 252) }, dp(12.5f)))
            if (combo % 10 == 0) { comboPop = 1f; texts.add(FloatText(px, py - dp(52f), "🔥 $combo COMBO ×$mult!", 0.9f, Color.rgb(251, 146, 60), dp(17f))) }
            if (lifesteal > 0 && hp > 0) {
                hp = min(maxHp, hp + lifesteal)
                if (Random.nextFloat() < 0.3f) texts.add(FloatText(px, py - dp(34f), "+${lifesteal}", 0.55f, Color.rgb(248, 113, 113), dp(12f)))
            }
            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)
            // #90 shard dissolve
            for (i in 0 until 6) {
                if (particles.size > 340) break
                val a = Random.nextFloat() * 6.283f; val s = 120f + Random.nextFloat() * 260f
                particles.add(Particle(e.x, e.y, cos(a) * s, sin(a) * s, 0.5f, 0.5f, enemyColor(e.type), dp(4.5f), 1, Random.nextFloat() * 6.28f))
            }
            // #74 scorch decal + #93 heat shimmer on big deaths
            if (e.type == 2 || e.type == 5 || e.elite) {
                decals.add(Decal(e.x, e.y, e.radius * 1.5f, 6f))
                for (i in 0 until 5) particles.add(Particle(e.x + Random.nextFloat() * 30 - 15, e.y, 0f, -130f, 0.7f, 0.7f, Color.rgb(254, 215, 170), dp(6f)))
            }
            // #83 frost nova on chilled death
            if (e.slowT > 0) {
                rings.add(Ring(e.x, e.y, e.radius, 0.4f, 0.4f, Color.rgb(147, 197, 253)))
                burst(e.x, e.y, Color.rgb(191, 219, 254), 10, 300f)
            }
            // #84 elite death lightning to player
            if (e.elite) bolts.add(Bolt(e.x, e.y, px, py, 0.14f))
            // #85 splitter leaves a poison pool
            if (e.type == 3) pools.add(PoisonPool(e.x, e.y, dp(40f), 3.5f))
            audio.sfx(if (e.type == 2 || e.type == 5) "explosion" else "hit", 0.8f + Random.nextFloat() * 0.4f)
            if (e.type == 5) { shake = 16f; vibrate(90) }
            if (e.elite) { eliteBannerT = 1.4f; save.elitesSlain++ }
            if (e.type == 5 || e.type == 2 || e.elite)
                rings.add(Ring(e.x, e.y, e.radius, 0.45f, 0.45f, if (e.elite) Color.rgb(250, 204, 21) else enemyColor(e.type)))
            if (e.type == 3) for (i in 0 until 2) { // splitter births minis
                val mini = Enemy(4, e.x + Random.nextFloat() * 30 - 15, e.y + Random.nextFloat() * 30 - 15,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 235f, dp(10f), 6f, 1, 8)
                enemies.add(mini)
            }
            if (e.type == 9) for (i in 0 until 3) { // #116 Necroshard births 3 darts
                val mini = Enemy(4, e.x + Random.nextFloat() * 36 - 18, e.y + Random.nextFloat() * 36 - 18,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 250f, dp(10f), 6f, 1, 8)
                enemies.add(mini)
            }
            if (poisonLvl > 0 && Random.nextFloat() < 0.10f + 0.08f * poisonLvl) // #103 Poison Vials
                pools.add(PoisonPool(e.x, e.y, dp(38f), 3.5f))
            if (e.type == 11) { // #118 Gnome looted: gem shower
                texts.add(FloatText(e.x, e.y - dp(20f), "💰 GNOME LOOTED! +60💎", 1.2f, Color.rgb(250, 204, 21), dp(17f)))
                audio.sfx("levelup")
                for (i in 0 until 12) {
                    val g = GemDrop(e.x + Random.nextFloat() * 70 - 35, e.y + Random.nextFloat() * 70 - 35, 5)
                    g.vx = Random.nextFloat() * 300 - 150; g.vy = Random.nextFloat() * 300 - 150
                    gemDrops.add(g)
                }
            }
            // 💚 heart drops: brutes/bosses/elites are generous
            val heartChance = when { e.type == 5 -> 1f; e.elite -> 0.5f; e.type == 2 -> 0.3f; else -> 0.04f }
            if (Random.nextFloat() < heartChance) {
                val hd = HeartDrop(e.x, e.y)
                hd.vx = Random.nextFloat() * 140 - 70; hd.vy = Random.nextFloat() * 140 - 70
                hearts.add(hd)
                if (e.type == 5) { val hd2 = HeartDrop(e.x + 30, e.y + 20); hearts.add(hd2) }
            }
            // gem drops
            var remaining = if (mutator == 1) e.xp * 2 else e.xp // #119 Gem Rush doubles loot
            while (remaining > 0) {
                val v = if (remaining >= 5) 5 else remaining
                val g = GemDrop(e.x + Random.nextFloat() * 36 - 18, e.y + Random.nextFloat() * 36 - 18, v)
                g.vx = Random.nextFloat() * 160 - 80; g.vy = Random.nextFloat() * 160 - 80
                gemDrops.add(g)
                remaining -= v
            }
        }

        // --- gems: drift, magnet, collect ---
        val gotGems = mutableListOf<GemDrop>()
        for (g in gemDrops) {
            val dx = px - g.x; val dy = py - g.y; val d = hyp(dx, dy)
            if (d < magnetR) g.mag = true
            if (g.mag && d > 1f) {
                val pull = 780f
                g.x += dx / d * pull * dt; g.y += dy / d * pull * dt
            } else {
                g.x += g.vx * dt; g.y += g.vy * dt; g.vx *= (1f - 4f * dt); g.vy *= (1f - 4f * dt)
            }
            if (d < dp(26f)) {
                if (rings.size < 12) rings.add(Ring(px, py, dp(6f), 0.25f, 0.25f, Color.rgb(52, 211, 153)))
                gotGems.add(g)
                gemsRun += g.value
                gainXp(g.value)
                if (time - lastPickupSfx > 0.05f) { audio.sfx("pickup", 0.95f + Random.nextFloat() * 0.2f); lastPickupSfx = time }
            }
        }
        gemDrops.removeAll(gotGems)

        // --- hearts: drift, magnet, collect to heal ---
        val gotHearts = mutableListOf<HeartDrop>()
        for (hd in hearts) {
            val dx = px - hd.x; val dy = py - hd.y; val d = hyp(dx, dy)
            if (d < magnetR) hd.mag = true
            if (hd.mag && d > 1f) { hd.x += dx / d * 700f * dt; hd.y += dy / d * 700f * dt }
            else { hd.x += hd.vx * dt; hd.y += hd.vy * dt; hd.vx *= (1f - 4f * dt); hd.vy *= (1f - 4f * dt) }
            if (d < dp(28f)) {
                gotHearts.add(hd)
                save.heartsCollected++
                hp = min(maxHp, hp + 15f)
                texts.add(FloatText(px, py - dp(40f), "+15 HP 💚", 0.8f, Color.rgb(74, 222, 128), dp(15f)))
                audio.sfx("pickup", 0.75f)
                burst(px, py, Color.rgb(74, 222, 128), 10, 260f)
            }
        }
        hearts.removeAll(gotHearts)
    }

    private fun abs2(x: Float, y: Float) = x * x + y * y
    private fun hyp(x: Float, y: Float): Float = sqrt(x * x + y * y)

    private fun damageEnemy(e: Enemy, raw: Float, dead: MutableList<Enemy>,
                            srcX: Float = px, srcY: Float = py, noChain: Boolean = false) {
        if (e.hp <= 0) return
        if (frostLvl > 0) e.slowT = 1.5f
        val crit = Random.nextFloat() < critCh
        var dmg = if (crit) raw * critMul else raw // #110 crit damage multiplier
        if (e.type == 10) { // #117 Bulwark Brute: frontal armor blocks 70%
            val toSrc = atan2(srcY - e.y, srcX - e.x); val toPlayer = atan2(py - e.y, px - e.x)
            var da = (toSrc - toPlayer) % 6.283f; if (da < 0) da = -da; if (da > 3.1416f) da = 6.283f - da
            if (da < 1.05f) {
                dmg *= 0.3f
                if (Random.nextFloat() < 0.4f) texts.add(FloatText(e.x, e.y - e.radius - dp(8f), "🛡️ BLOCKED", 0.5f, Color.rgb(148, 163, 184), dp(11f)))
            }
        }
        e.hp -= dmg
        e.flash = 0.09f
        if (crit) { sparks.add(Spark(e.x, e.y, 0.3f, Color.rgb(250, 204, 21), dp(13f))); if (save.dmgNumbers) texts.add(FloatText(e.x, e.y - e.radius, "💥 CRIT ${dmg.toInt()}", 0.65f, Color.rgb(250, 204, 21), dp(19f))) }
        else if (save.dmgNumbers) texts.add(FloatText(e.x + Random.nextFloat() * 16 - 8, e.y - e.radius, "${dmg.toInt()}", 0.45f, Color.rgb(226, 232, 240), dp(11.5f)))
        if (teslaLvl > 0 && !noChain && raw > 0.5f) { // #101 Tesla Coil chain lightning
            val targets = enemies.filter { it !== e && it.hp > 0 && hyp(it.x - e.x, it.y - e.y) < dp(160f) }
                .sortedBy { hyp(it.x - e.x, it.y - e.y) }.take(teslaLvl + 1)
            for (t in targets) {
                bolts.add(Bolt(e.x, e.y, t.x, t.y, 0.12f))
                t.hp -= raw * 0.4f; t.flash = 0.09f
                if (t.hp <= 0 && !dead.contains(t)) dead.add(t)
            }
        }
        if (e.hp <= 0 && !dead.contains(e)) dead.add(e)
    }

    private fun hurtPlayer(raw: Float) {
        var dmg = max(1f, raw - armor)
        lastHurtAt = runTime
        if (shieldHp > 0f) { // #112 Shield Generator absorbs first
            val absorbed = min(shieldHp, dmg)
            shieldHp -= absorbed; dmg -= absorbed
            sparks.add(Spark(px, py, 0.25f, Color.rgb(147, 197, 253), dp(16f)))
            if (dmg <= 0f) { invulnT = max(invulnT, 0.3f); audio.sfx("hit", 0.8f); return }
        }
        hp -= dmg
        invulnT = 0.45f
        hurtFlash = 0.85f
        shake = max(shake, 7f)
        audio.sfx("hurt")
        vibrate(45)
        burst(px, py, Color.rgb(248, 113, 113), 10, 300f)
        if (save.dmgNumbers) texts.add(FloatText(px, py - dp(30f), "-${dmg.toInt()}", 0.7f, Color.rgb(248, 113, 113), dp(16f)))
    }

    private fun gainXp(v: Int) {
        xp += (v * (1f + 0.25f * xpGreedLvl)).toInt().coerceAtLeast(v) // #111 XP Greed
        while (xp >= xpNext) {
            xp -= xpNext
            level++
            xpNext = 6 + level * 4
            score += 25
            offers = (0..23).shuffled(pickRng()).take(3)
            state = State.LEVELUP
            audio.sfx("levelup")
            burst(px, py, Color.rgb(52, 211, 153), 30, 420f)
        }
    }

    private fun applyUpgrade(idx: Int) {
        upgradeCounts[idx]++
        when (idx) {
            0 -> dmgMul *= 1.15f
            1 -> rateMul *= 1.15f
            2 -> speedMul *= 1.10f
            3 -> { maxHp += 25f; hp = min(maxHp, hp + 40f) }
            4 -> magnetR *= 1.45f
            5 -> critCh += 0.10f
            6 -> projBonus += 1
            7 -> regen += 0.9f
            8 -> dashCdMax *= 0.78f
            9 -> armor = min(5, armor + 1)
            10 -> lifesteal += 1.5f
            11 -> frostLvl++
            12 -> teslaLvl++
            13 -> meteorLvl++
            14 -> poisonLvl++
            15 -> thornLvl++
            16 -> magnetR *= 1.35f
            17 -> adrenalineLvl++
            18 -> { dmgMul *= 1.25f; maxHp = max(40f, maxHp - 15f); hp = min(hp, maxHp) }
            19 -> secondWind++
            20 -> { dashMaxCharges++; dashCharges++ }
            21 -> critMul += 0.5f
            22 -> xpGreedLvl++
            23 -> { shieldLvl++; shieldHp = shieldLvl * 20f }
        }
        audio.sfx("click")
        state = State.PLAYING
    }

    private fun doDash() {
        if (dashCharges <= 0 || state != State.PLAYING) return
        dashCharges--
        if (dashRechargeT <= 0f) dashRechargeT = dashCdMax
        dashT = 0.16f
        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))
        invulnT = max(invulnT, 0.22f)
        audio.sfx("dash")
        vibrate(20)
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int, speed: Float) {
        for (i in 0 until n) {
            if (particles.size > 320) break
            val a = Random.nextFloat() * 6.283f
            val s = speed * (0.25f + Random.nextFloat() * 0.75f)
            val life = 0.35f + Random.nextFloat() * 0.5f
            particles.add(Particle(x, y, cos(a) * s, sin(a) * s, life, life, color, dp(2.5f) + Random.nextFloat() * dp(3.5f)))
        }
    }

    private fun updateFx(dt: Float) {
        for (d in decals) d.life -= dt
        decals.removeAll { it.life <= 0 }
        for (pl in pools) pl.life -= dt
        pools.removeAll { it.life <= 0 }
        for (s in sparks) s.life -= dt
        sparks.removeAll { it.life <= 0 }
        for (b in bolts) b.life -= dt
        bolts.removeAll { it.life <= 0 }
        if (state == State.PLAYING) {
            for (pl in pools) {
                for (e in enemies) {
                    if (e.hp > 0 && hyp(e.x - pl.x, e.y - pl.y) < pl.r + e.radius * 0.5f) {
                        e.hp -= 9f * dt
                        if (e.hp <= 0) { val dl = mutableListOf<Enemy>(); damageEnemy(e, 0.01f, dl, noChain = true) }
                    }
                }
            }
        }
        for (g in ghosts) g.life -= dt
        ghosts.removeAll { it.life <= 0 }
        for (r in rings) { r.life -= dt; r.r += 300f * dt }
        rings.removeAll { it.life <= 0 }
        val deadP = particles.filter { it.life <= 0 }
        for (pt in particles) { pt.life -= dt; pt.x += pt.vx * dt; pt.y += pt.vy * dt; pt.vx *= (1f - 2.4f * dt); pt.vy *= (1f - 2.4f * dt) }
        particles.removeAll(deadP)
        val deadT = texts.filter { it.life <= 0 }
        for (t in texts) { t.life -= dt; t.y -= dt * 42f }
        texts.removeAll(deadT)
    }

    private fun vibrate(ms: Long) {
        if (!save.vibration) return
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vib.vibrate(ms)
        } catch (_: Exception) { }
    }

    private fun enemyColor(type: Int): Int = when (type) {
        0 -> Color.rgb(232, 121, 249)  // chaser magenta
        1 -> Color.rgb(250, 204, 21)   // dart yellow
        2 -> Color.rgb(248, 113, 113)  // brute red
        3 -> Color.rgb(74, 222, 128)   // splitter green
        4 -> Color.rgb(134, 239, 172)  // mini pale green
        5 -> Color.rgb(168, 85, 247)  // boss purple
        6 -> Color.rgb(96, 165, 250)   // weaver blue
        7 -> Color.rgb(244, 63, 94)    // lancer rose
        8 -> Color.rgb(45, 212, 191)   // orbiter teal
        9 -> Color.rgb(192, 38, 211)   // necroshard fuchsia
        10 -> Color.rgb(148, 163, 184) // bulwark slate
        else -> Color.rgb(250, 204, 21) // gnome gold
    }

    // ---------- input ----------
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val idx = e.actionIndex
        val pid = if (idx < e.pointerCount) e.getPointerId(idx) else -1
        val x = if (idx < e.pointerCount) e.getX(idx) else 0f
        val y = if (idx < e.pointerCount) e.getY(idx) else 0f
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (state == State.PLAYING) {
                    when {
                        pauseBtnRect.contains(x, y) -> pauseGame()
                        dashBtnRect.contains(x, y) -> doDash()
                        x < w * 0.55f && joyPointer == -1 -> {
                            joyPointer = pid; joyOx = x; joyOy = y; joyDx = 0f; joyDy = 0f
                        }
                        else -> if (x >= w * 0.55f) doDash()
                    }
                } else if (state == State.SETTINGS) {
                    if (!hitButton(x, y)) adjustSlider(x, y)
                } else {
                    hitButton(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (state == State.PLAYING) {
                    for (i in 0 until e.pointerCount) {
                        if (e.getPointerId(i) == joyPointer) {
                            val dx = e.getX(i) - joyOx; val dy = e.getY(i) - joyOy
                            val d = hyp(dx, dy); val maxR = dp(62f)
                            if (d > 1f) {
                                val cl = min(d, maxR)
                                joyDx = dx / d * (cl / maxR); joyDy = dy / d * (cl / maxR)
                                if (d > maxR) { joyOx = e.getX(i) - dx / d * maxR; joyOy = e.getY(i) - dy / d * maxR }
                            }
                        }
                    }
                } else if (state == State.SETTINGS) {
                    adjustSlider(e.getX(0), e.getY(0))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (pid == joyPointer) { joyPointer = -1; joyDx = 0f; joyDy = 0f }
            }
        }
        return true
    }

    private fun hitButton(x: Float, y: Float): Boolean {
        for (hb in hits) {
            if (hb.rect.contains(x, y)) { onButton(hb.id); return true }
        }
        return false
    }

    private fun onButton(id: String) {
        when {
            id == "play" -> startRun()
            id == "daily" -> startRun(true)
            id == "howto" -> { state = State.HOWTO; audio.sfx("click") }
            id == "shop" -> { state = State.SHOP; audio.sfx("click") }
            id == "settings" -> { state = State.SETTINGS; audio.sfx("click") }
            id == "stats" -> { state = State.STATS; audio.sfx("click") }
            id == "back" -> { toMenu(); audio.sfx("click") }
            id == "resume" -> resumeGame()
            id == "restart" -> startRun()
            id == "quit" -> { toMenu(); audio.sfx("click") }
            id == "vibrate" -> { save.vibration = !save.vibration; audio.sfx("click"); vibrate(30) }
            id == "dmgtoggle" -> { save.dmgNumbers = !save.dmgNumbers; audio.sfx("click") }
            id == "timertoggle" -> { save.showTimer = !save.showTimer; audio.sfx("click") }
            id == "hudtoggle" -> { save.bigHud = !save.bigHud; audio.sfx("click") }
            id == "hctoggle" -> { save.highContrast = !save.highContrast; audio.sfx("click") }
            id.startsWith("char:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsChar(i)) { save.selectedChar = i; audio.sfx("click") }
                else if (pendingConfirm == id) {
                    pendingConfirm = ""
                    if (save.gemBank >= CHARS[i].cost) { save.gemBank -= CHARS[i].cost; save.unlockChar(i); save.selectedChar = i; audio.sfx("levelup"); toast("🎉 Unlocked ${CHARS[i].name}!") }
                    else { audio.sfx("hurt"); toast("Not enough gems 💎") }
                }
                else if (save.gemBank >= CHARS[i].cost) { pendingConfirm = id; confirmT = 3f; audio.sfx("click"); toast("Tap again to confirm 💎 ${CHARS[i].cost}") }
                else { audio.sfx("hurt"); toast("Not enough gems — need 💎 ${CHARS[i].cost}") }
            }
            id.startsWith("weap:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsWeapon(i)) { save.selectedWeapon = i; audio.sfx("click") }
                else if (pendingConfirm == id) {
                    pendingConfirm = ""
                    if (save.gemBank >= WEAPONS[i].cost) { save.gemBank -= WEAPONS[i].cost; save.unlockWeapon(i); save.selectedWeapon = i; audio.sfx("levelup"); toast("🎉 Unlocked ${WEAPONS[i].name}!") }
                    else { audio.sfx("hurt"); toast("Not enough gems 💎") }
                }
                else if (save.gemBank >= WEAPONS[i].cost) { pendingConfirm = id; confirmT = 3f; audio.sfx("click"); toast("Tap again to confirm 💎 ${WEAPONS[i].cost}") }
                else { audio.sfx("hurt"); toast("Not enough gems — need 💎 ${WEAPONS[i].cost}") }
            }
            id.startsWith("up:") -> applyUpgrade(id.substringAfter(":").toInt())
        }
    }

    private val musicSlider = RectF()
    private val sfxSlider = RectF()
    private fun adjustSlider(x: Float, y: Float) {
        if (musicSlider.contains(x, y)) {
            save.musicVol = ((x - musicSlider.left) / musicSlider.width()).coerceIn(0f, 1f)
            audio.applyMusicVolume()
        } else if (sfxSlider.contains(x, y)) {
            save.sfxVol = ((x - sfxSlider.left) / sfxSlider.width()).coerceIn(0f, 1f)
        }
    }

    // ---------- drawing helpers ----------
    private fun txt(c: Canvas, s: String, cx: Float, y: Float, size: Float, color: Int, bold: Boolean = true, alpha: Int = 255) {
        p.style = Paint.Style.FILL
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = size; p.color = color; p.alpha = alpha
        p.textAlign = Paint.Align.CENTER
        c.drawText(s, cx, y, p)
        p.alpha = 255
    }

    private fun txtL(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.style = Paint.Style.FILL
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = size; p.color = color; p.textAlign = Paint.Align.LEFT
        c.drawText(s, x, y, p)
    }

    private fun txtR(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.style = Paint.Style.FILL
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = size; p.color = color; p.textAlign = Paint.Align.RIGHT
        c.drawText(s, x, y, p)
    }

    private fun roundRect(c: Canvas, r: RectF, radius: Float, color: Int, alpha: Int = 255) {
        p.style = Paint.Style.FILL; p.color = color; p.alpha = alpha
        c.drawRoundRect(r, radius, radius, p); p.alpha = 255
    }

    private fun button(c: Canvas, rect: RectF, label: String, fill: Int, textColor: Int = Color.WHITE,
                       size: Float = 20f, id: String, stroke: Int = 0) {
        roundRect(c, rect, dp(14f), fill)
        p.color = Color.WHITE; p.alpha = 20
        c.drawRoundRect(RectF(rect.left + dp(3f), rect.top + dp(3f), rect.right - dp(3f), rect.top + rect.height() * 0.45f), dp(11f), dp(11f), p)
        p.alpha = 255
        if (stroke != 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = stroke
            c.drawRoundRect(rect, dp(14f), dp(14f), p); p.style = Paint.Style.FILL
        }
        txt(c, label, rect.centerX(), rect.centerY() + size * 0.36f, dp(size), textColor)
        hits.add(HitBtn(RectF(rect), id))
    }

    /** Menu button with a circular icon medallion + label (#40). */
    private fun menuButton(c: Canvas, rect: RectF, icon: String, label: String, fill: Int, id: String,
                           size: Float = 18f, stroke: Int = 0, chipDp: Float = 30f) {
        button(c, rect, "", fill, Color.WHITE, size, id, stroke)
        p.color = Color.WHITE; p.alpha = 36
        c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(17f), p); p.alpha = 255
        txt(c, icon, rect.left + dp(chipDp), rect.centerY() + dp(7f), dp(16f), Color.WHITE)
        txt(c, label, rect.centerX() + dp(12f), rect.centerY() + size * 0.36f, dp(size), Color.WHITE)
    }

    private fun drawBackground(c: Canvas) {
        p.style = Paint.Style.FILL; p.color = Color.rgb(7, 3, 18)
        c.drawRect(0f, 0f, w, h, p)
        // #52 nebula blobs
        val neb = listOf(
            Triple(w * 0.22f + sin(time * 0.11f) * dp(20f), h * 0.3f + cos(time * 0.09f) * dp(16f), Color.rgb(88, 28, 135)),
            Triple(w * 0.8f + cos(time * 0.07f) * dp(24f), h * 0.62f + sin(time * 0.1f) * dp(18f), Color.rgb(30, 58, 138)),
            Triple(w * 0.55f + sin(time * 0.05f) * dp(14f), h * 0.85f, Color.rgb(80, 20, 80)))
        for ((nx, ny, ncol) in neb) {
            p.color = ncol; p.alpha = 26
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = 16
            c.drawCircle(nx, ny, dp(180f), p)
        }
        p.alpha = 255
        // neon grid — #53 pulses on the 126 BPM beat
        val beat = (sin(time * 6.6f) * 0.5f + 0.5f)
        p.color = Color.rgb(88, 28, 135); p.alpha = (if (save.highContrast) 95 else 46) + (beat * 14).toInt(); p.strokeWidth = 1.5f; p.style = Paint.Style.STROKE
        val gap = dp(54f)
        val off = (time * 12f) % gap
        var gx = -gap + off
        while (gx < w) { c.drawLine(gx, 0f, gx, h, p); gx += gap }
        var gy = -gap + off
        while (gy < h) { c.drawLine(0f, gy, w, gy, p); gy += gap }
        p.alpha = 255; p.style = Paint.Style.FILL
        // stars — #51 two-speed parallax drift (far layer crawls, near layer slides)
        for (s in stars) {
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 2f + s.phase))
            p.color = Color.rgb(165, 243, 252); p.alpha = (200 * tw).toInt()
            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
        }
        p.alpha = 255
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        hits.clear()
        val cd = "GEMSTORM — ${state.name.lowercase()} screen"
        if (contentDescription != cd) contentDescription = cd
        drawBackground(c)
        when (state) {
            State.MENU -> drawMenu(c)
            State.HOWTO -> drawHowTo(c)
            State.SHOP -> drawShop(c)
            State.SETTINGS -> drawSettings(c)
            State.STATS -> drawStats(c)
            State.PLAYING, State.LEVELUP, State.PAUSED -> {
                drawGame(c)
                if (state == State.LEVELUP) drawLevelUp(c)
                if (state == State.PAUSED) drawPaused(c)
            }
            State.GAMEOVER -> { drawGame(c); drawEnd(c, false) }
            State.VICTORY -> { drawGame(c); drawEnd(c, true) }
        }
        if (toastT > 0) {
            val twd = w * 0.72f
            val tr = RectF((w - twd) / 2, h - dp(118f), (w + twd) / 2, h - dp(76f))
            roundRect(c, tr, dp(21f), Color.rgb(23, 37, 64), 235)
            txt(c, toastMsg, w / 2, tr.centerY() + dp(6f), dp(14f), Color.rgb(250, 204, 21), true)
        }
    }

    // ---------- gameplay rendering ----------
    private fun poly(c: Canvas, cx: Float, cy: Float, r: Float, sides: Int, rot: Float, color: Int, fill: Boolean = true) {
        val path = Path()
        for (i in 0 until sides) {
            val a = rot + (2 * Math.PI * i / sides).toFloat()
            val vx = cx + cos(a) * r; val vy = cy + sin(a) * r
            if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
        }
        path.close()
        p.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        if (!fill) p.strokeWidth = dp(3f)
        p.color = color
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }

    private fun drawGame(c: Canvas) {
        val shx = if (shake > 0) (Random.nextFloat() - 0.5f) * shake else 0f
        val shy = if (shake > 0) (Random.nextFloat() - 0.5f) * shake else 0f
        c.save()
        c.translate(shx, shy)

        // #74 scorch decals
        for (d in decals) {
            p.color = Color.rgb(0, 0, 0); p.alpha = (90 * (d.life / 6f)).toInt().coerceIn(0, 90)
            c.drawCircle(d.x, d.y, d.r, p)
        }
        p.alpha = 255
        // #85 poison pools (drifting blobs)
        for (pl in pools) {
            val fade = min(1f, pl.life)
            p.color = Color.rgb(34, 197, 94); p.alpha = (44 * fade).toInt()
            c.drawCircle(pl.x, pl.y, pl.r, p)
            p.alpha = (60 * fade).toInt()
            c.drawCircle(pl.x + sin(time * 1.7f + pl.y) * dp(8f), pl.y + cos(time * 1.3f + pl.x) * dp(6f), pl.r * 0.62f, p)
            p.alpha = (80 * fade).toInt()
            c.drawCircle(pl.x + cos(time * 2.3f + pl.x) * dp(12f), pl.y + sin(time * 1.9f) * dp(9f), pl.r * 0.34f, p)
            p.alpha = 255
        }
        // #72 arena edge neon trim
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f)
        p.color = if (bossWave && enemies.any { it.type == 5 }) Color.rgb(220, 38, 38) else Color.rgb(168, 85, 247)
        p.alpha = 90
        c.drawRect(dp(5f), dp(5f), w - dp(5f), h - dp(5f), p)
        p.style = Paint.Style.FILL; p.alpha = 255
        if (enemies.any { it.type == 5 }) { // #82 boss arena tint
            p.color = Color.rgb(190, 18, 60); p.alpha = 13
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }

        // #31 magnet radius ring while moving
        if (state == State.PLAYING && (pvx != 0f || pvy != 0f)) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); p.color = Color.rgb(52, 211, 153)
            p.alpha = (26 + 14 * sin(time * 3.4f)).toInt().coerceIn(12, 40)
            c.drawCircle(px, py, magnetR, p); p.style = Paint.Style.FILL; p.alpha = 255
        }

        // gems
        for (g in gemDrops) {
            val big = g.value >= 5
            val r = if (big) dp(11f) else dp(7f)
            val bob = sin(time * 4f + g.x * 0.05f) * dp(2f)
            p.color = Color.rgb(52, 211, 153); p.alpha = 70
            c.drawCircle(g.x, g.y + bob, r * 1.9f, p); p.alpha = 255
            poly(c, g.x, g.y + bob, r, 4, 0.78f, Color.rgb(52, 211, 153))
            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))
            // #60 sparkle cross
            p.color = Color.WHITE; p.alpha = 190
            val sp = r * (0.7f + 0.3f * sin(time * 5f + g.x))
            c.drawRect(g.x - sp, g.y + bob - dp(0.8f), g.x + sp, g.y + bob + dp(0.8f), p)
            c.drawRect(g.x - dp(0.8f), g.y + bob - sp, g.x + dp(0.8f), g.y + bob + sp, p)
            p.alpha = 255
            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }
            if (g.mag) { // #75 magnet stream line to the player
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(52, 211, 153); p.alpha = 42
                c.drawLine(g.x, g.y + bob, px, py, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
        }

        // #91 merge glow when the field is rich with gems
        if (gemDrops.size > 20) {
            var mx = 0f; var my = 0f
            for (g in gemDrops) { mx += g.x; my += g.y }
            mx /= gemDrops.size; my /= gemDrops.size
            p.color = Color.rgb(52, 211, 153); p.alpha = (22 + 10 * sin(time * 3f)).toInt().coerceIn(12, 32)
            c.drawCircle(mx, my, dp(90f), p); p.alpha = 255
        }

        // hearts
        for (hd in hearts) {
            val pulse = 1f + 0.18f * sin(time * 6f + hd.x * 0.03f)
            val r = dp(10f) * pulse
            p.color = Color.rgb(74, 222, 128); p.alpha = 60
            c.drawCircle(hd.x, hd.y, r * 2f, p); p.alpha = 255
            p.color = Color.rgb(244, 114, 182)
            c.drawCircle(hd.x - r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            c.drawCircle(hd.x + r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            poly(c, hd.x, hd.y + r * 0.62f, r * 0.78f, 3, 3.14159f, Color.rgb(244, 114, 182))
            poly(c, hd.x, hd.y, r * 0.52f, 4, 0.78f, Color.rgb(249, 168, 212))
            p.color = Color.WHITE
            c.drawRect(hd.x - dp(1.6f), hd.y - dp(5f), hd.x + dp(1.6f), hd.y + dp(5f), p)
            c.drawRect(hd.x - dp(5f), hd.y - dp(1.6f), hd.x + dp(5f), hd.y + dp(1.6f), p)
        }

        // enemies
        for (e in enemies) {
            val col = when {
                e.flash > 0 -> Color.WHITE
                e.slowT > 0 -> Color.rgb(147, 197, 253)
                else -> enemyColor(e.type)
            }
            if (e.elite) { // ⭐ gold elite ring, rotating
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(250, 204, 21)
                c.drawCircle(e.x, e.y, e.radius * 1.3f + sin(time * 5f) * dp(2f), p)
                p.style = Paint.Style.FILL
                txt(c, "⭐", e.x, e.y - e.radius * 1.45f, dp(15f), Color.rgb(250, 204, 21))
                poly(c, e.x, e.y, e.radius * 1.52f, 4, time * 1.8f, Color.rgb(250, 204, 21), false)
            }
            // glow
            p.color = enemyColor(e.type); p.alpha = if (save.highContrast) 85 else 46
            c.drawCircle(e.x, e.y, e.radius * 1.7f, p); p.alpha = 255
            if (e.type == 1) { // #56 dart speed lines
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = col; p.alpha = 120
                val ba = atan2(e.y - py, e.x - px)
                for (k in -1..1) c.drawLine(e.x + cos(ba) * e.radius, e.y + sin(ba) * e.radius + k * dp(5f),
                    e.x + cos(ba) * e.radius * 2.1f, e.y + sin(ba) * e.radius * 2.1f + k * dp(5f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            when (e.type) {
                0 -> poly(c, e.x, e.y, e.radius * (1f + 0.13f * sin(e.wob * 3f)), 3, atan2(py - e.y, px - e.x) + 1.57f, col)
                1 -> poly(c, e.x, e.y, e.radius, 4, time * 5f, col)
                2 -> poly(c, e.x, e.y, e.radius, 6, time * 0.8f, col)
                3 -> poly(c, e.x, e.y, e.radius, 4, time * 1.6f, col)
                4 -> poly(c, e.x, e.y, e.radius * (1f + 0.2f * sin(time * 10f + e.wob)), 4, time * 3f, col)
                5 -> {
                    poly(c, e.x, e.y, e.radius, 8, time * 0.6f, col)
                    poly(c, e.x, e.y, e.radius * 0.62f, 8, -time * 0.9f, Color.rgb(233, 213, 255))
                    // crown spikes — #59 seven, gold-pulsed
                    for (i in 0 until 7) {
                        val a = -1.57f + (i - 3) * 0.38f
                        val spikeR = dp(7f) * (1f + 0.25f * sin(time * 4f + i))
                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR, 3, a, Color.rgb(250, 204, 21))
                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))
                    }
                }
                6 -> poly(c, e.x, e.y, e.radius, 5, time * 2.2f, col)
                7 -> poly(c, e.x, e.y, e.radius * 1.15f, 3, atan2(py - e.y, px - e.x) + 1.57f, col)
                8 -> { poly(c, e.x, e.y, e.radius, 6, -time * 2.6f, col); poly(c, e.x, e.y, e.radius * 0.45f, 4, time * 3f, Color.rgb(236, 254, 255)) }
                9 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.2f, col); poly(c, e.x, e.y, e.radius * 0.5f, 4, -time * 2f, Color.rgb(240, 171, 252)) }
                10 -> poly(c, e.x, e.y, e.radius, 6, 0.3f, col)
                11 -> { poly(c, e.x, e.y, e.radius, 4, 0.78f + sin(time * 6f) * 0.2f, col); poly(c, e.x, e.y - e.radius * 1.15f, dp(6f), 3, -1.57f, Color.rgb(34, 197, 94)) }
            }
            if (e.type == 7 && e.state == 1) { // #114 lancer telegraph line
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(248, 113, 113)
                p.alpha = (120 + 120 * sin(time * 30f)).toInt().coerceIn(30, 240)
                val ta = atan2(py - e.y, px - e.x)
                c.drawLine(e.x, e.y, e.x + cos(ta) * dp(330f), e.y + sin(ta) * dp(330f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 10) { // #117 bulwark shield arc faces the player
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(4f); p.color = Color.rgb(226, 232, 240); p.alpha = 200
                val fa = Math.toDegrees(atan2(py - e.y, px - e.x).toDouble()).toFloat()
                c.drawArc(RectF(e.x - e.radius * 1.15f, e.y - e.radius * 1.15f, e.x + e.radius * 1.15f, e.y + e.radius * 1.15f), fa - 55f, 110f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 5 && bossEnraged) { // #124 enrage aura
                p.color = Color.rgb(239, 68, 68); p.alpha = (50 + 30 * sin(time * 9f)).toInt().coerceIn(20, 80)
                c.drawCircle(e.x, e.y, e.radius * 1.45f, p); p.alpha = 255
            }
            if (e.type == 2 && e.hp < e.maxHp * 0.5f) { // #57 brute cracked armor
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(20, 4, 10); p.alpha = 200
                c.drawLine(e.x - e.radius * 0.6f, e.y - e.radius * 0.3f, e.x - e.radius * 0.1f, e.y + e.radius * 0.2f, p)
                c.drawLine(e.x - e.radius * 0.1f, e.y + e.radius * 0.2f, e.x + e.radius * 0.4f, e.y + e.radius * 0.1f, p)
                c.drawLine(e.x + e.radius * 0.2f, e.y - e.radius * 0.6f, e.x + e.radius * 0.35f, e.y - e.radius * 0.05f, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.slowT > 0) { // #66 frost crystals
                for (k in 0 until 3) {
                    val a = -0.9f + k * 0.9f + e.wob * 0.1f
                    poly(c, e.x + cos(a) * e.radius * 0.7f, e.y + sin(a) * e.radius * 0.7f - e.radius * 0.3f, dp(4.5f), 3, -1.57f, Color.rgb(191, 219, 254))
                }
            }
            if (e.burnT > 0) { // #67 ember tint (Pyro hook)
                p.color = Color.rgb(249, 115, 22); p.alpha = 90
                c.drawCircle(e.x, e.y, e.radius * 0.8f, p); p.alpha = 255
            }
            if (e.elite) { // #68 rotating ring sparkles
                for (k in 0 until 4) {
                    val a = time * 2.6f + k * 1.571f
                    poly(c, e.x + cos(a) * e.radius * 1.3f, e.y + sin(a) * e.radius * 1.3f, dp(3.6f), 4, a, Color.rgb(254, 240, 138))
                }
            }
            // eyes
            val ea = atan2(py - e.y, px - e.x)
            p.color = Color.rgb(10, 4, 24)
            c.drawCircle(e.x + cos(ea) * e.radius * 0.3f - dp(4f), e.y + sin(ea) * e.radius * 0.3f, dp(2.6f), p)
            c.drawCircle(e.x + cos(ea) * e.radius * 0.3f + dp(4f), e.y + sin(ea) * e.radius * 0.3f, dp(2.6f), p)
            // hp bar (damaged only, boss always at top instead)
            if (e.type != 5 && e.hp < e.maxHp) {
                val bw = e.radius * 2
                p.color = Color.rgb(30, 10, 40); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f), p)
                p.color = Color.rgb(74, 222, 128)
                c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f), p)
            }
        }

        // projectiles
        for (pr in projs) {
            p.color = pr.color; p.alpha = 70
            c.drawCircle(pr.x, pr.y, pr.radius * (2.1f + 0.25f * sin(time * 14f + pr.x * 0.05f)), p); p.alpha = 255
            if (pr.fromPlayer && pr.pierce > 0) { // #65 pulse lance beam: tail + core layers
                p.style = Paint.Style.STROKE; p.color = pr.color; p.alpha = 150; p.strokeWidth = pr.radius * 1.1f
                c.drawLine(pr.x - pr.vx * 0.045f, pr.y - pr.vy * 0.045f, pr.x, pr.y, p)
                p.color = Color.WHITE; p.alpha = 230; p.strokeWidth = pr.radius * 0.4f
                c.drawLine(pr.x - pr.vx * 0.03f, pr.y - pr.vy * 0.03f, pr.x, pr.y, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)
            p.color = Color.WHITE
            c.drawCircle(pr.x, pr.y, pr.radius * 0.45f, p)
        }

        // orbit blades
        if (save.selectedWeapon == 3 && state != State.GAMEOVER) {
            val bladeR = dp(64f)
            for (k in 0 until 2) {
                val bx = px + cos(orbitAngle + k * 3.14159f) * bladeR
                val by = py + sin(orbitAngle + k * 3.14159f) * bladeR
                p.color = Color.rgb(52, 211, 153); p.alpha = 60
                c.drawCircle(bx, by, dp(19f), p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(52, 211, 153); p.alpha = 110
                val ba = orbitAngle + k * 3.14159f
                c.drawArc(RectF(px - bladeR, py - bladeR, px + bladeR, py + bladeR), Math.toDegrees(ba.toDouble()).toFloat() - 50f, 42f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, bx, by, dp(13f), 3, orbitAngle * 3f + k * 2f, Color.rgb(52, 211, 153))
            }
        }

        // #70 dash ribbon through the ghosts
        if (ghosts.size > 1) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(10f); p.color = CHARS[save.selectedChar].color; p.alpha = 40
            val rp = Path()
            rp.moveTo(px, py)
            for (g in ghosts) rp.lineTo(g.x, g.y)
            c.drawPath(rp, p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // dash afterimage ghosts
        for (g in ghosts) {
            p.color = g.color; p.alpha = (130 * (g.life / 0.34f)).toInt().coerceIn(0, 130)
            poly(c, g.x, g.y, dp(19f), 4, 0.78f, g.color)
        }
        p.alpha = 255
        // shockwave rings
        for (r in rings) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f) * (r.life / r.maxLife) + 0.5f
            p.color = r.color; p.alpha = (200 * (r.life / r.maxLife)).toInt().coerceIn(0, 200)
            c.drawCircle(r.x, r.y, r.r, p)
        }
        p.style = Paint.Style.FILL; p.alpha = 255

        // #30/#29 off-screen direction arrows
        var arrowsDrawn = 0
        for (e in enemies) {
            if (arrowsDrawn >= 8) break
            if (e.x < -10 || e.x > w + 10 || e.y < -10 || e.y > h + 10) {
                val ax = e.x.coerceIn(dp(20f), w - dp(20f)); val ay = e.y.coerceIn(dp(80f), h - dp(30f))
                p.alpha = if (e.type == 5) 255 else 150
                poly(c, ax, ay, if (e.type == 5) dp(13f) else dp(8f), 3, atan2(e.y - ay, e.x - ax) + 1.57f, if (e.type == 5) Color.rgb(244, 114, 182) else enemyColor(e.type))
                p.alpha = 255
                arrowsDrawn++
            }
        }

        // player — gem guardian diamond
        if (state != State.GAMEOVER) {
            val chCol = CHARS[save.selectedChar].color
            val blink = invulnT > 0 && (time * 18f).toInt() % 2 == 0
            p.color = chCol; p.alpha = 55
            c.drawCircle(px, py, dp(34f), p); p.alpha = 255
            if (!blink) {
                val rot = sin(time * 2.2f) * 0.14f
                poly(c, px, py, dp(21f), 4, rot + 0.78f, chCol)
                poly(c, px, py, dp(12f), 4, rot + 0.78f, Color.rgb(236, 254, 255))
                poly(c, px, py, dp(5.5f), 4, rot + 0.78f, chCol)
            }
            // #54 rotating facet shine
            val shineA = time * 2.6f
            poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE)
            if (invulnT > 0 && dashT <= 0) { // #69 i-frame shield ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252)
                p.alpha = (120 + 80 * sin(time * 16f)).toInt().coerceIn(40, 200)
                c.drawCircle(px, py, dp(27f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (shieldHp > 0f && shieldLvl > 0) { // #112 shield bubble (arc = charge left)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(147, 197, 253); p.alpha = 170
                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (muzzleT > 0) { // muzzle flash star at the barrel
                val mx = px + cos(lastAim) * dp(26f); val my = py + sin(lastAim) * dp(26f)
                poly(c, mx, my, dp(9f), 4, time * 22f, Color.rgb(254, 240, 138))
            }
            if (dashT > 0) { // dash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol; p.alpha = 160
                c.drawCircle(px, py, dp(30f) + (0.16f - dashT) * dp(120f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            // #34 level badge
            p.color = Color.rgb(124, 58, 237)
            c.drawCircle(px + dp(17f), py + dp(15f), dp(9f), p)
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)
        }

        // #102/#86 meteors: shadow telegraph + falling streak
        for (m in meteors) {
            val frac = (m.t / 0.8f).coerceIn(0f, 1f)
            p.color = Color.BLACK; p.alpha = 120
            c.drawCircle(m.x, m.y, dp(30f) + dp(42f) * frac, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(249, 115, 22); p.alpha = 200
            c.drawCircle(m.x, m.y, dp(72f) * (1f - frac) + dp(8f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            if (m.t < 0.4f) {
                val off = dp(60f) * (m.t / 0.4f); val my2 = m.y - dp(300f) * (m.t / 0.4f)
                p.color = Color.rgb(249, 115, 22); p.alpha = 90
                c.drawCircle(m.x + off, my2, dp(20f), p)
                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255
            }
        }

        // particles & texts
        for (pt in particles) {
            p.color = pt.color; p.alpha = (255 * (pt.life / pt.maxLife)).toInt().coerceIn(0, 255)
            if (pt.shape == 1) poly(c, pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, 3, pt.rot + pt.life * 9f, pt.color)
            else c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, p)
        }
        p.alpha = 255
        // crit/elite sparks (4-point stars)
        for (s in sparks) {
            val sr = s.size * (s.life / 0.3f)
            poly(c, s.x, s.y, sr, 4, time * 9f, s.color)
            poly(c, s.x, s.y, sr * 0.45f, 4, time * 9f + 0.78f, Color.WHITE)
        }
        // lightning bolts (#84 renderer)
        for (b in bolts) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(253, 224, 71)
            p.alpha = (230 * (b.life / 0.14f)).toInt().coerceIn(0, 230)
            val bp = Path()
            bp.moveTo(b.x1, b.y1)
            val segs = 5
            for (i in 1 until segs) {
                val t = i / segs.toFloat()
                val jx = (Random.nextFloat() - 0.5f) * dp(16f); val jy = (Random.nextFloat() - 0.5f) * dp(16f)
                bp.lineTo(b.x1 + (b.x2 - b.x1) * t + jx, b.y1 + (b.y2 - b.y1) * t + jy)
            }
            bp.lineTo(b.x2, b.y2)
            c.drawPath(bp, p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // #73 vignette corners
        p.color = Color.BLACK; p.alpha = 30
        c.drawCircle(0f, 0f, dp(120f), p); c.drawCircle(w, 0f, dp(120f), p)
        c.drawCircle(0f, h, dp(120f), p); c.drawCircle(w, h, dp(120f), p)
        p.alpha = 255
        for (t in texts) {
            val ta = (255 * min(1f, t.life * 2)).toInt().coerceIn(0, 255)
            txt(c, t.text, t.x - dp(1.5f), t.y, t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x + dp(1.5f), t.y, t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y - dp(1.5f), t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y + dp(1.5f), t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y, t.size, t.color, true, ta)
        }
        if (hp > 0 && hp / maxHp < 0.25f && state == State.PLAYING) { // #76 low-HP desat wash
            p.color = Color.rgb(100, 116, 139); p.alpha = 34
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }
        c.restore()

        // hurt vignette
        if (hurtFlash > 0) {
            p.color = Color.rgb(220, 38, 38); p.alpha = (70 * hurtFlash).toInt()
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(10f) * hurtFlash + dp(1f)
            p.alpha = (150 * hurtFlash).toInt()
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // #5 low-HP heartbeat edge glow
        if (state == State.PLAYING && hp > 0 && hp / maxHp < 0.25f) {
            val pulseA = (60 + 55 * sin(time * 6.2f)).toInt().coerceIn(5, 115)
            p.color = Color.rgb(239, 68, 68); p.alpha = pulseA
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f)
            c.drawRect(dp(2f), dp(2f), w - dp(2f), h - dp(2f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // #39 last-stand tint
        if (state == State.PLAYING && hp > 0 && hp / maxHp < 0.10f) {
            p.color = Color.rgb(30, 27, 75); p.alpha = (40 + 25 * sin(time * 9f)).toInt().coerceIn(15, 65)
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)
        }
        // #15 HUD dims during boss intro
        if (bossIntroT > 0) {
            p.color = Color.rgb(7, 3, 18); p.alpha = (110 * min(1f, bossIntroT)).toInt().coerceIn(0, 110)
            c.drawRect(0f, 0f, w, dp(120f), p); p.alpha = 255
        }

        if (mutator == 2 && state == State.PLAYING) { // #120 Darkness: light radius around the player
            val dpath = Path()
            dpath.fillType = Path.FillType.EVEN_ODD
            dpath.addRect(0f, 0f, w, h, Path.Direction.CW)
            dpath.addCircle(px, py, dp(150f), Path.Direction.CCW)
            p.color = Color.rgb(2, 0, 10); p.alpha = 218
            c.drawPath(dpath, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 90
            c.drawCircle(px, py, dp(150f), p); p.style = Paint.Style.FILL; p.alpha = 255
        }

        drawHud(c)

        // wave banner
        if (waveBannerT > 0 && state == State.PLAYING) {
            val a = (min(1f, waveBannerT) * 255).toInt()
            txt(c, if (bossWave) "⚠ WAVE $wave — BOSS ⚠" else "WAVE $wave", w / 2, h * 0.3f, dp(30f),
                if (bossWave) Color.rgb(244, 114, 182) else Color.rgb(165, 243, 252), true, a)
            val sub = when {
                wave == 1 -> "Survive the storm!"
                bossWave -> "👑 Warden + ${max(0, toSpawn + enemies.size - 1)} minions — bring it down!"
                wave == MAX_WAVE -> "Final wave — end this!"
                else -> "${toSpawn + enemies.size} hostiles inbound"
            }
            txt(c, sub, w / 2, h * 0.3f + dp(34f), dp(15f), Color.WHITE, false, a)
            if (mutator > 0) txt(c, mutatorTitle(), w / 2, h * 0.3f + dp(58f), dp(14f), Color.rgb(250, 204, 21), true, a)
        }
        if (eliteBannerT > 0 && state == State.PLAYING) {
            val a2 = (min(1f, eliteBannerT) * 255).toInt().coerceIn(0, 255)
            txt(c, "⭐ ELITE DOWN ⭐", w / 2, h * 0.2f, dp(22f), Color.rgb(250, 204, 21), true, a2)
        }
        if (waveClearT > 0 && state == State.PLAYING) {
            val a3 = (min(1f, waveClearT) * 255).toInt().coerceIn(0, 255)
            txt(c, "✔ WAVE $waveClearedNum CLEAR!  +$waveClearBonus", w / 2, h * 0.38f, dp(19f), Color.rgb(52, 211, 153), true, a3)
        }
    }

    private fun drawHud(c: Canvas) {
        val m = dp(14f)
        val hs = if (save.bigHud) 1.25f else 1f
        // HP bar
        val barW = w * 0.42f; val barH = dp(15f)
        roundRect(c, RectF(m, m, m + barW, m + barH), dp(7f), Color.rgb(30, 10, 40), if (save.highContrast) 255 else 210)
        val hpFrac = (hp / maxHp).coerceIn(0f, 1f)
        val hpCol = when { hpFrac > 0.5f -> Color.rgb(74, 222, 128); hpFrac > 0.25f -> Color.rgb(250, 204, 21); else -> Color.rgb(248, 113, 113) }
        if (hpFrac < 0.25f) { // #1 low-HP pulse
            p.color = Color.rgb(248, 113, 113); p.alpha = (120 + 100 * sin(time * 7f)).toInt().coerceIn(20, 220)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f)
            c.drawRoundRect(RectF(m - dp(2f), m - dp(2f), m + barW + dp(2f), m + barH + dp(2f)), dp(9f), dp(9f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)
        txtL(c, "HP ${hp.toInt()}/${maxHp.toInt()}", m + dp(6f), m + barH - dp(3.5f), dp(10.5f) * hs, Color.WHITE, true)
        // XP bar
        val xpY = m + barH + dp(5f)
        roundRect(c, RectF(m, xpY, m + barW, xpY + dp(9f)), dp(4f), Color.rgb(30, 10, 40), if (save.highContrast) 255 else 210)
        val xpFrac = (xp.toFloat() / xpNext).coerceIn(0f, 1f)
        if (xpFrac > 0) roundRect(c, RectF(m, xpY, m + barW * xpFrac, xpY + dp(9f)), dp(4f), Color.rgb(52, 211, 153))
        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), Color.rgb(52, 211, 153), true)
        txtR(c, "$xp/$xpNext", m + barW - dp(4f), xpY + dp(8.5f), dp(8.5f), Color.WHITE, true)
        if (combo >= 3) { // 🔥 combo meter with decay bar
            val cmult = 1 + min(combo / 10, 4)
            txtL(c, "🔥 COMBO $combo  ×$cmult SCORE", m, xpY + dp(30f), dp(14f) * (1f + 0.45f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)
            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f, xpY + dp(41f)), dp(2f), Color.rgb(30, 10, 40), 200)
            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(41f)), dp(2f), Color.rgb(251, 146, 60))
            txtL(c, "BEST $comboBest", m + barW * 0.6f + dp(10f), xpY + dp(41f), dp(10.5f), Color.rgb(148, 163, 184), true)
        }
        // wave + score + gems
        txt(c, "WAVE $wave/$MAX_WAVE", w / 2, m + dp(16f), dp(16f) * hs, Color.rgb(233, 213, 255), true)
        if (waveTotal > 0) {
            val remain = toSpawn + enemies.size
            val wfrac = (1f - remain.toFloat() / waveTotal).coerceIn(0f, 1f)
            roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 + dp(42f), m + dp(27f)), dp(2f), Color.rgb(30, 10, 40), 200)
            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(27f)), dp(2f), Color.rgb(52, 211, 153))
        }
        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(40f), dp(11.5f), Color.rgb(148, 163, 184))
        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(14f) * hs, Color.WHITE, true)
        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))
        // boss bar
        val boss = enemies.firstOrNull { it.type == 5 }
        if (boss != null) {
            val bw = w * 0.7f; val bx = (w - bw) / 2; val by = m + dp(46f)
            roundRect(c, RectF(bx, by, bx + bw, by + dp(13f)), dp(6f), Color.rgb(30, 10, 40), 220)
            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))
            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)
            txt(c, "${boss.hp.toInt()} / ${boss.maxHp.toInt()}", w / 2, by + dp(11f), dp(9f), Color.WHITE, true)
        }
        // pause button
        pauseBtnRect.set(w - m - dp(56f), m, w - m, m + dp(56f))
        val pvR = RectF(pauseBtnRect.centerX() - dp(20f), pauseBtnRect.centerY() - dp(20f), pauseBtnRect.centerX() + dp(20f), pauseBtnRect.centerY() + dp(20f))
        roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 200)
        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)
        // joystick
        if (state == State.PLAYING) {
            if (joyPointer != -1) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = (150 + 60 * sin(time * 6f)).toInt().coerceIn(90, 210)
                c.drawCircle(joyOx, joyOy, dp(52f), p)
                p.color = Color.rgb(34, 211, 238); p.alpha = 60
                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(34f), p)
                p.style = Paint.Style.FILL; p.alpha = 200; p.color = Color.rgb(34, 211, 238)
                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f), p); p.alpha = 255
            } else {
                txtL(c, "◀ drag to move", m, h - dp(96f), dp(12f), Color.rgb(148, 163, 184))
            }
            // dash button
            val dr = dp(38f)
            dashBtnRect.set(w - m - dr * 2, h - dp(110f) - dr * 2, w - m, h - dp(110f))
            val ready = dashCharges > 0
            p.color = if (ready) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105); p.alpha = 190
            c.drawCircle(dashBtnRect.centerX(), dashBtnRect.centerY(), dr, p); p.alpha = 255
            if (!ready) {
                p.color = Color.rgb(7, 3, 18); p.alpha = 130
                val sweep = if (dashCharges < dashMaxCharges) (dashRechargeT / dashCdMax).coerceIn(0f, 1f) else 0f
                c.drawArc(dashBtnRect, -90f, 360f * sweep, true, p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(4f); p.color = Color.rgb(165, 243, 252); p.alpha = 220
                val inset = RectF(dashBtnRect.left + dp(3f), dashBtnRect.top + dp(3f), dashBtnRect.right - dp(3f), dashBtnRect.bottom - dp(3f))
                c.drawArc(inset, -90f, 360f * (1f - sweep), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            } else {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(165, 243, 252)
                p.alpha = (140 + 90 * sin(time * 5f)).toInt().coerceIn(50, 230)
                c.drawCircle(dashBtnRect.centerX(), dashBtnRect.centerY(), dr + dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "💨", dashBtnRect.centerX(), dashBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)
            txt(c, "DASH", dashBtnRect.centerX(), dashBtnRect.bottom + dp(15f), dp(10f), Color.rgb(165, 243, 252), true)
            if (dashMaxCharges > 1) {
                for (ci in 0 until dashMaxCharges) {
                    p.color = if (ci < dashCharges) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105)
                    c.drawCircle(dashBtnRect.centerX() + (ci - (dashMaxCharges - 1) / 2f) * dp(15f), dashBtnRect.bottom + dp(28f), dp(4.5f), p)
                }
            }
        }
    }

    private fun drawLevelUp(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 200; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)
        txt(c, "Choose an upgrade", w / 2, h * 0.2f + dp(30f), dp(15f), Color.rgb(203, 213, 225))
        val cardW = w * 0.86f; val cardH = dp(96f); val x0 = (w - cardW) / 2
        var y0 = h * 0.3f
        for (oi in offers) {
            val r = RectF(x0, y0, x0 + cardW, y0 + cardH)
            roundRect(c, RectF(x0, y0 + dp(5f), x0 + cardW, y0 + cardH + dp(5f)), dp(14f), Color.BLACK, 110)
            val rarityCol = when (oi) { 6, 10 -> Color.rgb(192, 132, 252); 5, 11 -> Color.rgb(96, 165, 250); else -> Color.rgb(52, 211, 153) }
            button(c, r, "", Color.rgb(30, 14, 56), Color.WHITE, 16f, "up:$oi", rarityCol)
            txt(c, UPGRADE_NAMES[oi], r.centerX(), y0 + dp(38f), dp(19f), Color.WHITE, true)
            txt(c, UPGRADE_DESCS[oi], r.centerX(), y0 + dp(66f), dp(13.5f), Color.rgb(165, 243, 252), false)
            if (upgradeCounts[oi] > 0) txt(c, "Owned ×${upgradeCounts[oi]}", r.centerX(), y0 + dp(87f), dp(11f), Color.rgb(250, 204, 21), true)
            y0 += cardH + dp(14f)
        }
    }

    private fun drawPaused(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 205; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(168, 85, 247); p.alpha = 170
        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p)
        p.style = Paint.Style.FILL; p.alpha = 255
        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)
        txt(c, "Wave $wave  •  Score $score  •  💎 $gemsRun", w / 2, h * 0.3f + dp(34f), dp(15f), Color.rgb(165, 243, 252))
        val bw = w * 0.6f; val bh = dp(56f); val x0 = (w - bw) / 2
        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")
        button(c, RectF(x0, h * 0.42f + bh + dp(14f), x0 + bw, h * 0.42f + bh * 2 + dp(14f)), "↻  RESTART RUN", Color.rgb(71, 85, 105), Color.WHITE, 17f, "restart")
        button(c, RectF(x0, h * 0.42f + bh * 2 + dp(28f), x0 + bw, h * 0.42f + bh * 3 + dp(28f)), "🏠  QUIT TO MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "quit")
    }

    private fun drawEnd(c: Canvas, victory: Boolean) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 215; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        if (!victory) { // #100 shattered gem artwork
            val gy3 = h * 0.13f
            poly(c, w / 2, gy3, dp(30f), 4, 0.78f, Color.rgb(90, 20, 40))
            poly(c, w / 2, gy3, dp(17f), 4, 0.78f, Color.rgb(50, 10, 26))
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(248, 113, 113); p.alpha = 220
            c.drawLine(w / 2, gy3 - dp(26f), w / 2 - dp(6f), gy3 - dp(2f), p)
            c.drawLine(w / 2 - dp(6f), gy3 - dp(2f), w / 2 + dp(10f), gy3 + dp(12f), p)
            c.drawLine(w / 2 - dp(6f), gy3 - dp(2f), w / 2 - dp(16f), gy3 + dp(14f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        if (victory) {
            txt(c, "🏆 VICTORY! 🏆", w / 2, h * 0.24f, dp(32f), Color.rgb(250, 204, 21), true)
            txt(c, "The Abyss is sealed. The storm is yours.", w / 2, h * 0.24f + dp(34f), dp(14.5f), Color.rgb(203, 213, 225))
        } else {
            txt(c, "💀 SHATTERED 💀", w / 2, h * 0.24f, dp(32f), Color.rgb(248, 113, 113), true)
            txt(c, "Your gem broke on Wave $wave. The storm rages on…", w / 2, h * 0.24f + dp(34f), dp(14.5f), Color.rgb(203, 213, 225))
        }
        if (newRecord) txt(c, "🌟 NEW HIGH SCORE! 🌟", w / 2, h * 0.24f + dp(66f), dp(16f), Color.rgb(52, 211, 153), true)
        val lines = listOf(
            "SCORE" to "$score",
            "WAVE REACHED" to "$wave / $MAX_WAVE",
            "KILLS" to "$kills",
            "TIME" to "${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}",
            "🔥 BEST COMBO" to "$comboBest",
            "💎 GEMS BANKED" to "+$gemsRun  (bank: ${save.gemBank})"
        )
        var y = h * 0.42f
        for ((k, v) in lines) {
            txtL(c, k, w * 0.18f, y, dp(15f), Color.rgb(148, 163, 184), true)
            txtR(c, v, w * 0.82f, y, dp(16f), Color.WHITE, true)
            y += dp(30f)
        }
        val bw = w * 0.62f; val bh = dp(56f); val x0 = (w - bw) / 2
        button(c, RectF(x0, h * 0.68f, x0 + bw, h * 0.68f + bh), "↻  RUN IT AGAIN", Color.rgb(22, 163, 74), Color.WHITE, 18f, "restart")
        button(c, RectF(x0, h * 0.68f + bh + dp(14f), x0 + bw, h * 0.68f + bh * 2 + dp(14f)), "🛒  SHOP", Color.rgb(124, 58, 237), Color.WHITE, 17f, "shop")
        button(c, RectF(x0, h * 0.68f + bh * 2 + dp(28f), x0 + bw, h * 0.68f + bh * 3 + dp(28f)), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "back")
    }

    // ---------- menu screens ----------
    private fun drawMenu(c: Canvas) {
        // #94 drifting gem silhouettes
        for (i in 0 until 3) {
            val gx2 = w * (0.12f + i * 0.36f) + sin(time * 0.13f + i * 2f) * dp(26f)
            val gy2 = (h * 0.62f + i * dp(52f) + sin(time * 0.09f + i) * dp(20f)) % h
            p.alpha = 13
            poly(c, gx2, gy2, dp(38f) + i * dp(9f), 4, 0.78f + time * 0.2f * (i + 1), Color.rgb(168, 85, 247))
            p.alpha = 255
        }
        // hero gem — big rotating diamond with glow pulse
        val cx = w / 2; val cy = h * 0.21f
        val pulse = 1f + 0.06f * sin(time * 2.4f)
        p.color = Color.rgb(168, 85, 247); p.alpha = 46
        c.drawCircle(cx, cy, dp(86f) * pulse, p)
        p.color = Color.rgb(34, 211, 238); p.alpha = 60
        c.drawCircle(cx, cy, dp(64f) * pulse, p); p.alpha = 255
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(232, 121, 249); p.alpha = 120
        for (k in 0 until 4) c.drawArc(RectF(cx - dp(74f), cy - dp(74f), cx + dp(74f), cy + dp(74f)), time * 40f + k * 90f, 52f, false, p)
        p.style = Paint.Style.FILL; p.alpha = 255
        poly(c, cx, cy, dp(52f) * pulse, 4, 0.78f + sin(time * 1.4f) * 0.1f, Color.rgb(34, 211, 238))
        poly(c, cx, cy, dp(30f) * pulse, 4, 0.78f, Color.rgb(168, 85, 247))
        poly(c, cx, cy, dp(15f) * pulse, 4, 0.78f, Color.rgb(236, 254, 255))

        // orbiting satellite gems
        for (i in 0 until 3) {
            val a = time * (0.9f + i * 0.23f) + i * 2.094f
            val ox = cx + cos(a) * dp(88f); val oy = cy + sin(a) * dp(30f)
            poly(c, ox, oy, dp(8f), 4, 0.78f + time * 2f, arrayOf(Color.rgb(244, 114, 182), Color.rgb(52, 211, 153), Color.rgb(250, 204, 21))[i])
        }
        // gentle gem rain behind the menu
        for (i in 0 until 9) {
            val fall = (time * (26f + i * 5f) + i * 233f) % (h + dp(90f))
            val gx = ((i * 0.618f + 0.07f) * w + sin(time * 0.8f + i) * dp(16f))
            p.alpha = 90
            poly(c, gx, fall - dp(45f), dp(6f) + (i % 3) * dp(1.6f), 4, 0.78f + time * (1f + i * 0.2f), Color.rgb(52, 211, 153))
            p.alpha = 255
        }
        val shimmer = 0.5f + 0.5f * sin(time * 2.2f)
        val titleCol = Color.rgb(
            (165 + (216 - 165) * shimmer).toInt(),
            (243 + (180 - 243) * shimmer).toInt(),
            (252 + (254 - 252) * shimmer).toInt())
        txt(c, "GEMSTORM", cx, cy + dp(108f), dp(40f), titleCol, true)
        val sweepX = ((time * 0.35f) % 1.6f - 0.3f) * w
        if (sweepX > cx - dp(160f) && sweepX < cx + dp(160f)) {
            p.color = Color.WHITE; p.alpha = 30
            c.drawRect(sweepX - dp(15f), cy + dp(74f), sweepX + dp(15f), cy + dp(114f), p); p.alpha = 255
        }
        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)
        txt(c, "20 waves. Bosses. Loot. Survive the storm.", cx, cy + dp(166f), dp(13.5f), Color.rgb(203, 213, 225), false)

        // loadout summary
        val ch = CHARS[save.selectedChar]; val wp = WEAPONS[save.selectedWeapon]
        txt(c, "${ch.icon} ${ch.name}   •   ${wp.icon} ${wp.name}", cx, h * 0.485f, dp(15f), Color.rgb(250, 204, 21), true)
        txt(c, "💎 Bank: ${save.gemBank}      🏆 Best: ${save.highScore}      🌊 Best wave: ${save.bestWave}", cx, h * 0.485f + dp(24f), dp(13f), Color.rgb(148, 163, 184))
        if (save.dailyBest > 0) txt(c, "📅 Daily seed best: ${save.dailyBest}  (${save.dailyDate})", cx, h * 0.485f + dp(46f), dp(12f), Color.rgb(94, 234, 212))

        val bw = w * 0.68f; val bh = dp(58f); val x0 = (w - bw) / 2
        var y0 = h * 0.55f
        menuButton(c, RectF(x0, y0, x0 + bw, y0 + bh), "▶", "ENTER THE ABYSS", Color.rgb(22, 163, 74), "play", 20f, Color.rgb(74, 222, 128))
        y0 += bh + dp(13f)
        menuButton(c, RectF(x0, y0, x0 + bw, y0 + bh), "🛒", "SHOP — HEROES & WEAPONS", Color.rgb(124, 58, 237), "shop", 16.5f)
        y0 += bh + dp(13f)
        val half = (bw - dp(13f)) / 2
        menuButton(c, RectF(x0, y0, x0 + half, y0 + bh), "❓", "HOW TO", Color.rgb(30, 41, 59), "howto", 14f, 0, 21f)
        menuButton(c, RectF(x0 + half + dp(13f), y0, x0 + bw, y0 + bh), "📊", "RECORDS", Color.rgb(30, 41, 59), "stats", 14f, 0, 21f)
        y0 += bh + dp(13f)
        menuButton(c, RectF(x0, y0, x0 + half, y0 + bh), "📅", "DAILY", Color.rgb(13, 60, 60), "daily", 14f, 0, 21f)
        menuButton(c, RectF(x0 + half + dp(13f), y0, x0 + bw, y0 + bh), "⚙️", "SETTINGS", Color.rgb(30, 41, 59), "settings", 14f, 0, 21f)
        txt(c, "by QamoTech 💎N🎱  •  v2.0 — 400-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), false)
    }

    private fun screenTitle(c: Canvas, title: String) {
        txt(c, title, w / 2, dp(64f), dp(25f), Color.rgb(165, 243, 252), true)
        val bw = dp(110f)
        button(c, RectF(dp(14f), dp(26f), dp(14f) + bw, dp(26f) + dp(42f)), "◀ BACK", Color.rgb(30, 41, 59), Color.WHITE, 14f, "back")
    }

    private fun drawHowTo(c: Canvas) {
        screenTitle(c, "❓ HOW TO PLAY")
        val lines = listOf(
            "🎯 GOAL" to "Survive all 20 waves of the Neon Abyss.",
            "🕹️ MOVE" to "Drag anywhere on the left side — virtual joystick.",
            "🔫 ATTACK" to "Automatic! Your weapon fires at the nearest foe.",
            "💨 DASH" to "Tap the DASH button (or right side) — brief invincibility.",
            "💎 GEMS" to "Fallen enemies drop gems. Grab them to level up AND bank them.",
            "⭐ LEVEL UP" to "Fill the XP bar, then pick 1 of 3 upgrades. Stack them!",
            "👑 BOSSES" to "An Abyss Warden hits every 5 waves. Dodge the radial bursts!",
            "🧩 ENEMIES" to "▲ Chaser  ◆ Dart (weaves)  ⬡ Brute (tank)  ■ Splitter (splits!)",
            "🛒 SHOP" to "Banked gems unlock new heroes & weapons — permanently.",
            "🔥 COMBO" to "Chain kills within 2.5s to multiply your score — up to ×5!",
            "⭐ ELITES" to "Gold-ringed elites are tougher but drop triple loot.",
            "💚 HEARTS" to "Brutes, elites and bosses drop hearts that heal 15 HP.",
            "🩸 BUILD SYNERGY" to "Vampire + Frost + Split Shot stacks turn crowds into health.",
            "⏸️ PAUSE" to "Tap ❚❚ top-right anytime. Combo & boss bars live in the HUD.",
            "💀 DEATH" to "You keep every banked gem. Upgrade, then run it again."
        )
        var y = dp(120f)
        for ((k, v) in lines) {
            txtL(c, k, dp(22f), y, dp(15.5f), Color.rgb(250, 204, 21), true)
            y += dp(21f)
            txtL(c, v, dp(22f), y, dp(13.5f), Color.rgb(226, 232, 240))
            if (k == "🕹️ MOVE") { // #96 joystick illustration
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = 160
                c.drawCircle(w - dp(52f), y - dp(14f), dp(20f), p)
                p.style = Paint.Style.FILL; p.color = Color.rgb(34, 211, 238); p.alpha = 200
                c.drawCircle(w - dp(45f), y - dp(20f), dp(10.5f), p); p.alpha = 255
            }
            y += dp(30f)
        }
    }

    private fun drawShop(c: Canvas) {
        screenTitle(c, "🛒 SHOP")
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)
        var y = dp(96f)
        txtL(c, "HEROES", dp(18f), y, dp(15f), Color.rgb(232, 121, 249), true); y += dp(8f)
        for (i in CHARS.indices) {
            val chd = CHARS[i]
            val owned = save.ownsChar(i); val sel = save.selectedChar == i
            val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
            roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
            if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = chd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
            p.color = chd.color; p.alpha = 46
            c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
            txt(c, chd.icon, dp(40f), y + dp(38f), dp(17f), Color.WHITE)
            txtL(c, chd.name, dp(70f), y + dp(24f), dp(16f), Color.WHITE, true)
            txtL(c, "HP ${chd.hp.toInt()} • SPD ×${chd.spd} • DMG ×${chd.dmg} — ${chd.desc}", dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184))
            val label = when { pendingConfirm == "char:$i" -> "❓ SURE?"; sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 ${chd.cost}" }
            val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
            val affordC = owned || save.gemBank >= chd.cost
            button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !affordC -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12.5f, "char:$i")
            y += dp(72f)
        }
        txtL(c, "WEAPONS", dp(18f), y + dp(6f), dp(15f), Color.rgb(232, 121, 249), true); y += dp(14f)
        for (i in WEAPONS.indices) {
            val wpd = WEAPONS[i]
            val owned = save.ownsWeapon(i); val sel = save.selectedWeapon == i
            val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
            roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
            if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = wpd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
            p.color = wpd.color; p.alpha = 46
            c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
            txt(c, wpd.icon, dp(40f), y + dp(38f), dp(17f), Color.WHITE)
            txtL(c, wpd.name, dp(70f), y + dp(24f), dp(16f), Color.WHITE, true)
            txtL(c, "DMG ${wpd.dmg.toInt()} • ROF ${wpd.rate}/s — ${wpd.desc}", dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184))
            val label = when { pendingConfirm == "weap:$i" -> "❓ SURE?"; sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 ${wpd.cost}" }
            val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
            val affordW = owned || save.gemBank >= wpd.cost
            button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !affordW -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12.5f, "weap:$i")
            y += dp(72f)
        }
        txt(c, "Unlocks are permanent. Earn gems in every run — win or lose.", w / 2, y + dp(14f), dp(12f), Color.rgb(100, 116, 139))
    }

    private fun drawSettings(c: Canvas) {
        screenTitle(c, "⚙️ SETTINGS")
        var y = dp(130f)
        txtL(c, "🎵 Music volume", dp(24f), y, dp(16f), Color.WHITE, true); y += dp(12f)
        musicSlider.set(dp(24f), y, w - dp(24f), y + dp(34f))
        roundRect(c, musicSlider, dp(17f), Color.rgb(30, 20, 50))
        if (save.musicVol > 0) roundRect(c, RectF(musicSlider.left, musicSlider.top, musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.bottom), dp(17f), Color.rgb(168, 85, 247))
        txt(c, "${(save.musicVol * 100).toInt()}%${when { save.musicVol <= 0.01f -> " • OFF"; save.musicVol < 0.4f -> " • LOW"; save.musicVol < 0.75f -> " • MED"; else -> " • MAX" }}", w / 2, y + dp(23f), dp(13f), Color.WHITE, true)
        y += dp(76f)
        txtL(c, "🔊 Sound effects volume", dp(24f), y, dp(16f), Color.WHITE, true); y += dp(12f)
        sfxSlider.set(dp(24f), y, w - dp(24f), y + dp(34f))
        roundRect(c, sfxSlider, dp(17f), Color.rgb(30, 20, 50))
        if (save.sfxVol > 0) roundRect(c, RectF(sfxSlider.left, sfxSlider.top, sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.bottom), dp(17f), Color.rgb(34, 211, 238))
        txt(c, "${(save.sfxVol * 100).toInt()}%${when { save.sfxVol <= 0.01f -> " • OFF"; save.sfxVol < 0.4f -> " • LOW"; save.sfxVol < 0.75f -> " • MED"; else -> " • MAX" }}", w / 2, y + dp(23f), dp(13f), Color.WHITE, true)
        y += dp(84f)
        val vibR = RectF(dp(24f), y, w - dp(24f), y + dp(54f))
        button(c, vibR, "📳 Vibration: ${if (save.vibration) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 16f, "vibrate")
        y += dp(66f)
        val dmgR = RectF(dp(24f), y, w - dp(24f), y + dp(54f))
        button(c, dmgR, "🔢 Damage numbers: ${if (save.dmgNumbers) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 16f, "dmgtoggle")
        y += dp(64f)
        val tmrR = RectF(dp(24f), y, w - dp(24f), y + dp(50f))
        button(c, tmrR, "⏱ Run timer: ${if (save.showTimer) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 15f, "timertoggle")
        y += dp(60f)
        val hudR = RectF(dp(24f), y, w - dp(24f), y + dp(50f))
        button(c, hudR, "🔎 Large HUD text: ${if (save.bigHud) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 15f, "hudtoggle")
        y += dp(60f)
        val hcR = RectF(dp(24f), y, w - dp(24f), y + dp(50f))
        button(c, hcR, "🌗 High contrast: ${if (save.highContrast) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 15f, "hctoggle")
        y += dp(76f)
        txtL(c, "🎨 Hero colors", dp(24f), y, dp(14f), Color.WHITE, true)
        for (i in CHARS.indices) {
            val sxp = dp(44f) + i * dp(54f); val syp = y + dp(18f)
            p.color = CHARS[i].color
            c.drawCircle(sxp, syp, dp(15f), p)
            if (save.selectedChar == i) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.WHITE
                c.drawCircle(sxp, syp, dp(19.5f), p); p.style = Paint.Style.FILL
            }
        }
        y += dp(52f)
        txtL(c, "🎼 Music composed in-engine for GEMSTORM:", dp(24f), y, dp(13.5f), Color.rgb(148, 163, 184)); y += dp(22f)
        txtL(c, "Menu — ambient pads in A minor • Battle — 126 BPM synthwave drive", dp(24f), y, dp(12.5f), Color.rgb(148, 163, 184)); y += dp(40f)
        txtL(c, "Animations always run at full strength. No reduced-motion, ever — house rule. 💎", dp(24f), y, dp(12.5f), Color.rgb(250, 204, 21))
        y += dp(20f)
        txtL(c, "🔋 Battery tip: if your phone runs hot, lower music/SFX above — the storm will wait.", dp(24f), y, dp(12.5f), Color.rgb(148, 163, 184))
    }

    private fun drawStats(c: Canvas) {
        screenTitle(c, "📊 RECORDS")
        val rows = listOf(
            "🏆 High score" to "${save.highScore}",
            "🌊 Best wave" to "${save.bestWave} / $MAX_WAVE",
            "🌊 Best wave (${CHARS[save.selectedChar].name})" to "${save.bestWaveChar(save.selectedChar)} / $MAX_WAVE",
            "💎 Gem bank" to "${save.gemBank}",
            "☠ Lifetime kills" to "${save.totalKills}",
            "🏃 Total runs" to "${save.totalRuns}",
            "👑 Victories" to "${save.victories}",
            "🔥 Best combo" to "${save.bestCombo}",
            "⭐ Elites slain" to "${save.elitesSlain}",
            "💚 Hearts collected" to "${save.heartsCollected}",
            "⏱ Total playtime" to "${save.totalPlaytime / 3600}h ${(save.totalPlaytime % 3600) / 60}m",
            "🧑‍🚀 Hero" to "${CHARS[save.selectedChar].icon} ${CHARS[save.selectedChar].name}",
            "🔫 Weapon" to "${WEAPONS[save.selectedWeapon].icon} ${WEAPONS[save.selectedWeapon].name}"
        )
        var y = dp(130f)
        for ((k, v) in rows) {
            roundRect(c, RectF(dp(16f), y - dp(24f), w - dp(16f), y + dp(12f)), dp(10f), Color.rgb(17, 12, 34), 220)
            txtL(c, k, dp(28f), y, dp(15.5f), Color.rgb(226, 232, 240), true)
            txtR(c, v, w - dp(28f), y, dp(15.5f), Color.rgb(52, 211, 153), true)
            val barFrac = when (k) {
                "☠ Lifetime kills" -> min(1f, save.totalKills / 1000f)
                "👑 Victories" -> min(1f, save.victories / 10f)
                "💎 Gem bank" -> min(1f, save.gemBank / 2000f)
                else -> -1f
            }
            if (barFrac >= 0) {
                roundRect(c, RectF(dp(28f), y + dp(5f), w - dp(28f), y + dp(9f)), dp(2f), Color.rgb(30, 10, 40), 200)
                if (barFrac > 0) roundRect(c, RectF(dp(28f), y + dp(5f), dp(28f) + (w - dp(56f)) * barFrac, y + dp(9f)), dp(2f), Color.rgb(52, 211, 153))
            }
            y += dp(48f)
        }
    }
}
