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
            "🩸 Vampire Core", "❄️ Frost Rounds")
        val UPGRADE_DESCS = arrayOf("+15% damage", "+15% fire rate", "+10% move speed", "+25 max HP, heal 40",
            "+45% gem magnet range", "+10% crit chance (crits deal 2×)", "+1 projectile per shot", "+0.9 HP/s regeneration",
            "Dash recharges 22% faster", "+1 armor (blocks 1 dmg per hit)",
            "Heal 1.5 HP on every kill", "Hits chill enemies: −22% speed for 1.5s (stacks)")
        const val MAX_WAVE = 20
    }

    // ---------- entities ----------
    class Enemy(val type: Int, var x: Float, var y: Float, var hp: Float, var maxHp: Float,
                val speed: Float, var radius: Float, var damage: Float, var xp: Int, var score: Int) {
        var slowT = 0f; var elite = false
        var flash = 0f; var shootT = 1.5f + Random.nextFloat() * 1.5f; var bladeT = 0f
        var wob = Random.nextFloat() * 6.28f; var chargeT = 0f; var vx = 0f; var vy = 0f
    }
    class Proj(var x: Float, var y: Float, var vx: Float, var vy: Float, val dmg: Float,
               var pierce: Int, val fromPlayer: Boolean, val radius: Float, val color: Int) { var life = 1.6f }
    class GemDrop(var x: Float, var y: Float, val value: Int) { var vx = 0f; var vy = 0f; var mag = false }
    class HeartDrop(var x: Float, var y: Float) { var vx = 0f; var vy = 0f; var mag = false }
    class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
                   var life: Float, val maxLife: Float, val color: Int, val size: Float)
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

    private val enemies = mutableListOf<Enemy>()
    private val projs = mutableListOf<Proj>()
    private val gemDrops = mutableListOf<GemDrop>()
    private val hearts = mutableListOf<HeartDrop>()
    private val particles = mutableListOf<Particle>()
    private val texts = mutableListOf<FloatText>()
    private val stars = mutableListOf<Star>()
    private val ghosts = mutableListOf<Ghost>()
    private val rings = mutableListOf<Ring>()

    // ---------- input ----------
    private var joyPointer = -1; private var joyOx = 0f; private var joyOy = 0f
    private var joyDx = 0f; private var joyDy = 0f
    private val dashBtnRect = RectF()
    private val pauseBtnRect = RectF()

    init {
        isFocusable = true
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
    private fun toMenu() {
        state = State.MENU
        audio.playMusic(R.raw.music_menu)
    }

    private fun startRun() {
        val ch = CHARS[save.selectedChar]
        val wp = WEAPONS[save.selectedWeapon]
        maxHp = ch.hp; hp = maxHp
        level = 1; xp = 0; xpNext = 6
        dmgMul = ch.dmg; rateMul = 1f; speedMul = ch.spd
        magnetR = 110f; critCh = 0.05f; regen = 0f; projBonus = 0; armor = 0; dashCdMax = 2.2f
        lifesteal = 0f; frostLvl = 0
        dashCd = 0f; dashT = 0f; invulnT = 0f; fireT = 0f
        wave = 0; score = 0; kills = 0; gemsRun = 0; runTime = 0f
        combo = 0; comboT = 0f; comboBest = 0; muzzleT = 0f
        shake = 0f; hurtFlash = 0f; runRecorded = false; newRecord = false
        enemies.clear(); projs.clear(); gemDrops.clear(); hearts.clear(); particles.clear(); texts.clear()
        ghosts.clear(); rings.clear()
        px = w / 2; py = h / 2; pvx = 0f; pvy = 0f
        joyPointer = -1; joyDx = 0f; joyDy = 0f
        state = State.PLAYING
        nextWave()
        audio.playMusic(R.raw.music_battle)
        audio.sfx("click")
    }

    private fun pauseGame() { if (state == State.PLAYING) { state = State.PAUSED; audio.playMusic(R.raw.music_menu) } }
    private fun resumeGame() { if (state == State.PAUSED) { state = State.PLAYING; audio.playMusic(R.raw.music_battle) } }

    private fun endRun(victory: Boolean) {
        if (!runRecorded) {
            newRecord = save.recordRun(score, wave, kills, gemsRun, victory)
            if (comboBest > save.bestCombo) save.bestCombo = comboBest
            runRecorded = true
        }
        burst(px, py, CHARS[save.selectedChar].color, 60, 520f)
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
        val spMul = 1f + (wave - 1) * 0.012f
        val type = if (forceType >= 0) forceType else {
            val roll = Random.nextFloat()
            when {
                wave >= 3 && roll < 0.14f -> 2  // brute
                wave >= 2 && roll < 0.34f -> 3  // splitter
                roll < 0.62f -> 1              // dart
                else -> 0                      // chaser
            }
        }
        val (sx, sy) = spawnPos()
        val e = when (type) {
            1 -> Enemy(1, sx, sy, 14f * hpMul, 14f * hpMul, 265f * spMul, dp(13f), 8f, 1, 15)
            2 -> Enemy(2, sx, sy, 95f * hpMul, 95f * hpMul, 92f * spMul, dp(30f), 22f, 3, 60)
            3 -> Enemy(3, sx, sy, 30f * hpMul, 30f * hpMul, 150f * spMul, dp(20f), 12f, 2, 30)
            4 -> Enemy(4, sx, sy, 9f * hpMul, 9f * hpMul, 235f * spMul, dp(10f), 6f, 1, 8)
            else -> Enemy(0, sx, sy, 26f * hpMul, 26f * hpMul, 168f * spMul, dp(17f), 12f, 1, 20)
        }
        // ⭐ ELITE: rare, huge, gold-ringed, drops triple loot
        if (wave >= 3 && Random.nextFloat() < 0.09f) {
            e.elite = true
            e.hp *= 2.6f; e.maxHp *= 2.6f; e.radius *= 1.35f
            e.xp *= 3; e.score *= 3; e.damage *= 1.3f
        }
        enemies.add(e)
    }

    private fun spawnBoss() {
        val hpMul = 1f + (wave - 1) * 0.24f
        val bossHp = 650f * hpMul * (1f + wave / 10f)
        val e = Enemy(5, w / 2, -80f, bossHp, bossHp, 105f, dp(46f), 30f, 25, 1000)
        enemies.add(e)
        texts.add(FloatText(w / 2, h * 0.3f, "⚠ ABYSS WARDEN ⚠", 2.2f, Color.rgb(244, 114, 182), dp(26f)))
    }

    // ---------- main tick ----------
    private fun tick(dt: Float) {
        time += dt
        if (state != State.PLAYING) {
            updateFx(dt)
            return
        }
        runTime += dt
        if (muzzleT > 0) muzzleT -= dt
        if (comboT > 0) { comboT -= dt; if (comboT <= 0) combo = 0 }
        if (waveBannerT > 0) waveBannerT -= dt
        if (shake > 0) shake = max(0f, shake - dt * 30f)
        if (hurtFlash > 0) hurtFlash = max(0f, hurtFlash - dt * 2.2f)
        if (dashCd > 0) dashCd -= dt
        if (dashT > 0) dashT -= dt
        if (invulnT > 0) invulnT -= dt
        if (regen > 0) hp = min(maxHp, hp + regen * dt)
        orbitAngle += dt * 3.4f

        // --- movement ---
        val spd = 335f * speedMul * if (dashT > 0) 3.1f else 1f
        pvx = joyDx * spd; pvy = joyDy * spd
        px = (px + pvx * dt).coerceIn(dp(20f), w - dp(20f))
        py = (py + pvy * dt).coerceIn(dp(90f), h - dp(20f))
        if (dashT > 0) ghosts.add(Ghost(px, py, 0.34f, CHARS[save.selectedChar].color))

        // --- spawning / wave flow ---
        if (toSpawn > 0) {
            spawnT -= dt
            if (spawnT <= 0) { spawnEnemy(); toSpawn--; spawnT = spawnInterval * (0.7f + Random.nextFloat() * 0.6f) }
        } else if (enemies.isEmpty()) {
            score += 50 * wave
            if (wave >= MAX_WAVE) { endRun(true); return }
            nextWave()
        }

        updateCombat(dt)
        updateFx(dt)

        if (hp <= 0f) { hp = 0f; endRun(false) }
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
                projs.add(Proj(px + cos(a) * dp(20f), py + sin(a) * dp(20f),
                    cos(a) * wp.projSpeed, sin(a) * wp.projSpeed,
                    wp.dmg * dmgMul, wp.pierce, true, dp(6f), wp.color))
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
        for (e in enemies) {
            if (e.flash > 0) e.flash -= dt
            if (e.slowT > 0) e.slowT -= dt
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
            val slowMul = if (e.slowT > 0) 1f - min(0.55f, 0.22f * frostLvl) else 1f
            if (abs2(e.vx, e.vy) > 1f) {
                e.x += e.vx * dt; e.y += e.vy * dt
                e.vx *= (1f - 3.2f * dt); e.vy *= (1f - 3.2f * dt)
            } else {
                e.x += mvx * e.speed * slowMul * dt; e.y += mvy * e.speed * slowMul * dt
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
            if (dist < e.radius + dp(17f) && invulnT <= 0 && dashT <= 0) {
                hurtPlayer(e.damage)
            }
        }

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
                        damageEnemy(e, pr.dmg, dead)
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
            if (combo % 10 == 0) texts.add(FloatText(px, py - dp(52f), "🔥 $combo COMBO ×$mult!", 0.9f, Color.rgb(251, 146, 60), dp(17f)))
            if (lifesteal > 0 && hp > 0) {
                hp = min(maxHp, hp + lifesteal)
                if (Random.nextFloat() < 0.3f) texts.add(FloatText(px, py - dp(34f), "+${lifesteal}", 0.55f, Color.rgb(248, 113, 113), dp(12f)))
            }
            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)
            audio.sfx(if (e.type == 2 || e.type == 5) "explosion" else "hit", 0.8f + Random.nextFloat() * 0.4f)
            if (e.type == 5) { shake = 16f; vibrate(90) }
            if (e.type == 5 || e.type == 2 || e.elite)
                rings.add(Ring(e.x, e.y, e.radius, 0.45f, 0.45f, if (e.elite) Color.rgb(250, 204, 21) else enemyColor(e.type)))
            if (e.type == 3) for (i in 0 until 2) { // splitter births minis
                val mini = Enemy(4, e.x + Random.nextFloat() * 30 - 15, e.y + Random.nextFloat() * 30 - 15,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 235f, dp(10f), 6f, 1, 8)
                enemies.add(mini)
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
            var remaining = e.xp
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

    private fun damageEnemy(e: Enemy, raw: Float, dead: MutableList<Enemy>) {
        if (e.hp <= 0) return
        if (frostLvl > 0) e.slowT = 1.5f
        val crit = Random.nextFloat() < critCh
        val dmg = if (crit) raw * 2f else raw
        e.hp -= dmg
        e.flash = 0.09f
        if (crit) texts.add(FloatText(e.x, e.y - e.radius, "CRIT ${dmg.toInt()}", 0.6f, Color.rgb(250, 204, 21), dp(15f)))
        if (e.hp <= 0 && !dead.contains(e)) dead.add(e)
    }

    private fun hurtPlayer(raw: Float) {
        val dmg = max(1f, raw - armor)
        hp -= dmg
        invulnT = 0.45f
        hurtFlash = 0.85f
        shake = max(shake, 7f)
        audio.sfx("hurt")
        vibrate(45)
        burst(px, py, Color.rgb(248, 113, 113), 10, 300f)
        texts.add(FloatText(px, py - dp(30f), "-${dmg.toInt()}", 0.7f, Color.rgb(248, 113, 113), dp(16f)))
    }

    private fun gainXp(v: Int) {
        xp += v
        while (xp >= xpNext) {
            xp -= xpNext
            level++
            xpNext = 6 + level * 4
            score += 25
            offers = (0..11).shuffled().take(3)
            state = State.LEVELUP
            audio.sfx("levelup")
            burst(px, py, Color.rgb(52, 211, 153), 30, 420f)
        }
    }

    private fun applyUpgrade(idx: Int) {
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
        }
        audio.sfx("click")
        state = State.PLAYING
    }

    private fun doDash() {
        if (dashCd > 0 || state != State.PLAYING) return
        dashCd = dashCdMax
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
        else -> Color.rgb(168, 85, 247) // boss purple
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
            id == "howto" -> { state = State.HOWTO; audio.sfx("click") }
            id == "shop" -> { state = State.SHOP; audio.sfx("click") }
            id == "settings" -> { state = State.SETTINGS; audio.sfx("click") }
            id == "stats" -> { state = State.STATS; audio.sfx("click") }
            id == "back" -> { toMenu(); audio.sfx("click") }
            id == "resume" -> resumeGame()
            id == "restart" -> startRun()
            id == "quit" -> { toMenu(); audio.sfx("click") }
            id == "vibrate" -> { save.vibration = !save.vibration; audio.sfx("click"); vibrate(30) }
            id.startsWith("char:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsChar(i)) { save.selectedChar = i; audio.sfx("click") }
                else if (save.gemBank >= CHARS[i].cost) { save.gemBank -= CHARS[i].cost; save.unlockChar(i); save.selectedChar = i; audio.sfx("levelup") }
                else audio.sfx("hurt")
            }
            id.startsWith("weap:") -> {
                val i = id.substringAfter(":").toInt()
                if (save.ownsWeapon(i)) { save.selectedWeapon = i; audio.sfx("click") }
                else if (save.gemBank >= WEAPONS[i].cost) { save.gemBank -= WEAPONS[i].cost; save.unlockWeapon(i); save.selectedWeapon = i; audio.sfx("levelup") }
                else audio.sfx("hurt")
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
        if (stroke != 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = stroke
            c.drawRoundRect(rect, dp(14f), dp(14f), p); p.style = Paint.Style.FILL
        }
        txt(c, label, rect.centerX(), rect.centerY() + size * 0.36f, dp(size), textColor)
        hits.add(HitBtn(RectF(rect), id))
    }

    private fun drawBackground(c: Canvas) {
        p.style = Paint.Style.FILL; p.color = Color.rgb(7, 3, 18)
        c.drawRect(0f, 0f, w, h, p)
        // neon grid
        p.color = Color.rgb(88, 28, 135); p.alpha = 46; p.strokeWidth = 1.5f; p.style = Paint.Style.STROKE
        val gap = dp(54f)
        val off = (time * 12f) % gap
        var gx = -gap + off
        while (gx < w) { c.drawLine(gx, 0f, gx, h, p); gx += gap }
        var gy = -gap + off
        while (gy < h) { c.drawLine(0f, gy, w, gy, p); gy += gap }
        p.alpha = 255; p.style = Paint.Style.FILL
        // stars
        for (s in stars) {
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 2f + s.phase))
            p.color = Color.rgb(165, 243, 252); p.alpha = (200 * tw).toInt()
            c.drawCircle(s.x, s.y, s.size, p)
        }
        p.alpha = 255
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        hits.clear()
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

        // gems
        for (g in gemDrops) {
            val big = g.value >= 5
            val r = if (big) dp(11f) else dp(7f)
            val bob = sin(time * 4f + g.x * 0.05f) * dp(2f)
            p.color = Color.rgb(52, 211, 153); p.alpha = 70
            c.drawCircle(g.x, g.y + bob, r * 1.9f, p); p.alpha = 255
            poly(c, g.x, g.y + bob, r, 4, 0.78f, Color.rgb(52, 211, 153))
            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))
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
            }
            // glow
            p.color = enemyColor(e.type); p.alpha = 46
            c.drawCircle(e.x, e.y, e.radius * 1.7f, p); p.alpha = 255
            when (e.type) {
                0 -> poly(c, e.x, e.y, e.radius, 3, atan2(py - e.y, px - e.x) + 1.57f, col)
                1 -> poly(c, e.x, e.y, e.radius, 4, time * 5f, col)
                2 -> poly(c, e.x, e.y, e.radius, 6, time * 0.8f, col)
                3 -> poly(c, e.x, e.y, e.radius, 4, time * 1.6f, col)
                4 -> poly(c, e.x, e.y, e.radius, 4, time * 3f, col)
                5 -> {
                    poly(c, e.x, e.y, e.radius, 8, time * 0.6f, col)
                    poly(c, e.x, e.y, e.radius * 0.62f, 8, -time * 0.9f, Color.rgb(233, 213, 255))
                    // crown spikes
                    for (i in 0 until 5) {
                        val a = -1.57f + (i - 2) * 0.5f
                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, dp(7f), 3, a, Color.rgb(250, 204, 21))
                    }
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
            c.drawCircle(pr.x, pr.y, pr.radius * 2.1f, p); p.alpha = 255
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
                poly(c, bx, by, dp(13f), 3, orbitAngle * 3f + k * 2f, Color.rgb(52, 211, 153))
            }
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
            if (muzzleT > 0) { // muzzle flash star at the barrel
                val mx = px + cos(lastAim) * dp(26f); val my = py + sin(lastAim) * dp(26f)
                poly(c, mx, my, dp(9f), 4, time * 22f, Color.rgb(254, 240, 138))
            }
            if (dashT > 0) { // dash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol; p.alpha = 160
                c.drawCircle(px, py, dp(30f) + (0.16f - dashT) * dp(120f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
        }

        // particles & texts
        for (pt in particles) {
            p.color = pt.color; p.alpha = (255 * (pt.life / pt.maxLife)).toInt().coerceIn(0, 255)
            c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, p)
        }
        p.alpha = 255
        for (t in texts) txt(c, t.text, t.x, t.y, t.size, t.color, true, (255 * min(1f, t.life * 2)).toInt().coerceIn(0, 255))
        c.restore()

        // hurt vignette
        if (hurtFlash > 0) {
            p.color = Color.rgb(220, 38, 38); p.alpha = (70 * hurtFlash).toInt()
            c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        }

        drawHud(c)

        // wave banner
        if (waveBannerT > 0 && state == State.PLAYING) {
            val a = (min(1f, waveBannerT) * 255).toInt()
            txt(c, if (bossWave) "⚠ WAVE $wave — BOSS ⚠" else "WAVE $wave", w / 2, h * 0.3f, dp(30f),
                if (bossWave) Color.rgb(244, 114, 182) else Color.rgb(165, 243, 252), true, a)
            val sub = when {
                wave == 1 -> "Survive the storm!"
                bossWave -> "Bring down the Abyss Warden!"
                wave == MAX_WAVE -> "Final wave — end this!"
                else -> "${toSpawn + enemies.size} hostiles inbound"
            }
            txt(c, sub, w / 2, h * 0.3f + dp(34f), dp(15f), Color.WHITE, false, a)
        }
    }

    private fun drawHud(c: Canvas) {
        val m = dp(14f)
        // HP bar
        val barW = w * 0.42f; val barH = dp(15f)
        roundRect(c, RectF(m, m, m + barW, m + barH), dp(7f), Color.rgb(30, 10, 40), 210)
        val hpFrac = (hp / maxHp).coerceIn(0f, 1f)
        val hpCol = when { hpFrac > 0.5f -> Color.rgb(74, 222, 128); hpFrac > 0.25f -> Color.rgb(250, 204, 21); else -> Color.rgb(248, 113, 113) }
        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)
        txtL(c, "HP ${hp.toInt()}/${maxHp.toInt()}", m + dp(6f), m + barH - dp(3.5f), dp(10.5f), Color.WHITE, true)
        // XP bar
        val xpY = m + barH + dp(5f)
        roundRect(c, RectF(m, xpY, m + barW, xpY + dp(9f)), dp(4f), Color.rgb(30, 10, 40), 210)
        val xpFrac = (xp.toFloat() / xpNext).coerceIn(0f, 1f)
        if (xpFrac > 0) roundRect(c, RectF(m, xpY, m + barW * xpFrac, xpY + dp(9f)), dp(4f), Color.rgb(52, 211, 153))
        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), Color.rgb(52, 211, 153), true)
        if (combo >= 3) { // 🔥 combo meter with decay bar
            val cmult = 1 + min(combo / 10, 4)
            txtL(c, "🔥 COMBO $combo  ×$cmult SCORE", m, xpY + dp(30f), dp(14f), Color.rgb(251, 146, 60), true)
            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f, xpY + dp(41f)), dp(2f), Color.rgb(30, 10, 40), 200)
            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(41f)), dp(2f), Color.rgb(251, 146, 60))
        }
        // wave + score + gems
        txt(c, "WAVE $wave/$MAX_WAVE", w / 2, m + dp(16f), dp(16f), Color.rgb(233, 213, 255), true)
        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(14f), Color.WHITE, true)
        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f), Color.rgb(165, 243, 252))
        // boss bar
        val boss = enemies.firstOrNull { it.type == 5 }
        if (boss != null) {
            val bw = w * 0.7f; val bx = (w - bw) / 2; val by = m + dp(46f)
            roundRect(c, RectF(bx, by, bx + bw, by + dp(13f)), dp(6f), Color.rgb(30, 10, 40), 220)
            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))
            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)
        }
        // pause button
        pauseBtnRect.set(w - m - dp(40f), m, w - m, m + dp(40f))
        roundRect(c, pauseBtnRect, dp(10f), Color.rgb(40, 16, 70), 200)
        txt(c, "❚❚", pauseBtnRect.centerX(), pauseBtnRect.centerY() + dp(7f), dp(15f), Color.WHITE)
        // joystick
        if (state == State.PLAYING) {
            if (joyPointer != -1) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = 130
                c.drawCircle(joyOx, joyOy, dp(52f), p)
                p.style = Paint.Style.FILL; p.alpha = 170; p.color = Color.rgb(34, 211, 238)
                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f), p); p.alpha = 255
            } else {
                txtL(c, "◀ drag to move", m, h - dp(96f), dp(12f), Color.rgb(148, 163, 184))
            }
            // dash button
            val dr = dp(38f)
            dashBtnRect.set(w - m - dr * 2, h - dp(110f) - dr * 2, w - m, h - dp(110f))
            val ready = dashCd <= 0
            p.color = if (ready) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105); p.alpha = 190
            c.drawCircle(dashBtnRect.centerX(), dashBtnRect.centerY(), dr, p); p.alpha = 255
            if (!ready) {
                p.color = Color.rgb(7, 3, 18); p.alpha = 130
                val sweep = dashCd / dashCdMax
                c.drawArc(dashBtnRect, -90f, 360f * sweep, true, p); p.alpha = 255
            }
            txt(c, "💨", dashBtnRect.centerX(), dashBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)
            txt(c, "DASH", dashBtnRect.centerX(), dashBtnRect.bottom + dp(15f), dp(10f), Color.rgb(165, 243, 252), true)
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
            button(c, r, "", Color.rgb(30, 14, 56), Color.WHITE, 16f, "up:$oi", Color.rgb(52, 211, 153))
            txt(c, UPGRADE_NAMES[oi], r.centerX(), y0 + dp(38f), dp(19f), Color.WHITE, true)
            txt(c, UPGRADE_DESCS[oi], r.centerX(), y0 + dp(66f), dp(13.5f), Color.rgb(165, 243, 252), false)
            y0 += cardH + dp(14f)
        }
    }

    private fun drawPaused(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 205; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
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
        // hero gem — big rotating diamond with glow pulse
        val cx = w / 2; val cy = h * 0.21f
        val pulse = 1f + 0.06f * sin(time * 2.4f)
        p.color = Color.rgb(168, 85, 247); p.alpha = 46
        c.drawCircle(cx, cy, dp(86f) * pulse, p)
        p.color = Color.rgb(34, 211, 238); p.alpha = 60
        c.drawCircle(cx, cy, dp(64f) * pulse, p); p.alpha = 255
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
        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)
        txt(c, "20 waves. Bosses. Loot. Survive the storm.", cx, cy + dp(166f), dp(13.5f), Color.rgb(203, 213, 225), false)

        // loadout summary
        val ch = CHARS[save.selectedChar]; val wp = WEAPONS[save.selectedWeapon]
        txt(c, "${ch.icon} ${ch.name}   •   ${wp.icon} ${wp.name}", cx, h * 0.485f, dp(15f), Color.rgb(250, 204, 21), true)
        txt(c, "💎 Bank: ${save.gemBank}      🏆 Best: ${save.highScore}      🌊 Best wave: ${save.bestWave}", cx, h * 0.485f + dp(24f), dp(13f), Color.rgb(148, 163, 184))

        val bw = w * 0.68f; val bh = dp(58f); val x0 = (w - bw) / 2
        var y0 = h * 0.55f
        button(c, RectF(x0, y0, x0 + bw, y0 + bh), "▶  ENTER THE ABYSS", Color.rgb(22, 163, 74), Color.WHITE, 20f, "play", Color.rgb(74, 222, 128))
        y0 += bh + dp(13f)
        button(c, RectF(x0, y0, x0 + bw, y0 + bh), "🛒  SHOP — HEROES & WEAPONS", Color.rgb(124, 58, 237), Color.WHITE, 16.5f, "shop")
        y0 += bh + dp(13f)
        val half = (bw - dp(13f)) / 2
        button(c, RectF(x0, y0, x0 + half, y0 + bh), "❓ HOW TO PLAY", Color.rgb(30, 41, 59), Color.WHITE, 15f, "howto")
        button(c, RectF(x0 + half + dp(13f), y0, x0 + bw, y0 + bh), "📊  RECORDS", Color.rgb(30, 41, 59), Color.WHITE, 15f, "stats")
        y0 += bh + dp(13f)
        button(c, RectF(x0, y0, x0 + bw, y0 + bh), "⚙️  SETTINGS", Color.rgb(30, 41, 59), Color.WHITE, 16f, "settings")
        txt(c, "by QamoTech 💎N🎱  •  v1.0", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), false)
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
            "💀 DEATH" to "You keep every banked gem. Upgrade, then run it again."
        )
        var y = dp(120f)
        for ((k, v) in lines) {
            txtL(c, k, dp(22f), y, dp(15.5f), Color.rgb(250, 204, 21), true)
            y += dp(21f)
            txtL(c, v, dp(22f), y, dp(13.5f), Color.rgb(226, 232, 240))
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
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
            if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = chd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
            txtL(c, "${chd.icon} ${chd.name}", dp(26f), y + dp(24f), dp(16f), Color.WHITE, true)
            txtL(c, "HP ${chd.hp.toInt()} • SPD ×${chd.spd} • DMG ×${chd.dmg} — ${chd.desc}", dp(26f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184))
            val label = when { sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 ${chd.cost}" }
            val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
            button(c, br, label, if (sel) Color.rgb(71, 85, 105) else Color.rgb(22, 163, 74), Color.WHITE, 12.5f, "char:$i")
            y += dp(72f)
        }
        txtL(c, "WEAPONS", dp(18f), y + dp(6f), dp(15f), Color.rgb(232, 121, 249), true); y += dp(14f)
        for (i in WEAPONS.indices) {
            val wpd = WEAPONS[i]
            val owned = save.ownsWeapon(i); val sel = save.selectedWeapon == i
            val r = RectF(dp(14f), y, w - dp(14f), y + dp(64f))
            roundRect(c, r, dp(12f), if (sel) Color.rgb(23, 37, 64) else Color.rgb(17, 12, 34), 235)
            if (sel) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = wpd.color; c.drawRoundRect(r, dp(12f), dp(12f), p); p.style = Paint.Style.FILL }
            txtL(c, "${wpd.icon} ${wpd.name}", dp(26f), y + dp(24f), dp(16f), Color.WHITE, true)
            txtL(c, "DMG ${wpd.dmg.toInt()} • ROF ${wpd.rate}/s — ${wpd.desc}", dp(26f), y + dp(46f), dp(11f), Color.rgb(148, 163, 184))
            val label = when { sel -> "✔ EQUIPPED"; owned -> "EQUIP"; else -> "💎 ${wpd.cost}" }
            val br = RectF(w - dp(118f), y + dp(13f), w - dp(24f), y + dp(51f))
            button(c, br, label, if (sel) Color.rgb(71, 85, 105) else Color.rgb(22, 163, 74), Color.WHITE, 12.5f, "weap:$i")
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
        txt(c, "${(save.musicVol * 100).toInt()}%", w / 2, y + dp(23f), dp(13f), Color.WHITE, true)
        y += dp(76f)
        txtL(c, "🔊 Sound effects volume", dp(24f), y, dp(16f), Color.WHITE, true); y += dp(12f)
        sfxSlider.set(dp(24f), y, w - dp(24f), y + dp(34f))
        roundRect(c, sfxSlider, dp(17f), Color.rgb(30, 20, 50))
        if (save.sfxVol > 0) roundRect(c, RectF(sfxSlider.left, sfxSlider.top, sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.bottom), dp(17f), Color.rgb(34, 211, 238))
        txt(c, "${(save.sfxVol * 100).toInt()}%", w / 2, y + dp(23f), dp(13f), Color.WHITE, true)
        y += dp(84f)
        val vibR = RectF(dp(24f), y, w - dp(24f), y + dp(54f))
        button(c, vibR, "📳 Vibration: ${if (save.vibration) "ON" else "OFF"}", Color.rgb(30, 41, 59), Color.WHITE, 16f, "vibrate")
        y += dp(90f)
        txtL(c, "🎼 Music composed in-engine for GEMSTORM:", dp(24f), y, dp(13.5f), Color.rgb(148, 163, 184)); y += dp(22f)
        txtL(c, "Menu — ambient pads in A minor • Battle — 126 BPM synthwave drive", dp(24f), y, dp(12.5f), Color.rgb(148, 163, 184)); y += dp(40f)
        txtL(c, "Animations always run at full strength. No reduced-motion, ever — house rule. 💎", dp(24f), y, dp(12.5f), Color.rgb(250, 204, 21))
    }

    private fun drawStats(c: Canvas) {
        screenTitle(c, "📊 RECORDS")
        val rows = listOf(
            "🏆 High score" to "${save.highScore}",
            "🌊 Best wave" to "${save.bestWave} / $MAX_WAVE",
            "💎 Gem bank" to "${save.gemBank}",
            "☠ Lifetime kills" to "${save.totalKills}",
            "🏃 Total runs" to "${save.totalRuns}",
            "👑 Victories" to "${save.victories}",
            "🔥 Best combo" to "${save.bestCombo}",
            "🧑‍🚀 Hero" to "${CHARS[save.selectedChar].icon} ${CHARS[save.selectedChar].name}",
            "🔫 Weapon" to "${WEAPONS[save.selectedWeapon].icon} ${WEAPONS[save.selectedWeapon].name}"
        )
        var y = dp(130f)
        for ((k, v) in rows) {
            roundRect(c, RectF(dp(16f), y - dp(24f), w - dp(16f), y + dp(12f)), dp(10f), Color.rgb(17, 12, 34), 220)
            txtL(c, k, dp(28f), y, dp(15.5f), Color.rgb(226, 232, 240), true)
            txtR(c, v, w - dp(28f), y, dp(15.5f), Color.rgb(52, 211, 153), true)
            y += dp(48f)
        }
    }
}
