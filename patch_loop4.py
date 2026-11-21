#!/usr/bin/env python3
"""GemStorm graphics loop cycle 4 — upgrades #533-544."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        if (crit) { sparks.add(Spark(e.x, e.y, 0.3f, Color.rgb(250, 204, 21), dp(13f))); sparks.add(Spark(e.x, e.y, 0.38f, Color.WHITE, dp(19f))); if (rings.size < 14) rings.add(Ring(e.x, e.y, dp(8f), 0.3f, 0.3f, Color.rgb(250, 204, 21))); if (save.dmgNumbers) texts.add(FloatText(e.x, e.y - e.radius, "💥 CRIT ${dmg.toInt()}", 0.65f, Color.rgb(250, 204, 21), dp(19f)).also { it.spin = true }) } // #173 spin-in""",
"""        if (crit) { sparks.add(Spark(e.x, e.y, 0.3f, Color.rgb(250, 204, 21), dp(13f))); sparks.add(Spark(e.x, e.y, 0.38f, Color.WHITE, dp(19f))); if (rings.size < 14) rings.add(Ring(e.x, e.y, dp(8f), 0.3f, 0.3f, Color.rgb(250, 204, 21))); if (save.dmgNumbers) texts.add(FloatText(e.x, e.y - e.radius, "💥 CRIT ${dmg.toInt()}", 0.65f, Color.rgb(250, 204, 21), dp(19f)).also { it.spin = true }) } // #173 spin-in
        if (crit && particles.size < 370) for (k in 0 until 3) particles.add(Particle(e.x, e.y, (Random.nextFloat() - 0.5f) * 260f, (Random.nextFloat() - 0.5f) * 260f, 0.3f, 0.3f, Color.rgb(250, 204, 21), dp(2.6f))) // #533 crits shed gold sparks""")

E("""        e.flash = 0.06f // #162 hit flash 60ms""",
"""        e.flash = 0.06f // #162 hit flash 60ms
        if (particles.size < 370 && Random.nextFloat() < 0.35f) particles.add(Particle(e.x, e.y, (Random.nextFloat() - 0.5f) * 200f, (Random.nextFloat() - 0.5f) * 200f, 0.22f, 0.22f, Color.WHITE, dp(2f))) // #534 ordinary hits spark too""")

E("""                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)
                p.strokeWidth = dp(1.4f); p.alpha = 90 // #535 counter-rotating dashed shell
                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""")

E("""            for (ch3 in bTitle) { c.drawText(ch3.toString(), bx3, h * 0.3f, p); bx3 += p.measureText(ch3.toString()) + spread }
            p.alpha = 255""",
"""            var li5 = 0 // #536 a shimmer travels the banner letters
            for (ch3 in bTitle) { p.alpha = (a * (0.72f + 0.28f * sin(time * 6f - li5 * 0.55f))).toInt().coerceIn(0, a); c.drawText(ch3.toString(), bx3, h * 0.3f, p); bx3 += p.measureText(ch3.toString()) + spread; li5++ }
            p.alpha = 255""")

E("""            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail""",
"""            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail
            if (e.type == 6 && particles.size < 340 && Random.nextFloat() < 0.10f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 10f, 0.55f, 0.55f, Color.rgb(226, 232, 240), dp(2.2f))) // #537 weaver silk dots""")

E("""            txt(c, "DASH", dashBtnRect.centerX(), dashBtnRect.bottom + dp(15f), dp(10f), Color.rgb(165, 243, 252), true)""",
"""            if (dashCharges < dashMaxCharges) { // #538 dash button sweeps its recharge
                val dprog2 = 1f - (dashRechargeT / dashCdMax).coerceIn(0f, 1f)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(34, 211, 238); p.alpha = 190
                c.drawArc(RectF(dashBtnRect.left + dp(4f), dashBtnRect.top + dp(4f), dashBtnRect.right - dp(4f), dashBtnRect.bottom - dp(4f)), -90f, 360f * dprog2, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "DASH", dashBtnRect.centerX(), dashBtnRect.bottom + dp(15f), dp(10f), Color.rgb(165, 243, 252), true)""")

E("""        c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)""",
"""        c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)
        if (state == State.PLAYING && maxHp > 0 && hp / maxHp < 0.25f) { // #539 low-HP heartbeat edge glow
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(239, 68, 68)
            p.alpha = (40 + 35 * sin(time * 6.5f)).toInt().coerceIn(5, 75)
            c.drawRect(dp(8f), dp(8f), w - dp(8f), h - dp(8f), p)
        }""")

E("""            if (e.type == 7 && e.state == 2 && particles.size < 360 && Random.nextFloat() < 0.5f) particles.add(Particle(e.x, e.y + e.radius * 0.6f, Random.nextFloat() * 30 - 15, 26f, 0.4f, 0.4f, Color.rgb(168, 162, 158), dp(3.6f))) // #461 charge dust""",
"""            if (e.type == 7 && e.state == 2 && particles.size < 360 && Random.nextFloat() < 0.5f) particles.add(Particle(e.x, e.y + e.radius * 0.6f, Random.nextFloat() * 30 - 15, 26f, 0.4f, 0.4f, Color.rgb(168, 162, 158), dp(3.6f))) // #461 charge dust
            if (e.type == 2 && particles.size < 360 && Random.nextFloat() < 0.30f) particles.add(Particle(e.x + (Random.nextFloat() - 0.5f) * e.radius, e.y + e.radius * 0.7f, (Random.nextFloat() - 0.5f) * 24f, 18f, 0.45f, 0.45f, Color.rgb(168, 162, 158), dp(4.2f))) // #540 brute stomp dust""")

E("""            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
            }""",
"""            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
            }
            if (e.type == 8) { // #541 orbiter's path rings around you
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.WHITE; p.alpha = 26
                c.drawCircle(px, py, hyp(e.x - px, e.y - py), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.slowT > 0 && e.frostAge > 0.5f) { // #542 deep-frozen foes get an ice-cube outline
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(165, 243, 252); p.alpha = 120
                c.drawRoundRect(RectF(e.x - e.radius - dp(2f), e.y - e.radius - dp(2f), e.x + e.radius + dp(2f), e.y + e.radius + dp(2f)), dp(5f), dp(5f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")

E("""            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)""",
"""            for (k in 0 until 2) { // #543 flame tips lick the boss bar's ends
                val fx3 = if (k == 0) bx - dp(6f) else bx + bw + dp(6f)
                poly(c, fx3, by + dp(6f) + sin(time * 9f + k * 2f) * dp(1.5f), dp(5f + 1.5f * sin(time * 11f + k)), 3, -1.5708f, Color.rgb(249, 115, 22))
            }
            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)""")

E("""                if (hyp(aimDx, aimDy) > 0.12f) { // #470 arrowhead shows the fire direction
                    val aa2 = atan2(aimDy, aimDx)
                    poly(c, aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(8f), 4, aa2 + 0.78f, Color.rgb(244, 114, 182))
                }""",
"""                if (hyp(aimDx, aimDy) > 0.12f) { // #470 arrowhead shows the fire direction
                    val aa2 = atan2(aimDy, aimDx)
                    poly(c, aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(8f), 4, aa2 + 0.78f, Color.rgb(244, 114, 182))
                    p.color = Color.WHITE; p.alpha = (170 + 70 * sin(time * 10f)).toInt().coerceIn(100, 240) // #544 the arrowhead's core pulses
                    c.drawCircle(aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(2.6f), p); p.alpha = 255
                }""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop4: {len(EDITS)} edits applied")
