#!/usr/bin/env python3
"""GemStorm graphics loop cycle 5 — upgrades #545-556."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""            p.textAlign = Paint.Align.LEFT; p.color = titleCol; p.alpha = (lt * 255).toInt().coerceIn(0, 255)
            c.drawText(titleStr[i].toString(), lx, cy + dp(108f) - (1f - le) * dp(52f), p)
            lx += lws[i]""",
"""            p.textAlign = Paint.Align.LEFT; p.color = titleCol; p.alpha = (lt * 255).toInt().coerceIn(0, 255)
            c.drawText(titleStr[i].toString(), lx, cy + dp(108f) - (1f - le) * dp(52f), p)
            val sh5 = 0.5f + 0.5f * sin(time * 2.6f - i * 0.6f) // #545 gold shimmer travels the title
            p.color = Color.rgb(250, 204, 21); p.alpha = (lt * 120 * sh5).toInt().coerceIn(0, 120)
            c.drawText(titleStr[i].toString(), lx, cy + dp(108f) - (1f - le) * dp(52f), p)
            lx += lws[i]""")

E("""    private fun explodeProj(pr: Proj, dead: MutableList<Enemy>) { // #130 Meteor Staff AoE
        rings.add(Ring(pr.x, pr.y, dp(10f), 0.35f, 0.35f, Color.rgb(249, 115, 22)))""",
"""    private fun explodeProj(pr: Proj, dead: MutableList<Enemy>) { // #130 Meteor Staff AoE
        if (pr.rocket) blooms.add(Bloom(pr.x, pr.y, 0.22f, dp(120f), Color.WHITE)) // #546 rocket impacts flash white-hot
        rings.add(Ring(pr.x, pr.y, dp(10f), 0.35f, 0.35f, Color.rgb(249, 115, 22)))""")

E("""        px = (px + pvx * dt).coerceIn(dp(20f), w - dp(20f))""",
"""        px = (px + pvx * dt).coerceIn(dp(20f), w - dp(20f))
        if ((pvx * pvx + pvy * pvy) > 3600f && rings.size < 16 && Random.nextFloat() < 0.10f) rings.add(Ring(px, py + dp(15f), dp(3f), 0.5f, 0.5f, CHARS[save.selectedChar].color)) // #547 flipper ripples trail as you swim""")

E("""        for ((nx, ny, ncol) in neb) {
            val np2 = 0.82f + 0.18f * sin(time * 0.23f + nx * 0.011f) // #497 nebulae breathe
            p.color = ncol; p.alpha = (26 * np2).toInt()
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = (16 * np2).toInt()
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = (12 * np2).toInt()""",
"""        for ((nx, ny, ncol) in neb) {
            val np2 = 0.82f + 0.18f * sin(time * 0.23f + nx * 0.011f) // #497 nebulae breathe
            p.color = ncol; p.alpha = (26 * np2).toInt()
            c.drawCircle(nx, ny, dp(120f), p)
            p.alpha = (16 * np2).toInt()
            c.drawCircle(nx, ny, dp(180f), p)
            p.alpha = (12 * np2).toInt()
            if (sin(time * 1.9f + nx * 0.021f + ny * 0.013f) > 0.93f) poly(c, nx, ny, dp(7f), 4, time * 1.2f, Color.WHITE) // #548 nebula cores twinkle""")

E("""        while (gy < h) { c.drawLine(0f, gy, w, gy, p); gy += gap }
        p.alpha = 255; p.style = Paint.Style.FILL""",
"""        while (gy < h) { c.drawLine(0f, gy, w, gy, p); gy += gap }
        val shx = ((time * 0.045f) % 1.4f - 0.2f) * w // #549 a slow sheen sweeps the grid
        p.color = Color.WHITE; p.alpha = 7
        c.drawLine(shx, 0f, shx - dp(120f), h, p)
        c.drawLine(shx + dp(26f), 0f, shx + dp(26f) - dp(120f), h, p)
        p.alpha = 255; p.style = Paint.Style.FILL""")

E("""            if (e.elite) { eliteBannerT = 1.4f; save.elitesSlain++; rerollTokens++; texts.add(FloatText(e.x, e.y - dp(24f), "+1 🔄 REROLL", 0.8f, Color.rgb(165, 243, 252), dp(12f))) } // #139""",
"""            if (e.elite) { eliteBannerT = 1.4f; save.elitesSlain++; rerollTokens++; texts.add(FloatText(e.x, e.y - dp(24f), "+1 🔄 REROLL", 0.8f, Color.rgb(165, 243, 252), dp(12f))) } // #139
            if (e.elite) texts.add(FloatText(e.x, e.y - dp(44f), "👑 ELITE DOWN", 0.9f, Color.rgb(250, 204, 21), dp(13f))) // #550 elites get a crown callout""")

