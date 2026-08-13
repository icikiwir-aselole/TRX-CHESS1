#!/usr/bin/env python3
"""TRX-CHESS brand asset generator.

Generates:
  - app/src/main/res/drawable/ic_trx_knight.xml  (vector knight mark)
  - app/src/main/res/drawable/ic_trx_foreground.xml (adaptive icon foreground)
  - app/src/main/res/drawable/ic_trx_background.xml (adaptive icon background)
  - app/src/main/res/mipmap-*/ic_launcher.png + ic_launcher_round.png (legacy API 24/25)

The knight geometry is the classic chess-knight outline scaled to the 0..100
unit box (shared with KnightMark.kt). Run: python3 scripts/gen_brand.py
Requires: Pillow
"""

import os
import math
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"

# -- knight geometry (absolute commands in the 0..45 space, public-domain outline)
COMMANDS = [
    ("M", (24.55, 10.03)),
    ("C", (21.06, 10.03, 19.24, 12.55, 19.19, 14.53)),
    ("C", (19.12, 17.22, 21.18, 18.30, 21.18, 18.30)),
    ("L", (19.69, 19.61)),
    ("C", (15.35, 23.12, 16.17, 27.80, 16.17, 27.80)),
    ("C", (15.30, 27.75, 15.75, 29.40, 15.75, 29.40)),
    ("C", (15.75, 29.40, 13.75, 30.30, 13.75, 30.30)),
    ("C", (12.85, 29.30, 11.53, 29.85, 11.53, 29.85)),
    ("C", (12.35, 27.50, 13.56, 25.63, 14.51, 24.26)),
    ("C", (14.25, 23.53, 13.14, 23.22, 13.14, 23.22)),
    ("C", (12.05, 25.18, 10.09, 27.85, 9.06, 30.64)),
    ("C", (8.40, 32.50, 10.05, 34.15, 11.80, 34.15)),
    ("C", (13.53, 34.15, 13.94, 32.53, 13.94, 32.53)),
    ("C", (14.32, 33.79, 13.20, 35.63, 13.71, 36.72)),
    ("C", (14.16, 37.64, 15.65, 37.56, 15.96, 36.62)),
    ("C", (16.24, 35.80, 16.79, 35.30, 17.33, 34.72)),
    ("C", (17.69, 34.38, 18.08, 34.02, 18.41, 33.65)),
    ("C", (18.29, 35.12, 18.24, 37.11, 18.42, 38.04)),
    ("C", (18.54, 38.63, 19.29, 39.16, 19.87, 38.57)),
    ("C", (20.58, 37.86, 20.39, 37.11, 20.60, 36.40)),
    ("C", (20.93, 35.19, 21.91, 34.73, 23.04, 33.98)),
    ("C", (25.13, 32.65, 27.33, 32.13, 29.50, 32.13)),
    ("C", (31.17, 32.13, 32.60, 32.13, 33.54, 31.63)),
    ("C", (33.54, 32.72, 34.19, 33.80, 35.27, 33.80)),
    ("C", (36.13, 33.80, 36.55, 32.95, 36.55, 32.95)),
    ("L", (37.14, 33.53)),
    ("C", (37.54, 34.80, 37.50, 36.15, 37.50, 36.15)),
    ("C", (39.57, 35.24, 39.70, 32.83, 39.70, 32.83)),
    ("C", (40.17, 31.13, 40.20, 29.68, 39.50, 27.60)),
    ("C", (38.84, 25.65, 38.14, 24.02, 37.48, 22.53)),
    ("C", (39.18, 21.82, 39.94, 19.67, 39.94, 19.67)),
    ("C", (41.14, 17.48, 39.50, 14.34, 39.50, 14.34)),
    ("C", (38.60, 13.90, 37.90, 14.18, 37.90, 14.18)),
    ("C", (38.60, 12.95, 38.50, 10.32, 38.50, 10.32)),
    ("C", (36.95, 10.20, 36.33, 11.50, 36.33, 11.50)),
    ("C", (35.04, 10.62, 33.90, 10.60, 33.90, 10.60)),
    ("C", (33.10, 8.30, 29.70, 8.00, 29.70, 8.00)),
    ("C", (28.55, 8.00, 27.05, 8.50, 25.70, 9.30)),
    ("C", (25.30, 9.15, 24.90, 9.03, 24.55, 10.03)),
    ("Z", ()),
]

S = 100.0 / 45.0


