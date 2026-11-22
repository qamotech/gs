#!/usr/bin/env python3
"""GemStorm v2.6.0 graphics+resolution upgrades #761-784. Draw-only."""
P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

rep("""            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core""",
"""            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = ncol; p.alpha = (20 * np2).toInt(); c.drawCircle(nx, ny, dp(58f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #761 a hairline ring rims the nebula core""",
"#761")

rep("""            for (tk in 0 until 4) { // #441 rotating crosshair ticks""",
"""            for (tk in 0 until 4) { // #441 rotating crosshair ticks
            poly(c, m.x, m.y, dp(5f), 4, -time * 3f, Color.rgb(249, 115, 22)) // #762 an inner diamond counter-turns in the crosshair""",
"#762")

rep("""            c.drawCircle(fp.x + sin(time * 9.3f + fp.y) * dp(3f), fp.y + cos(time * 7.7f + fp.x) * dp(3f), fp.r * 0.3f, p)""",
"""            c.drawCircle(fp.x + sin(time * 9.3f + fp.y) * dp(3f), fp.y + cos(time * 7.7f + fp.x) * dp(3f), fp.r * 0.3f, p)
            p.color = Color.WHITE; p.alpha = (110 * fade).toInt(); c.drawCircle(fp.x, fp.y, dp(1.5f), p); p.alpha = 255 // #763 a white speck hearts every fire pool""",
"#763")

rep("""            c.drawCircle(pl.x + cos(-time * 1.1f + pl.y * 0.05f) * pl.r * 0.3f, pl.y + sin(-time * 0.9f + pl.x * 0.05f) * pl.r * 0.3f, pl.r * 0.3f, p)""",
"""            c.drawCircle(pl.x + cos(-time * 1.1f + pl.y * 0.05f) * pl.r * 0.3f, pl.y + sin(-time * 0.9f + pl.x * 0.05f) * pl.r * 0.3f, pl.r * 0.3f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(20, 83, 45); p.alpha = (90 * fade).toInt(); c.drawCircle(pl.x, pl.y, pl.r - dp(1f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #764 a dark rim rings every poison pool""",
"#764")

rep("""        for (d in decals) {""",
"""        for (d in decals) {
            if (d.life > 4f) { p.color = Color.rgb(249, 115, 22); p.alpha = 36; c.drawCircle(d.x, d.y, dp(4f), p); p.alpha = 255 } // #765 fresh scorch glows at the center""",
"#765")

rep("""            if (i % 5 == 0) { p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 3f + i * 2.3f)).toInt().coerceIn(10, 90); c.drawCircle(mx, my, dp(0.9f), p) } // #605 every fifth dust mote glints""",
"""            if (i % 5 == 0) { p.color = Color.WHITE; p.alpha = (50 + 40 * sin(time * 3f + i * 2.3f)).toInt().coerceIn(10, 90); c.drawCircle(mx, my, dp(0.9f), p) } // #605 every fifth dust mote glints
            if (i % 5 == 0) { p.color = Color.rgb(165, 243, 252); p.alpha = 55; c.drawCircle(mx + dp(3f), my - dp(2f), dp(0.6f), p); p.alpha = 255 } // #766 glinting motes trail a smaller twin""",
"#766")

rep("""        for (s in sparks) {""",
"""        for (s in sparks) {
            p.color = s.color; p.alpha = 60; c.drawCircle(s.x - dp(2.5f), s.y - dp(2.5f), s.size * 0.45f, p); p.alpha = 255 // #767 every spark trails a fading twin""",
"#767")

rep("""            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))""",
"""            poly(c, g.x, g.y + bob, r * 0.5f, 4, 0.78f, Color.rgb(167, 243, 208))
            p.color = Color.WHITE; p.alpha = 110; c.drawRect(g.x - dp(0.6f), g.y + bob - r * 0.34f, g.x + dp(0.6f), g.y + bob + r * 0.34f, p); p.alpha = 255 // #768 a facet line splits every gem""",
"#768")

rep("""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))""",
"""            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            p.color = Color.rgb(185, 28, 28); p.alpha = (200 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 200); c.drawCircle(hx3, hy3 + hr * 0.18f, hr * 0.22f, p); p.alpha = 255 // #769 a deep-red core hearts the pickup pop""",
"#769")

