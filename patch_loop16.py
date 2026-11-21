#!/usr/bin/env python3
"""GemStorm graphics loop cycle 16 — upgrades #677-688."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)""",
"""            if (pr.fromPlayer && !pr.rocket) { p.color = pr.color; p.alpha = 60; c.drawCircle(pr.x, pr.y, pr.radius * 1.9f, p); p.alpha = 255 } // #677 your shots wear a soft halo
            p.color = pr.color
            c.drawCircle(pr.x, pr.y, pr.radius, p)""")

E("""            else { // #440 hostile orbs throb — readable in a swarm
                p.alpha = (170 + 85 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(85, 255)
                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
            }""",
"""            else { // #440 hostile orbs throb — readable in a swarm
                p.alpha = (170 + 85 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(85, 255)
                c.drawCircle(pr.x, pr.y, pr.radius * (0.45f + 0.12f * sin(time * 10f + pr.y * 0.1f)), p); p.alpha = 255
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = pr.color; p.alpha = (60 + 40 * sin(time * 10f + pr.x * 0.1f)).toInt().coerceIn(20, 100); c.drawCircle(pr.x, pr.y, pr.radius * 1.5f, p); p.style = Paint.Style.FILL; p.alpha = 255 // #678 hostile orbs wear a warning ring
            }""")

E("""                p.color = Color.rgb(254, 240, 138); c.drawCircle(pr.x + cos(ra) * pr.radius * 0.5f, pr.y + sin(ra) * pr.radius * 0.5f, pr.radius * 0.42f, p)""",
"""                p.color = Color.rgb(254, 240, 138); c.drawCircle(pr.x + cos(ra) * pr.radius * 0.5f, pr.y + sin(ra) * pr.radius * 0.5f, pr.radius * 0.42f, p)
                if (((time * 6f + pr.x * 0.02f).toInt() % 2) == 0) { p.color = Color.rgb(239, 68, 68); c.drawCircle(pr.x - cos(ra) * pr.radius * 1.1f, pr.y - sin(ra) * pr.radius * 1.1f, dp(1.8f), p) } // #679 the rocket's tail light blinks""")

E("""                poly(c, bx, by, dp(13f), 3, orbitAngle * 3f + k * 2f, Color.rgb(52, 211, 153))
            }
        }""",
"""                poly(c, bx, by, dp(13f), 3, orbitAngle * 3f + k * 2f, Color.rgb(52, 211, 153))
                poly(c, bx, by, dp(5f), 4, time * 6f + k * 2f, Color.rgb(167, 243, 208)) // #680 orbit blades spin a bright heart
            }
        }""")

E("""        for (g in ghosts) {
            p.color = g.color; p.alpha = (130 * (g.life / 0.34f)).toInt().coerceIn(0, 130)
            poly(c, g.x, g.y, dp(19f), 4, 0.78f, g.color)
        }""",
"""        for (g in ghosts) {
            p.color = g.color; p.alpha = (130 * (g.life / 0.34f)).toInt().coerceIn(0, 130)
            poly(c, g.x, g.y, dp(19f), 4, 0.78f, g.color)
            p.color = Color.WHITE; p.alpha = (70 * (g.life / 0.34f)).toInt().coerceIn(0, 70); c.drawCircle(g.x, g.y, dp(3f), p) // #681 dash ghosts burn bright at the center
        }""")

E("""            if (level >= 15) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 150
                c.drawCircle(bpx, bpy, dp(33f) + dp(1.5f) * sin(time * 3f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (level >= 15) {
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = 150
                c.drawCircle(bpx, bpy, dp(33f) + dp(1.5f) * sin(time * 3f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 4).forEach { k -> val ha2 = k * 1.5708f + time * 1.1f; poly(c, bpx + cos(ha2) * (dp(33f) + dp(1.5f) * sin(time * 3f)), bpy + sin(ha2) * (dp(33f) + dp(1.5f) * sin(time * 3f)), dp(2.6f), 4, ha2, Color.rgb(250, 204, 21)) } // #682 gem studs ride the evolution halo
            }""")

