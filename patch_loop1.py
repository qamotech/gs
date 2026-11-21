#!/usr/bin/env python3
"""GemStorm graphics loop cycle 1 — upgrades #497-508."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        for ((nx, ny, ncol) in neb) {
            p.color = ncol; p.alpha = 26
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = 16
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = 12""",
"""        for ((nx, ny, ncol) in neb) {
            val np2 = 0.82f + 0.18f * sin(time * 0.23f + nx * 0.011f) // #497 nebulae breathe
            p.color = ncol; p.alpha = (26 * np2).toInt()
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = (16 * np2).toInt()
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = (12 * np2).toInt()""")

E("""                if (particles.size < 380) particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.3f, 0.3f, Color.rgb(148, 163, 184), dp(3.6f)))""",
"""                if (particles.size < 380) particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.3f, 0.3f, Color.rgb(148, 163, 184), dp(3.6f)))
                if (particles.size < 380 && Random.nextFloat() < 0.5f) particles.add(Particle(pr.x, pr.y, (Random.nextFloat() - 0.5f) * 60f, (Random.nextFloat() - 0.5f) * 60f, 0.22f, 0.22f, Color.rgb(251, 146, 60), dp(2f))) // #498 rocket spark trail""")

E("""            combo++; comboT = 2.5f
            if (combo > comboBest) comboBest = combo""",
"""            combo++; comboT = 2.5f
            if (combo % 25 == 0) texts.add(FloatText(px, py - dp(76f), "🔥 RAMPAGE ×$combo", 1.0f, Color.rgb(248, 113, 113), dp(17f))) // #499 combo milestone callout
            if (combo > comboBest) comboBest = combo""")

E("""            if (g.mag) { // #442 comet tail as gems rocket home""",
"""            if (sin(time * 2.4f + g.x * 0.13f + g.y * 0.071f) > 0.94f) // #500 idle gems twinkle
                poly(c, g.x, g.y + bob, r * 0.85f, 4, time * 1.5f, Color.WHITE)
            if (g.mag) { // #442 comet tail as gems rocket home""")

E("""            if (e.elite) { // #247 elite beacon light column
                p.color = Color.rgb(250, 204, 21); p.alpha = (30 + 14 * sin(time * 5f)).toInt().coerceIn(16, 44)
                c.drawRect(e.x - dp(8f), e.y - dp(320f), e.x + dp(8f), e.y, p)
                p.alpha = 255
            }
            c.save(); c.translate(e.x, e.y); c.scale(eSc, eSc); c.translate(-e.x, -e.y)""",
"""            if (e.elite) { // #247 elite beacon light column
                p.color = Color.rgb(250, 204, 21); p.alpha = (30 + 14 * sin(time * 5f)).toInt().coerceIn(16, 44)
                c.drawRect(e.x - dp(8f), e.y - dp(320f), e.x + dp(8f), e.y, p)
                p.alpha = 255
            }
            if (e.elite) { // #501 elites wear a rotating gold double-ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21); p.alpha = 150
                c.drawArc(RectF(e.x - e.radius - dp(4f), e.y - e.radius - dp(4f), e.x + e.radius + dp(4f), e.y + e.radius + dp(4f)), time * 130f, 240f, false, p)
                p.alpha = 90
                c.drawArc(RectF(e.x - e.radius - dp(7f), e.y - e.radius - dp(7f), e.x + e.radius + dp(7f), e.y + e.radius + dp(7f)), -time * 95f, 200f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 5) { // #502 boss dread ring on the ground
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.6f); p.color = Color.rgb(220, 38, 38); p.alpha = (60 + 30 * sin(time * 3.2f)).toInt().coerceIn(30, 90)
                c.drawCircle(e.x, e.y, e.radius * (1.25f + 0.05f * sin(time * 3.2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
            }
            c.save(); c.translate(e.x, e.y); c.scale(eSc, eSc); c.translate(-e.x, -e.y)""")

E("""                // #474 dome gloss
                p.color = Color.WHITE; p.alpha = 46
                c.drawOval(RectF(bpx - dp(11f), bpy - dp(12.5f), bpx - dp(1f), bpy - dp(6.5f)), p); p.alpha = 255""",
"""                // #474 dome gloss
                p.color = Color.WHITE; p.alpha = 46
                c.drawOval(RectF(bpx - dp(11f), bpy - dp(12.5f), bpx - dp(1f), bpy - dp(6.5f)), p); p.alpha = 255
                // #504 shine sweep circles the shell gem
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.WHITE; p.alpha = 110
                c.drawArc(RectF(bpx - dp(10.5f), bpy - dp(10.5f), bpx + dp(10.5f), bpy + dp(10.5f)), time * 150f, 65f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""")

E("""        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))""",
"""        rings.add(Ring(px, py, dp(10f), 0.32f, 0.32f, CHARS[save.selectedChar].color))
        rings.add(Ring(px - dashDx * dp(14f), py - dashDy * dp(14f), dp(6f), 0.26f, 0.26f, Color.rgb(165, 243, 252))) // #505 flipper bubble ring trails the dash""")

E("""            val pulse = 1f + 0.18f * sin(time * 6f + hd.x * 0.03f)""",
"""            val urg = if (maxHp > 0) 1f - hp / maxHp else 0f // #506 the heart beats faster the more hurt you are
            val pulse = 1f + (0.18f + 0.14f * urg) * sin(time * (6f + 7f * urg) + hd.x * 0.03f)""")

E("""            if (s.size > 2.6f) { // #464 the brightest stars throw a cross glint
                val gy = (s.y + drift) % (h + 8f) - 4f; val gl = dp(5f) * (0.6f + 0.4f * sin(time * 2f + s.phase))
                p.alpha = (120 * tw).toInt()
                c.drawLine(s.x - gl, gy, s.x + gl, gy, p); c.drawLine(s.x, gy - gl, s.x, gy + gl, p)
            }""",
"""            if (s.size > 2.6f) { // #464 the brightest stars throw a cross glint, #507 slowly rotating
                val gy = (s.y + drift) % (h + 8f) - 4f; val gl = dp(5f) * (0.6f + 0.4f * sin(time * 2f + s.phase))
                val ga3 = 0.45f * sin(time * 0.7f + s.phase)
                p.alpha = (120 * tw).toInt()
                c.drawLine(s.x - cos(ga3) * gl, gy - sin(ga3) * gl, s.x + cos(ga3) * gl, gy + sin(ga3) * gl, p)
                c.drawLine(s.x - cos(ga3 + 1.57f) * gl, gy - sin(ga3 + 1.57f) * gl, s.x + cos(ga3 + 1.57f) * gl, gy + sin(ga3 + 1.57f) * gl, p)
            }""")

E("""        rings.add(Ring(cxp, cyp, dp(8f), 0.4f, 0.4f, enemyColor(e.type)))""",
"""        rings.add(Ring(cxp, cyp, dp(8f), 0.4f, 0.4f, enemyColor(e.type)))
        rings.add(Ring(cxp, cyp, dp(4f), 0.28f, 0.28f, Color.WHITE)) // #508 inner portal swirl ring""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop1: {len(EDITS)} edits applied")
