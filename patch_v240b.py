#!/usr/bin/env python3
"""GemStorm v2.4.0 addendum (Nathan): kill the two pickup text-hearts, ONE animated red heart instead."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""    private var heartBeatT = 0f""",
"""    private var heartBeatT = 0f
    private var heartPopT = 0f // v2.4: the one animated pickup heart""")
E("""                texts.add(FloatText(px, py - dp(40f), "+15 HP ❤️", 0.8f, Color.rgb(248, 113, 113), dp(15f))) // #482 red to match the heart""",
"""                texts.add(FloatText(px, py - dp(40f), "+15 HP", 0.8f, Color.rgb(248, 113, 113), dp(15f))) // v2.4: no text hearts — the animated heart below says it
                heartPopT = 0.9f""")
E("""                texts.add(FloatText(px, py - dp(58f), "❤️", 0.9f, Color.WHITE, dp(18f))) // #168 rising heart
""",
"""""")
E("""            heartBeatT -= dt""",
"""            heartBeatT -= dt
            if (heartPopT > 0) heartPopT -= dt""")
E("""        // #187 elite spawn beams of light""",
"""        if (heartPopT > 0) { // ❤️ ONE red animated heart on pickup (Nathan v2.4 — replaces the two text hearts)
            val hf = (heartPopT / 0.9f).coerceIn(0f, 1f)
            val hr = dp(13f) * (1.35f - 0.35f * hf) * (1f + 0.16f * sin(time * 16f)) // heartbeat pulse as it settles
            val hx3 = px; val hy3 = py - dp(48f) - dp(16f) * (1f - hf)
            p.color = Color.rgb(239, 68, 68); p.alpha = (255 * min(1f, hf * 2.5f)).toInt().coerceIn(0, 255)
            c.drawCircle(hx3 - hr * 0.45f, hy3 - hr * 0.25f, hr * 0.55f, p)
            c.drawCircle(hx3 + hr * 0.45f, hy3 - hr * 0.25f, hr * 0.55f, p)
            poly(c, hx3, hy3 + hr * 0.62f, hr * 0.78f, 3, 3.14159f, Color.rgb(239, 68, 68))
            poly(c, hx3, hy3, hr * 0.5f, 4, 0.78f, Color.rgb(252, 165, 165))
            p.alpha = 255
        }

        // #187 elite spawn beams of light""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK v2.4.0b: {len(EDITS)} edits applied")
