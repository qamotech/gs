#!/usr/bin/env python3
"""GemStorm graphics loop cycle 20 (finale) — #725-736. Draw-only."""
import sys

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# #725 gold rule underlines the menu title
rep("""        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)""",
"""        p.color = Color.rgb(250, 204, 21); p.alpha = (40 + 30 * sin(time * 2.2f)).toInt().coerceIn(10, 70); c.drawRect(cx - lws.sum() / 2, cy + dp(116f), cx + lws.sum() / 2, cy + dp(117.5f), p); p.alpha = 255 // #725 a gold rule underlines the title
        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)""",
"#725")

# #726 embers flank the SHATTERED shout
rep("""            txt(c, "💀 SHATTERED 💀", w / 2, h * 0.24f, dp(32f), Color.rgb(248, 113, 113), true)""",
"""            txt(c, "💀 SHATTERED 💀", w / 2, h * 0.24f, dp(32f), Color.rgb(248, 113, 113), true)
            (0 until 2).forEach { k -> val ex = w / 2 + (if (k == 0) -1f else 1f) * dp(168f); poly(c, ex, h * 0.24f - dp(11f), dp(4.4f), 3, time * 3.8f + k * 2.2f, Color.rgb(249, 115, 22)) } // #726 embers flank the SHATTERED shout""",
"#726")

# #727 gold sparks flank the victory rank
rep("""        if (victory) txt(c, "🏅 RANK ${rankLetter()}  •  ⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}  •  💀 $deaths", w / 2, y + dp(54f), dp(14.5f), Color.rgb(52, 211, 153), true)""",
"""        if (victory) txt(c, "🏅 RANK ${rankLetter()}  •  ⏱ ${(runTime / 60).toInt()}:${(runTime % 60).toInt().toString().padStart(2, '0')}  •  💀 $deaths", w / 2, y + dp(54f), dp(14.5f), Color.rgb(52, 211, 153), true)
        if (victory) (0 until 2).forEach { k -> val mx4 = w / 2 + (if (k == 0) -1f else 1f) * dp(196f); poly(c, mx4, y + dp(50f), dp(3.8f), 4, time * 3.6f + k * 1.9f, Color.rgb(250, 204, 21)) } // #727 gold sparks flank the victory rank""",
"#727")

# #728 gold ticks counter-orbit the countdown
rep("""            for (k in 0 until 4) { val ta2 = k * 1.5708f + time * 1.2f; poly(c, w / 2 + cos(ta2) * dp(52f), h * 0.40f - dp(20f) + sin(ta2) * dp(52f), dp(3.4f), 4, ta2, CHARS[save.selectedChar].color) } // #619 ticks orbit the countdown""",
"""            for (k in 0 until 4) { val ta2 = k * 1.5708f + time * 1.2f; poly(c, w / 2 + cos(ta2) * dp(52f), h * 0.40f - dp(20f) + sin(ta2) * dp(52f), dp(3.4f), 4, ta2, CHARS[save.selectedChar].color) } // #619 ticks orbit the countdown
            (0 until 4).forEach { k -> val ta3 = k * 1.5708f - time * 1.2f; poly(c, w / 2 + cos(ta3) * dp(66f), h * 0.40f - dp(20f) + sin(ta3) * dp(66f), dp(2.4f), 4, ta3, Color.rgb(250, 204, 21)) } // #728 gold ticks counter-orbit the countdown""",
"#728")

# #729 pink diamond heralds the Warden's banner
rep("""            txt(c, sub, w / 2, h * 0.3f + dp(34f), dp(15f), Color.WHITE, false, a)""",
"""            txt(c, sub, w / 2, h * 0.3f + dp(34f), dp(15f), Color.WHITE, false, a)
            if (bossWave) poly(c, w / 2 - tw3 / 2 - dp(26f), h * 0.3f + dp(30f), dp(4f), 4, time * 4f, Color.rgb(244, 114, 182)) // #729 a pink diamond heralds the Warden's banner""",
"#729")

# #730 a spark trails the XP tip
rep("""        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow""",
"""        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow
        if (xpFrac > 0.1f) { p.color = Color.rgb(52, 211, 153); p.alpha = 90; c.drawCircle(m + barW * xpFrac - dp(7f), xpY + dp(4.5f), dp(1.4f), p); p.alpha = 255 } // #730 a spark trails the XP tip""",
"#730")