rep("""        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)""",
"""        txt(c, "⭐ LEVEL $level ⭐", w / 2, h * 0.2f, dp(27f), Color.rgb(52, 211, 153), true)
        p.color = Color.rgb(250, 204, 21); p.alpha = (60 + 40 * sin(time * 3f)).toInt().coerceIn(20, 100); c.drawRect(w / 2 - dp(84f), h * 0.2f + dp(8f), w / 2 + dp(84f), h * 0.2f + dp(10f), p); p.alpha = 255 // #770 a gold rule underlines LEVEL UP""",
"#770")

rep("""        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)""",
"""        txt(c, "⏸ PAUSED", w / 2, h * 0.3f, dp(30f), Color.WHITE, true)
        (0 until 2).forEach { k -> poly(c, w / 2 + (if (k == 0) -1f else 1f) * dp(122f), h * 0.3f - dp(11f), dp(4f), 4, time * 3.4f + k * 1.9f, CHARS[save.selectedChar].color) } // #771 diamonds flank the PAUSED title""",
"#771")

rep("""        menuButton(c, RectF(x0, stag(0, y0), x0 + bw, stag(0, y0) + bh), "▶", "ENTER THE ABYSS", Color.rgb(22, 163, 74), "play", 20f, Color.rgb(74, 222, 128))""",
"""        menuButton(c, RectF(x0, stag(0, y0), x0 + bw, stag(0, y0) + bh), "▶", "ENTER THE ABYSS", Color.rgb(22, 163, 74), "play", 20f, Color.rgb(74, 222, 128))
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(74, 222, 128); p.alpha = (40 + 30 * sin(time * 2.6f)).toInt().coerceIn(10, 70); c.drawRoundRect(RectF(x0 - dp(3f), stag(0, y0) - dp(3f), x0 + bw + dp(3f), stag(0, y0) + bh + dp(3f)), dp(14f), dp(14f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #772 a halo breathes around ENTER THE ABYSS""",
"#772")

rep("""        txtFitC(c, "💎 ${save.gemBank}  •  🌊 Best wave ${save.bestWave}  •  by QamoTech 💎N🎱  •  v2.5 — 736-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), w - dp(16f), false) // #337 footer chips""",
"""        txtFitC(c, "💎 ${save.gemBank}  •  🌊 Best wave ${save.bestWave}  •  by QamoTech 💎N🎱  •  v2.5 — 736-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), w - dp(16f), false) // #337 footer chips
        (0 until 2).forEach { k -> poly(c, (if (k == 0) dp(14f) else w - dp(14f)), h - dp(18f), dp(3.2f), 4, time * 3f + k * 1.9f, Color.rgb(52, 211, 153)) } // #773 gems pin the menu footer corners""",
"#773")

rep("""            txt(c, "💨", pvx + dp(70f), pvy + dp(8f), dp(20f), Color.WHITE)""",
"""            txt(c, "💨", pvx + dp(70f), pvy + dp(8f), dp(20f), Color.WHITE)
            poly(c, pvx - dp(70f), pvy, dp(3.4f), 4, time * 3.8f, Color.rgb(250, 204, 21)) // #774 a gem spins on the preview stick""",
"#774")

rep("""        if (save.bestWaveChar(save.selectedChar) > 0) poly(c, dp(20f), y + dp(134f), dp(3.4f), 4, time * 3.4f, Color.rgb(96, 165, 250)) // #731 a wave spark marks your best run with this hero""",
"""        if (save.bestWaveChar(save.selectedChar) > 0) poly(c, dp(20f), y + dp(134f), dp(3.4f), 4, time * 3.4f, Color.rgb(96, 165, 250)) // #731 a wave spark marks your best run with this hero
        if (save.bestWaveChar(save.selectedChar) >= 10) poly(c, dp(20f), y + dp(134f), dp(5.4f), 4, -time * 2.6f, Color.rgb(250, 204, 21)) // #775 a gold ring honors a 10+ best wave""",
"#775")

