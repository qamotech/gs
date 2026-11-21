#!/usr/bin/env python3
"""GemStorm v2.3.0 part 2: turtle hero + enhancements #449-472 + version strings."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

# ============ E. TURTLE HERO (💎N🎱) ============
E("""            CharDef("Cryo", "❄️", 110f, 0.95f, 1.00f, 1200, Color.rgb(147, 197, 253), "A freezing aura slows nearby foes.")
        )""",
"""            CharDef("Cryo", "❄️", 110f, 0.95f, 1.00f, 1200, Color.rgb(147, 197, 253), "A freezing aura slows nearby foes."),
            CharDef("💎N🎱 Turtle", "🐢", 130f, 1.02f, 1.02f, 0, Color.rgb(34, 197, 94), "A turtle with jets. Slow is smooth, smooth is fast. Free — a gift from 💎N🎱.")
        )""")
E("""        audio.playMusic(R.raw.music_menu)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); requestFocus() }""",
"""        audio.playMusic(R.raw.music_menu)
        if (!save.ownsChar(5)) save.unlockChar(5) // 💎N🎱 turtle: free for everyone — granted on first v2.3 launch
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); requestFocus() }""")
# turtle body, inserted right after the ground shadow
E("""            // #407 soft ground shadow under the hero
            p.color = Color.BLACK; p.alpha = 95
            c.drawOval(RectF(px - dp(17f), py + dp(19f), px + dp(17f), py + dp(28f)), p); p.alpha = 255""",
"""            // #407 soft ground shadow under the hero
            p.color = Color.BLACK; p.alpha = 95
            c.drawOval(RectF(px - dp(17f), py + dp(19f), px + dp(17f), py + dp(28f)), p); p.alpha = 255
            if (save.selectedChar == 5 && !blink) { // 🐢💎N🎱 TURTLE WITH JETS
                val ta = lastAim
                val spdF = 0.75f + 0.5f * min(1f, hyp(pvx, pvy) / 300f)
                for (s in -1..1 step 2) { // rear jet pods + flames (flames swell with speed)
                    val jx = bpx - cos(ta) * dp(17f) + cos(ta + 1.57f) * dp(10f) * s
                    val jy = bpy - sin(ta) * dp(17f) + sin(ta + 1.57f) * dp(10f) * s
                    val fl = (dp(8f) + dp(3f) * sin(time * 29f + s * 2f)) * spdF
                    poly(c, jx - cos(ta) * fl * 0.9f, jy - sin(ta) * fl * 0.9f, fl * 0.62f, 4, ta + 0.78f + 3.14159f, Color.rgb(249, 115, 22))
                    poly(c, jx - cos(ta) * fl * 0.5f, jy - sin(ta) * fl * 0.5f, fl * 0.34f, 4, ta + 0.78f + 3.14159f, Color.rgb(254, 240, 138))
                    p.color = Color.rgb(71, 85, 105); c.drawCircle(jx, jy, dp(5.5f), p)
                    p.color = Color.rgb(148, 163, 184); c.drawCircle(jx, jy, dp(3f), p)
                }
                for (fx2 in -1..1 step 2) for (fy2 in -1..1 step 2) { // flippers, paddle wiggle
                    val wig = sin(time * 6f + (fx2 + fy2) * 1.7f) * dp(1.6f)
                    p.color = Color.rgb(22, 163, 74)
                    c.drawOval(RectF(bpx + fx2 * dp(15f) - dp(6f) + wig, bpy + fy2 * dp(13f) - dp(3.5f), bpx + fx2 * dp(15f) + dp(6f) + wig, bpy + fy2 * dp(13f) + dp(3.5f)), p)
                }
                // shell: dark rim, green dome, drifting plate arcs
                p.color = Color.rgb(20, 83, 45); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                p.color = Color.rgb(34, 197, 94); c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(134, 239, 172); p.alpha = 170
                c.drawArc(RectF(bpx - dp(12f), bpy - dp(12f), bpx + dp(12f), bpy + dp(12f)), time * 40f, 110f, false, p)
                c.drawArc(RectF(bpx - dp(12f), bpy - dp(12f), bpx + dp(12f), bpy + dp(12f)), time * 40f + 180f, 110f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                // 💎 the gem on the shell
                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, Color.rgb(34, 211, 238))
                poly(c, bpx, bpy, dp(3.4f) * spawnSc, 4, 0.78f, Color.rgb(236, 254, 255))
                // head + eyes, leading the aim
                val hx2 = bpx + cos(ta) * dp(20f); val hy2 = bpy + sin(ta) * dp(20f)
                p.color = Color.rgb(22, 163, 74); c.drawCircle(hx2, hy2, dp(8.6f) * spawnSc, p)
                p.color = Color.rgb(74, 222, 128); c.drawCircle(hx2, hy2, dp(7.4f) * spawnSc, p)
                for (s in -1..1 step 2) {
                    val ex = hx2 + cos(ta) * dp(3.4f) + cos(ta + 1.57f) * dp(3.6f) * s
                    val ey = hy2 + sin(ta) * dp(3.4f) + sin(ta + 1.57f) * dp(3.6f) * s
                    p.color = Color.rgb(8, 2, 18); c.drawCircle(ex, ey, dp(1.9f), p)
                    p.color = Color.WHITE; c.drawCircle(ex + dp(0.6f), ey - dp(0.6f), dp(0.8f), p)
                }
            }""")
# gate the generic body pieces for the turtle
E("""            if (!blink) { // #401 layered armor plate + bright rim under the gem body""",
"""            if (!blink && save.selectedChar != 5) { // #401 layered armor plate + bright rim under the gem body""")
E("""            if (!blink) {
                val rot = sin(time * 2.2f) * 0.14f""",
"""            if (!blink && save.selectedChar != 5) {
                val rot = sin(time * 2.2f) * 0.14f""")
