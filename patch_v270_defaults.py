#!/usr/bin/env python3
"""GemStorm v2.7.0 defaults (Nathan, 2026-11-22): harder game by default —
manual aim ON, aim assist OFF, hard mode ON. New saves get the new defaults;
existing saves get a one-time migration (Settings can still change them after)."""

P = "app/src/main/java/com/qamotech/gemstorm/SaveData.kt"
s = open(P).read(); orig = s

def rep(old, new, tag):
    global s
    n = s.count(old)
    assert n == 1, f"{tag}: anchor count {n} != 1"
    s = s.replace(old, new, 1)
    print(f"ok {tag}")

rep('get() = prefs.getBoolean("manualAim", false)',
    'get() = prefs.getBoolean("manualAim", true) // v2.7 (Nathan): manual aim is the default — you aim your own shots',
    "manualAim default")
rep('get() = prefs.getInt("aimAssist", 1)',
    'get() = prefs.getInt("aimAssist", 0) // v2.7 (Nathan): no snap assist by default — pure manual aim',
    "aimAssist default")
rep("""    var hardMode: Boolean
        get() = prefs.getBoolean("hardMode", false)
        set(v) = prefs.edit().putBoolean("hardMode", v).apply()""",
"""    var hardMode: Boolean
        get() = prefs.getBoolean("hardMode", true) // v2.7 (Nathan): hard mode is the default difficulty
        set(v) = prefs.edit().putBoolean("hardMode", v).apply()

    /** v2.7 one-time defaults migration flag (existing saves get the harder defaults once). */
    var defaultsV27: Boolean
        get() = prefs.getBoolean("defaultsV27", false)
        set(v) = prefs.edit().putBoolean("defaultsV27", v).apply()""",
    "hardMode default + migration flag")
assert s != orig
open(P, "w").write(s)

P2 = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P2).read(); orig = s
old = '        if (!save.ownsChar(5)) save.unlockChar(5) // 💎N🎱 turtle: free for everyone — granted on first v2.3 launch'
assert s.count(old) == 1
s = s.replace(old, old + '\n        if (!save.defaultsV27) { save.manualAim = true; save.aimAssist = 0; save.hardMode = true; save.defaultsV27 = true } // v2.7 (Nathan): existing players get the harder defaults once — manual aim, no assist, hard mode', 1)
assert s != orig
open(P2, "w").write(s)
print("ok migration in init")
print("patch_v270_defaults applied")
