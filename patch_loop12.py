#!/usr/bin/env python3
"""GemStorm graphics loop cycle 12 — upgrades #629-640."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            p.color = Color.WHITE; p.alpha = (220 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(2.4f), p)""",
"""            p.color = Color.rgb(165, 243, 252); p.alpha = (60 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(7f), p)
            p.color = Color.WHITE; p.alpha = (220 * (1f - sf)).toInt()
            c.drawCircle(hx, hy, dp(2.4f), p) // #629 the shooting star's head glows""")

E("""            c.drawOval(RectF(plx - dp(98f), ply - dp(18f), plx + dp(98f), ply + dp(18f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }""",
"""            c.drawOval(RectF(plx - dp(98f), ply - dp(18f), plx + dp(98f), ply + dp(18f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(203, 213, 225); p.alpha = 130 // #630 a moon rides with the giant
            c.drawCircle(plx + cos(time * 0.5f) * dp(84f), ply + sin(time * 0.5f) * dp(30f), dp(5f), p); p.alpha = 255
        }""")

E("""            if (remain in 1..3) { // #446 almost there — the bar burns gold
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                p.alpha = (140 + 100 * sin(time * 8f)).toInt().coerceIn(40, 240)
                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
        }""",
"""            if (remain in 1..3) { // #446 almost there — the bar burns gold
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                p.alpha = (140 + 100 * sin(time * 8f)).toInt().coerceIn(40, 240)
                c.drawRoundRect(RectF(w / 2 - dp(42f), m + dp(52f), w / 2 + dp(42f), m + dp(56f)), dp(2f), dp(2f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (remain in 1..8) { p.color = Color.rgb(250, 204, 21); p.alpha = (140 + 80 * sin(time * 7f)).toInt().coerceIn(60, 220); for (k in 0 until remain) c.drawCircle(w / 2 - (remain - 1) * dp(3f) + k * dp(6f), m + dp(63f), dp(1.8f), p); p.alpha = 255 } // #631 the last foes show as pips under the bar
        }""")

E("""        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(80f), dp(11.5f), Color.rgb(148, 163, 184))""",
"""        if (save.showTimer) { p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 100 * sin(time * 6.283f)).toInt().coerceIn(20, 220); c.drawCircle(w / 2 + dp(36f), m + dp(76f), dp(1.8f), p); p.alpha = 255 } // #632 a second-hand dot ticks beside the timer
        if (save.showTimer) txt(c, "⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}", w / 2, m + dp(80f), dp(11.5f), Color.rgb(148, 163, 184))""")

E("""        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.WHITE, true) // #492""",
"""        if (scorePop > 0.1f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21); p.alpha = (140 * (scorePop / 0.35f)).toInt().coerceIn(0, 140); c.drawCircle(w - m - dp(88f), m + dp(11f), dp(13f) + dp(6f) * (1f - scorePop / 0.35f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #633 big scores ring the SCORE label
        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.WHITE, true) // #492""")

E("""                p.color = Color.rgb(187, 247, 208); c.drawCircle(txp + dp(3.6f), m + dp(54f), dp(1.7f), p)""",
"""                p.color = Color.rgb(187, 247, 208); c.drawCircle(txp + dp(3.6f), m + dp(54f), dp(1.7f), p)
                p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 9f)).toInt().coerceIn(10, 90); c.drawCircle(txp - dp(6f), m + dp(54f), dp(1.1f), p); p.alpha = 255 // #634 the progress turtle kicks a wake""")

E("""            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
            }""",
"""            if (e.slowT > 0) { // #503 frost crystals on slowed foes
                for (k in 0 until 3) { val ca = k * 2.09f + e.x * 0.05f
                    poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
                if (e.frostAge > 1.2f) poly(c, e.x, e.y - e.radius - dp(3f), dp(3.6f), 4, time * 2.4f, Color.WHITE) // #635 deep-frozen foes grow a crown crystal
            }""")

E("""            if (e.type == 5) { // #502 boss dread ring on the ground
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.6f); p.color = Color.rgb(220, 38, 38); p.alpha = (60 + 30 * sin(time * 3.2f)).toInt().coerceIn(30, 90)
                c.drawCircle(e.x, e.y, e.radius * (1.25f + 0.05f * sin(time * 3.2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (e.type == 5) { // #502 boss dread ring on the ground
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.6f); p.color = Color.rgb(220, 38, 38); p.alpha = (60 + 30 * sin(time * 3.2f)).toInt().coerceIn(30, 90)
                c.drawCircle(e.x, e.y, e.radius * (1.25f + 0.05f * sin(time * 3.2f)), p)
                p.style = Paint.Style.FILL; p.alpha = 255
                for (k in 0 until 4) { val ta3 = k * 1.5708f + time * 0.9f; poly(c, e.x + cos(ta3) * e.radius * 1.25f, e.y + sin(ta3) * e.radius * 1.25f, dp(3f), 4, ta3, Color.rgb(220, 38, 38)) } // #636 thorn sparks ride the dread ring
            }""")

E("""                p.alpha = 90
                c.drawArc(RectF(e.x - e.radius - dp(7f), e.y - e.radius - dp(7f), e.x + e.radius + dp(7f), e.y + e.radius + dp(7f)), -time * 95f, 200f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                p.alpha = 90
                c.drawArc(RectF(e.x - e.radius - dp(7f), e.y - e.radius - dp(7f), e.x + e.radius + dp(7f), e.y + e.radius + dp(7f)), -time * 95f, 200f, false, p)
                p.strokeWidth = dp(1.1f); p.color = Color.rgb(254, 240, 138); p.alpha = 60 // #637 a third hairline ring counter-turns
                c.drawArc(RectF(e.x - e.radius - dp(10f), e.y - e.radius - dp(10f), e.x + e.radius + dp(10f), e.y + e.radius + dp(10f)), time * 60f, 120f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""")

E("""                texts.add(FloatText(e.x, e.y - dp(20f), "💰 GNOME LOOTED! +60💎", 1.2f, Color.rgb(250, 204, 21), dp(17f)))""",
"""                texts.add(FloatText(e.x, e.y - dp(20f), "💰 GNOME LOOTED! +60💎", 1.2f, Color.rgb(250, 204, 21), dp(17f)))
                for (k in 0 until 3) sparks.add(Spark(e.x + (k - 1) * dp(9f), e.y - dp(6f), 0.35f, Color.rgb(250, 204, 21), dp(8f))) // #638 the loot bursts coin-sparks""")

E("""                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(6f), 0.25f, 0.25f, Color.rgb(74, 222, 128))) // #467 birth pop ring""",
"""                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(6f), 0.25f, 0.25f, Color.rgb(74, 222, 128))) // #467 birth pop ring
                sparks.add(Spark(mini.x, mini.y, 0.25f, Color.rgb(74, 222, 128), dp(6f))) // #639 splitter births spark""")

E("""                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(7f), 0.3f, 0.3f, Color.rgb(232, 121, 249))) // #513 necro births ring fuchsia""",
"""                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(7f), 0.3f, 0.3f, Color.rgb(232, 121, 249))) // #513 necro births ring fuchsia
                sparks.add(Spark(mini.x, mini.y - dp(4f), 0.3f, Color.rgb(232, 121, 249), dp(7f))) // #640 necro births spark fuchsia""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop12: {len(EDITS)} edits applied")
