#!/usr/bin/env python3
"""GemStorm graphics loop cycle 11 — upgrades #617-628 (+ #598 gem seating fix)."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        c.drawLine(shx + dp(26f), 0f, shx + dp(26f) - dp(120f), h, p)
        p.alpha = 255; p.style = Paint.Style.FILL""",
"""        c.drawLine(shx + dp(26f), 0f, shx + dp(26f) - dp(120f), h, p)
        if (beat > 0.9f) { p.color = Color.rgb(216, 180, 254); p.alpha = 60; for (k in 0 until 5) c.drawCircle((k * 0.37f + 0.08f) * w, (k * 0.53f + 0.11f) * h, dp(1.6f), p) } // #617 grid nodes sparkle on the beat peak
        p.alpha = 255; p.style = Paint.Style.FILL""")

E("""            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)""",
"""            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
            if (s.phase > 3.14f && s.size > 2f) { p.alpha = (36 * tw).toInt(); c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size * 2.4f, p) } // #618 warm stars wear a halo""")

E("""            txt(c, "$countStage", w / 2, h * 0.40f, dp(76f), CHARS[save.selectedChar].color, true, 46) // #591 echo numeral behind the count""",
"""            for (k in 0 until 4) { val ta2 = k * 1.5708f + time * 1.2f; poly(c, w / 2 + cos(ta2) * dp(52f), h * 0.40f - dp(20f) + sin(ta2) * dp(52f), dp(3.4f), 4, ta2, CHARS[save.selectedChar].color) } // #619 ticks orbit the countdown
            txt(c, "$countStage", w / 2, h * 0.40f, dp(76f), CHARS[save.selectedChar].color, true, 46) // #591 echo numeral behind the count""")

E("""            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))""",
"""            roundRect(c, RectF(bx, by, bx + bw * (boss.hp / boss.maxHp).coerceIn(0f, 1f), by + dp(13f)), dp(6f), Color.rgb(168, 85, 247))
            val hpF2 = (boss.hp / boss.maxHp).coerceIn(0f, 1f); val shb = ((time * 0.35f) % 1.3f - 0.15f) // #620 a shine stripe travels the boss bar's fill
            if (shb in 0f..1f && hpF2 > 0.02f) { val sxb = bx + bw * hpF2 * shb; p.color = Color.WHITE; p.alpha = 34; c.drawRect(sxb, by + dp(1.5f), min(sxb + dp(10f), bx + bw * hpF2), by + dp(11.5f), p); p.alpha = 255 }""")

E("""            poly(c, m + dp(6f), by + dp(4f), dp(4f), 4, time * 2f, Color.rgb(168, 85, 247)) // #598 boss bar end-gems spin
            poly(c, w - m - dp(6f), by + dp(4f), dp(4f), 4, -time * 2f, Color.rgb(168, 85, 247))""",
"""            poly(c, bx + dp(7f), by + dp(6.5f), dp(4f), 4, time * 2f, Color.rgb(216, 180, 254)) // #598 boss bar end-gems spin (loop-11: seated on the bar itself)
            poly(c, bx + bw - dp(7f), by + dp(6.5f), dp(4f), 4, -time * 2f, Color.rgb(216, 180, 254))""")

E("""            p.color = chCol; p.alpha = (55 * spawnSc).toInt()
            c.drawCircle(bpx, bpy, dp(34f) * spawnSc, p); p.alpha = 255""",
"""            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (110 + 80 * sin(time * 8f)).toInt().coerceIn(30, 190); c.drawCircle(bpx, bpy, dp(38f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #621 a level-up is about to pop
            p.color = chCol; p.alpha = (55 * spawnSc).toInt()
            c.drawCircle(bpx, bpy, dp(34f) * spawnSc, p); p.alpha = 255""")

E("""            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), if (comboT < 0.6f && (time * 10f).toInt() % 2 == 0) Color.rgb(248, 113, 113) else Color.rgb(251, 146, 60)) // #488 expiry flash""",
"""            if (comboT > 0.05f) { p.color = Color.rgb(254, 240, 138); p.alpha = 200; c.drawCircle(m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(52.5f), dp(2.6f), p); p.alpha = 255 } // #622 the combo fuse burns at the tip
            roundRect(c, RectF(m, xpY + dp(50f), m + barW * 0.6f * (comboT / 2.5f).coerceIn(0f, 1f), xpY + dp(55f)), dp(2f), if (comboT < 0.6f && (time * 10f).toInt() % 2 == 0) Color.rgb(248, 113, 113) else Color.rgb(251, 146, 60)) // #488 expiry flash""")

