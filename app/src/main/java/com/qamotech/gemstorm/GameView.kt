package com.qamotech.gemstorm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
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

    enum class State { INTRO, MENU, HOWTO, SHOP, SETTINGS, STATS, PLAYING, LEVELUP, PAUSED, GAMEOVER, VICTORY, HEROES, WEAPONS, ONBOARD, WHATSNEW, ABOUT }

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
            CharDef("Bulwark", "🛡️", 190f, 0.82f, 1.18f, 900, Color.rgb(251, 146, 60), "A walking fortress. Slow, hits like a truck."),
            CharDef("Pyro", "🔥", 100f, 1.10f, 1.05f, 1200, Color.rgb(249, 115, 22), "His dash leaves a burning trail."),
            CharDef("Cryo", "❄️", 110f, 0.95f, 1.00f, 1200, Color.rgb(147, 197, 253), "A freezing aura slows nearby foes."),
            CharDef("💎N🎱 Turtle", "🐢", 130f, 1.02f, 1.02f, 0, Color.rgb(34, 197, 94), "A turtle with jets. Slow is smooth, smooth is fast. Free — a gift from 💎N🎱.")
        )
        val WEAPONS = listOf(
            WeaponDef("Gem Blaster", "🔫", 12f, 4.0f, 950f, 1, 0, 0, Color.rgb(34, 211, 238), "Reliable single shots."),
            WeaponDef("Scatter Nova", "🌟", 9f, 3.4f, 900f, 3, 0, 300, Color.rgb(244, 114, 182), "Three-way spread. Shreds crowds up close."),
            WeaponDef("Pulse Lance", "⚡", 10f, 7.0f, 1250f, 1, 3, 750, Color.rgb(167, 139, 250), "Piercing bolts at machine-gun pace."),
            WeaponDef("Orbit Blades", "🌀", 15f, 3.6f, 950f, 1, 0, 1500, Color.rgb(52, 211, 153), "Twin blades orbit you + a blaster. Area denial."),
            WeaponDef("Tesla Arc", "⚡", 9f, 5.0f, 1100f, 1, 0, 1200, Color.rgb(103, 232, 249), "Shots chain lightning to nearby foes."),
            WeaponDef("Meteor Staff", "☄️", 26f, 1.6f, 700f, 1, 0, 1600, Color.rgb(249, 115, 22), "Lobbed orbs explode in an area."),
            WeaponDef("Void Repeater", "🟣", 11f, 5.5f, 1150f, 1, 2, 2000, Color.rgb(192, 38, 211), "Piercing bolts siphon health on hit.")
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
        var slowT = 0f; var elite = false; var burnT = 0f; var spawnT = 0.32f; var frostAge = 0f
        var ghostHp = maxHp // #432 damage-ghost bar starts full
        var flash = 0f; var shootT = 1.5f + Random.nextFloat() * 1.5f; var bladeT = 0f
        var wob = Random.nextFloat() * 6.28f; var chargeT = 0f; var vx = 0f; var vy = 0f
        var state = 0; var stateT = 0f; var dir = if (Random.nextBoolean()) 1f else -1f
    }
    class Proj(var x: Float, var y: Float, var vx: Float, var vy: Float, val dmg: Float,
               var pierce: Int, val fromPlayer: Boolean, val radius: Float, val color: Int) { var life = 1.6f; var aoe = 0f; var rocket = false }
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
    class FirePool(var x: Float, var y: Float, val r: Float, var life: Float)
    class FloatText(var x: Float, var y: Float, val text: String, var life: Float, val color: Int, val size: Float) { val maxLife: Float = life; var spin = false }
    class FlyGem(val sx: Float, val sy: Float, val cx: Float, val cy: Float, val tx: Float, val ty: Float, val color: Int) { var t = 0f }
    class ShardGhost(var x: Float, var y: Float, val r: Float, val sides: Int, val color: Int, var life: Float)
    class FallShard(var x: Float, var y: Float, var vy: Float, var vx: Float, val r: Float, val sides: Int, val color: Int, val grav: Float) { var rot = Random.nextFloat() * 6.28f }
    class Beam(var x: Float, var y: Float, var life: Float, val color: Int = Color.rgb(250, 204, 21))
    class Bloom(var x: Float, var y: Float, var life: Float, val r: Float, val color: Int) { var maxLife = life }
    class Star(val x: Float, val y: Float, val size: Float, val phase: Float)
    class Ghost(var x: Float, var y: Float, var life: Float, val color: Int)
    class Ring(var x: Float, var y: Float, var r: Float, var life: Float, val maxLife: Float, val color: Int)
    class HitBtn(val rect: RectF, val id: String)

    // ---------- core ----------
    private val save = SaveData(context)
    val audio = AudioManager(context, save)
    private var state = State.INTRO // #793 the opening sequence plays first
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
            val rawDt = if (lastFrame == 0L) 0.016f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0.001f, 0.033f)
            avgFrameMs = avgFrameMs * 0.95f + rawDt * 1000f * 0.05f // #312 auto graphics watches frame time
            lastFrame = frameTimeNanos
            if (hitstopT > 0) { hitstopT -= rawDt; choreo.postFrameCallback(this); return } // #203/#204 hitstop freeze frame
            val dt = rawDt * timeScale
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
    private var bossGhostHp = 0f // #487
    private var playerGhostHp = 0f // #518
    private var comboPop = 0f; private var killsPop = 0f; private var gemsPop = 0f; private var scorePop = 0f // #492
    private var lastKillsSeen = 0; private var lastGemsSeen = 0
    private val upgradeCounts = IntArray(24)
    private var bossIntroT = 0f
    private var screenT = 0f; private var lastState = State.MENU
    private var pressId: String? = null; private var pressStartT = -10f
    private var spawnAnimT = 0f
    private var kbx = 0f; private var kby = 0f
    private var dashDx = 1f; private var dashDy = 0f
    private val flyGems = mutableListOf<FlyGem>()
    private val shardGhosts = mutableListOf<ShardGhost>()
    private val toggleAnim = mutableMapOf<String, Float>()
    private val fallShards = mutableListOf<FallShard>()
    private val beams = mutableListOf<Beam>()
    private var sliderDrag: String? = null; private var sliderGrow = 0f
    private var joySpringT = 0f; private var springOx = 0f; private var springOy = 0f; private var springKx = 0f; private var springKy = 0f
    private var dashReadyT = 0f; private var recoilT = 0f; private var orbitSpeed = 3.4f
    private var bossBoomT = 0f; private var bossBoomX = 0f; private var bossBoomY = 0f
    private var lastTouchAt = 0f
    private var hitstopT = 0f; private var footT = 0f; private var shardRainT = 0f
    private var eliteBannerT = 0f; private var waveClearT = 0f
    private var waveClearBonus = 0; private var waveClearedNum = 0; private var waveTotal = 1
    private var waveHurt = false; private var waveStartRun = 0f // #786/#789 flawless + speed trackers
    private var gemPickups = 0; private var comboSaved = false // #790/#792
    private var introT = 0f // #793 opening sequence clock
    private var dashCharges = 1; private var dashMaxCharges = 1; private var dashRechargeT = 0f
    private var rocketCharges = 3; private var rocketMaxCharges = 3; private var rocketRechargeT = 0f; private val rocketCdMax = 2.2f
    private var rocketReadyT = 0f // #481
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
    private val firePools = mutableListOf<FirePool>()
    private var endlessMode = false; private var victoryBanked = -1
    private var rerollTokens = 0; private var freeRerollUsed = false
    private var vacuumT = 0f; private var gemMergeT = 0f
    private var stillT = 0f; private var focusReady = false
    private var dodgeMeter = 0f
    private var slowmoT = 0f; private var timeScale = 1f
    private var bossStartT = 0f; private var firePoolT = 0f
    private var siphonT = 0f; private var siphonHp = 0f
    private var shopTab = 0
    private val blooms = mutableListOf<Bloom>()
    private var flashT = 0f; private var goldFlashT = 0f; private var saveSlowT = 0f; private var chromaT = 0f
    private var lastHurtAngle = 0f; private var lastHurtT = 0f
    private var heartBeatT = 0f
    private var heartPopT = 0f // v2.4: the one animated pickup heart (v2.6: no longer drawn — Nathan)
    private val heartPath = android.graphics.Path() // v2.6: true heart silhouette
    private var countT = 0f; private var countStage = 0
    private var lastHitSfx = 0f; private var lastSpawnSfx = 0f; private var lastPreviewT = 0f
    private var pickupChain = 0; private var pickupChainT = 0f

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
    private val rocketBtnRect = RectF()
    private val pauseBtnRect = RectF()

    init {
        isFocusable = true
        isFocusableInTouchMode = true // #366 hardware keyboard input
        contentDescription = "GEMSTORM game canvas" 
        audio.playMusic(R.raw.music_menu)
        if (!save.ownsChar(5)) save.unlockChar(5) // 💎N🎱 turtle: free for everyone — granted on first v2.3 launch
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); requestFocus() }

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
            State.MENU, State.HOWTO, State.SETTINGS, State.STATS,
            State.GAMEOVER, State.VICTORY, State.PAUSED -> audio.playMusic(R.raw.music_menu)
            State.SHOP -> audio.playMusic(R.raw.music_shop) // #277 shop theme
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
        State.HOWTO, State.SHOP, State.SETTINGS, State.STATS, State.HEROES, State.WEAPONS, State.ABOUT -> { toMenu(); true }
        State.LEVELUP -> true // must choose
        else -> false
    }

    // ---------- state transitions ----------
    private fun toast(msg: String) { toastMsg = msg; toastT = 2.2f }

    private fun toMenu() {
        state = State.MENU
        audio.playMusic(R.raw.music_menu)
    }

    private fun startRun(daily: Boolean = false, countStats: Boolean = true) {
        val ch = CHARS[save.selectedChar]
        val wp = WEAPONS[save.selectedWeapon]
        if (countStats) { save.addRunChar(save.selectedChar); save.addRunWeapon(save.selectedWeapon) } // #320/#323 run tallies
        maxHp = ch.hp; hp = maxHp
        level = 1; xp = 0; xpNext = 6
        dmgMul = ch.dmg; rateMul = 1f; speedMul = ch.spd
        magnetR = 110f * (1f + 0.10f * save.permMagnet) // #143 permanent magnet stacks
        critCh = 0.05f + 0.02f * save.permCrit // #142 permanent crit stacks
        regen = 0f; projBonus = 0; armor = 0
        dashCdMax = 2.2f * Math.pow(0.95, save.permDash.toDouble()).toFloat() // #144 dash servos
        lifesteal = 0f; frostLvl = 0
        teslaLvl = 0; meteorLvl = 0; poisonLvl = 0; thornLvl = 0; adrenalineLvl = 0
        secondWind = 0; xpGreedLvl = 0; shieldLvl = 0; shieldHp = 0f; critMul = 2f; meteorT = 5f
        mutator = 0; dailyMode = daily
        endlessMode = false; victoryBanked = -1; rerollTokens = 0; freeRerollUsed = false
        rerollTokens += save.bankedRerolls; if (save.bankedRerolls > 0) save.bankedRerolls = 0 // #311 banked rerolls join the run
        vacuumT = 0f; stillT = 0f; focusReady = false; dodgeMeter = 0f; slowmoT = 0f; timeScale = 1f
        if (daily) { dailyRng = Random(java.time.LocalDate.now().toEpochDay()); dailyDay = java.time.LocalDate.now().toEpochDay() } // #334 seed day kept for display + share code
        dashCd = 0f; dashT = 0f; invulnT = 0f; fireT = 0f
        dashMaxCharges = 1; dashCharges = 1; dashRechargeT = 0f
        rocketMaxCharges = 3; rocketCharges = 3; rocketRechargeT = 0f // 🚀 full rack every run
        waveClearT = 0f; eliteBannerT = 0f; pendingConfirm = ""
        wave = 0; score = 0; kills = 0; gemsRun = 0; runTime = 0f
        save.runSnap = ""; save.runSnapWave = 0 // #301 a fresh run clears any saved snapshot
        combo = 0; comboT = 0f; comboBest = 0; muzzleT = 0f; gemPickups = 0; comboSaved = false; waveHurt = false; waveStartRun = 0f
        deaths = 0 // #331
        resumeT = 0f; dashBufferT = 0f
        upgradeCounts.fill(0); lastKillsSeen = 0; lastGemsSeen = 0; bossIntroT = 0f
        shake = 0f; hurtFlash = 0f; runRecorded = false; newRecord = false
        enemies.clear(); projs.clear(); gemDrops.clear(); hearts.clear(); particles.clear(); texts.clear()
        ghosts.clear(); rings.clear(); decals.clear(); pools.clear(); sparks.clear(); bolts.clear()
        meteors.clear(); pendingSpawns.clear(); firePools.clear(); fallShards.clear()
        px = w / 2; py = h / 2; pvx = 0f; pvy = 0f
        spawnAnimT = 0.55f; kbx = 0f; kby = 0f; orbitSpeed = 0.7f // #159 materialize
        rings.add(Ring(px, py, dp(12f), 0.5f, 0.5f, CHARS[save.selectedChar].color))
        joyPointer = -1; joyDx = 0f; joyDy = 0f
        state = State.PLAYING
        nextWave()
        if (daily) texts.add(FloatText(w / 2, h * 0.3f, "📅 DAILY SEED RUN", 2f, Color.rgb(250, 204, 21), dp(22f)))
        audio.crossfadeTo(R.raw.music_battle) // #346 menu music crossfades into battle
        audio.sfx("click")
    }

    // #301 run snapshot: saved on pause/quit; the menu offers CONTINUE (resumes at wave start)
    private fun snapshotRun() {
        if (runRecorded || wave < 1 || save.runSnap.isNotEmpty() && save.runSnapWave == wave) return
        val sb = StringBuilder()
        sb.append(wave).append(',').append(score).append(',').append(kills).append(',').append(gemsRun).append(',')
        sb.append(runTime).append(',').append(level).append(',').append(xp).append(',').append(xpNext).append(',')
        sb.append(hp / maxHp).append(',').append(maxHp).append(',').append(dmgMul).append(',').append(rateMul).append(',')
        sb.append(speedMul).append(',').append(magnetR).append(',').append(critCh).append(',').append(critMul).append(',')
        sb.append(regen).append(',').append(projBonus).append(',').append(armor).append(',').append(dashCdMax).append(',')
        sb.append(lifesteal).append(',').append(frostLvl).append(',').append(teslaLvl).append(',').append(meteorLvl).append(',')
        sb.append(poisonLvl).append(',').append(thornLvl).append(',').append(adrenalineLvl).append(',').append(secondWind).append(',')
        sb.append(xpGreedLvl).append(',').append(shieldLvl).append(',').append(dashMaxCharges).append(',').append(rerollTokens).append(',')
        sb.append(if (freeRerollUsed) 1 else 0).append(',').append(if (endlessMode) 1 else 0).append(',').append(if (dailyMode) 1 else 0).append(',')
        sb.append(comboBest).append(',').append(upgradeCounts.joinToString("."))
        save.runSnap = sb.toString(); save.runSnapWave = wave
    }

    private fun continueRun() {
        val parts = save.runSnap.split(',')
        if (parts.size < 37) { toast("No saved run"); return }
        val daily = parts[34] == "1"
        startRun(daily, countStats = false)
        var i = 0
        fun f() = parts[i++].toFloat()
        fun n() = parts[i++].toInt()
        wave = n() - 1; score = n(); kills = n(); gemsRun = n(); runTime = f()
        level = n(); xp = n(); xpNext = n()
        val hpFrac = f(); maxHp = f(); dmgMul = f(); rateMul = f(); speedMul = f(); magnetR = f()
        critCh = f(); critMul = f(); regen = f(); projBonus = n(); armor = n(); dashCdMax = f()
        lifesteal = f(); frostLvl = n(); teslaLvl = n(); meteorLvl = n(); poisonLvl = n(); thornLvl = n()
        adrenalineLvl = n(); secondWind = n(); xpGreedLvl = n(); shieldLvl = n(); dashMaxCharges = n()
        rerollTokens = n(); freeRerollUsed = n() == 1; endlessMode = n() == 1; i++
        comboBest = n()
        val ups = parts[36].split('.')
        for (u in ups.indices) if (u < upgradeCounts.size) upgradeCounts[u] = ups[u].toInt()
        hp = hpFrac * maxHp; shieldHp = shieldLvl * 20f; dashCharges = dashMaxCharges
        save.runSnap = ""; save.runSnapWave = 0
        state = State.PLAYING
        audio.playMusic(if (endlessMode) R.raw.music_endless else R.raw.music_battle)
        nextWave()
        toast("⏩ Run restored — wave $wave!")
    }

    // #331 victory rank: clear time + deaths
    private fun rankLetter(): String = when {
        deaths == 0 && runTime < 540f -> "S"
        deaths <= 1 && runTime < 720f -> "A"
        deaths <= 2 && runTime < 960f -> "B"
        else -> "C"
    }
    private fun rankScore(): Int = when (rankLetter()) { "S" -> 4; "A" -> 3; "B" -> 2; else -> 1 }

    private fun pauseGame() { if (state == State.PLAYING) { snapshotRun(); state = State.PAUSED; audio.playMusic(R.raw.music_menu) } }
    private fun resumeGame() { if (state == State.PAUSED) { state = State.PLAYING; resumeT = 1.5f; audio.playMusic(if (endlessMode) R.raw.music_endless else R.raw.music_battle) } } // #278 + #391 countdown

    private fun endRun(victory: Boolean) {
        save.runSnap = ""; save.runSnapWave = 0 // #301 a finished run leaves nothing to continue
        if (endlessMode && wave > save.bestEndlessWave) save.bestEndlessWave = wave // #347
        if (!runRecorded) {
            newRecord = save.recordRun(score, wave, kills, gemsRun, victory, runTime.toLong())
            save.totalGemsEarned += gemsRun // #322 lifetime gems earned
            if (wave > save.bestWaveChar(save.selectedChar)) save.setBestWaveChar(save.selectedChar, wave)
            if (comboBest > save.bestCombo) save.bestCombo = comboBest
            if (dailyMode && score > save.dailyBest) { save.dailyBest = score; save.dailyDate = java.time.LocalDate.now().toString() }
            if (victory) { victoryBanked = gemsRun; if (rankScore() > save.bestRank) save.bestRank = rankScore() } // #331 best rank saved
            runRecorded = true
        } else if (!victory && victoryBanked >= 0) {
            save.gemBank += gemsRun - victoryBanked // endless tail earnings still bank
            save.totalGemsEarned += gemsRun - victoryBanked // #322
            victoryBanked = -1
        }
        burst(px, py, CHARS[save.selectedChar].color, 60, 520f)
        if (victory) { // #179 rising gem fountain
            for (i in 0 until 26) { val fc = arrayOf(Color.rgb(250, 204, 21), Color.rgb(52, 211, 153), Color.rgb(34, 211, 238), Color.rgb(244, 114, 182))[i % 4]
                fallShards.add(FallShard(w / 2 + Random.nextFloat() * 60 - 30, h * 0.98f, -(520f + Random.nextFloat() * 380f), Random.nextFloat() * 170 - 85, dp(4f) + Random.nextFloat() * dp(4f), 4, fc, 560f)) }
            for (i in 0 until 18) { val fc = arrayOf(Color.rgb(244, 114, 182), Color.rgb(250, 204, 21), Color.rgb(52, 211, 153), Color.rgb(34, 211, 238))[i % 4] // #205 confetti gems drift down
                fallShards.add(FallShard(Random.nextFloat() * w, -dp(12f) - Random.nextFloat() * dp(120f), 55f + Random.nextFloat() * 70f, Random.nextFloat() * 30 - 15, dp(4.5f), 4, fc, 55f)) }
        } else { // #178 shattered gem: slow-falling shards
            for (i in 0 until 16) { val fc = arrayOf(Color.rgb(190, 18, 60), Color.rgb(124, 58, 237), Color.rgb(90, 20, 40))[i % 3]
                fallShards.add(FallShard(w / 2 + Random.nextFloat() * 76 - 38, h * 0.13f + Random.nextFloat() * 20 - 10, 26f + Random.nextFloat() * 44f, Random.nextFloat() * 34 - 17, dp(3.5f) + Random.nextFloat() * dp(5f), 3, fc, 120f)) }
        }
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
        countT = 1.5f; countStage = 3 // #294 3-2-1 countdown before the wave pours in
        waveHurt = false; waveStartRun = runTime // #786/#789 a fresh flawless + speed clock each wave
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
        val hpMul = (1f + (wave - 1) * 0.24f) * (if (save.hardMode) 1.3f else 1f) // #149 hard mode
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
            beams.add(Beam(sx.coerceIn(dp(16f), w - dp(16f)), sy.coerceIn(dp(80f), h - dp(16f)), 0.55f)) // #187 beam of light
            audio.sfx("horn", 0.9f) // #258 elite spawn horn
        }
        enemies.add(e)
        // #71/#89 spawn portal ring + swirl at the arena edge
        val cxp = sx.coerceIn(dp(16f), w - dp(16f)); val cyp = sy.coerceIn(dp(80f), h - dp(16f))
        rings.add(Ring(cxp, cyp, dp(8f), 0.4f, 0.4f, enemyColor(e.type)))
        rings.add(Ring(cxp, cyp, dp(4f), 0.28f, 0.28f, Color.WHITE)) // #508 inner portal swirl ring
        sparks.add(Spark(cxp, cyp, 0.3f, enemyColor(e.type), dp(10f))); sparks.add(Spark(cxp + dp(10f), cyp - dp(6f), 0.3f, Color.WHITE, dp(7f))) // #223 portal sparks
        if (e.elite) sparks.add(Spark(cxp, cyp, 0.4f, Color.rgb(250, 204, 21), dp(12f))) // #612 elite portals flare gold
        if (e.type == 6 && Random.nextFloat() < 0.7f) audio.sfx("warp", 0.85f + Random.nextFloat() * 0.3f) // #270 weaver warp blip
        if (time - lastSpawnSfx > 0.18f && Random.nextFloat() < 0.5f) { audio.sfx("spawn", 0.9f + Random.nextFloat() * 0.2f); lastSpawnSfx = time } // #296 spawn warning blip
        if (particles.size < 300) for (i in 0 until 5) {
            val a = Random.nextFloat() * 6.283f
            particles.add(Particle(cxp + cos(a) * dp(20f), cyp + sin(a) * dp(20f), -cos(a) * 60f, -sin(a) * 60f, 0.35f, 0.35f, enemyColor(e.type), dp(2.4f)))
        }
    }

    private fun spawnBoss() {
        val hpMul = (1f + (wave - 1) * 0.24f) * (if (save.hardMode) 1.3f else 1f) // #149 hard mode
        val bossHp = 650f * hpMul * (1f + wave / 10f)
        val e = Enemy(5, w / 2, -80f, bossHp, bossHp, 105f, dp(46f), 30f, 25, 1000)
        enemies.add(e)
        e.spawnT = 0.95f // #199 rise-in (scale formula clamps the first stretch)
        decals.add(Decal(w / 2, dp(120f), dp(56f), 8f)); decals.add(Decal(w / 2, dp(120f), dp(30f), 8f))
        burst(w / 2, dp(110f), Color.rgb(120, 113, 108), 18, 300f)
        rings.add(Ring(w / 2, dp(110f), dp(14f), 0.6f, 0.6f, Color.rgb(168, 85, 247)))
        bossBurst66 = false; bossBurst33 = false; bossSummoned = false; bossEnraged = false; bossAliveT = 0f
        bossStartT = runTime
        bossIntroT = 2f
        texts.add(FloatText(w / 2, h * 0.3f, "⚠ ABYSS WARDEN ⚠", 2.2f, Color.rgb(244, 114, 182), dp(26f)))
        rings.add(Ring(e.x, e.y, dp(24f), 0.5f, 0.5f, Color.rgb(244, 114, 182))) // #614 the Warden's arrival rings out
        audio.playMusic(R.raw.music_boss) // #273 boss variant loop
    }

    private fun explodeProj(pr: Proj, dead: MutableList<Enemy>) { // #130 Meteor Staff AoE
        if (pr.rocket) blooms.add(Bloom(pr.x, pr.y, 0.22f, dp(120f), Color.WHITE)) // #546 rocket impacts flash white-hot
        if (pr.rocket && firePools.size < 42) firePools.add(FirePool(pr.x, pr.y, dp(44f), 2.2f)) // #785 rocket impacts leave the ground burning
        rings.add(Ring(pr.x, pr.y, dp(10f), 0.35f, 0.35f, Color.rgb(249, 115, 22)))
        burst(pr.x, pr.y, Color.rgb(249, 115, 22), 14, 360f)
        chromaT = max(chromaT, 0.15f); blooms.add(Bloom(pr.x, pr.y, 0.3f, dp(70f), Color.rgb(249, 115, 22))) // #230 + #245
        decals.add(Decal(pr.x, pr.y, pr.aoe * 0.8f, 5f))
        audio.sfx("explosion", 1.1f, distVol(pr.x, pr.y)) // #281
        for (e in enemies) if (e.hp > 0 && hyp(e.x - pr.x, e.y - pr.y) < pr.aoe + e.radius * 0.5f) damageEnemy(e, pr.dmg * 0.8f, dead, pr.x, pr.y)
    }

    private fun mergeGems() { // #134 nearby gems fuse into bigger ones
        if (gemDrops.size < 6) return
        val used = mutableSetOf<GemDrop>()
        val born = mutableListOf<GemDrop>()
        for (g in gemDrops) {
            if (g in used || g.mag) continue
            for (g2 in gemDrops) {
                if (g2 === g || g2 in used || g2.mag) continue
                if (hyp(g.x - g2.x, g.y - g2.y) < dp(34f)) {
                    used.add(g); used.add(g2)
                    born.add(GemDrop((g.x + g2.x) / 2, (g.y + g2.y) / 2, g.value + g2.value))
                    break
                }
            }
        }
        if (born.isNotEmpty()) { gemDrops.removeAll(used); gemDrops.addAll(born) // #222 merge flash
            for (b in born) { sparks.add(Spark(b.x, b.y, 0.3f, Color.rgb(52, 211, 153), dp(11f))); if (rings.size < 12) rings.add(Ring(b.x, b.y, dp(6f), 0.3f, 0.3f, Color.rgb(52, 211, 153))) }
            audio.sfx("merge", 0.95f + Random.nextFloat() * 0.15f) // #267 merge sparkle
        }
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
        if (!introChecked) { // #338 onboarding first, #339 whats-new for returning players
            introChecked = true
            if (!save.onboarded && save.totalRuns == 0) { state = State.ONBOARD; save.seenWhatsNew = true }
            else if (!save.seenWhatsNew) state = State.WHATSNEW
        }
        if (state != lastState) { lastState = state; screenT = 0f } // #154 transition clock
        if (state == State.INTRO) { introT += dt; if (introT > 3.2f) state = State.MENU } // #793
        screenT += dt
        if (flashT > 0) flashT -= dt; if (goldFlashT > 0) goldFlashT -= dt
        if (chromaT > 0) chromaT -= dt; if (lastHurtT > 0) lastHurtT -= dt
        if (spawnAnimT > 0) spawnAnimT -= dt
        if (joySpringT > 0) joySpringT -= dt
        if (dashReadyT > 0) dashReadyT -= dt
        if (rocketReadyT > 0) rocketReadyT -= dt // #481
        val bb2 = enemies.firstOrNull { it.type == 5 } // #487 boss damage ghost trails the real bar
        if (bb2 != null) { if (bossGhostHp < bb2.hp) bossGhostHp = bb2.hp; if (bossGhostHp > bb2.hp) bossGhostHp = max(bb2.hp, bossGhostHp - bb2.maxHp * 0.12f * dt) } else bossGhostHp = 0f
        if (playerGhostHp < hp) playerGhostHp = hp; if (playerGhostHp > hp) playerGhostHp = max(hp, playerGhostHp - maxHp * 0.10f * dt) // #518
        if (waveClearT > 0 && particles.size < 350 && Random.nextFloat() < 0.55f) particles.add(Particle(Random.nextFloat() * w, -dp(8f), 0f, 130f, 1.4f, 1.4f, if (Random.nextBoolean()) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), dp(2.6f))) // #493 victory sparkle rain
        if (waveClearT > 0 && Random.nextFloat() < 0.10f) sparks.add(Spark(Random.nextFloat() * w * 0.7f, dp(30f) + Random.nextFloat() * dp(60f), 0.5f, Color.rgb(254, 240, 138), dp(9f))) // #552 shooting stars celebrate a cleared wave
        if (recoilT > 0) recoilT -= dt
        if (slowmoT > 0) slowmoT -= dt
        if (saveSlowT > 0) saveSlowT -= dt
        timeScale = when { slowmoT > 0 && state == State.LEVELUP -> 0.3f; saveSlowT > 0 -> 0.5f; slowmoT > 0 -> 0.25f; else -> 1f } // #150 + #165 + #229 (0.5x near-death save)
        if (toastT > 0) toastT -= dt
        if (confirmT > 0) { confirmT -= dt; if (confirmT <= 0) pendingConfirm = "" }
        if (state == State.PLAYING && resumeT > 0) { // #391 world frozen during the resume countdown
            resumeT -= dt
            updateFx(dt)
            return
        }
        if (state != State.PLAYING) {
            updateFx(dt)
            return
        }
        runTime += dt
        if (muzzleT > 0) muzzleT -= dt
        if (comboT > 0 && dashT <= 0) { comboT -= dt; if (comboT <= 0) { if (combo >= 75 && !comboSaved) { comboSaved = true; combo /= 2; comboT = 1.2f; texts.add(FloatText(px, py - dp(52f), "🔥 COMBO SAVED!", 0.9f, Color.rgb(251, 146, 60), dp(16f))); audio.sfx("sting", 1.2f) } else combo = 0 } } // #132 + #792 once a run, a 75+ combo halves instead of dying
        if (comboPop > 0) comboPop -= dt * 3f
        if (killsPop > 0) killsPop -= dt * 3f
        if (gemsPop > 0) gemsPop -= dt * 3f
        if (scorePop > 0) scorePop -= dt * 3f // #492
        if (bossIntroT > 0) bossIntroT -= dt
        if (bossBoomT > 0) { // #186 multi-stage boss explosion sequence
            val prevBoom = bossBoomT; bossBoomT -= dt
            for (th in listOf(0.62f, 0.34f)) if (prevBoom > th && bossBoomT <= th) {
                burst(bossBoomX + Random.nextFloat() * 70 - 35, bossBoomY + Random.nextFloat() * 70 - 35, Color.rgb(168, 85, 247), 24, 430f)
                rings.add(Ring(bossBoomX, bossBoomY, dp(16f), 0.5f, 0.5f, Color.rgb(244, 114, 182)))
                shake = max(shake, 6f); audio.sfx("explosion", 0.9f)
            }
            if (bossBoomT <= 0) {
                burst(bossBoomX, bossBoomY, Color.WHITE, 46, 540f)
                burst(bossBoomX, bossBoomY, Color.rgb(250, 204, 21), 30, 460f)
                rings.add(Ring(bossBoomX, bossBoomY, dp(20f), 0.8f, 0.8f, Color.rgb(250, 204, 21)))
                shake = 13f; audio.sfx("explosion", 1.2f)
            }
        }
        if (eliteBannerT > 0) eliteBannerT -= dt
        if (waveClearT > 0) waveClearT -= dt
        if (kills != lastKillsSeen) { lastKillsSeen = kills; killsPop = 1f }
        if (gemsRun != lastGemsSeen) { lastGemsSeen = gemsRun; gemsPop = 1f }
        if (waveBannerT > 0) waveBannerT -= dt
        if (shake > 0) shake = max(0f, shake - dt * 30f)
        if (hurtFlash > 0) hurtFlash = max(0f, hurtFlash - dt * 2.2f)
        if (state == State.PLAYING && hp > 0 && hp / maxHp < 0.25f) { // #274 heartbeat layer under the battle music
            heartBeatT -= dt
            if (heartPopT > 0) heartPopT -= dt
            if (heartBeatT <= 0f) { heartBeatT = 0.85f; audio.sfx("heartbeat"); pulseVib(longArrayOf(0, 55, 95, 40)) } // #287 heartbeat pattern
        }
        if (dashCharges < dashMaxCharges) {
            dashRechargeT -= dt
            if (dashRechargeT <= 0) { dashCharges++; dashReadyT = 0.35f; if (dashCharges < dashMaxCharges) dashRechargeT = dashCdMax; rings.add(Ring(px, py, dp(8f), 0.3f, 0.3f, Color.rgb(34, 211, 238))) } // #183 + #458 charge-ready ring
        }
        if (rocketCharges < rocketMaxCharges) { // 🚀 rocket rack recharges like the dash
            rocketRechargeT -= dt
            if (rocketRechargeT <= 0) { rocketCharges++; rocketReadyT = 0.35f; if (rocketCharges < rocketMaxCharges) rocketRechargeT = rocketCdMax } // #481
        }
        if (dashBufferT > 0) { // #385 buffered dash fires the moment a charge lands
            dashBufferT -= dt
            if (dashCharges > 0) { dashBufferT = 0f; doDash() }
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
                meteors.add(Meteor(t.x, t.y, 0.8f)); audio.sfx("meteor", 0.95f) // #252 whistle + boom
            }
        }
        if (vacuumT > 0) vacuumT -= dt
        if (pickupChainT > 0) { pickupChainT -= dt; if (pickupChainT <= 0) pickupChain = 0 } // #297 pitch ladder chain resets
        siphonT -= dt; if (siphonT <= 0) { siphonT = 1f; siphonHp = 0f } // siphon cap window
        gemMergeT -= dt
        if (gemMergeT <= 0) { gemMergeT = 0.8f; mergeGems() } // #134
        orbitSpeed = min(3.4f, orbitSpeed + dt * 1.1f) // #185 blades spin up after each run starts
        orbitAngle += dt * orbitSpeed

        // --- movement ---
        val spd = 335f * speedMul * (if (dashT > 0) 3.1f else 1f) *
            (if (adrenalineLvl > 0 && combo >= 10) 1f + 0.18f * adrenalineLvl else 1f) // #106 Adrenaline
        var ix = joyDx; var iy = joyDy // #366/#370 keyboard + gamepad override the stick when active
        val kvx = (if (keyRight) 1f else 0f) - (if (keyLeft) 1f else 0f) + padDx
        val kvy = (if (keyDown) 1f else 0f) - (if (keyUp) 1f else 0f) + padDy
        if (kvx != 0f || kvy != 0f) { val kd = hyp(kvx, kvy); val km = min(1f, kd); ix = kvx / max(1f, kd) * km; iy = kvy / max(1f, kd) * km }
        val acc = 1f - exp(-dt * (if (dashT > 0) 30f else 14f)) // #387 acceleration curve (snappy in dash)
        pvx += (ix * spd - pvx) * acc; pvy += (iy * spd - pvy) * acc
        px = (px + pvx * dt).coerceIn(dp(20f), w - dp(20f))
        if ((pvx * pvx + pvy * pvy) > 3600f && rings.size < 16 && Random.nextFloat() < 0.10f) rings.add(Ring(px, py + dp(15f), dp(3f), 0.5f, 0.5f, CHARS[save.selectedChar].color)) // #547 flipper ripples trail as you swim
        py = (py + pvy * dt).coerceIn(dp(90f), h - dp(20f))
        if (kbx != 0f || kby != 0f) { // #163 hurt knockback slide (decays fast)
            px = (px + kbx * dt).coerceIn(dp(20f), w - dp(20f))
            py = (py + kby * dt).coerceIn(dp(90f), h - dp(20f))
            kbx *= max(0f, 1f - 7f * dt); kby *= max(0f, 1f - 7f * dt)
            if (hyp(kbx, kby) < 4f) { kbx = 0f; kby = 0f }
        }
        if (hyp(pvx, pvy) > 200f && particles.size < 340)
            particles.add(Particle(px, py, -pvx * 0.06f, -pvy * 0.06f, 0.3f, 0.3f, CHARS[save.selectedChar].color, dp(3.2f)))
        if (Random.nextFloat() < 0.12f && sparks.size < 40) { // #214 gem sparkle ambient around the hero
            val sa = Random.nextFloat() * 6.283f
            sparks.add(Spark(px + cos(sa) * dp(27f), py + sin(sa) * dp(27f), 0.3f, Color.rgb(52, 211, 153), dp(7f)))
        }
        if (hyp(pvx, pvy) > 240f) { // #224 footstep dust at speed
            footT -= dt
            if (footT <= 0f && particles.size < 350) { footT = 0.07f; particles.add(Particle(px + Random.nextFloat() * 12 - 6, py + dp(15f), -pvx * 0.12f + Random.nextFloat() * 24 - 12, -18f, 0.35f, 0.35f, Color.rgb(148, 140, 130), dp(3.4f))) }
        }
        if (dashT > 0) ghosts.add(Ghost(px, py, 0.34f, CHARS[save.selectedChar].color))
        if (dashT > 0 && particles.size < 350) for (i in 0 until 2) // #212 wind streaks
            particles.add(Particle(px + Random.nextFloat() * 16 - 8, py + Random.nextFloat() * 16 - 8, -dashDx * 320f + Random.nextFloat() * 60 - 30, -dashDy * 320f + Random.nextFloat() * 60 - 30, 0.18f, 0.18f, Color.rgb(207, 250, 254), dp(2.2f)))
        if (dashT > 0 && save.selectedChar == 3) { // #127 Pyro fire trail
            firePoolT -= dt
            if (firePoolT <= 0f && firePools.size < 42) { firePoolT = 0.05f; firePools.add(FirePool(px, py, dp(26f), 1.6f)) }
            if (particles.size < 350) for (i in 0 until 2) particles.add(Particle(px + Random.nextFloat() * 20 - 10, py + Random.nextFloat() * 20 - 10, Random.nextFloat() * 60 - 30, -(60f + Random.nextFloat() * 90f), 0.5f, 0.5f, if (i == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21), dp(2.8f))) // #208 ember sparks
        }
        // #137 focus charge + #138 dodge meter from movement
        if (hyp(joyDx, joyDy) < 0.1f) { stillT += dt; dodgeMeter = max(0f, dodgeMeter - dt * 0.5f) }
        else { stillT = 0f; focusReady = false; dodgeMeter = min(1f, dodgeMeter + dt * 0.5f) }
        if (stillT > 1.2f && !focusReady) { focusReady = true; audio.sfx("focus") } // #268 full-charge ping
        if ((pvx != 0f || pvy != 0f) && particles.size < 340 && Random.nextFloat() < 0.5f) { // #428 engine trail sparks
            val ba = atan2(pvy, pvx)
            particles.add(Particle(px - cos(ba) * dp(20f) + Random.nextFloat() * 8 - 4, py - sin(ba) * dp(20f) + Random.nextFloat() * 8 - 4, -cos(ba) * 40f, -sin(ba) * 40f, 0.34f, 0.34f, CHARS[save.selectedChar].color, dp(2.6f)))
        }
        if (stillT > 0.15f && particles.size < 330 && Random.nextFloat() < 0.55f) { // #195 gathering particles spiral in
            val ga = Random.nextFloat() * 6.283f
            particles.add(Particle(px + cos(ga) * dp(44f), py + sin(ga) * dp(44f), -cos(ga) * 215f, -sin(ga) * 215f, 0.22f, 0.22f, if (focusReady) Color.rgb(254, 240, 138) else Color.rgb(165, 243, 252), dp(2.6f)))
        }

        // --- spawning / wave flow ---
        if (countT > 0) { // #294 countdown beeps 3-2-1
            countT -= dt
            val st = ((countT / 0.5f) + 0.999f).toInt()
            if (st in 1..3 && st != countStage) { countStage = st; audio.sfx("count") }
            if (countT <= 0f) { audio.sfx("count", 1.5f); texts.add(FloatText(w / 2, h * 0.40f, "GO!", 0.7f, Color.rgb(250, 204, 21), dp(26f))); rings.add(Ring(w / 2, h * 0.40f - dp(10f), dp(12f), 0.45f, 0.45f, Color.rgb(250, 204, 21))) } // #295 final beep, #564 GO! flash, #663 GO! burst ring
        }
        if (toSpawn > 0) {
            if (countT <= 0f) { // spawns held while the countdown runs
                spawnT -= dt
                if (spawnT <= 0) { spawnEnemy(); toSpawn--; spawnT = spawnInterval * (0.7f + Random.nextFloat() * 0.6f) }
            }
        } else if (enemies.isEmpty()) {
            waveClearBonus = 50 * wave; waveClearedNum = wave; waveClearT = 1.6f
            audio.sfx("waveclear") // #264 wave clear jingle
            for (i in 0 until 16) { if (particles.size > 330) break // #197 sparkle rain
                particles.add(Particle(Random.nextFloat() * w, -dp(10f) - Random.nextFloat() * dp(50f), Random.nextFloat() * 40 - 20, 130f + Random.nextFloat() * 110f, 1.15f, 1.15f, if (i % 2 == 0) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21), dp(3.6f), 1, Random.nextFloat() * 6.28f)) }
            score += waveClearBonus
            if (!waveHurt && wave >= 2) { gemsRun += 25; texts.add(FloatText(w / 2, h * 0.36f, "✨ FLAWLESS WAVE! +25💎", 1.4f, Color.rgb(165, 243, 252), dp(17f))); audio.sfx("levelup") } // #786 clear a wave untouched, get paid
            if (wave >= 2 && runTime - waveStartRun < 25f) { gemsRun += 10; texts.add(FloatText(w / 2, h * 0.42f, "⚡ SPEED BONUS! +10💎", 1.2f, Color.rgb(250, 204, 21), dp(15f))) } // #789 clear it fast, get paid
            hearts.add(HeartDrop(px + dp(46f), py - dp(20f))) // #133 wave-clear heart
            if (wave >= MAX_WAVE && !endlessMode) { endRun(true); return }
            nextWave()
        }

        updateCombat(dt)
        updateFx(dt)

        if (hp <= 0f) {
            if (secondWind > 0) { // #108 Second Wind revive
                deaths++; secondWind--; hp = maxHp * 0.5f; invulnT = 1.6f // #331 death counted
                rings.add(Ring(px, py, dp(20f), 0.6f, 0.6f, Color.rgb(250, 204, 21)))
                burst(px, py, Color.rgb(250, 204, 21), 36, 460f)
                // #194 phoenix rise: staggered rings + rising embers
                rings.add(Ring(px, py, dp(10f), 0.9f, 0.9f, Color.rgb(249, 115, 22)))
                rings.add(Ring(px, py, dp(30f), 1.1f, 1.1f, Color.rgb(239, 68, 68)))
                saveSlowT = 0.45f // #229 near-death save slow-mo
                rings.add(Ring(px, py, dp(46f), 0.6f, 0.6f, Color.WHITE)) // #233 Second Wind shockwave
                for (i in 0 until 16) { val a = Random.nextFloat() * 6.283f
                    particles.add(Particle(px + cos(a) * dp(16f), py + sin(a) * dp(10f), cos(a) * 40f, -(170f + Random.nextFloat() * 200f), 0.8f, 0.8f, if (i % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21), dp(3.4f), 1, Random.nextFloat() * 6.28f)) }
                texts.add(FloatText(px, py - dp(56f), "💫 SECOND WIND!", 1.2f, Color.rgb(250, 204, 21), dp(19f)))
                for (e2 in enemies) {
                    val ddx = e2.x - px; val ddy = e2.y - py; val dd = max(1f, hyp(ddx, ddy))
                    if (dd < dp(170f)) { e2.vx = ddx / dd * 620f; e2.vy = ddy / dd * 620f }
                }
                audio.sfx("revive"); vibrate(80) // #257 phoenix chime
            } else if (save.reviveTokens > 0) { // #145 revive token
                deaths++ // #331 death counted
                save.reviveTokens--; hp = maxHp * 0.6f; invulnT = 2f
                rings.add(Ring(px, py, dp(24f), 0.7f, 0.7f, Color.rgb(52, 211, 153)))
                burst(px, py, Color.rgb(52, 211, 153), 40, 480f)
                texts.add(FloatText(px, py - dp(56f), "🪙 REVIVE TOKEN!", 1.3f, Color.rgb(52, 211, 153), dp(19f)))
                audio.sfx("revive"); vibrate(90) // #257 phoenix chime
            } else { deaths++; hp = 0f; endRun(false) } // #331 final death counted
        }
    }

    private fun updateCombat(dt: Float) {
        val wp = WEAPONS[save.selectedWeapon]

        // --- auto-fire at nearest enemy ---
        fireT -= dt
        val target = enemies.minByOrNull { hyp(it.x - px, it.y - py) }
        val aimActive = save.manualAim && aimPointer != -1 && hyp(aimDx, aimDy) > 0.12f // #360/#400 aim stick live past its dead-zone
        if ((target != null || aimActive) && fireT <= 0f) {
            fireT = 1f / (wp.rate * rateMul)
            var baseAng = if (target != null) atan2(target.y - py, target.x - px) else atan2(aimDy, aimDx)
            if (aimActive) { // #361 fire toward the aim stick; #357 soft snap to a near enemy
                baseAng = atan2(aimDy, aimDx)
                val snapRad = when (save.aimAssist) { 2 -> 0.21f; 1 -> 0.10f; else -> 0f }
                if (snapRad > 0f) {
                    var bestD = snapRad; var bestA = baseAng
                    for (e in enemies) {
                        val ea = atan2(e.y - py, e.x - px); var dd = abs(ea - baseAng); if (dd > 3.14159f) dd = 6.28318f - dd
                        if (dd < bestD) { bestD = dd; bestA = ea }
                    }
                    baseAng = bestA
                }
            }
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
                val pr2 = Proj(px + cos(a) * dp(20f), py + sin(a) * dp(20f),
                    cos(a) * wp.projSpeed, sin(a) * wp.projSpeed,
                    wp.dmg * dmgMul * (if (focusReady) 3f else 1f), wp.pierce, true,
                    if (focusReady) dp(9f) else dp(6f), if (focusReady) Color.rgb(254, 240, 138) else pcol)
                if (save.selectedWeapon == 5) pr2.aoe = dp(55f) // #130 Meteor Staff
                projs.add(pr2)
            }
            if (focusReady) { focusReady = false; stillT = 0f } // #137 focus shot spent
            recoilT = 0.09f // #184 muzzle recoil
            if (particles.size < 350) particles.add(Particle(px + cos(lastAim) * dp(26f), py + sin(lastAim) * dp(26f), cos(lastAim) * 55f, sin(lastAim) * 55f - 26f, 0.4f, 0.4f, Color.rgb(100, 116, 139), dp(4.2f))) // #219 smoke puff
            if (time - lastShootSfx > 0.07f) { audio.sfx("shoot", 0.95f + Random.nextFloat() * 0.1f); lastShootSfx = time } // #279 pitch varies +-5%
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
            if (particles.size < 360) { val mfr = (m.t / 0.8f).coerceIn(0f, 1f); particles.add(Particle(m.x + (1f - mfr) * dp(140f), m.y - mfr * h * 0.38f, 0f, 0f, 0.3f, 0.3f, Color.rgb(249, 115, 22), dp(3.4f))) } // #234 comet trail
            if (m.t <= 0f) {
                deadMeteors.add(m)
                rings.add(Ring(m.x, m.y, dp(10f), 0.4f, 0.4f, Color.rgb(249, 115, 22)))
                burst(m.x, m.y, Color.rgb(249, 115, 22), 22, 420f)
                rings.add(Ring(m.x, m.y, dp(30f), 0.55f, 0.55f, Color.rgb(168, 162, 158))) // #211 impact dust ring
                rings.add(Ring(m.x, m.y, dp(16f), 0.3f, 0.3f, Color.rgb(254, 240, 138))) // #231 fast shockwave
                chromaT = max(chromaT, 0.2f) // #230 chromatic split
                blooms.add(Bloom(m.x, m.y, 0.35f, dp(95f), Color.rgb(249, 115, 22))) // #245 light bloom
                for (i in 0 until 8) { val a = i * 0.785f; if (particles.size < 360) particles.add(Particle(m.x + cos(a) * dp(20f), m.y + sin(a) * dp(20f), cos(a) * 120f, sin(a) * 120f, 0.6f, 0.6f, Color.rgb(168, 162, 158), dp(4.5f))) }
                decals.add(Decal(m.x, m.y, dp(52f), 6f))
                decals.add(Decal(m.x, m.y, dp(26f), 11f)) // #451 burnt core lingers
                audio.sfx("explosion", 0.9f, distVol(m.x, m.y)) // #281
                val mdmg = 42f * dmgMul * (1f + 0.3f * (meteorLvl - 1))
                for (e2 in enemies) if (e2.hp > 0 && hyp(e2.x - m.x, e2.y - m.y) < dp(72f) + e2.radius * 0.5f) damageEnemy(e2, mdmg, dead)
            }
        }
        meteors.removeAll(deadMeteors)
        for (e in enemies) {
            if (e.flash > 0) e.flash -= dt
            if (e.ghostHp > e.hp) e.ghostHp = max(e.hp, e.ghostHp - e.maxHp * 0.3f * dt) // #432 ghost drains toward real HP
            if (e.spawnT > 0) e.spawnT -= dt
            if (e.slowT > 0) e.frostAge += dt else e.frostAge = 0f
            if (e.type == 7 && e.state == 2 && ghosts.size < 24 && Random.nextFloat() < 0.7f) ghosts.add(Ghost(e.x, e.y, 0.25f, enemyColor(7))) // #235 lancer charge afterimage
            if (e.type == 7 && e.state == 2 && particles.size < 360 && Random.nextFloat() < 0.5f) particles.add(Particle(e.x, e.y + e.radius * 0.6f, Random.nextFloat() * 30 - 15, 26f, 0.4f, 0.4f, Color.rgb(168, 162, 158), dp(3.6f))) // #461 charge dust
            if (e.type == 2 && particles.size < 360 && Random.nextFloat() < 0.30f) particles.add(Particle(e.x + (Random.nextFloat() - 0.5f) * e.radius, e.y + e.radius * 0.7f, (Random.nextFloat() - 0.5f) * 24f, 18f, 0.45f, 0.45f, Color.rgb(168, 162, 158), dp(4.2f))) // #540 brute stomp dust
            if (e.slowT > 0 && particles.size < 340 && Random.nextFloat() < 0.1f) particles.add(Particle(e.x + Random.nextFloat() * 14 - 7, e.y + e.radius * 0.5f, 0f, 34f, 0.5f, 0.5f, Color.rgb(207, 250, 254), dp(5f))) // #237 frost mist
            if (e.slowT > 0 && particles.size < 340 && Random.nextFloat() < 0.05f) particles.add(Particle(e.x, e.y - e.radius, 0f, -22f, 0.7f, 0.7f, Color.rgb(224, 242, 254), dp(3.4f))) // #676 slowed foes breathe frost upward
            if (e.slowT > 0) e.slowT -= dt
            if (e.burnT > 0) e.burnT -= dt
            if (e.burnT > 0 && particles.size < 340 && Random.nextFloat() < 0.22f) particles.add(Particle(e.x + Random.nextFloat() * 12 - 6, e.y - e.radius * 0.4f, 0f, -46f, 0.4f, 0.4f, Color.rgb(251, 146, 60), dp(2.6f))) // #511 burning foes shed embers
            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail
            if (e.type == 11 && hyp(px - e.x, py - e.y) < dp(130f) && particles.size < 340 && Random.nextFloat() < 0.2f) particles.add(Particle(e.x, e.y - e.radius * 0.6f, (Random.nextFloat() - 0.5f) * 30f, -40f, 0.35f, 0.35f, Color.rgb(147, 197, 253), dp(2f))) // #573 gnome panic sweat when you close in
            if (e.type == 6 && particles.size < 340 && Random.nextFloat() < 0.10f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 10f, 0.55f, 0.55f, Color.rgb(226, 232, 240), dp(2.2f))) // #537 weaver silk dots
            if (e.type == 1 && particles.size < 340 && Random.nextFloat() < 0.08f) particles.add(Particle(e.x, e.y, 0f, 0f, 0.4f, 0.4f, enemyColor(1), dp(2f))) // #577 dart ribbon dots
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
                if (!bossBurst66 && frac <= 0.66f) { bossBurst66 = true; hitstopT = max(hitstopT, 0.09f); radialBurst(e, 16, 300f); audio.sfx("roar", 0.85f); texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "💥 WARDEN SURGE!", 1f, Color.rgb(244, 114, 182), dp(16f))) }
                if (!bossSummoned && frac <= 0.5f) { // #123 summon minions
                    bossSummoned = true
                    for (i in 0 until 4) {
                        val mt = if (i % 2 == 0) 0 else 1
                        val mh = if (mt == 0) 26f else 14f
                        val mhp = mh * (1f + (wave - 1) * 0.24f)
                        pendingSpawns.add(Enemy(mt, e.x + (i - 1.5f) * dp(38f), e.y + dp(30f), mhp, mhp,
                            if (mt == 0) 168f else 265f, if (mt == 0) dp(17f) else dp(13f), 12f, 1, 20))
                    }
                    waveTotal += 4 // v2.3 boss minions count toward the wave total
                    texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "👹 MINIONS, RISE!", 1f, Color.rgb(244, 114, 182), dp(16f)))
                }
                if (!bossBurst33 && frac <= 0.33f) { bossBurst33 = true; hitstopT = max(hitstopT, 0.09f); radialBurst(e, 20, 330f); audio.sfx("roar", 0.8f); texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "💥 DESPERATE SURGE!", 1f, Color.rgb(248, 113, 113), dp(16f))) }
                if (!bossEnraged && bossAliveT > 90f) { bossEnraged = true; texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "🔥 WARDEN ENRAGED!", 1.4f, Color.rgb(248, 113, 113), dp(18f))); audio.sfx("roar", 0.75f) } // #265 enrage roar
            }
            if (e.type == 7) { // #114 Lancer state machine
                e.stateT -= dt
                when (e.state) {
                    0 -> if (dist < dp(330f) && e.stateT <= 0f) { e.state = 1; e.stateT = 0.55f }
                    1 -> if (e.stateT <= 0f) { e.state = 2; e.stateT = 1.5f; e.vx = mvx * 900f; e.vy = mvy * 900f; audio.sfx("whoosh", 0.95f) } // #269 charge whoosh
                    2 -> if (e.stateT <= 0f) { e.state = 0; e.stateT = 1.4f }
                }
            }
            if (save.selectedChar == 4 && dist < dp(130f)) e.slowT = max(e.slowT, 0.12f) // #128 Cryo aura
            val slowMul = if (e.slowT > 0) 1f - min(0.55f, max(0.22f * frostLvl, if (save.selectedChar == 4) 0.30f else 0f)) else 1f
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
            if (dashT > 0 && dist < e.radius + dp(22f) && e.bladeT <= 0) { // #136 dash-through damage
                damageEnemy(e, 18f * dmgMul, dead)
                e.bladeT = 0.4f
            }
            if (dist < e.radius + dp(17f) && invulnT <= 0 && dashT <= 0 && e.damage > 0) {
                hurtPlayer(e.damage)
                if (thornLvl > 0) damageEnemy(e, e.damage * 0.3f * thornLvl, dead) // #104 Thorn Aura
            }
            // v2.3 WAVE-INTEGRITY LEASH: the gnome used to flee clean off the arena and
            // strand the wave forever (Nathan's Endless Wave 12). The gnome is now
            // penned inside the arena — catchable, killable, never a soft-lock — and
            // every other enemy is leashed to 70dp outside so knockbacks and lancer
            // charges can never lose one off-screen either.
            if (e.type == 11) { val ox = e.x; val oy = e.y; e.x = e.x.coerceIn(e.radius, w - e.radius); e.y = e.y.coerceIn(e.radius, h - e.radius); if ((ox != e.x || oy != e.y) && Random.nextFloat() < 0.22f && sparks.size < 40) sparks.add(Spark(e.x, e.y - e.radius - dp(4f), 0.3f, Color.rgb(250, 204, 21), dp(7f))) } // #490 penned gnome bonks the wall
            else { e.x = e.x.coerceIn(-dp(70f), w + dp(70f)); e.y = e.y.coerceIn(-dp(70f), h + dp(70f)) }
        }

        if (pendingSpawns.isNotEmpty()) { enemies.addAll(pendingSpawns); pendingSpawns.clear() }

        // --- projectiles ---
        val deadProj = mutableListOf<Proj>()
        for (pr in projs) {
            pr.life -= dt
            if (pr.rocket) { // 🚀 rockets home in and breathe smoke
                val rt = enemies.filter { it.hp > 0 }.minByOrNull { hyp(it.x - pr.x, it.y - pr.y) }
                if (rt != null) {
                    val cur = atan2(pr.vy, pr.vx); val want = atan2(rt.y - pr.y, rt.x - pr.x)
                    var dd = want - cur; while (dd > 3.14159f) dd -= 6.28318f; while (dd < -3.14159f) dd += 6.28318f
                    val turn = dd.coerceIn(-4.4f * dt, 4.4f * dt); val spd = hyp(pr.vx, pr.vy)
                    pr.vx = cos(cur + turn) * spd; pr.vy = sin(cur + turn) * spd
                }
                if (particles.size < 380) particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.3f, 0.3f, Color.rgb(148, 163, 184), dp(3.6f)))
                if (particles.size < 380 && Random.nextFloat() < 0.5f) particles.add(Particle(pr.x, pr.y, (Random.nextFloat() - 0.5f) * 60f, (Random.nextFloat() - 0.5f) * 60f, 0.22f, 0.22f, Color.rgb(251, 146, 60), dp(2f))) // #498 rocket spark trail
            }
            pr.x += pr.vx * dt; pr.y += pr.vy * dt
            if (pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.65f)
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.16f, 0.16f, pr.color, dp(2.6f)))
            if (!pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.30f) // #453 hostile shots shed faint embers
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.22f, 0.22f, pr.color, dp(2f)))
            if (pr.life <= 0 || pr.x < -80 || pr.x > w + 80 || pr.y < -80 || pr.y > h + 80) {
                if (pr.fromPlayer && pr.aoe > 0f) explodeProj(pr, dead) // #130 lobbed orb bursts at end of arc
                deadProj.add(pr); continue
            }
            if (pr.fromPlayer) {
                for (e in enemies) {
                    if (e.hp > 0 && hyp(pr.x - e.x, pr.y - e.y) < e.radius + pr.radius) {
                        damageEnemy(e, pr.dmg, dead, pr.x, pr.y)
                        if (particles.size < 360) for (i in 0 until 2) { val a = Random.nextFloat() * 6.283f; val s = 90f + Random.nextFloat() * 130f // #220 impact sparks in the projectile's color
                            particles.add(Particle(pr.x, pr.y, cos(a) * s, sin(a) * s, 0.22f, 0.22f, pr.color, dp(2.4f))) }
                        if (save.selectedWeapon == 6 && siphonHp < 3f) { // #131 siphon, capped 3 HP/s (Gemini tune)
                            val heal = min(0.15f, 3f - siphonHp); hp = min(maxHp, hp + heal); siphonHp += heal
                        }
                        if (pr.aoe > 0f) { explodeProj(pr, dead); deadProj.add(pr); break }
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
        // Sweep: pool DoT (updateFx) drains hp outside damageEnemy, so a
        // pool-killed enemy can sit at hp <= 0 unregistered. Left alone it
        // becomes an unshootable zombie — bullets skip hp <= 0, yet it
        // still draws, still moves, and the wave can never clear.
        for (e in enemies) if (e.hp <= 0 && !dead.contains(e)) dead.add(e)
        for (e in dead) {
            if (!enemies.remove(e)) continue
            kills++
            if (kills == 50 || kills == 100 || kills == 200 || kills == 350 || kills == 500) { // #147 streak milestones
                texts.add(FloatText(px, py - dp(64f), "🔥 $kills KILL STREAK! +25💎", 1.1f, Color.rgb(251, 146, 60), dp(16f)))
                if (rings.size < 16) rings.add(Ring(px, py, dp(14f), 0.3f, 0.3f, Color.rgb(251, 146, 60))) // #579 streaks pop an orange ring
                rings.add(Ring(px, py, dp(14f), 0.5f, 0.5f, Color.rgb(251, 146, 60))) // #457 streak ring burst
                for (i in 0 until 5) gemDrops.add(GemDrop(px + Random.nextFloat() * 60 - 30, py + Random.nextFloat() * 60 - 30, 5))
                audio.sfx("levelup")
            }
            combo++; comboT = 2.5f
            if (combo % 25 == 0) texts.add(FloatText(px, py - dp(76f), "🔥 RAMPAGE ×$combo", 1.0f, Color.rgb(248, 113, 113), dp(17f))) // #499 combo milestone callout
            if (combo > comboBest) comboBest = combo
            val mult = 1 + min(combo / 10, 4)
            val scBefore = score // #589 thousand milestones get a shout
            score += e.score * mult
            if (score / 1000 > scBefore / 1000) texts.add(FloatText(px, py - dp(92f), "⭐ ${(score / 1000) * 1000}!", 1.0f, Color.rgb(250, 204, 21), dp(16f)))
            if (e.score >= 60) scorePop = 0.35f // #492 big kills pop the score
            if (save.dmgNumbers) texts.add(FloatText(e.x, e.y, "+${e.score * mult}", 0.6f,
                when { mult >= 4 -> Color.rgb(250, 204, 21); mult >= 2 -> Color.rgb(251, 146, 60); else -> Color.rgb(165, 243, 252) }, dp(12.5f)))
            if (combo % 10 == 0) { audio.sfx("sting", 0.85f + min(5, combo / 10) * 0.12f); comboPop = 1f; vibrate(30); shake = max(shake, 3.5f); rings.add(Ring(px, py, dp(14f), 0.4f, 0.4f, Color.rgb(251, 146, 60))); texts.add(FloatText(px, py - dp(52f), "🔥 $combo COMBO ×$mult!", 0.9f, Color.rgb(251, 146, 60), dp(17f))) } // #171
            if (lifesteal > 0 && hp > 0) {
                hp = min(maxHp, hp + lifesteal)
                if (Random.nextFloat() < 0.3f) texts.add(FloatText(px, py - dp(34f), "+${lifesteal}", 0.55f, Color.rgb(248, 113, 113), dp(12f)))
            }
            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)
            if (rings.size < 20) rings.add(Ring(e.x, e.y, dp(5f), 0.18f, 0.18f, Color.WHITE)) // #514 white core flash on every kill
            if (rings.size < 14) rings.add(Ring(e.x, e.y, e.radius * 0.7f, 0.32f, 0.32f, enemyColor(e.type))) // #444 kill shockwave
            if (e.elite) for (i in 0 until 6) { if (particles.size > 360) break // #454 elite gold fountain
                val a = -1.57f + (Random.nextFloat() - 0.5f) * 1.6f; val s = 200f + Random.nextFloat() * 220f
                particles.add(Particle(e.x, e.y, cos(a) * s, sin(a) * s, 0.7f, 0.7f, Color.rgb(250, 204, 21), dp(3.4f), 1, Random.nextFloat() * 6.28f)) }
            if (e.elite) for (i in 0 until 3) gemDrops.add(GemDrop(e.x + Random.nextFloat() * 50 - 25, e.y + Random.nextFloat() * 50 - 25, 5)) // #788 every elite pays a 3-gem bounty
            if (e.frostAge > 1.2f) burst(e.x, e.y, Color.rgb(165, 243, 252), 10, 300f) // #471 frozen kills shatter icy
            if (e.type == 2 || e.type == 10) shake = max(shake, 4.5f) // #201 brute death shake
            if (e.slowT > 0 && particles.size < 360) for (i in 0 until 6) { // #207 frost shards on chilled death
                val a = Random.nextFloat() * 6.283f; val s = 130f + Random.nextFloat() * 200f
                particles.add(Particle(e.x, e.y, cos(a) * s, sin(a) * s, 0.45f, 0.45f, Color.rgb(191, 219, 254), dp(4f), 1, Random.nextFloat() * 6.28f))
            }
            // #90 shard dissolve
            for (i in 0 until 6) {
                if (particles.size > 340) break
                val a = Random.nextFloat() * 6.283f; val s = 120f + Random.nextFloat() * 260f
                particles.add(Particle(e.x, e.y, cos(a) * s, sin(a) * s, 0.5f, 0.5f, enemyColor(e.type), dp(4.5f), 1, Random.nextFloat() * 6.28f))
            }
            if (shardGhosts.size < 20) shardGhosts.add(ShardGhost(e.x, e.y, e.radius, when (e.type) { 0, 7 -> 3; 2, 8, 10 -> 6; 5 -> 8; 6 -> 5; else -> 4 }, enemyColor(e.type), 0.45f)) // #161
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
            if (e.type == 3) { pools.add(PoisonPool(e.x, e.y, dp(40f), 3.5f)); if (Random.nextFloat() < 0.5f) audio.sfx("poison", 1.1f) } // #253 bubble burst
            audio.sfx(if (e.type == 2 || e.type == 5) "explosion" else "hit", 0.8f + Random.nextFloat() * 0.4f)
            if (e.type == 5) { // #150 cinematic + #146 speed challenge + #148 first-boss discount
                shake = 22f; vibrate(90); slowmoT = 0.25f // #202 large boss-death shake
                bossBoomT = 0.95f; bossBoomX = e.x; bossBoomY = e.y // #186 staged explosions
                for (i in 0 until 8) { // #217 boss death gem fountain
                    val g = GemDrop(e.x + Random.nextFloat() * 40 - 20, e.y, 5)
                    g.vx = Random.nextFloat() * 320 - 160; g.vy = -(120f + Random.nextFloat() * 220f)
                    gemDrops.add(g)
                }
                flashT = 0.18f; chromaT = 0.25f; blooms.add(Bloom(e.x, e.y, 0.45f, dp(120f), Color.rgb(168, 85, 247))) // #226 white flash + #230 chroma + #245 bloom
                audio.playMusic(if (endlessMode) R.raw.music_endless else R.raw.music_battle) // #273/#278 restore the run loop
                vibrate(420) // #286 long buzz on boss death
                save.bossKills++
                texts.add(FloatText(w / 2, h * 0.34f, "👑 WARDEN SLAIN! 👑", 1.6f, Color.rgb(250, 204, 21), dp(24f)))
                rings.add(Ring(e.x, e.y, dp(20f), 0.9f, 0.9f, Color.rgb(250, 204, 21)))
                if (runTime - bossStartT <= 60f) {
                    gemsRun += 150
                    texts.add(FloatText(px, py - dp(70f), "⚔️ CHALLENGE CLEAR! +150💎", 1.3f, Color.rgb(52, 211, 153), dp(17f)))
                }
                if (save.bossKills == 1) { save.heroDiscount = true; toast("🏷️ Boss bonus: 25% OFF your next hero!") }
            }
            if (e.elite) { eliteBannerT = 1.4f; save.elitesSlain++; rerollTokens++; texts.add(FloatText(e.x, e.y - dp(24f), "+1 🔄 REROLL", 0.8f, Color.rgb(165, 243, 252), dp(12f))) } // #139
            if (e.elite) texts.add(FloatText(e.x, e.y - dp(44f), "👑 ELITE DOWN", 0.9f, Color.rgb(250, 204, 21), dp(13f))) // #550 elites get a crown callout
            if (e.elite) { // #198 spiral implosion: ring shards converge with a swirl
                for (i in 0 until 10) { val a = i * 0.628f; val rr = dp(54f)
                    particles.add(Particle(e.x + cos(a) * rr, e.y + sin(a) * rr, -cos(a) * 250f - sin(a) * 130f, -sin(a) * 250f + cos(a) * 130f, 0.38f, 0.38f, Color.rgb(250, 204, 21), dp(3.8f), 1, a)) }
            }
            if (e.elite) { // #203 hitstop 40ms + #206 soul wisps rise
                hitstopT = max(hitstopT, 0.04f); goldFlashT = 0.12f // #228 gold flash
                pulseVib(longArrayOf(0, 35, 70, 35)) // #285 double pulse on elite kill
                for (i in 0 until 3) if (particles.size < 360) particles.add(Particle(e.x + Random.nextFloat() * 30 - 15, e.y, Random.nextFloat() * 30 - 15, -(46f + Random.nextFloat() * 40f), 1.1f, 1.1f, Color.rgb(216, 180, 254), dp(5.5f)))
            }
            if (e.type == 5 || e.type == 2 || e.elite)
                rings.add(Ring(e.x, e.y, e.radius, 0.45f, 0.45f, if (e.elite) Color.rgb(250, 204, 21) else enemyColor(e.type)))
            if (e.type == 3) for (i in 0 until 2) { // splitter births minis
                val mini = Enemy(4, e.x + Random.nextFloat() * 30 - 15, e.y + Random.nextFloat() * 30 - 15,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 235f, dp(10f), 6f, 1, 8)
                enemies.add(mini); waveTotal++ // v2.3 spawned mid-wave foes count toward the wave total
                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(6f), 0.25f, 0.25f, Color.rgb(74, 222, 128))) // #467 birth pop ring
                sparks.add(Spark(mini.x, mini.y, 0.25f, Color.rgb(74, 222, 128), dp(6f))) // #639 splitter births spark
            }
            if (e.type == 9) for (i in 0 until 3) { // #116 Necroshard births 3 darts
                audio.sfx("necropop", 1f - i * 0.12f) // #271 triple pop
                val mini = Enemy(4, e.x + Random.nextFloat() * 36 - 18, e.y + Random.nextFloat() * 36 - 18,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 250f, dp(10f), 6f, 1, 8)
                enemies.add(mini); waveTotal++ // v2.3 spawned mid-wave foes count toward the wave total
                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(7f), 0.3f, 0.3f, Color.rgb(232, 121, 249))) // #513 necro births ring fuchsia
                sparks.add(Spark(mini.x, mini.y - dp(4f), 0.3f, Color.rgb(232, 121, 249), dp(7f))) // #640 necro births spark fuchsia
            }
            if (poisonLvl > 0 && Random.nextFloat() < 0.10f + 0.08f * poisonLvl) { // #103 Poison Vials
                pools.add(PoisonPool(e.x, e.y, dp(38f), 3.5f)); if (Random.nextFloat() < 0.6f) audio.sfx("poison", 0.9f + Random.nextFloat() * 0.25f) // #253
            }
            if (e.type == 11) { // #118 Gnome looted: gem shower
                texts.add(FloatText(e.x, e.y - dp(20f), "💰 GNOME LOOTED! +60💎", 1.2f, Color.rgb(250, 204, 21), dp(17f)))
                for (k in 0 until 3) sparks.add(Spark(e.x + (k - 1) * dp(9f), e.y - dp(6f), 0.35f, Color.rgb(250, 204, 21), dp(8f))) // #638 the loot bursts coin-sparks
                audio.sfx("gnome") // #272 gnome laugh
                for (i in 0 until 12) {
                    val g = GemDrop(e.x + Random.nextFloat() * 70 - 35, e.y + Random.nextFloat() * 70 - 35, 5)
                    g.vx = Random.nextFloat() * 300 - 150; g.vy = Random.nextFloat() * 300 - 150
                    gemDrops.add(g)
                }
            }
            // ❤️ heart drops (v2.3.1: rare treats, max 2 on the field, boss gives exactly one)
            val heartChance = when { e.type == 5 -> 1f; e.elite -> 0.18f; e.type == 2 -> 0.10f; else -> 0.012f }
            if (Random.nextFloat() < heartChance && hearts.size < 2) {
                val hd = HeartDrop(e.x, e.y)
                hd.vx = Random.nextFloat() * 140 - 70; hd.vy = Random.nextFloat() * 140 - 70
                hearts.add(hd)
            }
            // gem drops
            var remaining = (e.xp * (if (mutator == 1) 2f else 1f) * (if (save.hardMode) 1.5f else 1f)).toInt() // #119 + #149 hard loot
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
            if (d < magnetR || vacuumT > 0) g.mag = true
            if (g.mag && sparks.size < 40 && Random.nextFloat() < 0.16f) sparks.add(Spark(g.x, g.y, 0.22f, Color.rgb(167, 243, 208), dp(6f))) // #485 magnet shimmer
            if (g.mag && d > 1f) {
                val pull = if (vacuumT > 0) 1700f else 780f // #135 vacuum pull
                g.x += dx / d * pull * dt; g.y += dy / d * pull * dt
            } else {
                g.x += g.vx * dt; g.y += g.vy * dt; g.vx *= (1f - 4f * dt); g.vy *= (1f - 4f * dt)
            }
            if (d < dp(26f)) {
                if (rings.size < 12) rings.add(Ring(px, py, dp(6f), 0.25f, 0.25f, Color.rgb(52, 211, 153)))
                if (flyGems.size < 24) flyGems.add(FlyGem(g.x, g.y, (g.x + w - dp(96f)) / 2, min(g.y, dp(50f)) - dp(110f), w - dp(96f), dp(50f), Color.rgb(52, 211, 153))) // #167
                if (sparks.size < 40) sparks.add(Spark(g.x, g.y, 0.28f, Color.rgb(167, 243, 208), dp(9f))) // #450 pickup sparkle
                gotGems.add(g)
                gemsRun += g.value
                gemPickups++; if (gemPickups % 50 == 0) { gemDrops.add(GemDrop(px + dp(30f), py - dp(24f), 25)); texts.add(FloatText(px, py - dp(58f), "💎 GEM RUSH! +25", 1.0f, Color.rgb(52, 211, 153), dp(15f))) } // #790 every 50th gem drops a bonus
                gainXp(g.value)
                pickupChain++; pickupChainT = 0.9f // #297 each gem in a sweep raises the next pitch
                if (time - lastPickupSfx > 0.05f) { audio.sfx("pickup", 1f + min(0.6f, pickupChain * 0.04f)); lastPickupSfx = time }
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
                texts.add(FloatText(px, py - dp(40f), "+15 HP", 0.8f, Color.rgb(248, 113, 113), dp(15f))) // v2.4: no text hearts — the animated heart below says it
                heartPopT = 0.9f
                audio.sfx("heart") // #266 warm chime
                burst(px, py, Color.rgb(239, 68, 68), 10, 260f)
                rings.add(Ring(px, py, dp(8f), 0.5f, 0.5f, Color.rgb(239, 68, 68))) // #168 expanding ring
                for (i in 0 until 4) { val sa = i * 1.571f + 0.4f; sparks.add(Spark(px + cos(sa) * dp(22f), py + sin(sa) * dp(22f), 0.3f, Color.rgb(252, 165, 165), dp(8f))) } // #215 heal sparkles
            }
        }
        hearts.removeAll(gotHearts)
    }

    private fun abs2(x: Float, y: Float) = x * x + y * y
    private fun hyp(x: Float, y: Float): Float = sqrt(x * x + y * y)

    private fun damageEnemy(e: Enemy, raw: Float, dead: MutableList<Enemy>,
                            srcX: Float = px, srcY: Float = py, noChain: Boolean = false) {
        if (e.hp <= 0) return
        if (frostLvl > 0) { e.slowT = 1.5f; e.frostAge = 0f } // #188 crystals regrow
        val crit = Random.nextFloat() < critCh
        var dmg = if (crit) raw * critMul else raw // #110 crit damage multiplier
        if (e.frostAge > 1.2f) { dmg *= 1.5f; if (Random.nextFloat() < 0.3f) texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "❄️ SHATTER!", 0.5f, Color.rgb(165, 243, 252), dp(11f))) } // #787 deep-frozen foes take +50% damage
        if (e.type == 10) { // #117 Bulwark Brute: frontal armor blocks 70%
            val toSrc = atan2(srcY - e.y, srcX - e.x); val toPlayer = atan2(py - e.y, px - e.x)
            var da = (toSrc - toPlayer) % 6.283f; if (da < 0) da = -da; if (da > 3.1416f) da = 6.283f - da
            if (da < 1.05f) {
                dmg *= 0.3f
                if (Random.nextFloat() < 0.4f) texts.add(FloatText(e.x, e.y - e.radius - dp(8f), "🛡️ BLOCKED", 0.5f, Color.rgb(148, 163, 184), dp(11f)))
                sparks.add(Spark(e.x, e.y, 0.22f, Color.rgb(203, 213, 225), dp(8f))) // #560 armor blocks clang sparks
            }
        }
        e.hp -= dmg
        save.totalDamage += dmg.toLong() // #321 lifetime damage dealt
        e.flash = 0.06f // #162 hit flash 60ms
        if (particles.size < 370 && Random.nextFloat() < 0.35f) particles.add(Particle(e.x, e.y, (Random.nextFloat() - 0.5f) * 200f, (Random.nextFloat() - 0.5f) * 200f, 0.22f, 0.22f, Color.WHITE, dp(2f))) // #534 ordinary hits spark too
        if (e.hp > 0 && time - lastHitSfx > 0.05f) { audio.sfx("hit", 0.65f + 0.45f * (e.hp / e.maxHp).coerceIn(0f, 1f)); lastHitSfx = time } // #280 hit pitch drops as HP drops
        if (crit) { sparks.add(Spark(e.x, e.y, 0.3f, Color.rgb(250, 204, 21), dp(13f))); sparks.add(Spark(e.x, e.y, 0.38f, Color.WHITE, dp(19f))); if (rings.size < 14) rings.add(Ring(e.x, e.y, dp(8f), 0.3f, 0.3f, Color.rgb(250, 204, 21))); if (save.dmgNumbers) texts.add(FloatText(e.x, e.y - e.radius, "💥 CRIT ${dmg.toInt()}", 0.65f, Color.rgb(250, 204, 21), dp(19f)).also { it.spin = true }) } // #173 spin-in
        if (crit && particles.size < 370) for (k in 0 until 3) particles.add(Particle(e.x, e.y, (Random.nextFloat() - 0.5f) * 260f, (Random.nextFloat() - 0.5f) * 260f, 0.3f, 0.3f, Color.rgb(250, 204, 21), dp(2.6f))) // #533 crits shed gold sparks
        else if (save.dmgNumbers) texts.add(FloatText(e.x + Random.nextFloat() * 16 - 8, e.y - e.radius, "${dmg.toInt()}", 0.45f, if (dmg >= 40f) Color.rgb(253, 224, 71) else Color.rgb(226, 232, 240), if (dmg >= 40f) dp(14f) else dp(11.5f))) // #559 heavy hits read gold and bigger
        val chainLv = teslaLvl + if (save.selectedWeapon == 4) 1 else 0 // #101 coil + #129 Tesla Arc
        if (chainLv > 0 && !noChain && raw > 0.5f) { // chain lightning
            val targets = enemies.filter { it !== e && it.hp > 0 && hyp(it.x - e.x, it.y - e.y) < dp(160f) }
                .sortedBy { hyp(it.x - e.x, it.y - e.y) }.take(chainLv + 1)
            for (t in targets) {
                bolts.add(Bolt(e.x, e.y, t.x, t.y, 0.12f))
                if (Random.nextFloat() < 0.3f) bolts.add(Bolt(t.x, t.y, t.x + Random.nextFloat() * 70 - 35, t.y + Random.nextFloat() * 70 - 35, 0.08f)) // #209 ambient arc
                t.hp -= raw * 0.4f; t.flash = 0.06f // #162
                sparks.add(Spark(t.x, t.y, 0.25f, Color.rgb(253, 224, 71), dp(9f))) // #561 chain hits crackle
                if (t.hp <= 0 && !dead.contains(t)) dead.add(t)
            }
            audio.sfx("zap", 0.95f + Random.nextFloat() * 0.15f) // #251 tesla zap
            if (targets.size >= 2) audio.sfx("sizzle", 0.9f + Random.nextFloat() * 0.2f) // #254 chain sizzle
        }
        if (e.hp <= 0 && !dead.contains(e)) dead.add(e)
    }

    private fun hurtPlayer(raw: Float) {
        if (save.selectedChar == 1 && dodgeMeter > 0.2f && Random.nextFloat() < 0.05f + 0.25f * dodgeMeter) { // #138 Swift dodge
            texts.add(FloatText(px, py - dp(34f), "💨 DODGED!", 0.6f, Color.rgb(250, 204, 21), dp(14f)))
            if (rings.size < 12) rings.add(Ring(px, py, dp(18f), 0.28f, 0.28f, Color.rgb(250, 204, 21))) // #580 a dodge pops a gold ring
            invulnT = max(invulnT, 0.25f); audio.sfx("dash", 1.2f); return
        }
        var dmg = max(1f, raw - armor)
        lastHurtAt = runTime
        if (shieldHp > 0f) { // #112 Shield Generator absorbs first
            val absorbed = min(shieldHp, dmg)
            shieldHp -= absorbed; dmg -= absorbed
            sparks.add(Spark(px, py, 0.25f, Color.rgb(147, 197, 253), dp(16f)))
            if (rings.size < 12) rings.add(Ring(px, py, dp(30f), 0.3f, 0.3f, Color.rgb(147, 197, 253))) // #216 shield hit ripple
            if (shieldHp <= 0f) { // #192 shield break: ring shatter
                rings.add(Ring(px, py, dp(30f), 0.45f, 0.45f, Color.rgb(147, 197, 253)))
                for (i in 0 until 8) { val a = Random.nextFloat() * 6.283f; val s = 190f + Random.nextFloat() * 190f
                    particles.add(Particle(px, py, cos(a) * s, sin(a) * s, 0.42f, 0.42f, Color.rgb(147, 197, 253), dp(4.5f), 1, Random.nextFloat() * 6.28f)) }
                texts.add(FloatText(px, py - dp(44f), "🛡️ SHIELD DOWN", 0.7f, Color.rgb(147, 197, 253), dp(13f)))
                audio.sfx("shieldbreak") // #256 glass ring
            }
            if (dmg <= 0f) { invulnT = max(invulnT, 0.3f); audio.sfx("shieldhit", 0.9f + Random.nextFloat() * 0.2f); return } // #255 metallic ping
        }
        hp -= dmg
        waveHurt = true // #786 real HP loss ends the flawless run
        val ne = enemies.minByOrNull { hyp(it.x - px, it.y - py) } // #163 knockback away from nearest foe
        if (ne != null) { val kd = max(1f, hyp(px - ne.x, py - ne.y)); kbx = (px - ne.x) / kd * 300f; kby = (py - ne.y) / kd * 300f; lastHurtAngle = atan2(ne.y - py, ne.x - px); lastHurtT = 0.9f } // #244 damage direction
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
            vacuumT = 0.9f; for (g in gemDrops) g.mag = true // #135 level-up vacuum
            beams.add(Beam(px, py, 0.7f, Color.WHITE)) // #213 level-up pillar of light
            rings.add(Ring(px, py, dp(20f), 0.5f, 0.5f, Color.rgb(167, 243, 208))) // #232 level-up shockwave
            slowmoT = 0.15f // #165 brief 0.3x slow-mo before the cards settle
            state = State.LEVELUP
            audio.sfx("levelchord") // #298 major arpeggio chord
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
        if (state != State.PLAYING) return
        if (dashCharges <= 0) { dashBufferT = 0.15f; return } // #385 early press is buffered, fires on recharge
        dashCharges--
        kbx = 0f; kby = 0f // #386 dashing out of a knockback cancels the slide (coyote grace)
        if (dashRechargeT <= 0f) dashRechargeT = dashCdMax
        dashT = 0.16f
        if (hyp(joyDx, joyDy) > 0.1f) { dashDx = joyDx; dashDy = joyDy } else { dashDx = cos(lastAim); dashDy = sin(lastAim) } // #164
        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))
        rings.add(Ring(px - dashDx * dp(14f), py - dashDy * dp(14f), dp(6f), 0.26f, 0.26f, Color.rgb(165, 243, 252))) // #505 flipper bubble ring trails the dash
        burst(px, py, CHARS[save.selectedChar].color, 8, 250f) // #495 dash-start spark burst
        invulnT = max(invulnT, 0.22f)
        audio.sfx("dash")
        vibrate(20)
    }

    private fun doRocket() { // 🚀 secondary weapon: homing rocket, big AoE
        if (state != State.PLAYING) return
        if (rocketCharges <= 0) { vibrate(8); return }
        rocketCharges--
        if (rocketRechargeT <= 0f) rocketRechargeT = rocketCdMax
        val wp = WEAPONS[save.selectedWeapon]
        val tgt = enemies.minByOrNull { hyp(it.x - px, it.y - py) }
        val a = if (tgt != null) atan2(tgt.y - py, tgt.x - px) else lastAim
        val rp = Proj(px + cos(a) * dp(22f), py + sin(a) * dp(22f), cos(a) * 560f, sin(a) * 560f,
            wp.dmg * dmgMul * 2.6f, 0, true, dp(8f), Color.rgb(249, 115, 22))
        rp.aoe = dp(80f); rp.life = 2.4f; rp.rocket = true
        projs.add(rp)
        rings.add(Ring(px, py, dp(12f), 0.3f, 0.3f, Color.rgb(249, 115, 22)))
        burst(px - cos(a) * dp(18f), py - sin(a) * dp(18f), Color.rgb(148, 163, 184), 5, 160f) // backblast
        audio.sfx("whoosh", 0.8f)
        vibrate(25)
    }

    private fun coinBurst() { // #174 purchase coins fly to the gem counter
        audio.sfx("purchase") // #262 cha-ching arpeggio
        for (i in 0 until 8) {
            val sx = w / 2 + Random.nextFloat() * 90 - 45; val sy = h * 0.55f + Random.nextFloat() * 44 - 22
            flyGems.add(FlyGem(sx, sy, (sx + w - dp(56f)) / 2, min(sy, dp(62f)) - dp(120f), w - dp(56f), dp(62f), Color.rgb(250, 204, 21)))
        }
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int, speed: Float) {
        val n2 = when (save.gfxQuality) { 2 -> n / 2; 1 -> n; else -> if (avgFrameMs > 21f) n * 3 / 4 else n } // #312 quality scales particles
        for (i in 0 until n2) {
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
        if (state == State.PLAYING) for (pl in pools) if (particles.size < 350 && Random.nextFloat() < 0.1f) // #210 rising poison bubble particles
            particles.add(Particle(pl.x + Random.nextFloat() * pl.r * 0.8f - pl.r * 0.4f, pl.y + Random.nextFloat() * pl.r * 0.5f, 0f, -(26f + Random.nextFloat() * 22f), 0.5f, 0.5f, Color.rgb(134, 239, 172), dp(2.6f)))
        if (state == State.GAMEOVER) { // #225 slow rain of shards over the end screen
            shardRainT -= dt
            if (shardRainT <= 0f) { shardRainT = 0.45f
                fallShards.add(FallShard(Random.nextFloat() * w, -dp(10f), 40f + Random.nextFloat() * 50f, Random.nextFloat() * 24 - 12, dp(3.5f) + Random.nextFloat() * dp(4f), 3, Color.rgb(148, 60, 90), 90f))
            }
        }
        for (s in sparks) s.life -= dt
        sparks.removeAll { it.life <= 0 }
        for (b in blooms) b.life -= dt
        blooms.removeAll { it.life <= 0 }
        for (b in bolts) b.life -= dt
        bolts.removeAll { it.life <= 0 }
        if (state == State.PLAYING) {
            for (pl in pools) {
                for (e in enemies) {
                    // Pool DoT drains hp directly; the death sweep in
                    // updateCombat registers the kill (damageEnemy()
                    // refuses hp <= 0, so it could never do it from here).
                    if (e.hp > 0 && hyp(e.x - pl.x, e.y - pl.y) < pl.r + e.radius * 0.5f)
                        e.hp -= 9f * dt
                }
            }
        }
        for (fp in firePools) fp.life -= dt
        if (state == State.PLAYING) for (fp in firePools) if (particles.size < 350 && Random.nextFloat() < 0.10f) particles.add(Particle(fp.x + (Random.nextFloat() - 0.5f) * fp.r, fp.y, 0f, -34f, 0.5f, 0.5f, Color.rgb(249, 115, 22), dp(2.2f))) // #600 fire pools shed rising embers
        firePools.removeAll { it.life <= 0 }
        if (state == State.PLAYING) { // #127 fire pools burn + set ember tint
            for (fp in firePools) {
                for (e in enemies) {
                    if (e.hp > 0 && hyp(e.x - fp.x, e.y - fp.y) < fp.r + e.radius * 0.5f) {
                        e.burnT = 0.6f
                        e.hp -= 16f * dt // death registered by the updateCombat sweep
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
        for (t in texts) { t.life -= dt; t.y -= dt * 42f * (0.3f + 0.7f * (t.life / t.maxLife)) } // #172 ease-out rise
        texts.removeAll(deadT)
        for (fg in flyGems) fg.t += dt * 1.7f
        flyGems.removeAll { it.t >= 1f }
        for (sg in shardGhosts) sg.life -= dt
        shardGhosts.removeAll { it.life <= 0 }
        for (bm in beams) bm.life -= dt
        beams.removeAll { it.life <= 0 }
        for (fs in fallShards) { fs.vy += fs.grav * dt; fs.x += fs.vx * dt; fs.y += fs.vy * dt; fs.rot += dt * 2.6f }
        fallShards.removeAll { it.y > h + dp(30f) }
    }

    private fun pulseVib(pattern: LongArray) {
        if (!save.vibration) return
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            else @Suppress("DEPRECATION") vib.vibrate(pattern, -1)
        } catch (_: Exception) { }
    }

    private fun distVol(x: Float, y: Float): Float = 1f - min(1f, hyp(x - px, y - py) / (w * 0.8f)) * 0.65f // #281 distance-based explosion volume

    private fun vibrate(ms: Long) {
        if (!save.vibration) return
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vib.vibrate(ms)
        } catch (_: Exception) { }
    }

    // scale a color's brightness (f < 1 darker, f > 1 lighter, clamped)
    private fun shade(col: Int, f: Float): Int = Color.rgb(
        (Color.red(col) * f).toInt().coerceIn(0, 255),
        (Color.green(col) * f).toInt().coerceIn(0, 255),
        (Color.blue(col) * f).toInt().coerceIn(0, 255))

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
        if (e.pointerCount > 2) return true // #382 pinch / extra fingers are ignored — no accidental gestures
        val idx = e.actionIndex
        val pid = if (idx < e.pointerCount) e.getPointerId(idx) else -1
        val x = if (idx < e.pointerCount) e.getX(idx) else 0f
        val y = if (idx < e.pointerCount) e.getY(idx) else 0f
        if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) lastTouchAt = time // #196
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (state == State.INTRO) { state = State.MENU; return true } // #793 tap skips the opening
                if (state == State.PLAYING) {
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) { playDownT = time; rings.add(Ring(x, y, dp(5f), 0.22f, 0.22f, Color.rgb(165, 243, 252))) } // #379 touch ripple
                    val zoneW = if (save.oneHanded) w * 0.7f else w * 0.55f // #375 one-handed: bigger stick zone
                    when { // v2.6: #363 two-finger-tap pause REMOVED (Nathan: double-tap was pausing mid-fight)
                        pauseBtnRect.contains(x, y) -> { if (save.pauseGuard) { pauseHoldPid = pid; pauseHoldT = time } else pauseGame() } // #377 hold-to-pause guard
                        rocketBtnRect.contains(x, y) -> doRocket()
                        dashBtnRect.contains(x, y) -> doDash()
                        x < zoneW && joyPointer == -1 -> {
                            if (time - lastJoyTapT < 0.32f && hyp(x - lastJoyTapX, y - lastJoyTapY) < dp(90f)) doDash() // #354 double-tap dash
                            lastJoyTapT = time; lastJoyTapX = x; lastJoyTapY = y
                            joyPointer = pid; joyOx = x; joyOy = y; joyDx = 0f; joyDy = 0f
                            joyDownT = time; joyStartX = x; joyStartY = y; joySwiped = false; joyWasMax = false
                            vibrate(8) // #372 haptic tick on engage
                        }
                        else -> {
                            if (x >= zoneW) {
                                if (save.manualAim && aimPointer == -1) { aimPointer = pid; aimOx = x; aimOy = y; aimDx = 0f; aimDy = 0f } // #360 aim stick claims the right side
                                else doDash()
                            }
                        }
                    }
                } else if (state == State.LEVELUP) {
                    luDownX = x; luDownY = y // #364 swipe-up-to-pick starts here
                    hitButton(x, y)
                } else if (state == State.SETTINGS) {
                    if (!hitButton(x, y)) {
                        adjustSlider(x, y)
                        if (sliderDrag == null) { scrollDragY = y; scrollDragStart = settingsScroll } // #381 drag the page from any gap
                    }
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
                                var mag = cl / maxR
                                mag = if (mag < 0.08f) 0f else (mag - 0.08f) / 0.92f // #352 8% dead-zone, rescaled
                                joyDx = dx / d * mag; joyDy = dy / d * mag
                                joyDx *= save.joySens; joyDy *= save.joySens // #374 sensitivity
                                val sm = hyp(joyDx, joyDy); if (sm > 1f) { joyDx /= sm; joyDy /= sm }
                                if (mag >= 0.99f && !joyWasMax) { joyWasMax = true; vibrate(15) } else if (mag < 0.9f) joyWasMax = false // #399 tick at max radius
                                if (d > maxR) { joyOx = e.getX(i) - dx / d * maxR; joyOy = e.getY(i) - dy / d * maxR }
                                if (!joySwiped && time - joyDownT < 0.3f && e.getY(i) - joyStartY < -dp(85f)) { joySwiped = true; doDash() } // #356 swipe-up dash
                            }
                        }
                        if (e.getPointerId(i) == aimPointer) { // #360 aim stick tracking, #400 12% dead-zone
                            val dx = e.getX(i) - aimOx; val dy = e.getY(i) - aimOy
                            val d = hyp(dx, dy); val maxR = dp(62f)
                            if (d > 1f) {
                                val cl = min(d, maxR)
                                var mag = cl / maxR
                                mag = if (mag < 0.12f) 0f else (mag - 0.12f) / 0.88f
                                aimDx = dx / d * mag; aimDy = dy / d * mag
                                if (d > maxR) { aimOx = e.getX(i) - dx / d * maxR; aimOy = e.getY(i) - dy / d * maxR }
                            }
                        }
                    }
                } else if (state == State.SETTINGS) {
                    if (sliderDrag != null) adjustSlider(e.getX(0), e.getY(0))
                    else if (scrollDragY >= 0) settingsScroll = (scrollDragStart + (scrollDragY - e.getY(0))).coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                sliderDrag = null; scrollDragY = -1f
                if (e.actionMasked == MotionEvent.ACTION_CANCEL && state == State.PLAYING) pauseGame() // #390 system interrupt auto-pauses
                if (pid == pauseHoldPid) { // #377 long-press pause resolves on release
                    if (time - pauseHoldT >= 0.45f) pauseGame() else if (save.pauseGuard) toast("⏸ Hold the pause button a beat — guard is on")
                    pauseHoldPid = -1
                }
                if (state == State.LEVELUP && e.actionMasked == MotionEvent.ACTION_UP && luDownY - y > dp(60f)) { // #364 swipe a card up to pick it
                    for (hb in hits) if (hb.id.startsWith("up:") && hb.rect.contains(luDownX, luDownY)) { pressId = hb.id; pressStartT = time; onButton(hb.id); break }
                }
                if (pid == aimPointer) { aimPointer = -1; aimDx = 0f; aimDy = 0f }
                if (pid == joyPointer) {
                    if (joyDx != 0f || joyDy != 0f) { // #182 spring return: remember the knob position
                        joySpringT = 0.28f; springOx = joyOx; springOy = joyOy
                        springKx = joyOx + joyDx * dp(38f); springKy = joyOy + joyDy * dp(38f)
                    }
                    joyPointer = -1; joyDx = 0f; joyDy = 0f
                }
            }
        }
        return true
    }

    // #366-#369 hardware keyboard, #370/#371 gamepad buttons
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> keyUp = true
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> keyDown = true
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> keyLeft = true
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> keyRight = true
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_BUTTON_A -> { if (state == State.PLAYING) { doDash(); return true } }
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_BUTTON_START -> {
                when (state) { State.PLAYING -> pauseGame(); State.PAUSED -> resumeGame(); else -> handleBack() }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_B -> {
                when (state) {
                    State.MENU -> startRun()
                    State.PAUSED -> resumeGame()
                    State.GAMEOVER, State.VICTORY -> startRun()
                    State.ONBOARD -> onButton("onbnext")
                    State.WHATSNEW -> onButton("wnok")
                    else -> {}
                }
                return true
            }
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3 -> { // #365 pick a card by number
                if (state == State.LEVELUP) { val i = keyCode - KeyEvent.KEYCODE_1; if (i < offers.size) { onButton("up:${offers[i]}"); return true } }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> keyUp = false
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> keyDown = false
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> keyLeft = false
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> keyRight = false
        }
        return super.onKeyUp(keyCode, event)
    }

    // #370 gamepad left stick
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            val ax = event.getAxisValue(MotionEvent.AXIS_X); val ay = event.getAxisValue(MotionEvent.AXIS_Y)
            padDx = if (abs(ax) > 0.25f) ax else 0f; padDy = if (abs(ay) > 0.25f) ay else 0f
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun hitButton(x: Float, y: Float): Boolean {
        for (hb in hits) {
            val ey = if (state == State.SETTINGS && hb.id != "back") y + settingsScroll else y // #381 scrolled content hit-test
            if (hb.rect.contains(x, ey)) { pressId = hb.id; pressStartT = time; vibrate(12); onButton(hb.id); return true } // #284 light tap
        }
        return false
    }

    private fun onButton(id: String) {
        when {
            id == "play" -> startRun()
            id == "daily" -> startRun(true)
            id == "hardtoggle" -> { save.hardMode = !save.hardMode; audio.sfx("click"); toast(if (save.hardMode) "☠️ HARD MODE ON" else "Hard mode off") }
            id == "endless" -> {
                endlessMode = true; state = State.PLAYING
                audio.playMusic(R.raw.music_endless); audio.sfx("levelup") // #278 endless loop
                nextWave()
                texts.add(FloatText(w / 2, h * 0.3f, "♾️ ENDLESS — how deep can you go?", 2f, Color.rgb(244, 114, 182), dp(19f)))
            }
            id == "reroll" -> {
                val avail = (if (freeRerollUsed) 0 else 1) + rerollTokens
                if (state == State.LEVELUP && avail > 0) {
                    if (!freeRerollUsed) freeRerollUsed = true else rerollTokens--
                    var no = (0..23).shuffled(pickRng()).filter { it !in offers }.take(3)
                    if (no.size < 3) no = (0..23).shuffled(pickRng()).take(3)
                    offers = no
                    audio.sfx("dice") // #260 dice shuffle
                } else audio.sfx("error") // #263
            }
            id == "banish" -> {
                if (state == State.LEVELUP) { gemsRun += 25; state = State.PLAYING; audio.sfx("poof"); toast("🚫 Skipped — +25💎") } // #261 banish poof
            }
            id.startsWith("tab:") -> { shopTab = id.substringAfter(":").toInt(); audio.sfx("click") }
            id.startsWith("perk:") -> {
                val key = id.substringAfter(":")
                val maxLv = when (key) { "crit", "magnet" -> 5; "dash" -> 4; else -> 99 }
                val lvl = when (key) { "crit" -> save.permCrit; "magnet" -> save.permMagnet; "dash" -> save.permDash; else -> 0 }
                if (key != "revive" && lvl >= maxLv) { audio.sfx("hurt"); toast("Already MAX ⭐") }
                else if (pendingConfirm == id) {
                    pendingConfirm = ""
                    val cost = perkCost(key)
                    if (save.gemBank >= cost) {
                        save.gemBank -= cost
                        when (key) { "crit" -> save.permCrit++; "magnet" -> save.permMagnet++; "dash" -> save.permDash++; "reroll" -> save.bankedRerolls++; else -> save.reviveTokens++ }
                        audio.sfx("levelup"); coinBurst(); toast("✨ Perk secured!")
                    } else { audio.sfx("error"); toast("Not enough gems 💎") }
                }
                else if (save.gemBank >= perkCost(key)) { pendingConfirm = id; confirmT = 3f; audio.sfx("click"); toast("Tap again to confirm 💎 ${perkCost(key)}") }
                else { audio.sfx("error"); toast("Not enough gems — need 💎 ${perkCost(key)}") }
            }
            id == "howto" -> { state = State.HOWTO; audio.sfx("click") }
            id == "shop" -> { state = State.SHOP; audio.sfx("click"); audio.playMusic(R.raw.music_shop) } // #277 shop theme
            id == "settings" -> { state = State.SETTINGS; audio.sfx("click") }
            id == "stats" -> { state = State.STATS; audio.sfx("click") }
            id == "heroes" -> { state = State.HEROES; audio.sfx("click") }
            id == "about" -> { state = State.ABOUT; audio.sfx("click") }
            id == "sorttoggle" -> { save.shopSort = !save.shopSort; audio.sfx("click"); toast(if (save.shopSort) "↕ Shop sorted by price" else "↕ Shop order: default") }
            id == "onbnext" -> { if (onboardPage < 2) { onboardPage++; audio.sfx("click") } else { save.onboarded = true; toMenu(); audio.sfx("levelup") } }
            id == "onbskip" -> { save.onboarded = true; toMenu(); audio.sfx("click") }
            id == "wnok" -> { save.seenWhatsNew = true; toMenu(); audio.sfx("click") }
            id == "weapons" -> { state = State.WEAPONS; audio.sfx("click") }
            id == "continue" -> { continueRun() }
            id == "endlessmenu" -> { startRun(); endlessMode = true; audio.playMusic(R.raw.music_endless); audio.sfx("levelup") }
            id == "howtonext" -> { howtoPage = (howtoPage + 1) % 4; audio.sfx("click") }
            id == "howtoprev" -> { howtoPage = (howtoPage + 3) % 4; audio.sfx("click") }
            id == "back" -> { toMenu(); audio.sfx("click") }
            id == "resume" -> resumeGame()
            id == "restart" -> startRun()
            id == "share" -> { shareScore(); audio.sfx("click") } // #791
            id == "quit" -> { snapshotRun(); toMenu(); audio.sfx("click") } // #301 quitting mid-run saves it
            id == "vibrate" -> { save.vibration = !save.vibration; audio.sfx("click"); vibrate(30) }
            id == "musictoggle" -> { // #290 music toggle, separate from the SFX controls
                save.musicOn = !save.musicOn; audio.sfx("click")
                if (save.musicOn) audio.playMusic(if (state == State.PLAYING) R.raw.music_battle else R.raw.music_menu) else audio.stopMusic()
            }
            id == "muteall" -> { // #292 pause-screen mute-all quick toggle
                save.muteAll = !save.muteAll; audio.setMuted(save.muteAll)
                if (!save.muteAll) audio.sfx("click")
            }
            id == "dmgtoggle" -> { save.dmgNumbers = !save.dmgNumbers; audio.sfx("click") }
            id == "timertoggle" -> { save.showTimer = !save.showTimer; audio.sfx("click") }
            id == "hudtoggle" -> { save.bigHud = !save.bigHud; audio.sfx("click") }
            id == "hctoggle" -> { save.highContrast = !save.highContrast; audio.sfx("click") }
            id == "gfxtoggle" -> { save.gfxQuality = (save.gfxQuality + 1) % 3; audio.sfx("click"); toast("✨ Graphics: ${when (save.gfxQuality) { 1 -> "HIGH"; 2 -> "LOW"; else -> "AUTO" }}") } // #312
            id == "aimtoggle" -> { save.manualAim = !save.manualAim; audio.sfx("click"); toast(if (save.manualAim) "🎯 Manual aim — right stick aims, dash is the button" else "🎯 Auto aim") } // #359
            id == "pauseguard" -> { save.pauseGuard = !save.pauseGuard; audio.sfx("click"); toast(if (save.pauseGuard) "⏸ Pause guard: HOLD to pause" else "⏸ Pause guard off — tap pauses") } // #377
            id == "onehand" -> { save.oneHanded = !save.oneHanded; audio.sfx("click"); toast(if (save.oneHanded) "✋ One-handed: wide stick zone, compact HUD" else "✋ One-handed off") } // #375
            id == "ctrlreset" -> { // #397 controls back to defaults
                save.joySize = 1.0f; save.joySens = 1.0f; save.btnOpacity = 0.85f; save.btnScale = 1.0f
                save.joyLeft = false; save.shakeOn = true; settingsScroll = 0f
                audio.sfx("click"); toast("🎛 Controls reset to defaults")
            }
            id == "assisttoggle" -> { save.aimAssist = (save.aimAssist + 1) % 3; audio.sfx("click"); toast("🧲 Aim assist: ${when (save.aimAssist) { 2 -> "HIGH"; 1 -> "LOW"; else -> "OFF" }}") } // #358
            id == "shaketoggle" -> { save.shakeOn = !save.shakeOn; audio.sfx("click") } // #314
            id == "joyside" -> { save.joyLeft = !save.joyLeft; audio.sfx("click"); toast(if (save.joyLeft) "🕹️ Controls: LEFT side" else "🕹️ Controls: RIGHT side") } // #318
            id == "reset" -> { // #319 double confirm
                if (time < resetArmT) { resetArmT = 0f; save.resetAll(); toast("🗑 Save wiped — fresh storm!"); toMenu() }
                else { resetArmT = time + 3f; audio.sfx("error"); toast("⚠️ Tap RESET again to erase EVERYTHING") }
            }
            id.startsWith("char:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsChar(i)) { save.selectedChar = i; audio.sfx("click") }
                else if (pendingConfirm == id) {
                    pendingConfirm = ""
                    val cost = charCost(i)
                    if (save.gemBank >= cost) {
                        save.gemBank -= cost; save.unlockChar(i); save.selectedChar = i
                        if (cost < CHARS[i].cost) save.heroDiscount = false // #148 discount consumed
                        audio.sfx("levelup"); coinBurst(); toast("🎉 Unlocked ${CHARS[i].name}!")
                    }
                    else { audio.sfx("error"); toast("Not enough gems 💎") }
                }
                else if (save.gemBank >= charCost(i)) { pendingConfirm = id; confirmT = 3f; audio.sfx("click"); toast("Tap again to confirm 💎 ${charCost(i)}") }
                else { audio.sfx("error"); toast("Not enough gems — need 💎 ${charCost(i)}") }
            }
            id.startsWith("weap:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsWeapon(i)) { save.selectedWeapon = i; audio.sfx("click") }
                else if (pendingConfirm == id) {
                    pendingConfirm = ""
                    if (save.gemBank >= WEAPONS[i].cost) { save.gemBank -= WEAPONS[i].cost; save.unlockWeapon(i); save.selectedWeapon = i; audio.sfx("levelup"); coinBurst(); toast("🎉 Unlocked ${WEAPONS[i].name}!") }
                    else { audio.sfx("error"); toast("Not enough gems 💎") }
                }
                else if (save.gemBank >= WEAPONS[i].cost) { pendingConfirm = id; confirmT = 3f; audio.sfx("click"); toast("Tap again to confirm 💎 ${WEAPONS[i].cost}") }
                else { audio.sfx("error"); toast("Not enough gems — need 💎 ${WEAPONS[i].cost}") }
            }
            id.startsWith("up:") -> applyUpgrade(id.substringAfter(":").toInt())
        }
    }

    private val musicSlider = RectF()
    private val sfxSlider = RectF()
    private val joySlider = RectF()
    private val sensSlider = RectF(); private val opacSlider = RectF(); private val btnSlider = RectF()
    private var settingsScroll = 0f; private var scrollDragY = -1f; private var scrollDragStart = 0f
    private var avgFrameMs = 16f; private var resetArmT = 0f; private var howtoPage = 0
    private var deaths = 0; private var dailyDay = 0L
    private var onboardPage = 0; private var introChecked = false
    private var aimPointer = -1; private var aimOx = 0f; private var aimOy = 0f; private var aimDx = 0f; private var aimDy = 0f
    private var lastJoyTapT = -9f; private var lastJoyTapX = 0f; private var lastJoyTapY = 0f
    private var joyDownT = 0f; private var joyStartX = 0f; private var joyStartY = 0f; private var joySwiped = false; private var joyWasMax = false
    private var playDownT = -9f; private var luDownX = 0f; private var luDownY = 0f
    private var keyUp = false; private var keyDown = false; private var keyLeft = false; private var keyRight = false
    private var dashBufferT = 0f; private var resumeT = 0f
    private var pauseHoldPid = -1; private var pauseHoldT = 0f
    private var padDx = 0f; private var padDy = 0f
    private fun adjustSlider(x: Float, y: Float) {
        val yy = if (state == State.SETTINGS) y + settingsScroll else y // #381
        if (musicSlider.contains(x, yy)) {
            sliderDrag = "music" // #176
            save.musicVol = ((x - musicSlider.left) / musicSlider.width()).coerceIn(0f, 1f)
            audio.applyMusicVolume()
        } else if (sfxSlider.contains(x, yy)) {
            sliderDrag = "sfx" // #176
            save.sfxVol = ((x - sfxSlider.left) / sfxSlider.width()).coerceIn(0f, 1f)
            if (time - lastPreviewT > 0.13f) { audio.sfx("pickup", 1f); lastPreviewT = time } // #291 live preview sound
        } else if (joySlider.contains(x, yy)) {
            sliderDrag = "joy"
            save.joySize = 0.8f + ((x - joySlider.left) / joySlider.width()).coerceIn(0f, 1f) * 0.5f // #317 joystick size 0.8x-1.3x
        } else if (sensSlider.contains(x, yy)) {
            sliderDrag = "sens"
            save.joySens = 0.7f + ((x - sensSlider.left) / sensSlider.width()).coerceIn(0f, 1f) * 0.7f // #374
        } else if (opacSlider.contains(x, yy)) {
            sliderDrag = "opac"
            save.btnOpacity = 0.35f + ((x - opacSlider.left) / opacSlider.width()).coerceIn(0f, 1f) * 0.65f // #395
        } else if (btnSlider.contains(x, yy)) {
            sliderDrag = "btn"
            save.btnScale = 0.8f + ((x - btnSlider.left) / btnSlider.width()).coerceIn(0f, 1f) * 0.5f // #396
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

    // shrink-to-fit: largest size <= size whose measured width fits maxW (floor 62%)
    private fun fitSize(s: String, size: Float, maxW: Float, bold: Boolean): Float {
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = size
        val w0 = p.measureText(s)
        if (w0 <= maxW || w0 <= 0f) return size
        return max(size * (maxW / w0), size * 0.62f)
    }

    // left-aligned text that can never run past maxX: shrinks, then ellipsizes at the floor
    private fun txtFitL(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, maxX: Float, bold: Boolean = false) {
        val sz = fitSize(s, size, maxX - x, bold)
        var t = s
        p.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        p.textSize = sz
        if (p.measureText(t) > maxX - x) {
            while (t.length > 4 && p.measureText(t + "…") > maxX - x) t = t.dropLast(1)
            t += "…"
        }
        txtL(c, t, x, y, sz, color, bold)
    }

    // centered text shrunk to fit maxW
    private fun txtFitC(c: Canvas, s: String, cx: Float, y: Float, size: Float, color: Int, maxW: Float, bold: Boolean = true, alpha: Int = 255) {
        txt(c, s, cx, y, fitSize(s, size, maxW, bold), color, bold, alpha)
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
        // #151 press scale-down + #152 spring overshoot on release
        var sc = 1f
        if (id == pressId) {
            val e = time - pressStartT
            sc = when {
                e < 0f -> 1f
                e < 0.07f -> 1f - 0.08f * (e / 0.07f)
                e < 0.32f -> { val t2 = (e - 0.07f) / 0.25f; val u = t2 - 1f; 0.92f + 0.08f * (1f + 2.70158f * u * u * u + 1.70158f * u * u) }
                else -> 1f
            }
        }
        val r2 = if (sc == 1f) rect else RectF(rect.centerX() - rect.width() * sc / 2, rect.centerY() - rect.height() * sc / 2, rect.centerX() + rect.width() * sc / 2, rect.centerY() + rect.height() * sc / 2)
        if (state == State.MENU) roundRect(c, RectF(r2.left - dp(5f), r2.top - dp(5f), r2.right + dp(5f), r2.bottom + dp(5f)), dp(17f), fill, (30 + 22 * sin(time * 2.2f + r2.top * 0.013f)).toInt().coerceIn(8, 52)) // #238 idle glow pulse
        roundRect(c, RectF(r2.left, r2.top + dp(3.5f), r2.right, r2.bottom + dp(3.5f)), dp(14f), Color.rgb(2, 0, 10), 90) // #796 a shadow lip grounds every button
        roundRect(c, r2, dp(14f), fill)
        val shx2 = ((time * 0.24f + r2.top * 0.0011f) % 1.6f - 0.3f) // #653 a sheen sweeps every button
        if (shx2 in 0.06f..0.94f) { p.color = Color.WHITE; p.alpha = 15; c.drawRect(r2.left + dp(12f) + (r2.width() - dp(31f)) * shx2, r2.top + dp(3f), r2.left + dp(19f) + (r2.width() - dp(31f)) * shx2, r2.top + r2.height() * 0.45f, p); p.alpha = 255 }
        p.color = Color.WHITE; p.alpha = 20
        c.drawRoundRect(RectF(r2.left + dp(3f), r2.top + dp(3f), r2.right - dp(3f), r2.top + r2.height() * 0.45f), dp(11f), dp(11f), p)
        p.alpha = 255
        if (stroke != 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = stroke
            c.drawRoundRect(r2, dp(14f), dp(14f), p); p.style = Paint.Style.FILL
        }
        val fitFs = fitSize(label, dp(size * sc), r2.width() - dp(10f), true) // labels shrink to fit, never spill
        txt(c, label, r2.centerX(), r2.centerY() + fitFs * 0.36f, fitFs, textColor)
        hits.add(HitBtn(RectF(rect), id))
    }

    private fun drawKnob(c: Canvas, r: RectF, id: String, on: Boolean) { // #175 sliding toggle knob
        val target = if (on) 1f else 0f
        val cur = toggleAnim[id] ?: target
        val nv = cur + (target - cur) * 0.35f
        toggleAnim[id] = nv
        val tw = dp(48f); val th = dp(24f)
        val tx = r.right - tw - dp(14f); val ty = r.centerY() - th / 2
        roundRect(c, RectF(tx, ty, tx + tw, ty + th), th / 2, if (on) Color.rgb(22, 163, 74) else Color.rgb(71, 85, 105))
        if (on) { p.color = Color.rgb(74, 222, 128); p.alpha = (40 + 20 * sin(time * 3f)).toInt().coerceIn(20, 60); c.drawCircle(tx + th / 2 + (tw - th) * nv, r.centerY(), dp(13f), p); p.alpha = 255 } // #716 a live toggle's knob glows
        p.color = Color.WHITE
        c.drawCircle(tx + th / 2 + (tw - th) * nv, r.centerY(), dp(9f), p)
    }

    /** Menu button with a circular icon medallion + label (#40). */
    private fun menuButton(c: Canvas, rect: RectF, icon: String, label: String, fill: Int, id: String,
                           size: Float = 18f, stroke: Int = 0, chipDp: Float = 30f) {
        button(c, rect, "", fill, Color.WHITE, size, id, stroke)
        p.color = Color.WHITE; p.alpha = 36
        c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(17f), p); p.alpha = 255
        poly(c, rect.left + dp(chipDp) + dp(12f), rect.centerY() - dp(12f), dp(3f), 4, time * 2.6f + rect.top * 0.01f, Color.rgb(250, 204, 21)) // #717 menu medallions wear a spinning spark
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.WHITE; p.alpha = (40 + 25 * sin(time * 2.8f + rect.top * 0.01f)).toInt().coerceIn(15, 65); c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(19f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #800 a breathing ring rims every menu medallion
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
            val np2 = 0.82f + 0.18f * sin(time * 0.23f + nx * 0.011f) // #497 nebulae breathe
            p.color = ncol; p.alpha = (26 * np2).toInt()
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = (16 * np2).toInt()
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = (12 * np2).toInt()
            if (sin(time * 1.9f + nx * 0.021f + ny * 0.013f) > 0.93f) poly(c, nx, ny, dp(7f), 4, time * 1.2f, Color.WHITE) // #548 nebula cores twinkle
            p.color = ncol; p.alpha = (12 * np2).toInt() // loop-10 fix: poly() leaves the pen white — restore it before the core (was drawing a solid white disc)
            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = ncol; p.alpha = (20 * np2).toInt(); c.drawCircle(nx, ny, dp(58f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #761 a hairline ring rims the nebula core
        }
        p.alpha = 255
        p.color = Color.rgb(165, 243, 252); p.alpha = 13 // #509 soft spotlight follows your turtle
        c.drawCircle(px, py, dp(200f), p); p.alpha = 255
        // neon grid — #53 pulses on the 126 BPM beat
        val beat = (sin(time * 6.6f) * 0.5f + 0.5f)
        p.color = Color.rgb(88, 28, 135); p.alpha = (if (save.highContrast) 95 else 46) + (beat * 14).toInt(); p.strokeWidth = 1.5f; p.style = Paint.Style.STROKE
        val gap = dp(54f)
        val off = (time * 12f) % gap
        var gx = -gap + off
        while (gx < w) { c.drawLine(gx, 0f, gx, h, p); gx += gap }
        var gy = -gap + off
        while (gy < h) { c.drawLine(0f, gy, w, gy, p); gy += gap }
        val shx = ((time * 0.045f) % 1.4f - 0.2f) * w // #549 a slow sheen sweeps the grid
        p.color = Color.WHITE; p.alpha = 7
        c.drawLine(shx, 0f, shx - dp(120f), h, p)
        c.drawLine(shx + dp(26f), 0f, shx + dp(26f) - dp(120f), h, p)
        if (beat > 0.9f) { p.color = Color.rgb(216, 180, 254); p.alpha = 60; for (k in 0 until 5) c.drawCircle((k * 0.37f + 0.08f) * w, (k * 0.53f + 0.11f) * h, dp(1.6f), p) } // #617 grid nodes sparkle on the beat peak
        p.alpha = 255; p.style = Paint.Style.FILL
        // stars — #51 two-speed parallax drift (far layer crawls, near layer slides)
        for (s in stars) {
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 2f + s.phase))
            p.color = if (s.phase > 3.14f) Color.rgb(254, 240, 138) else Color.rgb(165, 243, 252) // #427 half the sky burns warm
            p.alpha = (200 * tw).toInt()
            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
            if (s.phase > 3.14f && s.size > 2f) { p.alpha = (36 * tw).toInt(); c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size * 2.4f, p) } // #618 warm stars wear a halo
            if (s.size > 2.6f) { // #464 the brightest stars throw a cross glint, #507 slowly rotating
                val gy = (s.y + drift) % (h + 8f) - 4f; val gl = dp(5f) * (0.6f + 0.4f * sin(time * 2f + s.phase))
                val ga3 = 0.45f * sin(time * 0.7f + s.phase)
                p.alpha = (120 * tw).toInt()
                c.drawLine(s.x - cos(ga3) * gl, gy - sin(ga3) * gl, s.x + cos(ga3) * gl, gy + sin(ga3) * gl, p)
                c.drawLine(s.x - cos(ga3 + 1.57f) * gl, gy - sin(ga3 + 1.57f) * gl, s.x + cos(ga3 + 1.57f) * gl, gy + sin(ga3 + 1.57f) * gl, p)
            }
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.WHITE; p.alpha = 16 // #582 a vast slow star-ring turns in the deep background
        for (k in 0 until 4) c.drawArc(RectF(w * 0.85f - dp(150f), h * 0.18f - dp(150f), w * 0.85f + dp(150f), h * 0.18f + dp(150f)), time * 6f + k * 90f, 40f, false, p)
        p.style = Paint.Style.FILL; p.alpha = 255
        // #426 shooting stars: one streak every ~9s, path set by the cycle
        val cyc = (time / 9f).toInt(); val sph = time - cyc * 9f
        if (sph < 0.8f) {
            val sx0 = (0.5f + 0.5f * sin(cyc * 12.9898f)) * w * 0.7f
            val sy0 = (0.5f + 0.5f * cos(cyc * 78.233f)) * h * 0.3f
            val sf = sph / 0.8f; val hx = sx0 + sf * dp(190f); val hy = sy0 + sf * dp(120f)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252)
            p.alpha = (170 * (1f - sf)).toInt()
            c.drawLine(hx - dp(44f), hy - dp(28f), hx, hy, p)
            p.style = Paint.Style.FILL
            p.color = Color.rgb(165, 243, 252); p.alpha = (60 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(7f), p)
            p.color = Color.WHITE; p.alpha = (220 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(2.4f), p) // #629 the shooting star's head glows
        }
        // #463 a ringed giant drifts across the deep background
        val pph = (time % 75f) / 75f
        if (pph < 0.6f) {
            val plx = -dp(90f) + (w + dp(180f)) * (pph / 0.6f); val ply = h * 0.16f
            p.color = Color.rgb(30, 16, 54); p.alpha = 150
            c.drawCircle(plx, ply, dp(64f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(168, 85, 247); p.alpha = 110
            c.drawArc(RectF(plx - dp(64f), ply - dp(64f), plx + dp(64f), ply + dp(64f)), -60f, 90f, false, p)
            p.strokeWidth = dp(5f); p.color = Color.rgb(88, 60, 150); p.alpha = 80
            c.drawOval(RectF(plx - dp(98f), ply - dp(18f), plx + dp(98f), ply + dp(18f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(203, 213, 225); p.alpha = 130 // #630 a moon rides with the giant
            c.drawCircle(plx + cos(time * 0.5f) * dp(84f), ply + sin(time * 0.5f) * dp(30f), dp(5f), p); p.alpha = 255
        }
        p.alpha = 255
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        hits.clear()
        val cd = "GEMSTORM — ${state.name.lowercase()} screen"
        if (contentDescription != cd) contentDescription = cd
        drawBackground(c)
        val zooming = screenT < 0.25f && state != State.PLAYING // #154 crossfade + slight zoom
        if (zooming) { val z = 0.96f + 0.04f * (screenT / 0.25f); c.save(); c.scale(z, z, w / 2, h / 2) }
        when (state) {
            State.INTRO -> drawIntro(c)
            State.MENU -> drawMenu(c)
            State.HOWTO -> drawHowTo(c)
            State.SHOP -> drawShop(c)
            State.SETTINGS -> drawSettings(c)
            State.STATS -> drawStats(c)
            State.HEROES -> drawHeroes(c)
            State.WEAPONS -> drawWeapons(c)
            State.ONBOARD -> drawOnboard(c)
            State.WHATSNEW -> drawWhatsNew(c)
            State.ABOUT -> drawAbout(c)
            State.PLAYING, State.LEVELUP, State.PAUSED -> {
                drawGame(c)
                if (state == State.LEVELUP) drawLevelUp(c)
                if (state == State.PAUSED) drawPaused(c)
            }
            State.GAMEOVER -> { drawGame(c); drawEnd(c, false) }
            State.VICTORY -> { drawGame(c); drawEnd(c, true) }
        }
        if (zooming) c.restore()
        if (screenT < 0.22f && state != State.PLAYING) {
            p.color = Color.BLACK; p.alpha = ((1f - screenT / 0.22f) * 150).toInt().coerceIn(0, 150)
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }
        if (flashT > 0) { p.color = Color.WHITE; p.alpha = (150 * (flashT / 0.18f)).toInt().coerceIn(0, 150); c.drawRect(0f, 0f, w, h, p); p.alpha = 255 } // #226 boss-death white flash frame
        if (goldFlashT > 0) { p.color = Color.rgb(250, 204, 21); p.alpha = (80 * (goldFlashT / 0.12f)).toInt().coerceIn(0, 80); c.drawRect(0f, 0f, w, h, p); p.alpha = 255 } // #228 elite gold flash
        for (fg in flyGems) { // #167/#174 gems + coins arcing to their counters
            val ft = fg.t.coerceIn(0f, 1f); val fu = 1f - ft
            val fx = fu * fu * fg.sx + 2 * fu * ft * fg.cx + ft * ft * fg.tx
            val fy = fu * fu * fg.sy + 2 * fu * ft * fg.cy + ft * ft * fg.ty
            p.alpha = (255 * (1f - ft * 0.35f)).toInt()
            poly(c, fx, fy, dp(6f) * (1f - ft * 0.35f), 4, 0.78f + ft * 5f, fg.color)
            p.alpha = 255
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
        val shx = if (shake > 0 && save.shakeOn) (Random.nextFloat() - 0.5f) * shake else 0f // #314 shake toggle
        val shy = if (shake > 0 && save.shakeOn) (Random.nextFloat() - 0.5f) * shake else 0f
        c.save()
        c.translate(shx, shy)

        // #74 scorch decals
        for (d in decals) {
            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = 36; c.drawCircle(d.x, d.y, dp(4f), p); p.alpha = 255 } // #765 fresh scorch glows at the center
            p.color = Color.rgb(0, 0, 0); p.alpha = (90 * (d.life / 6f)).toInt().coerceIn(0, 90)
            c.drawCircle(d.x, d.y, d.r, p)
            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = (26 * ((d.life - 4f) / 2f)).toInt().coerceIn(0, 26); c.drawCircle(d.x, d.y, d.r * 0.7f, p) } // #601 fresh scorch still glows
            if (d.life > 2f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(120, 53, 15); p.alpha = (40 * (d.life / 6f)).toInt().coerceIn(0, 40); c.drawCircle(d.x, d.y, d.r, p); p.style = Paint.Style.FILL } // #642 scorch marks keep a baked rim
        }
        p.alpha = 255
        for (b in blooms) { // #245 explosion light bloom (radial gradient)
            val bf = (b.life / b.maxLife).coerceIn(0f, 1f)
            p.shader = RadialGradient(b.x, b.y, b.r, Color.argb((110 * bf).toInt(), Color.red(b.color), Color.green(b.color), Color.blue(b.color)), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
            c.drawCircle(b.x, b.y, b.r, p)
            p.shader = null
            p.color = Color.WHITE; p.alpha = (120 * bf).toInt().coerceIn(0, 120); c.drawCircle(b.x, b.y, b.r * 0.18f, p); p.alpha = 255 // #603 blooms burn white at the core
        }

        // #239 ambient dust motes drifting up through the arena
        for (i in 0 until 14) {
            val mx = ((i * 0.173f + 0.03f) % 1f) * w + sin(time * 0.24f + i * 1.7f) * dp(14f)
            val my = h - ((time * 11f + i * 173f) % (h + dp(40f))) + dp(20f)
            p.color = Color.rgb(203, 213, 225); p.alpha = 34 + (i % 3) * 12
            c.drawCircle(mx, my, dp(1.7f), p)
            if (i % 5 == 0) { p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 3f + i * 2.3f)).toInt().coerceIn(10, 90); c.drawCircle(mx, my, dp(0.9f), p) } // #605 every fifth dust mote glints
            if (i % 5 == 0) { p.color = Color.rgb(165, 243, 252); p.alpha = 55; c.drawCircle(mx + dp(3f), my - dp(2f), dp(0.6f), p); p.alpha = 255 } // #766 glinting motes trail a smaller twin
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
            p.color = Color.rgb(20, 83, 45); p.alpha = (70 * fade).toInt() // #522 dark toxic core churns
            c.drawCircle(pl.x + cos(-time * 1.1f + pl.y * 0.05f) * pl.r * 0.3f, pl.y + sin(-time * 0.9f + pl.x * 0.05f) * pl.r * 0.3f, pl.r * 0.3f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(20, 83, 45); p.alpha = (90 * fade).toInt(); c.drawCircle(pl.x, pl.y, pl.r - dp(1f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #764 a dark rim rings every poison pool
            for (k in 0 until 3) { // #191 bubbling pulse
                val ph = (time * 0.8f + pl.x * 0.13f + k * 0.37f) % 1f
                p.color = Color.rgb(134, 239, 172); p.alpha = (80 * (1f - ph) * fade).toInt()
                c.drawCircle(pl.x + cos(k * 2.4f + pl.y) * pl.r * 0.45f, pl.y + sin(k * 1.9f + pl.x) * pl.r * 0.4f, dp(3.5f) * sin(ph * 3.14159f), p)
                if (ph > 0.42f && ph < 0.58f) { p.color = Color.WHITE; p.alpha = (120 * fade).toInt(); c.drawCircle(pl.x + cos(k * 2.4f + pl.y) * pl.r * 0.45f - dp(1f), pl.y + sin(k * 1.9f + pl.x) * pl.r * 0.4f - dp(1f), dp(1f), p) } // #644 bubbles flash a highlight at their peak
            }
            p.alpha = 255
        }
        // #127 Pyro fire pools (flickering)
        for (fp in firePools) {
            val fade = min(1f, fp.life)
            p.color = Color.rgb(249, 115, 22); p.alpha = (52 * fade).toInt()
            c.drawCircle(fp.x, fp.y, fp.r * (1f + 0.08f * sin(time * 11f + fp.x)), p)
            p.color = Color.rgb(254, 240, 138); p.alpha = (46 * fade).toInt() // #607 a pale hot core flickers at the center
            c.drawCircle(fp.x + sin(time * 9.3f + fp.y) * dp(3f), fp.y + cos(time * 7.7f + fp.x) * dp(3f), fp.r * 0.3f, p)
            p.color = Color.WHITE; p.alpha = (110 * fade).toInt(); c.drawCircle(fp.x, fp.y, dp(1.5f), p); p.alpha = 255 // #763 a white speck hearts every fire pool
            p.color = Color.rgb(239, 68, 68); p.alpha = (70 * fade).toInt()
            c.drawCircle(fp.x, fp.y, fp.r * 0.6f, p)
            p.color = Color.rgb(254, 240, 138); p.alpha = (90 * fade).toInt()
            c.drawCircle(fp.x + sin(time * 9f + fp.y) * dp(5f), fp.y - dp(3f), fp.r * 0.28f, p)
            for (k in 0 until 3) { // #521 flame tongues lick upward
                val fx2 = fp.x + cos(k * 2.1f + fp.x * 0.07f) * fp.r * 0.45f
                val fh = fp.r * (0.30f + 0.16f * sin(time * 13f + k * 2.1f + fp.y * 0.05f))
                p.color = Color.rgb(251, 146, 60); p.alpha = (70 * fade).toInt()
                c.drawCircle(fx2, fp.y - fh, fp.r * 0.16f, p)
                p.color = Color.rgb(254, 240, 138); p.alpha = (60 * fade).toInt(); c.drawCircle(fx2, fp.y - fh, fp.r * 0.07f, p) // #643 flame tongues burn pale at the tip
            }
            p.alpha = 255
        }
        // #72 arena edge neon trim
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f)
        p.color = if (bossWave && enemies.any { it.type == 5 }) Color.rgb(220, 38, 38) else Color.rgb(168, 85, 247)
        p.alpha = 90
        c.drawRect(dp(5f), dp(5f), w - dp(5f), h - dp(5f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(3.4f); p.alpha = 170 // #523 bright corner brackets
        val cl = dp(18f); val ci = dp(5f)
        c.drawLine(ci, ci + cl, ci, ci, p); c.drawLine(ci, ci, ci + cl, ci, p)
        c.drawLine(w - ci - cl, ci, w - ci, ci, p); c.drawLine(w - ci, ci, w - ci, ci + cl, p)
        c.drawLine(ci, h - ci - cl, ci, h - ci, p); c.drawLine(ci, h - ci, ci + cl, h - ci, p)
        c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)
        if (state == State.PLAYING && maxHp > 0 && hp / maxHp < 0.25f) { // #539 low-HP heartbeat edge glow
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(239, 68, 68)
            p.alpha = (40 + 35 * sin(time * 6.5f)).toInt().coerceIn(5, 75)
            c.drawRect(dp(8f), dp(8f), w - dp(8f), h - dp(8f), p)
            p.strokeWidth = dp(3.4f); p.alpha = (120 + 90 * sin(time * 6.5f)).toInt().coerceIn(30, 210) // #652 the corner brackets throb with the heartbeat
            c.drawLine(ci, ci + cl, ci, ci, p); c.drawLine(ci, ci, ci + cl, ci, p)
            c.drawLine(w - ci - cl, ci, w - ci, ci, p); c.drawLine(w - ci, ci, w - ci, ci + cl, p)
            c.drawLine(ci, h - ci - cl, ci, h - ci, p); c.drawLine(ci, h - ci, ci + cl, h - ci, p)
            c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)
        }
        p.style = Paint.Style.FILL; p.alpha = 255
        if (enemies.any { it.type == 5 }) { // #82 boss arena tint
            p.color = Color.rgb(190, 18, 60); p.alpha = (11 + 5 * sin(time * 2f)).toInt().coerceIn(6, 16) // #524 the boss tint breathes
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }

        // #31 magnet radius ring while moving
        if (state == State.PLAYING && (pvx != 0f || pvy != 0f)) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); p.color = Color.rgb(52, 211, 153)
            p.alpha = (26 + 14 * sin(time * 3.4f)).toInt().coerceIn(12, 40)
            c.drawCircle(px, py, magnetR, p); p.style = Paint.Style.FILL; p.alpha = 255
            (0 until 3).forEach { k -> val ma = time * 1.8f + k * 2.09f; p.color = Color.rgb(167, 243, 208); p.alpha = 60; c.drawCircle(px + cos(ma) * magnetR, py + sin(ma) * magnetR, dp(1.8f), p) }; p.alpha = 255 // #684 ants march the magnet ring
        }

        // gems
        for (g in gemDrops) {
            val big = g.value >= 5
            val r = if (big) dp(11f) else dp(7f)
            val bob = sin(time * 4f + g.x * 0.05f) * dp(2f)
            p.color = Color.rgb(52, 211, 153); p.alpha = 70
            c.drawCircle(g.x, g.y + bob, r * 1.9f, p); p.alpha = 255
            val gNear = if (!g.mag) gemDrops.firstOrNull { it !== g && !it.mag && hyp(it.x - g.x, it.y - g.y) < dp(30f) } else null // #562 gems about to merge hum together
            if (gNear != null) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(167, 243, 208); p.alpha = (40 + 25 * sin(time * 6f + g.x * 0.05f)).toInt().coerceIn(15, 65); c.drawLine(g.x, g.y + bob, gNear.x, gNear.y + sin(time * 4f + gNear.x * 0.05f) * dp(2f), p); p.style = Paint.Style.FILL; p.alpha = 255 }
            poly(c, g.x, g.y + bob, r, 4, 0.78f, Color.rgb(52, 211, 153))
            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))
            p.color = Color.WHITE; p.alpha = 110; c.drawRect(g.x - dp(0.6f), g.y + bob - r * 0.34f, g.x + dp(0.6f), g.y + bob + r * 0.34f, p); p.alpha = 255 // #768 a facet line splits every gem
            // #60 sparkle cross
            p.color = Color.WHITE; p.alpha = 190
            val sp = r * (0.7f + 0.3f * sin(time * 5f + g.x))
            c.drawRect(g.x - sp, g.y + bob - dp(0.8f), g.x + sp, g.y + bob + dp(0.8f), p)
            c.drawRect(g.x - dp(0.8f), g.y + bob - sp, g.x + dp(0.8f), g.y + bob + sp, p)
            p.alpha = 255
            if (!big && sin(time * 5f + g.x) > 0.92f) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(g.x, g.y + bob, dp(1.3f), p); p.alpha = 255 } // #613 small gems wink
            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }
            if (big) { // #460 rich gems wear a rotating sparkle
                val sa2 = time * 3f + g.x * 0.05f
                poly(c, g.x + cos(sa2) * r * 1.5f, g.y + bob + sin(sa2) * r * 1.5f, r * 0.4f, 4, sa2, Color.rgb(254, 240, 138))
            }
            if (big) { // #527 rich gems also wear a slow halo ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.3f); p.color = Color.rgb(254, 240, 138); p.alpha = 90
                c.drawArc(RectF(g.x - r * 1.7f, g.y + bob - r * 1.7f, g.x + r * 1.7f, g.y + bob + r * 1.7f), time * 60f + g.x * 0.03f, 200f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (sin(time * 2.4f + g.x * 0.13f + g.y * 0.071f) > 0.94f) // #500 idle gems twinkle
                poly(c, g.x, g.y + bob, r * 0.85f, 4, time * 1.5f, Color.WHITE)
            if (g.mag) { // #442 comet tail as gems rocket home
                val ga2 = atan2(py - g.y, px - g.x)
                p.color = Color.rgb(52, 211, 153); p.alpha = 90
                poly(c, g.x - cos(ga2) * r * 1.5f, g.y + bob - sin(ga2) * r * 1.5f, r * 0.55f, 4, 0.78f, Color.rgb(52, 211, 153))
                p.alpha = 255
            }
            if (vacuumT > 0 && g.mag) { // #590 vacuum streams burn white at the tip
                val ga3 = atan2(py - g.y, px - g.x)
                poly(c, g.x - cos(ga3) * r * 2.6f, g.y + bob - sin(ga3) * r * 2.6f, r * 0.4f, 4, 0.78f, Color.WHITE)
            }
            if (g.mag) { // #75 magnet stream line to the player
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(52, 211, 153); p.alpha = 42
                c.drawLine(g.x, g.y + bob, px, py, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (vacuumT > 0 && g.mag) { // #243 bright vacuum lines during the level-up vacuum
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(167, 243, 208); p.alpha = 120
                c.drawLine(g.x, g.y + bob, px, py, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (vacuumT > 0 && g.mag) { p.color = Color.WHITE; p.alpha = 150; c.drawCircle((g.x + px) / 2, (g.y + bob + py) / 2, dp(1.6f), p); p.alpha = 255 } // #685 a bead rides each vacuum stream
        }

        // #91 merge glow when the field is rich with gems
        if (gemDrops.size > 20) {
            var mx = 0f; var my = 0f
            for (g in gemDrops) { mx += g.x; my += g.y }
            mx /= gemDrops.size; my /= gemDrops.size
            p.color = Color.rgb(52, 211, 153); p.alpha = (22 + 10 * sin(time * 3f)).toInt().coerceIn(12, 32)
            c.drawCircle(mx, my, dp(90f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.alpha = 60 // #520 rotating dashed ring on the merge glow
            for (k in 0 until 3) c.drawArc(RectF(mx - dp(90f), my - dp(90f), mx + dp(90f), my + dp(90f)), time * 40f + k * 120f, 60f, false, p)
            p.strokeWidth = dp(1.1f); p.alpha = 34; c.drawCircle(mx, my, dp(90f) * (0.7f + 0.3f * (0.5f + 0.5f * sin(time * 3f))), p) // #686 a second ring breathes inside the merge glow
            p.style = Paint.Style.FILL; p.alpha = 255
        }

        // hearts
        for (hd in hearts) {
            val urg = if (maxHp > 0) 1f - hp / maxHp else 0f // #506 the heart beats faster the more hurt you are
            val pulse = 1f + (0.18f + 0.14f * urg) * sin(time * (6f + 7f * urg) + hd.x * 0.03f)
            val r = dp(10f) * pulse
            p.color = Color.BLACK; p.alpha = 70 // #455 soft shadow grounds the heart
            c.drawOval(RectF(hd.x - r * 0.8f, hd.y + r * 1.15f, hd.x + r * 0.8f, hd.y + r * 1.55f), p); p.alpha = 255
            // v2.3.1: ONE red heart — the screen-filling green beacon (#246), the ring and the cross are gone
            p.color = Color.rgb(220, 38, 38); p.alpha = 45
            c.drawCircle(hd.x, hd.y, r * 1.9f, p); p.alpha = 255
            drawHeart(c, hd.x, hd.y, r, Color.rgb(239, 68, 68)) // v2.6: one true heart, no triangle
            p.alpha = 130; drawHeart(c, hd.x - r * 0.16f, hd.y - r * 0.2f, r * 0.44f, Color.rgb(252, 165, 165)); p.alpha = 255 // #769 a pale inner heart glows inside every health drop
            if (hyp(px - hd.x, py - hd.y) < dp(140f)) { // #528 a faint red thread reaches for you when close
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(239, 68, 68); p.alpha = (50 + 30 * sin(time * 7f)).toInt().coerceIn(20, 80)
                c.drawLine(hd.x, hd.y, px, py, p); c.drawCircle((hd.x + px) / 2, (hd.y + py) / 2 + sin(time * 7f) * dp(2f), dp(2f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #595 a bead rides the heart thread
            }
        }

        // v2.6: the pickup heart that hovered on the player is GONE (Nathan) — the +15 HP text, ring and burst at the collect site still say it

        // #187 elite spawn beams of light
        for (bm in beams) {
            val bf = (bm.life / 0.55f).coerceIn(0f, 1f)
            // v2.6: the #187/#213 light-pillar columns are GONE — opaque trails removed globally; the landing ring below stays
            p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = bm.color; p.alpha = (200 * bf).toInt()
            c.drawCircle(bm.x, bm.y, dp(26f) * (1.6f - bf * 0.6f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }

        // enemies
        for (e in enemies) {
            val eSc = if (e.spawnT > 0) 1f - 0.55f * min(1f, e.spawnT / 0.32f) else 1f // #160 + #199
            if (e.spawnT > 0.24f && (e.type == 2 || e.type == 10 || e.type == 5)) { // #452 heavy units land with a dust ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(168, 162, 158)
                p.alpha = ((e.spawnT - 0.24f) / 0.08f * 120f).toInt().coerceIn(0, 120)
                c.drawCircle(e.x, e.y, e.radius * (2.2f - (e.spawnT - 0.24f) * 6f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            // v2.6: the #247 elite beacon column + #626 sparks are GONE — opaque trails removed globally (elites keep their #501 rings)
            if (e.type == 5 && bossIntroT > 0) { // #563 the Warden enters under a spotlight
                p.color = Color.WHITE; p.alpha = (26 * (bossIntroT / 2f).coerceIn(0f, 1f)).toInt()
                c.drawCircle(e.x, e.y, e.radius * 2.6f, p); p.alpha = 255
            }
            if (e.elite) { // #501 elites wear a rotating gold double-ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21); p.alpha = 150
                c.drawArc(RectF(e.x - e.radius - dp(4f), e.y - e.radius - dp(4f), e.x + e.radius + dp(4f), e.y + e.radius + dp(4f)), time * 130f, 240f, false, p)
                p.alpha = 90
                c.drawArc(RectF(e.x - e.radius - dp(7f), e.y - e.radius - dp(7f), e.x + e.radius + dp(7f), e.y + e.radius + dp(7f)), -time * 95f, 200f, false, p)
                p.strokeWidth = dp(1.1f); p.color = Color.rgb(254, 240, 138); p.alpha = 60 // #637 a third hairline ring counter-turns
                c.drawArc(RectF(e.x - e.radius - dp(10f), e.y - e.radius - dp(10f), e.x + e.radius + dp(10f), e.y + e.radius + dp(10f)), time * 60f, 120f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 5) { // #502 boss dread ring on the ground
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.6f); p.color = Color.rgb(220, 38, 38); p.alpha = (60 + 30 * sin(time * 3.2f)).toInt().coerceIn(30, 90)
                c.drawCircle(e.x, e.y, e.radius * (1.25f + 0.05f * sin(time * 3.2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                for (k in 0 until 4) { val ta3 = k * 1.5708f + time * 0.9f; poly(c, e.x + cos(ta3) * e.radius * 1.25f, e.y + sin(ta3) * e.radius * 1.25f, dp(3f), 4, ta3, Color.rgb(220, 38, 38)) } // #636 thorn sparks ride the dread ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(220, 38, 38); p.alpha = 36; c.drawCircle(e.x, e.y, e.radius * 1.12f, p); p.style = Paint.Style.FILL; p.alpha = 255 // #742 the dread ring echoes inside itself
            }
            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
                for (k in 0 until 3) { val ca2 = k * 2.09f + e.x * 0.05f; p.color = Color.WHITE; p.alpha = 170; c.drawCircle(e.x + cos(ca2) * e.radius * 0.92f, e.y + sin(ca2) * e.radius * 0.92f, dp(0.9f), p) }; p.alpha = 255 // #743 frost crystals cap with white tips
                if (e.frostAge > 1.2f) poly(c, e.x, e.y - e.radius - dp(3f), dp(3.6f), 4, time * 2.4f, Color.WHITE) // #635 deep-frozen foes grow a crown crystal
            }
            if (e.type == 8) { // #541 orbiter's path rings around you
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.WHITE; p.alpha = 26
                c.drawCircle(px, py, hyp(e.x - px, e.y - py), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 7 && e.state == 1) { // #574 the lancer telegraphs its charge line
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(239, 68, 68); p.alpha = (40 + 30 * sin(time * 10f)).toInt().coerceIn(10, 70)
                c.drawLine(e.x, e.y, px, py, p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 3) { // #575 splitters jitter a ghost echo
                p.color = enemyColor(3); p.alpha = 36
                c.drawCircle(e.x + sin(e.wob * 2.2f) * dp(4f), e.y + cos(e.wob * 1.9f) * dp(3f), e.radius * 0.9f, p); p.alpha = 255
            }
            if (e.type == 2) { // #576 brute shoulders smolder
                p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 4.5f + e.x * 0.02f)).toInt().coerceIn(20, 100)
                c.drawCircle(e.x - e.radius * 0.55f, e.y - e.radius * 0.5f, dp(3.6f), p)
                c.drawCircle(e.x + e.radius * 0.55f, e.y - e.radius * 0.5f, dp(3.6f), p); p.alpha = 255
            }
            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
                p.color = shade(enemyColor(8), 1.5f); p.alpha = 120; c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f + 3.1416f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f + 3.1416f) * (e.radius + dp(6f)), dp(1.4f), p); p.alpha = 255 // #667 a dimmer twin spark counterweights it
            }
            if (e.type == 4 && e.spawnT > 0.12f) { // #585 summoned minis flash a fuchsia birth ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(232, 121, 249); p.alpha = (150 * (e.spawnT / 0.32f)).toInt().coerceIn(0, 150)
                c.drawCircle(e.x, e.y, e.radius + dp(3f), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 9) { // #586 rune sparks orbit the Necroshard
                for (k in 0 until 3) { val ra9 = time * 2.2f + k * 2.09f
                    poly(c, e.x + cos(ra9) * (e.radius + dp(5f)), e.y + sin(ra9) * (e.radius + dp(5f)), dp(2.6f), 4, ra9, Color.rgb(232, 121, 249)) }
            }
            if (e.type == 10) { // #587 the Bulwark Brute's guard arc rotates
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(147, 197, 253); p.alpha = 130
                c.drawArc(RectF(e.x - e.radius - dp(4f), e.y - e.radius - dp(4f), e.x + e.radius + dp(4f), e.y + e.radius + dp(4f)), time * 70f, 150f, false, p)
                p.style = Paint.Style.FILL; (0 until 3).forEach { k -> val ba3 = (time * 70f + k * 50f) * 0.01745f; p.color = Color.rgb(219, 234, 254); p.alpha = 170; c.drawCircle(e.x + cos(ba3) * (e.radius + dp(4f)), e.y + sin(ba3) * (e.radius + dp(4f)), dp(1.5f), p) } // #669 rivets ride the guard arc
                p.alpha = 255
            }
            if (e.type == 0 && enemies.count { it.type == 0 } >= 6) { // #588 a full chaser pack glows hungry
                p.color = Color.rgb(239, 68, 68); p.alpha = 22
                c.drawCircle(e.x, e.y, e.radius * 1.5f, p); p.alpha = 255
            }
            if (e.slowT > 0 && e.frostAge > 0.5f) { // #542 deep-frozen foes get an ice-cube outline
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(165, 243, 252); p.alpha = 120
                c.drawRoundRect(RectF(e.x - e.radius - dp(2f), e.y - e.radius - dp(2f), e.x + e.radius + dp(2f), e.y + e.radius + dp(2f)), dp(5f), dp(5f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            c.save(); c.translate(e.x, e.y); c.scale(eSc, eSc); c.translate(-e.x, -e.y)
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
            // #407 ground shadow grounds the unit
            p.color = Color.BLACK; p.alpha = 80
            c.drawOval(RectF(e.x - e.radius * 0.95f, e.y + e.radius * 0.72f, e.x + e.radius * 0.95f, e.y + e.radius * 1.12f), p); p.alpha = 255
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
            if (chromaT > 0) { // #230 chromatic RGB-split ghosts on big explosions
                p.alpha = 80
                poly(c, e.x - dp(3f), e.y, e.radius, 4, time * 2f, Color.rgb(239, 68, 68))
                poly(c, e.x + dp(3f), e.y, e.radius, 4, time * 2f, Color.rgb(34, 211, 238))
                p.alpha = 255
            }
            when (e.type) {
                0 -> poly(c, e.x, e.y, e.radius * (1f + 0.13f * sin(e.wob * 3f)), 3, atan2(py - e.y, px - e.x) + 1.57f, col)
                1 -> { // dart + #469 motion streaks
                    for (tr3 in 1..2) { val da = atan2(py - e.y, px - e.x); p.alpha = 80 - tr3 * 25; poly(c, e.x - cos(da) * e.radius * 0.55f * tr3, e.y - sin(da) * e.radius * 0.55f * tr3, e.radius * (1f - tr3 * 0.22f), 4, time * 5f, col) }
                    p.alpha = 255
                    poly(c, e.x, e.y, e.radius, 4, time * 5f, col)
                }
                2 -> poly(c, e.x, e.y, e.radius, 6, time * 0.8f, col)
                3 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.6f, col); poly(c, e.x, e.y, e.radius * 0.55f, 4, -time * 2.3f, shade(col, 1.5f)) } // #672 the splitter's heart counter-turns
                4 -> { poly(c, e.x, e.y, e.radius * (1f + 0.2f * sin(time * 10f + e.wob)), 4, time * 3f, col); p.color = Color.WHITE; p.alpha = 190; c.drawCircle(e.x, e.y, dp(1.6f), p); p.alpha = 255 } // #673 minis carry a bright speck
                5 -> {
                    poly(c, e.x, e.y, e.radius, 8, time * 0.6f, col)
                    poly(c, e.x, e.y, e.radius * 0.62f, 8, -time * 0.9f, Color.rgb(233, 213, 255))
                    // crown spikes — #59 seven, gold-pulsed
                    for (i in 0 until 7) {
                        val a = -1.57f + (i - 3) * 0.38f
                        val spikeR = dp(7f) * (1f + 0.25f * sin(time * 4f + i))
                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR, 3, a, Color.rgb(250, 204, 21))
                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))
                        if (i == 3) { p.color = Color.WHITE; p.alpha = (130 + 90 * sin(time * 6f)).toInt().coerceIn(40, 220); c.drawCircle(e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, dp(1.7f), p); p.alpha = 255 } // #748 the crown's center spike glints
                    }
                    if (((time * 2f + e.x * 0.01f).toInt() % 3) == 0) poly(c, e.x, e.y - e.radius * 1.05f, dp(4.4f), 4, time * 3f, Color.WHITE) // #674 the crown's center gem flashes
                }
                6 -> poly(c, e.x, e.y, e.radius, 5, time * 2.2f, col)
                7 -> poly(c, e.x, e.y, e.radius * 1.15f, 3, atan2(py - e.y, px - e.x) + 1.57f, col)
                8 -> { poly(c, e.x, e.y, e.radius, 6, -time * 2.6f, col); poly(c, e.x, e.y, e.radius * 0.45f, 4, time * 3f, Color.rgb(236, 254, 255)) }
                9 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.2f, col); poly(c, e.x, e.y, e.radius * 0.5f, 4, -time * 2f, Color.rgb(240, 171, 252)); poly(c, e.x, e.y, e.radius * 0.78f, 4, time * 0.7f, Color.rgb(88, 28, 135), false) } // #668 a rune ring turns inside the Necroshard
                10 -> poly(c, e.x, e.y, e.radius, 6, 0.3f, col)
                11 -> { poly(c, e.x, e.y, e.radius, 4, 0.78f + sin(time * 6f) * 0.2f, col); poly(c, e.x, e.y - e.radius * 1.15f, dp(6f), 3, -1.57f, Color.rgb(34, 197, 94)); p.color = Color.rgb(250, 204, 21); p.alpha = (170 + 70 * sin(time * 7f)).toInt().coerceIn(100, 240); c.drawCircle(e.x, e.y - e.radius * 1.55f - (sin(time * 5f) * sin(time * 5f)) * dp(4f), dp(3.2f), p); p.alpha = 255 } // #670 a gold coin bounces over the gnome
            }
            if (e.type == 11) { p.color = Color.rgb(187, 247, 208); p.alpha = 210; c.drawCircle(e.x, e.y - e.radius * 1.15f - dp(5.5f), dp(2.1f), p); p.alpha = 255 } // #749 a pompom caps the gnome's hat
            // ---- main-unit detail pass ----
            val efa = atan2(py - e.y, px - e.x)
            val eHpF = (e.hp / e.maxHp).coerceIn(0f, 1f)
            if (e.type != 5 && e.type != 8 && e.type != 9) { // #411 pulsing inner core, sized by remaining HP
                p.alpha = 210
                poly(c, e.x, e.y, e.radius * (0.20f + 0.16f * eHpF) * (1f + 0.12f * sin(time * 6f + e.wob)), 4, time * 2f, shade(col, 1.55f))
                p.color = Color.WHITE; p.alpha = 150; c.drawCircle(e.x, e.y, dp(1.2f), p); p.alpha = 255 // #750 a white speck hearts every core
            if (e.type == 0) poly(c, e.x, e.y, e.radius * 0.52f, 3, atan2(py - e.y, px - e.x) + 1.57f, shade(col, 1.45f)) // #744 the chaser's inset triangle
            if (e.type == 2) poly(c, e.x, e.y, e.radius * 0.6f, 6, time * 0.8f, shade(col, 0.72f)) // #745 the brute's inset hexagon
            if (e.type == 6) poly(c, e.x, e.y, e.radius * 0.58f, 5, time * 2.2f, shade(col, 1.4f)) // #746 the weaver's inset pentagon
            if (e.type == 10) poly(c, e.x, e.y, e.radius * 0.6f, 6, 0.3f, shade(col, 1.35f)) // #747 the bulwark's inset hexagon
                p.alpha = 255
            }
            // #412 rim light on the player-facing edge
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = shade(col, 1.6f); p.alpha = 130
            c.drawArc(RectF(e.x - e.radius * 0.98f, e.y - e.radius * 0.98f, e.x + e.radius * 0.98f, e.y + e.radius * 0.98f), efa * 57.2958f - 42f, 84f, false, p)
            p.style = Paint.Style.FILL; p.color = Color.WHITE; p.alpha = 130; c.drawCircle(e.x + cos(efa) * e.radius * 0.98f, e.y + sin(efa) * e.radius * 0.98f, dp(1.6f), p); p.alpha = 255 // #751 a hot spot rides the rim light
            p.style = Paint.Style.FILL; p.alpha = 255
            if (e.elite) poly(c, e.x, e.y, e.radius * 0.84f, 6, time * 1.4f, Color.rgb(250, 204, 21), false) // #424 elite gold trim
            when (e.type) {
                0 -> { for (k in 0 until 4) { val ta = efa + (k - 1.5f) * 0.30f; poly(c, e.x + cos(ta) * e.radius * 0.88f, e.y + sin(ta) * e.radius * 0.88f, dp(3.4f), 3, ta + 1.57f, Color.rgb(226, 232, 240)) } // #413 chaser teeth
                    p.color = Color.rgb(254, 202, 202); p.alpha = (90 + 60 * sin(time * 5f + e.wob)).toInt().coerceIn(30, 150); c.drawCircle(e.x + cos(efa) * e.radius * 0.3f, e.y + sin(efa) * e.radius * 0.3f, dp(2f), p); p.alpha = 255 } // #675 the chaser's maw glows
                1 -> { // #414 dart speed ghosts + fins
                    for (k in 1..2) { p.alpha = 70 - k * 22; poly(c, e.x - cos(efa) * e.radius * 0.75f * k, e.y - sin(efa) * e.radius * 0.75f * k, e.radius * (1f - 0.22f * k), 4, time * 5f, col) }
                    p.alpha = 255
                    for (s in -1..1 step 2) poly(c, e.x - cos(efa) * e.radius * 0.7f + cos(efa + 1.57f) * e.radius * 0.55f * s, e.y - sin(efa) * e.radius * 0.7f + sin(efa + 1.57f) * e.radius * 0.55f * s, dp(4.5f), 3, efa - 1.57f + 0.6f * s, shade(col, 1.25f))
                }
                2 -> { // #415 brute armor plates, shed as HP falls
                    val plates = when { eHpF > 0.66f -> 3; eHpF > 0.33f -> 2; else -> 1 }
                    for (i in 0 until plates) {
                        val pa = efa + (i - (plates - 1) / 2f) * 0.8f
                        val pcx = e.x + cos(pa) * e.radius * 0.42f; val pcy = e.y + sin(pa) * e.radius * 0.42f
                        poly(c, pcx, pcy, e.radius * 0.36f, 6, pa, shade(col, 0.72f))
                        p.color = Color.rgb(15, 5, 20); c.drawCircle(pcx, pcy, dp(2f), p)
                        p.color = Color.WHITE; p.alpha = 170; c.drawCircle(pcx - dp(2.5f), pcy - dp(2.5f), dp(1.1f), p); p.alpha = 255 // #433 rivet glint
                    }
                    if (eHpF < 0.3f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 80 * sin(time * 9f)).toInt().coerceIn(40, 200); c.drawCircle(e.x, e.y, e.radius * 0.3f, p); p.alpha = 255 } // #671 a wounded brute's core burns red
                }
                3 -> for (k in 0 until 3) { val ba2 = e.wob * 2f + k * 2.094f; p.color = shade(col, 1.45f); c.drawCircle(e.x + cos(ba2) * e.radius * 0.38f, e.y + sin(ba2) * e.radius * 0.38f, e.radius * 0.21f * (1f + 0.18f * sin(time * 7f + k * 2.1f)), p) } // #416 splitter brood blobs + #434 breathing pulse
                6 -> { // #417 weaver ribbon trail segments
                    for (k in 1..3) {
                        val bx2 = e.x - cos(efa) * e.radius * 0.78f * k
                        val by2 = e.y - sin(efa) * e.radius * 0.78f * k + sin(time * 6f - k * 1.25f) * dp(3.5f)
                        p.alpha = 235 - k * 55
                        poly(c, bx2, by2, e.radius * (1f - 0.17f * k), 5, time * 2.2f - k * 0.5f, shade(col, 1f - 0.13f * k))
                    }
                    p.alpha = 255
                    p.color = Color.WHITE; p.alpha = (120 + 60 * sin(time * 8f)).toInt().coerceIn(60, 180) // #465 ribbon head sparkles
                    c.drawCircle(e.x - cos(efa) * e.radius * 0.78f, e.y - sin(efa) * e.radius * 0.78f, dp(2f), p); p.alpha = 255
                    p.color = shade(col, 1.3f); p.alpha = 120; c.drawCircle(e.x + cos(efa + 1.57f) * e.radius * 0.8f, e.y + sin(efa + 1.57f) * e.radius * 0.8f, dp(1.8f), p); c.drawCircle(e.x - cos(efa + 1.57f) * e.radius * 0.8f, e.y - sin(efa + 1.57f) * e.radius * 0.8f, dp(1.8f), p); p.alpha = 255 // #665 silk anchor beads flank the weaver
                }
                7 -> { // #418 lancer lance rides ahead of the body
                    val glowA = if (e.state == 1) (170 + 80 * sin(time * 30f)).toInt().coerceIn(90, 250) else 150
                    p.style = Paint.Style.STROKE; p.strokeWidth = if (e.state == 1) dp(4.5f) else dp(3f); p.color = shade(col, 1.5f); p.alpha = glowA
                    c.drawLine(e.x + cos(efa) * e.radius * 0.6f, e.y + sin(efa) * e.radius * 0.6f, e.x + cos(efa) * e.radius * 1.65f, e.y + sin(efa) * e.radius * 1.65f, p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                    poly(c, e.x + cos(efa) * e.radius * 1.8f, e.y + sin(efa) * e.radius * 1.8f, dp(5f), 3, efa + 1.57f, shade(col, 1.6f))
                    if (e.state == 1) { p.color = Color.WHITE; p.alpha = (140 + 100 * sin(time * 30f)).toInt().coerceIn(40, 240); c.drawCircle(e.x + cos(efa) * e.radius * 1.8f, e.y + sin(efa) * e.radius * 1.8f, dp(2.2f), p); p.alpha = 255 } // #666 the winding-up lance tip strobes
                }
                8 -> { // #419 orbiter satellites + orbit ring
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); p.color = col; p.alpha = (60 + 25 * sin(time * 3f + e.wob)).toInt().coerceIn(35, 85) // #462 orbit ring breathes
                    c.drawCircle(e.x, e.y, e.radius * 1.5f, p); p.style = Paint.Style.FILL; p.alpha = 255
                    for (k in 0 until 2) { val sa = time * 3.4f + k * 3.14159f
                        for (tr2 in 1..2) { val ta2 = sa - tr2 * 0.22f; p.alpha = 90 - tr2 * 30; poly(c, e.x + cos(ta2) * e.radius * 1.5f, e.y + sin(ta2) * e.radius * 1.5f, dp(4.6f) * (1f - tr2 * 0.25f), 4, ta2 * 2f, shade(col, 1.5f)) } // #435 satellite trails
                        p.alpha = 255
                        poly(c, e.x + cos(sa) * e.radius * 1.5f, e.y + sin(sa) * e.radius * 1.5f, dp(4.6f), 4, sa * 2f, shade(col, 1.5f)) }
                }
                9 -> { // #420 necroshard skull core + summoning runes
                    p.color = Color.rgb(248, 250, 252); c.drawCircle(e.x, e.y, e.radius * 0.30f, p)
                    p.color = Color.rgb(24, 4, 36)
                    c.drawCircle(e.x - e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)
                    c.drawCircle(e.x + e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)
                    if (e.shootT < 0.5f) { // #436 eyes flare red before a summon
                        p.color = Color.rgb(239, 68, 68); p.alpha = (150 + 90 * sin(time * 16f)).toInt().coerceIn(60, 240)
                        c.drawCircle(e.x - e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.045f, p)
                        c.drawCircle(e.x + e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.045f, p); p.alpha = 255
                    }
                    if (e.shootT < 0.5f) {
                        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(240, 171, 252); p.alpha = (140 + 90 * sin(time * 14f)).toInt().coerceIn(50, 230)
                        c.drawCircle(e.x, e.y, e.radius * 1.35f, p)
                        p.strokeWidth = dp(1.2f); c.drawCircle(e.x, e.y, e.radius * (1.35f - 0.18f * (0.5f + 0.5f * sin(time * 2f))), p) // #466 inner ring breathes against the runes
                        for (k in 0 until 4) { val ra = time * 2f + k * 1.571f; poly(c, e.x + cos(ra) * e.radius * 1.35f, e.y + sin(ra) * e.radius * 1.35f, dp(3.4f), 4, ra, Color.rgb(240, 171, 252)) }
                        p.style = Paint.Style.FILL; p.alpha = 255
                    }
                }
                11 -> { // #422 gnome gem-sack + sparkle trail
                    p.color = Color.rgb(146, 96, 44); c.drawCircle(e.x - cos(efa) * e.radius * 0.85f, e.y - sin(efa) * e.radius * 0.85f, e.radius * 0.55f, p)
                    p.color = Color.rgb(250, 204, 21); c.drawCircle(e.x - cos(efa) * e.radius * 0.85f, e.y - sin(efa) * e.radius * 0.85f - e.radius * 0.4f, dp(2.4f), p)
                    if ((time * 2f + e.wob).toInt() % 3 == 0) { p.color = Color.WHITE; c.drawCircle(e.x - cos(efa) * e.radius * 0.85f + dp(2f), e.y - sin(efa) * e.radius * 0.85f - e.radius * 0.4f, dp(1.2f), p) } // #472 loot glints in the sack
                    for (k in 1..2) { p.alpha = (150 - k * 45 + 40 * sin(time * 9f + k * 2f)).toInt().coerceIn(30, 200); poly(c, e.x - cos(efa) * e.radius * (1.1f + 0.5f * k), e.y - sin(efa) * e.radius * (1.1f + 0.5f * k) + dp(3f) * k, dp(3.6f), 4, time * 4f + k, Color.rgb(254, 240, 138)) }
                    p.alpha = 255
                }
            }
            if (e.type == 5) { // #423 warden armor ring, tracking eye, phase cracks
                poly(c, e.x, e.y, e.radius * 1.14f, 8, -time * 0.3f, shade(col, 0.6f), false)
                p.color = Color.rgb(248, 250, 252); c.drawCircle(e.x, e.y, e.radius * 0.17f, p)
                p.color = Color.rgb(24, 4, 36); c.drawCircle(e.x + cos(efa) * e.radius * 0.06f, e.y + sin(efa) * e.radius * 0.06f, e.radius * 0.085f, p)
                if (eHpF < 0.66f) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(20, 4, 10); p.alpha = 210
                    c.drawLine(e.x - e.radius * 0.5f, e.y - e.radius * 0.55f, e.x - e.radius * 0.05f, e.y - e.radius * 0.1f, p)
                    c.drawLine(e.x - e.radius * 0.05f, e.y - e.radius * 0.1f, e.x + e.radius * 0.3f, e.y - e.radius * 0.25f, p)
                    if (eHpF < 0.33f) {
                        p.color = Color.rgb(249, 115, 22); p.alpha = (150 + 80 * sin(time * 9f)).toInt().coerceIn(70, 230) // #468 deep cracks glow molten
                        c.drawLine(e.x + e.radius * 0.45f, e.y + e.radius * 0.4f, e.x + e.radius * 0.05f, e.y + e.radius * 0.15f, p)
                        c.drawLine(e.x + e.radius * 0.05f, e.y + e.radius * 0.15f, e.x - e.radius * 0.35f, e.y + e.radius * 0.45f, p)
                    }
                    p.style = Paint.Style.FILL; p.alpha = 255
                }
            }
            if (e.type == 7 && e.state == 1) { // #114 lancer telegraph line
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(248, 113, 113)
                p.alpha = (120 + 120 * sin(time * 30f)).toInt().coerceIn(30, 240)
                val ta = atan2(py - e.y, px - e.x)
                c.drawLine(e.x, e.y, e.x + cos(ta) * dp(330f), e.y + sin(ta) * dp(330f), p)
                p.style = Paint.Style.FILL // #437 charge chevrons race down the line
                for (cv2 in 1..3) {
                    val cd2 = (cv2 / 4f + (time * 1.4f) % 0.25f) * dp(330f)
                    poly(c, e.x + cos(ta) * cd2, e.y + sin(ta) * cd2, dp(5f), 3, ta + 1.57f, Color.rgb(248, 113, 113))
                }
                p.style = Paint.Style.STROKE
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 10) { // #117 bulwark shield arc faces the player; #421 flashes thick + white on a hit
                p.style = Paint.Style.STROKE; p.strokeWidth = if (e.flash > 0) dp(7f) else dp(4f); p.color = if (e.flash > 0) Color.WHITE else Color.rgb(226, 232, 240); p.alpha = 200
                val fa = Math.toDegrees(atan2(py - e.y, px - e.x).toDouble()).toFloat()
                c.drawArc(RectF(e.x - e.radius * 1.15f, e.y - e.radius * 1.15f, e.x + e.radius * 1.15f, e.y + e.radius * 1.15f), fa - 55f, 110f, false, p)
                p.strokeWidth = dp(1.6f); p.alpha = 90 // #438 inner shield sheen
                c.drawArc(RectF(e.x - e.radius * 0.98f, e.y - e.radius * 0.98f, e.x + e.radius * 0.98f, e.y + e.radius * 0.98f), fa - 55f, 110f, false, p)
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
                    poly(c, e.x + cos(a) * e.radius * 0.7f, e.y + sin(a) * e.radius * 0.7f - e.radius * 0.3f, dp(4.5f) * min(1f, e.frostAge / 0.25f), 3, -1.57f, Color.rgb(191, 219, 254)) // #188 grow-in
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
                p.color = Color.rgb(30, 10, 40); c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p) // #625 enemy HP bars get rounded caps
                if (e.ghostHp > e.hp) { p.color = Color.rgb(226, 232, 240); c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.ghostHp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p) } // #432 recent-damage ghost
                p.color = if (e.elite) Color.rgb(250, 204, 21) else Color.rgb(74, 222, 128) // #510 elite bars run gold
                c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p)
                if (e.elite) { p.color = Color.rgb(250, 204, 21); p.alpha = 230; c.drawCircle(e.x - e.radius - dp(5f), e.y - e.radius - dp(7.5f), dp(2.2f), p); p.alpha = 255 } // #568 a gold bead marks the elite bar
                p.color = Color.WHITE; p.alpha = 200 // #510 bright tip on the fill edge
                c.drawCircle(e.x - e.radius + bw * (e.hp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(7.5f), dp(1.6f), p); p.alpha = 255
            }
            c.restore()
        }

        // projectiles
        for (pr in projs) {
            p.color = pr.color; p.alpha = 70
            c.drawCircle(pr.x, pr.y, pr.radius * (2.1f + 0.25f * sin(time * 14f + pr.x * 0.05f)), p); p.alpha = 255
            if (pr.fromPlayer) { // #439 wide soft halo on your shots
                p.color = pr.color; p.alpha = 26
                c.drawCircle(pr.x, pr.y, pr.radius * 3.4f, p); p.alpha = 255
            }
            if (pr.fromPlayer && pr.pierce > 0) { // #65 pulse lance beam: tail + core layers
                p.style = Paint.Style.STROKE; p.color = pr.color; p.alpha = 150; p.strokeWidth = pr.radius * 1.1f
                c.drawLine(pr.x - pr.vx * 0.045f, pr.y - pr.vy * 0.045f, pr.x, pr.y, p)
                p.color = Color.WHITE; p.alpha = 230; p.strokeWidth = pr.radius * 0.4f
                c.drawLine(pr.x - pr.vx * 0.03f, pr.y - pr.vy * 0.03f, pr.x, pr.y, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (pr.fromPlayer && !pr.rocket) { p.color = pr.color; p.alpha = 60; c.drawCircle(pr.x, pr.y, pr.radius * 1.9f, p); p.alpha = 255 } // #677 your shots wear a soft halo
            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)
            p.color = Color.WHITE
            if (pr.fromPlayer) c.drawCircle(pr.x, pr.y, pr.radius * 0.45f, p)
            else { // #440 hostile orbs throb — readable in a swarm
                p.alpha = (170 + 85 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(85, 255)
                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = pr.color; p.alpha = (60 + 40 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(20, 100); c.drawCircle(pr.x, pr.y, pr.radius * 1.5f, p); p.style = Paint.Style.FILL; p.alpha = 255 // #678 hostile orbs wear a warning ring
            }
            if (pr.rocket) { // 🚀 rocket flame + hot nose
                val ra = atan2(pr.vy, pr.vx)
                for (tr4 in 1..2) { p.alpha = 64 - tr4 * 18; poly(c, pr.x - cos(ra) * pr.radius * (1.9f + tr4 * 1.5f), pr.y - sin(ra) * pr.radius * (1.9f + tr4 * 1.5f), pr.radius * (0.85f - tr4 * 0.18f), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22)) } // #480 exhaust ribbon
                p.alpha = 255
                poly(c, pr.x - cos(ra) * pr.radius * 1.7f, pr.y - sin(ra) * pr.radius * 1.7f, pr.radius * (0.9f + 0.25f * sin(time * 34f + pr.x)), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22))
                p.color = Color.rgb(254, 240, 138); c.drawCircle(pr.x + cos(ra) * pr.radius * 0.5f, pr.y + sin(ra) * pr.radius * 0.5f, pr.radius * 0.42f, p)
                if (((time * 6f + pr.x * 0.02f).toInt() % 2) == 0) { p.color = Color.rgb(239, 68, 68); c.drawCircle(pr.x - cos(ra) * pr.radius * 1.1f, pr.y - sin(ra) * pr.radius * 1.1f, dp(1.8f), p) } // #679 the rocket's tail light blinks
            }
        }

        // orbit blades
        if (save.selectedWeapon == 3 && state != State.GAMEOVER) {
            val bladeR = dp(64f)
            for (k in 0 until 2) {
                val bx = px + cos(orbitAngle + k * 3.14159f) * bladeR
                val by = py + sin(orbitAngle + k * 3.14159f) * bladeR
                for (t3 in 3 downTo 1) { // #236 blade arc ghost trail (previous orbit angles)
                    val ga = orbitAngle + k * 3.14159f - t3 * 0.17f
                    p.alpha = 52 - t3 * 12
                    poly(c, px + cos(ga) * bladeR, py + sin(ga) * bladeR, dp(13f) * (1f - t3 * 0.15f), 3, orbitAngle * 3f + k * 2f - t3 * 0.4f, Color.rgb(52, 211, 153))
                }
                p.alpha = 255
                p.color = Color.rgb(52, 211, 153); p.alpha = 60
                c.drawCircle(bx, by, dp(19f), p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(52, 211, 153); p.alpha = 110
                val ba = orbitAngle + k * 3.14159f
                c.drawArc(RectF(px - bladeR, py - bladeR, px + bladeR, py + bladeR), Math.toDegrees(ba.toDouble()).toFloat() - 50f, 42f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, bx, by, dp(13f), 3, orbitAngle * 3f + k * 2f, Color.rgb(52, 211, 153))
                poly(c, bx, by, dp(5f), 4, time * 6f + k * 2f, Color.rgb(167, 243, 208)) // #680 orbit blades spin a bright heart
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
            p.color = Color.WHITE; p.alpha = (70 * (g.life / 0.34f)).toInt().coerceIn(0, 70); c.drawCircle(g.x, g.y, dp(3f), p) // #681 dash ghosts burn bright at the center
        }
        p.alpha = 255
        for (sg in shardGhosts) { // #161 death shard: shrink + fade
            val f = (sg.life / 0.45f).coerceIn(0f, 1f)
            p.alpha = (150 * f).toInt()
            poly(c, sg.x, sg.y, sg.r * (0.4f + 0.6f * f), sg.sides, time * 3f, sg.color)
            p.alpha = 255
        }
        // shockwave rings
        for (r in rings) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f) * (r.life / r.maxLife) + 0.5f
            p.color = r.color; p.alpha = (200 * (r.life / r.maxLife)).toInt().coerceIn(0, 200)
            c.drawCircle(r.x, r.y, r.r, p)
            p.style = Paint.Style.FILL; p.color = r.color; p.alpha = (16 * (r.life / r.maxLife)).toInt().coerceIn(0, 16); c.drawCircle(r.x, r.y, r.r, p) // #602 rings carry a whisper of fill
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
                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(244, 114, 182); p.alpha = (90 + 70 * sin(time * 6f)).toInt().coerceIn(20, 160); c.drawCircle(ax, ay, dp(17f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #596 the Warden's off-screen arrow wears a halo
                if (e.type == 11) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (80 + 60 * sin(time * 5f)).toInt().coerceIn(20, 140); c.drawCircle(ax, ay, dp(13f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #606 the Gnome's arrow wears a gold halo — follow the money
                arrowsDrawn++
            }
        }

        // player — gem guardian diamond
        if (state != State.GAMEOVER) {
            val chCol = CHARS[save.selectedChar].color
            val blink = invulnT > 0 && (time * 18f).toInt() % 2 == 0
            val spawnSc = if (spawnAnimT > 0) 0.25f + 0.75f * (1f - spawnAnimT / 0.55f) else 1f // #159 materialize scale-in
            if (combo >= 25) for (k in 0 until 6) { // kill-streak fire aura — orbiting orbs now, NOT triangles (Nathan's call, v2.3)
                val fa = k * 1.047f + time * 1.8f; val fh = dp(13f) + dp(6f) * sin(time * 13f + k * 2.1f)
                p.color = if (k % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21); p.alpha = 200
                c.drawCircle(px + cos(fa) * dp(31f), py + sin(fa) * dp(31f) - fh * 0.4f, fh * 0.42f, p); p.alpha = 255
                p.color = if (k % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21); p.alpha = 60 // #610 aura orbs trail a fading echo
                c.drawCircle(px + cos(fa - 0.35f) * dp(31f), py + sin(fa - 0.35f) * dp(31f) - fh * 0.4f, fh * 0.3f, p); p.alpha = 255
            }
            val recOff = if (recoilT > 0) dp(3.5f) * (recoilT / 0.09f) else 0f // #184 muzzle recoil
            val bpx = px - cos(lastAim) * recOff; val bpy = py - sin(lastAim) * recOff + sin(time * 2.4f) * dp(1.6f) // #406 hover bob
            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (110 + 80 * sin(time * 8f)).toInt().coerceIn(30, 190); c.drawCircle(bpx, bpy, dp(38f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #621 a level-up is about to pop
            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = CHARS[save.selectedChar].color; p.alpha = 70; c.drawCircle(bpx, bpy, dp(31f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #740 the level-up ring echoes inside itself
            p.color = chCol; p.alpha = (55 * spawnSc).toInt()
            c.drawCircle(bpx, bpy, dp(34f) * spawnSc, p); p.alpha = 255
            if (spawnAnimT > 0) { // #456 materialize shockring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol
                p.alpha = (200 * (spawnAnimT / 0.55f)).toInt().coerceIn(0, 200)
                c.drawCircle(px, py, dp(52f) * (1f - spawnAnimT / 0.55f) + dp(10f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            // #407 soft ground shadow under the hero
            p.color = Color.BLACK; p.alpha = 95
            c.drawOval(RectF(px - dp(17f), py + dp(19f), px + dp(17f), py + dp(28f)), p); p.alpha = 255
            if (!blink) { // 🐢 TURTLE WITH JETS — every hero is a turtle (v2.3.1), shell tinted by hero color
                val ta = lastAim
                val spdF = 0.75f + 0.5f * min(1f, hyp(pvx, pvy) / 300f)
                for (s in -1..1 step 2) { // rear jet pods + flames (flames swell with speed)
                    val jx = bpx - cos(ta) * dp(17f) + cos(ta + 1.57f) * dp(10f) * s
                    val jy = bpy - sin(ta) * dp(17f) + sin(ta + 1.57f) * dp(10f) * s
                    val fl = (dp(8f) + dp(3f) * sin(time * 29f + s * 2f)) * spdF
                    poly(c, jx - cos(ta) * fl * 0.9f, jy - sin(ta) * fl * 0.9f, fl * 0.62f, 4, ta + 0.78f + 3.14159f, Color.rgb(249, 115, 22))
                    poly(c, jx - cos(ta) * fl * 0.5f, jy - sin(ta) * fl * 0.5f, fl * 0.34f, 4, ta + 0.78f + 3.14159f, Color.rgb(254, 240, 138))
                    p.color = Color.rgb(71, 85, 105); c.drawCircle(jx, jy, dp(5.5f), p)
                    p.color = Color.rgb(148, 163, 184); c.drawCircle(jx, jy, dp(3f), p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(203, 213, 225); p.alpha = 190 // #477 metal band on the pod
                    c.drawCircle(jx, jy, dp(5.5f), p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                    p.color = Color.rgb(249, 115, 22); p.alpha = (140 + 90 * sin(time * 26f + s)).toInt().coerceIn(50, 230) // #477 nozzle glow
                    c.drawCircle(jx - cos(ta) * dp(6.5f), jy - sin(ta) * dp(6.5f), dp(2.4f), p); p.alpha = 255
                }
                for (fx2 in -1..1 step 2) for (fy2 in -1..1 step 2) { // flippers, paddle wiggle
                    val wig = sin(time * 6f + (fx2 + fy2) * 1.7f) * dp(1.6f)
                    p.color = shade(chCol, 0.75f)
                    c.drawOval(RectF(bpx + fx2 * dp(15f) - dp(6f) + wig, bpy + fy2 * dp(13f) - dp(3.5f), bpx + fx2 * dp(15f) + dp(6f) + wig, bpy + fy2 * dp(13f) + dp(3.5f)), p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.1f); p.color = shade(chCol, 0.5f); p.alpha = 150 // #478 flipper seam
                    c.drawLine(bpx + fx2 * dp(15f) - dp(3.6f) + wig, bpy + fy2 * dp(13f), bpx + fx2 * dp(15f) + dp(3.6f) + wig, bpy + fy2 * dp(13f), p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                }
                // shell: dark rim, green dome, drifting plate arcs
                p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                (0 until 8).forEach { k -> val sa3 = k * 0.785f + time * 0.6f; p.color = Color.WHITE; p.alpha = 100; c.drawCircle(bpx + cos(sa3) * dp(21f) * spawnSc, bpy + sin(sa3) * dp(21f) * spawnSc, dp(0.9f), p) }; p.alpha = 255 // #738 glints ride the shell rim
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = shade(chCol, 0.28f); p.alpha = 210 // #475 crisp shell outline
                c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255
                p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.1f); p.color = shade(chCol, 1.3f); p.alpha = 80; c.drawCircle(bpx, bpy, dp(14.2f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #737 an inner ring gives the dome depth
                // #473 shell scute plates: seams from a center plate, plus #475 rim studs
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.3f); p.color = shade(chCol, 0.55f); p.alpha = 170
                for (k in 0 until 6) { val sa3 = k * 1.047f + 0.52f; c.drawLine(bpx + cos(sa3) * dp(8.5f), bpy + sin(sa3) * dp(8.5f), bpx + cos(sa3) * dp(15.8f), bpy + sin(sa3) * dp(15.8f), p) }
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, bpx, bpy, dp(8.5f) * spawnSc, 6, 0.52f, shade(chCol, 0.85f))
                p.color = shade(chCol, 1.35f)
                for (k in 0 until 8) { val sa4 = time * 0.6f + k * 0.785f; c.drawCircle(bpx + cos(sa4) * dp(19.2f), bpy + sin(sa4) * dp(19.2f), dp(1.3f), p) }
                // #474 dome gloss
                p.color = Color.WHITE; p.alpha = 46
                c.drawOval(RectF(bpx - dp(11f), bpy - dp(12.5f), bpx - dp(1f), bpy - dp(6.5f)), p); p.alpha = 255
                // #504 shine sweep circles the shell gem
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.WHITE; p.alpha = 110
                c.drawArc(RectF(bpx - dp(10.5f), bpy - dp(10.5f), bpx + dp(10.5f), bpy + dp(10.5f)), time * 150f, 65f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                if (dashCharges < dashMaxCharges) { // #519 dash recharge arc rings the shell
                    val dprog = 1f - (dashRechargeT / dashCdMax).coerceIn(0f, 1f)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(34, 211, 238); p.alpha = 170
                    c.drawArc(RectF(bpx - dp(24.5f), bpy - dp(24.5f), bpx + dp(24.5f), bpy + dp(24.5f)), -90f, 360f * dprog, false, p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                }
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = shade(chCol, 1.45f); p.alpha = 170
                c.drawArc(RectF(bpx - dp(12f), bpy - dp(12f), bpx + dp(12f), bpy + dp(12f)), time * 40f, 110f, false, p)
                c.drawArc(RectF(bpx - dp(12f), bpy - dp(12f), bpx + dp(12f), bpy + dp(12f)), time * 40f + 180f, 110f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                // 💎 the gem on the shell
                poly(c, bpx, bpy, dp(8.6f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, shade(chCol, 0.4f))
                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, if (waveClearT > 0) Color.rgb((127 + 127 * sin(time * 9f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 2.1f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 4.2f)).toInt().coerceIn(0, 255)) else Color.rgb(34, 211, 238)) // #494 victory rainbow gem
                if (wave % 5 == 0) poly(c, bpx, bpy, dp(3.6f) * spawnSc, 4, -time * 3f, Color.rgb(250, 204, 21)) // #736 a gold heart spins in the shell gem on boss waves
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = Color.WHITE; p.alpha = (50 + 30 * sin(time * 5f)).toInt().coerceIn(20, 80); c.drawCircle(bpx, bpy, dp(10.5f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #739 a halo rings the shell gem
                for (sj in -1..1 step 2) { p.color = Color.rgb(254, 240, 138); p.alpha = (120 + 80 * sin(time * 9f + sj)).toInt().coerceIn(40, 200); c.drawCircle(bpx + sj * dp(13f) * spawnSc, bpy + dp(17f) * spawnSc, dp(1.5f), p) }; p.alpha = 255 // #741 sparks spit from the jet nozzles
                poly(c, bpx, bpy, dp(3.4f) * spawnSc, 4, 0.78f, Color.rgb(236, 254, 255))
                // head + eyes, leading the aim
                val hx2 = bpx + cos(ta) * dp(20f); val hy2 = bpy + sin(ta) * dp(20f)
                p.color = shade(chCol, 0.75f); c.drawCircle(hx2, hy2, dp(8.6f) * spawnSc, p)
                p.color = shade(chCol, 1.12f); c.drawCircle(hx2, hy2, dp(7.4f) * spawnSc, p)
                for (s in -1..1 step 2) {
                    val ex = hx2 + cos(ta) * dp(3.4f) + cos(ta + 1.57f) * dp(3.6f) * s
                    val ey = hy2 + sin(ta) * dp(3.4f) + sin(ta + 1.57f) * dp(3.6f) * s
                    if ((time % 4.3f) < 0.11f) { // #479 every turtle blinks
                        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.3f); p.color = Color.rgb(8, 2, 18)
                        c.drawLine(ex - dp(1.7f), ey, ex + dp(1.7f), ey, p); p.style = Paint.Style.FILL
                    } else {
                        p.color = Color.rgb(8, 2, 18); c.drawCircle(ex, ey, dp(1.9f), p)
                        p.color = Color.WHITE; c.drawCircle(ex + dp(0.6f), ey - dp(0.6f), dp(0.8f), p)
                    }
                }
                // #476 nostrils + smile + cheek
                p.color = Color.rgb(8, 2, 18)
                for (s in -1..1 step 2) c.drawCircle(hx2 + cos(ta) * dp(6.8f) + cos(ta + 1.57f) * dp(1.9f) * s, hy2 + sin(ta) * dp(6.8f) + sin(ta + 1.57f) * dp(1.9f) * s, dp(0.75f), p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f)
                c.drawArc(RectF(hx2 - dp(4.4f), hy2 - dp(4.4f), hx2 + dp(4.4f), hy2 + dp(4.4f)), ta * 57.2958f - 55f, 110f, false, p)
                p.style = Paint.Style.FILL
                p.color = Color.rgb(244, 114, 182); p.alpha = 55
                c.drawCircle(hx2 - cos(ta + 1.57f) * dp(5.2f) - cos(ta) * dp(1.5f), hy2 - sin(ta + 1.57f) * dp(5.2f) - sin(ta) * dp(1.5f), dp(2.3f), p); p.alpha = 255
            }
            if (!blink && save.selectedChar == -1) { // #401 RETIRED v2.3.1 — every hero is a turtle now
                val prot = sin(time * 2.2f) * 0.14f + 0.78f
                poly(c, bpx, bpy, dp(24.5f) * spawnSc, 4, prot, shade(chCol, 0.5f))
                poly(c, bpx, bpy, dp(24.5f) * spawnSc, 4, prot, shade(chCol, 1.35f), false)
            }
            if (!blink && save.selectedChar == -1) { // gem-ship body RETIRED v2.3.1
                val rot = sin(time * 2.2f) * 0.14f
                if (dashT > 0) { // #164 stretch along the dash vector
                    c.save(); c.translate(bpx, bpy)
                    c.rotate(Math.toDegrees(atan2(dashDy, dashDx).toDouble()).toFloat() + 45f)
                    c.scale(1.38f, 0.74f)
                    poly(c, 0f, 0f, dp(21f) * spawnSc, 4, 0.78f, chCol)
                    poly(c, 0f, 0f, dp(12f) * spawnSc, 4, 0.78f, Color.rgb(236, 254, 255))
                    poly(c, 0f, 0f, dp(5.5f) * spawnSc, 4, 0.78f, chCol)
                    c.restore()
                } else {
                    poly(c, bpx, bpy, dp(21f) * spawnSc, 4, rot + 0.78f, chCol)
                    poly(c, bpx, bpy, dp(12f) * spawnSc, 4, rot + 0.78f, Color.rgb(236, 254, 255))
                    val coreCol = when { hp > maxHp * 0.5f -> chCol; hp > maxHp * 0.25f -> Color.rgb(250, 204, 21); else -> Color.rgb(239, 68, 68) } // #402 core reads your HP
                    val corePulse = 1f + 0.18f * sin(time * (if (hp > maxHp * 0.25f) 5f else 11f))
                    poly(c, bpx, bpy, dp(5.5f) * spawnSc * corePulse, 4, rot + 0.78f, coreCol)
                }
            }
            if (save.selectedChar == -1) { // #403 visor RETIRED v2.3.1 — turtles have real eyes
                val visX = bpx + cos(lastAim) * dp(12.5f); val visY = bpy + sin(lastAim) * dp(12.5f)
                p.color = Color.rgb(8, 2, 18); c.drawCircle(visX, visY, dp(4.6f), p)
                p.color = Color.WHITE; c.drawCircle(visX + cos(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), visY + sin(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), dp(1.7f), p) // #431 living glint
                poly(c, bpx + cos(lastAim) * dp(22.5f), bpy + sin(lastAim) * dp(22.5f), dp(4.2f), 4, lastAim + 0.78f, shade(chCol, 0.75f)) // #408 barrel nub
            }
            // #404 thruster flame opposite the move vector
            val mvx2 = joyDx + padDx + (if (keyRight) 1f else 0f) - (if (keyLeft) 1f else 0f)
            val mvy2 = joyDy + padDy + (if (keyDown) 1f else 0f) - (if (keyUp) 1f else 0f)
            if (hyp(mvx2, mvy2) > 0.25f && save.selectedChar == -1) { // RETIRED v2.3.1 — flames live in the jet pods
                val ma = atan2(mvy2, mvx2); val flick = (dp(5.5f) + dp(2.5f) * sin(time * 31f)) * (0.8f + 0.45f * min(1f, hyp(pvx, pvy) / 320f)) // #429 flame grows with speed
                poly(c, bpx - cos(ma) * dp(25f), bpy - sin(ma) * dp(25f), flick, 3, ma - 1.57f, Color.rgb(249, 115, 22))
                poly(c, bpx - cos(ma) * dp(23f), bpy - sin(ma) * dp(23f), flick * 0.55f, 3, ma - 1.57f, Color.rgb(254, 240, 138))
            }
            // #405 level evolution: shoulder shards (5+), orbit shards (10+), halo (15+)
            if (level >= 5) for (s in -1..1 step 2) { val pa = lastAim + 1.57f; poly(c, bpx + cos(pa) * dp(25f) * s, bpy + sin(pa) * dp(25f) * s, dp(5f), 4, time * 2f, shade(chCol, 1.35f)) }
            if (level >= 10) for (k in 0 until 3) { val oa = time * 2.3f + k * 2.094f; poly(c, bpx + cos(oa) * dp(31f), bpy + sin(oa) * dp(31f), dp(4.4f), 4, oa, shade(chCol, 1.5f)) }
            if (level >= 15) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 150
                c.drawCircle(bpx, bpy, dp(33f) + dp(1.5f) * sin(time * 3f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 4).forEach { k -> val ha2 = k * 1.5708f + time * 1.1f; poly(c, bpx + cos(ha2) * (dp(33f) + dp(1.5f) * sin(time * 3f)), bpy + sin(ha2) * (dp(33f) + dp(1.5f) * sin(time * 3f)), dp(2.6f), 4, ha2, Color.rgb(250, 204, 21)) } // #682 gem studs ride the evolution halo
            }
            // #410 signature detail per hero
            when (save.selectedChar) {
                0 -> { poly(c, bpx, bpy - dp(13f), dp(4.5f), 4, 0.78f, Color.rgb(254, 240, 138), false) // Guardian crest
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(254, 240, 138); p.alpha = (60 + 40 * sin(time * 3.4f)).toInt().coerceIn(20, 100); c.drawCircle(bpx, bpy - dp(13f), dp(8f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #555 the crest wears a halo
                1 -> { if ((pvx * pvx + pvy * pvy) > 9000f) { // #557 Swift leaves speed streaks at pace
                    val spd = hyp(pvx, pvy); val ux = pvx / spd; val uy = pvy / spd
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 120
                    for (k in -1..1) { val ox2 = -uy * k * dp(9f); val oy2 = ux * k * dp(9f); val sx2 = bpx - ux * dp(24f) + ox2; val sy2 = bpy - uy * dp(24f) + oy2; c.drawLine(sx2, sy2, sx2 - ux * dp(13f + 4f * sin(time * 17f + k)), sy2 - uy * dp(13f + 4f * sin(time * 17f + k)), p) }
                    p.style = Paint.Style.FILL; p.alpha = 255 } }
                2 -> { p.color = Color.rgb(20, 8, 4); for (rx in -1..1 step 2) for (ry in -1..1 step 2) c.drawCircle(bpx + rx * dp(13f), bpy + ry * dp(13f), dp(1.9f), p)
                    p.color = Color.rgb(251, 146, 60); p.alpha = (50 + 35 * sin(time * 2.8f)).toInt().coerceIn(15, 85); for (rx in -1..1 step 2) for (ry in -1..1 step 2) c.drawCircle(bpx + rx * dp(13f), bpy + ry * dp(13f), dp(0.9f), p); p.alpha = 255 } // Bulwark rivets, #556 with a warm core glow
                3 -> { poly(c, bpx, bpy - dp(24f) - dp(2f) * sin(time * 12f), dp(6.5f), 3, -1.57f + 0.2f * sin(time * 9f), Color.rgb(249, 115, 22)) // Pyro flame crest
                    for (k in 0 until 2) { val ea2 = time * 1.7f + k * 3.1f; p.color = Color.rgb(251, 146, 60); p.alpha = 150; c.drawCircle(bpx + cos(ea2) * dp(19f), bpy - dp(6f) + sin(ea2 * 1.2f) * dp(12f), dp(1.9f), p) }; p.alpha = 255 } // #554 ember motes orbit Pyro
                4 -> { // Cryo snowflake spokes
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(219, 234, 254)
                    for (k in 0 until 3) { val sa = time * 1.5f + k * 1.047f; c.drawLine(bpx - cos(sa) * dp(8f), bpy - sin(sa) * dp(8f), bpx + cos(sa) * dp(8f), bpy + sin(sa) * dp(8f), p) }
                    p.style = Paint.Style.FILL
                    for (k in 0 until 2) { val fa = time * 0.9f + k * 2.4f; poly(c, bpx + cos(fa) * dp(20f), bpy + sin(fa * 1.3f) * dp(16f), dp(2.2f), 4, fa, Color.rgb(219, 234, 254)) } // #553 snow motes drift around Cryo
                }
                5 -> poly(c, bpx, bpy, dp(3.4f), 4, time * 1.8f, Color.WHITE) // #558 the 💎N🎱 gem throws a rotating glint
            }
            // #409 hurt flash washes the body right after a hit
            if (hurtFlash > 0.45f) {
                p.alpha = ((hurtFlash - 0.45f) * 380f).toInt().coerceIn(0, 200)
                c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p) // v2.3.1: hurt flash is a shell flash now
                p.alpha = 255
            }
            // #54 rotating facet shine
            val shineA = time * 2.6f
            if (save.selectedChar == -1) poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE) // RETIRED v2.3.1 — the shell gem has its own glint
            if (invulnT > 0 && dashT <= 0) { // #69 i-frame shield ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252)
                p.alpha = (120 + 80 * sin(time * 16f)).toInt().coerceIn(40, 200)
                c.drawCircle(px, py, dp(27f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (shieldHp > 0f && shieldLvl > 0) { // #112 shield bubble (arc = charge left)
                val regenActive = shieldHp < shieldLvl * 20f && runTime - lastHurtAt > 3f // #193 regrow shimmer
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(147, 197, 253)
                p.alpha = if (regenActive) (150 + 85 * sin(time * 7f)).toInt().coerceIn(65, 235) else 170
                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)
                if (lastHurtT > 0.45f) { p.strokeWidth = dp(4f); p.color = Color.WHITE; p.alpha = (160 * (lastHurtT / 0.9f)).toInt().coerceIn(0, 160); c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), lastHurtAngle * 57.2958f - 20f, 40f, false, p); p.strokeWidth = dp(2.5f); p.color = Color.rgb(147, 197, 253) } // #688 the shield flashes where the hit landed
                p.strokeWidth = dp(1.4f); p.alpha = 90 // #535 counter-rotating dashed shell
                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)
                p.style = Paint.Style.FILL; poly(c, px + cos(-time * 0.9f) * dp(30f), py + sin(-time * 0.9f) * dp(30f), dp(3f), 4, time * 2f, Color.rgb(219, 234, 254)); p.style = Paint.Style.STROKE // #649 a glint rides the shield rim
                if (regenActive) { p.style = Paint.Style.FILL; p.color = Color.rgb(147, 197, 253); p.alpha = (16 + 10 * sin(time * 7f)).toInt().coerceIn(6, 26); c.drawCircle(px, py, dp(30f), p) } // #611 a regrowing shield fills with light
                p.style = Paint.Style.FILL; p.alpha = 255
                if (regenActive) for (k in 0 until 3) { val sa = time * 2.6f + k * 2.094f; poly(c, px + cos(sa) * dp(30f), py + sin(sa) * dp(30f), dp(3.4f), 4, sa, Color.rgb(191, 219, 254)) }
            }
            if (lastHurtT > 0) { // #244 damage direction indicator arc
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(239, 68, 68); p.alpha = (200 * (lastHurtT / 0.9f)).toInt().coerceIn(0, 200)
                val arcR = dp(46f)
                c.drawArc(RectF(px - arcR, py - arcR, px + arcR, py + arcR), lastHurtAngle * 57.2958f - 26f, 52f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, px + cos(lastHurtAngle) * arcR, py + sin(lastHurtAngle) * arcR, dp(4f), 4, time * 3f, Color.rgb(254, 202, 202)) // #651 the hurt arc points with a hot head
            }
            if (save.selectedChar == 4) { // #128 Cryo aura ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(147, 197, 253); p.alpha = 70
                c.drawCircle(px, py, dp(130f) * (1f + 0.02f * sin(time * 2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 5).forEach { k -> val sa5 = time * 0.7f + k * 2.4f; p.color = Color.WHITE; p.alpha = (40 + 30 * sin(time * 2f + k * 1.9f)).toInt().coerceIn(10, 70); c.drawCircle(px + cos(sa5) * dp(96f), py + sin(sa5 * 1.3f) * dp(96f), dp(1.7f), p) }; p.alpha = 255 // #683 snow sparkles drift inside the Cryo aura
            }
            if (stillT > 0.15f) { // #137 focus charge ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f)
                p.color = if (focusReady) Color.rgb(254, 240, 138) else Color.rgb(165, 243, 252)
                p.alpha = if (focusReady) 220 else 120
                c.drawArc(RectF(px - dp(24f), py - dp(24f), px + dp(24f), py + dp(24f)), -90f, 360f * min(1f, stillT / 1.2f), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                if (focusReady) for (k in 0 until 2) { val fa9 = time * 3.4f + k * 3.1416f; poly(c, px + cos(fa9) * dp(24f), py + sin(fa9) * dp(24f), dp(2.6f), 4, fa9, Color.rgb(254, 240, 138)) } // #650 a charged focus sparkles
            }
            if (muzzleT > 0) { // muzzle flash star at the barrel
                val mx = px + cos(lastAim) * dp(26f); val my = py + sin(lastAim) * dp(26f)
                poly(c, mx, my, dp(9f), 4, time * 22f, Color.rgb(254, 240, 138))
                poly(c, mx, my, dp(4f), 4, -time * 30f, Color.WHITE) // #627 the flash burns white at the core
            }
            if (dashT > 0) { // dash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol; p.alpha = 160
                c.drawCircle(px, py, dp(30f) + (0.16f - dashT) * dp(120f), p)
                p.strokeWidth = dp(1.4f); p.alpha = 90; c.drawCircle(px, py, (dp(30f) + (0.16f - dashT) * dp(120f)) * 0.72f, p) // #623 the dash ring echoes inside itself
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            // #34 level badge
            p.color = Color.rgb(124, 58, 237)
            c.drawCircle(px + dp(17f), py + dp(15f), dp(9f), p)
            if (level >= 10) { // #430 gold ring for double-digit captains
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                c.drawCircle(px + dp(17f), py + dp(15f), dp(10f), p); p.style = Paint.Style.FILL
            }
            if (xpNext > 0 && xp.toFloat() / xpNext > 0.9f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 100 * sin(time * 7f)).toInt().coerceIn(20, 220); c.drawCircle(px + dp(17f), py + dp(15f), dp(11.5f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #593 your level badge burns gold near a level-up
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)
        }

        // #102/#86 meteors: shadow telegraph + falling streak
        for (m in meteors) {
            val frac = (m.t / 0.8f).coerceIn(0f, 1f)
            p.color = Color.BLACK; p.alpha = (90 + 80 * (1f - frac)).toInt() // #190 shadow grows as it falls
            c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p)
            if (frac < 0.5f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(249, 115, 22); p.alpha = (90 * (1f - frac * 2f)).toInt().coerceIn(0, 90); c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #646 the shadow wears a hot rim while it's high
            if (frac < 0.35f) { // #516 final-approach strobe ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(239, 68, 68)
                p.alpha = if (((time * 9f).toInt() % 2) == 0) 220 else 60
                c.drawCircle(m.x, m.y, dp(30f), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(249, 115, 22); p.alpha = 130 // #599 an inner ring collapses onto the impact point
            c.drawCircle(m.x, m.y, dp(8f) + dp(40f) * frac, p); p.style = Paint.Style.FILL; p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(249, 115, 22); p.alpha = 200
            c.drawCircle(m.x, m.y, dp(72f) * (1f - frac) + dp(8f), p)
            for (tk in 0 until 4) { // #441 rotating crosshair ticks
            poly(c, m.x, m.y, dp(5f), 4, -time * 3f, Color.rgb(249, 115, 22)) // #762 an inner diamond counter-turns in the crosshair
                val tka = time * 2.4f + tk * 1.571f; val tkr = dp(72f) * (1f - frac) + dp(8f)
                c.drawLine(m.x + cos(tka) * (tkr - dp(5f)), m.y + sin(tka) * (tkr - dp(5f)), m.x + cos(tka) * (tkr + dp(5f)), m.y + sin(tka) * (tkr + dp(5f)), p)
            }
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(254, 202, 202); p.alpha = (120 + 100 * sin(time * 10f)).toInt().coerceIn(20, 220); c.drawCircle(m.x, m.y, dp(2.4f), p); p.alpha = 255 // #645 the impact point itself pulses
            if (m.t < 0.4f) {
                val off = dp(60f) * (m.t / 0.4f); val my2 = m.y - dp(300f) * (m.t / 0.4f)
                p.color = Color.rgb(249, 115, 22); p.alpha = 90
                c.drawCircle(m.x + off, my2, dp(20f), p)
                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255
                for (k in 0 until 2) { // #529 twin flame wisps orbit the falling head
                    val wa = time * 11f + k * 3.14159f
                    p.color = Color.rgb(249, 115, 22); p.alpha = 130
                    c.drawCircle(m.x + off + cos(wa) * dp(13f), my2 + sin(wa) * dp(13f), dp(4.5f), p)
                }
                p.alpha = 255
                p.color = Color.rgb(148, 163, 184); p.alpha = 60 // #489 smoke puffs trail the falling head
                c.drawCircle(m.x + off + dp(16f), my2 - dp(52f), dp(13f), p)
                p.alpha = 40
                c.drawCircle(m.x + off + dp(24f), my2 - dp(96f), dp(17f), p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(249, 115, 22); p.alpha = 170 // #190 fall streaks
                c.drawLine(m.x + off, my2 - dp(14f), m.x + off + dp(26f), my2 - dp(120f), p)
                p.strokeWidth = dp(1.6f); p.alpha = 110
                c.drawLine(m.x + off - dp(12f), my2 - dp(8f), m.x + off + dp(8f), my2 - dp(84f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
        }

        // particles & texts
        for (pt in particles) {
            p.color = pt.color; p.alpha = (255 * (pt.life / pt.maxLife)).toInt().coerceIn(0, 255)
            if (pt.shape == 1) poly(c, pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, 3, pt.rot + pt.life * 9f, pt.color)
            else { c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, p); if (pt.life / pt.maxLife > 0.7f) { p.color = Color.WHITE; p.alpha = 120; c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) * 0.4f + 0.3f, p) } } // #624 young particles burn bright at the core
        }
        p.alpha = 255
        // crit/elite sparks (4-point stars)
        for (s in sparks) {
            p.color = s.color; p.alpha = 60; c.drawCircle(s.x - dp(2.5f), s.y - dp(2.5f), s.size * 0.45f, p); p.alpha = 255 // #767 every spark trails a fading twin
            val sr = s.size * (s.life / 0.3f)
            poly(c, s.x, s.y, sr, 4, time * 9f, s.color)
            poly(c, s.x, s.y, sr * 0.45f, 4, time * 9f + 0.78f, Color.WHITE)
            if (s.life > 0.22f) { p.color = Color.WHITE; p.alpha = 160; c.drawCircle(s.x, s.y, dp(1.2f), p); p.alpha = 255 } // #604 young sparks flash a white core
        }
        // lightning bolts (#84 renderer)
        for (b in bolts) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(7f); p.color = Color.rgb(253, 224, 71); p.alpha = (46 * (b.life / 0.14f)).toInt().coerceIn(0, 46) // #515 soft glow under the bolt
            c.drawLine(b.x1, b.y1, b.x2, b.y2, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(253, 224, 71)
            val flick = if (b.life > 0.05f && ((time * 47f + b.x1 * 0.01f).toInt() % 2 == 0)) 0.3f else 1f // #189 flicker
            p.alpha = (230 * (b.life / 0.14f) * flick).toInt().coerceIn(0, 230)
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
            p.color = Color.WHITE; p.alpha = (150 * (b.life / 0.14f)).toInt().coerceIn(0, 150); c.drawCircle(b.x2, b.y2, dp(2.2f), p) // #641 bolts strike a white-hot point
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // #73 vignette corners
        p.color = Color.BLACK; p.alpha = (26 + 8 * sin(time * 0.9f)).toInt().coerceIn(18, 34) // #701 the vignette breathes
        c.drawCircle(0f, 0f, dp(120f), p); c.drawCircle(w, 0f, dp(120f), p)
        c.drawCircle(0f, h, dp(120f), p); c.drawCircle(w, h, dp(120f), p)
        p.alpha = 255
        for (t in texts) {
            val ta = (255 * min(1f, t.life * 2)).toInt().coerceIn(0, 255)
            if (t.spin) { c.save(); c.translate(t.x, t.y); c.rotate(-32f * (t.life / t.maxLife)); c.translate(-t.x, -t.y) } // #173 spin-in
            txt(c, t.text, t.x - dp(1.5f), t.y, t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x + dp(1.5f), t.y, t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y - dp(1.5f), t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y + dp(1.5f), t.size, Color.rgb(5, 2, 12), true, ta)
            txt(c, t.text, t.x, t.y, t.size, t.color, true, ta)
            if (t.spin) c.restore()
        }
        if (hp > 0 && hp / maxHp < 0.25f && state == State.PLAYING) { // #76 low-HP desat wash
            p.color = Color.rgb(100, 116, 139); p.alpha = 34
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }
        c.restore()

        // hurt vignette (#227 stronger: hotter fill + thicker edge pulse)
        if (hurtFlash > 0) {
            p.color = Color.rgb(220, 38, 38); p.alpha = (115 * hurtFlash).toInt().coerceIn(0, 115)
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(16f) * hurtFlash + dp(2f)
            p.alpha = (215 * hurtFlash).toInt().coerceIn(0, 215)
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.WHITE; p.alpha = (90 * hurtFlash).toInt().coerceIn(0, 90); p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); c.drawRect(dp(9f), dp(9f), w - dp(9f), h - dp(9f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #702 a white-hot seam lines the hurt flash
        }
        // #5 + #200 low-HP heartbeat border — full strength, lub-dub double beat
        if (state == State.PLAYING && hp > 0 && hp / maxHp < 0.25f) {
            val s1 = max(0f, sin(time * 6.2f)); val s2 = max(0f, sin(time * 6.2f - 0.6f))
            val beat = s1 * s1 + 0.6f * s2 * s2
            val pulseA = (110 + 120 * min(1f, beat)).toInt().coerceIn(40, 230)
            p.color = Color.rgb(239, 68, 68); p.alpha = pulseA
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(8f)
            c.drawRect(dp(3f), dp(3f), w - dp(3f), h - dp(3f), p)
            p.alpha = (pulseA * 0.65f).toInt(); p.strokeWidth = dp(2.5f)
            c.drawRect(dp(13f), dp(13f), w - dp(13f), h - dp(13f), p)
            p.style = Paint.Style.FILL; (0 until 4).forEach { k -> val cxk = if (k % 2 == 0) dp(13f) else w - dp(13f); val cyk = if (k < 2) dp(13f) else h - dp(13f); poly(c, cxk, cyk, dp(5f) * (1f + 0.3f * min(1f, beat)), 4, time * 3f + k * 1.57f, Color.rgb(239, 68, 68)) } // #712 heartbeat diamonds stud the danger frame
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        // #39 last-stand tint
        if (state == State.PLAYING && hp > 0 && hp / maxHp < 0.10f) {
            p.color = Color.rgb(30, 27, 75); p.alpha = (40 + 25 * sin(time * 9f)).toInt().coerceIn(15, 65)
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(248, 113, 113) // #459 heartbeat edge pulse
            p.alpha = (60 + 55 * sin(time * 7.4f)).toInt().coerceIn(5, 115)
            c.drawRect(dp(4f), dp(4f), w - dp(4f), h - dp(4f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)
            (0 until 2).forEach { k -> val sx = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx, h * 0.16f - dp(5f), dp(5f) * (1f + 0.25f * sin(time * 9f)), 4, time * 4f + k * 1.57f, Color.rgb(248, 113, 113)) } // #703 warning diamonds flank LAST STAND
        }
        // #15 HUD dims during boss intro
        if (bossIntroT > 0) {
            p.color = Color.rgb(7, 3, 18); p.alpha = (110 * min(1f, bossIntroT)).toInt().coerceIn(0, 110)
            c.drawRect(0f, 0f, w, dp(120f), p); p.alpha = 255
        }

        if (mutator == 2 && state == State.PLAYING) { // #120 Darkness + #242 gradient edge (wider, softer cutout underneath)
            val dpath2 = Path()
            dpath2.fillType = Path.FillType.EVEN_ODD
            dpath2.addRect(0f, 0f, w, h, Path.Direction.CW)
            dpath2.addCircle(px, py, dp(182f), Path.Direction.CCW)
            p.color = Color.rgb(2, 0, 10); p.alpha = 105
            c.drawPath(dpath2, p)
            val dpath = Path()
            dpath.fillType = Path.FillType.EVEN_ODD
            dpath.addRect(0f, 0f, w, h, Path.Direction.CW)
            dpath.addCircle(px, py, dp(150f), Path.Direction.CCW)
            p.alpha = 218
            c.drawPath(dpath, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 90
            c.drawCircle(px, py, dp(150f), p); p.style = Paint.Style.FILL; p.alpha = 255
        }

        drawHud(c)

        // wave banner
        if (waveBannerT > 0 && state == State.PLAYING) {
            val a = (min(1f, waveBannerT) * 255).toInt()
            // #169 letterspacing expand-in; #170 boss name slam (oversize while bossIntroT runs down)
            val bTitle = if (bossWave) "⚠ WAVE $wave — BOSS ⚠" else "WAVE $wave"
            val bCol = if (bossWave) Color.rgb(244, 114, 182) else Color.rgb(165, 243, 252)
            val bSize = dp(30f) * (if (bossWave) 1f + 0.5f * (bossIntroT / 2f).coerceIn(0f, 1f) else 1f)
            val bAge = 2f - waveBannerT
            val spread = dp(1.5f) + dp(15f) * (1f - min(1f, bAge / 0.55f))
            p.typeface = android.graphics.Typeface.DEFAULT_BOLD; p.textSize = bSize
            var tw3 = 0f; for (ch3 in bTitle) tw3 += p.measureText(ch3.toString()) + spread
            tw3 -= spread
            roundRect(c, RectF(w / 2 - tw3 / 2 - dp(16f), h * 0.3f - dp(31f), w / 2 + tw3 / 2 + dp(16f), h * 0.3f + dp(13f)), dp(12f), Color.rgb(7, 3, 18), (a * 0.55f).toInt()) // #483 readability pill behind the banner
            var bx3 = w / 2 - tw3 / 2
            p.textAlign = Paint.Align.LEFT; p.color = bCol; p.alpha = a
            var li5 = 0 // #536 a shimmer travels the banner letters
            for (ch3 in bTitle) { p.alpha = (a * (0.72f + 0.28f * sin(time * 6f - li5 * 0.55f))).toInt().coerceIn(0, a); c.drawText(ch3.toString(), bx3, h * 0.3f, p); bx3 += p.measureText(ch3.toString()) + spread; li5++ }
            p.alpha = 255
            val swx = w / 2 - tw3 / 2 + tw3 * ((time * 0.7f) % 1f); p.color = Color.rgb(250, 204, 21); p.alpha = (a * 0.8f).toInt(); c.drawCircle(swx, h * 0.3f + dp(9f), dp(2.2f), p); p.alpha = 255 // #664 a spark runs the banner's underline
            val sub = when {
                wave == 1 -> "Survive the storm!"
                bossWave -> "👑 Warden + ${max(0, toSpawn + enemies.size - 1)} minions — bring it down!"
                wave == MAX_WAVE -> "Final wave — end this!"
                else -> "${toSpawn + enemies.size} hostiles inbound"
            }
            txt(c, sub, w / 2, h * 0.3f + dp(34f), dp(15f), Color.WHITE, false, a)
            if (bossWave) poly(c, w / 2 - tw3 / 2 - dp(26f), h * 0.3f + dp(30f), dp(4f), 4, time * 4f, Color.rgb(244, 114, 182)) // #729 a pink diamond heralds the Warden's banner
            if (bossWave) poly(c, w / 2 + tw3 / 2 + dp(26f), h * 0.3f + dp(30f), dp(4f), 4, -time * 4f, Color.rgb(244, 114, 182)) // #779 a twin diamond answers on the banner's right
            if (mutator > 0) txt(c, mutatorTitle(), w / 2, h * 0.3f + dp(58f), dp(14f), Color.rgb(250, 204, 21), true, (a * (0.75f + 0.25f * sin(time * 5f))).toInt().coerceIn(0, a)) // #584 the mutator line pulses
        }
        if (eliteBannerT > 0 && state == State.PLAYING) {
            val a2 = (min(1f, eliteBannerT) * 255).toInt().coerceIn(0, 255)
            txt(c, "⭐ ELITE DOWN ⭐", w / 2, h * 0.2f, dp(22f), Color.rgb(250, 204, 21), true, a2)
            for (k in 0 until 2) { val sx9 = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx9, h * 0.2f - dp(7f), dp(4f), 4, time * 4f + k * 1.7f, Color.rgb(250, 204, 21)) } // #660 stars spin at the elite shout's shoulders
            p.color = Color.rgb(250, 204, 21); p.alpha = (a2 * 0.7f).toInt(); c.drawRect(w / 2 - dp(88f), h * 0.2f + dp(6f), w / 2 + dp(88f), h * 0.2f + dp(8f), p); p.alpha = 255 // #735 a gold rule underlines ELITE DOWN
            p.color = Color.rgb(254, 240, 138); p.alpha = a2; c.drawRect(w / 2 - dp(92f), h * 0.2f + dp(4f), w / 2 - dp(90f), h * 0.2f + dp(10f), p); c.drawRect(w / 2 + dp(90f), h * 0.2f + dp(4f), w / 2 + dp(92f), h * 0.2f + dp(10f), p); p.alpha = 255 // #780 end ticks cap the ELITE DOWN rule
        }
        if (countT > 0 && state == State.PLAYING) { // #294 big countdown numeral
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = CHARS[save.selectedChar].color // #449 countdown pulse ring, #525 in your hero's color
            p.alpha = (160 * (countT % 0.5f) / 0.5f).toInt().coerceIn(0, 160)
            c.drawCircle(w / 2, h * 0.40f - dp(22f), dp(30f) + dp(26f) * (1f - (countT % 0.5f) / 0.5f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            for (k in 0 until 4) { val ta2 = k * 1.5708f + time * 1.2f; poly(c, w / 2 + cos(ta2) * dp(52f), h * 0.40f - dp(20f) + sin(ta2) * dp(52f), dp(3.4f), 4, ta2, CHARS[save.selectedChar].color) } // #619 ticks orbit the countdown
            (0 until 4).forEach { k -> val ta3 = k * 1.5708f - time * 1.2f; poly(c, w / 2 + cos(ta3) * dp(66f), h * 0.40f - dp(20f) + sin(ta3) * dp(66f), dp(2.4f), 4, ta3, Color.rgb(250, 204, 21)) } // #728 gold ticks counter-orbit the countdown
            (0 until 4).forEach { k -> val ca4 = k * 1.5708f; p.color = CHARS[save.selectedChar].color; p.alpha = 110; c.drawCircle(w / 2 + cos(ca4) * dp(58f), h * 0.40f - dp(20f) + sin(ca4) * dp(58f), dp(1.8f), p) }; p.alpha = 255 // #778 cardinal ticks stud the countdown ring
            txt(c, "$countStage", w / 2, h * 0.40f, dp(76f), CHARS[save.selectedChar].color, true, 46) // #591 echo numeral behind the count
            txt(c, "$countStage", w / 2, h * 0.40f, dp(64f), Color.rgb(250, 204, 21), true)
        }
        if (resumeT > 0) { // #391 resume countdown numeral
            val rs = ((resumeT / 0.5f) + 0.999f).toInt().coerceIn(1, 3)
            txt(c, "$rs", w / 2, h * 0.40f, dp(64f), Color.rgb(52, 211, 153), true)
            txt(c, "GET READY…", w / 2, h * 0.40f + dp(52f), dp(16f), Color.WHITE, true, (170 + 80 * sin(time * 8f)).toInt().coerceIn(90, 250)) // #592 GET READY pulses
        }
        if (bossIntroT > 0 && state == State.PLAYING) { // #241 boss intro letterbox bars
            val bf2 = if (bossIntroT > 1.6f) ((2f - bossIntroT) / 0.4f).coerceIn(0f, 1f) else (bossIntroT / 0.5f).coerceIn(0f, 1f)
            p.color = Color.BLACK; p.alpha = 255
            c.drawRect(0f, 0f, w, dp(48f) * bf2, p)
            c.drawRect(0f, h - dp(48f) * bf2, w, h, p)
            p.color = Color.rgb(220, 38, 38); p.alpha = (200 * bf2).toInt() // #531 red edge lines on the letterbox
            c.drawRect(0f, dp(48f) * bf2, w, dp(48f) * bf2 + dp(2f), p)
            c.drawRect(0f, h - dp(48f) * bf2 - dp(2f), w, h - dp(48f) * bf2, p); p.alpha = 255
        }
        if (waveClearT > 0 && state == State.PLAYING) {
            val a3 = (min(1f, waveClearT) * 255).toInt().coerceIn(0, 255)
            txt(c, "✔ WAVE $waveClearedNum CLEAR!  +$waveClearBonus", w / 2, h * 0.38f, dp(19f), Color.rgb(52, 211, 153), true, a3)
            p.color = Color.rgb(250, 204, 21); p.alpha = (a3 * 0.8f).toInt() // #566 gold underline sweeps the clear text
            c.drawRect(w / 2 - dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(8f), w / 2 + dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(10.5f), p); p.alpha = 255
            (0 until 2).forEach { k -> val sx2 = w / 2 + (if (k == 0) -1f else 1f) * dp(196f); poly(c, sx2, h * 0.38f - dp(6f), dp(6f), 4, time * 5f + k * 2.1f, Color.rgb(250, 204, 21)) } // #704 gold stars spin beside the CLEAR! shout
            // #197/#218 REMOVED in v2.4 (Nathan's call): the full-height green sweep band
            // read as a weird opaque bar sliding across the screen. The CLEAR! text
            // and the #493 sparkle rain carry the moment now.
        }
    }

    private fun drawHud(c: Canvas) {
        val m = dp(14f)
        val hs = (if (save.bigHud) 1.25f else 1f) * (if (save.oneHanded) 0.88f else 1f) // #375 compressed HUD
        // HP bar
        val barW = w * 0.31f; val barH = dp(15f) // v2.3 HUD: three clean columns — the left bars never reach the center
        roundRect(c, RectF(m, m, m + barW, m + barH), dp(7f), Color.rgb(30, 10, 40), if (save.highContrast) 255 else 210)
        val hpFrac = (hp / maxHp).coerceIn(0f, 1f)
        val hpCol = when { hpFrac > 0.5f -> Color.rgb(74, 222, 128); hpFrac > 0.25f -> Color.rgb(250, 204, 21); else -> Color.rgb(248, 113, 113) }
        if (hpFrac < 0.25f) { // #1 low-HP pulse
            p.color = Color.rgb(248, 113, 113); p.alpha = (120 + 100 * sin(time * 7f)).toInt().coerceIn(20, 220)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f)
            c.drawRoundRect(RectF(m - dp(2f), m - dp(2f), m + barW + dp(2f), m + barH + dp(2f)), dp(9f), dp(9f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        if (playerGhostHp > hp) roundRect(c, RectF(m, m, m + barW * (playerGhostHp / maxHp).coerceIn(0f, 1f), m + barH), dp(7f), Color.rgb(254, 226, 226)) // #518 damage ghost trails your HP
        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)
        for (q in 1..3) { val qx2 = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 110; c.drawRect(qx2 - dp(0.7f), m + dp(1f), qx2 + dp(0.7f), m + barH - dp(1f), p) }; p.alpha = 255 // #752 the HP bar wears segment ticks
        if (hpFrac > 0.03f) { p.color = Color.WHITE; p.alpha = (120 + 70 * sin(time * 6f)).toInt().coerceIn(50, 190); c.drawCircle(m + barW * hpFrac, m + barH / 2, dp(2f), p); p.alpha = 255 } // #753 the HP tip glows
        if (hpFrac in 0f..0.25f) { // #445 danger pulse on a dying bar
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f)
            p.color = Color.WHITE; p.alpha = (120 + 110 * sin(time * 9f)).toInt().coerceIn(10, 230)
            c.drawRoundRect(RectF(m, m, m + barW, m + barH), dp(7f), dp(7f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        txtL(c, "HP ${hp.toInt()}/${maxHp.toInt()}", m + dp(6f), m + barH - dp(3.5f), dp(10.5f) * hs, Color.WHITE, true)
        // XP bar
        val xpY = m + barH + dp(5f)
        roundRect(c, RectF(m, xpY, m + barW, xpY + dp(9f)), dp(4f), Color.rgb(30, 10, 40), if (save.highContrast) 255 else 210)
        val xpFrac = (xp.toFloat() / xpNext).coerceIn(0f, 1f)
        if (xpFrac > 0) roundRect(c, RectF(m, xpY, m + barW * xpFrac, xpY + dp(9f)), dp(4f), if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153)) // #486 near-level gold
        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow
        if (xpFrac > 0.1f) { p.color = Color.rgb(52, 211, 153); p.alpha = 90; c.drawCircle(m + barW * xpFrac - dp(7f), xpY + dp(4.5f), dp(1.4f), p); p.alpha = 255 } // #730 a spark trails the XP tip
        for (q in 1..3) { val qx = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 130; c.drawRect(qx - dp(0.7f), xpY + dp(1f), qx + dp(0.7f), xpY + dp(8f), p) } // #517 XP quarter ticks
        for (q in 1..7 step 2) { val qx3 = m + barW * q / 8f; p.color = Color.rgb(7, 3, 18); p.alpha = 60; c.drawRect(qx3 - dp(0.5f), xpY + dp(2f), qx3 + dp(0.5f), xpY + dp(7f), p) }; p.alpha = 255 // #754 finer eighth ticks resolve the XP bar
        p.alpha = 255
        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), true) // #594 LV runs gold when a level is close
        txtR(c, "$xp/$xpNext", m + barW - dp(4f), xpY + dp(8.5f), dp(8.5f), Color.WHITE, true)
        if (combo >= 3) { // 🔥 combo meter with decay bar
            val cmult = 1 + min(combo / 10, 4)
            if (combo >= 10) { // #448 hot-streak backlight
                p.color = Color.rgb(249, 115, 22); p.alpha = (36 + 16 * sin(time * 7f)).toInt().coerceIn(20, 52)
                c.drawRoundRect(RectF(m - dp(6f), xpY + dp(26f), m + barW * 0.62f, xpY + dp(48f)), dp(9f), dp(9f), p); p.alpha = 255
            }
            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)
            if (combo >= 100) (0 until 2).forEach { k -> poly(c, m + dp(6f) + k * dp(11f), xpY + dp(34f), dp(2.6f), 4, time * 4f + k * 2.1f, Color.rgb(251, 146, 60)) } // #755 twin sparks mark a 100+ combo
            if (combo >= 100) poly(c, m + dp(118f), xpY + dp(40f), dp(4.4f), 4, time * 5f, Color.rgb(250, 204, 21)) // #698 a gold star crowns a 100+ combo
            if (combo >= 50) for (k in 0 until 3) { val fx4 = m + barW * 0.6f * (k + 0.5f) / 3f; poly(c, fx4, xpY + dp(47f) + sin(time * 11f + k * 2.2f) * dp(1.6f), dp(3.6f + 1.2f * sin(time * 13f + k)), 3, -1.5708f, Color.rgb(249, 115, 22)) } // #551 at 50+ the combo bar sprouts flames
            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f, xpY + dp(55f)), dp(2f), Color.rgb(30, 10, 40), 200)
            if (comboT > 0.05f) { p.color = Color.rgb(254, 240, 138); p.alpha = 200; c.drawCircle(m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(52.5f), dp(2.6f), p); p.alpha = 255 } // #622 the combo fuse burns at the tip
            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), if (comboT < 0.6f && (time * 10f).toInt() % 2 == 0) Color.rgb(248, 113, 113) else Color.rgb(251, 146, 60)) // #488 expiry flash
            txtL(c, "BEST $comboBest", m + barW * 0.6f + dp(10f), xpY + dp(55f), dp(10.5f), Color.rgb(148, 163, 184), true)
        }
        // wave + score + gems
        txt(c, if (endlessMode) "WAVE $wave ∞" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(47f), dp(15f), if (endlessMode) Color.rgb(250, 204, 21) else Color.rgb(233, 213, 255), true) // v2.3 own row — collides with nothing; #583 endless runs gold
        if (dailyMode) txt(c, "📅 DAILY SEED $dailyDay", w / 2, m + dp(93f), dp(10.5f), Color.rgb(94, 234, 212), true) // #334 (v2.3: moved to its own clear row)
        if (save.totalRuns == 0 && wave == 1 && runTime < 7f) { // #392 first-run joystick guide
            val gp = 0.5f + 0.5f * sin(time * 5f)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.argb((140 + 100 * gp).toInt(), 34, 211, 238)
            c.drawCircle(w * 0.27f, h * 0.76f, dp(40f) + dp(8f) * gp, p); p.style = Paint.Style.FILL
            txt(c, "👆 DRAG HERE TO MOVE", w * 0.27f, h * 0.76f - dp(58f), dp(13f), Color.rgb(165, 243, 252), true)
        }
        if (save.totalRuns == 0 && dashCharges > 0 && enemies.any { hyp(it.x - px, it.y - py) < dp(170f) }) { // #393 first dash prompt
            val dpulse = 0.5f + 0.5f * sin(time * 6f)
            txt(c, "💨 TAP DASH!", dashBtnRect.centerX(), dashBtnRect.top - dp(14f) - dp(4f) * dpulse, dp(13f), Color.rgb(250, 204, 21), true)
        }
        if (save.totalRuns == 0 && rocketCharges >= rocketMaxCharges && enemies.any { hyp(it.x - px, it.y - py) < dp(220f) }) { // #484 first rocket prompt
            val rpulse = 0.5f + 0.5f * sin(time * 6f + 1f)
            txt(c, "🚀 TAP ROCKET!", rocketBtnRect.centerX(), rocketBtnRect.top - dp(24f) - dp(4f) * rpulse, dp(13f), Color.rgb(251, 146, 60), true)
            poly(c, rocketBtnRect.centerX(), rocketBtnRect.top - dp(10f) + dp(2f) * rpulse, dp(4f), 4, time * 2f, Color.rgb(251, 146, 60)) // #608 a diamond spins under the prompt
        }
        if (waveTotal > 0) {
            val remain = toSpawn + enemies.size
            val wfrac = (1f - remain.toFloat() / waveTotal).coerceIn(0f, 1f)
            roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), Color.rgb(30, 10, 40), 200)
            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))
            p.color = Color.rgb(7, 3, 18); p.alpha = 160; c.drawCircle(w / 2 - dp(42f), m + dp(54f), dp(2.6f), p); c.drawCircle(w / 2 + dp(42f), m + dp(54f), dp(2.6f), p); p.alpha = 255 // #756 end caps pin the wave bar
            if (wfrac > 0.02f) { // #526 a tiny turtle rides the wave progress
                val txp = w / 2 - dp(42f) + dp(84f) * wfrac
                p.color = Color.rgb(167, 243, 208); p.alpha = 120; c.drawCircle(txp - dp(8f), m + dp(54f), dp(1.5f), p); p.alpha = 255 // #757 the progress turtle kicks a wake dot
                p.color = Color.rgb(34, 197, 94); c.drawCircle(txp, m + dp(54f), dp(3.4f), p)
                p.color = Color.rgb(187, 247, 208); c.drawCircle(txp + dp(3.6f), m + dp(54f), dp(1.7f), p)
                p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 9f)).toInt().coerceIn(10, 90); c.drawCircle(txp - dp(6f), m + dp(54f), dp(1.1f), p); p.alpha = 255 // #634 the progress turtle kicks a wake
            }
            if (remain in 1..3) { // #446 almost there — the bar burns gold
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                p.alpha = (140 + 100 * sin(time * 8f)).toInt().coerceIn(40, 240)
                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (remain in 1..8) { p.color = Color.rgb(250, 204, 21); p.alpha = (140 + 80 * sin(time * 7f)).toInt().coerceIn(60, 220); for (k in 0 until remain) c.drawCircle(w / 2 - (remain - 1) * dp(3f) + k * dp(6f), m + dp(63f), dp(1.8f), p); p.alpha = 255 } // #631 the last foes show as pips under the bar
        }
        if (save.showTimer) { p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 100 * sin(time * 6.283f)).toInt().coerceIn(20, 220); c.drawCircle(w / 2 + dp(36f), m + dp(76f), dp(1.8f), p); p.alpha = 255 } // #632 a second-hand dot ticks beside the timer
        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(80f), dp(11.5f), Color.rgb(148, 163, 184))
        if (scorePop > 0.1f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21); p.alpha = (140 * (scorePop / 0.35f)).toInt().coerceIn(0, 140); c.drawCircle(w - m - dp(88f), m + dp(11f), dp(13f) + dp(6f) * (1f - scorePop / 0.35f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #633 big scores ring the SCORE label
        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.WHITE, true) // #492
        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))
        if (gemsPop > 0.05f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(52, 211, 153); p.alpha = (160 * gemsPop).toInt().coerceIn(0, 160); c.drawCircle(w - m - dp(170f), m + dp(32f), dp(6f) + dp(5f) * (1f - gemsPop), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #758 a ring pops off the gem counter
        if (gemsPop > 0.05f) poly(c, w - m - dp(170f), m + dp(32f), dp(4f) * (0.7f + 0.3f * gemsPop), 4, time * 7f, Color.rgb(52, 211, 153)) // #723 a gem spark spins while the gem count pops
        // boss bar
        val boss = enemies.firstOrNull { it.type == 5 }
        if (boss != null) {
            val bw = w * 0.7f; val bx = (w - bw) / 2; val by = h - dp(148f) // v2.3 boss bar lives at the bottom now — the top stays clean
            roundRect(c, RectF(bx, by, bx + bw, by + dp(13f)), dp(6f), Color.rgb(30, 10, 40), 220)
            if (bossGhostHp > boss.hp) roundRect(c, RectF(bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by, bx + bw * (bossGhostHp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(233, 213, 255)) // #487 damage ghost
            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))
            val hpF2 = (boss.hp / boss.maxHp).coerceIn(0f, 1f); val shb = ((time * 0.35f) % 1.3f - 0.15f) // #620 a shine stripe travels the boss bar's fill
            if (shb in 0f..1f && hpF2 > 0.02f) { val sxb = bx + bw * hpF2 * shb; p.color = Color.WHITE; p.alpha = 34; c.drawRect(sxb, by + dp(1.5f), min(sxb + dp(10f), bx + bw * hpF2), by + dp(11.5f), p); p.alpha = 255 }
            if (hpF2 < 0.25f && hpF2 > 0f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 100 * sin(time * 8f)).toInt().coerceIn(20, 220); c.drawCircle(bx + bw * hpF2, by + dp(6.5f), dp(3.4f), p); p.alpha = 255 } // #724 the boss bar's tip burns red under 25%
            if (hpF2 in 0.4f..0.6f) { p.color = Color.rgb(250, 204, 21); p.alpha = (130 + 80 * sin(time * 6f)).toInt().coerceIn(50, 210); c.drawCircle(bx + bw * 0.5f, by + dp(6.5f), dp(2.6f), p); p.alpha = 255 } // #759 a gold bead marks the Warden's halfway point
            p.color = Color.rgb(7, 3, 18); p.alpha = 160 // #447 phase ticks at the third marks
            c.drawRect(bx + bw / 3f - dp(1f), by + dp(1.5f), bx + bw / 3f + dp(1f), by + dp(11.5f), p)
            c.drawRect(bx + bw * 2f / 3f - dp(1f), by + dp(1.5f), bx + bw * 2f / 3f + dp(1f), by + dp(11.5f), p)
            p.alpha = 255
            for (k in 0 until 2) { // #543 flame tips lick the boss bar's ends
                val fx3 = if (k == 0) bx - dp(6f) else bx + bw + dp(6f)
                poly(c, fx3, by + dp(6f) + sin(time * 9f + k * 2f) * dp(1.5f), dp(5f + 1.5f * sin(time * 11f + k)), 3, -1.5708f, Color.rgb(249, 115, 22))
            }
            poly(c, bx + dp(7f), by + dp(6.5f), dp(4f), 4, time * 2f, Color.rgb(216, 180, 254)) // #598 boss bar end-gems spin (loop-11: seated on the bar itself)
            poly(c, bx + bw - dp(7f), by + dp(6.5f), dp(4f), 4, -time * 2f, Color.rgb(216, 180, 254))
            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)
            txt(c, "${boss.hp.toInt()} / ${boss.maxHp.toInt()}", w / 2, by + dp(11f), dp(9f), Color.WHITE, true)
        }
        // pause button
        pauseBtnRect.set(w - m - dp(56f), m, w - m, m + dp(56f))
        val pvR = RectF(pauseBtnRect.centerX() - dp(20f), pauseBtnRect.centerY() - dp(20f), pauseBtnRect.centerX() + dp(20f), pauseBtnRect.centerY() + dp(20f))
        roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 238) // v2.6 pause button more opaque
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = CHARS[save.selectedChar].color; p.alpha = (50 + 40 * sin(time * 2.4f)).toInt().coerceIn(10, 90) // #496 idle pulse, #532 in your hero's color
        c.drawRoundRect(RectF(pvR.left - dp(2.5f), pvR.top - dp(2.5f), pvR.right + dp(2.5f), pvR.bottom + dp(2.5f)), dp(12f), dp(12f), p)
        p.style = Paint.Style.FILL; p.alpha = 255
        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)
        (0 until 4).forEach { k -> val sx5 = if (k % 2 == 0) pvR.left + dp(4f) else pvR.right - dp(4f); val sy5 = if (k < 2) pvR.top + dp(4f) else pvR.bottom - dp(4f); p.color = Color.rgb(148, 163, 184); p.alpha = 150; c.drawCircle(sx5, sy5, dp(1.2f), p) }; p.alpha = 255 // #760 screws pin the pause button's corners
        // joystick
        if (state == State.PLAYING) {
            if (aimPointer != -1) { // #360 aim stick visual
                p.color = Color.rgb(244, 114, 182); p.alpha = 150
                c.drawCircle(aimOx, aimOy, dp(52f) * save.joySize, p)
                p.alpha = 225
                c.drawCircle(aimOx + aimDx * dp(38f), aimOy + aimDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255
                if (hyp(aimDx, aimDy) > 0.12f) { // #470 arrowhead shows the fire direction
                    val aa2 = atan2(aimDy, aimDx)
                    poly(c, aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(8f), 4, aa2 + 0.78f, Color.rgb(244, 114, 182))
                    p.color = Color.WHITE; p.alpha = (170 + 70 * sin(time * 10f)).toInt().coerceIn(100, 240) // #544 the arrowhead's core pulses
                    c.drawCircle(aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(2.6f), p); p.alpha = 255
                    poly(c, aimOx + cos(aa2) * dp(48f), aimOy + sin(aa2) * dp(48f), dp(5f), 4, aa2 + 0.78f, Color.rgb(249, 168, 212)) // #647 a fainter echo diamond trails the arrowhead
                }
            }
            if (joyPointer != -1) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = (150 + 60 * sin(time * 6f)).toInt().coerceIn(90, 210)
                c.drawCircle(joyOx, joyOy, dp(52f) * save.joySize, p) // #317
                p.color = Color.rgb(34, 211, 238); p.alpha = (130 * save.btnOpacity).toInt() // #395 v2.6 buttons more opaque
                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(34f) * save.joySize, p) // #317
                p.style = Paint.Style.FILL; p.alpha = (232 * save.btnOpacity).toInt(); p.color = Color.rgb(34, 211, 238) // #395 v2.6
                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255 // #317
                p.color = CHARS[save.selectedChar].color; p.alpha = (195 * save.btnOpacity).toInt(); c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(9f) * save.joySize, p); p.alpha = 255 // #648 the stick knob wears your hero's color at its heart
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(165, 243, 252); p.alpha = (70 + 40 * sin(time * 4f)).toInt().coerceIn(30, 110) // #530 outer halo breathes while you steer
                c.drawCircle(joyOx, joyOy, dp(60f) * save.joySize, p); p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 4).forEach { k -> val ja = k * 1.5708f + 0.785f; p.color = Color.rgb(165, 243, 252); p.alpha = 80; c.drawCircle(joyOx + cos(ja) * dp(60f) * save.joySize, joyOy + sin(ja) * dp(60f) * save.joySize, dp(1.6f), p) }; p.alpha = 255 // #783 tick dots stud the stick's halo
            } else if (joySpringT > 0) { // #182 knob springs home with a damped bounce
                val ph = 1f - joySpringT / 0.28f
                val off = (1f - ph) * (1f + 0.55f * sin(ph * 7f) * (1f - ph))
                val kx = springOx + (springKx - springOx) * off; val ky = springOy + (springKy - springOy) * off
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = (140 * (1f - ph)).toInt().coerceIn(0, 140)
                c.drawCircle(springOx, springOy, dp(52f) * save.joySize, p) // #317
                p.style = Paint.Style.FILL; p.color = Color.rgb(34, 211, 238); p.alpha = (190 * (1f - ph)).toInt().coerceIn(0, 190)
                c.drawCircle(kx, ky, dp(24f) * save.joySize, p); p.alpha = 255 // #317
            } else {
                if (save.totalRuns < 3) { // #394 control hints fade out over the first runs
                    val hintCol = Color.argb(when (save.totalRuns) { 0 -> 255; 1 -> 170; else -> 90 }, 148, 163, 184)
                    if (save.joyLeft) txtR(c, "drag to move ▶", w - m, h - dp(96f), dp(12f), hintCol) // #318
                    else txtL(c, "◀ drag to move", m, h - dp(96f), dp(12f), hintCol)
                }
            }
            // dash button
            val dr = dp(38f) * save.btnScale // #396 button scale
            val dby = if (save.oneHanded) dp(92f) else dp(110f) // #375 dash rides closer one-handed
            if (save.joyLeft) dashBtnRect.set(m, h - dby - dr * 2, m + dr * 2, h - dby) // #318 left-side layout
            else dashBtnRect.set(w - m - dr * 2, h - dby - dr * 2, w - m, h - dby)
            rocketBtnRect.set(dashBtnRect.left, dashBtnRect.top - dp(16f) - dr * 2, dashBtnRect.right, dashBtnRect.top - dp(16f)) // 🚀 stacked above dash
            val ready = dashCharges > 0
            p.color = if (ready) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105); p.alpha = (238 * save.btnOpacity).toInt() // #395 v2.6 buttons more opaque (Nathan)
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
            if (dashReadyT > 0) { // #183 charge-complete sweep reveal
                val rf = 1f - dashReadyT / 0.35f
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(34, 211, 238); p.alpha = (220 * (1f - rf)).toInt().coerceIn(0, 220)
                c.drawCircle(dashBtnRect.centerX(), dashBtnRect.centerY(), dr * (1f + rf * 0.8f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (dashCharges < dashMaxCharges) { // #538 dash button sweeps its recharge
                val dprog2 = 1f - (dashRechargeT / dashCdMax).coerceIn(0f, 1f)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(34, 211, 238); p.alpha = 190
                c.drawArc(RectF(dashBtnRect.left + dp(4f), dashBtnRect.top + dp(4f), dashBtnRect.right - dp(4f), dashBtnRect.bottom - dp(4f)), -90f, 360f * dprog2, false, p)
                val ta4 = (-90f + 360f * dprog2) * 0.01745f; p.style = Paint.Style.FILL; p.color = Color.WHITE; p.alpha = 210; c.drawCircle(dashBtnRect.centerX() + cos(ta4) * (dr - dp(4f)), dashBtnRect.centerY() + sin(ta4) * (dr - dp(4f)), dp(2f), p); p.alpha = 255 // #782 a bright tip rides the dash recharge arc
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "DASH", dashBtnRect.centerX(), dashBtnRect.bottom + dp(15f), dp(10f), Color.rgb(165, 243, 252), true)
            if (dashMaxCharges > 1) {
                for (ci in 0 until dashMaxCharges) {
                    p.color = if (ci < dashCharges) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105)
                    c.drawCircle(dashBtnRect.centerX() + (ci - (dashMaxCharges - 1) / 2f) * dp(15f), dashBtnRect.bottom + dp(28f), dp(4.5f), p)
                }
            }
            // 🚀 ROCKET button — the dash button's sibling, stacked above it
            val rdy = rocketCharges > 0
            p.color = if (rdy) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105); p.alpha = (238 * save.btnOpacity).toInt() // v2.6 buttons more opaque
            c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.centerY(), dr, p); p.alpha = 255
            if (rocketCharges < rocketMaxCharges) {
                val rsweep = (rocketRechargeT / rocketCdMax).coerceIn(0f, 1f)
                p.color = Color.rgb(7, 3, 18); p.alpha = 130
                c.drawArc(rocketBtnRect, -90f, 360f * rsweep, true, p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(4f); p.color = Color.rgb(254, 240, 138); p.alpha = 220
                val rinset = RectF(rocketBtnRect.left + dp(3f), rocketBtnRect.top + dp(3f), rocketBtnRect.right - dp(3f), rocketBtnRect.bottom - dp(3f))
                c.drawArc(rinset, -90f, 360f * (1f - rsweep), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            } else {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(254, 240, 138)
                p.alpha = (140 + 90 * sin(time * 5f)).toInt().coerceIn(50, 230)
                c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.centerY(), dr + dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (rocketReadyT > 0) { // #481 charge-complete flash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(254, 240, 138)
                p.alpha = (220 * (rocketReadyT / 0.35f)).toInt().coerceIn(0, 220)
                c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.centerY(), dr + dp(10f) * (1f - rocketReadyT / 0.35f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (rocketCharges == 0) { // #609 recharge arc sweeps the rocket button
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(249, 115, 22); p.alpha = 170
                c.drawArc(RectF(rocketBtnRect.left + dp(5f), rocketBtnRect.top + dp(5f), rocketBtnRect.right - dp(5f), rocketBtnRect.bottom - dp(5f)), -90f, 360f * (1f - (rocketRechargeT / rocketCdMax).coerceIn(0f, 1f)), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)
            txt(c, "ROCKET", rocketBtnRect.centerX(), rocketBtnRect.top - dp(5f), dp(10f), Color.rgb(254, 240, 138), true)
            for (ci in 0 until rocketMaxCharges) {
                if (ci < rocketCharges) { p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 5f + ci)).toInt().coerceIn(20, 100); c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(6.4f), p); p.alpha = 255 } // #565 charged pips glow
                if (ci == 0 && rocketCharges >= rocketMaxCharges) { p.color = Color.WHITE; p.alpha = (120 + 80 * sin(time * 6f)).toInt().coerceIn(40, 200); c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.bottom + dp(8f), dp(1.8f), p); p.alpha = 255 } // #781 a white cap crowns a full rocket rack
                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }
            if (rocketCharges >= rocketMaxCharges) (0 until 2).forEach { k -> val ra2 = time * 3.4f + k * 3.14159f; poly(c, rocketBtnRect.centerX() + cos(ra2) * dp(34f), rocketBtnRect.centerY() + sin(ra2) * dp(34f), dp(2.8f), 4, ra2, Color.rgb(254, 240, 138)) } // #733 sparks orbit a full rocket charge
        }
    }

    private fun drawLevelUp(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 200; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        for (k in 0 until 9) { // #572 gold motes rise through the draft
            val mx2 = (k * 0.618f + 0.07f) * w; val my2 = h - ((time * 26f + k * 173f) % (h + 40f)) + dp(20f)
            p.color = Color.rgb(250, 204, 21); p.alpha = (60 + 50 * sin(time * 2f + k * 1.7f)).toInt().coerceIn(10, 110)
            c.drawCircle(mx2, my2, dp(2.2f), p)
        }
        p.alpha = 255
        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)
        p.color = Color.rgb(250, 204, 21); p.alpha = (60 + 40 * sin(time * 3f)).toInt().coerceIn(20, 100); c.drawRect(w / 2 - dp(84f), h * 0.2f + dp(8f), w / 2 + dp(84f), h * 0.2f + dp(10f), p); p.alpha = 255 // #770 a gold rule underlines LEVEL UP
        (0 until 2).forEach { k -> val sx7 = w / 2 + (if (k == 0) -1f else 1f) * dp(132f); poly(c, sx7, h * 0.2f - dp(9f), dp(4.6f), 4, time * 3.6f + k * 1.8f, Color.rgb(250, 204, 21)) } // #692 stars spin beside the LEVEL shout
        txt(c, "Choose an upgrade", w / 2, h * 0.2f + dp(30f), dp(15f), Color.rgb(203, 213, 225))
        // #332 owned build icons row
        val ownedRow = (0..23).filter { upgradeCounts[it] > 0 }
        if (ownedRow.isNotEmpty()) {
            var ox = w / 2 - ownedRow.size * dp(21f)
            for (oi in ownedRow) {
                txt(c, UPGRADE_NAMES[oi].substringBefore(' '), ox + dp(19f), h * 0.2f + dp(64f), dp(15f), Color.WHITE)
                if (upgradeCounts[oi] > 1) txt(c, "×${upgradeCounts[oi]}", ox + dp(19f), h * 0.2f + dp(79f), dp(9.5f), Color.rgb(250, 204, 21), true)
                if (upgradeCounts[oi] >= 3) { p.color = Color.rgb(250, 204, 21); p.alpha = 120; c.drawCircle(ox + dp(19f), h * 0.2f + dp(86f), dp(1.7f), p); p.alpha = 255 } // #693 stacked builds earn a gold dot
                ox += dp(42f)
            }
        }
        val cardW = w * 0.86f; val cardH = dp(96f); val x0 = (w - cardW) / 2
        var y0 = h * 0.3f
        var cardIdx = 0
        for (oi in offers) {
            val r = RectF(x0, y0, x0 + cardW, y0 + cardH)
            val flip = ((screenT - cardIdx * 0.09f) / 0.38f).coerceIn(0f, 1f); cardIdx++ // #166 flip-in
            if (flip <= 0f) { y0 += cardH + dp(14f); continue }
            c.save(); c.translate(r.centerX(), r.centerY()); c.scale(max(0.03f, sin(flip * 1.5708f)), 1f); c.translate(-r.centerX(), -r.centerY())
            roundRect(c, RectF(x0, y0 + dp(5f), x0 + cardW, y0 + cardH + dp(5f)), dp(14f), Color.BLACK, 110)
            val rarityCol = when (oi) { 6, 10 -> Color.rgb(192, 132, 252); 5, 11 -> Color.rgb(96, 165, 250); else -> Color.rgb(52, 211, 153) }
            button(c, r, "", Color.rgb(30, 14, 56), Color.WHITE, 16f, "up:$oi", rarityCol)
            poly(c, r.right - dp(18f), y0 + dp(18f), dp(4.4f), 4, time * 3.4f + oi, rarityCol) // #691 draft cards sparkle in their rarity color
            txtFitC(c, UPGRADE_NAMES[oi], r.centerX(), y0 + dp(38f), dp(19f), Color.WHITE, r.width() - dp(16f), true)
            txtFitC(c, UPGRADE_DESCS[oi], r.centerX(), y0 + dp(66f), dp(13.5f), Color.rgb(165, 243, 252), r.width() - dp(16f), false)
            if (upgradeCounts[oi] > 0) txt(c, "Owned ×${upgradeCounts[oi]}", r.centerX(), y0 + dp(87f), dp(11f), Color.rgb(250, 204, 21), true)
            c.restore()
            y0 += cardH + dp(14f)
        }
        val avail = (if (freeRerollUsed) 0 else 1) + rerollTokens
        val bw2 = (cardW - dp(12f)) / 2
        button(c, RectF(x0, y0 + dp(2f), x0 + bw2, y0 + dp(48f)), "🔄 REROLL ($avail)", if (avail > 0) Color.rgb(30, 41, 59) else Color.rgb(60, 20, 30), Color.WHITE, 14f, "reroll")
        button(c, RectF(x0 + bw2 + dp(12f), y0 + dp(2f), x0 + cardW, y0 + dp(48f)), "🚫 SKIP +25💎", Color.rgb(30, 41, 59), Color.WHITE, 14f, "banish")
    }

    private fun drawPaused(c: Canvas) {
        val pIn = min(1f, screenT / 0.3f); val pEase = 1f - (1f - pIn) * (1f - pIn) // #177 overlay slides down
        p.color = Color.rgb(4, 2, 12); p.alpha = (205 * min(1f, screenT / 0.2f)).toInt(); p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        c.save(); c.translate(0f, (1f - pEase) * -dp(48f))
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = CHARS[save.selectedChar].color; p.alpha = (140 + 60 * sin(time * 2.2f)).toInt().coerceIn(80, 200)
        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p) // #569 pause frame breathes in your hero's color
        p.style = Paint.Style.FILL; (0 until 4).forEach { k -> val cx9 = if (k % 2 == 0) dp(12f) else w - dp(12f); val cy9 = if (k < 2) dp(12f) else h - dp(12f); poly(c, cx9, cy9, dp(4f), 4, time * 2f + k, CHARS[save.selectedChar].color) }; p.alpha = 255 // #697 the pause frame is studded at its corners
        p.style = Paint.Style.FILL; p.alpha = 255
        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)
        (0 until 2).forEach { k -> poly(c, w / 2 + (if (k == 0) -1f else 1f) * dp(122f), h * 0.3f - dp(11f), dp(4f), 4, time * 3.4f + k * 1.9f, CHARS[save.selectedChar].color) } // #771 diamonds flank the PAUSED title
        (0 until 2).forEach { k -> val px9 = w / 2 + (if (k == 0) -1f else 1f) * dp(104f); poly(c, px9, h * 0.3f - dp(10f), dp(4.2f), 4, time * 2.8f + k * 1.9f, CHARS[save.selectedChar].color) } // #694 hero-color sparks flank PAUSED
        txt(c, "Wave $wave  •  Score $score  •  💎 $gemsRun", w / 2, h * 0.3f + dp(34f), dp(15f), Color.rgb(165, 243, 252))
        // #328 run summary: build so far + run stats
        val pBuild = (0..23).filter { upgradeCounts[it] > 0 }.joinToString("  ") { "${UPGRADE_NAMES[it].substringBefore(' ')}×${upgradeCounts[it]}" }
        if (pBuild.isNotEmpty()) { p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 80 * sin(time * 3f)).toInt().coerceIn(40, 200); c.drawCircle(dp(20f), h * 0.3f + dp(54f), dp(2f), p); p.alpha = 255 } // #695 your build line wears a gold bead
        if (pBuild.isNotEmpty()) txtFitC(c, pBuild.take(62), w / 2, h * 0.3f + dp(58f), dp(12.5f), Color.rgb(250, 204, 21), w - dp(24f))
        txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}  •  ☠ $kills  •  ⭐ LV $level  •  💀 $deaths", w / 2, h * 0.3f + dp(79f), dp(12.5f), Color.rgb(148, 163, 184))
        val bw = w * 0.6f; val bh = dp(56f); val x0 = (w - bw) / 2
        p.color = Color.rgb(74, 222, 128); p.alpha = (30 + 22 * sin(time * 3.4f)).toInt().coerceIn(8, 52); c.drawRoundRect(RectF(x0 - dp(4f), h * 0.42f - dp(4f), x0 + bw + dp(4f), h * 0.42f + bh + dp(4f)), dp(16f), dp(16f), p); p.alpha = 255 // #597 RESUME breathes a soft halo
        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")
        (0 until 2).forEach { k -> poly(c, (if (k == 0) x0 + dp(10f) else x0 + bw - dp(10f)), h * 0.42f + dp(10f), dp(3.2f), 4, time * 3.2f + k * 1.9f, Color.rgb(187, 247, 208)) } // #734 studs pin the RESUME button's corners
        button(c, RectF(x0, h * 0.42f + bh + dp(14f), x0 + bw, h * 0.42f + bh * 2 + dp(14f)), "↻  RESTART RUN", Color.rgb(71, 85, 105), Color.WHITE, 17f, "restart")
        button(c, RectF(x0, h * 0.42f + bh * 2 + dp(28f), x0 + bw, h * 0.42f + bh * 3 + dp(28f)), "🏠  QUIT TO MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "quit")
        button(c, RectF(x0, h * 0.42f + bh * 3 + dp(42f), x0 + bw, h * 0.42f + bh * 4 + dp(42f)), if (save.muteAll) "🔊  UNMUTE ALL" else "🔇  MUTE ALL", Color.rgb(71, 85, 105), Color.WHITE, 17f, "muteall") // #292
        // #327 settings quick toggles row
        val chipW = (bw - dp(18f)) / 4
        val chipY = h * 0.42f + bh * 4 + dp(58f)
        button(c, RectF(x0, chipY, x0 + chipW, chipY + dp(48f)), "🎵 ${if (save.musicOn) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "musictoggle")
        button(c, RectF(x0 + chipW + dp(6f), chipY, x0 + chipW * 2 + dp(6f), chipY + dp(48f)), "📳 ${if (save.vibration) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "vibrate")
        button(c, RectF(x0 + chipW * 2 + dp(12f), chipY, x0 + chipW * 3 + dp(12f), chipY + dp(48f)), "💥 ${if (save.shakeOn) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "shaketoggle")
        button(c, RectF(x0 + chipW * 3 + dp(18f), chipY, x0 + bw, chipY + dp(48f)), "🔊 ${if (save.muteAll) "OFF" else "ON"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "muteall")
        (0 until 4).forEach { k -> val on = when (k) { 0 -> save.musicOn; 1 -> save.vibration; 2 -> save.shakeOn; else -> !save.muteAll }; if (on) { p.color = Color.rgb(74, 222, 128); p.alpha = 200; c.drawCircle(x0 + chipW * k + dp(6f) * k + chipW - dp(10f), chipY + dp(10f), dp(2.4f), p) } }; p.alpha = 255 // #696 live toggles wear a green bead
        c.restore()
    }

    private fun shareScore() { // #791
        val msg = "💎 GEMSTORM — I scored $score on Wave $wave with $kills kills! Best combo ×$comboBest 🔥 Can you beat that?"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, msg) }
        try { context.startActivity(android.content.Intent.createChooser(intent, "Share your run")) } catch (ex: Exception) { toast("📤 Sharing isn't available here") }
    }

    private fun drawHeart(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        // v2.6: a TRUE heart silhouette — bezier curves, no triangle stuck on the bottom (Nathan)
        heartPath.reset()
        heartPath.moveTo(x, y + r * 0.95f)
        heartPath.cubicTo(x - r * 1.35f, y + r * 0.1f, x - r * 0.95f, y - r * 1.05f, x, y - r * 0.38f)
        heartPath.cubicTo(x + r * 0.95f, y - r * 1.05f, x + r * 1.35f, y + r * 0.1f, x, y + r * 0.95f)
        heartPath.close()
        p.color = color; c.drawPath(heartPath, p)
    }

    private fun drawEnd(c: Canvas, victory: Boolean) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 215; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        for (fs in fallShards) poly(c, fs.x, fs.y, fs.r, fs.sides, fs.rot, fs.color) // #178 slow shards / #179 fountain gems
        if (victory) { // #570 golden glow orbs turn behind the victory
            for (k in 0 until 6) { val ra = time * 0.24f + k * 1.047f; p.color = Color.rgb(250, 204, 21); p.alpha = 12; c.drawCircle(w / 2 + cos(ra) * dp(170f), h * 0.30f + sin(ra) * dp(150f), dp(52f), p) }
            p.alpha = 255
        }
        if (victory) { // #249 slow victory zoom on the hero
            val vz = 1f + 0.07f * min(1f, screenT / 2.5f)
            c.save(); c.scale(vz, vz, w / 2, h * 0.42f)
        }
        if (victory) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(9f); p.color = Color.rgb(250, 204, 21); p.alpha = 13; (0 until 6).forEach { k -> val ra2 = k * 1.047f + time * 0.24f; c.drawLine(w / 2, h * 0.30f, w / 2 + cos(ra2) * dp(190f), h * 0.30f + sin(ra2) * dp(190f), p) }; p.style = Paint.Style.FILL; p.alpha = 255 } // #659 slow rays turn behind the victory hero
        if (!victory) { // #250 game-over desaturate wash + crack overlay
            p.color = Color.rgb(100, 116, 139); p.alpha = 34
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(148, 163, 184); p.alpha = 110
            for (k in 0 until 5) {
                var cxp2 = w * (0.2f + k * 0.15f); var cyp2 = 0f
                val seg = h * 0.16f / 4
                for (s2 in 0 until 4) { val nx2 = cxp2 + sin(k * 7.3f + s2 * 2.9f) * dp(16f); val ny2 = cyp2 + seg; c.drawLine(cxp2, cyp2, nx2, ny2, p); cxp2 = nx2; cyp2 = ny2 }
            }
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(248, 113, 113); p.alpha = (26 + 18 * sin(time * 2.2f)).toInt().coerceIn(8, 44); (0 until 5).forEach { k -> c.drawCircle(w * (0.2f + k * 0.15f), h * 0.16f, dp(2.6f), p) }; p.alpha = 255 // #658 ember light pools where the cracks end
        }
        if (!victory) { // #571 red embers drift up through the wreckage
            for (k in 0 until 7) { val ex2 = (k * 0.37f + 0.05f) * w; val ey2 = h - ((time * 20f + k * 211f) % (h + 30f)) + dp(15f)
                p.color = Color.rgb(248, 113, 113); p.alpha = (50 + 40 * sin(time * 2.4f + k * 1.9f)).toInt().coerceIn(10, 90); c.drawCircle(ex2, ey2, dp(2f), p) }
            p.alpha = 255
        }
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
        if (!victory) { // #615 sparks sift down from the shattered gem
            for (k in 0 until 5) { val sx9 = w / 2 + sin(k * 2.7f + time * 0.7f) * dp(20f); val sy9 = h * 0.13f + ((time * 24f + k * 47f) % dp(90f))
                p.color = Color.rgb(248, 113, 113); p.alpha = (70 + 50 * sin(time * 3f + k * 1.9f)).toInt().coerceIn(20, 120); c.drawCircle(sx9, sy9, dp(1.8f), p) }
            p.alpha = 255
        }
        if (victory) {
            txt(c, "🏆 VICTORY! 🏆", w / 2, h * 0.24f, dp(32f), Color.rgb(250, 204, 21), true)
            txt(c, "The Abyss is sealed. The storm is yours.", w / 2, h * 0.24f + dp(34f), dp(14.5f), Color.rgb(203, 213, 225))
        } else {
            txt(c, "💀 SHATTERED 💀", w / 2, h * 0.24f, dp(32f), Color.rgb(248, 113, 113), true)
            (0 until 2).forEach { k -> val ex = w / 2 + (if (k == 0) -1f else 1f) * dp(168f); poly(c, ex, h * 0.24f - dp(11f), dp(4.4f), 3, time * 3.8f + k * 2.2f, Color.rgb(249, 115, 22)) } // #726 embers flank the SHATTERED shout
            txt(c, "Your gem broke on Wave $wave. The storm rages on…", w / 2, h * 0.24f + dp(34f), dp(14.5f), Color.rgb(203, 213, 225))
        }
        if (newRecord) txt(c, "🌟 NEW HIGH SCORE! 🌟", w / 2, h * 0.24f + dp(66f), dp(16f), Color.rgb(52, 211, 153), true)
        if (newRecord) for (k in 0 until 8) { val cx8 = w / 2 + sin(k * 2.39f + time * 1.3f) * dp(120f); val cy8 = h * 0.24f + dp(58f) + cos(k * 1.71f + time * 1.7f) * dp(10f); poly(c, cx8, cy8, dp(3f), 4, time * 3f + k, if (k % 2 == 0) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153)) } // #628 record confetti orbits the shout
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
            p.color = Color.rgb(88, 28, 135); p.alpha = 60; c.drawRect(w * 0.18f, y + dp(8f), w * 0.82f, y + dp(9f), p); p.alpha = 255 // #699 stat rows rest on hairlines
            y += dp(30f)
        }
        // #329/#330 build recap (+ victory rank #331, daily share code #334)
        val eBuild = (0..23).filter { upgradeCounts[it] > 0 }.joinToString("  ") { "${UPGRADE_NAMES[it].substringBefore(' ')}×${upgradeCounts[it]}" }
        if (eBuild.isNotEmpty()) {
            txtFitC(c, "🧬 " + eBuild.take(60), w / 2, y + dp(14f), dp(12.5f), Color.rgb(250, 204, 21), w - dp(24f))
            if (eBuild.length > 60) txtFitC(c, eBuild.drop(60).take(60), w / 2, y + dp(32f), dp(12.5f), Color.rgb(250, 204, 21), w - dp(24f))
        }
        if (victory) txt(c, "🏅 RANK ${rankLetter()}  •  ⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}  •  💀 $deaths", w / 2, y + dp(54f), dp(14.5f), Color.rgb(52, 211, 153), true)
        if (victory) (0 until 2).forEach { k -> val mx4 = w / 2 + (if (k == 0) -1f else 1f) * dp(196f); poly(c, mx4, y + dp(50f), dp(3.8f), 4, time * 3.6f + k * 1.9f, Color.rgb(250, 204, 21)) } // #727 gold sparks flank the victory rank
        if (dailyMode) txt(c, "📅 Daily seed $dailyDay — share code: GS-$dailyDay-$score", w / 2, y + dp(74f), dp(12f), Color.rgb(94, 234, 212))
        val bw = w * 0.62f; val bh = dp(56f); val x0 = (w - bw) / 2
        var by = h * 0.70f
        if (victory && !endlessMode) { // #126 endless mode entry
            button(c, RectF(x0, by, x0 + bw, by + bh), "♾️  ENDLESS — KEEP GOING", Color.rgb(190, 24, 93), Color.WHITE, 16f, "endless")
            by += bh + dp(12f)
        }
        button(c, RectF(x0, by, x0 + bw, by + bh), "↻  RUN IT AGAIN", Color.rgb(22, 163, 74), Color.WHITE, 18f, "restart")
        by += bh + dp(12f)
        button(c, RectF(x0, by, x0 + bw, by + bh), "🛒  SHOP", Color.rgb(124, 58, 237), Color.WHITE, 17f, "shop")
        by += bh + dp(12f)
        button(c, RectF(x0, by, x0 + bw * 0.48f, by + bh), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "back")
        button(c, RectF(x0 + bw * 0.52f, by, x0 + bw, by + bh), "📤  SHARE", Color.rgb(8, 145, 178), Color.WHITE, 17f, "share") // #791 share your run
        if (victory) c.restore() // #249 zoom balance
    }

    // ---------- menu screens ----------
    private fun drawIntro(c: Canvas) { // #793 animated opening sequence
        val t = introT
        p.color = Color.rgb(4, 2, 12); p.alpha = 235; c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        val cx = w / 2; val cy = h * 0.42f
        // act 1: a gem falls out of the dark, trailing sparks
        val fall = min(1f, t / 0.9f); val fe = 1f - (1f - fall) * (1f - fall)
        val gy = -dp(30f) + (cy + dp(30f)) * fe
        if (t < 1.1f) for (k in 1..4) { p.color = Color.rgb(52, 211, 153); p.alpha = (130 - k * 26).coerceIn(0, 130); c.drawCircle(cx + sin(t * 9f + k) * dp(3f), gy - k * dp(16f), dp(3.4f) - k * dp(0.5f), p) }; p.alpha = 255
        poly(c, cx, gy, dp(17f) * (0.7f + 0.3f * fe), 4, 0.78f + t * 2.2f, Color.rgb(52, 211, 153))
        poly(c, cx, gy, dp(8.5f), 4, 0.78f + t * 2.2f, Color.rgb(167, 243, 208))
        // act 2: it lands — shockwaves roll out and the title slams in
        if (t > 0.9f) {
            val lt = min(1f, (t - 0.9f) / 0.5f)
            for (k in 0 until 2) { val ph = (t - 0.9f + k * 0.22f) % 0.9f; p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = if (k == 0) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21); p.alpha = (170 * (1f - ph / 0.9f)).toInt().coerceIn(0, 170); c.drawCircle(cx, cy, dp(20f) + dp(120f) * ph, p) }; p.style = Paint.Style.FILL; p.alpha = 255
            val ts = 0.7f + 0.3f * (1f - (1f - lt) * (1f - lt))
            c.save(); c.scale(ts, ts, cx, cy + dp(52f))
            txt(c, "GEMSTORM", cx, cy + dp(52f), dp(40f), Color.rgb(165, 243, 252), true, (lt * 255).toInt().coerceIn(0, 255))
            c.restore()
            p.color = Color.rgb(250, 204, 21); p.alpha = (lt * 180).toInt().coerceIn(0, 180); c.drawRect(cx - dp(120f) * lt, cy + dp(66f), cx + dp(120f) * lt, cy + dp(68f), p); p.alpha = 255
        }
        // act 3: your turtle jets across the bottom and the subtitle fades up
        if (t > 1.7f) {
            val fx = -dp(50f) + (w + dp(100f)) * min(1f, (t - 1.7f) / 1.3f)
            val fy = h * 0.78f + sin(t * 6f) * dp(5f)
            for (k in 1..3) poly(c, fx - dp(16f) - k * dp(11f), fy, dp(6f) - k * dp(1.2f), 3, -1.57f, Color.rgb(249, 115, 22))
            p.color = Color.rgb(34, 197, 94); c.drawCircle(fx, fy, dp(15f), p)
            p.color = Color.rgb(74, 222, 128); c.drawCircle(fx, fy, dp(11.5f), p)
            p.color = Color.rgb(187, 247, 208); c.drawCircle(fx + dp(13f), fy - dp(6f), dp(5.5f), p)
            txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(104f), dp(15f), Color.rgb(232, 121, 249), true, (min(1f, (t - 1.9f) / 0.6f) * 255).toInt().coerceIn(0, 255))
        }
        txt(c, "TAP TO SKIP", cx, h - dp(30f), dp(11f), Color.rgb(100, 116, 139), false, (120 + 80 * sin(time * 4f)).toInt().coerceIn(40, 200))
    }

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
        val pulse = 1f + 0.05f * sin(time * 1.6f) // #156 breathing scale
        p.color = Color.rgb(168, 85, 247); p.alpha = (52 + 24 * sin(time * 1.6f + 1.2f)).toInt().coerceIn(20, 90)
        c.drawCircle(cx, cy, dp(86f) * pulse, p)
        p.color = Color.rgb(34, 211, 238); p.alpha = (58 + 20 * sin(time * 1.6f)).toInt().coerceIn(24, 92)
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
            val satCol = arrayOf(Color.rgb(244, 114, 182), Color.rgb(52, 211, 153), Color.rgb(250, 204, 21))[i]
            for (k in 4 downTo 1) { // #157 easing orbit trails
                val ak = a - k * 0.15f
                p.alpha = 16 + (4 - k) * 12
                poly(c, cx + cos(ak) * dp(88f), cy + sin(ak) * dp(30f), dp(8f) * (1f - k * 0.16f), 4, 0.78f + time * 2f, satCol)
            }
            p.alpha = 255
            val ox = cx + cos(a) * dp(88f); val oy = cy + sin(a) * dp(30f)
            poly(c, ox, oy, dp(8f), 4, 0.78f + time * 2f, satCol)
        }
        // gentle gem rain behind the menu
        for (i in 0 until 9) {
            val fall = (time * (26f + i * 5f) + i * 233f) % (h + dp(90f))
            val gx = ((i * 0.618f + 0.07f) * w + sin(time * (0.8f + 0.7f * fall / (h + dp(90f))) + i) * (dp(10f) + dp(22f) * (fall / (h + dp(90f))))) // #158 sway grows as gems fall
            p.alpha = 90
            poly(c, gx, fall - dp(45f), dp(6f) + (i % 3) * dp(1.6f), 4, 0.78f + time * (1f + i * 0.2f), Color.rgb(52, 211, 153))
            p.alpha = 255
        }
        val shimmer = 0.5f + 0.5f * sin(time * 2.2f)
        val titleCol = Color.rgb(
            (165 + (216 - 165) * shimmer).toInt(),
            (243 + (180 - 243) * shimmer).toInt(),
            (252 + (254 - 252) * shimmer).toInt())
        (0 until 2).forEach { k -> p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = if (k == 0) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21); p.alpha = if (k == 0) 44 else 30; c.drawArc(RectF(cx - dp(128f) - k * dp(13f), cy + dp(96f) - dp(52f) - k * dp(9f), cx + dp(128f) + k * dp(13f), cy + dp(96f) + dp(52f) + k * dp(9f)), time * (26f - k * 17f) + k * 130f, 150f - k * 50f, false, p) }; p.style = Paint.Style.FILL; p.alpha = 255 // #797 twin rings orbit the menu title
        // #155 title letters stagger drop-in
        val titleStr = "GEMSTORM"
        p.typeface = android.graphics.Typeface.DEFAULT_BOLD; p.textSize = dp(40f)
        val lws = titleStr.map { p.measureText(it.toString()) }
        var lx = cx - lws.sum() / 2
        for (i in titleStr.indices) {
            val lt = ((screenT - 0.05f - i * 0.055f) / 0.4f).coerceIn(0f, 1f)
            val le = 1f - (1f - lt) * (1f - lt) * (1f - lt)
            p.textAlign = Paint.Align.LEFT; p.color = titleCol; p.alpha = (lt * 255).toInt().coerceIn(0, 255)
            c.drawText(titleStr[i].toString(), lx, cy + dp(108f) - (1f - le) * dp(52f), p)
            val sh5 = 0.5f + 0.5f * sin(time * 2.6f - i * 0.6f) // #545 gold shimmer travels the title
            p.color = Color.rgb(250, 204, 21); p.alpha = (lt * 120 * sh5).toInt().coerceIn(0, 120)
            c.drawText(titleStr[i].toString(), lx, cy + dp(108f) - (1f - le) * dp(52f), p)
            lx += lws[i]
        }
        p.alpha = 255
        val sweepX = ((time * 0.35f) % 1.6f - 0.3f) * w
        if (sweepX > cx - dp(160f) && sweepX < cx + dp(160f)) {
            p.color = Color.WHITE; p.alpha = 30
            c.drawRect(sweepX - dp(15f), cy + dp(74f), sweepX + dp(15f), cy + dp(114f), p); p.alpha = 255
        }
        (0 until 2).forEach { k -> val fx7 = cx + (if (k == 0) -1f else 1f) * (lws.sum() / 2 + dp(14f)); poly(c, fx7, cy + dp(96f), dp(5f), 4, time * 2.6f + k * 1.9f, Color.rgb(250, 204, 21)) } // #700 gold stars guard the title — the 700th upgrade
        p.color = Color.rgb(250, 204, 21); p.alpha = (40 + 30 * sin(time * 2.2f)).toInt().coerceIn(10, 70); c.drawRect(cx - lws.sum() / 2, cy + dp(116f), cx + lws.sum() / 2, cy + dp(117.5f), p); p.alpha = 255 // #725 a gold rule underlines the title
        (0 until 2).forEach { k -> poly(c, cx + (if (k == 0) -1f else 1f) * (lws.sum() / 2 + dp(8f)), cy + dp(116f), dp(3f), 4, time * 3.4f + k * 1.9f, Color.rgb(250, 204, 21)) } // #784 diamonds cap the title rule
        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)
        txt(c, "20 waves. Bosses. Loot. Survive the storm.", cx, cy + dp(166f), dp(13.5f), Color.rgb(203, 213, 225), false)
        // #349 high-score ticker scrolling under the title
        val tickStr = "🏆 BEST ${save.highScore}   •   💎 BANK ${save.gemBank}   •   🌊 BEST WAVE ${save.bestWave}   •   👑 VICTORIES ${save.victories}   •   ☠ KILLS ${save.totalKills}   •   "
        p.typeface = android.graphics.Typeface.DEFAULT_BOLD; p.textSize = dp(12.5f)
        val tickW = p.measureText(tickStr)
        var tickX = cx - ((time * dp(42f)) % tickW) - tickW
        p.textAlign = Paint.Align.LEFT; p.color = Color.rgb(148, 163, 184)
        roundRect(c, RectF(dp(10f), cy + dp(175f), w - dp(10f), cy + dp(199f)), dp(12f), Color.rgb(15, 8, 30), 120) // #798 the ticker scrolls on a backing pill
        while (tickX < w) { c.drawText(tickStr, tickX, cy + dp(192f), p); tickX += tickW }
        p.textAlign = Paint.Align.CENTER

        // loadout summary
        val ch = CHARS[save.selectedChar]; val wp = WEAPONS[save.selectedWeapon]
        val mAlpha = (min(1f, screenT / 0.45f) * 255).toInt().coerceIn(0, 255) // #153 fade with the stagger
        txt(c, "${ch.icon} ${ch.name}   •   ${wp.icon} ${wp.name}", cx, h * 0.485f, dp(15f), Color.rgb(250, 204, 21), true, mAlpha)
        txt(c, "💎 Bank: ${save.gemBank}      🏆 Best: ${save.highScore}      🌊 Best wave: ${save.bestWave}", cx, h * 0.485f + dp(24f), dp(13f), Color.rgb(148, 163, 184), true, mAlpha)
        if (save.dailyBest > 0) txt(c, "📅 Daily seed best: ${save.dailyBest}  (${save.dailyDate})", cx, h * 0.485f + dp(46f), dp(12f), Color.rgb(94, 234, 212))

        val compact = save.runSnap.isNotEmpty() || save.victories > 0
        val bw = w * 0.68f; val bh = if (compact) dp(50f) else dp(58f); val x0 = (w - bw) / 2
        val gap = if (compact) dp(10f) else dp(13f)
        var y0 = if (compact) h * 0.50f else h * 0.55f
        fun stag(i: Int, top: Float): Float { // #153 buttons stagger slide-in
            val lt = ((screenT - i * 0.07f) / 0.38f).coerceIn(0f, 1f)
            val e = 1f - (1f - lt) * (1f - lt)
            return top + (1f - e) * dp(26f)
        }
        menuButton(c, RectF(x0, stag(0, y0), x0 + bw, stag(0, y0) + bh), "▶", "ENTER THE ABYSS", Color.rgb(22, 163, 74), "play", 20f, Color.rgb(74, 222, 128))
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(74, 222, 128); p.alpha = (40 + 30 * sin(time * 2.6f)).toInt().coerceIn(10, 70); c.drawRoundRect(RectF(x0 - dp(3f), stag(0, y0) - dp(3f), x0 + bw + dp(3f), stag(0, y0) + bh + dp(3f)), dp(14f), dp(14f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #772 a halo breathes around ENTER THE ABYSS
        y0 += bh + gap
        if (save.runSnap.isNotEmpty()) { // #301 continue a saved run
            menuButton(c, RectF(x0, stag(1, y0), x0 + bw, stag(1, y0) + bh), "⏩", "CONTINUE — WAVE ${save.runSnapWave}", Color.rgb(180, 83, 9), "continue", 16f)
            y0 += bh + gap
        }
        menuButton(c, RectF(x0, stag(1, y0), x0 + bw, stag(1, y0) + bh), "🛒", "SHOP — HEROES & WEAPONS", Color.rgb(124, 58, 237), "shop", 16.5f)
        y0 += bh + gap
        if (save.victories > 0) { // #303 endless unlocked by a victory
            menuButton(c, RectF(x0, stag(2, y0), x0 + bw, stag(2, y0) + bh), "♾️", if (save.bestEndlessWave > 0) "ENDLESS — BEST 🌊 ${save.bestEndlessWave}" else "ENDLESS MODE", Color.rgb(157, 23, 77), "endlessmenu", 16f) // #347
            y0 += bh + gap
        }
        val half = (bw - dp(13f)) / 2
        menuButton(c, RectF(x0, stag(2, y0), x0 + half, stag(2, y0) + bh), "❓", "HOW TO", Color.rgb(30, 41, 59), "howto", 14f, 0, 21f)
        menuButton(c, RectF(x0 + half + dp(13f), stag(3, y0), x0 + bw, stag(3, y0) + bh), "📊", "RECORDS", Color.rgb(30, 41, 59), "stats", 14f, 0, 21f)
        y0 += bh + gap
        menuButton(c, RectF(x0, stag(4, y0), x0 + half, stag(4, y0) + bh), "📅", "DAILY", Color.rgb(13, 60, 60), "daily", 14f, 0, 21f)
        menuButton(c, RectF(x0 + half + dp(13f), stag(5, y0), x0 + bw, stag(5, y0) + bh), "⚙️", "SETTINGS", Color.rgb(30, 41, 59), "settings", 14f, 0, 21f)
        y0 += bh + gap
        menuButton(c, RectF(x0, stag(6, y0), x0 + half, stag(6, y0) + bh), "🦸", "HEROES", Color.rgb(30, 41, 59), "heroes", 14f, 0, 21f)
        menuButton(c, RectF(x0 + half + dp(13f), stag(7, y0), x0 + bw, stag(7, y0) + bh), "🔫", "WEAPONS", Color.rgb(30, 41, 59), "weapons", 14f, 0, 21f)
        if (time - lastTouchAt > 30f) { // #196 idle attract: demo ghost after 30s untouched
            val ga = time * 0.55f
            val gx3 = cx + cos(ga) * dp(96f); val gy3 = h * 0.36f + sin(ga * 1.7f) * dp(30f)
            for (k in 3 downTo 1) { p.alpha = 26 - k * 5; poly(c, gx3 - cos(ga) * k * dp(13f), gy3 - sin(ga * 1.7f) * k * dp(7f), dp(17f), 4, 0.78f + time, CHARS[save.selectedChar].color) }
            p.alpha = 95
            poly(c, gx3, gy3, dp(19f), 4, 0.78f + sin(time * 2f) * 0.2f, CHARS[save.selectedChar].color)
            p.alpha = 255
            txt(c, "✨ the storm awaits… ✨", cx, h * 0.47f, dp(14f), Color.rgb(232, 121, 249), true, (120 + 100 * sin(time * 2.4f)).toInt().coerceIn(30, 220))
        }
        p.color = Color.rgb(100, 116, 139); p.alpha = 60; c.drawRect(dp(24f), h - dp(33f), w - dp(24f), h - dp(32f), p); p.alpha = 255 // #799 a divider hairline rules the footer
        poly(c, cx, h - dp(32.5f), dp(3.4f), 4, time * 3f, Color.rgb(52, 211, 153))
        txtFitC(c, "💎 ${save.gemBank}  •  🌊 Best wave ${save.bestWave}  •  by QamoTech 💎N🎱  •  v2.6 — 800-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), w - dp(16f), false) // #337 footer chips
        (0 until 2).forEach { k -> poly(c, (if (k == 0) dp(14f) else w - dp(14f)), h - dp(18f), dp(3.2f), 4, time * 3f + k * 1.9f, Color.rgb(52, 211, 153)) } // #773 gems pin the menu footer corners
    }

    private fun screenTitle(c: Canvas, title: String) {
        txt(c, title, w / 2, dp(64f), dp(25f), Color.rgb(165, 243, 252), true)
        poly(c, w / 2 - dp(118f), dp(56f), dp(4.4f), 4, time * 2.4f, Color.rgb(250, 204, 21)) // #616 every screen title gets flanking gold sparks
        poly(c, w / 2 + dp(118f), dp(56f), dp(4.4f), 4, -time * 2.4f, Color.rgb(250, 204, 21))
        p.color = Color.rgb(250, 204, 21); p.alpha = 46; c.drawRect(w / 2 - dp(104f), dp(76f), w / 2 + dp(104f), dp(77.5f), p); p.alpha = 255 // #794 an underline sweeps under every screen title
        val swu = ((time * 0.3f) % 1.4f - 0.2f); if (swu in 0f..1f) { p.color = Color.WHITE; p.alpha = 120; c.drawRect(w / 2 - dp(104f) + dp(208f) * swu - dp(7f), dp(75.5f), w / 2 - dp(104f) + dp(208f) * swu + dp(7f), dp(78f), p); p.alpha = 255 }
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = 110 // #795 corner brackets frame every screen title
        for (sx in -1..1 step 2) for (sy in -1..1 step 2) { val bx2 = w / 2 + sx * dp(158f); val by2 = dp(56f) + sy * dp(26f); c.drawLine(bx2, by2, bx2 - sx * dp(12f), by2, p); c.drawLine(bx2, by2, bx2, by2 - sy * dp(12f), p) }
        p.style = Paint.Style.FILL; p.alpha = 255
        val bw = dp(110f)
        button(c, RectF(dp(14f), dp(26f), dp(14f) + bw, dp(26f) + dp(42f)), "◀ BACK", Color.rgb(30, 41, 59), Color.WHITE, 14f, "back")
    }

    private fun drawHowTo(c: Canvas) {
        screenTitle(c, "❓ HOW TO PLAY")
        if (howtoPage == 1) { drawDashDemo(c); drawHowToNav(c); return }
        if (howtoPage == 2) { drawEnemyGallery(c); drawHowToNav(c); return }
        if (howtoPage == 3) { drawUpgradeGallery(c); drawHowToNav(c); return }
        val hIn = min(1f, screenT / 0.32f); val hEase = 1f - (1f - hIn) * (1f - hIn) // #180 swipe slide transition
        c.save(); c.translate((1f - hEase) * w * 0.28f, 0f)
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
            "❤️ HEARTS" to "Elites and bosses sometimes drop a red heart that heals 15 HP. Rare — grab it!",
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
        c.restore()
        drawHowToNav(c)
    }

    private fun drawHowToNav(c: Canvas) {
        val by = h - dp(84f)
        button(c, RectF(dp(14f), by, dp(134f), by + dp(52f)), "◀ PREV", Color.rgb(30, 41, 59), Color.WHITE, 14f, "howtoprev")
        button(c, RectF(w - dp(134f), by, w - dp(14f), by + dp(52f)), "NEXT ▶", Color.rgb(30, 41, 59), Color.WHITE, 14f, "howtonext")
        button(c, RectF(w / 2 - dp(62f), by, w / 2 + dp(62f), by + dp(52f)), "🏠 MENU", Color.rgb(71, 85, 105), Color.WHITE, 13f, "back")
        txt(c, "PAGE ${howtoPage + 1} / 4", w / 2, by - dp(12f), dp(12f), Color.rgb(148, 163, 184), true)
    }

    // #324 interactive dash demo page
    private fun drawDashDemo(c: Canvas) {
        txt(c, "💨 DASH DEMO", w / 2, dp(150f), dp(19f), Color.rgb(250, 204, 21), true)
        txt(c, "Dashing makes you invincible for a split second.", w / 2, dp(182f), dp(13.5f), Color.rgb(203, 213, 225))
        txt(c, "Dodge through bullets, brutes and boss bursts!", w / 2, dp(206f), dp(13.5f), Color.rgb(203, 213, 225))
        val trackY = h * 0.40f
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(71, 85, 105)
        c.drawLine(dp(30f), trackY, w - dp(30f), trackY, p); p.style = Paint.Style.FILL
        val cyc = (time % 3f) / 3f
        val hx = when {
            cyc < 0.35f -> w * 0.12f + (cyc / 0.35f) * w * 0.2f
            cyc < 0.5f -> w * 0.32f + ((cyc - 0.35f) / 0.15f) * w * 0.42f
            else -> w * 0.74f + ((cyc - 0.5f) / 0.5f) * w * 0.14f
        }
        if (cyc in 0.35f..0.55f) for (g in 1..3) poly(c, hx - g * dp(16f), trackY, dp(17f), 4, 0.78f, Color.argb(120 - g * 30, 34, 211, 238))
        poly(c, w * 0.55f, trackY, dp(15f), 3, time * 2f, Color.rgb(239, 68, 68))
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(239, 68, 68); p.alpha = (60 + 45 * sin(time * 5f)).toInt().coerceIn(15, 105); c.drawCircle(w * 0.55f, trackY, dp(21f) * (1f + 0.08f * sin(time * 5f)), p); p.style = Paint.Style.FILL; p.alpha = 255 // #708 the demo hazard wears a warning ring
        if (cyc in 0.35f..0.5f) poly(c, hx - dp(24f), trackY, dp(9f) * (1f + 0.3f * sin(time * 31f)), 3, 3.14159f, Color.rgb(249, 115, 22)) // #709 a jet flame flickers behind the dashing gem
        poly(c, hx, trackY, dp(19f), 4, 0.78f, CHARS[save.selectedChar].color)
        poly(c, hx, trackY, dp(10f), 4, 0.78f, Color.rgb(168, 85, 247))
        txt(c, if (cyc in 0.35f..0.5f) "💨 DASH! invincible!" else "walk… then DASH through danger", w / 2, trackY + dp(64f), dp(14f), Color.rgb(165, 243, 252), true)
        txtL(c, "• Tap DASH (bottom corner) while moving", dp(24f), h * 0.56f, dp(13.5f), Color.rgb(226, 232, 240))
        txtL(c, "• Dash recharges — watch the pips under the button", dp(24f), h * 0.56f + dp(28f), dp(13.5f), Color.rgb(226, 232, 240))
        txtL(c, "• Blade upgrades let you cut THROUGH enemies mid-dash", dp(24f), h * 0.56f + dp(56f), dp(13.5f), Color.rgb(226, 232, 240))
    }

    // #326 upgrade gallery page: the full level-up catalog
    private fun drawUpgradeGallery(c: Canvas) {
        txt(c, "⭐ UPGRADE GALLERY", w / 2, dp(150f), dp(19f), Color.rgb(250, 204, 21), true)
        txt(c, "Every run offers 3 random picks per level — stack them all!", w / 2, dp(178f), dp(12.5f), Color.rgb(148, 163, 184))
        var y = dp(208f)
        for (i in 0..23) {
            val nm = UPGRADE_NAMES[i]
            if (i % 2 == 0) { p.color = Color.rgb(88, 28, 135); p.alpha = 22; c.drawRect(dp(16f), y - dp(15f), w - dp(16f), y + dp(8f), p); p.alpha = 255 } // #710 zebra tint steadies the gallery rows
            txt(c, nm.substringBefore(' '), dp(34f), y, dp(14f), Color.WHITE)
            txtL(c, nm.substringAfter(' '), dp(62f), y, dp(12.5f), Color.WHITE, true)
            txtFitL(c, UPGRADE_DESCS[i], dp(186f), y, dp(10.5f), Color.rgb(148, 163, 184), w - dp(10f))
            y += dp(25.5f)
        }
    }

    // #325 enemy gallery page
    private fun drawEnemyGallery(c: Canvas) {
        txt(c, "🧩 ENEMY GALLERY", w / 2, dp(150f), dp(19f), Color.rgb(250, 204, 21), true)
        val info = listOf(
            Triple(0, "Chaser", "Relentless — hunts you down"),
            Triple(1, "Dart", "Fast and fragile"),
            Triple(2, "Brute", "Slow tank, huge HP"),
            Triple(3, "Splitter", "Splits into darts on death"),
            Triple(6, "Weaver", "Sine-weave strafes"),
            Triple(7, "Lancer", "Telegraphs, then charges"),
            Triple(8, "Orbiter", "Circles you, fires orbs"),
            Triple(9, "Necroshard", "Births darts when slain"),
            Triple(10, "Bulwark Brute", "Frontal shield — flank it!"),
            Triple(11, "Treasure Gnome", "Flees — loot shower if caught")
        )
        var y = dp(196f)
        for ((t, name, trait) in info) {
            val sides = when (t) { 0 -> 3; 1, 6, 9 -> 4; 2, 10 -> 6; 3 -> 5; 7 -> 4; else -> 20 }
            poly(c, dp(44f), y - dp(6f), dp(13f), sides, time * 1.2f + t, enemyColor(t))
            txtL(c, name, dp(76f), y, dp(14.5f), Color.WHITE, true)
            txtFitL(c, trait, dp(216f), y, dp(12.5f), Color.rgb(148, 163, 184), w - dp(10f))
            y += dp(40f)
        }
        txt(c, "👑 Abyss Warden — boss every 5 waves", w / 2, y + dp(16f), dp(13.5f), Color.rgb(244, 114, 182), true)
        (0 until 2).forEach { k -> val sx3 = w / 2 + (if (k == 0) -1f else 1f) * dp(172f); poly(c, sx3, y + dp(7f), dp(4.2f), 4, time * 3.4f + k * 1.9f, Color.rgb(244, 114, 182)) } // #711 pink sparks flank the Warden's gallery line
    }

    private fun charCost(i: Int): Int {
        val base = CHARS[i].cost
        return if (save.heroDiscount && !save.ownsChar(i) && base > 0) (base * 0.75f).toInt() else base
    }

    // #344 animated entrance glow on pending confirm buttons
    private fun confirmGlow(c: Canvas, r: RectF, id: String) {
        if (pendingConfirm != id) return
        val a = (150 + 90 * sin(time * 9f)).toInt().coerceIn(60, 240)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.argb(a, 250, 204, 21)
        c.drawRoundRect(RectF(r.left - dp(3f), r.top - dp(3f), r.right + dp(3f), r.bottom + dp(3f)), dp(10f), dp(10f), p)
        p.style = Paint.Style.FILL
    }

    // compact half-width slider used by the controls grid (#374/#395/#396)
    private fun miniSlider(c: Canvas, r: RectF, frac: Float, col: Int, id: String) {
        val f = frac.coerceIn(0f, 1f)
        roundRect(c, r, dp(12f), Color.rgb(30, 20, 50))
        if (f > 0) roundRect(c, RectF(r.left, r.top, r.left + r.width() * f, r.bottom), dp(12f), col)
        val tr = dp(9f) + dp(4f) * (if (sliderDrag == id) sliderGrow else 0f)
        p.color = Color.WHITE; c.drawCircle(r.left + r.width() * f, r.centerY(), tr, p)
        p.color = col; c.drawCircle(r.left + r.width() * f, r.centerY(), tr * 0.55f, p)
        p.color = Color.WHITE; p.alpha = 220; c.drawCircle(r.left + r.width() * f - tr * 0.2f, r.centerY() - tr * 0.2f, tr * 0.16f, p); p.alpha = 255 // #720 slider thumbs catch a glint
    }

    private fun perkCost(key: String): Int = when (key) {
        "crit" -> 150 * (save.permCrit + 1)
        "magnet" -> 120 * (save.permMagnet + 1)
        "dash" -> 180 * (save.permDash + 1)
        "reroll" -> 80
        else -> 300
    }

    // #304 hero select screen + #306 detail card + #342 unlock hints
    private fun drawHeroes(c: Canvas) {
        screenTitle(c, "🦸 HEROES")
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)
        var y = dp(104f)
        for (i in CHARS.indices) {
            val chd = CHARS[i]; val owned = save.ownsChar(i); val sel = save.selectedChar == i; val cost = charCost(i)
            val r = RectF(dp(14f), y, w - dp(14f), y + dp(56f))
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 84) else Color.rgb(17, 12, 34), 235)
            p.color = chd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255
            if (sel) for (k in 0 until 2) { val sa3 = time * 2.6f + k * 3.1416f + i * 1.3f; poly(c, dp(40f) + cos(sa3) * dp(21f), y + dp(28f) + sin(sa3) * dp(21f), dp(2.8f), 4, sa3, chd.color) } // #654 the equipped hero's portrait sparkles
            txt(c, chd.icon, dp(40f), y + dp(34f), dp(17f), Color.WHITE)
            txtL(c, chd.name + if (sel) "  ✔ EQUIPPED" else "", dp(70f), y + dp(21f), dp(15.5f), Color.WHITE, true)
            txtFitL(c, if (owned || save.gemBank >= cost) chd.desc else "🔒 Earn 💎 ${cost - save.gemBank} more to unlock", dp(70f), y + dp(41f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
            val label = when { pendingConfirm == "char:$i" -> "❓ SURE?"; sel -> "✔"; owned -> "EQUIP"; else -> "💎 $cost" }
            val br = RectF(w - dp(118f), y + dp(10f), w - dp(24f), y + dp(46f))
            button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !owned && save.gemBank < cost -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12f, "char:$i")
            confirmGlow(c, br, "char:$i")
            y += dp(64f)
        }
        val chd = CHARS[save.selectedChar]
        y += dp(6f)
        roundRect(c, RectF(dp(14f), y, w - dp(14f), y + dp(148f)), dp(12f), Color.rgb(17, 12, 34), 235)
        txtFitL(c, "${chd.icon} ${chd.name} — ${chd.desc}", dp(28f), y + dp(25f), dp(13f), Color.rgb(250, 204, 21), w - dp(28f), true)
        fun bar(label: String, frac: Float, yy: Float, col: Int) {
            txtL(c, label, dp(28f), yy, dp(12f), Color.rgb(203, 213, 225), true)
            roundRect(c, RectF(dp(92f), yy - dp(9f), w - dp(120f), yy - dp(2f)), dp(3f), Color.rgb(30, 10, 40), 220)
            if (frac > 0) roundRect(c, RectF(dp(92f), yy - dp(9f), dp(92f) + (w - dp(212f)) * frac.coerceIn(0f, 1f), yy - dp(2f)), dp(3f), col)
            if (frac > 0) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(92f) + (w - dp(212f)) * frac.coerceIn(0f, 1f), yy - dp(5.5f), dp(1.8f), p); p.alpha = 255 } // #656 stat bars cap with a bright tip
        }
        bar("HP", chd.hp / 140f, y + dp(54f), Color.rgb(74, 222, 128))
        bar("SPD", chd.spd / 1.4f, y + dp(83f), Color.rgb(34, 211, 238))
        bar("DMG", chd.dmg / 1.2f, y + dp(112f), Color.rgb(244, 114, 182))
        txtL(c, "🌊 Best wave with ${chd.name}: ${save.bestWaveChar(save.selectedChar)}", dp(28f), y + dp(138f), dp(12f), Color.rgb(148, 163, 184))
        if (save.bestWaveChar(save.selectedChar) > 0) poly(c, dp(20f), y + dp(134f), dp(3.4f), 4, time * 3.4f, Color.rgb(96, 165, 250)) // #731 a wave spark marks your best run with this hero
        if (save.bestWaveChar(save.selectedChar) >= 10) poly(c, dp(20f), y + dp(134f), dp(5.4f), 4, -time * 2.6f, Color.rgb(250, 204, 21)) // #775 a gold ring honors a 10+ best wave
        poly(c, w - dp(34f), y + dp(134f), dp(3.6f), 4, time * 2.2f, Color.rgb(34, 211, 238)) // #661 a spark marks the hero's best-wave line
        button(c, RectF(dp(14f), h - dp(76f), w - dp(14f), h - dp(20f)), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 15f, "back")
    }

    // #305 weapon select screen + #307 DPS detail card + #343 unlock hints
    private fun drawWeapons(c: Canvas) {
        screenTitle(c, "🔫 WEAPONS")
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)
        var y = dp(104f)
        for (i in WEAPONS.indices) {
            val wpd = WEAPONS[i]; val owned = save.ownsWeapon(i); val sel = save.selectedWeapon == i
            val r = RectF(dp(14f), y, w - dp(14f), y + dp(56f))
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 84) else Color.rgb(17, 12, 34), 235)
            p.color = wpd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255
            if (sel) for (k in 0 until 2) { val sa4 = time * 2.6f + k * 3.1416f + i * 1.7f; poly(c, dp(40f) + cos(sa4) * dp(21f), y + dp(28f) + sin(sa4) * dp(21f), dp(2.8f), 4, sa4, wpd.color) } // #655 the equipped weapon's icon sparkles
            txt(c, wpd.icon, dp(40f), y + dp(34f), dp(17f), Color.WHITE)
            txtL(c, wpd.name + if (sel) "  ✔ EQUIPPED" else "", dp(70f), y + dp(21f), dp(15f), Color.WHITE, true)
            txtFitL(c, if (owned || save.gemBank >= wpd.cost) "DMG ${wpd.dmg.toInt()} • ${wpd.rate}/s • pierce ${wpd.pierce}" else "🔒 Earn 💎 ${wpd.cost - save.gemBank} more to unlock", dp(70f), y + dp(41f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
            val label = when { pendingConfirm == "weap:$i" -> "❓ SURE?"; sel -> "✔"; owned -> "EQUIP"; else -> "💎 ${wpd.cost}" }
            val br = RectF(w - dp(118f), y + dp(10f), w - dp(24f), y + dp(46f))
            button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !owned && save.gemBank < wpd.cost -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12f, "weap:$i")
            confirmGlow(c, br, "weap:$i")
            y += dp(64f)
        }
        val wpd = WEAPONS[save.selectedWeapon]
        y += dp(6f)
        roundRect(c, RectF(dp(14f), y, w - dp(14f), y + dp(118f)), dp(12f), Color.rgb(17, 12, 34), 235)
        txtFitL(c, "${wpd.icon} ${wpd.name} — ${wpd.desc}", dp(28f), y + dp(25f), dp(13f), Color.rgb(250, 204, 21), w - dp(28f), true)
        val dps = wpd.dmg * wpd.rate * wpd.pellets
        val maxDps = WEAPONS.maxOf { it.dmg * it.rate * it.pellets }
        txtL(c, "DPS", dp(28f), y + dp(54f), dp(12f), Color.rgb(203, 213, 225), true)
        roundRect(c, RectF(dp(92f), y + dp(45f), w - dp(120f), y + dp(52f)), dp(3f), Color.rgb(30, 10, 40), 220)
        roundRect(c, RectF(dp(92f), y + dp(45f), dp(92f) + (w - dp(212f)) * (dps / maxDps), y + dp(52f)), dp(3f), Color.rgb(244, 114, 182))
        p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(92f) + (w - dp(212f)) * (dps / maxDps), y + dp(48.5f), dp(1.8f), p); p.alpha = 255 // #657 the DPS bar caps with a bright tip
        txtFitL(c, "DPS ${dps.toInt()} • DMG ${wpd.dmg} • RATE ${wpd.rate}/s • PELLETS ${wpd.pellets} • PIERCE ${wpd.pierce}", dp(28f), y + dp(82f), dp(12f), Color.rgb(148, 163, 184), w - dp(28f))
        txtL(c, "Shop price: ${if (save.ownsWeapon(save.selectedWeapon)) "owned" else "💎 ${wpd.cost}"}", dp(28f), y + dp(106f), dp(12f), Color.rgb(148, 163, 184))
        if (!save.ownsWeapon(save.selectedWeapon) && save.gemBank >= wpd.cost) poly(c, w - dp(34f), y + dp(102f), dp(3.6f), 4, time * 3f, Color.rgb(52, 211, 153)) // #662 you can afford it — the price line sparkles
        button(c, RectF(dp(14f), h - dp(76f), w - dp(14f), h - dp(20f)), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 15f, "back")
    }

    // #338 first-run onboarding: 3-tip carousel
    private fun drawOnboard(c: Canvas) {
        screenTitle(c, "👋 WELCOME!")
        val tips = listOf(
            Triple("🕹️", "MOVE & DASH", "Drag anywhere to move — your weapon\nfires on its own. Tap DASH to dodge\nthrough danger, invincible for a blink."),
            Triple("💎", "GEMS ARE EVERYTHING", "Fallen foes drop gems: they fill your XP\nbar AND your bank. Level up, pick 1 of 3\nupgrades, and stack a build."),
            Triple("👑", "BOSSES & THE SHOP", "An Abyss Warden rises every 5 waves.\nSurvive all 20 to win! Banked gems unlock\nheroes, weapons & perks — forever.")
        )
        val (icon, title, body) = tips[onboardPage]
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (50 + 30 * sin(time * 2.6f)).toInt().coerceIn(20, 80); c.drawCircle(w / 2, h * 0.32f - dp(19f), dp(44f) * (1f + 0.04f * sin(time * 2.6f)), p); p.style = Paint.Style.FILL; p.alpha = 255 // #705 a gold halo breathes behind the welcome icon
        txt(c, icon, w / 2, h * 0.32f, dp(54f), Color.WHITE)
        txt(c, title, w / 2, h * 0.32f + dp(58f), dp(21f), Color.rgb(250, 204, 21), true)
        val blines = body.split("\n")
        for ((li, ln) in blines.withIndex()) txt(c, ln, w / 2, h * 0.32f + dp(94f) + li * dp(24f), dp(13.5f), Color.rgb(203, 213, 225))
        txt(c, "● ".repeat(onboardPage) + "●" + " ○".repeat(2 - onboardPage), w / 2, h * 0.60f, dp(14f), Color.rgb(148, 163, 184))
        button(c, RectF(w * 0.2f, h * 0.66f, w * 0.8f, h * 0.66f + dp(54f)), if (onboardPage < 2) "NEXT ▶" else "▶ START PLAYING", Color.rgb(22, 163, 74), Color.WHITE, 16f, "onbnext")
        if (onboardPage < 2) button(c, RectF(w * 0.2f, h * 0.66f + dp(66f), w * 0.8f, h * 0.66f + dp(120f)), "Skip intro", Color.rgb(30, 41, 59), Color.WHITE, 13.5f, "onbskip")
    }

    // #339 what's-new panel for v2.0 (returning players, once)
    private fun drawWhatsNew(c: Canvas) {
        screenTitle(c, "✨ WHAT'S NEW")
        var y = dp(150f)
        val items = listOf(
            "🎮 800 upgrades across every system in the game",
            "⏩ Continue Run — pause mid-run, resume from the menu",
            "🦸 Hero & Weapon screens with stat cards + unlock hints",
            "🎒 Shop tabs incl. Items — bank reroll tokens for a run",
            "🔊 Boss / shop / endless music loops + patterned haptics",
            "🏅 Victory ranks S/A/B/C, daily seeds & share codes",
            "🕹️ Joystick size + side, graphics quality, shake toggle"
        )
        for (t in items) { poly(c, dp(16f), y - dp(4f), dp(3.6f), 4, time * 2.4f, Color.rgb(250, 204, 21)); txtL(c, t, dp(26f), y, dp(13f), Color.rgb(226, 232, 240)); y += dp(30f) } // #706 gold bullets spin beside each What's New line
        button(c, RectF(w * 0.2f, y + dp(18f), w * 0.8f, y + dp(72f)), "▶ LET'S GO", Color.rgb(22, 163, 74), Color.WHITE, 16f, "wnok")
    }

    // #350 about page: version + credits
    private fun drawAbout(c: Canvas) {
        screenTitle(c, "ℹ️ ABOUT")
        var y = dp(150f)
        txt(c, "GEMSTORM 💎", w / 2, y, dp(26f), Color.rgb(165, 243, 252), true); y += dp(36f)
        (0 until 3).forEach { k -> val oa = time * 1.8f + k * 2.09f; poly(c, w / 2 + cos(oa) * dp(118f), dp(141f) + sin(oa) * dp(10f), dp(3.4f), 4, oa * 2f, Color.rgb(250, 204, 21)) } // #707 sparkles orbit the About title
        val ver = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "2.0.0" }
        txt(c, "Neon Abyss — version $ver (800-upgrade edition)", w / 2, y, dp(14f), Color.rgb(203, 213, 225)); y += dp(48f)
        (0 until 2).forEach { k -> val ax5 = w / 2 + (if (k == 0) -1f else 1f) * dp(158f); p.color = Color.rgb(250, 204, 21); p.alpha = (90 + 60 * sin(time * 3.6f + k * 2.2f)).toInt().coerceIn(30, 150); c.drawCircle(ax5, y - dp(53f), dp(1.8f), p) }; p.alpha = 255 // #777 dots flank the About version line
        txtL(c, "Built by Nathan Clark — QamoTech 💎N🎱", dp(28f), y, dp(14.5f), Color.WHITE, true); y += dp(28f)
        txtL(c, "Kotlin + Android Canvas. Zero dependencies.", dp(28f), y, dp(13.5f), Color.rgb(148, 163, 184)); y += dp(24f)
        txtL(c, "All music & SFX composed in-engine — synthesized, never sampled.", dp(28f), y, dp(13.5f), Color.rgb(148, 163, 184)); y += dp(24f)
        txtL(c, "Unlocks are earned with banked gems — no ads, no IAP, ever.", dp(28f), y, dp(13.5f), Color.rgb(148, 163, 184)); y += dp(48f)
        txt(c, "Thanks for playing the storm. 💎", w / 2, y, dp(14f), Color.rgb(250, 204, 21), true)
    }

    private fun drawShop(c: Canvas) {
        screenTitle(c, "🛒 SHOP")
        for (i in 0 until 6) { // #248 sparkle trail drifting across the shop header
            val sx2 = ((time * 60f + i * 150f) % (w + dp(60f))) - dp(30f)
            val sy2 = dp(96f) + sin(time * 2f + i * 1.3f) * dp(16f) + i * dp(7f)
            p.alpha = (120 + 100 * sin(time * 5f + i * 2f)).toInt().coerceIn(20, 220)
            poly(c, sx2, sy2, dp(5f), 4, time * 3f + i, Color.rgb(250, 204, 21))
        }
        p.alpha = 255
        p.color = Color.rgb(250, 204, 21); p.alpha = (26 + 18 * sin(time * 3f)).toInt().coerceIn(8, 44); c.drawCircle(w - dp(52f), dp(55f), dp(26f), p); p.alpha = 255 // #581 the gem bank glows softly
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)
        val tabW = (w - dp(178f)) / 4 // tabs + 3 gaps end before the sort toggle — no overlap
        val tabNames = arrayOf("🦸 HEROES", "🔫 WEAPONS", "✨ PERKS", "🎒 ITEMS")
        for (t in 0..3) {
            val tx = dp(14f) + t * (tabW + dp(8f))
            button(c, RectF(tx, dp(82f), tx + tabW, dp(130f)), tabNames[t], // #380
                if (shopTab == t) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "tab:$t")
            if (shopTab == t) { p.color = Color.rgb(250, 204, 21); p.alpha = (150 + 80 * sin(time * 4f)).toInt().coerceIn(70, 230); c.drawCircle(tx + tabW / 2, dp(122f), dp(2.4f), p); p.alpha = 255 } // #690 the active shop tab wears a gold bead
        }
        button(c, RectF(w - dp(132f), dp(82f), w - dp(14f), dp(130f)), if (save.shopSort) "↕ PRICE ✓" else "↕ PRICE", if (save.shopSort) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 12f, "sorttoggle") // #341 + #380
        if (save.shopSort) poly(c, w - dp(73f), dp(140f), dp(3.6f), 4, time * 3.6f, Color.rgb(250, 204, 21)) // #732 the price-sort toggle sparkles while active
        if (save.shopSort) poly(c, w - dp(73f), dp(152f), dp(2.4f), 4, -time * 3f, Color.rgb(196, 181, 253)) // #776 a violet twin sparkles under the sort toggle
        var y = dp(146f)
        if (shopTab == 0 && save.heroDiscount) {
            txt(c, "🏷️ BOSS BONUS: 25% OFF your next hero!", w / 2, y, dp(13f), Color.rgb(250, 204, 21), true)
            y += dp(24f)
        }
        when (shopTab) {
            0 -> {
                val charOrder = if (save.shopSort) CHARS.indices.sortedWith(compareBy({ if (save.ownsChar(it)) 1 else 0 }, { charCost(it) })) else CHARS.indices.toList() // #341
                for (i in charOrder) {
                    val chd = CHARS[i]
                    val cost = charCost(i)
                    val owned = save.ownsChar(i); val sel = save.selectedChar == i
                    val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
                    roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
                    roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
                    if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = chd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
                    p.color = chd.color; p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
                    if (!owned && save.gemBank >= cost) poly(c, r.right - dp(14f), r.top + dp(12f), dp(4f), 4, time * 3f, Color.rgb(52, 211, 153)) // #689 affordable heroes wink a gem in the corner
                    txt(c, chd.icon, dp(40f), y + dp(38f), dp(17f), Color.WHITE)
                    txtFitL(c, chd.name + if (sel) "  ✔ EQUIPPED" else "", dp(70f), y + dp(24f), dp(16f), Color.WHITE, w - dp(126f), true) // #340
                    txtFitL(c, "HP ${chd.hp.toInt()} • SPD ×${chd.spd} • DMG ×${chd.dmg} — ${chd.desc}", dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
                    val label = when { pendingConfirm == "char:$i" -> "❓ SURE?"; sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 $cost" }
                    val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
                    val affordC = owned || save.gemBank >= cost
                    button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !affordC -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12.5f, "char:$i")
                    confirmGlow(c, br, "char:$i")
                    y += dp(72f)
                }
            }
            1 -> {
                val weapOrder = if (save.shopSort) WEAPONS.indices.sortedWith(compareBy({ if (save.ownsWeapon(it)) 1 else 0 }, { WEAPONS[it].cost })) else WEAPONS.indices.toList() // #341
                for (i in weapOrder) {
                    val wpd = WEAPONS[i]
                    val owned = save.ownsWeapon(i); val sel = save.selectedWeapon == i
                    val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
                    roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
                    roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
                    if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = wpd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
                    p.color = wpd.color; p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
                    txt(c, wpd.icon, dp(40f), y + dp(38f), dp(17f), Color.WHITE)
                    txtFitL(c, wpd.name + if (sel) "  ✔ EQUIPPED" else "", dp(70f), y + dp(24f), dp(16f), Color.WHITE, w - dp(126f), true) // #340
                    txtFitL(c, "DMG ${wpd.dmg.toInt()} • ROF ${wpd.rate}/s — ${wpd.desc}", dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
                    val label = when { pendingConfirm == "weap:$i" -> "❓ SURE?"; sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 ${wpd.cost}" }
                    val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
                    val affordW = owned || save.gemBank >= wpd.cost
                    button(c, br, label, when { sel -> Color.rgb(71, 85, 105); !affordW -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12.5f, "weap:$i")
                    confirmGlow(c, br, "weap:$i")
                    y += dp(72f)
                }
            }
            2 -> {
                val perksBase = listOf(
                    Triple("🎯", "Crit Edge", "crit"),
                    Triple("🧲", "Magnet Coil", "magnet"),
                    Triple("💨", "Dash Servos", "dash")
                )
                val perks = if (save.shopSort) perksBase.sortedBy { perkCost(it.third) } else perksBase // sorter covers perks too
                for ((icon, name, key) in perks) {
                    val lvl = when (key) { "crit" -> save.permCrit; "magnet" -> save.permMagnet; else -> save.permDash }
                    val maxLv = if (key == "dash") 4 else 5
                    val cost = perkCost(key)
                    val desc = when (key) {
                        "crit" -> "+2% crit chance per stack — permanent"
                        "magnet" -> "+10% gem magnet range per stack — permanent"
                        else -> "−5% dash recharge per stack — permanent"
                    }
                    val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
                    roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
                    roundRect(c, r, dp(12f), Color.rgb(17, 12, 34), 235)
                    p.color = Color.rgb(168, 85, 247); p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
                    txt(c, icon, dp(40f), y + dp(38f), dp(17f), Color.WHITE)
                    txtFitL(c, "$name  " + "●".repeat(lvl) + "○".repeat(maxLv - lvl), dp(70f), y + dp(24f), dp(16f), Color.WHITE, w - dp(126f), true)
                    txtFitL(c, desc, dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
                    val label = when { pendingConfirm == "perk:$key" -> "❓ SURE?"; lvl >= maxLv -> "MAX ⭐"; else -> "💎 $cost" }
                    val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
                    val affordP = lvl >= maxLv || save.gemBank >= cost
                    button(c, br, label, when { lvl >= maxLv -> Color.rgb(71, 85, 105); !affordP -> Color.rgb(127, 29, 29); else -> Color.rgb(22, 163, 74) }, Color.WHITE, 12.5f, "perk:$key")
                    confirmGlow(c, br, "perk:$key")
                    y += dp(72f)
                }
            }
            else -> {
                // consumables (#145 revive + #311 banked reroll) — one loop so the price sorter covers Items too
                val items = listOf("revive", "reroll")
                val itemOrder = if (save.shopSort) items.sortedBy { if (it == "revive") 300 else 80 } else items
                for (item in itemOrder) {
                    val isRevive = item == "revive"
                    val cost = if (isRevive) 300 else 80
                    val count = if (isRevive) save.reviveTokens else save.bankedRerolls
                    val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
                    roundRect(c, RectF(r.left, r.top + dp(4f), r.right, r.bottom + dp(4f)), dp(12f), Color.BLACK, 100)
                    roundRect(c, r, dp(12f), Color.rgb(17, 12, 34), 235)
                    p.color = if (isRevive) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21); p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
                    txt(c, if (isRevive) "🪙" else "🎲", dp(40f), y + dp(38f), dp(17f), Color.WHITE)
                    txtFitL(c, if (isRevive) "Revive Token ×$count" else "Reroll Token ×$count", dp(70f), y + dp(24f), dp(16f), Color.WHITE, w - dp(126f), true)
                    txtFitL(c, if (isRevive) "Consumable — auto-revive at 60% HP when slain" else "Consumable — joins your next run's level-up rerolls", dp(70f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184), w - dp(126f))
                    val label = if (pendingConfirm == "perk:$item") "❓ SURE?" else "💎 $cost"
                    val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
                    button(c, br, label, if (save.gemBank >= cost) Color.rgb(22, 163, 74) else Color.rgb(127, 29, 29), Color.WHITE, 12.5f, "perk:$item")
                    confirmGlow(c, br, "perk:$item")
                    y += dp(72f)
                }
            }
        }
        txt(c, "Unlocks are permanent. Earn gems in every run — win or lose.", w / 2, y + dp(14f), dp(12f), Color.rgb(100, 116, 139))
        (0 until 2).forEach { k -> poly(c, w / 2 + (if (k == 0) -1f else 1f) * dp(182f), y + dp(10f), dp(3.2f), 4, time * 3f + k * 1.9f, Color.rgb(52, 211, 153)) } // #721 gems flank the shop footer
    }

    private fun drawSettings(c: Canvas) {
        screenTitle(c, "⚙️ SETTINGS")
        settingsScroll = settingsScroll.coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381
        c.save(); c.translate(0f, -settingsScroll)
        var y = dp(96f)
        txtL(c, "🎵 Music volume", dp(24f), y, dp(15f), Color.WHITE, true); y += dp(10f)
        musicSlider.set(dp(24f), y, w - dp(24f), y + dp(30f))
        roundRect(c, musicSlider, dp(15f), Color.rgb(30, 20, 50))
        if (save.musicVol > 0) roundRect(c, RectF(musicSlider.left, musicSlider.top, musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.bottom), dp(15f), Color.rgb(168, 85, 247))
        sliderGrow += ((if (sliderDrag != null) 1f else 0f) - sliderGrow) * 0.3f // #176 thumb grows while dragging
        val mThumbR = dp(10f) + dp(5f) * (if (sliderDrag == "music") sliderGrow else 0f)
        p.color = Color.rgb(168, 85, 247); p.alpha = 36; c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR + dp(5f), p); p.alpha = 255 // #718 the music thumb glows purple
        p.color = Color.WHITE; c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR, p)
        p.color = Color.rgb(168, 85, 247); c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR * 0.55f, p)
        txt(c, "${(save.musicVol * 100).toInt()}%${when { save.musicVol <= 0.01f -> " • OFF"; save.musicVol < 0.4f -> " • LOW"; save.musicVol < 0.75f -> " • MED"; else -> " • MAX" }}", w / 2, y + dp(20f), dp(12.5f), Color.WHITE, true)
        y += dp(58f)
        txtL(c, "🔊 Sound effects volume", dp(24f), y, dp(15f), Color.WHITE, true); y += dp(10f)
        sfxSlider.set(dp(24f), y, w - dp(24f), y + dp(30f))
        roundRect(c, sfxSlider, dp(15f), Color.rgb(30, 20, 50))
        if (save.sfxVol > 0) roundRect(c, RectF(sfxSlider.left, sfxSlider.top, sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.bottom), dp(15f), Color.rgb(34, 211, 238))
        val sThumbR = dp(10f) + dp(5f) * (if (sliderDrag == "sfx") sliderGrow else 0f)
        p.color = Color.rgb(34, 211, 238); p.alpha = 36; c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR + dp(5f), p); p.alpha = 255 // #719 the sfx thumb glows cyan
        p.color = Color.WHITE; c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR, p)
        p.color = Color.rgb(34, 211, 238); c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR * 0.55f, p)
        txt(c, "${(save.sfxVol * 100).toInt()}%${when { save.sfxVol <= 0.01f -> " • OFF"; save.sfxVol < 0.4f -> " • LOW"; save.sfxVol < 0.75f -> " • MED"; else -> " • MAX" }}", w / 2, y + dp(20f), dp(12.5f), Color.WHITE, true)
        y += dp(58f)
        val gridW = (w - dp(56f)) / 2 // #374/#395/#396 controls grid: size | sensitivity | opacity | button scale
        txtL(c, "🕹️ Stick size", dp(24f), y, dp(12.5f), Color.WHITE, true)
        txtL(c, "🎚 Sensitivity", dp(32f) + gridW, y, dp(12.5f), Color.WHITE, true); y += dp(8f)
        joySlider.set(dp(24f), y, dp(24f) + gridW, y + dp(24f))
        sensSlider.set(dp(32f) + gridW, y, w - dp(24f), y + dp(24f))
        miniSlider(c, joySlider, (save.joySize - 0.8f) / 0.5f, Color.rgb(250, 204, 21), "joy")
        miniSlider(c, sensSlider, (save.joySens - 0.7f) / 0.7f, Color.rgb(52, 211, 153), "sens")
        txt(c, "${(save.joySize * 100).toInt()}%", joySlider.centerX(), y + dp(17f), dp(11f), Color.WHITE, true)
        txt(c, "${(save.joySens * 100).toInt()}%", sensSlider.centerX(), y + dp(17f), dp(11f), Color.WHITE, true)
        y += dp(32f)
        txtL(c, "👻 Button opacity", dp(24f), y, dp(12.5f), Color.WHITE, true)
        txtL(c, "🔘 Button scale", dp(32f) + gridW, y, dp(12.5f), Color.WHITE, true); y += dp(8f)
        opacSlider.set(dp(24f), y, dp(24f) + gridW, y + dp(24f))
        btnSlider.set(dp(32f) + gridW, y, w - dp(24f), y + dp(24f))
        miniSlider(c, opacSlider, (save.btnOpacity - 0.35f) / 0.65f, Color.rgb(96, 165, 250), "opac")
        miniSlider(c, btnSlider, (save.btnScale - 0.8f) / 0.5f, Color.rgb(244, 114, 182), "btn")
        txt(c, "${(save.btnOpacity * 100).toInt()}%", opacSlider.centerX(), y + dp(17f), dp(11f), Color.WHITE, true)
        txt(c, "${(save.btnScale * 100).toInt()}%", btnSlider.centerX(), y + dp(17f), dp(11f), Color.WHITE, true)
        y += dp(30f)
        fun tgl(id: String, label: String, on: Boolean, knob: Boolean = true) {
            val r = RectF(dp(24f), y, w - dp(24f), y + dp(48f))
            button(c, r, label, Color.rgb(30, 41, 59), Color.WHITE, 14.5f, id)
            if (knob) drawKnob(c, r, id, on)
            y += dp(54f)
        }
        tgl("vibrate", "📳 Haptics: ${if (save.vibration) "ON" else "OFF"}", save.vibration)
        tgl("musictoggle", "🎵 Music: ${if (save.musicOn) "ON" else "OFF"}", save.musicOn)
        tgl("dmgtoggle", "💥 Damage numbers: ${if (save.dmgNumbers) "ON" else "OFF"}", save.dmgNumbers)
        tgl("timertoggle", "⏱ Run timer: ${if (save.showTimer) "ON" else "OFF"}", save.showTimer)
        tgl("hudtoggle", "🔠 Large HUD: ${if (save.bigHud) "ON" else "OFF"}", save.bigHud)
        tgl("hctoggle", "🌗 High contrast: ${if (save.highContrast) "ON" else "OFF"}", save.highContrast)
        val hardR = RectF(dp(24f), y, w - dp(24f), y + dp(48f)) // #380 targets >= 48dp
        button(c, hardR, "☠️ Hard mode: ${if (save.hardMode) "ON — tougher foes, +50% loot" else "OFF"}", if (save.hardMode) Color.rgb(127, 29, 29) else Color.rgb(30, 41, 59), Color.WHITE, 13f, "hardtoggle")
        drawKnob(c, hardR, "hardtoggle", save.hardMode)
        y += dp(54f)
        val halfW = (w - dp(56f)) / 2
        val gfxR = RectF(dp(24f), y, dp(24f) + halfW, y + dp(48f))
        button(c, gfxR, "✨ Graphics: ${when (save.gfxQuality) { 1 -> "HIGH"; 2 -> "LOW"; else -> "AUTO" }}", Color.rgb(30, 41, 59), Color.WHITE, 12f, "gfxtoggle")
        val grdR = RectF(dp(32f) + halfW, y, w - dp(24f), y + dp(48f))
        button(c, grdR, "⏸ Pause: ${if (save.pauseGuard) "HOLD" else "TAP"}", Color.rgb(30, 41, 59), Color.WHITE, 12f, "pauseguard") // #377
        y += dp(56f)
        val thirdW = (w - dp(64f)) / 3 // #375 joins shake + side as a third
        val shkR = RectF(dp(24f), y, dp(24f) + thirdW, y + dp(48f))
        button(c, shkR, "💥 Shake: ${if (save.shakeOn) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "shaketoggle")
        val sideR = RectF(dp(32f) + thirdW, y, dp(56f) + thirdW * 2, y + dp(48f))
        button(c, sideR, "🕹️ Side: ${if (save.joyLeft) "LEFT" else "RIGHT"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "joyside")
        val oneR = RectF(dp(64f) + thirdW * 2, y, w - dp(24f), y + dp(48f))
        button(c, oneR, "✋ 1-Hand: ${if (save.oneHanded) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "onehand")
        y += dp(56f)
        val aimW = (w - dp(56f)) / 2 // #358/#359 aim controls row
        val aimR = RectF(dp(24f), y, dp(24f) + aimW, y + dp(48f)) // #380
        button(c, aimR, "🎯 Aim: ${if (save.manualAim) "MANUAL 🎮" else "AUTO"}", Color.rgb(30, 41, 59), Color.WHITE, 12.5f, "aimtoggle")
        val astR = RectF(dp(32f) + aimW, y, w - dp(24f), y + dp(48f)) // #380
        button(c, astR, "🧲 Assist: ${when (save.aimAssist) { 2 -> "HIGH"; 1 -> "LOW"; else -> "OFF" }}", Color.rgb(30, 41, 59), Color.WHITE, 12.5f, "assisttoggle")
        y += dp(54f)
        txtFitL(c, "🎼 GEMSTORM by QamoTech — music composed in-engine (A minor pads • 126 BPM battle)", dp(24f), y, dp(12f), Color.rgb(148, 163, 184), w - dp(24f)); y += dp(19f) // #336 credits
        txtFitL(c, "Animations always run at full strength. No reduced-motion, ever — house rule. 💎", dp(24f), y, dp(12f), Color.rgb(250, 204, 21), w - dp(24f)); y += dp(19f)
        val armed = time < resetArmT // #319 double-confirm reset
        val rstW = (w - dp(64f)) / 3 // #397 controls defaults joins reset + about
        button(c, RectF(dp(24f), y, dp(24f) + rstW, y + dp(48f)), if (armed) "⚠️ ERASE?" else "🗑 Reset", if (armed) Color.rgb(185, 28, 28) else Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "reset")
        button(c, RectF(dp(32f) + rstW, y, dp(56f) + rstW * 2, y + dp(48f)), "🎛 Defaults", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "ctrlreset")
        button(c, RectF(dp(64f) + rstW * 2, y, w - dp(24f), y + dp(48f)), "ℹ️ About", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "about") // #350
        c.restore() // #381 end of scrolled content
        if (sliderDrag == "joy" || sliderDrag == "sens" || sliderDrag == "opac" || sliderDrag == "btn") { // #398 live controls preview while dragging
            val pvx = w / 2; val pvy = h - dp(168f)
            p.color = Color.rgb(34, 211, 238); p.alpha = (130 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(52f) * save.joySize, p)
            p.alpha = (238 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(24f) * save.joySize, p)
            p.color = Color.rgb(34, 211, 238); p.alpha = (238 * save.btnOpacity).toInt()
            c.drawCircle(pvx + dp(70f), pvy, dp(38f) * save.btnScale, p); p.alpha = 255
            txt(c, "💨", pvx + dp(70f), pvy + dp(8f), dp(20f), Color.WHITE)
            poly(c, pvx - dp(70f), pvy, dp(3.4f), 4, time * 3.8f, Color.rgb(250, 204, 21)) // #774 a gem spins on the preview stick
            txt(c, "🎛 LIVE PREVIEW", pvx, pvy + dp(84f), dp(12f), Color.rgb(250, 204, 21), true)
            (0 until 2).forEach { k -> poly(c, pvx + (if (k == 0) -1f else 1f) * dp(74f), pvy + dp(80f), dp(3f), 4, time * 3.2f + k * 1.9f, Color.rgb(250, 204, 21)) } // #722 sparks flank the live preview label
        }
    }


    private fun drawStats(c: Canvas) {
        screenTitle(c, "📊 RECORDS")
        val favW = (0 until WEAPONS.size).maxByOrNull { save.runsWeapon(it) } ?: 0 // #323 favorite weapon
        val rows = listOf(
            "🏆 High score" to "${save.highScore}",
            "🌊 Best wave" to "${save.bestWave} / $MAX_WAVE",
            "🌊 Best wave (${CHARS[save.selectedChar].name})" to "${save.bestWaveChar(save.selectedChar)} / $MAX_WAVE",
            "💎 Gem bank" to "${save.gemBank}",
            "☠ Lifetime kills" to "${save.totalKills}",
            "🏃 Total runs" to "${save.totalRuns}",
            "👑 Victories" to "${save.victories}",
            "📅 Daily best" to "${save.dailyBest}",
            "🔥 Best combo" to "${save.bestCombo}",
            "⭐ Elites slain" to "${save.elitesSlain}",
            "❤️ Hearts collected" to "${save.heartsCollected}",
            "⏱ Total playtime" to "${save.totalPlaytime / 3600}h ${(save.totalPlaytime % 3600) / 60}m",
            "🧑‍🚀 Hero" to "${CHARS[save.selectedChar].icon} ${CHARS[save.selectedChar].name}",
            "🔫 Weapon" to "${WEAPONS[save.selectedWeapon].icon} ${WEAPONS[save.selectedWeapon].name}",
            "💥 Damage dealt" to "${save.totalDamage}",
            "💎 Gems earned (lifetime)" to "${save.totalGemsEarned}",
            "🔫 Favorite weapon" to "${WEAPONS[favW].icon} ${WEAPONS[favW].name} (${save.runsWeapon(favW)} runs)"
        )
        var y = dp(100f)
        for ((ri, row) in rows.withIndex()) {
            val (k, v) = row
            roundRect(c, RectF(dp(16f), y - dp(24f), w - dp(16f), y + dp(12f)), dp(10f), Color.rgb(17, 12, 34), 220)
            p.color = CHARS[save.selectedChar].color; p.alpha = 90; c.drawRect(dp(16f), y - dp(24f), dp(19f), y + dp(12f), p); p.alpha = 255 // #713 stat cards wear a hero-color spine
            txtL(c, k, dp(28f), y, dp(15.5f), Color.rgb(226, 232, 240), true)
            txtR(c, v, w - dp(28f), y, dp(15.5f), Color.rgb(52, 211, 153), true)
            val barFrac = when (k) {
                "☠ Lifetime kills" -> min(1f, save.totalKills / 1000f)
                "👑 Victories" -> min(1f, save.victories / 10f)
                "💎 Gem bank" -> min(1f, save.gemBank / 2000f)
                else -> -1f
            }
            if (barFrac >= 0) {
                val bAnim = ((screenT - ri * 0.05f) / 0.6f).coerceIn(0f, 1f) // #181 bars animate to value
                val bEase = 1f - (1f - bAnim) * (1f - bAnim)
                roundRect(c, RectF(dp(28f), y + dp(5f), w - dp(28f), y + dp(9f)), dp(2f), Color.rgb(30, 10, 40), 200)
                if (barFrac > 0) roundRect(c, RectF(dp(28f), y + dp(5f), dp(28f) + (w - dp(56f)) * barFrac * bEase, y + dp(9f)), dp(2f), Color.rgb(52, 211, 153))
                if (barFrac > 0) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(28f) + (w - dp(56f)) * barFrac * bEase, y + dp(7f), dp(2.2f), p); p.alpha = 255 } // #714 stat bars cap with a bright tip
            }
            y += dp(38f)
        }
        // #320 runs by hero chart
        y += dp(4f)
        txtL(c, "🦸 Runs by hero", dp(28f), y + dp(8f), dp(15f), Color.rgb(250, 204, 21), true); y += dp(20f)
        val maxRuns = (0 until CHARS.size).maxOf { save.runsChar(it) }.coerceAtLeast(1)
        for (i in CHARS.indices) {
            val rc = save.runsChar(i)
            txtL(c, "${CHARS[i].icon} ${CHARS[i].name}", dp(28f), y, dp(13f), Color.rgb(226, 232, 240), true)
            roundRect(c, RectF(dp(150f), y - dp(10f), w - dp(90f), y - dp(3f)), dp(3f), Color.rgb(30, 10, 40), 200)
            if (rc > 0) roundRect(c, RectF(dp(150f), y - dp(10f), dp(150f) + (w - dp(240f)) * (rc.toFloat() / maxRuns), y - dp(3f)), dp(3f), CHARS[i].color)
            if (rc == maxRuns && rc > 0) poly(c, dp(150f) + (w - dp(240f)) + dp(8f), y - dp(6.5f), dp(3.2f), 4, time * 3f, Color.rgb(250, 204, 21)) // #715 your most-played hero's bar wears a gold star
            txtR(c, "$rc", w - dp(28f), y, dp(13f), Color.rgb(52, 211, 153), true)
            y += dp(22f)
        }
    }
}