rep("""        if (save.shopSort) poly(c, w - dp(73f), dp(140f), dp(3.6f), 4, time * 3.6f, Color.rgb(250, 204, 21)) // #732 the price-sort toggle sparkles while active""",
"""        if (save.shopSort) poly(c, w - dp(73f), dp(140f), dp(3.6f), 4, time * 3.6f, Color.rgb(250, 204, 21)) // #732 the price-sort toggle sparkles while active
        if (save.shopSort) poly(c, w - dp(73f), dp(152f), dp(2.4f), 4, -time * 3f, Color.rgb(196, 181, 253)) // #776 a violet twin sparkles under the sort toggle""",
"#776")

rep("""        txt(c, "Neon Abyss — version $ver (736-upgrade edition)", w / 2, y, dp(14f), Color.rgb(203, 213, 225)); y += dp(48f)""",
"""        txt(c, "Neon Abyss — version $ver (736-upgrade edition)", w / 2, y, dp(14f), Color.rgb(203, 213, 225)); y += dp(48f)
        (0 until 2).forEach { k -> val ax5 = w / 2 + (if (k == 0) -1f else 1f) * dp(158f); p.color = Color.rgb(250, 204, 21); p.alpha = (90 + 60 * sin(time * 3.6f + k * 2.2f)).toInt().coerceIn(30, 150); c.drawCircle(ax5, y - dp(53f), dp(1.8f), p) }; p.alpha = 255 // #777 dots flank the About version line""",
"#777")

rep("""            (0 until 4).forEach { k -> val ta3 = k * 1.5708f - time * 1.2f; poly(c, w / 2 + cos(ta3) * dp(66f), h * 0.40f - dp(20f) + sin(ta3) * dp(66f), dp(2.4f), 4, ta3, Color.rgb(250, 204, 21)) } // #728 gold ticks counter-orbit the countdown""",
"""            (0 until 4).forEach { k -> val ta3 = k * 1.5708f - time * 1.2f; poly(c, w / 2 + cos(ta3) * dp(66f), h * 0.40f - dp(20f) + sin(ta3) * dp(66f), dp(2.4f), 4, ta3, Color.rgb(250, 204, 21)) } // #728 gold ticks counter-orbit the countdown
            (0 until 4).forEach { k -> val ca4 = k * 1.5708f; p.color = CHARS[save.selectedChar].color; p.alpha = 110; c.drawCircle(w / 2 + cos(ca4) * dp(58f), h * 0.40f - dp(20f) + sin(ca4) * dp(58f), dp(1.8f), p) }; p.alpha = 255 // #778 cardinal ticks stud the countdown ring""",
"#778")

rep("""            if (bossWave) poly(c, w / 2 - tw3 / 2 - dp(26f), h * 0.3f + dp(30f), dp(4f), 4, time * 4f, Color.rgb(244, 114, 182)) // #729 a pink diamond heralds the Warden's banner""",
"""            if (bossWave) poly(c, w / 2 - tw3 / 2 - dp(26f), h * 0.3f + dp(30f), dp(4f), 4, time * 4f, Color.rgb(244, 114, 182)) // #729 a pink diamond heralds the Warden's banner
            if (bossWave) poly(c, w / 2 + tw3 / 2 + dp(26f), h * 0.3f + dp(30f), dp(4f), 4, -time * 4f, Color.rgb(244, 114, 182)) // #779 a twin diamond answers on the banner's right""",
"#779")

rep("""            p.color = Color.rgb(250, 204, 21); p.alpha = (a2 * 0.7f).toInt(); c.drawRect(w / 2 - dp(88f), h * 0.2f + dp(6f), w / 2 + dp(88f), h * 0.2f + dp(8f), p); p.alpha = 255 // #735 a gold rule underlines ELITE DOWN""",
"""            p.color = Color.rgb(250, 204, 21); p.alpha = (a2 * 0.7f).toInt(); c.drawRect(w / 2 - dp(88f), h * 0.2f + dp(6f), w / 2 + dp(88f), h * 0.2f + dp(8f), p); p.alpha = 255 // #735 a gold rule underlines ELITE DOWN
            p.color = Color.rgb(254, 240, 138); p.alpha = a2; c.drawRect(w / 2 - dp(92f), h * 0.2f + dp(4f), w / 2 - dp(90f), h * 0.2f + dp(10f), p); c.drawRect(w / 2 + dp(90f), h * 0.2f + dp(4f), w / 2 + dp(92f), h * 0.2f + dp(10f), p); p.alpha = 255 // #780 end ticks cap the ELITE DOWN rule""",
"#780")

