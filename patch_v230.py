#!/usr/bin/env python3
"""GemStorm v2.3.0: wave-stuck fix, HUD rework, triangle removal, rockets, turtle hero, #449-472."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

# ============ A. WAVE-STUCK FIX ============
# A1 leash at the end of the enemy update loop
E("""            if (dist < e.radius + dp(17f) && invulnT <= 0 && dashT <= 0 && e.damage > 0) {
                hurtPlayer(e.damage)
                if (thornLvl > 0) damageEnemy(e, e.damage * 0.3f * thornLvl, dead) // #104 Thorn Aura
            }
        }""",
"""            if (dist < e.radius + dp(17f) && invulnT <= 0 && dashT <= 0 && e.damage > 0) {
                hurtPlayer(e.damage)
                if (thornLvl > 0) damageEnemy(e, e.damage * 0.3f * thornLvl, dead) // #104 Thorn Aura
            }
            // v2.3 WAVE-INTEGRITY LEASH: the gnome used to flee clean off the arena and
            // strand the wave forever (Nathan's Endless Wave 12). The gnome is now
            // penned inside the arena — catchable, killable, never a soft-lock — and
            // every other enemy is leashed to 70dp outside so knockbacks and lancer
            // charges can never lose one off-screen either.
            if (e.type == 11) { e.x = e.x.coerceIn(e.radius, w - e.radius); e.y = e.y.coerceIn(e.radius, h - e.radius) }
            else { e.x = e.x.coerceIn(-dp(70f), w + dp(70f)); e.y = e.y.coerceIn(-dp(70f), h + dp(70f)) }
        }""")
# A2 spawned-in-wave enemies count toward waveTotal (+ #467 pop ring)
E("""                enemies.add(mini)""",
"""                enemies.add(mini); waveTotal++ // v2.3 spawned mid-wave foes count toward the wave total
                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(6f), 0.25f, 0.25f, Color.rgb(74, 222, 128))) // #467 birth pop ring""", count=2)
# A3 boss minions count too
E("""                    texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "👹 MINIONS, RISE!", 1f, Color.rgb(244, 114, 182), dp(16f)))""",
"""                    waveTotal += 4 // v2.3 boss minions count toward the wave total
                    texts.add(FloatText(e.x, e.y - e.radius - dp(14f), "👹 MINIONS, RISE!", 1f, Color.rgb(244, 114, 182), dp(16f)))""")

# ============ B. HUD REWORK ============
E("""        val barW = w * 0.42f; val barH = dp(15f)""",
"""        val barW = w * 0.31f; val barH = dp(15f) // v2.3 HUD: three clean columns — the left bars never reach the center""")
E("""            if (combo >= 10) { // #448 hot-streak backlight
                p.color = Color.rgb(249, 115, 22); p.alpha = (36 + 16 * sin(time * 7f)).toInt().coerceIn(20, 52)
                c.drawRoundRect(RectF(m - dp(6f), xpY + dp(12f), m + barW * 0.62f, xpY + dp(34f)), dp(9f), dp(9f), p); p.alpha = 255
            }
            txtL(c, "🔥 COMBO $combo  ×$cmult SCORE", m, xpY + dp(30f), dp(14f) * (1f + 0.45f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""",
"""            if (combo >= 10) { // #448 hot-streak backlight
                p.color = Color.rgb(249, 115, 22); p.alpha = (36 + 16 * sin(time * 7f)).toInt().coerceIn(20, 52)
                c.drawRoundRect(RectF(m - dp(6f), xpY + dp(26f), m + barW * 0.62f, xpY + dp(48f)), dp(9f), dp(9f), p); p.alpha = 255
            }
            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""")
E("""            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f, xpY + dp(41f)), dp(2f), Color.rgb(30, 10, 40), 200)
            roundRect(c, RectF(m, xpY + dp(36f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(41f)), dp(2f), Color.rgb(251, 146, 60))
            txtL(c, "BEST $comboBest", m + barW * 0.6f + dp(10f), xpY + dp(41f), dp(10.5f), Color.rgb(148, 163, 184), true)""",
"""            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f, xpY + dp(55f)), dp(2f), Color.rgb(30, 10, 40), 200)
            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), Color.rgb(251, 146, 60))
            txtL(c, "BEST $comboBest", m + barW * 0.6f + dp(10f), xpY + dp(55f), dp(10.5f), Color.rgb(148, 163, 184), true)""")
