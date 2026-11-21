#!/usr/bin/env python3
"""GemStorm graphics loop cycle 17 — upgrades #689-700."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""                    p.color = chd.color; p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255""",
"""                    p.color = chd.color; p.alpha = 46
                    c.drawCircle(dp(40f), y + dp(32f), dp(19f), p); p.alpha = 255
                    if (!owned && save.gemBank >= cost) poly(c, r.right - dp(14f), r.top + dp(12f), dp(4f), 4, time * 3f, Color.rgb(52, 211, 153)) // #689 affordable heroes wink a gem in the corner""")

E("""            button(c, RectF(tx, dp(82f), tx + tabW, dp(130f)), tabNames[t], // #380
                if (shopTab == t) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "tab:$t")""",
"""            button(c, RectF(tx, dp(82f), tx + tabW, dp(130f)), tabNames[t], // #380
                if (shopTab == t) Color.rgb(124, 58, 237) else Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "tab:$t")
            if (shopTab == t) { p.color = Color.rgb(250, 204, 21); p.alpha = (150 + 80 * sin(time * 4f)).toInt().coerceIn(70, 230); c.drawCircle(tx + tabW / 2, dp(122f), dp(2.4f), p); p.alpha = 255 } // #690 the active shop tab wears a gold bead""")

E("""            txtFitC(c, UPGRADE_NAMES[oi], r.centerX(), y0 + dp(38f), dp(19f), Color.WHITE, r.width() - dp(16f), true)""",
"""            poly(c, r.right - dp(18f), y0 + dp(18f), dp(4.4f), 4, time * 3.4f + oi, rarityCol) // #691 draft cards sparkle in their rarity color
            txtFitC(c, UPGRADE_NAMES[oi], r.centerX(), y0 + dp(38f), dp(19f), Color.WHITE, r.width() - dp(16f), true)""")

E("""        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)""",
"""        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)
        (0 until 2).forEach { k -> val sx7 = w / 2 + (if (k == 0) -1f else 1f) * dp(132f); poly(c, sx7, h * 0.2f - dp(9f), dp(4.6f), 4, time * 3.6f + k * 1.8f, Color.rgb(250, 204, 21)) } // #692 stars spin beside the LEVEL shout""")

E("""                if (upgradeCounts[oi] > 1) txt(c, "×${upgradeCounts[oi]}", ox + dp(19f), h * 0.2f + dp(79f), dp(9.5f), Color.rgb(250, 204, 21), true)""",
"""                if (upgradeCounts[oi] > 1) txt(c, "×${upgradeCounts[oi]}", ox + dp(19f), h * 0.2f + dp(79f), dp(9.5f), Color.rgb(250, 204, 21), true)
                if (upgradeCounts[oi] >= 3) { p.color = Color.rgb(250, 204, 21); p.alpha = 120; c.drawCircle(ox + dp(19f), h * 0.2f + dp(86f), dp(1.7f), p); p.alpha = 255 } // #693 stacked builds earn a gold dot""")

E("""        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)""",
"""        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)
        (0 until 2).forEach { k -> val px9 = w / 2 + (if (k == 0) -1f else 1f) * dp(104f); poly(c, px9, h * 0.3f - dp(10f), dp(4.2f), 4, time * 2.8f + k * 1.9f, CHARS[save.selectedChar].color) } // #694 hero-color sparks flank PAUSED""")

E("""        if (pBuild.isNotEmpty()) txtFitC(c, pBuild.take(62), w / 2, h * 0.3f + dp(58f), dp(12.5f), Color.rgb(250, 204, 21), w - dp(24f))""",
"""        if (pBuild.isNotEmpty()) { p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 80 * sin(time * 3f)).toInt().coerceIn(40, 200); c.drawCircle(dp(20f), h * 0.3f + dp(54f), dp(2f), p); p.alpha = 255 } // #695 your build line wears a gold bead
        if (pBuild.isNotEmpty()) txtFitC(c, pBuild.take(62), w / 2, h * 0.3f + dp(58f), dp(12.5f), Color.rgb(250, 204, 21), w - dp(24f))""")

E("""        button(c, RectF(x0 + chipW * 3 + dp(18f), chipY, x0 + bw, chipY + dp(48f)), "🔊 ${if (save.muteAll) "OFF" else "ON"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "muteall")""",
"""        button(c, RectF(x0 + chipW * 3 + dp(18f), chipY, x0 + bw, chipY + dp(48f)), "🔊 ${if (save.muteAll) "OFF" else "ON"}", Color.rgb(30, 41, 59), Color.WHITE, 11.5f, "muteall")
        (0 until 4).forEach { k -> val on = when (k) { 0 -> save.musicOn; 1 -> save.vibration; 2 -> save.shakeOn; else -> !save.muteAll }; if (on) { p.color = Color.rgb(74, 222, 128); p.alpha = 200; c.drawCircle(x0 + chipW * k + dp(6f) * k + chipW - dp(10f), chipY + dp(10f), dp(2.4f), p) } }; p.alpha = 255 // #696 live toggles wear a green bead""")

E("""        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p) // #569 pause frame breathes in your hero's color""",
"""        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p) // #569 pause frame breathes in your hero's color
        p.style = Paint.Style.FILL; (0 until 4).forEach { k -> val cx9 = if (k % 2 == 0) dp(12f) else w - dp(12f); val cy9 = if (k < 2) dp(12f) else h - dp(12f); poly(c, cx9, cy9, dp(4f), 4, time * 2f + k, CHARS[save.selectedChar].color) }; p.alpha = 255 // #697 the pause frame is studded at its corners""")

E("""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""",
"""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)
            if (combo >= 100) poly(c, m + dp(118f), xpY + dp(40f), dp(4.4f), 4, time * 5f, Color.rgb(250, 204, 21)) // #698 a gold star crowns a 100+ combo""")

E("""        for ((k, v) in lines) {
            txtL(c, k, w * 0.18f, y, dp(15f), Color.rgb(148, 163, 184), true)
            txtR(c, v, w * 0.82f, y, dp(16f), Color.WHITE, true)
            y += dp(30f)
        }""",
"""        for ((k, v) in lines) {
            txtL(c, k, w * 0.18f, y, dp(15f), Color.rgb(148, 163, 184), true)
            txtR(c, v, w * 0.82f, y, dp(16f), Color.WHITE, true)
            p.color = Color.rgb(88, 28, 135); p.alpha = 60; c.drawRect(w * 0.18f, y + dp(8f), w * 0.82f, y + dp(9f), p); p.alpha = 255 // #699 stat rows rest on hairlines
            y += dp(30f)
        }""")

E("""        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)""",
"""        (0 until 2).forEach { k -> val fx7 = cx + (if (k == 0) -1f else 1f) * (lws.sum() / 2 + dp(14f)); poly(c, fx7, cy + dp(96f), dp(5f), 4, time * 2.6f + k * 1.9f, Color.rgb(250, 204, 21)) } // #700 gold stars guard the title — the 700th upgrade
        txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(138f), dp(15f), Color.rgb(232, 121, 249), true)""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop17: {len(EDITS)} edits applied")
