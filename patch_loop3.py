#!/usr/bin/env python3
"""GemStorm graphics loop cycle 3 — upgrades #521-532."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            p.color = Color.rgb(254, 240, 138); p.alpha = (90 * fade).toInt()
            c.drawCircle(fp.x + sin(time * 9f + fp.y) * dp(5f), fp.y - dp(3f), fp.r * 0.28f, p)
            p.alpha = 255""",
"""            p.color = Color.rgb(254, 240, 138); p.alpha = (90 * fade).toInt()
            c.drawCircle(fp.x + sin(time * 9f + fp.y) * dp(5f), fp.y - dp(3f), fp.r * 0.28f, p)
            for (k in 0 until 3) { // #521 flame tongues lick upward
                val fx2 = fp.x + cos(k * 2.1f + fp.x * 0.07f) * fp.r * 0.45f
                val fh = fp.r * (0.30f + 0.16f * sin(time * 13f + k * 2.1f + fp.y * 0.05f))
                p.color = Color.rgb(251, 146, 60); p.alpha = (70 * fade).toInt()
                c.drawCircle(fx2, fp.y - fh, fp.r * 0.16f, p)
            }
            p.alpha = 255""")

E("""            p.alpha = (80 * fade).toInt()
            c.drawCircle(pl.x + cos(time * 2.3f + pl.x) * dp(12f), pl.y + sin(time * 1.9f) * dp(9f), pl.r * 0.34f, p)""",
"""            p.alpha = (80 * fade).toInt()
            c.drawCircle(pl.x + cos(time * 2.3f + pl.x) * dp(12f), pl.y + sin(time * 1.9f) * dp(9f), pl.r * 0.34f, p)
            p.color = Color.rgb(20, 83, 45); p.alpha = (70 * fade).toInt() // #522 dark toxic core churns
            c.drawCircle(pl.x + cos(-time * 1.1f + pl.y * 0.05f) * pl.r * 0.3f, pl.y + sin(-time * 0.9f + pl.x * 0.05f) * pl.r * 0.3f, pl.r * 0.3f, p)""")

E("""        p.color = if (bossWave && enemies.any { it.type == 5 }) Color.rgb(220, 38, 38) else Color.rgb(168, 85, 247)
        p.alpha = 90
        c.drawRect(dp(5f), dp(5f), w - dp(5f), h - dp(5f), p)
        p.style = Paint.Style.FILL; p.alpha = 255""",
"""        p.color = if (bossWave && enemies.any { it.type == 5 }) Color.rgb(220, 38, 38) else Color.rgb(168, 85, 247)
        p.alpha = 90
        c.drawRect(dp(5f), dp(5f), w - dp(5f), h - dp(5f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(3.4f); p.alpha = 170 // #523 bright corner brackets
        val cl = dp(18f); val ci = dp(5f)
        c.drawLine(ci, ci + cl, ci, ci, p); c.drawLine(ci, ci, ci + cl, ci, p)
        c.drawLine(w - ci - cl, ci, w - ci, ci, p); c.drawLine(w - ci, ci, w - ci, ci + cl, p)
        c.drawLine(ci, h - ci - cl, ci, h - ci, p); c.drawLine(ci, h - ci, ci + cl, h - ci, p)
        c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)
        p.style = Paint.Style.FILL; p.alpha = 255""")

E("""            p.color = Color.rgb(190, 18, 60); p.alpha = 13""",
"""            p.color = Color.rgb(190, 18, 60); p.alpha = (11 + 5 * sin(time * 2f)).toInt().coerceIn(6, 16) // #524 the boss tint breathes""")

E("""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(250, 204, 21) // #449 countdown pulse ring""",
"""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = CHARS[save.selectedChar].color // #449 countdown pulse ring, #525 in your hero's color""")