E("""        txt(c, if (endlessMode) "WAVE $wave ♾️" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(16f), dp(16f) * hs, Color.rgb(233, 213, 255), true)""",
"""        txt(c, if (endlessMode) "WAVE $wave ∞" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(47f), dp(15f), Color.rgb(233, 213, 255), true) // v2.3 own row — collides with nothing""")
E("""        if (dailyMode) txt(c, "📅 DAILY SEED $dailyDay", w / 2, m + dp(33f), dp(10.5f) * hs, Color.rgb(94, 234, 212), true) // #334""",
"""        if (dailyMode) txt(c, "📅 DAILY SEED $dailyDay", w / 2, m + dp(93f), dp(10.5f), Color.rgb(94, 234, 212), true) // #334 (v2.3: moved to its own clear row)""")
E("""            roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 + dp(42f), m + dp(27f)), dp(2f), Color.rgb(30, 10, 40), 200)
            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(27f)), dp(2f), Color.rgb(52, 211, 153))""",
"""            roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), Color.rgb(30, 10, 40), 200)
            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))""")
E("""                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(23f), w / 2 + dp(42f), m + dp(27f)), dp(2f), dp(2f), p)""",
"""                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), dp(2f), p)""")
E("""        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(40f), dp(11.5f), Color.rgb(148, 163, 184))""",
"""        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(80f), dp(11.5f), Color.rgb(148, 163, 184))""")
E("""        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(14f) * hs, Color.WHITE, true)""",
"""        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f), Color.WHITE, true)""")
E("""            val bw = w * 0.7f; val bx = (w - bw) / 2; val by = m + dp(46f)""",
"""            val bw = w * 0.7f; val bx = (w - bw) / 2; val by = h - dp(148f) // v2.3 boss bar lives at the bottom now — the top stays clean""")

# ============ C. TRIANGLE REMOVAL ============
E("""            if (combo >= 25) for (k in 0 until 6) { // #240 kill-streak fire aura at combo 25+
                val fa = k * 1.047f + time * 1.8f; val fh = dp(13f) + dp(6f) * sin(time * 13f + k * 2.1f)
                poly(c, px + cos(fa) * dp(31f), py + sin(fa) * dp(31f) - fh * 0.4f, fh * 0.5f, 3, fa + 1.57f, if (k % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21))
            }""",
"""            if (combo >= 25) for (k in 0 until 6) { // kill-streak fire aura — orbiting orbs now, NOT triangles (Nathan's call, v2.3)
                val fa = k * 1.047f + time * 1.8f; val fh = dp(13f) + dp(6f) * sin(time * 13f + k * 2.1f)
                p.color = if (k % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21); p.alpha = 200
                c.drawCircle(px + cos(fa) * dp(31f), py + sin(fa) * dp(31f) - fh * 0.4f, fh * 0.42f, p); p.alpha = 255
            }""")
E("""                1 -> for (s in -1..1 step 2) poly(c, bpx - cos(lastAim) * dp(16f) + cos(lastAim + 1.57f) * dp(11f) * s, bpy - sin(lastAim) * dp(16f) + sin(lastAim + 1.57f) * dp(11f) * s, dp(6f), 3, lastAim - 1.57f + 0.5f * s, shade(chCol, 1.2f)) // Swift fins""",
"""                1 -> for (s in -1..1 step 2) { // Swift thruster pods (the old fins were triangles — removed v2.3 per Nathan)
                    val podX = bpx - cos(lastAim) * dp(16f) + cos(lastAim + 1.57f) * dp(11f) * s
                    val podY = bpy - sin(lastAim) * dp(16f) + sin(lastAim + 1.57f) * dp(11f) * s
                    p.color = shade(chCol, 1.2f); c.drawCircle(podX, podY, dp(5f), p)
                    p.color = Color.rgb(254, 240, 138); c.drawCircle(podX, podY, dp(2f), p)
                }""")

# ============ D. ROCKET LAUNCHER ============
E("""    private var dashCharges = 1; private var dashMaxCharges = 1; private var dashRechargeT = 0f""",
"""    private var dashCharges = 1; private var dashMaxCharges = 1; private var dashRechargeT = 0f
    private var rocketCharges = 3; private var rocketMaxCharges = 3; private var rocketRechargeT = 0f; private val rocketCdMax = 2.2f""")
E("""    private val dashBtnRect = RectF()""",
"""    private val dashBtnRect = RectF()
    private val rocketBtnRect = RectF()""")
E("""               var pierce: Int, val fromPlayer: Boolean, val radius: Float, val color: Int) { var life = 1.6f; var aoe = 0f }""",
"""               var pierce: Int, val fromPlayer: Boolean, val radius: Float, val color: Int) { var life = 1.6f; var aoe = 0f; var rocket = false }""")
E("""        dashMaxCharges = 1; dashCharges = 1; dashRechargeT = 0f""",
"""        dashMaxCharges = 1; dashCharges = 1; dashRechargeT = 0f
        rocketMaxCharges = 3; rocketCharges = 3; rocketRechargeT = 0f // 🚀 full rack every run""")