E("""            // #403 visor slit + glint ride the aim direction
            val visX = bpx + cos(lastAim) * dp(12.5f); val visY = bpy + sin(lastAim) * dp(12.5f)
            p.color = Color.rgb(8, 2, 18); c.drawCircle(visX, visY, dp(4.6f), p)
            p.color = Color.WHITE; c.drawCircle(visX + cos(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), visY + sin(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), dp(1.7f), p) // #431 living glint
            poly(c, bpx + cos(lastAim) * dp(22.5f), bpy + sin(lastAim) * dp(22.5f), dp(4.2f), 4, lastAim + 0.78f, shade(chCol, 0.75f)) // #408 barrel nub""",
"""            if (save.selectedChar != 5) { // #403 visor slit + glint ride the aim direction (the turtle has real eyes)
                val visX = bpx + cos(lastAim) * dp(12.5f); val visY = bpy + sin(lastAim) * dp(12.5f)
                p.color = Color.rgb(8, 2, 18); c.drawCircle(visX, visY, dp(4.6f), p)
                p.color = Color.WHITE; c.drawCircle(visX + cos(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), visY + sin(lastAim) * (dp(1.6f) + dp(0.5f) * sin(time * 3f)), dp(1.7f), p) // #431 living glint
                poly(c, bpx + cos(lastAim) * dp(22.5f), bpy + sin(lastAim) * dp(22.5f), dp(4.2f), 4, lastAim + 0.78f, shade(chCol, 0.75f)) // #408 barrel nub
            }""")
E("""            if (hyp(mvx2, mvy2) > 0.25f) {
                val ma = atan2(mvy2, mvx2); val flick = (dp(5.5f) + dp(2.5f) * sin(time * 31f)) * (0.8f + 0.45f * min(1f, hyp(pvx, pvy) / 320f)) // #429 flame grows with speed""",
"""            if (hyp(mvx2, mvy2) > 0.25f && save.selectedChar != 5) { // the turtle's flames live in its jet pods
                val ma = atan2(mvy2, mvx2); val flick = (dp(5.5f) + dp(2.5f) * sin(time * 31f)) * (0.8f + 0.45f * min(1f, hyp(pvx, pvy) / 320f)) // #429 flame grows with speed""")
E("""                poly(c, bpx, bpy, dp(21f) * spawnSc, 4, 0.78f, Color.WHITE)
                p.alpha = 255""",
"""                if (save.selectedChar == 5) c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p) else poly(c, bpx, bpy, dp(21f) * spawnSc, 4, 0.78f, Color.WHITE)
                p.alpha = 255""")
E("""            val shineA = time * 2.6f
            poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE)""",
"""            val shineA = time * 2.6f
            if (save.selectedChar != 5) poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE) // the turtle's gem has its own glint""")
E("""                    for (k in 0 until 3) { val sa = time * 1.5f + k * 1.047f; c.drawLine(bpx - cos(sa) * dp(8f), bpy - sin(sa) * dp(8f), bpx + cos(sa) * dp(8f), bpy + sin(sa) * dp(8f), p) }
                    p.style = Paint.Style.FILL
                }
            }""",
"""                    for (k in 0 until 3) { val sa = time * 1.5f + k * 1.047f; c.drawLine(bpx - cos(sa) * dp(8f), bpy - sin(sa) * dp(8f), bpx + cos(sa) * dp(8f), bpy + sin(sa) * dp(8f), p) }
                    p.style = Paint.Style.FILL
                }
                5 -> { } // 💎N🎱 turtle: the whole body is the signature
            }""")

