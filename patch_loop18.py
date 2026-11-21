#!/usr/bin/env python3
"""GemStorm graphics loop cycle 18 — #701-712: vignette/hurt/last-stand/wave-clear
accents, onboard/whats-new/about polish, dash-demo + gallery details, heartbeat frame studs.
All draw-only; no gameplay/shooting changes."""
import re, sys

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# #701 vignette corners breathe
rep("""        // #73 vignette corners
        p.color = Color.BLACK; p.alpha = 30""",
"""        // #73 vignette corners
        p.color = Color.BLACK; p.alpha = (26 + 8 * sin(time * 0.9f)).toInt().coerceIn(18, 34) // #701 the vignette breathes""",
"#701")

# #702 white-hot seam inside the hurt flash
rep("""            p.alpha = (215 * hurtFlash).toInt().coerceIn(0, 215)
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }""",
"""            p.alpha = (215 * hurtFlash).toInt().coerceIn(0, 215)
            c.drawRect(0f, 0f, w, h, p)
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.WHITE; p.alpha = (90 * hurtFlash).toInt().coerceIn(0, 90); p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); c.drawRect(dp(9f), dp(9f), w - dp(9f), h - dp(9f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #702 a white-hot seam lines the hurt flash
        }""",
"#702")

# #703 warning diamonds flank LAST STAND
rep("""            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)
        }""",
"""            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)
            (0 until 2).forEach { k -> val sx = w / 2 + (if (k == 0) -1f else 1f) * dp(118f); poly(c, sx, h * 0.16f - dp(5f), dp(5f) * (1f + 0.25f * sin(time * 9f)), 4, time * 4f + k * 1.57f, Color.rgb(248, 113, 113)) } // #703 warning diamonds flank LAST STAND
        }""",
"#703")

# #704 gold stars spin beside the wave-clear shout
rep("""            c.drawRect(w / 2 - dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(8f), w / 2 + dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(10.5f), p); p.alpha = 255""",
"""            c.drawRect(w / 2 - dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(8f), w / 2 + dp(120f) * (waveClearT / 1.6f).coerceIn(0f, 1f), h * 0.38f + dp(10.5f), p); p.alpha = 255
            (0 until 2).forEach { k -> val sx2 = w / 2 + (if (k == 0) -1f else 1f) * dp(196f); poly(c, sx2, h * 0.38f - dp(6f), dp(6f), 4, time * 5f + k * 2.1f, Color.rgb(250, 204, 21)) } // #704 gold stars spin beside the CLEAR! shout""",
"#704")

# #705 gold halo breathes behind the onboard icon
rep("""        val (icon, title, body) = tips[onboardPage]
        txt(c, icon, w / 2, h * 0.32f, dp(54f), Color.WHITE)""",
"""        val (icon, title, body) = tips[onboardPage]
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(250, 204, 21); p.alpha = (50 + 30 * sin(time * 2.6f)).toInt().coerceIn(20, 80); c.drawCircle(w / 2, h * 0.32f - dp(19f), dp(44f) * (1f + 0.04f * sin(time * 2.6f)), p); p.style = Paint.Style.FILL; p.alpha = 255 // #705 a gold halo breathes behind the welcome icon
        txt(c, icon, w / 2, h * 0.32f, dp(54f), Color.WHITE)""",
"#705")

# #706 gold bullets beside What's New lines
rep("""        for (t in items) { txtL(c, t, dp(26f), y, dp(13f), Color.rgb(226, 232, 240)); y += dp(30f) }""",
"""        for (t in items) { poly(c, dp(16f), y - dp(4f), dp(3.6f), 4, time * 2.4f, Color.rgb(250, 204, 21)); txtL(c, t, dp(26f), y, dp(13f), Color.rgb(226, 232, 240)); y += dp(30f) } // #706 gold bullets spin beside each What's New line""",
"#706")

# #707 sparkles orbit the About title
rep("""        txt(c, "GEMSTORM 💎", w / 2, y, dp(26f), Color.rgb(165, 243, 252), true); y += dp(36f)""",
"""        txt(c, "GEMSTORM 💎", w / 2, y, dp(26f), Color.rgb(165, 243, 252), true); y += dp(36f)
        (0 until 3).forEach { k -> val oa = time * 1.8f + k * 2.09f; poly(c, w / 2 + cos(oa) * dp(118f), dp(141f) + sin(oa) * dp(10f), dp(3.4f), 4, oa * 2f, Color.rgb(250, 204, 21)) } // #707 sparkles orbit the About title""",
"#707")

