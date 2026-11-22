#!/usr/bin/env python3
"""GemStorm v2.6.0 graphics+resolution upgrades #737-760. Draw-only."""
P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

rep("""p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)""",
"""p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.1f); p.color = shade(chCol, 1.3f); p.alpha = 80; c.drawCircle(bpx, bpy, dp(14.2f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #737 an inner ring gives the dome depth""",
"#737")

rep("""p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)""",
"""p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                (0 until 8).forEach { k -> val sa3 = k * 0.785f + time * 0.6f; p.color = Color.WHITE; p.alpha = 100; c.drawCircle(bpx + cos(sa3) * dp(21f) * spawnSc, bpy + sin(sa3) * dp(21f) * spawnSc, dp(0.9f), p) }; p.alpha = 255 // #738 glints ride the shell rim""",
"#738")

rep("""                if (wave % 5 == 0) poly(c, bpx, bpy, dp(3.6f) * spawnSc, 4, -time * 3f, Color.rgb(250, 204, 21)) // #736 a gold heart spins in the shell gem on boss waves""",
"""                if (wave % 5 == 0) poly(c, bpx, bpy, dp(3.6f) * spawnSc, 4, -time * 3f, Color.rgb(250, 204, 21)) // #736 a gold heart spins in the shell gem on boss waves
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = Color.WHITE; p.alpha = (50 + 30 * sin(time * 5f)).toInt().coerceIn(20, 80); c.drawCircle(bpx, bpy, dp(10.5f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #739 a halo rings the shell gem""",
"#739")

rep("""                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = Color.WHITE; p.alpha = (50 + 30 * sin(time * 5f)).toInt().coerceIn(20, 80); c.drawCircle(bpx, bpy, dp(10.5f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #739 a halo rings the shell gem""",
"""                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = Color.WHITE; p.alpha = (50 + 30 * sin(time * 5f)).toInt().coerceIn(20, 80); c.drawCircle(bpx, bpy, dp(10.5f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 // #739 a halo rings the shell gem
                for (sj in -1..1 step 2) { p.color = Color.rgb(254, 240, 138); p.alpha = (120 + 80 * sin(time * 9f + sj)).toInt().coerceIn(40, 200); c.drawCircle(bpx + sj * dp(13f) * spawnSc, bpy + dp(17f) * spawnSc, dp(1.5f), p) }; p.alpha = 255 // #741 sparks spit from the jet nozzles""",
"#741")

rep("""            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (110 + 80 * sin(time * 8f)).toInt().coerceIn(30, 190); c.drawCircle(bpx, bpy, dp(38f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #621 a level-up is about to pop""",
"""            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (110 + 80 * sin(time * 8f)).toInt().coerceIn(30, 190); c.drawCircle(bpx, bpy, dp(38f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #621 a level-up is about to pop
            if (xpNext > 0 && xp.toFloat() / xpNext > 0.95f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = CHARS[save.selectedChar].color; p.alpha = 70; c.drawCircle(bpx, bpy, dp(31f) * spawnSc, p); p.style = Paint.Style.FILL; p.alpha = 255 } // #740 the level-up ring echoes inside itself""",
"#740")

rep("""                for (k in 0 until 4) { val ta3 = k * 1.5708f + time * 0.9f; poly(c, e.x + cos(ta3) * e.radius * 1.25f, e.y + sin(ta3) * e.radius * 1.25f, dp(3f), 4, ta3, Color.rgb(220, 38, 38)) } // #636 thorn sparks ride the dread ring""",
"""                for (k in 0 until 4) { val ta3 = k * 1.5708f + time * 0.9f; poly(c, e.x + cos(ta3) * e.radius * 1.25f, e.y + sin(ta3) * e.radius * 1.25f, dp(3f), 4, ta3, Color.rgb(220, 38, 38)) } // #636 thorn sparks ride the dread ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(220, 38, 38); p.alpha = 36; c.drawCircle(e.x, e.y, e.radius * 1.12f, p); p.style = Paint.Style.FILL; p.alpha = 255 // #742 the dread ring echoes inside itself""",
"#742")

rep("""poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }""",
"""poly(c, e.x + cos(ca) * e.radius * 0.92f, e.y + sin(ca) * e.radius * 0.92f, dp(3.2f), 4, ca, Color.rgb(191, 219, 254)) }
                for (k in 0 until 3) { val ca2 = k * 2.09f + e.x * 0.05f; p.color = Color.WHITE; p.alpha = 170; c.drawCircle(e.x + cos(ca2) * e.radius * 0.92f, e.y + sin(ca2) * e.radius * 0.92f, dp(0.9f), p) }; p.alpha = 255 // #743 frost crystals cap with white tips""",
"#743")

