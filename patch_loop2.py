#!/usr/bin/env python3
"""GemStorm graphics loop cycle 2 — upgrades #509-520."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        p.alpha = 255
        // neon grid — #53 pulses on the 126 BPM beat""",
"""        p.alpha = 255
        p.color = Color.rgb(165, 243, 252); p.alpha = 13 // #509 soft spotlight follows your turtle
        c.drawCircle(px, py, dp(200f), p); p.alpha = 255
        // neon grid — #53 pulses on the 126 BPM beat""")

E("""                p.color = Color.rgb(74, 222, 128)
                c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f), p)""",
"""                p.color = if (e.elite) Color.rgb(250, 204, 21) else Color.rgb(74, 222, 128) // #510 elite bars run gold
                c.drawRect(e.x - e.radius, e.y - e.radius - dp(10f), e.x - e.radius + bw * (e.hp / e.maxHp), e.y - e.radius - dp(5f), p)
                p.color = Color.WHITE; p.alpha = 200 // #510 bright tip on the fill edge
                c.drawCircle(e.x - e.radius + bw * (e.hp / e.maxHp).coerceIn(0f, 1f), e.y - e.radius - dp(7.5f), dp(1.6f), p); p.alpha = 255""")

E("""            if (e.burnT > 0) e.burnT -= dt""",
"""            if (e.burnT > 0) e.burnT -= dt
            if (e.burnT > 0 && particles.size < 340 && Random.nextFloat() < 0.22f) particles.add(Particle(e.x + Random.nextFloat() * 12 - 6, e.y - e.radius * 0.4f, 0f, -46f, 0.4f, 0.4f, Color.rgb(251, 146, 60), dp(2.6f))) // #511 burning foes shed embers
            if (e.type == 11 && particles.size < 340 && Random.nextFloat() < 0.12f) particles.add(Particle(e.x, e.y + e.radius * 0.5f, 0f, 12f, 0.5f, 0.5f, Color.rgb(250, 204, 21), dp(2.4f))) // #512 gnome gold dust trail""")

E("""            if (e.type == 9) for (i in 0 until 3) { // #116 Necroshard births 3 darts
                audio.sfx("necropop", 1f - i * 0.12f) // #271 triple pop
                val mini = Enemy(4, e.x + Random.nextFloat() * 36 - 18, e.y + Random.nextFloat() * 36 - 18,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 250f, dp(10f), 6f, 1, 8)
                enemies.add(mini); waveTotal++ // v2.3 spawned mid-wave foes count toward the wave total
                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(6f), 0.25f, 0.25f, Color.rgb(74, 222, 128))) // #467 birth pop ring
            }""",
"""            if (e.type == 9) for (i in 0 until 3) { // #116 Necroshard births 3 darts
                audio.sfx("necropop", 1f - i * 0.12f) // #271 triple pop
                val mini = Enemy(4, e.x + Random.nextFloat() * 36 - 18, e.y + Random.nextFloat() * 36 - 18,
                    9f * (1f + (wave - 1) * 0.24f), 9f * (1f + (wave - 1) * 0.24f), 250f, dp(10f), 6f, 1, 8)
                enemies.add(mini); waveTotal++ // v2.3 spawned mid-wave foes count toward the wave total
                if (rings.size < 14) rings.add(Ring(mini.x, mini.y, dp(7f), 0.3f, 0.3f, Color.rgb(232, 121, 249))) // #513 necro births ring fuchsia
            }""")

E("""            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)""",
"""            burst(e.x, e.y, enemyColor(e.type), if (e.type == 5) 70 else 16, 380f)
            if (rings.size < 20) rings.add(Ring(e.x, e.y, dp(5f), 0.18f, 0.18f, Color.WHITE)) // #514 white core flash on every kill""")

E("""        for (b in bolts) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(253, 224, 71)""",
"""        for (b in bolts) {
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(7f); p.color = Color.rgb(253, 224, 71); p.alpha = (46 * (b.life / 0.14f)).toInt().coerceIn(0, 46) // #515 soft glow under the bolt
            c.drawLine(b.x1, b.y1, b.x2, b.y2, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2.4f); p.color = Color.rgb(253, 224, 71)""")

