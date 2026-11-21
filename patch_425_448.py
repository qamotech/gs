#!/usr/bin/env python3
"""GemStorm #425-448: 24 all-around graphics enhancements. Exact-anchor, count-asserted."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()

EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

# #425 nebula bright cores
E("""            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = 16
            c.drawCircle(nx, ny, dp(180f), p)""",
"""            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = 16
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = 12
            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core""")

# #426 shooting stars + #427 warm near stars (stars loop)
E("""            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 2f + s.phase))
            p.color = Color.rgb(165, 243, 252); p.alpha = (200 * tw).toInt()""",
"""            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(time * 2f + s.phase))
            p.color = if (s.phase > 3.14f) Color.rgb(254, 240, 138) else Color.rgb(165, 243, 252) // #427 half the sky burns warm
            p.alpha = (200 * tw).toInt()""")

E("""            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
        }
        p.alpha = 255""",
"""            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
        }
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
            p.color = Color.WHITE; p.alpha = (220 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(2.4f), p)
        }
        p.alpha = 255""")

# #428 engine trail sparks (tick, before the #195 gathering block)
E("""        if (stillT > 0.15f && particles.size < 330 && Random.nextFloat() < 0.55f) { // #195 gathering particles spiral in""",
"""        if ((pvx != 0f || pvy != 0f) && particles.size < 340 && Random.nextFloat() < 0.5f) { // #428 engine trail sparks
            val ba = atan2(pvy, pvx)
            particles.add(Particle(px - cos(ba) * dp(20f) + Random.nextFloat() * 8 - 4, py - sin(ba) * dp(20f) + Random.nextFloat() * 8 - 4, -cos(ba) * 40f, -sin(ba) * 40f, 0.34f, 0.34f, CHARS[save.selectedChar].color, dp(2.6f)))
        }
        if (stillT > 0.15f && particles.size < 330 && Random.nextFloat() < 0.55f) { // #195 gathering particles spiral in""")

# #432 ghost HP — (a) Enemy field
E("""        var slowT = 0f; var elite = false; var burnT = 0f; var spawnT = 0.32f; var frostAge = 0f""",
"""        var slowT = 0f; var elite = false; var burnT = 0f; var spawnT = 0.32f; var frostAge = 0f
        var ghostHp = maxHp // #432 damage-ghost bar starts full""")
# #432 (b) ghost drain in the enemy loop
E("""            if (e.flash > 0) e.flash -= dt""",
"""            if (e.flash > 0) e.flash -= dt
            if (e.ghostHp > e.hp) e.ghostHp = max(e.hp, e.ghostHp - e.maxHp * 0.3f * dt) // #432 ghost drains toward real HP""")
# #432 (c) ghost segment under the green bar
E("""                p.color = Color.rgb(30, 10, 40); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f), p)
                p.color = Color.rgb(74, 222, 128)""",
"""                p.color = Color.rgb(30, 10, 40); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f), p)
                if (e.ghostHp > e.hp) { p.color = Color.rgb(226, 232, 240); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.ghostHp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(5f), p) } // #432 recent-damage ghost
                p.color = Color.rgb(74, 222, 128)""")

# #433 brute rivet glint
E("""                        poly(c, pcx, pcy, e.radius * 0.36f, 6, pa, shade(col, 0.72f))
                        p.color = Color.rgb(15, 5, 20); c.drawCircle(pcx, pcy, dp(2f), p)""",
"""                        poly(c, pcx, pcy, e.radius * 0.36f, 6, pa, shade(col, 0.72f))
                        p.color = Color.rgb(15, 5, 20); c.drawCircle(pcx, pcy, dp(2f), p)
                        p.color = Color.WHITE; p.alpha = 170; c.drawCircle(pcx - dp(2.5f), pcy - dp(2.5f), dp(1.1f), p); p.alpha = 255 // #433 rivet glint""")

# #434 splitter brood blobs breathe
E("""e.radius * 0.21f, p) } // #416 splitter brood blobs""",
"""e.radius * 0.21f * (1f + 0.18f * sin(time * 7f + k * 2.1f)), p) } // #416 splitter brood blobs + #434 breathing pulse""")

# #435 orbiter satellite trails
E("""                    for (k in 0 until 2) { val sa = time * 3.4f + k * 3.14159f; poly(c, e.x + cos(sa) * e.radius * 1.5f, e.y + sin(sa) * e.radius * 1.5f, dp(4.6f), 4, sa * 2f, shade(col, 1.5f)) }""",
"""                    for (k in 0 until 2) { val sa = time * 3.4f + k * 3.14159f
                        for (tr2 in 1..2) { val ta2 = sa - tr2 * 0.22f; p.alpha = 90 - tr2 * 30; poly(c, e.x + cos(ta2) * e.radius * 1.5f, e.y + sin(ta2) * e.radius * 1.5f, dp(4.6f) * (1f - tr2 * 0.25f), 4, ta2 * 2f, shade(col, 1.5f)) } // #435 satellite trails
                        p.alpha = 255
                        poly(c, e.x + cos(sa) * e.radius * 1.5f, e.y + sin(sa) * e.radius * 1.5f, dp(4.6f), 4, sa * 2f, shade(col, 1.5f)) }""")

