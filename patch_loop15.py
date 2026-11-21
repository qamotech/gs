#!/usr/bin/env python3
"""GemStorm graphics loop cycle 15 — upgrades #665-676."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""                    p.color = Color.WHITE; p.alpha = (120 + 60 * sin(time * 8f)).toInt().coerceIn(60, 180) // #465 ribbon head sparkles
                    c.drawCircle(e.x - cos(efa) * e.radius * 0.78f, e.y - sin(efa) * e.radius * 0.78f, dp(2f), p); p.alpha = 255
                }""",
"""                    p.color = Color.WHITE; p.alpha = (120 + 60 * sin(time * 8f)).toInt().coerceIn(60, 180) // #465 ribbon head sparkles
                    c.drawCircle(e.x - cos(efa) * e.radius * 0.78f, e.y - sin(efa) * e.radius * 0.78f, dp(2f), p); p.alpha = 255
                    p.color = shade(col, 1.3f); p.alpha = 120; c.drawCircle(e.x + cos(efa + 1.57f) * e.radius * 0.8f, e.y + sin(efa + 1.57f) * e.radius * 0.8f, dp(1.8f), p); c.drawCircle(e.x - cos(efa + 1.57f) * e.radius * 0.8f, e.y - sin(efa + 1.57f) * e.radius * 0.8f, dp(1.8f), p); p.alpha = 255 // #665 silk anchor beads flank the weaver
                }""")

E("""                    poly(c, e.x + cos(efa) * e.radius * 1.8f, e.y + sin(efa) * e.radius * 1.8f, dp(5f), 3, efa + 1.57f, shade(col, 1.6f))
                }""",
"""                    poly(c, e.x + cos(efa) * e.radius * 1.8f, e.y + sin(efa) * e.radius * 1.8f, dp(5f), 3, efa + 1.57f, shade(col, 1.6f))
                    if (e.state == 1) { p.color = Color.WHITE; p.alpha = (140 + 100 * sin(time * 30f)).toInt().coerceIn(40, 240); c.drawCircle(e.x + cos(efa) * e.radius * 1.8f, e.y + sin(efa) * e.radius * 1.8f, dp(2.2f), p); p.alpha = 255 } // #666 the winding-up lance tip strobes
                }""")

E("""            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
            }""",
"""            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
                p.color = shade(enemyColor(8), 1.5f); p.alpha = 120; c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f + 3.1416f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f + 3.1416f) * (e.radius + dp(6f)), dp(1.4f), p); p.alpha = 255 // #667 a dimmer twin spark counterweights it
            }""")

E("""                9 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.2f, col); poly(c, e.x, e.y, e.radius * 0.5f, 4, -time * 2f, Color.rgb(240, 171, 252)) }""",
"""                9 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.2f, col); poly(c, e.x, e.y, e.radius * 0.5f, 4, -time * 2f, Color.rgb(240, 171, 252)); poly(c, e.x, e.y, e.radius * 0.78f, 4, time * 0.7f, Color.rgb(88, 28, 135), false) } // #668 a rune ring turns inside the Necroshard""")

E("""                c.drawArc(RectF(e.x - e.radius - dp(4f), e.y - e.radius - dp(4f), e.x + e.radius + dp(4f), e.y + e.radius + dp(4f)), time * 70f, 150f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                c.drawArc(RectF(e.x - e.radius - dp(4f), e.y - e.radius - dp(4f), e.x + e.radius + dp(4f), e.y + e.radius + dp(4f)), time * 70f, 150f, false, p)
                p.style = Paint.Style.FILL; (0 until 3).forEach { k -> val ba3 = (time * 70f + k * 50f) * 0.01745f; p.color = Color.rgb(219, 234, 254); p.alpha = 170; c.drawCircle(e.x + cos(ba3) * (e.radius + dp(4f)), e.y + sin(ba3) * (e.radius + dp(4f)), dp(1.5f), p) } // #669 rivets ride the guard arc
                p.alpha = 255""")

E("""                11 -> { poly(c, e.x, e.y, e.radius, 4, 0.78f + sin(time * 6f) * 0.2f, col); poly(c, e.x, e.y - e.radius * 1.15f, dp(6f), 3, -1.57f, Color.rgb(34, 197, 94)) }""",
"""                11 -> { poly(c, e.x, e.y, e.radius, 4, 0.78f + sin(time * 6f) * 0.2f, col); poly(c, e.x, e.y - e.radius * 1.15f, dp(6f), 3, -1.57f, Color.rgb(34, 197, 94)); p.color = Color.rgb(250, 204, 21); p.alpha = (170 + 70 * sin(time * 7f)).toInt().coerceIn(100, 240); c.drawCircle(e.x, e.y - e.radius * 1.55f - (sin(time * 5f) * sin(time * 5f)) * dp(4f), dp(3.2f), p); p.alpha = 255 } // #670 a gold coin bounces over the gnome""")