# #708 dash-demo hazard warning ring
rep("""        poly(c, w * 0.55f, trackY, dp(15f), 3, time * 2f, Color.rgb(239, 68, 68))""",
"""        poly(c, w * 0.55f, trackY, dp(15f), 3, time * 2f, Color.rgb(239, 68, 68))
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(239, 68, 68); p.alpha = (60 + 45 * sin(time * 5f)).toInt().coerceIn(15, 105); c.drawCircle(w * 0.55f, trackY, dp(21f) * (1f + 0.08f * sin(time * 5f)), p); p.style = Paint.Style.FILL; p.alpha = 255 // #708 the demo hazard wears a warning ring""",
"#708")

# #709 jet flame behind the dashing demo gem
rep("""        poly(c, hx, trackY, dp(19f), 4, 0.78f, CHARS[save.selectedChar].color)
        poly(c, hx, trackY, dp(10f), 4, 0.78f, Color.rgb(168, 85, 247))""",
"""        if (cyc in 0.35f..0.5f) poly(c, hx - dp(24f), trackY, dp(9f) * (1f + 0.3f * sin(time * 31f)), 3, 3.14159f, Color.rgb(249, 115, 22)) // #709 a jet flame flickers behind the dashing gem
        poly(c, hx, trackY, dp(19f), 4, 0.78f, CHARS[save.selectedChar].color)
        poly(c, hx, trackY, dp(10f), 4, 0.78f, Color.rgb(168, 85, 247))""",
"#709")

# #710 zebra tint on upgrade gallery rows
rep("""        for (i in 0..23) {
            val nm = UPGRADE_NAMES[i]""",
"""        for (i in 0..23) {
            val nm = UPGRADE_NAMES[i]
            if (i % 2 == 0) { p.color = Color.rgb(88, 28, 135); p.alpha = 22; c.drawRect(dp(16f), y - dp(15f), w - dp(16f), y + dp(8f), p); p.alpha = 255 } // #710 zebra tint steadies the gallery rows""",
"#710")

# #711 pink sparks flank the Warden gallery line
rep("""        txt(c, "👑 Abyss Warden — boss every 5 waves", w / 2, y + dp(16f), dp(13.5f), Color.rgb(244, 114, 182), true)""",
"""        txt(c, "👑 Abyss Warden — boss every 5 waves", w / 2, y + dp(16f), dp(13.5f), Color.rgb(244, 114, 182), true)
        (0 until 2).forEach { k -> val sx3 = w / 2 + (if (k == 0) -1f else 1f) * dp(172f); poly(c, sx3, y + dp(7f), dp(4.2f), 4, time * 3.4f + k * 1.9f, Color.rgb(244, 114, 182)) } // #711 pink sparks flank the Warden's gallery line""",
"#711")

# #712 heartbeat diamonds stud the low-HP frame corners
rep("""            p.alpha = (pulseA * 0.65f).toInt(); p.strokeWidth = dp(2.5f)
            c.drawRect(dp(13f), dp(13f), w - dp(13f), h - dp(13f), p)
            p.style = Paint.Style.FILL; p.alpha = 255""",
"""            p.alpha = (pulseA * 0.65f).toInt(); p.strokeWidth = dp(2.5f)
            c.drawRect(dp(13f), dp(13f), w - dp(13f), h - dp(13f), p)
            p.style = Paint.Style.FILL; (0 until 4).forEach { k -> val cxk = if (k % 2 == 0) dp(13f) else w - dp(13f); val cyk = if (k < 2) dp(13f) else h - dp(13f); poly(c, cxk, cyk, dp(5f) * (1f + 0.3f * min(1f, beat)), 4, time * 3f + k * 1.57f, Color.rgb(239, 68, 68)) } // #712 heartbeat diamonds stud the danger frame
            p.style = Paint.Style.FILL; p.alpha = 255""",
"#712")

assert s != orig
open(P, "w").write(s)
print("patch_loop18 applied: 12 edits")
