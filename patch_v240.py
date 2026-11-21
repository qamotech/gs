#!/usr/bin/env python3
"""GemStorm v2.4.0: sweep band removed, turtle detail pass, enhancements #473-496."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

# ---- A. the annoying green bar: #197 sweep band + #218 riders REMOVED ----
E("""            val sweepX = w * (1f - waveClearT / 1.6f) // #197 banner sweep band
            p.color = Color.rgb(52, 211, 153); p.alpha = 26
            c.drawRect(sweepX - dp(30f), 0f, sweepX + dp(30f), h, p); p.alpha = 255
            for (i in 0 until 8) { // #218 sparkles riding the sweep
                val sy = ((i * 0.618f + (time * 0.35f + i * 0.13f) % 0.31f) % 1f) * h
                p.alpha = 150
                poly(c, sweepX + sin(time * 3f + i * 2f) * dp(20f), sy, dp(4.5f), 4, time * 4f + i, Color.rgb(167, 243, 208))
            }
            p.alpha = 255""",
"""            // #197/#218 REMOVED in v2.4 (Nathan's call): the full-height green sweep band
            // read as a weird opaque bar sliding across the screen. The CLEAR! text
            // and the #493 sparkle rain carry the moment now.""")

# ---- B. turtle detail pass ("720p") ----
E("""                    p.color = Color.rgb(71, 85, 105); c.drawCircle(jx, jy, dp(5.5f), p)
                    p.color = Color.rgb(148, 163, 184); c.drawCircle(jx, jy, dp(3f), p)""",
"""                    p.color = Color.rgb(71, 85, 105); c.drawCircle(jx, jy, dp(5.5f), p)
                    p.color = Color.rgb(148, 163, 184); c.drawCircle(jx, jy, dp(3f), p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(203, 213, 225); p.alpha = 190 // #477 metal band on the pod
                    c.drawCircle(jx, jy, dp(5.5f), p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                    p.color = Color.rgb(249, 115, 22); p.alpha = (140 + 90 * sin(time * 26f + s)).toInt().coerceIn(50, 230) // #477 nozzle glow
                    c.drawCircle(jx - cos(ta) * dp(6.5f), jy - sin(ta) * dp(6.5f), dp(2.4f), p); p.alpha = 255""")
E("""                    p.color = shade(chCol, 0.75f)
                    c.drawOval(RectF(bpx + fx2 * dp(15f) - dp(6f) + wig, bpy + fy2 * dp(13f) - dp(3.5f), bpx + fx2 * dp(15f) + dp(6f) + wig, bpy + fy2 * dp(13f) + dp(3.5f)), p)""",
"""                    p.color = shade(chCol, 0.75f)
                    c.drawOval(RectF(bpx + fx2 * dp(15f) - dp(6f) + wig, bpy + fy2 * dp(13f) - dp(3.5f), bpx + fx2 * dp(15f) + dp(6f) + wig, bpy + fy2 * dp(13f) + dp(3.5f)), p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.1f); p.color = shade(chCol, 0.5f); p.alpha = 150 // #478 flipper seam
                    c.drawLine(bpx + fx2 * dp(15f) - dp(3.6f) + wig, bpy + fy2 * dp(13f), bpx + fx2 * dp(15f) + dp(3.6f) + wig, bpy + fy2 * dp(13f), p)
                    p.style = Paint.Style.FILL; p.alpha = 255""")
E("""                p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)""",
"""                p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = shade(chCol, 0.28f); p.alpha = 210 // #475 crisp shell outline
                c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255
                p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                // #473 shell scute plates: seams from a center plate, plus #475 rim studs
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.3f); p.color = shade(chCol, 0.55f); p.alpha = 170
                for (k in 0 until 6) { val sa3 = k * 1.047f + 0.52f; c.drawLine(bpx + cos(sa3) * dp(8.5f), bpy + sin(sa3) * dp(8.5f), bpx + cos(sa3) * dp(15.8f), bpy + sin(sa3) * dp(15.8f), p) }
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, bpx, bpy, dp(8.5f) * spawnSc, 6, 0.52f, shade(chCol, 0.85f))
                p.color = shade(chCol, 1.35f)
                for (k in 0 until 8) { val sa4 = time * 0.6f + k * 0.785f; c.drawCircle(bpx + cos(sa4) * dp(19.2f), bpy + sin(sa4) * dp(19.2f), dp(1.3f), p) }
                // #474 dome gloss
                p.color = Color.WHITE; p.alpha = 46
                c.drawOval(RectF(bpx - dp(11f), bpy - dp(12.5f), bpx - dp(1f), bpy - dp(6.5f)), p); p.alpha = 255""")
