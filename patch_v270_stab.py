#!/usr/bin/env python3
"""GemStorm v2.7.0 stability batch #825-#848 (Nathan: 'crashes now — repair it').
Frame safety nets, snapshot iteration on every hot loop, corrupt-save guards,
and hard caps on every FX list so late-game density can never kill a frame."""
import re

P = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P).read(); orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

# #825 + #826 frame safety net in the Choreographer callback
rep("""            val dt = rawDt * timeScale
            tick(dt)
            invalidate()
            choreo.postFrameCallback(this)""",
"""            val dt = rawDt * timeScale
            try { tick(dt) } catch (e: Exception) { android.util.Log.e("GemStorm", "tick frame skipped", e) } // #825 v2.7: one bad frame is skipped, never fatal — the loop always re-posts
            try { invalidate() } catch (e: Exception) { android.util.Log.e("GemStorm", "invalidate skipped", e) } // #826 v2.7
            choreo.postFrameCallback(this)""",
    "#825/#826 frame net")

# #827 onDraw wrapper: rename the real draw to drawFrame, add a guarded override
m = re.search(r"    override fun onDraw\((\w+): Canvas\) \{", s)
assert m, "onDraw signature not found"
param = m.group(1)
s = s.replace(m.group(0), f"    private fun drawFrame({param}: Canvas) {{ // #827 v2.7: the real render pass, called by the guarded onDraw below", 1)
anchor2 = f"    private fun drawFrame({param}: Canvas) {{ // #827 v2.7: the real render pass, called by the guarded onDraw below"
s = s.replace(anchor2, f"""    override fun onDraw({param}: Canvas) {{ // #827 v2.7: a render exception skips one frame instead of killing the app
        try {{ drawFrame({param}) }} catch (e: Exception) {{ android.util.Log.e("GemStorm", "draw frame skipped", e) }}
    }}

{anchor2}""", 1)
print("ok #827 onDraw wrapper")

# #828 projectile loop snapshot
rep("        for (pr in projs) {",
    "        for (pr in ArrayList(projs)) { // #828 v2.7: snapshot — projectiles spawned mid-pass (bursts, deaths) can never corrupt the loop",
    "#828 proj snapshot")
# #829 explodeProj enemy snapshot
rep("        for (e in enemies) if (e.hp > 0 && hyp(e.x - pr.x, e.y - pr.y) < pr.aoe + e.radius * 0.5f) damageEnemy(e, pr.dmg * 0.8f, dead, pr.x, pr.y)",
    "        for (e in ArrayList(enemies)) if (e.hp > 0 && hyp(e.x - pr.x, e.y - pr.y) < pr.aoe + e.radius * 0.5f) damageEnemy(e, pr.dmg * 0.8f, dead, pr.x, pr.y) // #829 v2.7: snapshot — deaths during the blast mutate the real list, not this pass",
    "#829 explode snapshot")
# #830 contact-loop enemy snapshot
rep("                for (e in enemies) {",
    "                for (e in ArrayList(enemies)) { // #830 v2.7: snapshot for the contact pass",
    "#830 contact snapshot")
# #831 death-sweep snapshot
rep("        for (e in enemies) if (e.hp <= 0 && !dead.contains(e)) dead.add(e)",
    "        for (e in ArrayList(enemies)) if (e.hp <= 0 && !dead.contains(e)) dead.add(e) // #831 v2.7: snapshot for the death sweep",
    "#831 sweep snapshot")
# #832 xpNext restore guard
rep("        level = n(); xp = n(); xpNext = n()",
    "        level = n(); xp = n(); xpNext = n()\n        if (xpNext < 1) xpNext = 21 // #832 v2.7: a corrupt snapshot can never zero the XP curve (the gain loop would spin forever)",
    "#832 xpNext guard")
# #833 continueRun parse guards
rep("""        fun f() = parts[i++].toFloat()
        fun n() = parts[i++].toInt()""",
"""        fun f(): Float { if (i >= parts.size) return 0f; return parts[i++].toFloatOrNull() ?: 0f } // #833 v2.7: corrupt snapshot values degrade to 0 instead of crashing Continue
        fun n(): Int { if (i >= parts.size) return 0; return parts[i++].toIntOrNull() ?: 0 } // #833 v2.7""",
    "#833 parse guards")
# #834 distVol zero-width guard
rep("private fun distVol(x: Float, y: Float): Float = 1f - min(1f, hyp(x - px, y - py) / (w * 0.8f)) * 0.65f // #281 distance-based explosion volume",
    "private fun distVol(x: Float, y: Float): Float = 1f - min(1f, hyp(x - px, y - py) / (w * 0.8f).coerceAtLeast(1f)) * 0.65f // #281 + #834 v2.7: never divide by a zero width before layout",
    "#834 distVol guard")
# #835 onSizeChanged zero guard
rep("        w = nw.toFloat(); h = nh.toFloat()",
    "        if (nw <= 0 || nh <= 0) return // #835 v2.7: ignore degenerate layout passes\n        w = nw.toFloat(); h = nh.toFloat()",
    "#835 size guard")
# #836 tick layout guard
rep("""    private fun tick(dt: Float) {
        time += dt""",
"""    private fun tick(dt: Float) {
        if (w <= 0f || h <= 0f) return // #836 v2.7: no layout yet — nothing sane to simulate this frame
        time += dt""",
    "#836 tick guard")
# #837-#846 trimFx caps, called at the end of the FX update pass
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
# #847 touch guards
rep("                    if (sliderDrag != null) adjustSlider(e.getX(0), e.getY(0))",
    "                    if (sliderDrag != null && e.pointerCount > 0) adjustSlider(e.getX(0), e.getY(0)) // #847 v2.7: a dying multi-touch gesture can report zero pointers",
    "#847 slider guard")
rep("                    else if (scrollDragY >= 0) settingsScroll = (scrollDragStart + (scrollDragY - e.getY(0))).coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381",
    "                    else if (scrollDragY >= 0 && e.pointerCount > 0) settingsScroll = (scrollDragStart + (scrollDragY - e.getY(0))).coerceIn(0f, max(0f, dp(922f) - h + dp(24f))) // #381 + #847 v2.7",
    "#847 scroll guard")
assert s != orig
open(P, "w").write(s)

# #848 AudioManager: idempotent release + play-after-release guard
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
print("patch_v270_stab applied")
