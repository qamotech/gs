#!/usr/bin/env python3
"""GemStorm graphics loop cycle 14 — upgrades #653-664."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        roundRect(c, r2, dp(14f), fill)
        p.color = Color.WHITE; p.alpha = 20""",
"""        roundRect(c, r2, dp(14f), fill)
        val shx2 = ((time * 0.24f + r2.top * 0.0011f) % 1.6f - 0.3f) // #653 a sheen sweeps every button
        if (shx2 in 0.06f..0.94f) { p.color = Color.WHITE; p.alpha = 15; c.drawRect(r2.left + dp(12f) + (r2.width() - dp(31f)) * shx2, r2.top + dp(3f), r2.left + dp(19f) + (r2.width() - dp(31f)) * shx2, r2.top + r2.height() * 0.45f, p); p.alpha = 255 }
        p.color = Color.WHITE; p.alpha = 20""")

E("""            p.color = chd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255""",
"""            p.color = chd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255
            if (sel) for (k in 0 until 2) { val sa3 = time * 2.6f + k * 3.1416f + i * 1.3f; poly(c, dp(40f) + cos(sa3) * dp(21f), y + dp(28f) + sin(sa3) * dp(21f), dp(2.8f), 4, sa3, chd.color) } // #654 the equipped hero's portrait sparkles""")

E("""            p.color = wpd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255""",
"""            p.color = wpd.color; p.alpha = 46; c.drawCircle(dp(40f), y + dp(28f), dp(18f), p); p.alpha = 255
            if (sel) for (k in 0 until 2) { val sa4 = time * 2.6f + k * 3.1416f + i * 1.7f; poly(c, dp(40f) + cos(sa4) * dp(21f), y + dp(28f) + sin(sa4) * dp(21f), dp(2.8f), 4, sa4, wpd.color) } // #655 the equipped weapon's icon sparkles""")

E("""            if (frac > 0) roundRect(c, RectF(dp(92f), yy - dp(9f), dp(92f) + (w - dp(212f)) * frac.coerceIn(0f, 1f), yy - dp(2f)), dp(3f), col)""",
"""            if (frac > 0) roundRect(c, RectF(dp(92f), yy - dp(9f), dp(92f) + (w - dp(212f)) * frac.coerceIn(0f, 1f), yy - dp(2f)), dp(3f), col)
            if (frac > 0) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(92f) + (w - dp(212f)) * frac.coerceIn(0f, 1f), yy - dp(5.5f), dp(1.8f), p); p.alpha = 255 } // #656 stat bars cap with a bright tip""")

E("""        roundRect(c, RectF(dp(92f), y + dp(45f), dp(92f) + (w - dp(212f)) * (dps / maxDps), y + dp(52f)), dp(3f), Color.rgb(244, 114, 182))""",
"""        roundRect(c, RectF(dp(92f), y + dp(45f), dp(92f) + (w - dp(212f)) * (dps / maxDps), y + dp(52f)), dp(3f), Color.rgb(244, 114, 182))
        p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(92f) + (w - dp(212f)) * (dps / maxDps), y + dp(48.5f), dp(1.8f), p); p.alpha = 255 // #657 the DPS bar caps with a bright tip""")

E("""            p.style = Paint.Style.FILL; p.alpha = 255
        }
        if (!victory) { // #571 red embers drift up through the wreckage""",
"""            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(248, 113, 113); p.alpha = (26 + 18 * sin(time * 2.2f)).toInt().coerceIn(8, 44); for (k in 0 until 5) c.drawCircle(w * (0.2f + k * 0.15f), h * 0.16f, dp(2.6f), p); p.alpha = 255 // #658 ember light pools where the cracks end
        }
        if (!victory) { // #571 red embers drift up through the wreckage""")