E("""                for (s in -1..1 step 2) {
                    val ex = hx2 + cos(ta) * dp(3.4f) + cos(ta + 1.57f) * dp(3.6f) * s
                    val ey = hy2 + sin(ta) * dp(3.4f) + sin(ta + 1.57f) * dp(3.6f) * s
                    p.color = Color.rgb(8, 2, 18); c.drawCircle(ex, ey, dp(1.9f), p)
                    p.color = Color.WHITE; c.drawCircle(ex + dp(0.6f), ey - dp(0.6f), dp(0.8f), p)
                }""",
"""                for (s in -1..1 step 2) {
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
                c.drawCircle(hx2 - cos(ta + 1.57f) * dp(5.2f) - cos(ta) * dp(1.5f), hy2 - sin(ta + 1.57f) * dp(5.2f) - sin(ta) * dp(1.5f), dp(2.3f), p); p.alpha = 255""")
E("""                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, Color.rgb(34, 211, 238))""",
"""                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, if (waveClearT > 0) Color.rgb((127 + 127 * sin(time * 9f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 2.1f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 4.2f)).toInt().coerceIn(0, 255)) else Color.rgb(34, 211, 238)) // #494 victory rainbow gem""")

# ---- C. gameplay + HUD improvements ----
E("""            if (pr.rocket) { // 🚀 rocket flame + hot nose
                val ra = atan2(pr.vy, pr.vx)
                poly(c, pr.x - cos(ra) * pr.radius * 1.7f, pr.y - sin(ra) * pr.radius * 1.7f, pr.radius * (0.9f + 0.25f * sin(time * 34f + pr.x)), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22))""",
"""            if (pr.rocket) { // 🚀 rocket flame + hot nose
                val ra = atan2(pr.vy, pr.vx)
                for (tr4 in 1..2) { p.alpha = 64 - tr4 * 18; poly(c, pr.x - cos(ra) * pr.radius * (1.9f + tr4 * 1.5f), pr.y - sin(ra) * pr.radius * (1.9f + tr4 * 1.5f), pr.radius * (0.85f - tr4 * 0.18f), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22)) } // #480 exhaust ribbon
                p.alpha = 255
                poly(c, pr.x - cos(ra) * pr.radius * 1.7f, pr.y - sin(ra) * pr.radius * 1.7f, pr.radius * (0.9f + 0.25f * sin(time * 34f + pr.x)), 4, ra + 0.78f + 3.14159f, Color.rgb(249, 115, 22))""")
E("""    private var rocketCharges = 3; private var rocketMaxCharges = 3; private var rocketRechargeT = 0f; private val rocketCdMax = 2.2f""",
"""    private var rocketCharges = 3; private var rocketMaxCharges = 3; private var rocketRechargeT = 0f; private val rocketCdMax = 2.2f
    private var rocketReadyT = 0f // #481""")
E("""    private var lastAim = 0f; private var muzzleT = 0f""",
"""    private var lastAim = 0f; private var muzzleT = 0f
    private var bossGhostHp = 0f // #487""")
E("""    private var comboPop = 0f; private var killsPop = 0f; private var gemsPop = 0f""",
"""    private var comboPop = 0f; private var killsPop = 0f; private var gemsPop = 0f; private var scorePop = 0f // #492""")
E("""        if (dashReadyT > 0) dashReadyT -= dt""",
"""        if (dashReadyT > 0) dashReadyT -= dt
        if (rocketReadyT > 0) rocketReadyT -= dt // #481""")