E("""        if (dashCharges < dashMaxCharges) {
            dashRechargeT -= dt
            if (dashRechargeT <= 0) { dashCharges++; dashReadyT = 0.35f; if (dashCharges < dashMaxCharges) dashRechargeT = dashCdMax } // #183
        }""",
"""        if (dashCharges < dashMaxCharges) {
            dashRechargeT -= dt
            if (dashRechargeT <= 0) { dashCharges++; dashReadyT = 0.35f; if (dashCharges < dashMaxCharges) dashRechargeT = dashCdMax; rings.add(Ring(px, py, dp(8f), 0.3f, 0.3f, Color.rgb(34, 211, 238))) } // #183 + #458 charge-ready ring
        }
        if (rocketCharges < rocketMaxCharges) { // 🚀 rocket rack recharges like the dash
            rocketRechargeT -= dt
            if (rocketRechargeT <= 0) { rocketCharges++; if (rocketCharges < rocketMaxCharges) rocketRechargeT = rocketCdMax }
        }""")
E("""    private fun coinBurst() { // #174 purchase coins fly to the gem counter""",
"""    private fun doRocket() { // 🚀 secondary weapon: homing rocket, big AoE
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

    private fun coinBurst() { // #174 purchase coins fly to the gem counter""")
E("""            pr.life -= dt
            pr.x += pr.vx * dt; pr.y += pr.vy * dt""",
"""            pr.life -= dt
            if (pr.rocket) { // 🚀 rockets home in and breathe smoke
                val rt = enemies.filter { it.hp > 0 }.minByOrNull { hyp(it.x - pr.x, it.y - pr.y) }
                if (rt != null) {
                    val cur = atan2(pr.vy, pr.vx); val want = atan2(rt.y - pr.y, rt.x - pr.x)
                    var dd = want - cur; while (dd > 3.14159f) dd -= 6.28318f; while (dd < -3.14159f) dd += 6.28318f
                    val turn = dd.coerceIn(-4.4f * dt, 4.4f * dt); val spd = hyp(pr.vx, pr.vy)
                    pr.vx = cos(cur + turn) * spd; pr.vy = sin(cur + turn) * spd
                }
                if (particles.size < 380) particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.3f, 0.3f, Color.rgb(148, 163, 184), dp(3.6f)))
            }
            pr.x += pr.vx * dt; pr.y += pr.vy * dt""")
E("""                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
            }""",
"""                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
            }
            if (pr.rocket) { // 🚀 rocket flame + hot nose
                val ra = atan2(pr.vy, pr.vx)
                poly(c, pr.x - cos(ra) * pr.radius * 1.7f, pr.y - sin(ra) * pr.radius * 1.7f, pr.radius * (0.9f + 0.25f * sin(time * 34f + pr.x)), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22))
                p.color = Color.rgb(254, 240, 138); c.drawCircle(pr.x + cos(ra) * pr.radius * 0.5f, pr.y + sin(ra) * pr.radius * 0.5f, pr.radius * 0.42f, p)
            }""")
E("""                        dashBtnRect.contains(x, y) -> doDash()""",
"""                        rocketBtnRect.contains(x, y) -> doRocket()
                        dashBtnRect.contains(x, y) -> doDash()""")
E("""            if (save.joyLeft) dashBtnRect.set(m, h - dby - dr * 2, m + dr * 2, h - dby) // #318 left-side layout
            else dashBtnRect.set(w - m - dr * 2, h - dby - dr * 2, w - m, h - dby)""",
"""            if (save.joyLeft) dashBtnRect.set(m, h - dby - dr * 2, m + dr * 2, h - dby) // #318 left-side layout
            else dashBtnRect.set(w - m - dr * 2, h - dby - dr * 2, w - m, h - dby)
            rocketBtnRect.set(dashBtnRect.left, dashBtnRect.top - dp(16f) - dr * 2, dashBtnRect.right, dashBtnRect.top - dp(16f)) // 🚀 stacked above dash""")
E("""            if (dashMaxCharges > 1) {
                for (ci in 0 until dashMaxCharges) {
                    p.color = if (ci < dashCharges) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105)
                    c.drawCircle(dashBtnRect.centerX() + (ci - (dashMaxCharges - 1) / 2f) * dp(15f), dashBtnRect.bottom + dp(28f), dp(4.5f), p)
                }
            }""",
"""            if (dashMaxCharges > 1) {
                for (ci in 0 until dashMaxCharges) {
                    p.color = if (ci < dashCharges) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105)
                    c.drawCircle(dashBtnRect.centerX() + (ci - (dashMaxCharges - 1) / 2f) * dp(15f), dashBtnRect.bottom + dp(28f), dp(4.5f), p)
                }
            }
            // 🚀 ROCKET button — the dash button's sibling, stacked above it
            val rdy = rocketCharges > 0
            p.color = if (rdy) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105); p.alpha = (190 * save.btnOpacity).toInt()
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
            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)
            txt(c, "ROCKET", rocketBtnRect.centerX(), rocketBtnRect.top - dp(5f), dp(10f), Color.rgb(254, 240, 138), true)
            for (ci in 0 until rocketMaxCharges) {
                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK part 1: {len(EDITS)} edits applied")
