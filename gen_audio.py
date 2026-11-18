#!/usr/bin/env python3
"""GEMSTORM audio composer — synthesizes real music loops + SFX as WAVs.
Battle: 126 BPM synthwave in A minor (Am F C G), kick/hats/bass/arp/pad.
Menu: 84 BPM ambient pads + soft arp. SFX: synthesized one-shots."""
import math, os, random, struct, wave

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/res/raw")
os.makedirs(OUT, exist_ok=True)
random.seed(42)

def save(name, samples):
    samples = [max(-1.0, min(1.0, s)) for s in samples]
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in samples))
    print(name, f"{len(samples)/SR:.2f}s", f"{os.path.getsize(path)//1024}KB")

def note(midi): return 440.0 * 2 ** ((midi - 69) / 12.0)

def add(dst, src, start, gain=1.0):
    i0 = int(start * SR)
    for i, s in enumerate(src):
        j = i0 + i
        if 0 <= j < len(dst): dst[j] += s * gain

def tone(freq, dur, kind="sine", attack=0.005, decay=None, vol=1.0, slide_to=None, vibrato=0.0):
    n = int(dur * SR); out = []
    decay = decay if decay is not None else dur
    for i in range(n):
        t = i / SR
        f = freq if slide_to is None else freq + (slide_to - freq) * (i / max(1, n - 1))
        if vibrato: f *= 1.0 + 0.02 * math.sin(2 * math.pi * vibrato * t)
        ph = 2 * math.pi * f * t
        if kind == "sine": v = math.sin(ph)
        elif kind == "saw": v = 2 * ((f * t) % 1.0) - 1.0
        elif kind == "square": v = 1.0 if math.sin(ph) >= 0 else -1.0
        elif kind == "rich":  # saw-ish with harmonics, softer
            v = (math.sin(ph) + 0.5 * math.sin(2 * ph) + 0.25 * math.sin(3 * ph) + 0.12 * math.sin(4 * ph)) / 1.87
        else: v = math.sin(ph)
        env = min(1.0, t / attack) * math.exp(-3.0 * t / decay) if decay < dur else min(1.0, t / attack) * (1.0 - 0.3 * t / dur)
        out.append(v * env * vol)
    return out

def noise(dur, vol=1.0, decay=None, smooth=1):
    n = int(dur * SR); decay = decay or dur; out = []; last = 0.0
    for i in range(n):
        t = i / SR
        white = random.uniform(-1, 1)
        last = (last + white) / (smooth + 1) if smooth > 1 else white
        v = last if smooth > 1 else white
        out.append(v * math.exp(-3.0 * t / decay) * vol)
    return out

def kick(dur=0.24): return tone(120, dur, "sine", 0.002, 0.22, 1.0, slide_to=38)
def hat(dur=0.05, vol=0.35): return noise(dur, vol, dur, smooth=1)
def snare(dur=0.16): return [a + b for a, b in zip(noise(dur, 0.5, dur, smooth=2), tone(190, dur, "sine", 0.002, 0.1, 0.3))]

