#!/usr/bin/env python3
"""GemStorm graphics loop cycle 7 — upgrades #569-580."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = Color.rgb(168, 85, 247); p.alpha = 170
        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p)""",
"""        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.5f); p.color = CHARS[save.selectedChar].color; p.alpha = (140 + 60 * sin(time * 2.2f)).toInt().coerceIn(80, 200)
        c.drawRoundRect(RectF(dp(12f), dp(12f), w - dp(12f), h - dp(12f)), dp(18f), dp(18f), p) // #569 pause frame breathes in your hero's color""")

E("""        if (victory) { // #249 slow victory zoom on the hero""",
"""        if (victory) { // #570 golden glow orbs turn behind the victory
            for (k in 0 until 6) { val ra = time * 0.24f + k * 1.047f; p.color = Color.rgb(250, 204, 21); p.alpha = 12; c.drawCircle(w / 2 + cos(ra) * dp(170f), h * 0.30f + sin(ra) * dp(150f), dp(52f), p) }
            p.alpha = 255
        }
        if (victory) { // #249 slow victory zoom on the hero""")

E("""        if (!victory) { // #100 shattered gem artwork""",
"""        if (!victory) { // #571 red embers drift up through the wreckage
            for (k in 0 until 7) { val ex2 = (k * 0.37f + 0.05f) * w; val ey2 = h - ((time * 20f + k * 211f) % (h + 30f)) + dp(15f)
                p.color = Color.rgb(248, 113, 113); p.alpha = (50 + 40 * sin(time * 2.4f + k * 1.9f)).toInt().coerceIn(10, 90); c.drawCircle(ex2, ey2, dp(2f), p) }
            p.alpha = 255
        }
        if (!victory) { // #100 shattered gem artwork""")

E("""    private fun drawLevelUp(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 200; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255""",
"""    private fun drawLevelUp(c: Canvas) {
        p.color = Color.rgb(4, 2, 12); p.alpha = 200; p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p); p.alpha = 255
        for (k in 0 until 9) { // #572 gold motes rise through the draft
            val mx2 = (k * 0.618f + 0.07f) * w; val my2 = h - ((time * 26f + k * 173f) % (h + 40f)) + dp(20f)
            p.color = Color.rgb(250, 204, 21); p.alpha = (60 + 50 * sin(time * 2f + k * 1.7f)).toInt().coerceIn(10, 110)
            c.drawCircle(mx2, my2, dp(2.2f), p)
        }
        p.alpha = 255""")

E("""            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail""",
"""            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail
            if (e.type == 11 && hyp(px - e.x, py - e.y) < dp(130f) && particles.size < 340 && Random.nextFloat() < 0.2f) particles.add(Particle(e.x, e.y - e.radius * 0.6f, (Random.nextFloat() - 0.5f) * 30f, -40f, 0.35f, 0.35f, Color.rgb(147, 197, 253), dp(2f))) // #573 gnome panic sweat when you close in""")

E("""            if (e.type == 8) { // #541 orbiter's path rings around you
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.WHITE; p.alpha = 26
                c.drawCircle(px, py, hyp(e.x - px, e.y - py), p); p.style = Paint.Style.FILL; p.alpha = 255
            }""",
"""            if (e.type == 8) { // #541 orbiter's path rings around you
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.WHITE; p.alpha = 26
                c.drawCircle(px, py, hyp(e.x - px, e.y - py), p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 7 && e.state == 1) { // #574 the lancer telegraphs its charge line
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(239, 68, 68); p.alpha = (40 + 30 * sin(time * 10f)).toInt().coerceIn(10, 70)
                c.drawLine(e.x, e.y, px, py, p); p.style = Paint.Style.FILL; p.alpha = 255
            }
            if (e.type == 3) { // #575 splitters jitter a ghost echo
                p.color = enemyColor(3); p.alpha = 36
                c.drawCircle(e.x + sin(e.wob * 2.2f) * dp(4f), e.y + cos(e.wob * 1.9f) * dp(3f), e.radius * 0.9f, p); p.alpha = 255
            }
            if (e.type == 2) { // #576 brute shoulders smolder
                p.color = Color.rgb(249, 115, 22); p.alpha = (60 + 40 * sin(time * 4.5f + e.x * 0.02f)).toInt().coerceIn(20, 100)
                c.drawCircle(e.x - e.radius * 0.55f, e.y - e.radius * 0.5f, dp(3.6f), p)
                c.drawCircle(e.x + e.radius * 0.55f, e.y - e.radius * 0.5f, dp(3.6f), p); p.alpha = 255
            }
            if (e.type == 8) { // #578 a satellite spark orbits the orbiter
                p.color = Color.WHITE; p.alpha = 190
                c.drawCircle(e.x + cos(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), e.y + sin(time * 3.2f + e.y * 0.01f) * (e.radius + dp(6f)), dp(1.9f), p); p.alpha = 255
            }""")

E("""            if (e.type == 6 && particles.size < 340 && Random.nextFloat() < 0.10f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 10f, 0.55f, 0.55f, Color.rgb(226, 232, 240), dp(2.2f))) // #537 weaver silk dots""",
"""            if (e.type == 6 && particles.size < 340 && Random.nextFloat() < 0.10f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 10f, 0.55f, 0.55f, Color.rgb(226, 232, 240), dp(2.2f))) // #537 weaver silk dots
            if (e.type == 1 && particles.size < 340 && Random.nextFloat() < 0.08f) particles.add(Particle(e.x, e.y, 0f, 0f, 0.4f, 0.4f, enemyColor(1), dp(2f))) // #577 dart ribbon dots""")

E("""                texts.add(FloatText(px, py - dp(64f), "🔥 $kills KILL STREAK! +25💎", 1.1f, Color.rgb(251, 146, 60), dp(16f)))""",
"""                texts.add(FloatText(px, py - dp(64f), "🔥 $kills KILL STREAK! +25💎", 1.1f, Color.rgb(251, 146, 60), dp(16f)))
                if (rings.size < 16) rings.add(Ring(px, py, dp(14f), 0.3f, 0.3f, Color.rgb(251, 146, 60))) // #579 streaks pop an orange ring""")

E("""        if (save.selectedChar == 1 && dodgeMeter > 0.2f && Random.nextFloat() < 0.05f + 0.25f * dodgeMeter) { // #138 Swift dodge
            texts.add(FloatText(px, py - dp(34f), "💨 DODGED!", 0.6f, Color.rgb(250, 204, 21), dp(14f)))""",
"""        if (save.selectedChar == 1 && dodgeMeter > 0.2f && Random.nextFloat() < 0.05f + 0.25f * dodgeMeter) { // #138 Swift dodge
            texts.add(FloatText(px, py - dp(34f), "💨 DODGED!", 0.6f, Color.rgb(250, 204, 21), dp(14f)))
            if (rings.size < 12) rings.add(Ring(px, py, dp(18f), 0.28f, 0.28f, Color.rgb(250, 204, 21))) // #580 a dodge pops a gold ring""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop7: {len(EDITS)} edits applied")