E("""        if (rocketReadyT > 0) rocketReadyT -= dt // #481""",
"""        if (rocketReadyT > 0) rocketReadyT -= dt // #481
        val bb2 = enemies.firstOrNull { it.type == 5 } // #487 boss damage ghost trails the real bar
        if (bb2 != null) { if (bossGhostHp < bb2.hp) bossGhostHp = bb2.hp; if (bossGhostHp > bb2.hp) bossGhostHp = max(bb2.hp, bossGhostHp - bb2.maxHp * 0.12f * dt) } else bossGhostHp = 0f
        if (waveClearT > 0 && particles.size < 350 && Random.nextFloat() < 0.55f) particles.add(Particle(Random.nextFloat() * w, -dp(8f), 0f, 130f, 1.4f, 1.4f, if (Random.nextBoolean()) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), dp(2.6f))) // #493 victory sparkle rain""")
E("""        if (gemsPop > 0) gemsPop -= dt * 3f""",
"""        if (gemsPop > 0) gemsPop -= dt * 3f
        if (scorePop > 0) scorePop -= dt * 3f // #492""")
E("""            if (rocketRechargeT <= 0) { rocketCharges++; if (rocketCharges < rocketMaxCharges) rocketRechargeT = rocketCdMax }""",
"""            if (rocketRechargeT <= 0) { rocketCharges++; rocketReadyT = 0.35f; if (rocketCharges < rocketMaxCharges) rocketRechargeT = rocketCdMax } // #481""")
E("""            score += e.score * mult""",
"""            score += e.score * mult
            if (e.score >= 60) scorePop = 0.35f // #492 big kills pop the score""")
E("""            if (e.type == 11) { e.x = e.x.coerceIn(e.radius, w - e.radius); e.y = e.y.coerceIn(e.radius, h - e.radius) }""",
"""            if (e.type == 11) { val ox = e.x; val oy = e.y; e.x = e.x.coerceIn(e.radius, w - e.radius); e.y = e.y.coerceIn(e.radius, h - e.radius); if ((ox != e.x || oy != e.y) && Random.nextFloat() < 0.22f && sparks.size < 40) sparks.add(Spark(e.x, e.y - e.radius - dp(4f), 0.3f, Color.rgb(250, 204, 21), dp(7f))) } // #490 penned gnome bonks the wall""")
E("""            if (d < magnetR || vacuumT > 0) g.mag = true""",
"""            if (d < magnetR || vacuumT > 0) g.mag = true
            if (g.mag && sparks.size < 40 && Random.nextFloat() < 0.16f) sparks.add(Spark(g.x, g.y, 0.22f, Color.rgb(167, 243, 208), dp(6f))) // #485 magnet shimmer""")
E("""                texts.add(FloatText(px, py - dp(40f), "+15 HP 💚", 0.8f, Color.rgb(74, 222, 128), dp(15f)))
                audio.sfx("heart") // #266 warm chime
                burst(px, py, Color.rgb(74, 222, 128), 10, 260f)
                rings.add(Ring(px, py, dp(8f), 0.5f, 0.5f, Color.rgb(74, 222, 128))) // #168 expanding ring
                for (i in 0 until 4) { val sa = i * 1.571f + 0.4f; sparks.add(Spark(px + cos(sa) * dp(22f), py + sin(sa) * dp(22f), 0.3f, Color.rgb(74, 222, 128), dp(8f))) } // #215 heal sparkles
                texts.add(FloatText(px, py - dp(58f), "💚", 0.9f, Color.WHITE, dp(18f))) // #168 rising heart""",
"""                texts.add(FloatText(px, py - dp(40f), "+15 HP ❤️", 0.8f, Color.rgb(248, 113, 113), dp(15f))) // #482 red to match the heart
                audio.sfx("heart") // #266 warm chime
                burst(px, py, Color.rgb(239, 68, 68), 10, 260f)
                rings.add(Ring(px, py, dp(8f), 0.5f, 0.5f, Color.rgb(239, 68, 68))) // #168 expanding ring
                for (i in 0 until 4) { val sa = i * 1.571f + 0.4f; sparks.add(Spark(px + cos(sa) * dp(22f), py + sin(sa) * dp(22f), 0.3f, Color.rgb(252, 165, 165), dp(8f))) } // #215 heal sparkles
                texts.add(FloatText(px, py - dp(58f), "❤️", 0.9f, Color.WHITE, dp(18f))) // #168 rising heart""")
E("""        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))
        invulnT = max(invulnT, 0.22f)""",
"""        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))
        burst(px, py, CHARS[save.selectedChar].color, 8, 250f) // #495 dash-start spark burst
        invulnT = max(invulnT, 0.22f)""")
