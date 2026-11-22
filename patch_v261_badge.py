#!/usr/bin/env python3
"""GemStorm v2.6.1 — remove the level badge from the hero (Nathan, 2026-11-22).
The number was #34's purple badge pinned to the unit (+ #430 gold ring, #593 burn ring).
Level stays designated in the HUD ("LV n" beside the XP bar)."""
import re

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read(); orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

BADGE = """            // #34 level badge
            p.color = Color.rgb(124, 58, 237)
            c.drawCircle(px + dp(17f), py + dp(15f), dp(9f), p)
            if (level >= 10) { // #430 gold ring for double-digit captains
                p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.6f); p.color = Color.rgb(250, 204, 21)
                c.drawCircle(px + dp(17f), py + dp(15f), dp(10f), p); p.style = Paint.Style.FILL
            }
            if (xpNext > 0 && xp.toFloat() / xpNext > 0.9f) { p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.8f); p.color = Color.rgb(250, 204, 21); p.alpha = (120 + 100 * sin(time * 7f)).toInt().coerceIn(20, 220); c.drawCircle(px + dp(17f), py + dp(15f), dp(11.5f), p); p.style = Paint.Style.FILL; p.alpha = 255 } // #593 your level badge burns gold near a level-up
            txt(c, "$level", px + dp(17f), py + dp(18.5f), dp(10f), Color.WHITE, true)
"""
rep(BADGE,
    "            // v2.6.1: the #34 level badge is OFF the hero (Nathan) — level is designated in the HUD beside the XP bar; #430/#593 badge rings went with it\n",
    "badge removed")

rep("v2.6 — 800-upgrade edition", "v2.6.1 — 800-upgrade edition", "footer")
assert s != orig
open(P, "w").write(s)

P2 = "app/build.gradle.kts"
s = open(P2).read()
assert s.count("versionCode = 10") == 1 and s.count('versionName = "2.6.0"') == 1
open(P2, "w").write(s.replace("versionCode = 10", "versionCode = 11")
                     .replace('versionName = "2.6.0"', 'versionName = "2.6.1"'))
print("ok version 2.6.1 vc11")

# UPGRADES.md: annotate the three badge entries (keep the checkmarks / 800 count)
P3 = "UPGRADES.md"
lines = open(P3).read().split("\n")
notes = {
    "#34**": " — RETIRED in v2.6.1 (Nathan): badge off the hero; level is designated in the HUD beside the XP bar",
    "#430**": " — retired with the badge in v2.6.1",
    "#593**": " — retired with the badge in v2.6.1",
}
hit = 0
for i, ln in enumerate(lines):
    for key, note in notes.items():
        if ln.startswith("- ✅ **" + key) and note not in ln:
            lines[i] = ln + note; hit += 1
assert hit == 3, f"UPGRADES notes hit {hit} != 3"
open(P3, "w").write("\n".join(lines))
print("ok UPGRADES.md notes (3)")
print("patch_v261_badge applied")