E("""                        p.color = Color.WHITE; p.alpha = 170; c.drawCircle(pcx - dp(2.5f), pcy - dp(2.5f), dp(1.1f), p); p.alpha = 255 // #433 rivet glint
                    }
                }""",
"""                        p.color = Color.WHITE; p.alpha = 170; c.drawCircle(pcx - dp(2.5f), pcy - dp(2.5f), dp(1.1f), p); p.alpha = 255 // #433 rivet glint
                    }
                    if (eHpF < 0.3f) { p.color = Color.rgb(239, 68, 68); p.alpha = (120 + 80 * sin(time * 9f)).toInt().coerceIn(40, 200); c.drawCircle(e.x, e.y, e.radius * 0.3f, p); p.alpha = 255 } // #671 a wounded brute's core burns red
                }""")

E("""                3 -> poly(c, e.x, e.y, e.radius, 4, time * 1.6f, col)""",
"""                3 -> { poly(c, e.x, e.y, e.radius, 4, time * 1.6f, col); poly(c, e.x, e.y, e.radius * 0.55f, 4, -time * 2.3f, shade(col, 1.5f)) } // #672 the splitter's heart counter-turns""")

E("""                4 -> poly(c, e.x, e.y, e.radius * (1f + 0.2f * sin(time * 10f + e.wob)), 4, time * 3f, col)""",
"""                4 -> { poly(c, e.x, e.y, e.radius * (1f + 0.2f * sin(time * 10f + e.wob)), 4, time * 3f, col); p.color = Color.WHITE; p.alpha = 190; c.drawCircle(e.x, e.y, dp(1.6f), p); p.alpha = 255 } // #673 minis carry a bright speck""")

E("""                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))
                    }""",
"""                        poly(c, e.x + cos(a) * e.radius * 1.05f, e.y + sin(a) * e.radius * 1.05f, spikeR * 0.45f, 3, a, Color.rgb(254, 240, 138))
                    }
                    if (((time * 2f + e.x * 0.01f).toInt() % 3) == 0) poly(c, e.x, e.y - e.radius * 1.05f, dp(4.4f), 4, time * 3f, Color.WHITE) // #674 the crown's center gem flashes""")

E("""                0 -> for (k in 0 until 4) { val ta = efa + (k - 1.5f) * 0.30f; poly(c, e.x + cos(ta) * e.radius * 0.88f, e.y + sin(ta) * e.radius * 0.88f, dp(3.4f), 3, ta + 1.57f, Color.rgb(226, 232, 240)) } // #413 chaser teeth""",
"""                0 -> { for (k in 0 until 4) { val ta = efa + (k - 1.5f) * 0.30f; poly(c, e.x + cos(ta) * e.radius * 0.88f, e.y + sin(ta) * e.radius * 0.88f, dp(3.4f), 3, ta + 1.57f, Color.rgb(226, 232, 240)) } // #413 chaser teeth
                    p.color = Color.rgb(254, 202, 202); p.alpha = (90 + 60 * sin(time * 5f + e.wob)).toInt().coerceIn(30, 150); c.drawCircle(e.x + cos(efa) * e.radius * 0.3f, e.y + sin(efa) * e.radius * 0.3f, dp(2f), p); p.alpha = 255 } // #675 the chaser's maw glows""")

E("""            if (e.slowT > 0 && particles.size < 340 && Random.nextFloat() < 0.1f) particles.add(Particle(e.x + Random.nextFloat() * 14 - 7, e.y + e.radius * 0.5f, 0f, 34f, 0.5f, 0.5f, Color.rgb(207, 250, 254), dp(5f))) // #237 frost mist""",
"""            if (e.slowT > 0 && particles.size < 340 && Random.nextFloat() < 0.1f) particles.add(Particle(e.x + Random.nextFloat() * 14 - 7, e.y + e.radius * 0.5f, 0f, 34f, 0.5f, 0.5f, Color.rgb(207, 250, 254), dp(5f))) // #237 frost mist
            if (e.slowT > 0 && particles.size < 340 && Random.nextFloat() < 0.05f) particles.add(Particle(e.x, e.y - e.radius, 0f, -22f, 0.7f, 0.7f, Color.rgb(224, 242, 254), dp(3.4f))) // #676 slowed foes breathe frost upward""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop15: {len(EDITS)} edits applied")