rep("""                if (ci < rocketCharges) { p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 5f + ci)).toInt().coerceIn(20, 100); c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(6.4f), p); p.alpha = 255 } // #565 charged pips glow""",
"""                if (ci < rocketCharges) { p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 5f + ci)).toInt().coerceIn(20, 100); c.drawCircle(rocketBtnRect.centerX() + (ci - (rocketMaxCharges - 1) / 2f) * dp(13f), rocketBtnRect.bottom + dp(8f), dp(6.4f), p); p.alpha = 255 } // #565 charged pips glow
                if (ci == 0 && rocketCharges >= rocketMaxCharges) { p.color = Color.WHITE; p.alpha = (120 + 80 * sin(time * 6f)).toInt().coerceIn(40, 200); c.drawCircle(rocketBtnRect.centerX(), rocketBtnRect.bottom + dp(8f), dp(1.8f), p); p.alpha = 255 } // #781 a white cap crowns a full rocket rack""",
"#781")

rep("""                c.drawArc(RectF(dashBtnRect.left + dp(4f), dashBtnRect.top + dp(4f), dashBtnRect.right - dp(4f), dashBtnRect.bottom - dp(4f)), -90f, 360f * dprog2, false, p)""",
"""                c.drawArc(RectF(dashBtnRect.left + dp(4f), dashBtnRect.top + dp(4f), dashBtnRect.right - dp(4f), dashBtnRect.bottom - dp(4f)), -90f, 360f * dprog2, false, p)
                val ta4 = (-90f + 360f * dprog2) * 0.01745f; p.style = Paint.Style.FILL; p.color = Color.WHITE; p.alpha = 210; c.drawCircle(dashBtnRect.centerX() + cos(ta4) * (dr - dp(4f)), dashBtnRect.centerY() + sin(ta4) * (dr - dp(4f)), dp(2f), p); p.alpha = 255 // #782 a bright tip rides the dash recharge arc""",
"#782")

rep("""                c.drawCircle(joyOx, joyOy, dp(60f) * save.joySize, p); p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawCircle(joyOx, joyOy, dp(60f) * save.joySize, p); p.style = Paint.Style.FILL; p.alpha = 255
                (0 until 4).forEach { k -> val ja = k * 1.5708f + 0.785f; p.color = Color.rgb(165, 243, 252); p.alpha = 80; c.drawCircle(joyOx + cos(ja) * dp(60f) * save.joySize, joyOy + sin(ja) * dp(60f) * save.joySize, dp(1.6f), p) }; p.alpha = 255 // #783 tick dots stud the stick's halo""",
"#783")

rep("""        p.color = Color.rgb(250, 204, 21); p.alpha = (40 + 30 * sin(time * 2.2f)).toInt().coerceIn(10, 70); c.drawRect(cx - lws.sum() / 2, cy + dp(116f), cx + lws.sum() / 2, cy + dp(117.5f), p); p.alpha = 255 // #725 a gold rule underlines the title""",
"""        p.color = Color.rgb(250, 204, 21); p.alpha = (40 + 30 * sin(time * 2.2f)).toInt().coerceIn(10, 70); c.drawRect(cx - lws.sum() / 2, cy + dp(116f), cx + lws.sum() / 2, cy + dp(117.5f), p); p.alpha = 255 // #725 a gold rule underlines the title
        (0 until 2).forEach { k -> poly(c, cx + (if (k == 0) -1f else 1f) * (lws.sum() / 2 + dp(8f)), cy + dp(116f), dp(3f), 4, time * 3.4f + k * 1.9f, Color.rgb(250, 204, 21)) } // #784 diamonds cap the title rule""",
"#784")

assert s != orig
open(P, "w").write(s)
print("patch_v260_b applied")