# #731 wave spark marks the hero's best-wave line
rep("""        txtL(c, "🌊 Best wave with ${chd.name}: ${save.bestWaveChar(save.selectedChar)}", dp(28f), y + dp(138f), dp(12f), Color.rgb(148, 163, 184))""",
"""        txtL(c, "🌊 Best wave with ${chd.name}: ${save.bestWaveChar(save.selectedChar)}", dp(28f), y + dp(138f), dp(12f), Color.rgb(148, 163, 184))
        if (save.bestWaveChar(save.selectedChar) > 0) poly(c, dp(20f), y + dp(134f), dp(3.4f), 4, time * 3.4f, Color.rgb(96, 165, 250)) // #731 a wave spark marks your best run with this hero""",
"#731")

# #732 price-sort toggle sparkles while active
rep("""        button(c, RectF(w - dp(132f), dp(82f), w - dp(14f), dp(130f)), if (save.shopSort) "↕ PRICE ✓" else "↕ PRICE", if (save.shopSort) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 12f, "sorttoggle") // #341 + #380""",
"""        button(c, RectF(w - dp(132f), dp(82f), w - dp(14f), dp(130f)), if (save.shopSort) "↕ PRICE ✓" else "↕ PRICE", if (save.shopSort) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 12f, "sorttoggle") // #341 + #380
        if (save.shopSort) poly(c, w - dp(73f), dp(140f), dp(3.6f), 4, time * 3.6f, Color.rgb(250, 204, 21)) // #732 the price-sort toggle sparkles while active""",
"#732")

# #733 sparks orbit a full rocket charge
rep("""                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }
        }""",
"""                p.color = if (ci < rocketCharges) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105)
                c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(3.4f), p)
            }
            if (rocketCharges >= rocketMaxCharges) (0 until 2).forEach { k -> val ra2 = time * 3.4f + k * 3.14159f; poly(c, rocketBtnRect.centerX() + cos(ra2) * dp(34f), rocketBtnRect.centerY() + sin(ra2) * dp(34f), dp(2.8f), 4, ra2, Color.rgb(254, 240, 138)) } // #733 sparks orbit a full rocket charge
        }""",
"#733")

# #734 studs pin the RESUME button's corners
rep("""        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")""",
"""        button(c, RectF(x0, h * 0.42f, x0 + bw, h * 0.42f + bh), "▶  RESUME", Color.rgb(22, 163, 74), Color.WHITE, 19f, "resume")
        (0 until 2).forEach { k -> poly(c, (if (k == 0) x0 + dp(10f) else x0 + bw - dp(10f)), h * 0.42f + dp(10f), dp(3.2f), 4, time * 3.2f + k * 1.9f, Color.rgb(187, 247, 208)) } // #734 studs pin the RESUME button's corners""",
"#734")

# #735 gold rule underlines ELITE DOWN
rep("""            for (k in 0 until 2) { val sx9 = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx9, h * 0.2f - dp(7f), dp(4f), 4, time * 4f + k * 1.7f, Color.rgb(250, 204, 21)) } // #660 stars spin at the elite shout's shoulders""",
"""            for (k in 0 until 2) { val sx9 = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx9, h * 0.2f - dp(7f), dp(4f), 4, time * 4f + k * 1.7f, Color.rgb(250, 204, 21)) } // #660 stars spin at the elite shout's shoulders
            p.color = Color.rgb(250, 204, 21); p.alpha = (a2 * 0.7f).toInt(); c.drawRect(w / 2 - dp(88f), h * 0.2f + dp(6f), w / 2 + dp(88f), h * 0.2f + dp(8f), p); p.alpha = 255 // #735 a gold rule underlines ELITE DOWN""",
"#735")

# #736 gold heart spins in the shell gem on boss waves
rep("""poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, if (waveClearT > 0) Color.rgb((127 + 127 * sin(time * 9f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 2.1f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 4.2f)).toInt().coerceIn(0, 255)) else Color.rgb(34, 211, 238)) // #494 victory rainbow gem""",
"""poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, if (waveClearT > 0) Color.rgb((127 + 127 * sin(time * 9f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 2.1f)).toInt().coerceIn(0, 255), (127 + 127 * sin(time * 9f + 4.2f)).toInt().coerceIn(0, 255)) else Color.rgb(34, 211, 238)) // #494 victory rainbow gem
                if (wave % 5 == 0) poly(c, bpx, bpy, dp(3.6f) * spawnSc, 4, -time * 3f, Color.rgb(250, 204, 21)) // #736 a gold heart spins in the shell gem on boss waves""",
"#736")

assert s != orig
open(P, "w").write(s)
print("patch_loop20 applied: 12 edits")
