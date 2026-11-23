#!/usr/bin/env python3
"""GemStorm v2.7.0 graphics batch #801-#824 — 24 small draw enhancements,
each anchored on a verified draw site, paint state restored after every add."""

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read(); orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

def after(old, add, tag):
    rep(old, old + "\n" + add, tag)

def before(old, add, tag):
    rep(old, add + "\n" + old, tag)

# ---- hero tail: #801-#805 (inserted where the badge used to sit) ----
before("            // v2.6.1: the #34 level badge is OFF the hero (Nathan)",
"""            poly(c, px + cos(time * 2.2f) * dp(19f), py + sin(time * 2.2f) * dp(19f), dp(2.4f), 4, time * 3f, Color.WHITE) // #801 a glint rides the shell rim
            poly(c, px - cos(time * 1.7f) * dp(21f), py - sin(time * 1.7f) * dp(21f), dp(1.8f), 4, -time * 2.4f, chCol) // #802 a hero-colored mote counter-orbits the glint
            if (save.manualAim) for (k in 1..3) { p.color = chCol; p.alpha = 96 - k * 24; c.drawCircle(px + cos(lastAim) * dp(32f + k * 15f), py + sin(lastAim) * dp(32f + k * 15f), dp(2.8f) - k * 0.5f, p) }; p.alpha = 255 // #803 manual-aim guide dots — your line of fire, faintly dotted
            if (hp < maxHp * 0.25f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 100 * sin(time * 6f)).toInt().coerceIn(20, 220); c.drawCircle(px, py, dp(31f) + dp(3f) * sin(time * 6f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #804 a heartbeat ring throbs when your HP is critical
            if (dashT > 0) { p.color = Color.BLACK; p.alpha = 70; c.drawCircle(px, py + dp(25f), dp(15f), p); p.alpha = 255 } // #805 a grounding shadow pools under a dashing hero""",
    "#801-805 hero")

# #806 muzzle third star
after("                poly(c, mx, my, dp(4f), 4, -time * 30f, Color.WHITE) // #627 the flash burns white at the core",
"                poly(c, mx, my, dp(2.2f), 4, time * 41f, Color.rgb(254, 240, 138)) // #806 a third, smaller star spins against the flash",
    "#806 muzzle")
# #807 dash ring third echo
after("                p.strokeWidth = dp(1.4f); p.alpha = 90; c.drawCircle(px, py, (dp(30f) + (0.16f - dashT) * dp(120f)) * 0.72f, p) // #623 the dash ring echoes inside itself",
"                p.strokeWidth = dp(1f); p.alpha = 50; c.drawCircle(px, py, (dp(30f) + (0.16f - dashT) * dp(120f)) * 0.45f, p); p.style = Paint.Style.FILL; p.alpha = 255 // #807 a third, fainter echo rides inside the dash ring",
    "#807 dash echo")
# #808 XP gold tip orb
after("        if (xpFrac > 0.1f) { p.color = Color.rgb(52, 211, 153); p.alpha = 90; c.drawCircle(m + barW * xpFrac - dp(7f), xpY + dp(4.5f), dp(1.4f), p); p.alpha = 255 } // #730 a spark trails the XP tip",
"        if (xpFrac > 0.9f) { p.color = Color.rgb(250, 204, 21); p.alpha = (150 + 90 * sin(time * 8f)).toInt().coerceIn(60, 240); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(3.4f), p); p.alpha = 255 } // #808 the XP tip swells gold when a level is about to pop",
    "#808 xp orb")
# #809 LV underline
after('        txtL(c, "LV $level", m + barW + dp(8f), xpY + dp(9f), dp(12f), if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), true) // #594 LV runs gold when a level is close',
"        p.color = if (xpFrac > 0.9f) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153); p.alpha = 150; c.drawRect(m + barW + dp(8f), xpY + dp(11.5f), m + barW + dp(34f), xpY + dp(13f), p); p.alpha = 255 // #809 a thin rule underlines your level",
    "#809 lv rule")
# #810 SCORE shadow (inserted before the score text)
before('        txtR(c, "SCORE $score", w - m - dp(52f), m + dp(15f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.WHITE, true) // #492',
'        txtR(c, "SCORE $score", w - m - dp(50.5f), m + dp(16.5f), dp(13.5f) * (1f + 0.3f * max(0f, scorePop)), Color.rgb(2, 6, 16), true) // #810 the score wears a soft drop shadow',
    "#810 score shadow")
# #811 WAVE underline sweep
after('        txt(c, if (endlessMode) "WAVE $wave ∞" else "WAVE $wave/$MAX_WAVE", w / 2, m + dp(47f), dp(15f), if (endlessMode) Color.rgb(250, 204, 21) else Color.rgb(233, 213, 255), true) // v2.3 own row — collides with nothing; #583 endless runs gold',
"        p.color = if (endlessMode) Color.rgb(250, 204, 21) else Color.rgb(167, 139, 250); p.alpha = (110 + 60 * sin(time * 3f)).toInt().coerceIn(50, 170); c.drawRect(w / 2 - dp(52f), m + dp(52f), w / 2 + dp(52f), m + dp(53.5f), p); p.alpha = 255 // #811 a hairline sweeps under the wave title",
    "#811 wave rule")
# #812 portal counter ring
after("            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = CHARS[save.selectedChar].color; p.alpha = 70; c.drawCircle(bpx, bpy, dp(31f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #740 the level-up ring echoes inside itself",
"            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = Color.WHITE; p.alpha = 60; c.drawCircle(bpx, bpy, dp(24f) * spawnSc + dp(2f) * sin(time * 9f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #812 a white counter-ring shivers inside the level-up halo",
    "#812 portal ring")
