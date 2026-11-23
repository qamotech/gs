#!/usr/bin/env python3
"""GemStorm v2.7.0 stability batch, part 2 (#828-#848) — part 1 (#825-#827) already applied."""

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read(); orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

rep("""        for (pr in projs) {
            pr.life -= dt""",
"""        for (pr in ArrayList(projs)) { // #828 v2.7: snapshot — projectiles spawned mid-pass (bursts, deaths) can never corrupt the loop
            pr.life -= dt""",
    "#828 proj snapshot")
rep("        for (e in enemies) if (e.hp > 0 && hyp(e.x - pr.x, e.y - pr.y) < pr.aoe + e.radius * 0.5f) damageEnemy(e, pr.dmg * 0.8f, dead, pr.x, pr.y)",
    "        for (e in ArrayList(enemies)) if (e.hp > 0 && hyp(e.x - pr.x, e.y - pr.y) < pr.aoe + e.radius * 0.5f) damageEnemy(e, pr.dmg * 0.8f, dead, pr.x, pr.y) // #829 v2.7: snapshot — deaths during the blast mutate the real list, not this pass",
    "#829 explode snapshot")
rep("""                for (e in enemies) {
                    if (e.hp > 0 && hyp(pr.x - e.x, pr.y - e.y) < e.radius + pr.radius) {""",
"""                for (e in ArrayList(enemies)) { // #830 v2.7: snapshot for the contact pass
                    if (e.hp > 0 && hyp(pr.x - e.x, pr.y - e.y) < e.radius + pr.radius) {""",
    "#830 contact snapshot")
rep("        for (e in enemies) if (e.hp <= 0 && !dead.contains(e)) dead.add(e)",
    "        for (e in ArrayList(enemies)) if (e.hp <= 0 && !dead.contains(e)) dead.add(e) // #831 v2.7: snapshot for the death sweep",
    "#831 sweep snapshot")
rep("        level = n(); xp = n(); xpNext = n()",
    "        level = n(); xp = n(); xpNext = n()\n        if (xpNext < 1) xpNext = 21 // #832 v2.7: a corrupt snapshot can never zero the XP curve (the gain loop would spin forever)",
    "#832 xpNext guard")
rep("""        fun f() = parts[i++].toFloat()
        fun n() = parts[i++].toInt()""",
"""        fun f(): Float { if (i >= parts.size) return 0f; return parts[i++].toFloatOrNull() ?: 0f } // #833 v2.7: corrupt snapshot values degrade to 0 instead of crashing Continue
        fun n(): Int { if (i >= parts.size) return 0; return parts[i++].toIntOrNull() ?: 0 } // #833 v2.7""",
    "#833 parse guards")
rep("private fun distVol(x: Float, y: Float): Float = 1f - min(1f, hyp(x - px, y - py) / (w * 0.8f)) * 0.65f // #281 distance-based explosion volume",
    "private fun distVol(x: Float, y: Float): Float = 1f - min(1f, hyp(x - px, y - py) / (w * 0.8f).coerceAtLeast(1f)) * 0.65f // #281 + #834 v2.7: never divide by a zero width before layout",
    "#834 distVol guard")
rep("        w = nw.toFloat(); h = nh.toFloat()",
    "        if (nw <= 0 || nh <= 0) return // #835 v2.7: ignore degenerate layout passes\n        w = nw.toFloat(); h = nh.toFloat()",
    "#835 size guard")
rep("""    private fun tick(dt: Float) {
        time += dt""",
"""    private fun tick(dt: Float) {
        if (w <= 0f || h <= 0f) return // #836 v2.7: no layout yet — nothing sane to simulate this frame
        time += dt""",
    "#836 tick guard")
rep("""        fallShards.removeAll { it.y > h + dp(30f) }
    }""",
"""        fallShards.removeAll { it.y > h + dp(30f) }
        trimFx()
    }

    // #837-#846 v2.7: every FX list is hard-capped, oldest dropped first — late-game density can never balloon memory or frame time into a crash
    private fun trimFx() {
        while (particles.size > 420) particles.removeAt(0) // #837
        while (texts.size > 40) texts.removeAt(0) // #838
        while (rings.size > 24) rings.removeAt(0) // #839
        while (blooms.size > 12) blooms.removeAt(0) // #840
        while (decals.size > 30) decals.removeAt(0) // #841
        while (sparks.size > 60) sparks.removeAt(0) // #842
        while (firePools.size > 42) firePools.removeAt(0) // #843
        while (fallShards.size > 40) fallShards.removeAt(0) // #844
        while (gemDrops.size > 140) gemDrops.removeAt(0) // #845
        while (projs.size > 300) { val idx = projs.indexOfFirst { !it.rocket }; if (idx < 0) break; projs.removeAt(idx) } // #846 rockets are never trimmed
    }""",
    "#837-846 trimFx")
rep("                    if (sliderDrag != null) adjustSlider(e.getX(0), e.getY(0))",
    "                    if (sliderDrag != null && e.pointerCount > 0) adjustSlider(e.getX(0), e.getY(0)) // #847 v2.7: a dying multi-touch gesture can report zero pointers",
    "#847 slider guard")
rep("                    else if (scrollDragY >= 0) settingsScroll = (scrollDragStart + (scrollDragY - e.getY(0))).coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381",
    "                    else if (scrollDragY >= 0 && e.pointerCount > 0) settingsScroll = (scrollDragStart + (scrollDragY - e.getY(0))).coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381 + #847 v2.7",
    "#847 scroll guard")
assert s != orig
open(P, "w").write(s)

P2 = "app/src/main/java/com/qamotech/gemstorm/AudioManager.kt"
s = open(P2).read(); orig2 = s
old = """    fun release() {
        stopMusic()
        soundPool.release()
    }"""
assert s.count(old) == 1, "release anchor"
s = s.replace(old, """    private var released = false // #848 v2.7

    fun release() { // #848 v2.7: idempotent — a double onDestroy can never double-release the pool
        if (released) return
        released = true
        stopMusic()
        try { soundPool.release() } catch (_: Exception) { }
    }""", 1)
old2 = """    fun sfx(name: String, rate: Float = 1.0f, volMul: Float = 1.0f) {
        if (save.muteAll) return"""
assert s.count(old2) == 1, "sfx anchor"
s = s.replace(old2, """    fun sfx(name: String, rate: Float = 1.0f, volMul: Float = 1.0f) {
        if (released) return // #848 v2.7: never play into a released SoundPool (throws on some devices)
        if (save.muteAll) return""", 1)
assert s != orig2
open(P2, "w").write(s)
print("ok #848 audio hardening")
print("patch_v270_stab2 applied")