# ============ F. ENHANCEMENTS #449-472 ============
E("""            txt(c, "$countStage", w / 2, h * 0.40f, dp(64f), Color.rgb(250, 204, 21), true)""",
"""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(250, 204, 21) // #449 countdown pulse ring
            p.alpha = (160 * (countT % 0.5f) / 0.5f).toInt().coerceIn(0, 160)
            c.drawCircle(w / 2, h * 0.40f - dp(22f), dp(30f) + dp(26f) * (1f - (countT % 0.5f) / 0.5f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            txt(c, "$countStage", w / 2, h * 0.40f, dp(64f), Color.rgb(250, 204, 21), true)""")
E("""                gotGems.add(g)""",
"""                if (sparks.size < 40) sparks.add(Spark(g.x, g.y, 0.28f, Color.rgb(167, 243, 208), dp(9f))) // #450 pickup sparkle
                gotGems.add(g)""")
E("""                decals.add(Decal(m.x, m.y, dp(52f), 6f))""",
"""                decals.add(Decal(m.x, m.y, dp(52f), 6f))
                decals.add(Decal(m.x, m.y, dp(26f), 11f)) // #451 burnt core lingers""")
E("""            val eSc = if (e.spawnT > 0) 1f - 0.55f * min(1f, e.spawnT / 0.32f) else 1f // #160 + #199""",
"""            val eSc = if (e.spawnT > 0) 1f - 0.55f * min(1f, e.spawnT / 0.32f) else 1f // #160 + #199
            if (e.spawnT > 0.24f && (e.type == 2 || e.type == 10 || e.type == 5)) { // #452 heavy units land with a dust ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(168, 162, 158)
                p.alpha = ((e.spawnT - 0.24f) / 0.08f * 120f).toInt().coerceIn(0, 120)
                c.drawCircle(e.x, e.y, e.radius * (2.2f - (e.spawnT - 0.24f) * 6f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")
E("""            if (pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.65f)
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.16f, 0.16f, pr.color, dp(2.6f)))""",
"""            if (pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.65f)
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.16f, 0.16f, pr.color, dp(2.6f)))
            if (!pr.fromPlayer && particles.size < 380 && Random.nextFloat() < 0.30f) // #453 hostile shots shed faint embers
                particles.add(Particle(pr.x, pr.y, 0f, 0f, 0.22f, 0.22f, pr.color, dp(2f)))""")
E("""            if (rings.size < 14) rings.add(Ring(e.x, e.y, e.radius * 0.7f, 0.32f, 0.32f, enemyColor(e.type))) // #444 kill shockwave""",
"""            if (rings.size < 14) rings.add(Ring(e.x, e.y, e.radius * 0.7f, 0.32f, 0.32f, enemyColor(e.type))) // #444 kill shockwave
            if (e.elite) for (i in 0 until 6) { if (particles.size > 360) break // #454 elite gold fountain
                val a = -1.57f + (Random.nextFloat() - 0.5f) * 1.6f; val s = 200f + Random.nextFloat() * 220f
                particles.add(Particle(e.x, e.y, cos(a) * s, sin(a) * s, 0.7f, 0.7f, Color.rgb(250, 204, 21), dp(3.4f), 1, Random.nextFloat() * 6.28f)) }
            if (e.frostAge > 1.2f) burst(e.x, e.y, Color.rgb(165, 243, 252), 10, 300f) // #471 frozen kills shatter icy""")
E("""            val r = dp(10f) * pulse""",
"""            val r = dp(10f) * pulse
            p.color = Color.BLACK; p.alpha = 70 // #455 soft shadow grounds the heart
            c.drawOval(RectF(hd.x - r * 0.8f, hd.y + r * 1.15f, hd.x + r * 0.8f, hd.y + r * 1.55f), p); p.alpha = 255""")
E("""            p.color = chCol; p.alpha = (55 * spawnSc).toInt()
            c.drawCircle(bpx, bpy, dp(34f) * spawnSc, p); p.alpha = 255""",
"""            p.color = chCol; p.alpha = (55 * spawnSc).toInt()
            c.drawCircle(bpx, bpy, dp(34f) * spawnSc, p); p.alpha = 255
            if (spawnAnimT > 0) { // #456 materialize shockring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = chCol
                p.alpha = (200 * (spawnAnimT / 0.55f)).toInt().coerceIn(0, 200)
                c.drawCircle(px, py, dp(52f) * (1f - spawnAnimT / 0.55f) + dp(10f), p)
                p.style = Paint.Style.FILL; p.alpha = 255
            }""")
