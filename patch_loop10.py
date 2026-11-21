#!/usr/bin/env python3
"""GemStorm graphics loop cycle 10 — upgrades #605-616."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            p.color = Color.rgb(203, 213, 225); p.alpha = 34 + (i % 3) * 12
            c.drawCircle(mx, my, dp(1.7f), p)""",
"""            p.color = Color.rgb(203, 213, 225); p.alpha = 34 + (i % 3) * 12
            c.drawCircle(mx, my, dp(1.7f), p)
            if (i % 5 == 0) { p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 3f + i * 2.3f)).toInt().coerceIn(10, 90); c.drawCircle(mx, my, dp(0.9f), p) } // #605 every fifth dust mote glints""")

E("""                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(244, 114, 182); p.alpha = (90 + 70 * sin(time * 6f)).toInt().coerceIn(20, 160); c.drawCircle(ax, ay, dp(17f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #596 the Warden's off-screen arrow wears a halo""",
"""                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(244, 114, 182); p.alpha = (90 + 70 * sin(time * 6f)).toInt().coerceIn(20, 160); c.drawCircle(ax, ay, dp(17f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #596 the Warden's off-screen arrow wears a halo
                if (e.type == 11) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (80 + 60 * sin(time * 5f)).toInt().coerceIn(20, 140); c.drawCircle(ax, ay, dp(13f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #606 the Gnome's arrow wears a gold halo — follow the money""")

E("""        // #127 Pyro fire pools (flickering)
        for (fp in firePools) {
            val fade = min(1f, fp.life)
            p.color = Color.rgb(249, 115, 22); p.alpha = (52 * fade).toInt()
            c.drawCircle(fp.x, fp.y, fp.r * (1f + 0.08f * sin(time * 11f + fp.x)), p)""",
"""        // #127 Pyro fire pools (flickering)
        for (fp in firePools) {
            val fade = min(1f, fp.life)
            p.color = Color.rgb(249, 115, 22); p.alpha = (52 * fade).toInt()
            c.drawCircle(fp.x, fp.y, fp.r * (1f + 0.08f * sin(time * 11f + fp.x)), p)
            p.color = Color.rgb(254, 240, 138); p.alpha = (46 * fade).toInt() // #607 a pale hot core flickers at the center
            c.drawCircle(fp.x + sin(time * 9.3f + fp.y) * dp(3f), fp.y + cos(time * 7.7f + fp.x) * dp(3f), fp.r * 0.3f, p)""")

E("""            txt(c, "🚀 TAP ROCKET!", rocketBtnRect.centerX(), rocketBtnRect.top - dp(24f) - dp(4f) * rpulse, dp(13f), Color.rgb(251, 146, 60), true)""",
"""            txt(c, "🚀 TAP ROCKET!", rocketBtnRect.centerX(), rocketBtnRect.top - dp(24f) - dp(4f) * rpulse, dp(13f), Color.rgb(251, 146, 60), true)
            poly(c, rocketBtnRect.centerX(), rocketBtnRect.top - dp(10f) + dp(2f) * rpulse, dp(4f), 4, time * 2f, Color.rgb(251, 146, 60)) // #608 a diamond spins under the prompt""")

E("""            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)""",
"""            if (rocketCharges == 0) { // #609 recharge arc sweeps the rocket button
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(249, 115, 22); p.alpha = 170
                c.drawArc(RectF(rocketBtnRect.left + dp(5f), rocketBtnRect.top + dp(5f), rocketBtnRect.right - dp(5f), rocketBtnRect.bottom - dp(5f)), -90f, 360f * (1f - (rocketRechargeT / rocketCdMax).coerceIn(0f, 1f)), false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }
            txt(c, "🚀", rocketBtnRect.centerX(), rocketBtnRect.centerY() + dp(8f), dp(20f), Color.WHITE)""")

E("""                c.drawCircle(px + cos(fa) * dp(31f), py + sin(fa) * dp(31f) - fh * 0.4f, fh * 0.42f, p); p.alpha = 255""",
"""                c.drawCircle(px + cos(fa) * dp(31f), py + sin(fa) * dp(31f) - fh * 0.4f, fh * 0.42f, p); p.alpha = 255
                p.color = if (k % 2 == 0) Color.rgb(249, 115, 22) else Color.rgb(250, 204, 21); p.alpha = 60 // #610 aura orbs trail a fading echo
                c.drawCircle(px + cos(fa - 0.35f) * dp(31f), py + sin(fa - 0.35f) * dp(31f) - fh * 0.4f, fh * 0.3f, p); p.alpha = 255""")