# #813 nebula shooting star
after("            if (sin(time * 1.9f + nx * 0.021f + ny * 0.013f) > 0.93f) poly(c, nx, ny, dp(7f), 4, time * 1.2f, Color.WHITE) // #548 nebula cores twinkle",
"            if (sin(time * 0.43f + nx * 0.017f) > 0.997f) { p.color = Color.WHITE; p.alpha = 170; p.strokeWidth = dp(1.6f); c.drawLine(nx - dp(26f), ny - dp(15f), nx + dp(6f), ny + dp(4f), p); p.alpha = 255 } // #813 a rare shooting star rakes across the nebula",
    "#813 shooting star")
# #814 nebula core dot
after("            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core",
"            p.color = Color.WHITE; p.alpha = (36 * np2).toInt(); c.drawCircle(nx, ny, dp(9f), p); p.alpha = 255 // #814 a bright heart beats at the nebula's core",
    "#814 nebula heart")
# #815 heart under-glow (inserted before the drop heart is drawn)
before("drawHeart(c, hd.x, hd.y, r, Color.rgb(239, 68, 68))",
"p.color = Color.rgb(239, 68, 68); p.alpha = (44 + 26 * sin(time * 5f)).toInt().coerceIn(18, 70); c.drawCircle(hd.x, hd.y, r * 2f, p); p.alpha = 255 // #815 a soft red under-glow pools beneath every heart drop\n            drawHeart(c, hd.x, hd.y, r, Color.rgb(239, 68, 68))",
    "#815 heart glow")
# #816 rocket nose pulse
after("if (pr.rocket) { // 🚀 rocket flame + hot nose",
"{ val ra2 = atan2(pr.vy, pr.vx); p.color = Color.rgb(254, 240, 138); p.alpha = (130 + 90 * sin(time * 18f)).toInt().coerceIn(40, 220); c.drawCircle(pr.x + cos(ra2) * pr.radius * 1.15f, pr.y + sin(ra2) * pr.radius * 1.15f, dp(3.4f), p); p.alpha = 255 } // #816 the rocket's nose cone pulses hot",
    "#816 rocket nose")
# #817 + #818 fire pool rings
after("            p.color = Color.WHITE; p.alpha = (110 * fade).toInt(); c.drawCircle(fp.x, fp.y, dp(1.5f), p); p.alpha = 255 // #763 a white speck hearts every fire pool",
"""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(249, 115, 22); p.alpha = (90 * fade).toInt(); c.drawCircle(fp.x, fp.y, fp.r * (0.92f + 0.05f * sin(time * 13f + fp.y)), p); p.style = Paint.Style.FILL // #817 an ember rim ring breathes at the pool's edge
            p.color = Color.rgb(254, 240, 138); p.alpha = (26 * fade).toInt(); c.drawCircle(fp.x, fp.y, fp.r * 1.35f, p); p.alpha = 255 // #818 a heat-shimmer halo hangs over the pool""",
    "#817/#818 fire pool")
# #819 Warden arrow second halo
after("                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(244, 114, 182); p.alpha = (90 + 70 * sin(time * 6f)).toInt().coerceIn(20, 160); c.drawCircle(ax, ay, dp(17f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #596 the Warden's off-screen arrow wears a halo",
"                if (e.type == 5) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(244, 114, 182); p.alpha = (50 + 40 * sin(time * 6f + 1.5f)).toInt().coerceIn(10, 90); c.drawCircle(ax, ay, dp(23f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #819 a second, wider halo echoes the Warden's arrow",
    "#819 warden halo")
# #820 Gnome arrow sparkle
after("                if (e.type == 11) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (80 + 60 * sin(time * 5f)).toInt().coerceIn(20, 140); c.drawCircle(ax, ay, dp(13f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #606 the Gnome's arrow wears a gold halo — follow the money",
"                if (e.type == 11) poly(c, ax, ay, dp(3f), 4, time * 4f, Color.rgb(254, 240, 138)) // #820 a sparkle spins at the Gnome arrow's heart",
    "#820 gnome sparkle")
# #821 boss arrival bloom
after("        rings.add(Ring(e.x, e.y, dp(24f), 0.5f, 0.5f, Color.rgb(244, 114, 182))) // #614 the Warden's arrival rings out",
"        blooms.add(Bloom(e.x, e.y, 0.4f, dp(95f), Color.rgb(244, 114, 182))) // #821 the Warden's arrival blooms pink across the arena",
    "#821 boss bloom")
# #822 footer gem (inserted before the footer line)
before('        txtFitC(c, "💎 ${save.gemBank}',
'        poly(c, cx, h - dp(31f), dp(3.2f) + dp(0.9f) * sin(time * 3f), 4, time * 1.5f, Color.rgb(34, 211, 238)) // #822 a small gem pulses above the menu footer',
    "#822 footer gem")
# #823 pause button inner ring
after("        roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 238) // v2.6 pause button more opaque",
"        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(167, 139, 250); p.alpha = 150; c.drawRoundRect(RectF(pvR.left + dp(4f), pvR.top + dp(4f), pvR.right - dp(4f), pvR.bottom - dp(4f)), dp(7f), dp(7f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #823 a violet hairline rings the pause button's face",
    "#823 pause ring")
# #824 combo spark
after('txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)',
"            poly(c, m + dp(4f), xpY + dp(38f), dp(4.6f), 4, time * 5f, Color.rgb(254, 240, 138)) // #824 a hot spark spins beside the combo counter",
    "#824 combo spark")

assert s != orig
open(P, "w").write(s)
print("patch_v270_gfx applied")
