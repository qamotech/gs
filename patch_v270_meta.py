#!/usr/bin/env python3
"""GemStorm v2.7.0 meta: version bump + UPGRADES.md entries #801-#848."""
P = "app/build.gradle.kts"
s = open(P).read()
assert s.count("versionCode = 12") == 1 and s.count('versionName = "2.6.2"') == 1
open(P, "w").write(s.replace("versionCode = 12", "versionCode = 13")
                     .replace('versionName = "2.6.2"', 'versionName = "2.7.0"'))
print("ok version 2.7.0 vc13")

P2 = "app/src/main/java/com/qamotech/gemstorm/GameView.kt"
s = open(P2).read()
assert s.count("v2.6.2 — 800-upgrade edition") == 1
s = s.replace("v2.6.2 — 800-upgrade edition", "v2.7 — 848-upgrade edition")
assert s.count("(800-upgrade edition)") == 1
s = s.replace("(800-upgrade edition)", "(848-upgrade edition)")
assert s.count("800 upgrades across every system") == 1
s = s.replace("800 upgrades across every system", "848 upgrades across every system")
open(P2, "w").write(s)
print("ok footer/about/whatsnew")

entries = """
- ✅ **#801** A glint rides the hero's shell rim
- ✅ **#802** A hero-colored mote counter-orbits the glint
- ✅ **#803** Manual-aim guide dots — your line of fire, faintly dotted
- ✅ **#804** A heartbeat ring throbs when your HP is critical
- ✅ **#805** A grounding shadow pools under a dashing hero
- ✅ **#806** A third, smaller star spins against the muzzle flash
- ✅ **#807** A third, fainter echo rides inside the dash ring
- ✅ **#808** The XP tip swells gold when a level is about to pop
- ✅ **#809** A thin rule underlines your level
- ✅ **#810** The score wears a soft drop shadow
- ✅ **#811** A hairline sweeps under the wave title
- ✅ **#812** A white counter-ring shivers inside the level-up halo
- ✅ **#813** A rare shooting star rakes across the nebula
- ✅ **#814** A bright heart beats at the nebula's core
- ✅ **#815** A soft red under-glow pools beneath every heart drop
- ✅ **#816** The rocket's nose cone pulses hot
- ✅ **#817** An ember rim ring breathes at the fire pool's edge
- ✅ **#818** A heat-shimmer halo hangs over the fire pool
- ✅ **#819** A second, wider halo echoes the Warden's off-screen arrow
- ✅ **#820** A sparkle spins at the Gnome arrow's heart
- ✅ **#821** The Warden's arrival blooms pink across the arena
- ✅ **#822** A small gem pulses above the menu footer
- ✅ **#823** A violet hairline rings the pause button's face
- ✅ **#824** A hot spark spins beside the combo counter
- ✅ **#825** Frame safety net: a bad tick frame is skipped, never fatal
- ✅ **#826** Invalidate is guarded the same way — the loop always re-posts
- ✅ **#827** The render pass runs behind a guarded onDraw
- ✅ **#828** The projectile loop iterates a snapshot — mid-pass spawns can't corrupt it
- ✅ **#829** Explosions iterate a snapshot of the enemy list
- ✅ **#830** The contact pass iterates a snapshot too
- ✅ **#831** The death sweep iterates a snapshot
- ✅ **#832** A restored XP curve can never be zero (no infinite gain loop)
- ✅ **#833** Corrupt run snapshots degrade to zeros instead of crashing Continue
- ✅ **#834** Explosion volume math can never divide by a zero width
- ✅ **#835** Degenerate zero-size layout passes are ignored
- ✅ **#836** Ticks wait for a real layout before simulating
- ✅ **#837** Particles hard-capped at 420, oldest dropped first
- ✅ **#838** Floating texts hard-capped at 40
- ✅ **#839** Rings hard-capped at 24
- ✅ **#840** Blooms hard-capped at 12
- ✅ **#841** Decals hard-capped at 30
- ✅ **#842** Sparks hard-capped at 60
- ✅ **#843** Fire pools hard-capped at 42 on every path
- ✅ **#844** Falling shards hard-capped at 40
- ✅ **#845** Gem drops hard-capped at 140
- ✅ **#846** Projectiles hard-capped at 300 — rockets are never trimmed
- ✅ **#847** Settings sliders/scroll guard against zero-pointer touch events
- ✅ **#848** Audio release is idempotent; SFX never plays into a released pool
"""
with open("UPGRADES.md", "a") as f:
    f.write(entries)
print("ok UPGRADES.md +48")
print("patch_v270_meta applied")