E("""        if (victory) { // #249 slow victory zoom on the hero
            val vz = 1f + 0.07f * min(1f, screenT / 2.5f)
            c.save(); c.scale(vz, vz, w / 2, h * 0.42f)
        }""",
"""        if (victory) { // #249 slow victory zoom on the hero
            val vz = 1f + 0.07f * min(1f, screenT / 2.5f)
            c.save(); c.scale(vz, vz, w / 2, h * 0.42f)
        }
        if (victory) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(9f); p.color = Color.rgb(250, 204, 21); p.alpha = 13; for (k in 0 until 6) { val ra2 = k * 1.047f + time * 0.24f; c.drawLine(w / 2, h * 0.30f, w / 2 + cos(ra2) * dp(190f), h * 0.30f + sin(ra2) * dp(190f), p) } p.style = Paint.Style.FILL; p.alpha = 255 } // #659 slow rays turn behind the victory hero""")

E("""            txt(c, "⭐ ELITE DOWN ⭐", w / 2, h * 0.2f, dp(22f), Color.rgb(250, 204, 21), true, a2)""",
"""            txt(c, "⭐ ELITE DOWN ⭐", w / 2, h * 0.2f, dp(22f), Color.rgb(250, 204, 21), true, a2)
            for (k in 0 until 2) { val sx9 = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx9, h * 0.2f - dp(7f), dp(4f), 4, time * 4f + k * 1.7f, Color.rgb(250, 204, 21)) } // #660 stars spin at the elite shout's shoulders""")

E("""        txtL(c, "🌊 Best wave with ${chd.name}: ${save.bestWaveChar(save.selectedChar)}", dp(28f), y + dp(138f), dp(12f), Color.rgb(148, 163, 184))""",
"""        txtL(c, "🌊 Best wave with ${chd.name}: ${save.bestWaveChar(save.selectedChar)}", dp(28f), y + dp(138f), dp(12f), Color.rgb(148, 163, 184))
        poly(c, w - dp(34f), y + dp(134f), dp(3.6f), 4, time * 2.2f, Color.rgb(34, 211, 238)) // #661 a spark marks the hero's best-wave line""")

E("""        txtL(c, "Shop price: ${if (save.ownsWeapon(save.selectedWeapon)) "owned" else "💎 ${wpd.cost}"}", dp(28f), y + dp(106f), dp(12f), Color.rgb(148, 163, 184))""",
"""        txtL(c, "Shop price: ${if (save.ownsWeapon(save.selectedWeapon)) "owned" else "💎 ${wpd.cost}"}", dp(28f), y + dp(106f), dp(12f), Color.rgb(148, 163, 184))
        if (!save.ownsWeapon(save.selectedWeapon) && save.gemBank >= wpd.cost) poly(c, w - dp(34f), y + dp(102f), dp(3.6f), 4, time * 3f, Color.rgb(52, 211, 153)) // #662 you can afford it — the price line sparkles""")

E("""            if (countT <= 0f) { audio.sfx("count", 1.5f); texts.add(FloatText(w / 2, h * 0.40f, "GO!", 0.7f, Color.rgb(250, 204, 21), dp(26f))) } // #295 final beep, #564 GO! flash""",
"""            if (countT <= 0f) { audio.sfx("count", 1.5f); texts.add(FloatText(w / 2, h * 0.40f, "GO!", 0.7f, Color.rgb(250, 204, 21), dp(26f))); rings.add(Ring(w / 2, h * 0.40f - dp(10f), dp(12f), 0.45f, 0.45f, Color.rgb(250, 204, 21))) } // #295 final beep, #564 GO! flash, #663 GO! burst ring""")

E("""            p.alpha = 255
            val sub = when {""",
"""            p.alpha = 255
            val swx = w / 2 - tw3 / 2 + tw3 * ((time * 0.7f) % 1f); p.color = Color.rgb(250, 204, 21); p.alpha = (a * 0.8f).toInt(); c.drawCircle(swx, h * 0.3f + dp(9f), dp(2.2f), p); p.alpha = 255 // #664 a spark runs the banner's underline
            val sub = when {""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop14: {len(EDITS)} edits applied")
