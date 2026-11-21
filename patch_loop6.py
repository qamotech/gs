#!/usr/bin/env python3
"""GemStorm graphics loop cycle 6 — upgrades #557-568."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""                1 -> { } // Swift: the turtle's jet pods are its signature now""",
"""                1 -> { if ((pvx * pvx + pvy * pvy) > 9000f) { // #557 Swift leaves speed streaks at pace
                    val spd = hyp(pvx, pvy); val ux = pvx / spd; val uy = pvy / spd
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 120
                    for (k in -1..1) { val ox2 = -uy * k * dp(9f); val oy2 = ux * k * dp(9f); val sx2 = bpx - ux * dp(24f) + ox2; val sy2 = bpy - uy * dp(24f) + oy2; c.drawLine(sx2, sy2, sx2 - ux * dp(13f + 4f * sin(time * 17f + k)), sy2 - uy * dp(13f + 4f * sin(time * 17f + k)), p) }
                    p.style = Paint.Style.FILL; p.alpha = 255 } }""")

E("""                5 -> { } // 💎N🎱 turtle: the whole body is the signature""",
"""                5 -> poly(c, bpx, bpy, dp(3.4f), 4, time * 1.8f, Color.WHITE) // #558 the 💎N🎱 gem throws a rotating glint""")

E("""        else if (save.dmgNumbers) texts.add(FloatText(e.x + Random.nextFloat() * 16 - 8, e.y - e.radius, "${dmg.toInt()}", 0.45f, Color.rgb(226, 232, 240), dp(11.5f)))""",
"""        else if (save.dmgNumbers) texts.add(FloatText(e.x + Random.nextFloat() * 16 - 8, e.y - e.radius, "${dmg.toInt()}", 0.45f, if (dmg >= 40f) Color.rgb(253, 224, 71) else Color.rgb(226, 232, 240), if (dmg >= 40f) dp(14f) else dp(11.5f))) // #559 heavy hits read gold and bigger""")

E("""                if (Random.nextFloat() < 0.4f) texts.add(FloatText(e.x, e.y - e.radius - dp(8f), "🛡️ BLOCKED", 0.5f, Color.rgb(148, 163, 184), dp(11f)))""",
"""                if (Random.nextFloat() < 0.4f) texts.add(FloatText(e.x, e.y - e.radius - dp(8f), "🛡️ BLOCKED", 0.5f, Color.rgb(148, 163, 184), dp(11f)))
                sparks.add(Spark(e.x, e.y, 0.22f, Color.rgb(203, 213, 225), dp(8f))) // #560 armor blocks clang sparks""")

E("""                t.hp -= raw * 0.4f; t.flash = 0.06f // #162""",
"""                t.hp -= raw * 0.4f; t.flash = 0.06f // #162
                sparks.add(Spark(t.x, t.y, 0.25f, Color.rgb(253, 224, 71), dp(9f))) // #561 chain hits crackle""")

E("""            poly(c, g.x, g.y + bob, r, 4, 0.78f, Color.rgb(52, 211, 153))
            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))""",
"""            val gNear = if (!g.mag) gemDrops.firstOrNull { it !== g && !it.mag && hyp(it.x - g.x, it.y - g.y) < dp(30f) } else null // #562 gems about to merge hum together
            if (gNear != null) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(167, 243, 208); p.alpha = (40 + 25 * sin(time * 6f + g.x * 0.05f)).toInt().coerceIn(15, 65); c.drawLine(g.x, g.y + bob, gNear.x, gNear.y + sin(time * 4f + gNear.x * 0.05f) * dp(2f), p); p.style = Paint.Style.FILL; p.alpha = 255 }
            poly(c, g.x, g.y + bob, r, 4, 0.78f, Color.rgb(52, 211, 153))
            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))""")

E("""            if (e.elite) { // #501 elites wear a rotating gold double-ring""",
"""            if (e.type == 5 && bossIntroT > 0) { // #563 the Warden enters under a spotlight
                p.color = Color.WHITE; p.alpha = (26 * (bossIntroT / 2f).coerceIn(0f, 1f)).toInt()
                c.drawCircle(e.x, e.y, e.radius * 2.6f, p); p.alpha = 255
            }
            if (e.elite) { // #501 elites wear a rotating gold double-ring""")

E("""            if (countT <= 0f) audio.sfx("count", 1.5f) // #295 final beep at a higher pitch""",
"""            if (countT <= 0f) { audio.sfx("count", 1.5f); texts.add(FloatText(w / 2, h * 0.40f, "GO!", 0.7f, Color.rgb(250, 204, 21), dp(26f))) } // #295 final beep, #564 GO! flash""")

E("""            for (ci in 0 until rocketMaxCharges) {
                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }""",
"""            for (ci in 0 until rocketMaxCharges) {
                if (ci < rocketCharges) { p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 5f + ci)).toInt().coerceIn(20, 100); c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(6.4f), p); p.alpha = 255 } // #565 charged pips glow
                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }""")

E("""            txt(c, "✔ WAVE $waveClearedNum CLEAR!  +$waveClearBonus", w / 2, h * 0.38f, dp(19f), Color.rgb(52, 211, 153), true, a3)""",
"""            txt(c, "✔ WAVE $waveClearedNum CLEAR!  +$waveClearBonus", w / 2, h * 0.38f, dp(19f), Color.rgb(52, 211, 153), true, a3)
            p.color = Color.rgb(250, 204, 21); p.alpha = (a3 * 0.8f).toInt() // #566 gold underline sweeps the clear text
            c.drawRect(w / 2 - dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(8f), w / 2 + dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(10.5f), p); p.alpha = 255""")

E("""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))""",
"""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            if (hf in 0.35f..0.75f && particles.size < 370 && Random.nextFloat() < 0.3f) particles.add(Particle(hx3 + (Random.nextFloat() - 0.5f) * dp(20f), hy3, (Random.nextFloat() - 0.5f) * 40f, -30f, 0.4f, 0.4f, Color.rgb(252, 165, 165), dp(2.2f))) // #567 the pickup heart sheds sparkles at its peak""")

E("""                p.color = Color.WHITE; p.alpha = 200 // #510 bright tip on the fill edge""",
"""                if (e.elite) { p.color = Color.rgb(250, 204, 21); p.alpha = 230; c.drawCircle(e.x - e.radius - dp(5f), e.y - e.radius - dp(7.5f), dp(2.2f), p); p.alpha = 255 } // #568 a gold bead marks the elite bar
                p.color = Color.WHITE; p.alpha = 200 // #510 bright tip on the fill edge""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop6: {len(EDITS)} edits applied")
