#!/usr/bin/env python3
"""Loop-10 fix: #548's poly() leaves the shared paint WHITE at full alpha, so the
#425 nebula core circle right after it drew as a solid white disc (the wandering
'moon' in cycle 9/10 verification shots). Restore color+alpha before the core."""
import io, sys

PATH = "/home/hatch/workspace/gemstorm/app/src/main/java/com/qamotech/gemstorm/GameView.kt"
src = io.open(PATH, encoding="utf-8").read()
OLD = """            if (sin(time * 1.9f + nx * 0.021f + ny * 0.013f) > 0.93f) poly(c, nx, ny, dp(7f), 4, time * 1.2f, Color.WHITE) // #548 nebula cores twinkle
            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core"""
NEW = """            if (sin(time * 1.9f + nx * 0.021f + ny * 0.013f) > 0.93f) poly(c, nx, ny, dp(7f), 4, time * 1.2f, Color.WHITE) // #548 nebula cores twinkle
            p.color = ncol; p.alpha = (12 * np2).toInt() // loop-10 fix: poly() leaves the pen white — restore it before the core (was drawing a solid white disc)
            c.drawCircle(nx, ny, dp(58f), p) // #425 bright nebula core"""
n = src.count(OLD)
if n != 1:
    print(f"anchor found {n}x, expected 1 — ABORT"); sys.exit(1)
src = src.replace(OLD, NEW)
io.open(PATH, "w", encoding="utf-8").write(src)
print("OK loop10 fix applied")
