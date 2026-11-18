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
print("DONE")