E("""            var bx3 = w / 2 - tw3 / 2""",
"""            roundRect(c, RectF(w / 2 - tw3 / 2 - dp(16f), h * 0.3f - dp(31f), w / 2 + tw3 / 2 + dp(16f), h * 0.3f + dp(13f)), dp(12f), Color.rgb(7, 3, 18), (a * 0.55f).toInt()) // #483 readability pill behind the banner
            var bx3 = w / 2 - tw3 / 2""")
E("""            txt(c, "💨 TAP DASH!", dashBtnRect.centerX(), dashBtnRect.top - dp(14f) - dp(4f) * dpulse, dp(13f), Color.rgb(250, 204, 21), true)
        }""",
"""            txt(c, "💨 TAP DASH!", dashBtnRect.centerX(), dashBtnRect.top - dp(14f) - dp(4f) * dpulse, dp(13f), Color.rgb(250, 204, 21), true)
        }
        if (save.totalRuns == 0 && rocketCharges >= rocketMaxCharges && enemies.any { hyp(it.x - px, it.y - py) < dp(220f) }) { // #484 first rocket prompt
            val rpulse = 0.5f + 0.5f * sin(time * 6f + 1f)
            txt(c, "🚀 TAP ROCKET!", rocketBtnRect.centerX(), rocketBtnRect.top - dp(24f) - dp(4f) * rpulse, dp(13f), Color.rgb(251, 146, 60), true)
        }""")
E("""        if (xpFrac > 0) roundRect(c, RectF(m, xpY, m + barW * xpFrac, xpY + dp(9f)), dp(4f), Color.rgb(52, 211, 153))""",
"""        if (xpFrac > 0) roundRect(c, RectF(m, xpY, m + barW * xpFrac, xpY + dp(9f)), dp(4f), if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153)) // #486 near-level gold
        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow""")
E("""            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), Color.rgb(251, 146, 60))""",
"""            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), if (comboT < 0.6f && (time * 10f).toInt() % 2 == 0) Color.rgb(248, 113, 113) else Color.rgb(251, 146, 60)) // #488 expiry flash""")
E("""        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f), Color.WHITE, true)""",
"""        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.WHITE, true) // #492""")
E("""            roundRect(c, RectF(bx, by, bx + bw, by + dp(13f)), dp(6f), Color.rgb(30, 10, 40), 220)
            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))""",
"""            roundRect(c, RectF(bx, by, bx + bw, by + dp(13f)), dp(6f), Color.rgb(30, 10, 40), 220)
            if (bossGhostHp > boss.hp) roundRect(c, RectF(bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by, bx + bw * (bossGhostHp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(233, 213, 255)) // #487 damage ghost
            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))""")
E("""        roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 200)
        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)""",
"""        roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 200)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(168, 85, 247); p.alpha = (50 + 40 * sin(time * 2.4f)).toInt().coerceIn(10, 90) // #496 idle pulse
        c.drawRoundRect(RectF(pvR.left - dp(2.5f), pvR.top - dp(2.5f), pvR.right + dp(2.5f), pvR.bottom + dp(2.5f)), dp(12f), dp(12f), p)
        p.style = Paint.Style.FILL; p.alpha = 255
        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)""")
E("""            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)""",
"""            if (rocketReadyT > 0) { // #481 charge-complete flash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(254, 240, 138)
                p.alpha = (220 * (rocketReadyT / 0.35f)).toInt().coerceIn(0, 220)
                c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.centerY(), dr + dp(10f) * (1f - rocketReadyT / 0.35f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)""")
E("""                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255""",
"""                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255
                p.color = Color.rgb(148, 163, 184); p.alpha = 60 // #489 smoke puffs trail the falling head
                c.drawCircle(m.x + off + dp(16f), my2 - dp(52f), dp(13f), p)
                p.alpha = 40
                c.drawCircle(m.x + off + dp(24f), my2 - dp(96f), dp(17f), p); p.alpha = 255""")

# ---- version strings ----
E("""v2.3 — 472-upgrade edition""", """v2.4 — 496-upgrade edition""")
E("""Neon Abyss — version $ver (472-upgrade edition)""", """Neon Abyss — version $ver (496-upgrade edition)""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK v2.4.0: {len(EDITS)} edits applied")
