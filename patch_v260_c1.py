#!/usr/bin/env python3
"""GemStorm v2.6.0 part C1: heart rework (true bezier heart, pop heart removed)
+ features #785-792."""
P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# ---- fields ----
rep("""    private var waveClearBonus = 0; private var waveClearedNum = 0; private var waveTotal = 1""",
"""    private var waveClearBonus = 0; private var waveClearedNum = 0; private var waveTotal = 1
    private var waveHurt = false; private var waveStartRun = 0f // #786/#789 flawless + speed trackers
    private var gemPickups = 0; private var comboSaved = false // #790/#792
    private var introT = 0f // #793 opening sequence clock""",
"fields")

rep("""    private var heartPopT = 0f // v2.4: the one animated pickup heart""",
"""    private var heartPopT = 0f // v2.4: the one animated pickup heart (v2.6: no longer drawn — Nathan)
    private val heartPath = android.graphics.Path() // v2.6: true heart silhouette""",
"heartPath field")

# ---- drawHeart helper (before drawEnd) ----
rep("""    private fun drawEnd(c: Canvas, victory: Boolean) {""",
"""    private fun drawHeart(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        // v2.6: a TRUE heart silhouette — bezier curves, no triangle stuck on the bottom (Nathan)
        heartPath.reset()
        heartPath.moveTo(x, y + r * 0.95f)
        heartPath.cubicTo(x - r * 1.35f, y + r * 0.1f, x - r * 0.95f, y - r * 1.05f, x, y - r * 0.38f)
        heartPath.cubicTo(x + r * 0.95f, y - r * 1.05f, x + r * 1.35f, y + r * 0.1f, x, y + r * 0.95f)
        heartPath.close()
        p.color = color; c.drawPath(heartPath, p)
    }

    private fun drawEnd(c: Canvas, victory: Boolean) {""",
"drawHeart helper")

# ---- drop heart redrawn as a true heart ----
rep("""            p.color = Color.rgb(239, 68, 68)
            c.drawCircle(hd.x - r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            c.drawCircle(hd.x + r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            poly(c, hd.x, hd.y + r * 0.62f, r * 0.78f, 3, 3.14159f, Color.rgb(239, 68, 68))
            poly(c, hd.x, hd.y, r * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))""",
"""            drawHeart(c, hd.x, hd.y, r, Color.rgb(239, 68, 68)) // v2.6: one true heart, no triangle
            p.alpha = 130; drawHeart(c, hd.x - r * 0.16f, hd.y - r * 0.2f, r * 0.44f, Color.rgb(252, 165, 165)); p.alpha = 255 // #769 a pale inner heart glows inside every health drop""",
"drop heart")

# ---- the pop heart on the player is REMOVED ----
rep("""        if (heartPopT > 0) { // ❤️ ONE red animated heart on pickup (Nathan v2.4 — replaces the two text hearts)
            val hf = (heartPopT / 0.9f).coerceIn(0f, 1f)
            val hr = dp(13f) * (1.35f - 0.35f * hf) * (1f + 0.16f * sin(time * 16f)) // heartbeat pulse as it settles
            val hx3 = px; val hy3 = py - dp(48f) - dp(16f) * (1f - hf)
            p.color = Color.rgb(239, 68, 68); p.alpha = (255 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 255)
            c.drawCircle(hx3 - hr * 0.45f, hy3 - hr * 0.25f, hr * 0.55f, p)
            c.drawCircle(hx3 + hr * 0.45f, hy3 - hr * 0.25f, hr * 0.55f, p)
            poly(c, hx3, hy3 + hr * 0.62f, hr * 0.78f, 3, 3.14159f, Color.rgb(239, 68, 68))
            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            p.color = Color.rgb(185, 28, 28); p.alpha = (200 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 200); c.drawCircle(hx3, hy3 + hr * 0.18f, hr * 0.22f, p); p.alpha = 255 // #769 a deep-red core hearts the pickup pop
            (0 until 3).forEach { k -> val ha = time * 5f + k * 2.09f; p.color = Color.rgb(254, 202, 202); p.alpha = (170 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 170); c.drawCircle(hx3 + cos(ha) * hr * 1.25f, hy3 + sin(ha) * hr * 1.25f, dp(1.6f), p) }; p.alpha = 255 // #687 sparkles orbit the pickup heart
            if (hf in 0.35f..0.75f && particles.size < 370 && Random.nextFloat() < 0.3f) particles.add(Particle(hx3 + (Random.nextFloat() - 0.5f) * dp(20f), hy3, (Random.nextFloat() - 0.5f) * 40f, -30f, 0.4f, 0.4f, Color.rgb(252, 165, 165), dp(2.2f))) // #567 the pickup heart sheds sparkles at its peak
            p.alpha = 255
        }
""",
"""        // v2.6: the pickup heart that hovered on the player is GONE (Nathan) — the +15 HP text, ring and burst at the collect site still say it
""",
"pop heart removed")

# ---- #785 rocket impacts leave a fire pool ----
rep("""        if (pr.rocket) blooms.add(Bloom(pr.x, pr.y, 0.22f, dp(120f), Color.WHITE)) // #546 rocket impacts flash white-hot""",
"""        if (pr.rocket) blooms.add(Bloom(pr.x, pr.y, 0.22f, dp(120f), Color.WHITE)) // #546 rocket impacts flash white-hot
        if (pr.rocket && firePools.size < 42) firePools.add(FirePool(pr.x, pr.y, dp(44f), 2.2f)) // #785 rocket impacts leave the ground burning""",
"#785")