E("""                texts.add(FloatText(px, py - dp(64f), "🔥 $kills KILL STREAK! +25💎", 1.1f, Color.rgb(251, 146, 60), dp(16f)))""",
"""                texts.add(FloatText(px, py - dp(64f), "🔥 $kills KILL STREAK! +25💎", 1.1f, Color.rgb(251, 146, 60), dp(16f)))
                rings.add(Ring(px, py, dp(14f), 0.5f, 0.5f, Color.rgb(251, 146, 60))) // #457 streak ring burst""")
E("""            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)""",
"""            p.style = Paint.Style.STROKE; p.strokeWidth = dp(5f); p.color = Color.rgb(248, 113, 113) // #459 heartbeat edge pulse
            p.alpha = (60 + 55 * sin(time * 7.4f)).toInt().coerceIn(5, 115)
            c.drawRect(dp(4f), dp(4f), w - dp(4f), h - dp(4f), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            txt(c, "⚠ LAST STAND ⚠", w / 2, h * 0.16f, dp(15f), Color.rgb(248, 113, 113), true)""")
E("""            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }""",
"""            if (g.mag) { p.color = Color.WHITE; p.alpha = 170; c.drawCircle(g.x, g.y + bob, r * 0.3f, p); p.alpha = 255 }
            if (big) { // #460 rich gems wear a rotating sparkle
                val sa2 = time * 3f + g.x * 0.05f
                poly(c, g.x + cos(sa2) * r * 1.5f, g.y + bob + sin(sa2) * r * 1.5f, r * 0.4f, 4, sa2, Color.rgb(254, 240, 138))
            }""")
E("""            if (e.type == 7 && e.state == 2 && ghosts.size < 24 && Random.nextFloat() < 0.7f) ghosts.add(Ghost(e.x, e.y, 0.25f, enemyColor(7))) // #235 lancer charge afterimage""",
"""            if (e.type == 7 && e.state == 2 && ghosts.size < 24 && Random.nextFloat() < 0.7f) ghosts.add(Ghost(e.x, e.y, 0.25f, enemyColor(7))) // #235 lancer charge afterimage
            if (e.type == 7 && e.state == 2 && particles.size < 360 && Random.nextFloat() < 0.5f) particles.add(Particle(e.x, e.y + e.radius * 0.6f, Random.nextFloat() * 30 - 15, 26f, 0.4f, 0.4f, Color.rgb(168, 162, 158), dp(3.6f))) // #461 charge dust""")
E("""                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); p.color = col; p.alpha = 60
                    c.drawCircle(e.x, e.y, e.radius * 1.5f, p); p.style = Paint.Style.FILL; p.alpha = 255""",
"""                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.5f); p.color = col; p.alpha = (60 + 25 * sin(time * 3f + e.wob)).toInt().coerceIn(35, 85) // #462 orbit ring breathes
                    c.drawCircle(e.x, e.y, e.radius * 1.5f, p); p.style = Paint.Style.FILL; p.alpha = 255""")
E("""            c.drawCircle(hx, hy, dp(2.4f), p)
        }
        p.alpha = 255""",
"""            c.drawCircle(hx, hy, dp(2.4f), p)
        }
        // #463 a ringed giant drifts across the deep background
        val pph = (time % 75f) / 75f
        if (pph < 0.6f) {
            val plx = -dp(90f) + (w + dp(180f)) * (pph / 0.6f); val ply = h * 0.16f
            p.color = Color.rgb(30, 16, 54); p.alpha = 150
            c.drawCircle(plx, ply, dp(64f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(168, 85, 247); p.alpha = 110
            c.drawArc(RectF(plx - dp(64f), ply - dp(64f), plx + dp(64f), ply + dp(64f)), -60f, 90f, false, p)
            p.strokeWidth = dp(5f); p.color = Color.rgb(88, 60, 150); p.alpha = 80
            c.drawOval(RectF(plx - dp(98f), ply - dp(18f), plx + dp(98f), ply + dp(18f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255
        }
        p.alpha = 255""")