# ---------------- BATTLE LOOP: 126 BPM, 8 bars, Am F C G (2 bars each) --------
BPM = 126.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
prog = [  # (root midi, chord midis) per 2-bar block
    (33, [57, 60, 64]),  # Am  (A1 bass, A3 C4 E4)
    (29, [53, 57, 60]),  # F
    (36, [48, 52, 55]),  # C
    (31, [55, 59, 62]),  # G
]
for bar in range(BARS):
    root, chord = prog[bar // 2]
    t0 = bar * BAR
    for b in range(4):  # drums
        add(buf, kick(), t0 + b * BEAT, 0.85)
        add(buf, hat(), t0 + b * BEAT + BEAT / 2, 0.5)
        if b in (1, 3): add(buf, snare(), t0 + b * BEAT, 0.55)
    for s in range(8):  # bass 8ths, root + octave bounce
        f = note(root + 12) if s % 4 == 2 else note(root)
        add(buf, tone(f, BEAT / 2, "rich", 0.004, BEAT / 2, 0.5), t0 + s * BEAT / 2, 1.0)
    arp_notes = chord + [chord[0] + 12, chord[2] + 12]
    for s in range(16):  # arp 16ths
        m = arp_notes[[0, 1, 2, 3, 2, 1][s % 6]] + 12
        add(buf, tone(note(m), BEAT / 4, "rich", 0.003, BEAT / 4, 0.20), t0 + s * BEAT / 4, 1.0)
    for m in chord:  # pad swell per bar
        add(buf, tone(note(m + 12), BAR, "sine", BAR * 0.3, BAR * 1.4, 0.10), t0, 1.0)
# gentle limiter fade at loop seam
seam = int(0.02 * SR)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_battle.wav", [s * 0.8 for s in buf])

# ---------------- MENU LOOP: 84 BPM pads + soft arp --------------------------
BPM = 84.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
prog = [
    (45, [57, 60, 64, 71]),  # Am9
    (41, [53, 57, 60, 64]),  # Fmaj7
    (48, [48, 52, 55, 59]),  # Cmaj9
    (43, [55, 59, 62, 65]),  # G6
]
for bar in range(BARS):
    root, chord = prog[bar // 2]
    t0 = bar * BAR
    for m in chord:
        add(buf, tone(note(m), BAR * 1.05, "sine", BAR * 0.35, BAR * 1.6, 0.13), t0, 1.0)
        add(buf, tone(note(m), BAR * 1.05, "rich", BAR * 0.4, BAR * 1.6, 0.035), t0, 1.0)
    add(buf, tone(note(root - 12), BAR, "sine", BAR * 0.25, BAR * 1.5, 0.16), t0, 1.0)
    seq = [chord[0] + 12, chord[2] + 12, chord[1] + 12, chord[3] + 12, chord[2] + 12, chord[1] + 12, chord[0] + 12, chord[2] + 12]
    for s in range(8):
        add(buf, tone(note(seq[s]), BEAT, "sine", 0.01, BEAT * 0.95, 0.10), t0 + s * BEAT, 1.0)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_menu.wav", [s * 0.85 for s in buf])

# ---------------- SFX ---------------------------------------------------------
save("sfx_shoot.wav", tone(920, 0.09, "square", 0.002, 0.09, 0.28, slide_to=340))
save("sfx_hit.wav", [a + b for a, b in zip(noise(0.07, 0.30, 0.07, smooth=2), tone(220, 0.07, "sine", 0.002, 0.07, 0.25, slide_to=110))])
save("sfx_explosion.wav", [a + b for a, b in zip(noise(0.45, 0.8, 0.45, smooth=6), tone(90, 0.45, "sine", 0.002, 0.4, 0.6, slide_to=30))])
pk = [0.0] * int(0.14 * SR); add(pk, tone(1318, 0.07, "sine", 0.002, 0.07, 0.4), 0); add(pk, tone(1760, 0.09, "sine", 0.002, 0.09, 0.4), 0.05)
save("sfx_pickup.wav", pk)
lu = [0.0] * int(0.55 * SR)
for i, m in enumerate([60, 64, 67, 72, 76]): add(lu, tone(note(m), 0.22, "rich", 0.004, 0.22, 0.32), i * 0.07)
save("sfx_levelup.wav", lu)
save("sfx_dash.wav", noise(0.18, 0.4, 0.18, smooth=3))
save("sfx_hurt.wav", tone(300, 0.25, "saw", 0.002, 0.25, 0.35, slide_to=85))
save("sfx_click.wav", tone(1250, 0.045, "sine", 0.002, 0.045, 0.35))
vic = [0.0] * int(1.3 * SR)
for i, m in enumerate([60, 64, 67, 72, 67, 72, 76, 79]): add(vic, tone(note(m), 0.3, "rich", 0.004, 0.3, 0.34), i * 0.13)
save("sfx_victory.wav", vic)
go = [0.0] * int(1.3 * SR)
for i, m in enumerate([57, 55, 53, 52, 45]): add(go, tone(note(m), 0.4, "rich", 0.005, 0.4, 0.34), i * 0.22)
save("sfx_gameover.wav", go)
save("sfx_boss.wav", tone(65, 0.7, "saw", 0.01, 0.7, 0.4, slide_to=98, vibrato=9.0))

# ---------------- BATCH K SFX (#251-#272) --------------------------------------
def bufcat(dur, parts):
    b = [0.0] * int(dur * SR)
    for src, start, gain in parts: add(b, src, start, gain)
    return b

# #251 tesla zap: buzz-crack arc
save("sfx_zap.wav", bufcat(0.22, [
    (tone(1400, 0.16, "saw", 0.002, 0.16, 0.16, slide_to=300), 0, 1.0),
    (noise(0.16, 0.20, 0.16, smooth=1), 0, 1.0),
    (tone(240, 0.20, "square", 0.002, 0.20, 0.10, slide_to=70), 0, 1.0)]))
# #252 meteor whistle + boom
save("sfx_meteor.wav", bufcat(0.95, [
    (tone(1600, 0.55, "sine", 0.01, 0.55, 0.28, slide_to=300), 0, 1.0),
    (noise(0.45, 0.75, 0.45, smooth=6), 0.5, 1.0),
    (tone(85, 0.45, "sine", 0.002, 0.42, 0.6, slide_to=28), 0.5, 1.0)]))
# #253 poison bubble burst
save("sfx_poison.wav", bufcat(0.20, [
    (tone(480, 0.07, "sine", 0.002, 0.07, 0.35, slide_to=920), 0, 1.0),
    (noise(0.13, 0.28, 0.13, smooth=4), 0.03, 1.0),
    (tone(300, 0.16, "sine", 0.002, 0.16, 0.22, slide_to=85), 0.04, 1.0)]))
# #254 chain sizzle: hiss + crackle pops
sizzle = [0.0] * int(0.30 * SR)
add(sizzle, noise(0.30, 0.26, 0.30, smooth=1), 0)
for i in range(7):
    add(sizzle, tone(1800 + random.random() * 1600, 0.02, "square", 0.001, 0.02, 0.10), random.random() * 0.26)
save("sfx_sizzle.wav", sizzle)
# #255 shield hit: metallic ping (inharmonic partials)
save("sfx_shieldhit.wav", bufcat(0.30, [
    (tone(1244, 0.28, "sine", 0.001, 0.28, 0.30), 0, 1.0),
    (tone(1866, 0.20, "sine", 0.001, 0.20, 0.20), 0, 1.0),
    (tone(2489, 0.14, "sine", 0.001, 0.14, 0.12), 0, 1.0)]))
# #256 shield break: glass ring cluster
save("sfx_shieldbreak.wav", bufcat(0.40, [
    (tone(2093, 0.30, "sine", 0.001, 0.30, 0.22), 0, 1.0),
    (tone(2736, 0.24, "sine", 0.001, 0.24, 0.18), 0.01, 1.0),
    (tone(3322, 0.18, "sine", 0.001, 0.18, 0.14), 0.02, 1.0),
    (noise(0.30, 0.22, 0.30, smooth=1), 0, 1.0),
    (tone(900, 0.36, "rich", 0.002, 0.36, 0.14, slide_to=190), 0, 1.0)]))
# #257 revive phoenix chime: rising major arp + shimmer
rev = [0.0] * int(0.85 * SR)
for i, m in enumerate([64, 68, 71, 76, 83]):
    add(rev, tone(note(m), 0.5, "rich", 0.004, 0.5, 0.30), i * 0.08)
add(rev, tone(note(88), 0.6, "sine", 0.01, 0.6, 0.10), 0.32)
save("sfx_revive.wav", rev)
# #258 elite spawn horn: low brass swell
save("sfx_horn.wav", bufcat(0.55, [
    (tone(note(41), 0.52, "saw", 0.06, 0.52, 0.26, slide_to=note(42)), 0, 1.0),
    (tone(note(48), 0.50, "saw", 0.06, 0.50, 0.18), 0, 1.0),
    (tone(note(53), 0.46, "rich", 0.06, 0.46, 0.14), 0.02, 1.0)]))
# #259 combo sting: brass stab (pitched by rate at call site for 5 tiers)
save("sfx_sting.wav", bufcat(0.30, [
    (tone(note(57), 0.28, "rich", 0.003, 0.28, 0.28), 0, 1.0),
    (tone(note(60), 0.28, "rich", 0.003, 0.28, 0.28), 0, 1.0),
    (tone(note(64), 0.28, "rich", 0.003, 0.28, 0.28), 0, 1.0),
    (tone(note(69), 0.30, "rich", 0.003, 0.30, 0.22), 0, 1.0),
    (kick(0.2), 0, 0.7)]))
# #260 reroll dice: clatter of knocks
dice = [0.0] * int(0.32 * SR)
for i, (st, v) in enumerate([(0, 0.5), (0.06, 0.42), (0.13, 0.46), (0.21, 0.34)]):
    add(dice, noise(0.03, v, 0.03, smooth=1), st)
    add(dice, tone(300 + i * 60, 0.03, "sine", 0.001, 0.03, 0.25), st)
save("sfx_dice.wav", dice)
# #261 banish poof
save("sfx_poof.wav", bufcat(0.24, [
    (noise(0.22, 0.38, 0.22, smooth=5), 0, 1.0),
    (tone(400, 0.20, "sine", 0.004, 0.20, 0.18, slide_to=75), 0, 1.0)]))
# #262 purchase cha-ching: coin arp + register ding
save("sfx_purchase.wav", bufcat(0.45, [
    (tone(note(83), 0.08, "sine", 0.002, 0.08, 0.32), 0, 1.0),
    (tone(note(88), 0.36, "sine", 0.002, 0.36, 0.32), 0.07, 1.0),
    (tone(1568, 0.30, "sine", 0.002, 0.30, 0.10), 0.07, 1.0)]))
# #263 error buzz: can't afford
save("sfx_error.wav", bufcat(0.24, [
    (tone(160, 0.10, "square", 0.002, 0.10, 0.22), 0, 1.0),
    (tone(138, 0.13, "square", 0.002, 0.13, 0.22), 0.11, 1.0)]))
# #264 wave clear jingle
wc = [0.0] * int(0.62 * SR)
for i, m in enumerate([69, 72, 76, 81]):
    add(wc, tone(note(m), 0.30, "rich", 0.004, 0.30, 0.30), i * 0.09)
add(wc, tone(note(81), 0.4, "sine", 0.004, 0.4, 0.14), 0.27)
save("sfx_waveclear.wav", wc)
# #265 boss phase roar
save("sfx_roar.wav", bufcat(0.62, [
    (tone(72, 0.60, "saw", 0.01, 0.60, 0.36, slide_to=52, vibrato=13.0), 0, 1.0),
    (tone(48, 0.60, "saw", 0.01, 0.60, 0.26, slide_to=38), 0, 1.0),
    (noise(0.60, 0.18, 0.60, smooth=3), 0, 1.0)]))
# #266 heart pickup warm chime
save("sfx_heart.wav", bufcat(0.40, [
    (tone(note(76), 0.18, "sine", 0.004, 0.18, 0.34), 0, 1.0),
    (tone(note(83), 0.30, "sine", 0.004, 0.30, 0.28), 0.09, 1.0),
    (tone(note(88), 0.34, "sine", 0.004, 0.34, 0.10), 0.09, 1.0)]))
# #267 gem merge sparkle
mg = [0.0] * int(0.30 * SR)
for i, m in enumerate([76, 83, 88]):
    add(mg, tone(note(m), 0.18, "sine", 0.002, 0.18, 0.24), i * 0.045)
save("sfx_merge.wav", mg)
# #268 focus full-charge ping
save("sfx_focus.wav", bufcat(0.32, [
    (tone(1568, 0.26, "sine", 0.002, 0.26, 0.28), 0, 1.0),
    (tone(2093, 0.18, "sine", 0.002, 0.18, 0.14), 0, 1.0),
    (tone(1046, 0.30, "sine", 0.002, 0.30, 0.10), 0, 1.0)]))
# #269 lancer charge whoosh: swelling swept noise
save("sfx_whoosh.wav", bufcat(0.38, [
    (noise(0.18, 0.26, 0.18, smooth=4), 0, 1.0),
    (noise(0.32, 0.42, 0.32, smooth=6), 0.05, 1.0)]))
# #270 weaver warp blip
save("sfx_warp.wav", bufcat(0.20, [
    (tone(300, 0.12, "sine", 0.002, 0.12, 0.30, slide_to=950), 0, 1.0),
    (tone(950, 0.07, "sine", 0.002, 0.07, 0.16, slide_to=520), 0.10, 1.0)]))
# #271 necro split triple pop
npop = [0.0] * int(0.30 * SR)
for i, f in enumerate([430, 390, 350]):
    add(npop, tone(f, 0.07, "sine", 0.001, 0.07, 0.38, slide_to=95), i * 0.085)
save("sfx_necropop.wav", npop)
# #272 treasure gnome laugh: hee-hee blips
save("sfx_gnome.wav", bufcat(0.42, [
    (tone(880, 0.09, "square", 0.003, 0.09, 0.24, slide_to=1180, vibrato=28.0), 0, 1.0),
    (tone(940, 0.09, "square", 0.003, 0.09, 0.24, slide_to=1240, vibrato=28.0), 0.12, 1.0),
    (tone(1180, 0.13, "square", 0.003, 0.13, 0.20, slide_to=990), 0.24, 1.0)]))
# #274 low-HP heartbeat layer: lub-dub thump pair (triggered on a timer in tick)
save("sfx_heartbeat.wav", bufcat(0.85, [
    (tone(58, 0.15, "sine", 0.004, 0.15, 0.85, slide_to=40), 0, 1.0),
    (tone(52, 0.13, "sine", 0.004, 0.13, 0.60, slide_to=38), 0.19, 1.0)]))

# ---------------- #273 BOSS MUSIC: battle loop + extra drums ------------------
BPM = 126.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
prog = [
    (33, [57, 60, 64]),
    (29, [53, 57, 60]),
    (36, [48, 52, 55]),
    (31, [55, 59, 62]),
]
for bar in range(BARS):
    root, chord = prog[bar // 2]
    t0 = bar * BAR
    add(buf, noise(0.5, 0.20, 0.5, smooth=1), t0, 1.0)  # crash at bar start
    for b in range(4):
        add(buf, kick(), t0 + b * BEAT, 0.9)
        add(buf, kick(), t0 + b * BEAT + BEAT / 2, 0.38)  # double-kick drive
        add(buf, hat(), t0 + b * BEAT + BEAT / 2, 0.5)
        add(buf, hat(0.04, 0.25), t0 + b * BEAT + BEAT / 4, 0.6)
        if b in (1, 3): add(buf, snare(), t0 + b * BEAT, 0.6)
    if bar % 2 == 1:  # tom fill closing each 2-bar block
        for i, f in enumerate([196, 165, 147, 110]):
            add(buf, tone(f, 0.16, "sine", 0.002, 0.15, 0.42, slide_to=f * 0.8), t0 + 3 * BEAT + i * BEAT / 4, 1.0)
    for s in range(8):
        f = note(root + 12) if s % 4 == 2 else note(root)
        add(buf, tone(f, BEAT / 2, "rich", 0.004, BEAT / 2, 0.55), t0 + s * BEAT / 2, 1.0)
    arp_notes = chord + [chord[0] + 12, chord[2] + 12]
    for s in range(16):
        m = arp_notes[[0, 1, 2, 3, 2, 1][s % 6]] + 12
        add(buf, tone(note(m), BEAT / 4, "rich", 0.003, BEAT / 4, 0.23), t0 + s * BEAT / 4, 1.0)
    for m in chord:
        add(buf, tone(note(m + 12), BAR, "sine", BAR * 0.3, BAR * 1.4, 0.10), t0, 1.0)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_boss.wav", [s * 0.8 for s in buf])

# ---------------- #275 VICTORY FANFARE EXTENDED: 8 mini-bars -------------------
BPM = 126.0; BEAT = 60.0 / BPM; MBAR = BEAT * 2
fan = [0.0] * int(MBAR * 8 * SR)
fprog = [
    (57, [69, 72, 76]), (53, [69, 72, 77]), (48, [67, 72, 76]), (55, [67, 71, 74]),
    (57, [69, 72, 76]), (53, [69, 72, 77]), (55, [71, 74, 79]), (52, [68, 71, 76]),
]
for bar in range(8):
    root, chord = fprog[bar]
    t0 = bar * MBAR
    add(fan, kick(), t0, 0.7); add(fan, kick(), t0 + BEAT, 0.7)
    add(fan, hat(), t0 + BEAT / 2, 0.4); add(fan, hat(), t0 + BEAT * 1.5, 0.4)
    add(fan, tone(note(root), MBAR, "rich", 0.005, MBAR, 0.4), t0, 1.0)
    mel = [chord[0] + 12, chord[1] + 12, chord[2] + 12, chord[1] + 12]
    for s in range(4):
        add(fan, tone(note(mel[s]), BEAT / 2, "rich", 0.004, BEAT / 2, 0.30), t0 + s * BEAT / 2, 1.0)
    for m in chord:
        add(fan, tone(note(m), MBAR, "sine", 0.01, MBAR, 0.10), t0, 1.0)
add(fan, tone(note(81), 1.2, "rich", 0.005, 1.2, 0.34), 8 * MBAR - BEAT, 1.0)
add(fan, tone(note(88), 1.2, "sine", 0.005, 1.2, 0.12), 8 * MBAR - BEAT, 1.0)
save("sfx_victory.wav", [s * 0.85 for s in fan])

# ---------------- BATCH L AUDIO (#276-#278, #294-#300 renders) -----------------
# #276 menu loop + soft pad swell layer (slow-attack octave-up chords)
BPM = 84.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
prog = [
    (45, [57, 60, 64, 71]),
    (41, [53, 57, 60, 64]),
    (48, [48, 52, 55, 59]),
    (43, [55, 59, 62, 65]),
]
for bar in range(BARS):
    root, chord = prog[bar // 2]
    t0 = bar * BAR
    for m in chord:
        add(buf, tone(note(m), BAR * 1.05, "sine", BAR * 0.35, BAR * 1.6, 0.13), t0, 1.0)
        add(buf, tone(note(m), BAR * 1.05, "rich", BAR * 0.4, BAR * 1.6, 0.035), t0, 1.0)
        add(buf, tone(note(m + 12), BAR * 1.02, "sine", BAR * 0.62, BAR * 1.7, 0.045), t0, 1.0)  # swell layer
    add(buf, tone(note(root - 12), BAR, "sine", BAR * 0.25, BAR * 1.5, 0.16), t0, 1.0)
    seq = [chord[0] + 12, chord[2] + 12, chord[1] + 12, chord[3] + 12, chord[2] + 12, chord[1] + 12, chord[0] + 12, chord[2] + 12]
    for s in range(8):
        add(buf, tone(note(seq[s]), BEAT, "sine", 0.01, BEAT * 0.95, 0.10), t0 + s * BEAT, 1.0)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_menu.wav", [s * 0.85 for s in buf])

# #277 shop theme: menu progression, plucked strings
BPM = 84.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
for bar in range(BARS):
    root, chord = prog[bar // 2]
    t0 = bar * BAR
    for m in chord:
        add(buf, tone(note(m), 0.5, "rich", 0.003, 0.45, 0.10), t0, 1.0)
    add(buf, tone(note(root - 12), BAR * 0.9, "sine", 0.01, BAR, 0.15), t0, 1.0)
    seq = [chord[0] + 12, chord[2] + 12, chord[3] + 12, chord[1] + 12, chord[2] + 12, chord[0] + 24, chord[1] + 12, chord[2] + 12,
           chord[3] + 12, chord[2] + 12, chord[1] + 12, chord[0] + 12, chord[2] + 12, chord[1] + 12, chord[0] + 12, chord[3] + 12]
    for s in range(16):
        add(buf, tone(note(seq[s]), 0.24, "rich", 0.002, 0.22, 0.16), t0 + s * BEAT / 2, 1.0)
    add(buf, hat(0.04, 0.12), t0 + BEAT, 1.0); add(buf, hat(0.04, 0.12), t0 + 3 * BEAT, 1.0)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_shop.wav", [s * 0.85 for s in buf])

# #278 endless loop: battle + extra octave arp layer
BPM = 126.0; BEAT = 60.0 / BPM; BAR = BEAT * 4; BARS = 8; DUR = BAR * BARS
buf = [0.0] * int(DUR * SR)
prog2 = [
    (33, [57, 60, 64]),
    (29, [53, 57, 60]),
    (36, [48, 52, 55]),
    (31, [55, 59, 62]),
]
for bar in range(BARS):
    root, chord = prog2[bar // 2]
    t0 = bar * BAR
    for b in range(4):
        add(buf, kick(), t0 + b * BEAT, 0.85)
        add(buf, hat(), t0 + b * BEAT + BEAT / 2, 0.5)
        if b in (1, 3): add(buf, snare(), t0 + b * BEAT, 0.55)
    for s in range(8):
        f = note(root + 12) if s % 4 == 2 else note(root)
        add(buf, tone(f, BEAT / 2, "rich", 0.004, BEAT / 2, 0.5), t0 + s * BEAT / 2, 1.0)
    arp_notes = chord + [chord[0] + 12, chord[2] + 12]
    for s in range(16):
        m = arp_notes[[0, 1, 2, 3, 2, 1][s % 6]] + 12
        add(buf, tone(note(m), BEAT / 4, "rich", 0.003, BEAT / 4, 0.20), t0 + s * BEAT / 4, 1.0)
        add(buf, tone(note(m + 12), BEAT / 4, "sine", 0.003, BEAT / 4, 0.11), t0 + s * BEAT / 4 + BEAT / 8, 1.0)  # extra arp
    for m in chord:
        add(buf, tone(note(m + 12), BAR, "sine", BAR * 0.3, BAR * 1.4, 0.10), t0, 1.0)
for i in range(seam):
    buf[i] *= i / seam; buf[-1 - i] *= i / seam
save("music_endless.wav", [s * 0.8 for s in buf])

# #294/#295 countdown beep (rate-shifted at call sites; final beep plays at 1.5x)
save("sfx_count.wav", bufcat(0.12, [
    (tone(880, 0.10, "sine", 0.003, 0.10, 0.34), 0, 1.0),
    (tone(1320, 0.08, "sine", 0.003, 0.08, 0.14), 0, 1.0)]))
# #296 spawn warning blip (soft, quiet)
save("sfx_spawn.wav", tone(620, 0.08, "sine", 0.004, 0.08, 0.15, slide_to=880))
# #298 level-up chord: bright major arpeggio
lc = [0.0] * int(0.70 * SR)
for i, m in enumerate([72, 76, 79, 84]):
    add(lc, tone(note(m), 0.5, "rich", 0.004, 0.5, 0.30), i * 0.045)
add(lc, tone(note(88), 0.55, "sine", 0.01, 0.55, 0.10), 0.14)
save("sfx_levelchord.wav", lc)
# #299 game-over sting recomposed: longer descending line + low drone tail
go2 = [0.0] * int(2.5 * SR)
for i, m in enumerate([57, 53, 50, 45, 41]):
    add(go2, tone(note(m), 0.9, "rich", 0.006, 0.9, 0.32), i * 0.30)
add(go2, tone(note(33), 2.2, "sine", 0.02, 2.2, 0.20), 0.3)
add(go2, tone(note(57), 2.0, "sine", 0.02, 2.0, 0.07), 0.3)
save("sfx_gameover.wav", [s * 0.9 for s in go2])
# #300 victory sting recomposed longer: 12 mini-bars, octave-up reprise + final stab
BPM = 126.0; BEAT = 60.0 / BPM; MBAR = BEAT * 2
fan = [0.0] * int(MBAR * 12 * SR)
fprog = [
    (57, [69, 72, 76]), (53, [69, 72, 77]), (48, [67, 72, 76]), (55, [67, 71, 74]),
    (57, [69, 72, 76]), (53, [69, 72, 77]), (55, [71, 74, 79]), (52, [68, 71, 76]),
    (53, [69, 72, 77]), (48, [67, 72, 76]), (55, [67, 71, 74]), (57, [69, 72, 76]),
]
for bar in range(12):
    root, chord = fprog[bar]
    t0 = bar * MBAR
    oct_up = 12 if bar >= 8 else 0
    add(fan, kick(), t0, 0.7); add(fan, kick(), t0 + BEAT, 0.7)
    add(fan, hat(), t0 + BEAT / 2, 0.4); add(fan, hat(), t0 + BEAT * 1.5, 0.4)
    add(fan, tone(note(root), MBAR, "rich", 0.005, MBAR, 0.4), t0, 1.0)
    mel = [chord[0] + 12 + oct_up, chord[1] + 12 + oct_up, chord[2] + 12 + oct_up, chord[1] + 12 + oct_up]
    for s in range(4):
        add(fan, tone(note(mel[s]), BEAT / 2, "rich", 0.004, BEAT / 2, 0.30), t0 + s * BEAT / 2, 1.0)
    for m in chord:
        add(fan, tone(note(m), MBAR, "sine", 0.01, MBAR, 0.10), t0, 1.0)
for m in [69, 76, 81, 88]:
    add(fan, tone(note(m), 1.4, "rich", 0.005, 1.4, 0.26), 12 * MBAR - BEAT / 2, 1.0)
save("sfx_victory.wav", [s * 0.85 for s in fan])
print("DONE")
