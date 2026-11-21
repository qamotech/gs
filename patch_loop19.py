#!/usr/bin/env python3
"""GemStorm graphics loop cycle 19 — #713-724: records/settings/menu polish,
slider glows + glints, HUD gem spark, boss-bar danger tip. Draw-only."""
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

# #713 stat cards wear a hero-color spine
rep("""            roundRect(c, RectF(dp(16f), y - dp(24f), w - dp(16f), y + dp(12f)), dp(10f), Color.rgb(17, 12, 34), 220)
            txtL(c, k, dp(28f), y, dp(15.5f), Color.rgb(226, 232, 240), true)""",
"""            roundRect(c, RectF(dp(16f), y - dp(24f), w - dp(16f), y + dp(12f)), dp(10f), Color.rgb(17, 12, 34), 220)
            p.color = CHARS[save.selectedChar].color; p.alpha = 90; c.drawRect(dp(16f), y - dp(24f), dp(19f), y + dp(12f), p); p.alpha = 255 // #713 stat cards wear a hero-color spine
            txtL(c, k, dp(28f), y, dp(15.5f), Color.rgb(226, 232, 240), true)""",
"#713")

# #714 stat bars cap with a bright tip
rep("""                if (barFrac > 0) roundRect(c, RectF(dp(28f), y + dp(5f), dp(28f) + (w - dp(56f)) * barFrac * bEase, y + dp(9f)), dp(2f), Color.rgb(52, 211, 153))""",
"""                if (barFrac > 0) roundRect(c, RectF(dp(28f), y + dp(5f), dp(28f) + (w - dp(56f)) * barFrac * bEase, y + dp(9f)), dp(2f), Color.rgb(52, 211, 153))
                if (barFrac > 0) { p.color = Color.WHITE; p.alpha = 200; c.drawCircle(dp(28f) + (w - dp(56f)) * barFrac * bEase, y + dp(7f), dp(2.2f), p); p.alpha = 255 } // #714 stat bars cap with a bright tip""",
"#714")

# #715 most-played hero's runs bar wears a gold star
rep("""            if (rc > 0) roundRect(c, RectF(dp(150f), y - dp(10f), dp(150f) + (w - dp(240f)) * (rc.toFloat() / maxRuns), y - dp(3f)), dp(3f), CHARS[i].color)""",
"""            if (rc > 0) roundRect(c, RectF(dp(150f), y - dp(10f), dp(150f) + (w - dp(240f)) * (rc.toFloat() / maxRuns), y - dp(3f)), dp(3f), CHARS[i].color)
            if (rc == maxRuns && rc > 0) poly(c, dp(150f) + (w - dp(240f)) + dp(8f), y - dp(6.5f), dp(3.2f), 4, time * 3f, Color.rgb(250, 204, 21)) // #715 your most-played hero's bar wears a gold star""",
"#715")

# #716 live toggle knob glows
rep("""        roundRect(c, RectF(tx, ty, tx + tw, ty + th), th / 2, if (on) Color.rgb(22, 163, 74) else Color.rgb(71, 85, 105))
        p.color = Color.WHITE
        c.drawCircle(tx + th / 2 + (tw - th) * nv, r.centerY(), dp(9f), p)""",
"""        roundRect(c, RectF(tx, ty, tx + tw, ty + th), th / 2, if (on) Color.rgb(22, 163, 74) else Color.rgb(71, 85, 105))
        if (on) { p.color = Color.rgb(74, 222, 128); p.alpha = (40 + 20 * sin(time * 3f)).toInt().coerceIn(20, 60); c.drawCircle(tx + th / 2 + (tw - th) * nv, r.centerY(), dp(13f), p); p.alpha = 255 } // #716 a live toggle's knob glows
        p.color = Color.WHITE
        c.drawCircle(tx + th / 2 + (tw - th) * nv, r.centerY(), dp(9f), p)""",
"#716")

# #717 menu medallions wear a spinning spark
rep("""        p.color = Color.WHITE; p.alpha = 36
        c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(17f), p); p.alpha = 255""",
"""        p.color = Color.WHITE; p.alpha = 36
        c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(17f), p); p.alpha = 255
        poly(c, rect.left + dp(chipDp) + dp(12f), rect.centerY() - dp(12f), dp(3f), 4, time * 2.6f + rect.top * 0.01f, Color.rgb(250, 204, 21)) // #717 menu medallions wear a spinning spark""",
"#717")