E("""            if (save.selectedChar == 4) { // #128 Cryo aura ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(147, 197, 253); p.alpha = 70
                c.drawCircle(px, py, dp(130f) * (1f + 0.02f * sin(time * 2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (save.selectedChar == 4) { // #128 Cryo aura ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(147, 197, 253); p.alpha = 70
                c.drawCircle(px, py, dp(130f) * (1f + 0.02f * sin(time * 2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 5).forEach { k -> val sa5 = time * 0.7f + k * 2.4f; p.color = Color.WHITE; p.alpha = (40 + 30 * sin(time * 2f + k * 1.9f)).toInt().coerceIn(10, 70); c.drawCircle(px + cos(sa5) * dp(96f), py + sin(sa5 * 1.3f) * dp(96f), dp(1.7f), p) }; p.alpha = 255 // #683 snow sparkles drift inside the Cryo aura
            }""")

E("""            p.alpha = (26 + 14 * sin(time * 3.4f)).toInt().coerceIn(12, 40)
            c.drawCircle(px, py, magnetR, p); p.style = Paint.Style.FILL; p.alpha = 255
        }""",
"""            p.alpha = (26 + 14 * sin(time * 3.4f)).toInt().coerceIn(12, 40)
            c.drawCircle(px, py, magnetR, p); p.style = Paint.Style.FILL; p.alpha = 255
            (0 until 3).forEach { k -> val ma = time * 1.8f + k * 2.09f; p.color = Color.rgb(167, 243, 208); p.alpha = 60; c.drawCircle(px + cos(ma) * magnetR, py + sin(ma) * magnetR, dp(1.8f), p) }; p.alpha = 255 // #684 ants march the magnet ring
        }""")

E("""            if (vacuumT > 0 && g.mag) { // #243 bright vacuum lines during the level-up vacuum
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(167, 243, 208); p.alpha = 120
                c.drawLine(g.x, g.y + bob, px, py, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (vacuumT > 0 && g.mag) { // #243 bright vacuum lines during the level-up vacuum
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(167, 243, 208); p.alpha = 120
                c.drawLine(g.x, g.y + bob, px, py, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (vacuumT > 0 && g.mag) { p.color = Color.WHITE; p.alpha = 150; c.drawCircle((g.x + px) / 2, (g.y + bob + py) / 2, dp(1.6f), p); p.alpha = 255 } // #685 a bead rides each vacuum stream""")

E("""            for (k in 0 until 3) c.drawArc(RectF(mx - dp(90f), my - dp(90f), mx + dp(90f), my + dp(90f)), time * 40f + k * 120f, 60f, false, p)
            p.style = Paint.Style.FILL; p.alpha = 255""",
"""            for (k in 0 until 3) c.drawArc(RectF(mx - dp(90f), my - dp(90f), mx + dp(90f), my + dp(90f)), time * 40f + k * 120f, 60f, false, p)
            p.strokeWidth = dp(1.1f); p.alpha = 34; c.drawCircle(mx, my, dp(90f) * (0.7f + 0.3f * (0.5f + 0.5f * sin(time * 3f))), p) // #686 a second ring breathes inside the merge glow
            p.style = Paint.Style.FILL; p.alpha = 255""")

E("""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))""",
"""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            (0 until 3).forEach { k -> val ha = time * 5f + k * 2.09f; p.color = Color.rgb(254, 202, 202); p.alpha = (170 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 170); c.drawCircle(hx3 + cos(ha) * hr * 1.25f, hy3 + sin(ha) * hr * 1.25f, dp(1.6f), p) }; p.alpha = 255 // #687 sparkles orbit the pickup heart""")

E("""                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)""",
"""                c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), -90f, 360f * (shieldHp / (shieldLvl * 20f)), false, p)
                if (lastHurtT > 0.45f) { p.strokeWidth = dp(4f); p.color = Color.WHITE; p.alpha = (160 * (lastHurtT / 0.9f)).toInt().coerceIn(0, 160); c.drawArc(RectF(px - dp(30f), py - dp(30f), px + dp(30f), py + dp(30f)), lastHurtAngle * 57.2958f - 20f, 40f, false, p); p.strokeWidth = dp(2.5f); p.color = Color.rgb(147, 197, 253) } // #688 the shield flashes where the hit landed""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop16: {len(EDITS)} edits applied")