rep("""poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))""",
"""poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))
                        if (i == 3) { p.color = Color.WHITE; p.alpha = (130 + 90 * sin(time * 6f)).toInt().coerceIn(40, 220); c.drawCircle(e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, dp(1.7f), p); p.alpha = 255 } // #748 the crown's center spike glints""",
"#748")

rep("""            // ---- main-unit detail pass ----""",
"""            if (e.type == 11) { p.color = Color.rgb(187, 247, 208); p.alpha = 210; c.drawCircle(e.x, e.y - e.radius * 1.15f - dp(5.5f), dp(2.1f), p); p.alpha = 255 } // #749 a pompom caps the gnome's hat
            // ---- main-unit detail pass ----""",
"#749")

rep("""poly(c, e.x, e.y, e.radius * (0.20f + 0.16f * eHpF) * (1f + 0.12f * sin(time * 6f + e.wob)), 4, time * 2f, shade(col, 1.55f))""",
"""poly(c, e.x, e.y, e.radius * (0.20f + 0.16f * eHpF) * (1f + 0.12f * sin(time * 6f + e.wob)), 4, time * 2f, shade(col, 1.55f))
                p.color = Color.WHITE; p.alpha = 150; c.drawCircle(e.x, e.y, dp(1.2f), p); p.alpha = 255 // #750 a white speck hearts every core""",
"#750")

rep("""c.drawArc(RectF(e.x - e.radius * 0.98f, e.y - e.radius * 0.98f, e.x + e.radius * 0.98f, e.y + e.radius * 0.98f), efa * 57.2958f - 42f, 84f, false, p)""",
"""c.drawArc(RectF(e.x - e.radius * 0.98f, e.y - e.radius * 0.98f, e.x + e.radius * 0.98f, e.y + e.radius * 0.98f), efa * 57.2958f - 42f, 84f, false, p)
            p.style = Paint.Style.FILL; p.color = Color.WHITE; p.alpha = 130; c.drawCircle(e.x + cos(efa) * e.radius * 0.98f, e.y + sin(efa) * e.radius * 0.98f, dp(1.6f), p); p.alpha = 255 // #751 a hot spot rides the rim light""",
"#751")

rep("""            p.color = Color.WHITE; p.alpha = 150; c.drawCircle(e.x, e.y, dp(1.2f), p); p.alpha = 255 // #750 a white speck hearts every core""",
"""            p.color = Color.WHITE; p.alpha = 150; c.drawCircle(e.x, e.y, dp(1.2f), p); p.alpha = 255 // #750 a white speck hearts every core
            if (e.type == 0) poly(c, e.x, e.y, e.radius * 0.52f, 3, atan2(py - e.y, px - e.x) + 1.57f, shade(col, 1.45f)) // #744 the chaser's inset triangle
            if (e.type == 2) poly(c, e.x, e.y, e.radius * 0.6f, 6, time * 0.8f, shade(col, 0.72f)) // #745 the brute's inset hexagon
            if (e.type == 6) poly(c, e.x, e.y, e.radius * 0.58f, 5, time * 2.2f, shade(col, 1.4f)) // #746 the weaver's inset pentagon
            if (e.type == 10) poly(c, e.x, e.y, e.radius * 0.6f, 6, 0.3f, shade(col, 1.35f)) // #747 the bulwark's inset hexagon""",
"#744-747")

rep("""        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)""",
"""        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)
        for (q in 1..3) { val qx2 = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 110; c.drawRect(qx2 - dp(0.7f), m + dp(1f), qx2 + dp(0.7f), m + barH - dp(1f), p) }; p.alpha = 255 // #752 the HP bar wears segment ticks""",
"#752")

rep("""        if (hpFrac in 0f..0.25f) { // #445 danger pulse on a dying bar""",
"""        if (hpFrac > 0.03f) { p.color = Color.WHITE; p.alpha = (120 + 70 * sin(time * 6f)).toInt().coerceIn(50, 190); c.drawCircle(m + barW * hpFrac, m + barH / 2, dp(2f), p); p.alpha = 255 } // #753 the HP tip glows
        if (hpFrac in 0f..0.25f) { // #445 danger pulse on a dying bar""",
"#753")