# #718 music slider thumb glows purple
rep("""        p.color = Color.WHITE; c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR, p)""",
"""        p.color = Color.rgb(168, 85, 247); p.alpha = 36; c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR + dp(5f), p); p.alpha = 255 // #718 the music thumb glows purple
        p.color = Color.WHITE; c.drawCircle(musicSlider.left + musicSlider.width() * save.musicVol, musicSlider.centerY(), mThumbR, p)""",
"#718")

# #719 sfx slider thumb glows cyan
rep("""        p.color = Color.WHITE; c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR, p)""",
"""        p.color = Color.rgb(34, 211, 238); p.alpha = 36; c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR + dp(5f), p); p.alpha = 255 // #719 the sfx thumb glows cyan
        p.color = Color.WHITE; c.drawCircle(sfxSlider.left + sfxSlider.width() * save.sfxVol, sfxSlider.centerY(), sThumbR, p)""",
"#719")

# #720 mini slider thumbs catch a glint
rep("""        p.color = Color.WHITE; c.drawCircle(r.left + r.width() * f, r.centerY(), tr, p)
        p.color = col; c.drawCircle(r.left + r.width() * f, r.centerY(), tr * 0.55f, p)""",
"""        p.color = Color.WHITE; c.drawCircle(r.left + r.width() * f, r.centerY(), tr, p)
        p.color = col; c.drawCircle(r.left + r.width() * f, r.centerY(), tr * 0.55f, p)
        p.color = Color.WHITE; p.alpha = 220; c.drawCircle(r.left + r.width() * f - tr * 0.2f, r.centerY() - tr * 0.2f, tr * 0.16f, p); p.alpha = 255 // #720 slider thumbs catch a glint""",
"#720")

# #721 gems flank the shop footer
rep("""        txt(c, "Unlocks are permanent. Earn gems in every run — win or lose.", w / 2, y + dp(14f), dp(12f), Color.rgb(100, 116, 139))""",
"""        txt(c, "Unlocks are permanent. Earn gems in every run — win or lose.", w / 2, y + dp(14f), dp(12f), Color.rgb(100, 116, 139))
        (0 until 2).forEach { k -> poly(c, w / 2 + (if (k == 0) -1f else 1f) * dp(182f), y + dp(10f), dp(3.2f), 4, time * 3f + k * 1.9f, Color.rgb(52, 211, 153)) } // #721 gems flank the shop footer""",
"#721")

# #722 sparks flank the live preview label
rep("""            txt(c, "🎛 LIVE PREVIEW", pvx, pvy + dp(84f), dp(12f), Color.rgb(250, 204, 21), true)""",
"""            txt(c, "🎛 LIVE PREVIEW", pvx, pvy + dp(84f), dp(12f), Color.rgb(250, 204, 21), true)
            (0 until 2).forEach { k -> poly(c, pvx + (if (k == 0) -1f else 1f) * dp(74f), pvy + dp(80f), dp(3f), 4, time * 3.2f + k * 1.9f, Color.rgb(250, 204, 21)) } // #722 sparks flank the live preview label""",
"#722")

# #723 gem spark spins while the HUD gem count pops
rep("""        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))""",
"""        txtR(c, "💎 $gemsRun   ☠ $kills", w - m - dp(52f), m + dp(36f), dp(13f) * (1f + 0.3f * max(0f, max(gemsPop, killsPop))), Color.rgb(165, 243, 252))
        if (gemsPop > 0.05f) poly(c, w - m - dp(170f), m + dp(32f), dp(4f) * (0.7f + 0.3f * gemsPop), 4, time * 7f, Color.rgb(52, 211, 153)) // #723 a gem spark spins while the gem count pops""",
"#723")

# #724 boss bar tip burns red under 25%
rep("""            if (shb in 0f..1f && hpF2 > 0.02f) { val sxb = bx + bw * hpF2 * shb; p.color = Color.WHITE; p.alpha = 34; c.drawRect(sxb, by + dp(1.5f), min(sxb + dp(10f), bx + bw * hpF2), by + dp(11.5f), p); p.alpha = 255 }""",
"""            if (shb in 0f..1f && hpF2 > 0.02f) { val sxb = bx + bw * hpF2 * shb; p.color = Color.WHITE; p.alpha = 34; c.drawRect(sxb, by + dp(1.5f), min(sxb + dp(10f), bx + bw * hpF2), by + dp(11.5f), p); p.alpha = 255 }
            if (hpF2 < 0.25f && hpF2 > 0f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 100 * sin(time * 8f)).toInt().coerceIn(20, 220); c.drawCircle(bx + bw * hpF2, by + dp(6.5f), dp(3.4f), p); p.alpha = 255 } // #724 the boss bar's tip burns red under 25%""",
"#724")

assert s != orig
open(P, "w").write(s)
print("patch_loop19 applied: 12 edits")
