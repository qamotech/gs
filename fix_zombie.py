#!/usr/bin/env python3
"""GemStorm v2.1.1 fix — pool-DoT zombie enemies.

Root cause: poison/fire pools in updateFx drain e.hp directly, then try to
register the kill via damageEnemy(e, 0.01f, throwawayList) — but damageEnemy
early-returns on hp <= 0, so the kill NEVER registers. The corpse stays in
`enemies` forever: drawn and moving, but every damage path skips hp <= 0,
and the wave can never clear (wave flow needs enemies.isEmpty()).

Fix: (1) pools just deal their DoT; (2) the death block in updateCombat
sweeps any enemy at hp <= 0 into the death list, so EVERY kill path —
present or future — dies properly with score/loot credit.
"""
BASE = "/home/hatch/workspace/gemstorm/"
GV = BASE + "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
GRADLE = BASE + "app/build.gradle.kts"

def patch(path, pairs):
    src = open(path).read()
    for old, new, cnt in pairs:
        found = src.count(old)
        assert found == cnt, f"{path}: anchor found {found}x, expected {cnt}x:\n{old[:120]}"
        src = src.replace(old, new)
    open(path, "w").write(src)
    print(f"OK {path}: {len(pairs)} edits")

patch(GV, [
    # 1) poison pool: DoT only — death registration lives in the sweep now
    ("""                    if (e.hp > 0 && hyp(e.x - pl.x, e.y - pl.y) < pl.r + e.radius * 0.5f) {
                        e.hp -= 9f * dt
                        if (e.hp <= 0) { val dl = mutableListOf<Enemy>(); damageEnemy(e, 0.01f, dl, noChain = true) }
                    }""",
     """                    // Pool DoT drains hp directly; the death sweep in
                    // updateCombat registers the kill (damageEnemy()
                    // refuses hp <= 0, so it could never do it from here).
                    if (e.hp > 0 && hyp(e.x - pl.x, e.y - pl.y) < pl.r + e.radius * 0.5f)
                        e.hp -= 9f * dt""", 1),

    # 2) fire pool: same cleanup (burn tint kept)
    ("""                    if (e.hp > 0 && hyp(e.x - fp.x, e.y - fp.y) < fp.r + e.radius * 0.5f) {
                        e.burnT = 0.6f
                        e.hp -= 16f * dt
                        if (e.hp <= 0) { val dl = mutableListOf<Enemy>(); damageEnemy(e, 0.01f, dl, noChain = true) }
                    }""",
     """                    if (e.hp > 0 && hyp(e.x - fp.x, e.y - fp.y) < fp.r + e.radius * 0.5f) {
                        e.burnT = 0.6f
                        e.hp -= 16f * dt // death registered by the updateCombat sweep
                    }""", 1),

    # 3) the sweep: no enemy may sit at hp <= 0 unregistered
    ("""        // --- deaths ---
        for (e in dead) {""",
     """        // --- deaths ---
        // Sweep: pool DoT (updateFx) drains hp outside damageEnemy, so a
        // pool-killed enemy can sit at hp <= 0 unregistered. Left alone it
        // becomes an unshootable zombie — bullets skip hp <= 0, yet it
        // still draws, still moves, and the wave can never clear.
        for (e in enemies) if (e.hp <= 0 && !dead.contains(e)) dead.add(e)
        for (e in dead) {""", 1),
])

patch(GRADLE, [
    ("        versionCode = 3\n        versionName = \"2.1.0\"",
     "        versionCode = 4\n        versionName = \"2.1.1\"", 1),
])
print("zombie fix applied")