rep("""        for (q in 1..3) { val qx = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 130; c.drawRect(qx - dp(0.7f), xpY + dp(1f), qx + dp(0.7f), xpY + dp(8f), p) } // #517 XP quarter ticks""",
"""        for (q in 1..3) { val qx = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 130; c.drawRect(qx - dp(0.7f), xpY + dp(1f), qx + dp(0.7f), xpY + dp(8f), p) } // #517 XP quarter ticks
        for (q in 1..7 step 2) { val qx3 = m + barW * q / 8f; p.color = Color.rgb(7, 3, 18); p.alpha = 60; c.drawRect(qx3 - dp(0.5f), xpY + dp(2f), qx3 + dp(0.5f), xpY + dp(7f), p) }; p.alpha = 255 // #754 finer eighth ticks resolve the XP bar""",
"#754")

rep("""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""",
"""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)
            if (combo >= 100) (0 until 2).forEach { k -> poly(c, m + dp(6f) + k * dp(11f), xpY + dp(34f), dp(2.6f), 4, time * 4f + k * 2.1f, Color.rgb(251, 146, 60)) } // #755 twin sparks mark a 100+ combo""",
"#755")

rep("""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))""",
"""            if (wfrac > 0) roundRect(c, RectF(w / 2 - dp(42f), m + dp(52f), w / 2 - dp(42f) + dp(84f) * wfrac, m + dp(56f)), dp(2f), Color.rgb(52, 211, 153))
            p.color = Color.rgb(7, 3, 18); p.alpha = 160; c.drawCircle(w / 2 - dp(42f), m + dp(54f), dp(2.6f), p); c.drawCircle(w / 2 + dp(42f), m + dp(54f), dp(2.6f), p); p.alpha = 255 // #756 end caps pin the wave bar""",
"#756")

rep("""                val txp = w / 2 - dp(42f) + dp(84f) * wfrac""",
"""                val txp = w / 2 - dp(42f) + dp(84f) * wfrac
                p.color = Color.rgb(167, 243, 208); p.alpha = 120; c.drawCircle(txp - dp(8f), m + dp(54f), dp(1.5f), p); p.alpha = 255 // #757 the progress turtle kicks a wake dot""",
"#757")

rep("""        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))""",
"""        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))
        if (gemsPop > 0.05f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.rgb(52, 211, 153); p.alpha = (160 * gemsPop).toInt().coerceIn(0, 160); c.drawCircle(w - m - dp(170f), m + dp(32f), dp(6f) + dp(5f) * (1f - gemsPop), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #758 a ring pops off the gem counter""",
"#758")

rep("""            if (hpF2 < 0.25f && hpF2 > 0f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 100 * sin(time * 8f)).toInt().coerceIn(20, 220); c.drawCircle(bx + bw * hpF2, by + dp(6.5f), dp(3.4f), p); p.alpha = 255 } // #724 the boss bar's tip burns red under 25%""",
"""            if (hpF2 < 0.25f && hpF2 > 0f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 100 * sin(time * 8f)).toInt().coerceIn(20, 220); c.drawCircle(bx + bw * hpF2, by + dp(6.5f), dp(3.4f), p); p.alpha = 255 } // #724 the boss bar's tip burns red under 25%
            if (hpF2 in 0.4f..0.6f) { p.color = Color.rgb(250, 204, 21); p.alpha = (130 + 80 * sin(time * 6f)).toInt().coerceIn(50, 210); c.drawCircle(bx + bw * 0.5f, by + dp(6.5f), dp(2.6f), p); p.alpha = 255 } // #759 a gold bead marks the Warden's halfway point""",
"#759")

rep("""        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)""",
"""        txt(c, "❚❚", pvR.centerX(), pvR.centerY() + dp(7f), dp(15f), Color.WHITE)
        (0 until 4).forEach { k -> val sx5 = if (k % 2 == 0) pvR.left + dp(4f) else pvR.right - dp(4f); val sy5 = if (k < 2) pvR.top + dp(4f) else pvR.bottom - dp(4f); p.color = Color.rgb(148, 163, 184); p.alpha = 150; c.drawCircle(sx5, sy5, dp(1.2f), p) }; p.alpha = 255 // #760 screws pin the pause button's corners""",
"#760")

assert s != orig
open(P, "w").write(s)
print("patch_v260_a applied")