E("""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)""",
"""            txtL(c, "🔥 COMBO $combo  ×$cmult", m, xpY + dp(44f), dp(13f) * (1f + 0.25f * max(0f, comboPop)), Color.rgb(251, 146, 60), true)
            if (combo >= 50) for (k in 0 until 3) { val fx4 = m + barW * 0.6f * (k + 0.5f) / 3f; poly(c, fx4, xpY + dp(47f) + sin(time * 11f + k * 2.2f) * dp(1.6f), dp(3.6f + 1.2f * sin(time * 13f + k)), 3, -1.5708f, Color.rgb(249, 115, 22)) } // #551 at 50+ the combo bar sprouts flames""")

E("""        if (waveClearT > 0 && particles.size < 350 && Random.nextFloat() < 0.55f) particles.add(Particle(Random.nextFloat() * w, -dp(8f), 0f, 130f, 1.4f, 1.4f, if (Random.nextBoolean()) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), dp(2.6f))) // #493 victory sparkle rain""",
"""        if (waveClearT > 0 && particles.size < 350 && Random.nextFloat() < 0.55f) particles.add(Particle(Random.nextFloat() * w, -dp(8f), 0f, 130f, 1.4f, 1.4f, if (Random.nextBoolean()) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153), dp(2.6f))) // #493 victory sparkle rain
        if (waveClearT > 0 && Random.nextFloat() < 0.10f) sparks.add(Spark(Random.nextFloat() * w * 0.7f, dp(30f) + Random.nextFloat() * dp(60f), 0.5f, Color.rgb(254, 240, 138), dp(9f))) // #552 shooting stars celebrate a cleared wave""")

E("""                4 -> { // Cryo snowflake spokes
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(219, 234, 254)
                    for (k in 0 until 3) { val sa = time * 1.5f + k * 1.047f; c.drawLine(bpx - cos(sa) * dp(8f), bpy - sin(sa) * dp(8f), bpx + cos(sa) * dp(8f), bpy + sin(sa) * dp(8f), p) }
                    p.style = Paint.Style.FILL
                }""",
"""                4 -> { // Cryo snowflake spokes
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(219, 234, 254)
                    for (k in 0 until 3) { val sa = time * 1.5f + k * 1.047f; c.drawLine(bpx - cos(sa) * dp(8f), bpy - sin(sa) * dp(8f), bpx + cos(sa) * dp(8f), bpy + sin(sa) * dp(8f), p) }
                    p.style = Paint.Style.FILL
                    for (k in 0 until 2) { val fa = time * 0.9f + k * 2.4f; poly(c, bpx + cos(fa) * dp(20f), bpy + sin(fa * 1.3f) * dp(16f), dp(2.2f), 4, fa, Color.rgb(219, 234, 254)) } // #553 snow motes drift around Cryo
                }""")

E("""                3 -> poly(c, bpx, bpy - dp(24f) - dp(2f) * sin(time * 12f), dp(6.5f), 3, -1.57f + 0.2f * sin(time * 9f), Color.rgb(249, 115, 22)) // Pyro flame crest""",
"""                3 -> { poly(c, bpx, bpy - dp(24f) - dp(2f) * sin(time * 12f), dp(6.5f), 3, -1.57f + 0.2f * sin(time * 9f), Color.rgb(249, 115, 22)) // Pyro flame crest
                    for (k in 0 until 2) { val ea2 = time * 1.7f + k * 3.1f; p.color = Color.rgb(251, 146, 60); p.alpha = 150; c.drawCircle(bpx + cos(ea2) * dp(19f), bpy - dp(6f) + sin(ea2 * 1.2f) * dp(12f), dp(1.9f), p) }; p.alpha = 255 } // #554 ember motes orbit Pyro""")

E("""                0 -> poly(c, bpx, bpy - dp(13f), dp(4.5f), 4, 0.78f, Color.rgb(254, 240, 138), false) // Guardian crest""",
"""                0 -> { poly(c, bpx, bpy - dp(13f), dp(4.5f), 4, 0.78f, Color.rgb(254, 240, 138), false) // Guardian crest
                    p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.2f); p.color = Color.rgb(254, 240, 138); p.alpha = (60 + 40 * sin(time * 3.4f)).toInt().coerceIn(20, 100); c.drawCircle(bpx, bpy - dp(13f), dp(8f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #555 the crest wears a halo""")

E("""                2 -> { p.color = Color.rgb(20, 8, 4); for (rx in -1..1 step 2) for (ry in -1..1 step 2) c.drawCircle(bpx + rx * dp(13f), bpy + ry * dp(13f), dp(1.9f), p) } // Bulwark rivets""",
"""                2 -> { p.color = Color.rgb(20, 8, 4); for (rx in -1..1 step 2) for (ry in -1..1 step 2) c.drawCircle(bpx + rx * dp(13f), bpy + ry * dp(13f), dp(1.9f), p)
                    p.color = Color.rgb(251, 146, 60); p.alpha = (50 + 35 * sin(time * 2.8f)).toInt().coerceIn(15, 85); for (rx in -1..1 step 2) for (ry in -1..1 step 2) c.drawCircle(bpx + rx * dp(13f), bpy + ry * dp(13f), dp(0.9f), p); p.alpha = 255 } // Bulwark rivets, #556 with a warm core glow""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop5: {len(EDITS)} edits applied")