E("""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))""",
"""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))
            if (wfrac > 0.02f) { // #526 a tiny turtle rides the wave progress
                val txp = w / 2 - dp(42f) + dp(84f) * wfrac
                p.color = Color.rgb(34, 197, 94); c.drawCircle(txp, m + dp(54f), dp(3.4f), p)
                p.color = Color.rgb(187, 247, 208); c.drawCircle(txp + dp(3.6f), m + dp(54f), dp(1.7f), p)
            }""")

E("""            if (big) { // #460 rich gems wear a rotating sparkle
                val sa2 = time * 3f + g.x * 0.05f
                poly(c, g.x + cos(sa2) * r * 1.5f, g.y + bob + sin(sa2) * r * 1.5f, r * 0.4f, 4, sa2, Color.rgb(254, 240, 138))
            }""",
"""            if (big) { // #460 rich gems wear a rotating sparkle
                val sa2 = time * 3f + g.x * 0.05f
                poly(c, g.x + cos(sa2) * r * 1.5f, g.y + bob + sin(sa2) * r * 1.5f, r * 0.4f, 4, sa2, Color.rgb(254, 240, 138))
            }
            if (big) { // #527 rich gems also wear a slow halo ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.3f); p.color = Color.rgb(254, 240, 138); p.alpha = 90
                c.drawArc(RectF(g.x - r * 1.7f, g.y + bob - r * 1.7f, g.x + r * 1.7f, g.y + bob + r * 1.7f), time * 60f + g.x * 0.03f, 200f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")

E("""            poly(c, hd.x, hd.y, r * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
        }""",
"""            poly(c, hd.x, hd.y, r * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            if (hyp(px - hd.x, py - hd.y) < dp(140f)) { // #528 a faint red thread reaches for you when close
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(239, 68, 68); p.alpha = (50 + 30 * sin(time * 7f)).toInt().coerceIn(20, 80)
                c.drawLine(hd.x, hd.y, px, py, p); p.style = Paint.Style.FILL; p.alpha = 255
            }
        }""")

E("""                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255""",
"""                p.color = Color.rgb(254, 240, 138)
                c.drawCircle(m.x + off, my2, dp(10f), p); p.alpha = 255
                for (k in 0 until 2) { // #529 twin flame wisps orbit the falling head
                    val wa = time * 11f + k * 3.14159f
                    p.color = Color.rgb(249, 115, 22); p.alpha = 130
                    c.drawCircle(m.x + off + cos(wa) * dp(13f), my2 + sin(wa) * dp(13f), dp(4.5f), p)
                }
                p.alpha = 255""")

E("""                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255 // #317""",
"""                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255 // #317
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(165, 243, 252); p.alpha = (70 + 40 * sin(time * 4f)).toInt().coerceIn(30, 110) // #530 outer halo breathes while you steer
                c.drawCircle(joyOx, joyOy, dp(60f) * save.joySize, p); p.style = Paint.Style.FILL; p.alpha = 255""")

E("""            p.color = Color.BLACK; p.alpha = 255
            c.drawRect(0f, 0f, w, dp(48f) * bf2, p)
            c.drawRect(0f, h - dp(48f) * bf2, w, h, p)""",
"""            p.color = Color.BLACK; p.alpha = 255
            c.drawRect(0f, 0f, w, dp(48f) * bf2, p)
            c.drawRect(0f, h - dp(48f) * bf2, w, h, p)
            p.color = Color.rgb(220, 38, 38); p.alpha = (200 * bf2).toInt() // #531 red edge lines on the letterbox
            c.drawRect(0f, dp(48f) * bf2, w, dp(48f) * bf2 + dp(2f), p)
            c.drawRect(0f, h - dp(48f) * bf2 - dp(2f), w, h - dp(48f) * bf2, p); p.alpha = 255""")

E("""        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(168, 85, 247); p.alpha = (50 + 40 * sin(time * 2.4f)).toInt().coerceIn(10, 90) // #496 idle pulse""",
"""        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = CHARS[save.selectedChar].color; p.alpha = (50 + 40 * sin(time * 2.4f)).toInt().coerceIn(10, 90) // #496 idle pulse, #532 in your hero's color""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop3: {len(EDITS)} edits applied")
