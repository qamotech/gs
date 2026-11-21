#!/usr/bin/env python3
"""GemStorm graphics loop cycle 8 — upgrades #581-592."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            poly(c, sx2, sy2, dp(5f), 4, time * 3f + i, Color.rgb(250, 204, 21))
        }
        p.alpha = 255
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)""",
"""            poly(c, sx2, sy2, dp(5f), 4, time * 3f + i, Color.rgb(250, 204, 21))
        }
        p.alpha = 255
        p.color = Color.rgb(250, 204, 21); p.alpha = (26 + 18 * sin(time * 3f)).toInt().coerceIn(8, 44); c.drawCircle(w - dp(52f), dp(55f), dp(26f), p); p.alpha = 255 // #581 the gem bank glows softly
        txtR(c, "💎 ${save.gemBank}", w - dp(16f), dp(62f), dp(18f), Color.rgb(52, 211, 153), true)""")

E("""        // #426 shooting stars: one streak every ~9s, path set by the cycle""",
"""        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.WHITE; p.alpha = 16 // #582 a vast slow star-ring turns in the deep background
        for (k in 0 until 4) c.drawArc(RectF(w * 0.85f - dp(150f), h * 0.18f - dp(150f), w * 0.85f + dp(150f), h * 0.18f + dp(150f)), time * 6f + k * 90f, 40f, false, p)
        p.style = Paint.Style.FILL; p.alpha = 255
        // #426 shooting stars: one streak every ~9s, path set by the cycle""")

E("""        txt(c, if (endlessMode) "WAVE $wave ∞" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(47f), dp(15f), Color.rgb(233, 213, 255), true) // v2.3 own row — collides with nothing""",
"""        txt(c, if (endlessMode) "WAVE $wave ∞" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(47f), dp(15f), if (endlessMode) Color.rgb(250, 204, 21) else Color.rgb(233, 213, 255), true) // v2.3 own row — collides with nothing; #583 endless runs gold""")

E("""            if (mutator > 0) txt(c, mutatorTitle(), w / 2, h * 0.3f + dp(58f), dp(14f), Color.rgb(250, 204, 21), true, a)""",
"""            if (mutator > 0) txt(c, mutatorTitle(), w / 2, h * 0.3f + dp(58f), dp(14f), Color.rgb(250, 204, 21), true, (a * (0.75f + 0.25f * sin(time * 5f))).toInt().coerceIn(0, a)) // #584 the mutator line pulses""")

E("""            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
            }""",
"""            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
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
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 0 && enemies.count { it.type == 0 } >= 6) { // #588 a full chaser pack glows hungry
                p.color = Color.rgb(239, 68, 68); p.alpha = 22
                c.drawCircle(e.x, e.y, e.radius * 1.5f, p); p.alpha = 255
            }""")

E("""            score += e.score * mult""",
"""            val scBefore = score // #589 thousand milestones get a shout
            score += e.score * mult
            if (score / 1000 > scBefore / 1000) texts.add(FloatText(px, py - dp(92f), "⭐ ${(score / 1000) * 1000}!", 1.0f, Color.rgb(250, 204, 21), dp(16f)))""")

E("""            if (g.mag) { // #442 comet tail as gems rocket home
                val ga2 = atan2(py - g.y, px - g.x)
                p.color = Color.rgb(52, 211, 153); p.alpha = 90
                poly(c, g.x - cos(ga2) * r * 1.5f, g.y + bob - sin(ga2) * r * 1.5f, r * 0.55f, 4, 0.78f, Color.rgb(52, 211, 153))
                p.alpha = 255
            }""",
"""            if (g.mag) { // #442 comet tail as gems rocket home
                val ga2 = atan2(py - g.y, px - g.x)
                p.color = Color.rgb(52, 211, 153); p.alpha = 90
                poly(c, g.x - cos(ga2) * r * 1.5f, g.y + bob - sin(ga2) * r * 1.5f, r * 0.55f, 4, 0.78f, Color.rgb(52, 211, 153))
                p.alpha = 255
            }
            if (vacuumT > 0 && g.mag) { // #590 vacuum streams burn white at the tip
                val ga3 = atan2(py - g.y, px - g.x)
                poly(c, g.x - cos(ga3) * r * 2.6f, g.y + bob - sin(ga3) * r * 2.6f, r * 0.4f, 4, 0.78f, Color.WHITE)
            }""")

E("""            txt(c, "$countStage", w / 2, h * 0.40f, dp(64f), Color.rgb(250, 204, 21), true)""",
"""            txt(c, "$countStage", w / 2, h * 0.40f, dp(76f), CHARS[save.selectedChar].color, true, 46) // #591 echo numeral behind the count
            txt(c, "$countStage", w / 2, h * 0.40f, dp(64f), Color.rgb(250, 204, 21), true)""")

E("""            txt(c, "GET READY…", w / 2, h * 0.40f + dp(52f), dp(16f), Color.WHITE, true)""",
"""            txt(c, "GET READY…", w / 2, h * 0.40f + dp(52f), dp(16f), Color.WHITE, true, (170 + 80 * sin(time * 8f)).toInt().coerceIn(90, 250)) // #592 GET READY pulses""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop8: {len(EDITS)} edits applied")