E("""            if (dashT > 0) { // dash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol; p.alpha = 160
                c.drawCircle(px, py, dp(30f) + (0.16f - dashT) * dp(120f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (dashT > 0) { // dash ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol; p.alpha = 160
                c.drawCircle(px, py, dp(30f) + (0.16f - dashT) * dp(120f), p)
                p.strokeWidth = dp(1.4f); p.alpha = 90; c.drawCircle(px, py, (dp(30f) + (0.16f - dashT) * dp(120f)) * 0.72f, p) // #623 the dash ring echoes inside itself
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")

E("""            else c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, p)""",
"""            else { c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) + 0.5f, p); if (pt.life / pt.maxLife > 0.7f) { p.color = Color.WHITE; p.alpha = 120; c.drawCircle(pt.x, pt.y, pt.size * (pt.life / pt.maxLife) * 0.4f + 0.3f, p) } } // #624 young particles burn bright at the core""")

E("""                val bw = e.radius * 2
                p.color = Color.rgb(30, 10, 40); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f), p)
                if (e.ghostHp > e.hp) { p.color = Color.rgb(226, 232, 240); c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.ghostHp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(5f), p) } // #432 recent-damage ghost
                p.color = if (e.elite) Color.rgb(250, 204, 21) else Color.rgb(74, 222, 128) // #510 elite bars run gold
                c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f), p)""",
"""                val bw = e.radius * 2
                p.color = Color.rgb(30, 10, 40); c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x + e.radius, e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p) // #625 enemy HP bars get rounded caps
                if (e.ghostHp > e.hp) { p.color = Color.rgb(226, 232, 240); c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.ghostHp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p) } // #432 recent-damage ghost
                p.color = if (e.elite) Color.rgb(250, 204, 21) else Color.rgb(74, 222, 128) // #510 elite bars run gold
                c.drawRoundRect(RectF(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f)), dp(2.5f), dp(2.5f), p)""")

E("""            if (e.elite) { // #247 elite beacon light column
                p.color = Color.rgb(250, 204, 21); p.alpha = (30 + 14 * sin(time * 5f)).toInt().coerceIn(16, 44)
                c.drawRect(e.x - dp(8f), e.y - dp(320f), e.x + dp(8f), e.y, p)
                p.alpha = 255
            }""",
"""            if (e.elite) { // #247 elite beacon light column
                p.color = Color.rgb(250, 204, 21); p.alpha = (30 + 14 * sin(time * 5f)).toInt().coerceIn(16, 44)
                c.drawRect(e.x - dp(8f), e.y - dp(320f), e.x + dp(8f), e.y, p)
                for (k in 0 until 2) { val byy = e.y - ((time * 46f + k * 131f + e.x) % dp(300f)); p.color = Color.rgb(254, 240, 138); p.alpha = (60 + 40 * sin(time * 6f + k * 2.6f)).toInt().coerceIn(20, 100); c.drawCircle(e.x + sin(time * 3f + k * 4.1f) * dp(4f), byy, dp(1.7f), p) } // #626 sparks rise in the elite beacon
                p.alpha = 255
            }""")

E("""            if (muzzleT > 0) { // muzzle flash star at the barrel
                val mx = px + cos(lastAim) * dp(26f); val my = py + sin(lastAim) * dp(26f)
                poly(c, mx, my, dp(9f), 4, time * 22f, Color.rgb(254, 240, 138))
            }""",
"""            if (muzzleT > 0) { // muzzle flash star at the barrel
                val mx = px + cos(lastAim) * dp(26f); val my = py + sin(lastAim) * dp(26f)
                poly(c, mx, my, dp(9f), 4, time * 22f, Color.rgb(254, 240, 138))
                poly(c, mx, my, dp(4f), 4, -time * 30f, Color.WHITE) // #627 the flash burns white at the core
            }""")

E("""        if (newRecord) txt(c, "🌟 NEW HIGH SCORE! 🌟", w / 2, h * 0.24f + dp(66f), dp(16f), Color.rgb(52, 211, 153), true)""",
"""        if (newRecord) txt(c, "🌟 NEW HIGH SCORE! 🌟", w / 2, h * 0.24f + dp(66f), dp(16f), Color.rgb(52, 211, 153), true)
        if (newRecord) for (k in 0 until 8) { val cx8 = w / 2 + sin(k * 2.39f + time * 1.3f) * dp(120f); val cy8 = h * 0.24f + dp(58f) + cos(k * 1.71f + time * 1.7f) * dp(10f); poly(c, cx8, cy8, dp(3f), 4, time * 3f + k, if (k % 2 == 0) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153)) } // #628 record confetti orbits the shout""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop11: {len(EDITS)} edits applied")
