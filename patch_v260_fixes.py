#!/usr/bin/env python3
"""GemStorm v2.6.0 fixes (Nathan 2026-11-21 19:20):
F1 remove #363 two-finger-tap pause (his 'double tap pauses' bug)
F2 remove #247 elite beacon column + #626 sparks (opaque yellow trail)
F3 remove #187/#213 light-pillar rects (landing ring stays)
F4 buttons more opaque (dash/rocket/joystick/aim/pause/preview base alphas up)
"""
P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read()
orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# F1 — two-finger tap pause GONE (pause button + hold-guard stay)
rep("""                    if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN && time - playDownT < 0.09f) { // #363 two-finger tap pauses
                        joyPointer = -1; joyDx = 0f; joyDy = 0f; pauseGame()
                    } else when {""",
"""                    when { // v2.6: #363 two-finger-tap pause REMOVED (Nathan: double-tap was pausing mid-fight)""",
"F1")

# F2 — elite beacon column + its sparks GONE
rep("""            if (e.elite) { // #247 elite beacon light column
                p.color = Color.rgb(250, 204, 21); p.alpha = (30 + 14 * sin(time * 5f)).toInt().coerceIn(16, 44)
                c.drawRect(e.x - dp(8f), e.y - dp(320f), e.x + dp(8f), e.y, p)
                for (k in 0 until 2) { val byy = e.y - ((time * 46f + k * 131f + e.x) % dp(300f)); p.color = Color.rgb(254, 240, 138); p.alpha = (60 + 40 * sin(time * 6f + k * 2.6f)).toInt().coerceIn(20, 100); c.drawCircle(e.x + sin(time * 3f + k * 4.1f) * dp(4f), byy, dp(1.7f), p) } // #626 sparks rise in the elite beacon
                p.alpha = 255
            }""",
"""            // v2.6: the #247 elite beacon column + #626 sparks are GONE — opaque trails removed globally (elites keep their #501 rings)""",
"F2")

# F3 — spawn/level-up light pillars GONE (landing ring stays)
rep("""            p.color = bm.color; p.alpha = (150 * bf).toInt()
            c.drawRect(bm.x - dp(5f), bm.y - dp(420f), bm.x + dp(5f), bm.y, p)
            p.alpha = (70 * bf).toInt()
            c.drawRect(bm.x - dp(13f), bm.y - dp(420f), bm.x + dp(13f), bm.y, p)
            p.alpha = 255""",
"""            // v2.6: the #187/#213 light-pillar columns are GONE — opaque trails removed globally; the landing ring below stays
            p.alpha = 255""",
"F3")

# F4 — buttons more opaque
rep("""p.color = if (ready) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105); p.alpha = (190 * save.btnOpacity).toInt() // #395""",
"""p.color = if (ready) Color.rgb(34, 211, 238) else Color.rgb(71, 85, 105); p.alpha = (238 * save.btnOpacity).toInt() // #395 v2.6 buttons more opaque (Nathan)""",
"F4-dash")
rep("""p.color = if (rdy) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105); p.alpha = (190 * save.btnOpacity).toInt()""",
"""p.color = if (rdy) Color.rgb(249, 115, 22) else Color.rgb(71, 85, 105); p.alpha = (238 * save.btnOpacity).toInt() // v2.6 buttons more opaque""",
"F4-rocket")
rep("""p.color = Color.rgb(34, 211, 238); p.alpha = (60 * save.btnOpacity).toInt() // #395""",
"""p.color = Color.rgb(34, 211, 238); p.alpha = (130 * save.btnOpacity).toInt() // #395 v2.6 buttons more opaque""",
"F4-joybase")
rep("""p.style = Paint.Style.FILL; p.alpha = (200 * save.btnOpacity).toInt(); p.color = Color.rgb(34, 211, 238) // #395""",
"""p.style = Paint.Style.FILL; p.alpha = (232 * save.btnOpacity).toInt(); p.color = Color.rgb(34, 211, 238) // #395 v2.6""",
"F4-joyknob")
rep("""p.color = CHARS[save.selectedChar].color; p.alpha = (150 * save.btnOpacity).toInt(); c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(9f) * save.joySize, p); p.alpha = 255 // #648 the stick knob wears your hero's color at its heart""",
"""p.color = CHARS[save.selectedChar].color; p.alpha = (195 * save.btnOpacity).toInt(); c.drawCircle(joyOx + joyDx * dp(38f), joyOy + joyDy * dp(38f), dp(9f) * save.joySize, p); p.alpha = 255 // #648 the stick knob wears your hero's color at its heart""",
"F4-joyheart")
rep("""roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 200)""",
"""roundRect(c, pvR, dp(10f), Color.rgb(40, 16, 70), 238) // v2.6 pause button more opaque""",
"F4-pause")
rep("""                p.color = Color.rgb(244, 114, 182); p.alpha = 90
                c.drawCircle(aimOx, aimOy, dp(52f) * save.joySize, p)
                p.alpha = 200""",
"""                p.color = Color.rgb(244, 114, 182); p.alpha = 150
                c.drawCircle(aimOx, aimOy, dp(52f) * save.joySize, p)
                p.alpha = 225""",
"F4-aim")
rep("""            p.color = Color.rgb(34, 211, 238); p.alpha = (70 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(52f) * save.joySize, p)
            p.alpha = (210 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(24f) * save.joySize, p)
            p.color = Color.rgb(34, 211, 238); p.alpha = (190 * save.btnOpacity).toInt()
            c.drawCircle(pvx + dp(70f), pvy, dp(38f) * save.btnScale, p); p.alpha = 255""",
"""            p.color = Color.rgb(34, 211, 238); p.alpha = (130 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(52f) * save.joySize, p)
            p.alpha = (238 * save.btnOpacity).toInt()
            c.drawCircle(pvx - dp(70f), pvy, dp(24f) * save.joySize, p)
            p.color = Color.rgb(34, 211, 238); p.alpha = (238 * save.btnOpacity).toInt()
            c.drawCircle(pvx + dp(70f), pvy, dp(38f) * save.btnScale, p); p.alpha = 255""",
"F4-preview")

assert s != orig
open(P, "w").write(s)
print("patch_v260_fixes applied")
