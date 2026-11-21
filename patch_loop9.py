#!/usr/bin/env python3
"""GemStorm graphics loop cycle 9 — upgrades #593-604."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)""",
"""            if (xpNext > 0 && xp.toFloat() / xpNext > 0.9f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 100 * sin(time * 7f)).toInt().coerceIn(20, 220); c.drawCircle(px + dp(17f), py + dp(15f), dp(11.5f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #593 your level badge burns gold near a level-up
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)""")

E("""        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), Color.rgb(52, 211, 153), true)""",
"""        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), true) // #594 LV runs gold when a level is close""")

E("""                c.drawLine(hd.x, hd.y, px, py, p); p.style = Paint.Style.FILL; p.alpha = 255
            }
        }

        if (heartPopT > 0) { // ❤️ ONE red animated heart on pickup (Nathan v2.4 — replaces the two text hearts)""",
"""                c.drawLine(hd.x, hd.y, px, py, p); c.drawCircle((hd.x + px) / 2, (hd.y + py) / 2 + sin(time * 7f) * dp(2f), dp(2f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #595 a bead rides the heart thread
            }
        }

        if (heartPopT > 0) { // ❤️ ONE red animated heart on pickup (Nathan v2.4 — replaces the two text hearts)""")

E("""                p.alpha = if (e.type == 5) 255 else 150
                poly(c, ax, ay, if (e.type == 5) dp(13f) else dp(8f), 3, atan2(e.y - ay, e.x - ax) + 1.57f, if (e.type == 5) Color.rgb(244, 114, 182) else enemyColor(e.type))
                p.alpha = 255""",
"""                p.alpha = if (e.type == 5) 255 else 150
                poly(c, ax, ay, if (e.type == 5) dp(13f) else dp(8f), 3, atan2(e.y - ay, e.x - ax) + 1.57f, if (e.type == 5) Color.rgb(244, 114, 182) else enemyColor(e.type))
                p.alpha = 255
                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(244, 114, 182); p.alpha = (90 + 70 * sin(time * 6f)).toInt().coerceIn(20, 160); c.drawCircle(ax, ay, dp(17f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #596 the Warden's off-screen arrow wears a halo""")

E("""        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")""",
"""        p.color = Color.rgb(74, 222, 128); p.alpha = (30 + 22 * sin(time * 3.4f)).toInt().coerceIn(8, 52); c.drawRoundRect(RectF(x0 - dp(4f), h * 0.42f - dp(4f), x0 + bw + dp(4f), h * 0.42f + bh + dp(4f)), dp(16f), dp(16f), p); p.alpha = 255 // #597 RESUME breathes a soft halo
        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")""")

E("""            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)""",
"""            poly(c, m + dp(6f), by + dp(4f), dp(4f), 4, time * 2f, Color.rgb(168, 85, 247)) // #598 boss bar end-gems spin
            poly(c, w - m - dp(6f), by + dp(4f), dp(4f), 4, -time * 2f, Color.rgb(168, 85, 247))
            txt(c, "👑 ABYSS WARDEN", w / 2, by - dp(5f), dp(11f), Color.rgb(233, 213, 255), true)""")

E("""                c.drawCircle(m.x, m.y, dp(30f), p); p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""                c.drawCircle(m.x, m.y, dp(30f), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(249, 115, 22); p.alpha = 130 // #599 an inner ring collapses onto the impact point
            c.drawCircle(m.x, m.y, dp(8f) + dp(40f) * frac, p); p.style = Paint.Style.FILL; p.alpha = 255""")

E("""        for (fp in firePools) fp.life -= dt""",
"""        for (fp in firePools) fp.life -= dt
        if (state == State.PLAYING) for (fp in firePools) if (particles.size < 350 && Random.nextFloat() < 0.10f) particles.add(Particle(fp.x + (Random.nextFloat() - 0.5f) * fp.r, fp.y, 0f, -34f, 0.5f, 0.5f, Color.rgb(249, 115, 22), dp(2.2f))) // #600 fire pools shed rising embers""")

E("""        for (d in decals) {
            p.color = Color.rgb(0, 0, 0); p.alpha = (90 * (d.life / 6f)).toInt().coerceIn(0, 90)
            c.drawCircle(d.x, d.y, d.r, p)
        }""",
"""        for (d in decals) {
            p.color = Color.rgb(0, 0, 0); p.alpha = (90 * (d.life / 6f)).toInt().coerceIn(0, 90)
            c.drawCircle(d.x, d.y, d.r, p)
            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = (26 * ((d.life - 4f) / 2f)).toInt().coerceIn(0, 26); c.drawCircle(d.x, d.y, d.r * 0.7f, p) } // #601 fresh scorch still glows
        }""")

E("""        for (r in rings) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f) * (r.life / r.maxLife) + 0.5f
            p.color = r.color; p.alpha = (200 * (r.life / r.maxLife)).toInt().coerceIn(0, 200)
            c.drawCircle(r.x, r.y, r.r, p)
        }""",
"""        for (r in rings) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f) * (r.life / r.maxLife) + 0.5f
            p.color = r.color; p.alpha = (200 * (r.life / r.maxLife)).toInt().coerceIn(0, 200)
            c.drawCircle(r.x, r.y, r.r, p)
            p.style = Paint.Style.FILL; p.color = r.color; p.alpha = (16 * (r.life / r.maxLife)).toInt().coerceIn(0, 16); c.drawCircle(r.x, r.y, r.r, p) // #602 rings carry a whisper of fill
        }""")

E("""            c.drawCircle(b.x, b.y, b.r, p)
            p.shader = null
        }""",
"""            c.drawCircle(b.x, b.y, b.r, p)
            p.shader = null
            p.color = Color.WHITE; p.alpha = (120 * bf).toInt().coerceIn(0, 120); c.drawCircle(b.x, b.y, b.r * 0.18f, p); p.alpha = 255 // #603 blooms burn white at the core
        }""")

E("""        for (s in sparks) {
            val sr = s.size * (s.life / 0.3f)
            poly(c, s.x, s.y, sr, 4, time * 9f, s.color)
            poly(c, s.x, s.y, sr * 0.45f, 4, time * 9f + 0.78f, Color.WHITE)
        }""",
"""        for (s in sparks) {
            val sr = s.size * (s.life / 0.3f)
            poly(c, s.x, s.y, sr, 4, time * 9f, s.color)
            poly(c, s.x, s.y, sr * 0.45f, 4, time * 9f + 0.78f, Color.WHITE)
            if (s.life > 0.22f) { p.color = Color.WHITE; p.alpha = 160; c.drawCircle(s.x, s.y, dp(1.2f), p); p.alpha = 255 } // #604 young sparks flash a white core
        }""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop9: {len(EDITS)} edits applied")