# #436 necroshard eye flare
E("""                    c.drawCircle(e.x - e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)
                    c.drawCircle(e.x + e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)""",
"""                    c.drawCircle(e.x - e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)
                    c.drawCircle(e.x + e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.075f, p)
                    if (e.shootT < 0.5f) { // #436 eyes flare red before a summon
                        p.color = Color.rgb(239, 68, 68); p.alpha = (150 + 90 * sin(time * 16f)).toInt().coerceIn(60, 240)
                        c.drawCircle(e.x - e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.045f, p)
                        c.drawCircle(e.x + e.radius * 0.11f, e.y - e.radius * 0.03f, e.radius * 0.045f, p); p.alpha = 255
                    }""")

# #437 lancer chevrons
E("""                val ta = atan2(py - e.y, px - e.x)
                c.drawLine(e.x, e.y, e.x + cos(ta) * dp(330f), e.y + sin(ta) * dp(330f), p)""",
"""                val ta = atan2(py - e.y, px - e.x)
                c.drawLine(e.x, e.y, e.x + cos(ta) * dp(330f), e.y + sin(ta) * dp(330f), p)
                p.style = Paint.Style.FILL // #437 charge chevrons race down the line
                for (cv2 in 1..3) {
                    val cd2 = (cv2 / 4f + (time * 1.4f) % 0.25f) * dp(330f)
                    poly(c, e.x + cos(ta) * cd2, e.y + sin(ta) * cd2, dp(5f), 3, ta + 1.57f, Color.rgb(248, 113, 113))
                }
                p.style = Paint.Style.STROKE""")

# #438 bulwark inner shield sheen
E("""                c.drawArc(RectF(e.x - e.radius * 1.15f, e.y - e.radius * 1.15f, e.x + e.radius * 1.15f, e.y + e.radius * 1.15f), fa - 55f, 110f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawArc(RectF(e.x - e.radius * 1.15f, e.y - e.radius * 1.15f, e.x + e.radius * 1.15f, e.y + e.radius * 1.15f), fa - 55f, 110f, false, p)
                p.strokeWidth = dp(1.6f); p.alpha = 90 // #438 inner shield sheen
                c.drawArc(RectF(e.x - e.radius * 0.98f, e.y - e.radius * 0.98f, e.x + e.radius * 0.98f, e.y + e.radius * 0.98f), fa - 55f, 110f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""")

# #439 player projectile halo
E("""            p.color = pr.color; p.alpha = 70
            c.drawCircle(pr.x, pr.y, pr.radius * (2.1f + 0.25f * sin(time * 14f + pr.x * 0.05f)), p); p.alpha = 255""",
"""            p.color = pr.color; p.alpha = 70
            c.drawCircle(pr.x, pr.y, pr.radius * (2.1f + 0.25f * sin(time * 14f + pr.x * 0.05f)), p); p.alpha = 255
            if (pr.fromPlayer) { // #439 wide soft halo on your shots
                p.color = pr.color; p.alpha = 26
                c.drawCircle(pr.x, pr.y, pr.radius * 3.4f, p); p.alpha = 255
            }""")

# #440 hostile orb throb
E("""            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)
            p.color = Color.WHITE
            c.drawCircle(pr.x, pr.y, pr.radius * 0.45f, p)""",
"""            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)
            p.color = Color.WHITE
            if (pr.fromPlayer) c.drawCircle(pr.x, pr.y, pr.radius * 0.45f, p)
            else { // #440 hostile orbs throb — readable in a swarm
                p.alpha = (170 + 85 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(85, 255)
                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
            }""")

# #429 thruster flame grows with speed
E("""                val ma = atan2(mvy2, mvx2); val flick = dp(5.5f) + dp(2.5f) * sin(time * 31f)""",
"""                val ma = atan2(mvy2, mvx2); val flick = (dp(5.5f) + dp(2.5f) * sin(time * 31f)) * (0.8f + 0.45f * min(1f, hyp(pvx, pvy) / 320f)) // #429 flame grows with speed""")

# #431 visor glint drift
E("""            p.color = Color.WHITE; c.drawCircle(visX + cos(lastAim) * dp(1.6f), visY + sin(lastAim) * dp(1.6f), dp(1.7f), p)""",
"""            p.color = Color.WHITE; c.drawCircle(visX + cos(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), visY + sin(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), dp(1.7f), p) // #431 living glint""")

