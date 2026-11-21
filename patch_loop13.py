#!/usr/bin/env python3
"""GemStorm graphics loop cycle 13 — upgrades #641-652."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            bp.lineTo(b.x2, b.y2)
            c.drawPath(bp, p)
            p.style = Paint.Style.FILL; p.alpha = 255""",
"""            bp.lineTo(b.x2, b.y2)
            c.drawPath(bp, p)
            p.color = Color.WHITE; p.alpha = (150 * (b.life / 0.14f)).toInt().coerceIn(0, 150); c.drawCircle(b.x2, b.y2, dp(2.2f), p) // #641 bolts strike a white-hot point
            p.style = Paint.Style.FILL; p.alpha = 255""")

E("""            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = (26 * ((d.life - 4f) / 2f)).toInt().coerceIn(0, 26); c.drawCircle(d.x, d.y, d.r * 0.7f, p) } // #601 fresh scorch still glows""",
"""            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = (26 * ((d.life - 4f) / 2f)).toInt().coerceIn(0, 26); c.drawCircle(d.x, d.y, d.r * 0.7f, p) } // #601 fresh scorch still glows
            if (d.life > 2f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(120, 53, 15); p.alpha = (40 * (d.life / 6f)).toInt().coerceIn(0, 40); c.drawCircle(d.x, d.y, d.r, p); p.style = Paint.Style.FILL } // #642 scorch marks keep a baked rim""")

E("""                p.color = Color.rgb(251, 146, 60); p.alpha = (70 * fade).toInt()
                c.drawCircle(fx2, fp.y - fh, fp.r * 0.16f, p)""",
"""                p.color = Color.rgb(251, 146, 60); p.alpha = (70 * fade).toInt()
                c.drawCircle(fx2, fp.y - fh, fp.r * 0.16f, p)
                p.color = Color.rgb(254, 240, 138); p.alpha = (60 * fade).toInt(); c.drawCircle(fx2, fp.y - fh, fp.r * 0.07f, p) // #643 flame tongues burn pale at the tip""")

E("""                p.color = Color.rgb(134, 239, 172); p.alpha = (80 * (1f - ph) * fade).toInt()
                c.drawCircle(pl.x + cos(k * 2.4f + pl.y) * pl.r * 0.45f, pl.y + sin(k * 1.9f + pl.x) * pl.r * 0.4f, dp(3.5f) * sin(ph * 3.14159f), p)""",
"""                p.color = Color.rgb(134, 239, 172); p.alpha = (80 * (1f - ph) * fade).toInt()
                c.drawCircle(pl.x + cos(k * 2.4f + pl.y) * pl.r * 0.45f, pl.y + sin(k * 1.9f + pl.x) * pl.r * 0.4f, dp(3.5f) * sin(ph * 3.14159f), p)
                if (ph > 0.42f && ph < 0.58f) { p.color = Color.WHITE; p.alpha = (120 * fade).toInt(); c.drawCircle(pl.x + cos(k * 2.4f + pl.y) * pl.r * 0.45f - dp(1f), pl.y + sin(k * 1.9f + pl.x) * pl.r * 0.4f - dp(1f), dp(1f), p) } // #644 bubbles flash a highlight at their peak""")

E("""            p.style = Paint.Style.FILL; p.alpha = 255
            if (m.t < 0.4f) {""",
"""            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(254, 202, 202); p.alpha = (120 + 100 * sin(time * 10f)).toInt().coerceIn(20, 220); c.drawCircle(m.x, m.y, dp(2.4f), p); p.alpha = 255 // #645 the impact point itself pulses
            if (m.t < 0.4f) {""")

E("""            p.color = Color.BLACK; p.alpha = (90 + 80 * (1f - frac)).toInt() // #190 shadow grows as it falls
            c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p)""",
"""            p.color = Color.BLACK; p.alpha = (90 + 80 * (1f - frac)).toInt() // #190 shadow grows as it falls
            c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p)
            if (frac < 0.5f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(249, 115, 22); p.alpha = (90 * (1f - frac * 2f)).toInt().coerceIn(0, 90); c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #646 the shadow wears a hot rim while it's high""")