E("""                p.strokeWidth = dp(1.4f); p.alpha = 90 // #535 counter-rotating dashed shell
                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)""",
"""                p.strokeWidth = dp(1.4f); p.alpha = 90 // #535 counter-rotating dashed shell
                for (k in 0 until 3) c.drawArc(RectF(px - dp(34f), py - dp(34f), px + dp(34f), py + dp(34f)), -time * 50f + k * 120f, 55f, false, p)
                if (regenActive) { p.style = Paint.Style.FILL; p.color = Color.rgb(147, 197, 253); p.alpha = (16 + 10 * sin(time * 7f)).toInt().coerceIn(6, 26); c.drawCircle(px, py, dp(30f), p) } // #611 a regrowing shield fills with light""")

E("""        sparks.add(Spark(cxp, cyp, 0.3f, enemyColor(e.type), dp(10f))); sparks.add(Spark(cxp + dp(10f), cyp - dp(6f), 0.3f, Color.WHITE, dp(7f))) // #223 portal sparks""",
"""        sparks.add(Spark(cxp, cyp, 0.3f, enemyColor(e.type), dp(10f))); sparks.add(Spark(cxp + dp(10f), cyp - dp(6f), 0.3f, Color.WHITE, dp(7f))) // #223 portal sparks
        if (e.elite) sparks.add(Spark(cxp, cyp, 0.4f, Color.rgb(250, 204, 21), dp(12f))) // #612 elite portals flare gold""")

E("""            c.drawRect(g.x - dp(0.8f), g.y + bob - sp, g.x + dp(0.8f), g.y + bob + sp, p)
            p.alpha = 255""",
"""            c.drawRect(g.x - dp(0.8f), g.y + bob - sp, g.x + dp(0.8f), g.y + bob + sp, p)
            p.alpha = 255
            if (!big && sin(time * 5f + g.x) > 0.92f) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(g.x, g.y + bob, dp(1.3f), p); p.alpha = 255 } // #613 small gems wink""")

E("""        texts.add(FloatText(w / 2, h * 0.3f, "⚠ ABYSS WARDEN ⚠", 2.2f, Color.rgb(244, 114, 182), dp(26f)))""",
"""        texts.add(FloatText(w / 2, h * 0.3f, "⚠ ABYSS WARDEN ⚠", 2.2f, Color.rgb(244, 114, 182), dp(26f)))
        rings.add(Ring(e.x, e.y, dp(24f), 0.5f, 0.5f, Color.rgb(244, 114, 182))) // #614 the Warden's arrival rings out""")

E("""        if (victory) {
            txt(c, "🏆 VICTORY! 🏆", w / 2, h * 0.24f, dp(32f), Color.rgb(250, 204, 21), true)""",
"""        if (!victory) { // #615 sparks sift down from the shattered gem
            for (k in 0 until 5) { val sx9 = w / 2 + sin(k * 2.7f + time * 0.7f) * dp(20f); val sy9 = h * 0.13f + ((time * 24f + k * 47f) % dp(90f))
                p.color = Color.rgb(248, 113, 113); p.alpha = (70 + 50 * sin(time * 3f + k * 1.9f)).toInt().coerceIn(20, 120); c.drawCircle(sx9, sy9, dp(1.8f), p) }
            p.alpha = 255
        }
        if (victory) {
            txt(c, "🏆 VICTORY! 🏆", w / 2, h * 0.24f, dp(32f), Color.rgb(250, 204, 21), true)""")

E("""        txt(c, title, w / 2, dp(64f), dp(25f), Color.rgb(165, 243, 252), true)""",
"""        txt(c, title, w / 2, dp(64f), dp(25f), Color.rgb(165, 243, 252), true)
        poly(c, w / 2 - dp(118f), dp(56f), dp(4.4f), 4, time * 2.4f, Color.rgb(250, 204, 21)) // #616 every screen title gets flanking gold sparks
        poly(c, w / 2 + dp(118f), dp(56f), dp(4.4f), 4, -time * 2.4f, Color.rgb(250, 204, 21))""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop10: {len(EDITS)} edits applied")
