#!/usr/bin/env python3
"""GemStorm v2.3.1: Nathan's field notes — one red heart (rare, no beacon), ALL heroes are turtles."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

# ---- hearts: rare + capped, boss gives exactly one ----
E("""            // 💚 heart drops: brutes/bosses/elites are generous
            val heartChance = when { e.type == 5 -> 1f; e.elite -> 0.5f; e.type == 2 -> 0.3f; else -> 0.04f }
            if (Random.nextFloat() < heartChance) {
                val hd = HeartDrop(e.x, e.y)
                hd.vx = Random.nextFloat() * 140 - 70; hd.vy = Random.nextFloat() * 140 - 70
                hearts.add(hd)
                if (e.type == 5) { val hd2 = HeartDrop(e.x + 30, e.y + 20); hearts.add(hd2) }
            }""",
"""            // ❤️ heart drops (v2.3.1: rare treats, max 2 on the field, boss gives exactly one)
            val heartChance = when { e.type == 5 -> 1f; e.elite -> 0.18f; e.type == 2 -> 0.10f; else -> 0.012f }
            if (Random.nextFloat() < heartChance && hearts.size < 2) {
                val hd = HeartDrop(e.x, e.y)
                hd.vx = Random.nextFloat() * 140 - 70; hd.vy = Random.nextFloat() * 140 - 70
                hearts.add(hd)
            }""")
# ---- hearts: ONE red heart, beacon + ring + cross removed ----
E("""            p.color = Color.rgb(74, 222, 128); p.alpha = 26 // #246 heart beacon light column
            c.drawRect(hd.x - dp(9f), hd.y - dp(300f), hd.x + dp(9f), hd.y, p)
            p.alpha = 52
            c.drawRect(hd.x - dp(3f), hd.y - dp(300f), hd.x + dp(3f), hd.y, p)
            p.alpha = 255
            p.color = Color.rgb(74, 222, 128); p.alpha = 60
            c.drawCircle(hd.x, hd.y, r * 2f, p); p.alpha = 255
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(244, 114, 182) // #443 heartbeat ring
            p.alpha = (60 + 40 * sin(time * 6f + hd.x * 0.03f)).toInt().coerceIn(20, 100)
            c.drawCircle(hd.x, hd.y, r * (2.6f + 0.25f * sin(time * 6f + hd.x * 0.03f)), p)
            p.style = Paint.Style.FILL; p.alpha = 255
            p.color = Color.rgb(244, 114, 182)
            c.drawCircle(hd.x - r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            c.drawCircle(hd.x + r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            poly(c, hd.x, hd.y + r * 0.62f, r * 0.78f, 3, 3.14159f, Color.rgb(244, 114, 182))
            poly(c, hd.x, hd.y, r * 0.52f, 4, 0.78f, Color.rgb(249, 168, 212))
            p.color = Color.WHITE
            c.drawRect(hd.x - dp(1.6f), hd.y - dp(5f), hd.x + dp(1.6f), hd.y + dp(5f), p)
            c.drawRect(hd.x - dp(5f), hd.y - dp(1.6f), hd.x + dp(5f), hd.y + dp(1.6f), p)""",
"""            // v2.3.1: ONE red heart — the screen-filling green beacon (#246), the ring and the cross are gone
            p.color = Color.rgb(220, 38, 38); p.alpha = 45
            c.drawCircle(hd.x, hd.y, r * 1.9f, p); p.alpha = 255
            p.color = Color.rgb(239, 68, 68)
            c.drawCircle(hd.x - r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            c.drawCircle(hd.x + r * 0.45f, hd.y - r * 0.25f, r * 0.55f, p)
            poly(c, hd.x, hd.y + r * 0.62f, r * 0.78f, 3, 3.14159f, Color.rgb(239, 68, 68))
            poly(c, hd.x, hd.y, r * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))""")
E("""            "💚 HEARTS" to "Brutes, elites and bosses drop hearts that heal 15 HP.",""",
"""            "❤️ HEARTS" to "Elites and bosses sometimes drop a red heart that heals 15 HP. Rare — grab it!",""")
E("""            "💚 Hearts collected" to "${save.heartsCollected}",""",
"""            "❤️ Hearts collected" to "${save.heartsCollected}",""")

# ---- ALL heroes are turtles now, tinted by hero color ----
E("""            if (save.selectedChar == 5 && !blink) { // 🐢💎N🎱 TURTLE WITH JETS""",
"""            if (!blink) { // 🐢 TURTLE WITH JETS — every hero is a turtle (v2.3.1), shell tinted by hero color""")
E("""                    p.color = Color.rgb(22, 163, 74)
                    c.drawOval(RectF(bpx + fx2 * dp(15f)""",
"""                    p.color = shade(chCol, 0.75f)
                    c.drawOval(RectF(bpx + fx2 * dp(15f)""")
E("""                p.color = Color.rgb(20, 83, 45); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                p.color = Color.rgb(34, 197, 94); c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(134, 239, 172); p.alpha = 170""",
"""                p.color = shade(chCol, 0.42f); c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p)
                p.color = chCol; c.drawCircle(bpx, bpy, dp(17.5f) * spawnSc, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = shade(chCol, 1.45f); p.alpha = 170""")
E("""                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, Color.rgb(34, 211, 238))""",
"""                poly(c, bpx, bpy, dp(8.6f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, shade(chCol, 0.4f))
                poly(c, bpx, bpy, dp(7f) * spawnSc * (1f + 0.1f * sin(time * 5f)), 4, 0.78f, Color.rgb(34, 211, 238))""")
E("""                p.color = Color.rgb(22, 163, 74); c.drawCircle(hx2, hy2, dp(8.6f) * spawnSc, p)
                p.color = Color.rgb(74, 222, 128); c.drawCircle(hx2, hy2, dp(7.4f) * spawnSc, p)""",
"""                p.color = shade(chCol, 0.75f); c.drawCircle(hx2, hy2, dp(8.6f) * spawnSc, p)
                p.color = shade(chCol, 1.12f); c.drawCircle(hx2, hy2, dp(7.4f) * spawnSc, p)""")
# ---- retire the old gem-ship body for good ----
E("""            if (!blink && save.selectedChar != 5) { // #401 layered armor plate + bright rim under the gem body""",
"""            if (!blink && save.selectedChar == -1) { // #401 RETIRED v2.3.1 — every hero is a turtle now""")
E("""            if (!blink && save.selectedChar != 5) {
                val rot = sin(time * 2.2f) * 0.14f""",
"""            if (!blink && save.selectedChar == -1) { // gem-ship body RETIRED v2.3.1
                val rot = sin(time * 2.2f) * 0.14f""")
E("""            if (save.selectedChar != 5) { // #403 visor slit + glint ride the aim direction (the turtle has real eyes)""",
"""            if (save.selectedChar == -1) { // #403 visor RETIRED v2.3.1 — turtles have real eyes""")
E("""            if (hyp(mvx2, mvy2) > 0.25f && save.selectedChar != 5) { // the turtle's flames live in its jet pods""",
"""            if (hyp(mvx2, mvy2) > 0.25f && save.selectedChar == -1) { // RETIRED v2.3.1 — flames live in the jet pods""")
E("""                if (save.selectedChar == 5) c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p) else poly(c, bpx, bpy, dp(21f) * spawnSc, 4, 0.78f, Color.WHITE)""",
"""                c.drawCircle(bpx, bpy, dp(21f) * spawnSc, p) // v2.3.1: hurt flash is a shell flash now""")
E("""            if (save.selectedChar != 5) poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE) // the turtle's gem has its own glint""",
"""            if (save.selectedChar == -1) poly(c, px + cos(shineA) * dp(10f), py + sin(shineA) * dp(10f), dp(4f), 4, shineA, Color.WHITE) // RETIRED v2.3.1 — the shell gem has its own glint""")
E("""                1 -> for (s in -1..1 step 2) { // Swift thruster pods (the old fins were triangles — removed v2.3 per Nathan)
                    val podX = bpx - cos(lastAim) * dp(16f) + cos(lastAim + 1.57f) * dp(11f) * s
                    val podY = bpy - sin(lastAim) * dp(16f) + sin(lastAim + 1.57f) * dp(11f) * s
                    p.color = shade(chCol, 1.2f); c.drawCircle(podX, podY, dp(5f), p)
                    p.color = Color.rgb(254, 240, 138); c.drawCircle(podX, podY, dp(2f), p)
                }""",
"""                1 -> { } // Swift: the turtle's jet pods are its signature now""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK v2.3.1: {len(EDITS)} edits applied")
