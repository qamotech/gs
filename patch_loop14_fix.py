#!/usr/bin/env python3
"""Loop-14 fix: restructure #659/#658 one-line blocks — a `for` statement after
semicolons in these single-line if-blocks does not parse here; use forEach."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
EDITS = []
def E(old, new, count=1): EDITS.append((old, new, count))

E("""        if (victory) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(9f); p.color = Color.rgb(250, 204, 21); p.alpha = 13; for (k in 0 until 6) { val ra2 = k * 1.047f + time * 0.24f; c.drawLine(w / 2, h * 0.30f, w / 2 + cos(ra2) * dp(190f), h * 0.30f + sin(ra2) * dp(190f), p) } p.style = Paint.Style.FILL; p.alpha = 255 } // #659 slow rays turn behind the victory hero""",
"""        if (victory) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(9f); p.color = Color.rgb(250, 204, 21); p.alpha = 13; (0 until 6).forEach { k -> val ra2 = k * 1.047f + time * 0.24f; c.drawLine(w / 2, h * 0.30f, w / 2 + cos(ra2) * dp(190f), h * 0.30f + sin(ra2) * dp(190f), p) }; p.style = Paint.Style.FILL; p.alpha = 255 } // #659 slow rays turn behind the victory hero""")

E("""            p.color = Color.rgb(248, 113, 113); p.alpha = (26 + 18 * sin(time * 2.2f)).toInt().coerceIn(8, 44); for (k in 0 until 5) c.drawCircle(w * (0.2f + k * 0.15f), h * 0.16f, dp(2.6f), p); p.alpha = 255 // #658 ember light pools where the cracks end""",
"""            p.color = Color.rgb(248, 113, 113); p.alpha = (26 + 18 * sin(time * 2.2f)).toInt().coerceIn(8, 44); (0 until 5).forEach { k -> c.drawCircle(w * (0.2f + k * 0.15f), h * 0.16f, dp(2.6f), p) }; p.alpha = 255 // #658 ember light pools where the cracks end""")

for i, (old, new, count) in enumerate(EDITS, 1):
    n = src.count(old)
    if n != count:
        print(f"EDIT {i}: anchor found {n}x, expected {count} — ABORT"); sys.exit(1)
    src = src.replace(old, new)
io.open(PATH, "w", encoding="utf-8").write(src)
print(f"OK loop14 fix: {len(EDITS)} edits applied")
