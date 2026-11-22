#!/usr/bin/env python3
"""GemStorm v2.6.0 part C2: #793 animated opening sequence + #794-800 menu upgrades."""
P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# ---- #793 opening sequence ----
rep("""    enum class State { MENU, HOWTO, SHOP, SETTINGS, STATS, PLAYING, LEVELUP, PAUSED, GAMEOVER, VICTORY, HEROES, WEAPONS, ONBOARD, WHATSNEW, ABOUT }""",
"""    enum class State { INTRO, MENU, HOWTO, SHOP, SETTINGS, STATS, PLAYING, LEVELUP, PAUSED, GAMEOVER, VICTORY, HEROES, WEAPONS, ONBOARD, WHATSNEW, ABOUT }""",
"enum")

rep("""    private var state = State.MENU""",
"""    private var state = State.INTRO // #793 the opening sequence plays first""",
"initial state")

rep("""        if (state != lastState) { lastState = state; screenT = 0f } // #154 transition clock""",
"""        if (state != lastState) { lastState = state; screenT = 0f } // #154 transition clock
        if (state == State.INTRO) { introT += dt; if (introT > 3.2f) state = State.MENU } // #793""",
"intro tick")

rep("""            State.MENU -> drawMenu(c)""",
"""            State.INTRO -> drawIntro(c)
            State.MENU -> drawMenu(c)""",
"intro draw dispatch")

rep("""            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (state == State.PLAYING) {""",
"""            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (state == State.INTRO) { state = State.MENU; return true } // #793 tap skips the opening
                if (state == State.PLAYING) {""",
"intro skip")

rep("""    // ---------- menu screens ----------
    private fun drawMenu(c: Canvas) {""",
"""    // ---------- menu screens ----------
    private fun drawIntro(c: Canvas) { // #793 animated opening sequence
        val t = introT
        p.color = Color.rgb(4, 2, 12); p.alpha = 235; c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        val cx = w / 2; val cy = h * 0.42f
        // act 1: a gem falls out of the dark, trailing sparks
        val fall = min(1f, t / 0.9f); val fe = 1f - (1f - fall) * (1f - fall)
        val gy = -dp(30f) + (cy + dp(30f)) * fe
        if (t < 1.1f) for (k in 1..4) { p.color = Color.rgb(52, 211, 153); p.alpha = (130 - k * 26).coerceIn(0, 130); c.drawCircle(cx + sin(t * 9f + k) * dp(3f), gy - k * dp(16f), dp(3.4f) - k * dp(0.5f), p) }; p.alpha = 255
        poly(c, cx, gy, dp(17f) * (0.7f + 0.3f * fe), 4, 0.78f + t * 2.2f, Color.rgb(52, 211, 153))
        poly(c, cx, gy, dp(8.5f), 4, 0.78f + t * 2.2f, Color.rgb(167, 243, 208))
        // act 2: it lands — shockwaves roll out and the title slams in
        if (t > 0.9f) {
            val lt = min(1f, (t - 0.9f) / 0.5f)
            for (k in 0 until 2) { val ph = (t - 0.9f + k * 0.22f) % 0.9f; p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = if (k == 0) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21); p.alpha = (170 * (1f - ph / 0.9f)).toInt().coerceIn(0, 170); c.drawCircle(cx, cy, dp(20f) + dp(120f) * ph, p) }; p.style = Paint.Style.FILL; p.alpha = 255
            val ts = 0.7f + 0.3f * (1f - (1f - lt) * (1f - lt))
            c.save(); c.scale(ts, ts, cx, cy + dp(52f))
            txt(c, "GEMSTORM", cx, cy + dp(52f), dp(40f), Color.rgb(165, 243, 252), true, (lt * 255).toInt().coerceIn(0, 255))
            c.restore()
            p.color = Color.rgb(250, 204, 21); p.alpha = (lt * 180).toInt().coerceIn(0, 180); c.drawRect(cx - dp(120f) * lt, cy + dp(66f), cx + dp(120f) * lt, cy + dp(68f), p); p.alpha = 255
        }
        // act 3: your turtle jets across the bottom and the subtitle fades up
        if (t > 1.7f) {
            val fx = -dp(50f) + (w + dp(100f)) * min(1f, (t - 1.7f) / 1.3f)
            val fy = h * 0.78f + sin(t * 6f) * dp(5f)
            for (k in 1..3) poly(c, fx - dp(16f) - k * dp(11f), fy, dp(6f) - k * dp(1.2f), 3, -1.57f, Color.rgb(249, 115, 22))
            p.color = Color.rgb(34, 197, 94); c.drawCircle(fx, fy, dp(15f), p)
            p.color = Color.rgb(74, 222, 128); c.drawCircle(fx, fy, dp(11.5f), p)
            p.color = Color.rgb(187, 247, 208); c.drawCircle(fx + dp(13f), fy - dp(6f), dp(5.5f), p)
            txt(c, "💎  N E O N   A B Y S S  💎", cx, cy + dp(104f), dp(15f), Color.rgb(232, 121, 249), true, (min(1f, (t - 1.9f) / 0.6f) * 255).toInt().coerceIn(0, 255))
        }
        txt(c, "TAP TO SKIP", cx, h - dp(30f), dp(11f), Color.rgb(100, 116, 139), false, (120 + 80 * sin(time * 4f)).toInt().coerceIn(40, 200))
    }

    private fun drawMenu(c: Canvas) {""",
"drawIntro")

