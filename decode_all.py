#!/usr/bin/env python3
"""GEMSTORM one-time bootstrap for a fresh clone of qamotech/gs.

GitHub snapshot pushes are UTF-8 text only, so this repo ships its binary
assets as base64 text (*.b64). Run this once after cloning:

    python3 decode_all.py

It restores:
  * gradle/wrapper/gradle-wrapper.jar   (so ./gradlew works)
  * app/src/main/res/raw/*.wav          (music + SFX)

After that:  ./gradlew assembleDebug
(The Gradle build also re-decodes the WAVs automatically via the
decodeB64Assets task in app/build.gradle.kts, so this script mainly
matters for the wrapper jar.)
"""
import base64
import os
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))


def decode(b64_path: str, out_path: str) -> None:
    if not os.path.exists(b64_path):
        print(f"skip (missing): {b64_path}")
        return
    with open(b64_path, "r", encoding="ascii") as fh:
        data = base64.b64decode("".join(fh.read().split()))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as fh:
        fh.write(data)
    print(f"decoded {os.path.relpath(out_path, ROOT)} ({len(data)} bytes)")


def main() -> int:
    decode(
        os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.jar.b64"),
        os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.jar"),
    )
    b64_dir = os.path.join(ROOT, "app", "b64assets", "raw")
    out_dir = os.path.join(ROOT, "app", "src", "main", "res", "raw")
    if os.path.isdir(b64_dir):
        for name in sorted(os.listdir(b64_dir)):
            if name.endswith(".wav.b64"):
                decode(os.path.join(b64_dir, name), os.path.join(out_dir, name[: -len(".b64")]))
    print("Done. Now run: ./gradlew assembleDebug")
    return 0


if __name__ == "__main__":
    sys.exit(main())