def fmt(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def path_data():
    """Vector pathData in the 0..100 unit box."""
    parts = []
    for cmd, args in COMMANDS:
        if cmd == "M":
            parts.append(f"M{fmt(args[0] * S)},{fmt(args[1] * S)}")
        elif cmd == "L":
            parts.append(f"L{fmt(args[0] * S)},{fmt(args[1] * S)}")
        elif cmd == "C":
            parts.append(
                "C"
                + " ".join(f"{fmt(a * S)},{fmt(b * S)}" for a, b in zip(args[0::2], args[1::2]))
            )
        elif cmd == "Z":
            parts.append("Z")
    return " ".join(parts)


def flatten_polygon(samples_per_curve=24):
    """Flatten all curves to a closed polygon in the 0..100 box."""
    pts = []
    start = None
    current = None
    for cmd, args in COMMANDS:
        if cmd == "M":
            current = (args[0] * S, args[1] * S)
            start = current
            pts.append(current)
        elif cmd == "L":
            current = (args[0] * S, args[1] * S)
            pts.append(current)
        elif cmd == "C":
            p0 = current
            p1 = (args[0] * S, args[1] * S)
            p2 = (args[2] * S, args[3] * S)
            p3 = (args[4] * S, args[5] * S)
            for i in range(1, samples_per_curve + 1):
                t = i / samples_per_curve
                mt = 1 - t
                x = (
                    mt ** 3 * p0[0]
                    + 3 * mt * mt * t * p1[0]
                    + 3 * mt * t * t * p2[0]
                    + t ** 3 * p3[0]
                )
                y = (
                    mt ** 3 * p0[1]
                    + 3 * mt * mt * t * p1[1]
                    + 3 * mt * t * t * p2[1]
                    + t ** 3 * p3[1]
                )
                pts.append((x, y))
            current = p3
        elif cmd == "Z":
            pts.append(start)
    return pts


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    print(f"  wrote {path.relative_to(RES.parent.parent.parent)}")


def gen_vector_drawables():
    data = path_data()
    knight = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
    <path
        android:pathData="{data}"
        android:fillColor="#E2E8F0"
        android:strokeColor="#5A1620"
        android:strokeWidth="1.5" />
</vector>
"""
    write(RES / "drawable" / "ic_trx_knight.xml", knight)

    # adaptive foreground: knight inside the 66/108 safe zone -> group scale 0.62, center 54
    fg = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.62"
        android:scaleY="0.62"
        android:translateX="23.0"
        android:translateY="25.0">
        <path
            android:pathData="{data}"
            android:fillColor="#E6EBF2"
            android:strokeColor="#3A1018"
            android:strokeWidth="1.2" />
    </group>
</vector>
"""
    write(RES / "drawable" / "ic_trx_foreground.xml", fg)

    bg = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="M0,0h108v108h-108z"
        android:fillColor="#0A0D12" />
    <path
        android:pathData="M54,14a40,40 0 1,0 0.001,0z"
        android:fillColor="#2A0A10"
        android:fillAlpha="0.9" />
</vector>
"""
    write(RES / "drawable" / "ic_trx_background.xml", bg)

    mono = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.62"
        android:scaleY="0.62"
        android:translateX="23.0"
        android:translateY="25.0">
        <path
            android:pathData="{data}"
            android:fillColor="#FFFFFF" />
    </group>
</vector>
"""
    write(RES / "drawable" / "ic_trx_monochrome.xml", mono)

    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_trx_background" />
    <foreground android:drawable="@drawable/ic_trx_foreground" />
    <monochrome android:drawable="@drawable/ic_trx_monochrome" />
</adaptive-icon>
"""
    write(RES / "mipmap-anydpi-v26" / "ic_launcher.xml", adaptive)
    write(RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml", adaptive)


def gen_legacy_pngs():
    from PIL import Image, ImageDraw, ImageFilter

    polygon = flatten_polygon()
    xs = [p[0] for p in polygon]
    ys = [p[1] for p in polygon]
    minx, maxx = min(xs), max(xs)
    miny, maxy = min(ys), max(ys)
    span = max(maxx - minx, maxy - miny)
    # center the knight and scale to 72% of the icon
    scale = 0.72
    ox = (100 - (maxx + minx)) / 2.0
    oy = (100 - (maxy + miny)) / 2.0

    def to_icon(p, size):
        return (
            ((p[0] + ox - 50) * scale + 50) * size / 100.0,
            ((p[1] + oy - 50) * scale + 50) * size / 100.0,
        )

    sizes = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for dpi, size in sizes.items():
        for name in ("ic_launcher", "ic_launcher_round"):
            img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            draw = ImageDraw.Draw(img)
            # background
            draw.ellipse(
                (size * 0.05, size * 0.05, size * 0.95, size * 0.95), fill=(10, 13, 18, 255)
            )
            # red glow behind knight
            glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            gd = ImageDraw.Draw(glow)
            gd.ellipse(
                (size * 0.18, size * 0.18, size * 0.82, size * 0.82), fill=(224, 48, 64, 90)
            )
            glow = glow.filter(ImageFilter.GaussianBlur(size * 0.08))
            img.alpha_composite(glow)
            draw = ImageDraw.Draw(img)
            # knight polygon
            pts = [to_icon(p, size) for p in polygon]
            draw.polygon(pts, fill=(226, 232, 240, 255), outline=(58, 16, 24, 255))
            # eye
            ex, ey = to_icon((30.0, 30.0), size)
            r = max(1, size * 0.012)
            draw.ellipse((ex - r * 3, ey - r * 3, ex + r * 3, ey + r * 3), fill=(255, 62, 78, 255))
            # rounded corners for round icon
            if name == "ic_launcher_round":
                mask = Image.new("L", (size, size), 0)
                md = ImageDraw.Draw(mask)
                md.ellipse((0, 0, size, size), fill=255)
                img.putalpha(mask)
            path = RES / f"mipmap-{dpi}" / f"{name}.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
            print(f"  wrote {path.relative_to(RES.parent.parent.parent)}")


def main():
    print("Generating TRX brand assets...")
    gen_vector_drawables()
    gen_legacy_pngs()
    print("Done.")


if __name__ == "__main__":
    main()