# ---- #794/#795 screenTitle upgrades ----
rep("""        poly(c, w / 2 + dp(118f), dp(56f), dp(4.4f), 4, -time * 2.4f, Color.rgb(250, 204, 21))""",
"""        poly(c, w / 2 + dp(118f), dp(56f), dp(4.4f), 4, -time * 2.4f, Color.rgb(250, 204, 21))
        p.color = Color.rgb(250, 204, 21); p.alpha = 46; c.drawRect(w / 2 - dp(104f), dp(76f), w / 2 + dp(104f), dp(77.5f), p); p.alpha = 255 // #794 an underline sweeps under every screen title
        val swu = ((time * 0.3f) % 1.4f - 0.2f); if (swu in 0f..1f) { p.color = Color.WHITE; p.alpha = 120; c.drawRect(w / 2 - dp(104f) + dp(208f) * swu - dp(7f), dp(75.5f), w / 2 - dp(104f) + dp(208f) * swu + dp(7f), dp(78f), p); p.alpha = 255 }
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(165, 243, 252); p.alpha = 110 // #795 corner brackets frame every screen title
        for (sx in -1..1 step 2) for (sy in -1..1 step 2) { val bx2 = w / 2 + sx * dp(158f); val by2 = dp(56f) + sy * dp(26f); c.drawLine(bx2, by2, bx2 - sx * dp(12f), by2, p); c.drawLine(bx2, by2, bx2, by2 - sy * dp(12f), p) }
        p.style = Paint.Style.FILL; p.alpha = 255""",
"#794+#795")

# ---- #796 button shadow lip ----
rep("""        roundRect(c, r2, dp(14f), fill)
        val shx2 = ((time * 0.24f + r2.top * 0.0011f) % 1.6f - 0.3f) // #653 a sheen sweeps every button""",
"""        roundRect(c, RectF(r2.left, r2.top + dp(3.5f), r2.right, r2.bottom + dp(3.5f)), dp(14f), Color.rgb(2, 0, 10), 90) // #796 a shadow lip grounds every button
        roundRect(c, r2, dp(14f), fill)
        val shx2 = ((time * 0.24f + r2.top * 0.0011f) % 1.6f - 0.3f) // #653 a sheen sweeps every button""",
"#796")

# ---- #800 medallion ring ----
rep("""        poly(c, rect.left + dp(chipDp) + dp(12f), rect.centerY() - dp(12f), dp(3f), 4, time * 2.6f + rect.top * 0.01f, Color.rgb(250, 204, 21)) // #717 menu medallions wear a spinning spark""",
"""        poly(c, rect.left + dp(chipDp) + dp(12f), rect.centerY() - dp(12f), dp(3f), 4, time * 2.6f + rect.top * 0.01f, Color.rgb(250, 204, 21)) // #717 menu medallions wear a spinning spark
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f); p.color = Color.WHITE; p.alpha = (40 + 25 * sin(time * 2.8f + rect.top * 0.01f)).toInt().coerceIn(15, 65); c.drawCircle(rect.left + dp(chipDp), rect.centerY(), dp(19f), p); p.style = Paint.Style.FILL; p.alpha = 255 // #800 a breathing ring rims every menu medallion""",
"#800")

# ---- #797 menu title emblem rings ----
rep("""        // #155 title letters stagger drop-in""",
"""        (0 until 2).forEach { k -> p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = if (k == 0) Color.rgb(52, 211, 153) else Color.rgb(250, 204, 21); p.alpha = if (k == 0) 44 else 30; c.drawArc(RectF(cx - dp(128f) - k * dp(13f), cy + dp(96f) - dp(52f) - k * dp(9f), cx + dp(128f) + k * dp(13f), cy + dp(96f) + dp(52f) + k * dp(9f)), time * (26f - k * 17f) + k * 130f, 150f - k * 50f, false, p) }; p.style = Paint.Style.FILL; p.alpha = 255 // #797 twin rings orbit the menu title
        // #155 title letters stagger drop-in""",
"#797")

# ---- #798 ticker backing pill ----
rep("""        while (tickX < w) { c.drawText(tickStr, tickX, cy + dp(192f), p); tickX += tickW }""",
"""        roundRect(c, RectF(dp(10f), cy + dp(175f), w - dp(10f), cy + dp(199f)), dp(12f), Color.rgb(15, 8, 30), 120) // #798 the ticker scrolls on a backing pill
        while (tickX < w) { c.drawText(tickStr, tickX, cy + dp(192f), p); tickX += tickW }""",
"#798")

# ---- #799 footer divider ----
rep("""        txtFitC(c, "💎 ${save.gemBank}  •  🌊 Best wave ${save.bestWave}  •  by QamoTech 💎N🎱  •  v2.5 — 736-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), w - dp(16f), false) // #337 footer chips""",
"""        p.color = Color.rgb(100, 116, 139); p.alpha = 60; c.drawRect(dp(24f), h - dp(33f), w - dp(24f), h - dp(32f), p); p.alpha = 255 // #799 a divider hairline rules the footer
        poly(c, cx, h - dp(32.5f), dp(3.4f), 4, time * 3f, Color.rgb(52, 211, 153))
        txtFitC(c, "💎 ${save.gemBank}  •  🌊 Best wave ${save.bestWave}  •  by QamoTech 💎N🎱  •  v2.5 — 736-upgrade edition", cx, h - dp(18f), dp(11.5f), Color.rgb(100, 116, 139), w - dp(16f), false) // #337 footer chips""",
"#799")

assert s != orig
open(P, "w").write(s)
print("patch_v260_c2 applied")