E("""            p.color = Color.BLACK; p.alpha = (90 + 80 * (1f - frac)).toInt() // #190 shadow grows as it falls
            c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p)""",
"""            p.color = Color.BLACK; p.alpha = (90 + 80 * (1f - frac)).toInt() // #190 shadow grows as it falls
            c.drawCircle(m.x, m.y, dp(26f) + dp(46f) * (1f - frac), p)
            if (frac < 0.35f) { // #516 final-approach strobe ring
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = Color.rgb(239, 68, 68)
                p.alpha = if (((time * 9f).toInt() % 2) == 0) 220 else 60
                c.drawCircle(m.x, m.y, dp(30f), p); p.style = Paint.Style.FILL; p.alpha = 255
            }""")

E("""        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow""",
"""        if (xpFrac > 0.02f) { p.color = Color.WHITE; p.alpha = (150 + 80 * sin(time * 6f)).toInt().coerceIn(70, 230); c.drawCircle(m + barW * xpFrac, xpY + dp(4.5f), dp(2.2f), p); p.alpha = 255 } // #491 fill-tip glow
        for (q in 1..3) { val qx = m + barW * q / 4f; p.color = Color.rgb(7, 3, 18); p.alpha = 130; c.drawRect(qx - dp(0.7f), xpY + dp(1f), qx + dp(0.7f), xpY + dp(8f), p) } // #517 XP quarter ticks
        p.alpha = 255""")

E("""    private var bossGhostHp = 0f // #487""",
"""    private var bossGhostHp = 0f // #487
    private var playerGhostHp = 0f // #518""")

E("""        if (bb2 != null) { if (bossGhostHp < bb2.hp) bossGhostHp = bb2.hp; if (bossGhostHp > bb2.hp) bossGhostHp = max(bb2.hp, bossGhostHp - bb2.maxHp * 0.12f * dt) } else bossGhostHp = 0f""",
"""        if (bb2 != null) { if (bossGhostHp < bb2.hp) bossGhostHp = bb2.hp; if (bossGhostHp > bb2.hp) bossGhostHp = max(bb2.hp, bossGhostHp - bb2.maxHp * 0.12f * dt) } else bossGhostHp = 0f
        if (playerGhostHp < hp) playerGhostHp = hp; if (playerGhostHp > hp) playerGhostHp = max(hp, playerGhostHp - maxHp * 0.10f * dt) // #518""")

E("""        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)""",
"""        if (playerGhostHp > hp) roundRect(c, RectF(m, m, m + barW * (playerGhostHp / maxHp).coerceIn(0f, 1f), m + barH), dp(7f), Color.rgb(254, 226, 226)) // #518 damage ghost trails your HP
        if (hpFrac > 0) roundRect(c, RectF(m, m, m + barW * hpFrac, m + barH), dp(7f), hpCol)""")

E("""                // #504 shine sweep circles the shell gem
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.WHITE; p.alpha = 110
                c.drawArc(RectF(bpx - dp(10.5f), bpy - dp(10.5f), bpx + dp(10.5f), bpy + dp(10.5f)), time * 150f, 65f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255""",
"""                // #504 shine sweep circles the shell gem
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.WHITE; p.alpha = 110
                c.drawArc(RectF(bpx - dp(10.5f), bpy - dp(10.5f), bpx + dp(10.5f), bpy + dp(10.5f)), time * 150f, 65f, false, p)
                p.style = Paint.Style.FILL; p.alpha = 255
                if (dashCharges < dashMaxCharges) { // #519 dash recharge arc rings the shell
                    val dprog = 1f - (dashRechargeT / dashCdMax).coerceIn(0f, 1f)
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = Color.rgb(34, 211, 238); p.alpha = 170
                    c.drawArc(RectF(bpx - dp(24.5f), bpy - dp(24.5f), bpx + dp(24.5f), bpy + dp(24.5f)), -90f, 360f * dprog, false, p)
                    p.style = Paint.Style.FILL; p.alpha = 255
                }""")

E("""            p.color = Color.rgb(52, 211, 153); p.alpha = (22 + 10 * sin(time * 3f)).toInt().coerceIn(12, 32)
            c.drawCircle(mx, my, dp(90f), p); p.alpha = 255""",
"""            p.color = Color.rgb(52, 211, 153); p.alpha = (22 + 10 * sin(time * 3f)).toInt().coerceIn(12, 32)
            c.drawCircle(mx, my, dp(90f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.alpha = 60 // #520 rotating dashed ring on the merge glow
            for (k in 0 until 3) c.drawArc(RectF(mx - dp(90f), my - dp(90f), mx + dp(90f), my + dp(90f)), time * 40f + k * 120f, 60f, false, p)
            p.style = Paint.Style.FILL; p.alpha = 255""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop2: {len(EDITS)} edits applied")