E("""                    c.drawCircle(aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(2.6f), p); p.alpha = 255
                }""",
"""                    c.drawCircle(aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(2.6f), p); p.alpha = 255
                    poly(c, aimOx + cos(aa2) * dp(48f), aimOy + sin(aa2) * dp(48f), dp(5f), 4, aa2 + 0.78f, Color.rgb(249, 168, 212)) // #647 a fainter echo diamond trails the arrowhead
                }""")

E("""                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255 // #317""",
"""                c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255 // #317
                p.color = CHARS[save.selectedChar].color; p.alpha = (150 * save.btnOpacity).toInt(); c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(9f) * save.joySize, p); p.alpha = 255 // #648 the stick knob wears your hero's color at its heart""")

E("""                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)""",
"""                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)
                p.style = Paint.Style.FILL; poly(c, px + cos(-time * 0.9f) * dp(30f), py + sin(-time * 0.9f) * dp(30f), dp(3f), 4, time * 2f, Color.rgb(219, 234, 254)); p.style = Paint.Style.STROKE // #649 a glint rides the shield rim""")

E("""                c.drawArc(RectF(px - dp(24f), py - dp(24f), px + dp(24f), py + dp(24f)), -90f, 360f * min(1f, stillT / 1.2f), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawArc(RectF(px - dp(24f), py - dp(24f), px + dp(24f), py + dp(24f)), -90f, 360f * min(1f, stillT / 1.2f), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                if (focusReady) for (k in 0 until 2) { val fa9 = time * 3.4f + k * 3.1416f; poly(c, px + cos(fa9) * dp(24f), py + sin(fa9) * dp(24f), dp(2.6f), 4, fa9, Color.rgb(254, 240, 138)) } // #650 a charged focus sparkles""")

E("""                c.drawArc(RectF(px - arcR, py - arcR, px + arcR, py + arcR), lastHurtAngle * 57.2958f - 26f, 52f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawArc(RectF(px - arcR, py - arcR, px + arcR, py + arcR), lastHurtAngle * 57.2958f - 26f, 52f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                poly(c, px + cos(lastHurtAngle) * arcR, py + sin(lastHurtAngle) * arcR, dp(4f), 4, time * 3f, Color.rgb(254, 202, 202)) // #651 the hurt arc points with a hot head""")

E("""        if (state == State.PLAYING && maxHp > 0 && hp / maxHp < 0.25f) { // #539 low-HP heartbeat edge glow
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(239, 68, 68)
            p.alpha = (40 + 35 * sin(time * 6.5f)).toInt().coerceIn(5, 75)
            c.drawRect(dp(8f), dp(8f), w - dp(8f), h - dp(8f), p)
        }""",
"""        if (state == State.PLAYING && maxHp > 0 && hp / maxHp < 0.25f) { // #539 low-HP heartbeat edge glow
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(239, 68, 68)
            p.alpha = (40 + 35 * sin(time * 6.5f)).toInt().coerceIn(5, 75)
            c.drawRect(dp(8f), dp(8f), w - dp(8f), h - dp(8f), p)
            p.strokeWidth = dp(3.4f); p.alpha = (120 + 90 * sin(time * 6.5f)).toInt().coerceIn(30, 210) // #652 the corner brackets throb with the heartbeat
            c.drawLine(ci, ci + cl, ci, ci, p); c.drawLine(ci, ci, ci + cl, ci, p)
            c.drawLine(w - ci - cl, ci, w - ci, ci, p); c.drawLine(w - ci, ci, w - ci, ci + cl, p)
            c.drawLine(ci, h - ci - cl, ci, h - ci, p); c.drawLine(ci, h - ci, ci + cl, h - ci, p)
            c.drawLine(w - ci - cl, h - ci, w - ci, h - ci, p); c.drawLine(w - ci, h - ci - cl, w - ci, h - ci, p)
        }""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop13: {len(EDITS)} edits applied")