# ---- #786/#789 trackers: reset per wave, flag on real HP loss ----
rep("""        countT = 1.5f; countStage = 3 // #294 3-2-1 countdown before the wave pours in""",
"""        countT = 1.5f; countStage = 3 // #294 3-2-1 countdown before the wave pours in
        waveHurt = false; waveStartRun = runTime // #786/#789 a fresh flawless + speed clock each wave""",
"wave trackers")

rep("""        hp -= dmg
        val ne = enemies.minByOrNull { hyp(it.x - px, it.y - py) } // #163 knockback away from nearest foe""",
"""        hp -= dmg
        waveHurt = true // #786 real HP loss ends the flawless run
        val ne = enemies.minByOrNull { hyp(it.x - px, it.y - py) } // #163 knockback away from nearest foe""",
"waveHurt flag")

rep("""            score += waveClearBonus""",
"""            score += waveClearBonus
            if (!waveHurt && wave >= 2) { gemsRun += 25; texts.add(FloatText(w / 2, h * 0.36f, "✨ FLAWLESS WAVE! +25💎", 1.4f, Color.rgb(165, 243, 252), dp(17f))); audio.sfx("levelup") } // #786 clear a wave untouched, get paid
            if (wave >= 2 && runTime - waveStartRun < 25f) { gemsRun += 10; texts.add(FloatText(w / 2, h * 0.42f, "⚡ SPEED BONUS! +10💎", 1.2f, Color.rgb(250, 204, 21), dp(15f))) } // #789 clear it fast, get paid""",
"#786+#789")

# ---- #787 cryo shatter ----
rep("""        var dmg = if (crit) raw * critMul else raw // #110 crit damage multiplier""",
"""        var dmg = if (crit) raw * critMul else raw // #110 crit damage multiplier
        if (e.frostAge > 1.2f) { dmg *= 1.5f; if (Random.nextFloat() < 0.3f) texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "❄️ SHATTER!", 0.5f, Color.rgb(165, 243, 252), dp(11f))) } // #787 deep-frozen foes take +50% damage""",
"#787")

# ---- #788 elite bounty ----
rep("""            if (e.frostAge > 1.2f) burst(e.x, e.y, Color.rgb(165, 243, 252), 10, 300f) // #471 frozen kills shatter icy""",
"""            if (e.elite) for (i in 0 until 3) gemDrops.add(GemDrop(e.x + Random.nextFloat() * 50 - 25, e.y + Random.nextFloat() * 50 - 25, 5)) // #788 every elite pays a 3-gem bounty
            if (e.frostAge > 1.2f) burst(e.x, e.y, Color.rgb(165, 243, 252), 10, 300f) // #471 frozen kills shatter icy""",
"#788")

# ---- #790 gem rush ----
rep("""                gemsRun += g.value
                gainXp(g.value)""",
"""                gemsRun += g.value
                gemPickups++; if (gemPickups % 50 == 0) { gemDrops.add(GemDrop(px + dp(30f), py - dp(24f), 25)); texts.add(FloatText(px, py - dp(58f), "💎 GEM RUSH! +25", 1.0f, Color.rgb(52, 211, 153), dp(15f))) } // #790 every 50th gem drops a bonus
                gainXp(g.value)""",
"#790")

# ---- #792 combo saver ----
rep("""        if (comboT > 0 && dashT <= 0) { comboT -= dt; if (comboT <= 0) combo = 0 } // #132 decay pauses while dashing""",
"""        if (comboT > 0 && dashT <= 0) { comboT -= dt; if (comboT <= 0) { if (combo >= 75 && !comboSaved) { comboSaved = true; combo /= 2; comboT = 1.2f; texts.add(FloatText(px, py - dp(52f), "🔥 COMBO SAVED!", 0.9f, Color.rgb(251, 146, 60), dp(16f))); audio.sfx("sting", 1.2f) } else combo = 0 } } // #132 + #792 once a run, a 75+ combo halves instead of dying""",
"#792")

# ---- #791 share score ----
rep("""        button(c, RectF(x0, by, x0 + bw, by + bh), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "back")""",
"""        button(c, RectF(x0, by, x0 + bw * 0.48f, by + bh), "🏠  MENU", Color.rgb(71, 85, 105), Color.WHITE, 17f, "back")
        button(c, RectF(x0 + bw * 0.52f, by, x0 + bw, by + bh), "📤  SHARE", Color.rgb(8, 145, 178), Color.WHITE, 17f, "share") // #791 share your run""",
"#791 draw")

rep("""    private fun drawHeart(c: Canvas, x: Float, y: Float, r: Float, color: Int) {""",
"""    private fun shareScore() { // #791
        val msg = "💎 GEMSTORM — I scored $score on Wave $wave with $kills kills! Best combo ×$comboBest 🔥 Can you beat that?"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, msg) }
        try { context.startActivity(android.content.Intent.createChooser(intent, "Share your run")) } catch (ex: Exception) { toast("📤 Sharing isn't available here") }
    }

    private fun drawHeart(c: Canvas, x: Float, y: Float, r: Float, color: Int) {""",
"#791 fn")

rep("""            id == "restart" -> startRun()""",
"""            id == "restart" -> startRun()
            id == "share" -> { shareScore(); audio.sfx("click") } // #791""",
"#791 handler")

# ---- run-start resets for the new trackers ----
rep("""        combo = 0; comboT = 0f; comboBest = 0; muzzleT = 0f""",
"""        combo = 0; comboT = 0f; comboBest = 0; muzzleT = 0f; gemPickups = 0; comboSaved = false; waveHurt = false; waveStartRun = 0f""",
"run resets")

assert s != orig
open(P, "w").write(s)
print("patch_v260_c1 applied")