E("""            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)""",
"""            val drift = (time * s.size * 5f) % (h + 8f)
            c.drawCircle(s.x, (s.y + drift) % (h + 8f) - 4f, s.size, p)
            if (s.size > 2.6f) { // #464 the brightest stars throw a cross glint
                val gy = (s.y + drift) % (h + 8f) - 4f; val gl = dp(5f) * (0.6f + 0.4f * sin(time * 2f + s.phase))
                p.alpha = (120 * tw).toInt()
                c.drawLine(s.x - gl, gy, s.x + gl, gy, p); c.drawLine(s.x, gy - gl, s.x, gy + gl, p)
            }""")
E("""                        poly(c, bx2, by2, e.radius * (1f - 0.17f * k), 5, time * 2.2f - k * 0.5f, shade(col, 1f - 0.13f * k))
                    }
                    p.alpha = 255""",
"""                        poly(c, bx2, by2, e.radius * (1f - 0.17f * k), 5, time * 2.2f - k * 0.5f, shade(col, 1f - 0.13f * k))
                    }
                    p.alpha = 255
                    p.color = Color.WHITE; p.alpha = (120 + 60 * sin(time * 8f)).toInt().coerceIn(60, 180) // #465 ribbon head sparkles
                    c.drawCircle(e.x - cos(efa) * e.radius * 0.78f, e.y - sin(efa) * e.radius * 0.78f, dp(2f), p); p.alpha = 255""")
E("""                        c.drawCircle(e.x, e.y, e.radius * 1.35f, p)""",
"""                        c.drawCircle(e.x, e.y, e.radius * 1.35f, p)
                        p.strokeWidth = dp(1.2f); c.drawCircle(e.x, e.y, e.radius * (1.35f - 0.18f * (0.5f + 0.5f * sin(time * 2f))), p) // #466 inner ring breathes against the runes""")
E("""                    if (eHpF < 0.33f) {
                        c.drawLine(e.x + e.radius * 0.45f, e.y + e.radius * 0.4f, e.x + e.radius * 0.05f, e.y + e.radius * 0.15f, p)""",
"""                    if (eHpF < 0.33f) {
                        p.color = Color.rgb(249, 115, 22); p.alpha = (150 + 80 * sin(time * 9f)).toInt().coerceIn(70, 230) // #468 deep cracks glow molten
                        c.drawLine(e.x + e.radius * 0.45f, e.y + e.radius * 0.4f, e.x + e.radius * 0.05f, e.y + e.radius * 0.15f, p)""")
E("""                1 -> poly(c, e.x, e.y, e.radius, 4, time * 5f, col)""",
"""                1 -> { // dart + #469 motion streaks
                    for (tr3 in 1..2) { p.alpha = 80 - tr3 * 25; poly(c, e.x - cos(efa) * e.radius * 0.55f * tr3, e.y - sin(efa) * e.radius * 0.55f * tr3, e.radius * (1f - tr3 * 0.22f), 4, time * 5f, col) }
                    p.alpha = 255
                    poly(c, e.x, e.y, e.radius, 4, time * 5f, col)
                }""")
E("""                c.drawCircle(aimOx + aimDx * dp(38f), aimOy + aimDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255
            }""",
"""                c.drawCircle(aimOx + aimDx * dp(38f), aimOy + aimDy * dp(38f), dp(24f) * save.joySize, p); p.alpha = 255
                if (hyp(aimDx, aimDy) > 0.12f) { // #470 arrowhead shows the fire direction
                    val aa2 = atan2(aimDy, aimDx)
                    poly(c, aimOx + cos(aa2) * dp(62f), aimOy + sin(aa2) * dp(62f), dp(8f), 4, aa2 + 0.78f, Color.rgb(244, 114, 182))
                }
            }""")
E("""                    p.color = Color.rgb(250, 204, 21); c.drawCircle(e.x - cos(efa) * e.radius * 0.85f, e.y - sin(efa) * e.radius * 0.85f - e.radius * 0.4f, dp(2.4f), p)""",
"""                    p.color = Color.rgb(250, 204, 21); c.drawCircle(e.x - cos(efa) * e.radius * 0.85f, e.y - sin(efa) * e.radius * 0.85f - e.radius * 0.4f, dp(2.4f), p)
                    if ((time * 2f + e.wob).toInt() % 3 == 0) { p.color = Color.WHITE; c.drawCircle(e.x - cos(efa) * e.radius * 0.85f + dp(2f), e.y - sin(efa) * e.radius * 0.85f - e.radius * 0.4f, dp(1.2f), p) } // #472 loot glints in the sack""")

# ============ version strings ============
E("""v2.1 — 424-upgrade edition""", """v2.3 — 472-upgrade edition""")
E("""Neon Abyss — version $ver (424-upgrade edition)""", """Neon Abyss — version $ver (472-upgrade edition)""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK part 2: {len(EDITS)} edits applied")