# #430 level badge gold ring
E("""            // #34 level badge
            p.color = Color.rgb(124, 58, 237)
            c.drawCircle(px + dp(17f), py + dp(15f), dp(9f), p)
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)""",
"""            // #34 level badge
            p.color = Color.rgb(124, 58, 237)
            c.drawCircle(px + dp(17f), py + dp(15f), dp(9f), p)
            if (level >= 10) { // #430 gold ring for double-digit captains
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                c.drawCircle(px + dp(17f), py + dp(15f), dp(10f), p); p.style = Paint.Style.FILL
            }
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)""")

# #441 meteor crosshair ticks
E("""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(249, 115, 22); p.alpha = 200
            c.drawCircle(m.x, m.y, dp(72f) * (1f - frac) + dp(8f), p)""",
"""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(249, 115, 22); p.alpha = 200
            c.drawCircle(m.x, m.y, dp(72f) * (1f - frac) + dp(8f), p)
            for (tk in 0 until 4) { // #441 rotating crosshair ticks
                val tka = time * 2.4f + tk * 1.571f; val tkr = dp(72f) * (1f - frac) + dp(8f)
                c.drawLine(m.x + cos(tka) * (tkr - dp(5f)), m.y + sin(tka) * (tkr - dp(5f)), m.x + cos(tka) * (tkr + dp(5f)), m.y + sin(tka) * (tkr + dp(5f)), p)
            }""")

# #442 gem magnet comet tail
E("""            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }""",
"""            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }
            if (g.mag) { // #442 comet tail as gems rocket home
                val ga2 = atan2(py - g.y, px - g.x)
                p.color = Color.rgb(52, 211, 153); p.alpha = 90
                poly(c, g.x - cos(ga2) * r * 1.5f, g.y + bob - sin(ga2) * r * 1.5f, r * 0.55f, 4, 0.78f, Color.rgb(52, 211, 153))
                p.alpha = 255
            }""")

# #443 heart heartbeat ring
E("""            p.color = Color.rgb(74, 222, 128); p.alpha = 60
            c.drawCircle(hd.x, hd.y, r * 2f, p); p.alpha = 255""",
"""            p.color = Color.rgb(74, 222, 128); p.alpha = 60
            c.drawCircle(hd.x, hd.y, r * 2f, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(244, 114, 182) // #443 heartbeat ring
            p.alpha = (60 + 40 * sin(time * 6f + hd.x * 0.03f)).toInt().coerceIn(20, 100)
            c.drawCircle(hd.x, hd.y, r * (2.6f + 0.25f * sin(time * 6f + hd.x * 0.03f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255""")

# #444 kill shockwave ring
E("""            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)""",
"""            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)
            if (rings.size < 14) rings.add(Ring(e.x, e.y, e.radius * 0.7f, 0.32f, 0.32f, enemyColor(e.type))) // #444 kill shockwave""")

# #445 low-HP danger pulse
E("""        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)""",
"""        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)
        if (hpFrac in 0f..0.25f) { // #445 danger pulse on a dying bar
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f)
            p.color = Color.WHITE; p.alpha = (120 + 110 * sin(time * 9f)).toInt().coerceIn(10, 230)
            c.drawRoundRect(RectF(m, m, m + barW, m + barH), dp(7f), dp(7f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }""")

# #446 wave bar gold when nearly clear
E("""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(27f)), dp(2f), Color.rgb(52, 211, 153))""",
"""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(23f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(27f)), dp(2f), Color.rgb(52, 211, 153))
            if (remain in 1..3) { // #446 almost there — the bar burns gold
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                p.alpha = (140 + 100 * sin(time * 8f)).toInt().coerceIn(40, 240)
                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(23f), w / 2 + dp(42f), m + dp(27f)), dp(2f), dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")

# #447 boss bar phase ticks
E("""            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))""",
"""            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))
            p.color = Color.rgb(7, 3, 18); p.alpha = 160 // #447 phase ticks at the third marks
            c.drawRect(bx + bw / 3f - dp(1f), by + dp(1.5f), bx + bw / 3f + dp(1f), by + dp(11.5f), p)
            c.drawRect(bx + bw * 2f / 3f - dp(1f), by + dp(1.5f), bx + bw * 2f / 3f + dp(1f), by + dp(11.5f), p)
            p.alpha = 255""")

# #448 combo hot-streak backlight
E("""            txtL(c, "🔥 COMBO $combo  ×$cmult SCORE", m, xpY + dp(30f), dp(14f) * (1f + 0.45f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""",
"""            if (combo >= 10) { // #448 hot-streak backlight
                p.color = Color.rgb(249, 115, 22); p.alpha = (36 + 16 * sin(time * 7f)).toInt().coerceIn(20, 52)
                c.drawRoundRect(RectF(m - dp(6f), xpY + dp(12f), m + barW * 0.62f, xpY + dp(34f)), dp(9f), dp(9f), p); p.alpha = 255
            }
            txtL(c, "🔥 COMBO $combo  ×$cmult SCORE", m, xpY + dp(30f), dp(14f) * (1f + 0.45f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)

io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK: {len(EDITS)} edits applied")
