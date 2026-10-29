"""Render the 千花万境 (Myriad Bloom) cover: a realm-globe wrapped by an overworld ring.

Three latitude bands and eight longitude cells are projected onto a sphere, so the nine landscapes curve
like a real globe instead of sitting in a photo grid. The sphere is shaded with a normal (diffuse,
specular, cool rim), and an overworld ring of grass, trees, a river and clouds circles it.

Drawn at 2x and downsampled once: blocky shapes, smooth curves and lighting.
"""

import math
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter, ImageFont

CJK_BOLD = "C:/Windows/Fonts/msyhbd.ttc"
CJK = "C:/Windows/Fonts/msyh.ttc"
MONO = "C:/Windows/Fonts/consola.ttf"
FONT_LOGO = "C:/Windows/Fonts/ariblk.ttf"

SS = 2
W, H = 1920 * SS, 1080 * SS
CX, CY = int(W * 0.5), int(H * 0.555)
R = int(H * 0.325)
PATCH = 480
SECTORS = 4
BANDS = 2


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def jit(c, rng, amount=8):
    return tuple(max(0, min(255, v + rng.randint(-amount, amount))) for v in c)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def vgrad(draw, w, y0, y1, top, bottom, power=1.0):
    for y in range(int(y0), int(y1)):
        draw.line([(0, y), (w, y)],
                  fill=mix(top, bottom, ((y - y0) / max(1.0, y1 - y0)) ** power))


def hills(draw, w, h, base, amp, colour, rng, step=6, freq=0.02):
    points = [(0, h)]
    for x in range(0, w + step, step):
        points.append((x, base + math.sin(x * freq + rng.random() * 2) * amp + rng.randint(-2, 2)))
    points += [(w, h)]
    draw.polygon(points, fill=colour)


def noise_band(draw, w, y0, y1, colour, rng, size=4, amount=10):
    for y in range(int(y0), int(y1), size):
        for x in range(0, w, size):
            draw.rectangle([x, y, x + size - 1, y + size - 1], fill=jit(colour, rng, amount))


def particles(draw, w, y0, y1, colours, count, rng, size=2):
    for _ in range(count):
        x, y = rng.randint(0, w - 1), rng.randint(int(y0), int(y1))
        draw.rectangle([x, y, x + size, y + size], fill=rng.choice(colours))


def tree(draw, x, ground, height, leaf, trunk, rng, block=6):
    top = ground - height
    draw.rectangle([x - block // 2, top, x + block // 2, ground], fill=trunk)
    draw.rectangle([x - block * 3, top - block, x + block * 3, top], fill=leaf)
    draw.rectangle([x - block * 2, top - block * 2, x + block * 2, top - block], fill=leaf)
    for _ in range(8):
        ox = rng.randint(-block * 3, block * 3)
        oy = rng.randint(-block * 2, block // 2)
        draw.rectangle([x + ox, top + oy, x + ox + block, top + oy + block],
                       fill=jit(leaf, rng, 12))


# --------------------------------------------------------------------- landscapes


def overworld(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, int(h * 0.64), (126, 186, 240), (200, 226, 246))
    for _ in range(5):
        cx = rng.randint(0, w - blk * 8)
        cy = rng.randint(blk * 2, int(h * 0.28))
        for dx in range(0, rng.randint(blk * 6, blk * 14), blk * 2):
            draw.rectangle([cx + dx, cy, cx + dx + blk * 4, cy + blk], fill=(248, 251, 255))
    hills(draw, w, h, h * 0.56, blk * 2, (94, 154, 216), rng, blk, 0.012)
    hills(draw, w, h, h * 0.63, blk * 2, (88, 146, 88), rng, blk, 0.02)
    noise_band(draw, w, h * 0.63, h, (98, 164, 84), rng, blk, 10)
    for i in range(3):
        x0 = w * (0.1 + i * 0.2)
        draw.line([(x0, h), (x0 + w * 0.18, h * 0.68)], fill=(74, 146, 210), width=blk * 4)
    for _ in range(10):
        x = rng.randint(blk, w - blk)
        y = rng.randint(int(h * 0.64), int(h * 0.92))
        tree(draw, x, y, rng.randint(blk * 5, blk * 9), (58, 134, 62), (86, 62, 40), rng, blk)


def frost(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, int(h * 0.62), (208, 230, 246), (148, 186, 216))
    for band in range(3):
        colour = [(156, 226, 214), (148, 198, 240), (188, 228, 246)][band]
        for x in range(0, w, 2):
            y = h * 0.10 + band * blk * 2 + math.sin(x * 0.02 + band) * blk
            draw.rectangle([x, y, x + 1, y + blk // 2], fill=colour)
    for base, amp, colour in ((h * 0.52, blk, (198, 218, 238)),
                              (h * 0.58, blk * 1.5, (168, 196, 222))):
        points = [(0, h)]
        for x in range(0, w + blk, blk):
            points.append((x, base - abs(math.sin(x * 0.015 + base)) * amp - rng.randint(0, blk)))
        points += [(w, h)]
        draw.polygon(points, fill=colour)
    noise_band(draw, w, h * 0.6, h, (242, 247, 252), rng, blk, 6)
    for _ in range(10):
        x = rng.randint(blk, w - blk)
        ground = rng.randint(int(h * 0.62), int(h * 0.82))
        height = rng.randint(blk * 5, blk * 9)
        for tier in range(3):
            top = ground - height + tier * (height // 3)
            half = blk + tier * blk
            draw.polygon([(x - half, top + height // 3), (x, top), (x + half, top + height // 3)],
                         fill=(28, 64, 58))
        draw.polygon([(x - blk, ground - height + height // 3), (x, ground - height),
                      (x + blk, ground - height + height // 3)], fill=(238, 246, 252))
    draw.ellipse([w * 0.04, h * 0.76, w * 0.46, h * 0.99], fill=(152, 196, 222))
    draw.ellipse([w * 0.06, h * 0.78, w * 0.44, h * 0.97], fill=(188, 222, 240))
    particles(draw, w, 0, h * 0.66, [(255, 255, 255), (240, 248, 255)], 300, rng, blk // 2)


def candy(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, int(h * 0.6), (255, 222, 238), (240, 148, 190))
    for _ in range(4):
        cx = rng.randint(0, w - blk * 10)
        cy = rng.randint(blk, int(h * 0.24))
        for dx in range(0, rng.randint(blk * 5, blk * 11), blk * 2):
            draw.rectangle([cx + dx, cy, cx + dx + blk * 4, cy + blk], fill=(255, 246, 250))
    for base, colour in ((h * 0.52, (252, 232, 240)), (h * 0.60, (246, 182, 210)),
                         (h * 0.68, (232, 138, 180))):
        hills(draw, w, h, base, blk, colour, rng, blk, 0.02)
    draw.polygon([(0, h * 0.86), (w * 0.3, h * 0.80), (w * 0.62, h * 0.90), (w, h * 0.84),
                  (w, h * 0.92), (w * 0.62, h * 0.97), (w * 0.3, h * 0.87), (0, h * 0.93)],
                 fill=(124, 80, 54))
    cx, cy, rad = int(w * 0.74), int(h * 0.44), blk * 5
    draw.rectangle([cx - blk // 2, cy, cx + blk // 2, int(h * 0.84)], fill=(252, 248, 240))
    draw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(255, 252, 250))
    for step in range(0, 78, 4):
        angle = step * 0.5
        rr = rad * (1 - step / 84.0)
        px, py = cx + math.cos(angle) * rr, cy + math.sin(angle) * rr
        draw.ellipse([px - blk // 2, py - blk // 2, px + blk // 2, py + blk // 2],
                     fill=(244, 92, 146))
    cane = int(w * 0.13)
    draw.rectangle([cane - blk, int(h * 0.56), cane + blk, int(h * 0.92)], fill=(252, 250, 248))
    for y in range(int(h * 0.56), int(h * 0.92), blk * 2):
        draw.rectangle([cane - blk, y, cane + blk, y + blk], fill=(232, 58, 84))
    for _ in range(12):
        x, y = rng.randint(blk, w - blk), rng.randint(int(h * 0.7), h - blk)
        r = rng.randint(blk, blk * 2)
        colour = rng.choice([(140, 226, 214), (255, 214, 92), (176, 140, 240), (255, 128, 168)])
        draw.ellipse([x - r, y - r, x + r, y + r], fill=colour)


def scorched(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, int(h * 0.62), (36, 12, 10), (156, 48, 14), 1.2)
    for _ in range(3):
        cx = rng.randint(0, w - blk * 12)
        cy = rng.randint(blk, int(h * 0.2))
        for dx in range(0, rng.randint(blk * 7, blk * 16), blk * 2):
            draw.rectangle([cx + dx, cy, cx + dx + blk * 5, cy + blk], fill=(62, 34, 28))
    wall = [(0, h)]
    for x in range(0, w + blk, blk):
        wall.append((x, h * 0.38 + math.sin(x * 0.03) * blk * 2 + rng.randint(-blk, blk)))
    wall += [(w, h)]
    draw.polygon(wall, fill=(30, 22, 26))
    for x in range(0, w, blk):
        top = h * 0.38 + math.sin(x * 0.03) * blk * 2
        draw.line([(x, top), (x, top + blk * 3)], fill=(50, 38, 42))
    shore = h * 0.62
    points = [(0, h)]
    for x in range(0, w + blk, blk):
        points.append((x, shore + math.sin(x * 0.02) * blk))
    points += [(w, h)]
    draw.polygon(points, fill=(226, 74, 14))
    inner = [(0, h)]
    for x in range(0, w + blk, blk):
        inner.append((x, shore + blk * 2 + math.sin(x * 0.02) * blk))
    inner += [(w, h)]
    draw.polygon(inner, fill=(255, 132, 28))
    for y in range(int(shore) + blk * 2, h, blk):
        for x in range(0, w, blk):
            if rng.random() < 0.55:
                draw.rectangle([x, y, x + blk, y + blk],
                               fill=rng.choice([(255, 190, 60), (252, 112, 20), (206, 58, 10)]))
    draw.line([(0, shore), (w, shore)], fill=(255, 220, 130), width=blk)
    for _ in range(8):
        x = rng.randint(0, w)
        top = rng.randint(int(h * 0.32), int(shore) - blk)
        wid = rng.randint(blk * 2, blk * 4)
        draw.polygon([(x, shore + blk), (x + wid // 2, top), (x + wid, shore + blk)],
                     fill=(22, 18, 24))
    particles(draw, w, h * 0.2, shore, [(255, 190, 70), (255, 120, 30), (255, 244, 190)], 220,
              rng, blk // 2)


def elmyia(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h, (28, 36, 64), (176, 108, 116), 1.6)
    draw.ellipse([w * 0.78, blk, w * 0.78 + blk * 5, blk * 6], fill=(250, 244, 216))
    for base, amp, colour in ((h * 0.56, blk, (44, 44, 66)), (h * 0.62, blk * 1.2, (32, 30, 50))):
        hills(draw, w, h, base, amp, colour, rng, blk, 0.015)
    base = int(h * 0.70)
    cx = int(w * 0.32)
    for tier in range(4):
        y = base - tier * blk * 2
        half = blk * (7 - tier)
        draw.rectangle([cx - half, y - blk * 2, cx + half, y], fill=(58, 30, 34))
        left, right = cx - half - blk * 2, cx + half + blk * 2
        draw.polygon([(left, y - blk), (left + blk, y - blk * 2), (cx - half + blk, y - blk * 3),
                      (cx + half - blk, y - blk * 3), (right - blk, y - blk * 2), (right, y - blk)],
                     fill=(24, 16, 22))
        draw.line([(cx - half + blk, y - blk * 3), (cx + half - blk, y - blk * 3)],
                  fill=(226, 180, 94))
        for side in (-1, 1):
            lx = cx + side * (half + blk)
            draw.rectangle([lx, y - blk, lx + blk, y], fill=(238, 76, 58))
    top_y = base - 4 * blk * 2 - blk * 3
    draw.polygon([(cx - blk, top_y), (cx, top_y - blk * 3), (cx + blk, top_y)], fill=(226, 180, 94))
    noise_band(draw, w, h * 0.72, h, (56, 42, 48), rng, blk, 8)
    for x in (blk * 2, blk * 9, w - blk * 9, w - blk * 2):
        draw.rectangle([x, base - blk * 4, x + blk, base + blk * 2], fill=(56, 40, 34))
        for dx, dy in ((-blk, -blk * 5), (blk, -blk * 6), (0, -blk * 7)):
            r = blk * 2
            draw.ellipse([x + dx - r, base + dy - r, x + dx + r, base + dy + r],
                         fill=(236, 158, 186))
    particles(draw, w, 0, h, [(246, 176, 196), (255, 214, 226)], 180, rng, blk // 2)


def moon(draw, w, h, rng):
    blk = max(4, w // 110)
    draw.rectangle([0, 0, w, h], fill=(5, 6, 13))
    particles(draw, w, 0, h * 0.62, [(255, 255, 255), (206, 220, 255), (150, 160, 200)], 340, rng, 2)
    ex, ey, er = int(w * 0.2), blk * 5, blk * 5
    draw.ellipse([ex - er, ey - er, ex + er, ey + er], fill=(30, 62, 128))
    draw.ellipse([ex - er + blk, ey - er + blk, ex - blk, ey + blk], fill=(58, 116, 78))
    draw.ellipse([ex + blk, ey, ex + er - blk, ey + er - blk], fill=(78, 138, 90))
    draw.ellipse([ex - blk * 2, ey - blk * 2, ex + blk // 2, ey], fill=(238, 246, 255))
    ground = h * 0.62
    noise_band(draw, w, ground, h, (126, 126, 132), rng, blk, 6)
    for _ in range(14):
        x, y = rng.randint(0, w - blk * 4), rng.randint(int(ground) + blk, h - blk * 2)
        r = rng.randint(blk * 2, blk * 4)
        draw.ellipse([x, y, x + r * 2, y + r], fill=(104, 104, 110))
        draw.arc([x, y, x + r * 2, y + r], 190, 350, fill=(152, 152, 158))
    fx, fy = int(w * 0.8), int(h * 0.6)
    draw.rectangle([fx, fy - blk * 6, fx + blk // 2, fy], fill=(216, 216, 222))
    draw.polygon([(fx + blk // 2, fy - blk * 6), (fx + blk * 3, fy - blk * 5),
                  (fx + blk // 2, fy - blk * 3)], fill=(228, 228, 234))


def ash(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h * 0.68, (58, 50, 46), (172, 152, 128), 1.25)
    for _ in range(3):
        cx = rng.randint(0, w - blk * 12)
        cy = rng.randint(blk, int(h * 0.2))
        for dx in range(0, rng.randint(blk * 6, blk * 14), blk * 2):
            draw.rectangle([cx + dx, cy, cx + dx + blk * 5, cy + blk], fill=(84, 74, 66))
    for _ in range(16):
        x = rng.randint(-blk, w - blk * 2)
        height = rng.randint(blk * 3, blk * 11)
        width = rng.randint(blk * 2, blk * 4)
        base = int(h * 0.68 + rng.randint(-blk, blk))
        colour = jit(shade((110, 104, 98), rng.uniform(0.55, 1.0)), rng, 5)
        draw.rectangle([x, base - height, x + width, base], fill=colour)
        for step in range(0, width, max(2, blk // 2)):
            draw.line([(x + step, base - height),
                       (x + step + rng.randint(-1, 1), base - height - rng.randint(blk // 2, blk * 2))],
                      fill=(64, 58, 54))
        for wy in range(base - height + blk, base, blk * 2):
            if rng.random() < 0.45:
                draw.rectangle([x + blk, wy, x + width - blk, wy + blk], fill=(30, 28, 28))
    tx, ty = int(w * 0.14), int(h * 0.72)
    draw.rectangle([tx, ty - blk * 5, tx + blk, ty], fill=(38, 32, 30))
    for dx, dy in ((-blk * 2, -blk * 4), (blk, -blk * 3), (-blk, -blk * 2)):
        draw.line([(tx, ty - blk * 4), (tx + dx, ty - blk * 4 + dy)], fill=(38, 32, 30))
    noise_band(draw, w, h * 0.7, h, (112, 104, 96), rng, blk, 8)
    for _ in range(24):
        x, y = rng.randint(0, w - blk * 2), rng.randint(int(h * 0.7), h - blk)
        draw.rectangle([x, y, x + blk * 2, y + blk], fill=(76, 70, 64))
    particles(draw, w, 0, h, [(186, 180, 170), (150, 146, 138), (118, 114, 108)], 340, rng, blk // 2)


def garden(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h * 0.7, (186, 232, 244), (126, 200, 220))
    for _ in range(5):
        cx = rng.randint(blk, w - blk * 10)
        cy = rng.randint(blk, int(h * 0.22))
        for dx in range(0, rng.randint(blk * 5, blk * 12), blk * 2):
            draw.rectangle([cx + dx, cy, cx + dx + blk * 4, cy + blk], fill=(250, 253, 255))
    sea = h * 0.72
    draw.rectangle([0, sea, w, h], fill=(54, 130, 176))
    for y in range(int(sea), h, blk):
        for x in range(0, w, blk * 2):
            if rng.random() < 0.35:
                draw.rectangle([x, y, x + blk, y + blk // 2], fill=(86, 170, 204))
    cx = w * 0.5
    island = w * 0.66
    draw.ellipse([cx - island / 2, sea - blk, cx + island / 2, sea + blk * 2], fill=(52, 40, 34))
    draw.ellipse([cx - island / 2 + blk, sea - blk * 2, cx + island / 2 - blk, sea + blk * 1.5],
                 fill=(206, 184, 138))
    draw.ellipse([cx - island / 2 + blk * 2, sea - blk * 3, cx + island / 2 - blk * 2, sea + blk],
                 fill=(62, 148, 76))
    draw.ellipse([cx - island / 2 + blk * 3, sea - blk * 3, cx + island / 2 - blk * 3, sea - blk],
                 fill=(104, 186, 96))
    top = sea - blk * 9
    draw.rectangle([cx - blk, top, cx + blk, sea - blk], fill=(84, 60, 40))
    for radius, colour in ((blk * 4, (44, 108, 56)), (blk * 3, (66, 142, 68)),
                           (blk * 2, (94, 172, 82))):
        r = int(radius)
        draw.ellipse([cx - r, top - r, cx + r, top + r], fill=colour)
    draw.rectangle([cx - island / 2 + blk * 2, sea - blk, cx - island / 2 + blk * 3, sea + blk * 2],
                   fill=(152, 216, 234))


def eternity(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h * 0.76, (20, 7, 38), (120, 60, 166), 1.3)
    particles(draw, w, 0, h * 0.76, [(240, 230, 255), (196, 176, 240), (150, 120, 210)], 320, rng, 2)
    for _ in range(4):
        x = rng.randint(blk * 2, w - blk * 4)
        y = rng.randint(int(h * 0.26), int(h * 0.52))
        wdt = rng.randint(blk * 2, blk * 4)
        draw.polygon([(x - wdt, y), (x + wdt, y), (x, y + wdt)], fill=(52, 26, 76))
        draw.rectangle([x - wdt, y - blk, x + wdt, y], fill=(102, 62, 136))
        draw.rectangle([x - wdt + blk, y - blk * 2, x + wdt - blk, y - blk], fill=(134, 92, 172))
        for _ in range(2):
            sx = x + rng.randint(-wdt + blk, wdt - blk)
            draw.polygon([(sx, y - blk * 3), (sx + blk, y - blk * 2), (sx, y - blk),
                          (sx - blk, y - blk * 2)], fill=(210, 172, 252))
    ground = int(h * 0.78)
    noise_band(draw, w, ground, h, (56, 28, 80), rng, blk, 8)
    cx = int(w * 0.5)
    for i, colour in enumerate([(112, 58, 170), (162, 106, 224), (208, 172, 248)]):
        pad = blk * (6 - i)
        draw.ellipse([cx - pad, ground - blk * 14 + i * blk, cx + pad, ground],
                     outline=colour, width=blk // 2)
    draw.ellipse([cx - blk * 2, ground - blk * 11, cx + blk * 2, ground - blk * 2],
                 fill=(198, 158, 242))


def ice_cave(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h, (16, 40, 56), (44, 104, 130))
    for _ in range(12):
        x = rng.randint(0, w)
        draw.polygon([(x, 0), (x + rng.randint(blk, blk * 3), 0),
                      (x + rng.randint(blk * 2, blk * 6), h)], fill=(26, 62, 84))
    noise_band(draw, w, h * 0.7, h, (122, 192, 216), rng, blk, 8)
    particles(draw, w, 0, h, [(150, 220, 240)], 150, rng, blk // 2)


def lava_cave(draw, w, h, rng):
    blk = max(4, w // 110)
    vgrad(draw, w, 0, h, (24, 8, 8), (88, 28, 10))
    noise_band(draw, w, h * 0.6, h, (42, 24, 22), rng, blk, 8)
    for _ in range(9):
        x = rng.randint(0, w)
        draw.polygon([(x, h * 0.6), (x + blk * 2, h * 0.6), (x + blk, h)], fill=(255, 112, 26))
    particles(draw, w, 0, h * 0.7, [(255, 170, 60)], 120, rng, blk // 2)


# ---------------------------------------------------------------------- globe


def patch_image(fn, seed):
    img = Image.new("RGB", (PATCH, PATCH))
    fn(ImageDraw.Draw(img), PATCH, PATCH, random.Random(seed))
    # keep the authored exposure: the night realms should stay dark
    return np.asarray(img).astype(np.float32)


def build_globe():
    patches = {
        "overworld": patch_image(overworld, 1),
        "frost": patch_image(frost, 2),
        "candy": patch_image(candy, 3),
        "scorched": patch_image(scorched, 4),
        "elmyia": patch_image(elmyia, 5),
        "moon": patch_image(moon, 6),
        "ash": patch_image(ash, 7),
        "garden": patch_image(garden, 8),
        "eternity": patch_image(eternity, 9),
        "ice": patch_image(ice_cave, 10),
        "lava": patch_image(lava_cave, 11),
    }
    north = ["frost", "eternity", "moon", "ash"]
    south = ["scorched", "candy", "garden", "elmyia"]

    size = R * 2
    ys, xs = np.mgrid[0:size, 0:size]
    nx = (xs - R + 0.5) / R
    ny = (ys - R + 0.5) / R
    r2 = nx * nx + ny * ny
    inside = r2 <= 1.0

    lon = np.arctan2(nx, np.sqrt(np.clip(1.0 - r2, 0.0, 1.0)))
    lon_norm = (lon + math.pi / 2) / math.pi * SECTORS
    sector = np.clip(lon_norm.astype(int), 0, SECTORS - 1)
    u = lon_norm % 1.0
    # equal bands in screen space, so a patch keeps its height instead of tapering at the poles
    band_f = np.clip((ny + 1.0) * (BANDS / 2.0), 0.0, BANDS - 1e-6)
    band = band_f.astype(int)
    v = band_f - band

    out = np.zeros((size, size, 3), np.float32)
    for b in range(BANDS):
        names = (north, south)[b]
        for s in range(SECTORS):
            mask = inside & (band == b) & (sector == s)
            if not mask.any():
                continue
            art = patches[names[s]]
            ph, pw = art.shape[:2]
            sx = np.clip((u[mask] * (pw - 1)).astype(int), 0, pw - 1)
            # keep most of each landscape, trimming only the empty top of the sky
            vv = 0.06 + v[mask] * 0.90
            sy = np.clip((vv * (ph - 1)).astype(int), 0, ph - 1)
            out[mask] = art[sy, sx]

    # faint graticule on the seams so the patchwork reads as a globe grid
    u_edge = np.minimum(u, 1.0 - u)
    v_edge = np.minimum(v, 1.0 - v)
    seam = np.where((u_edge < 0.012) | (v_edge < 0.03), 0.82, 1.0)
    out = out * seam[..., None]

    z = np.sqrt(np.clip(1.0 - r2, 0.0, 1.0))
    light = np.array([-0.42, -0.52, 0.74])
    light /= np.linalg.norm(light)
    nx3 = nx * inside
    ny3 = ny * inside
    ndotl = np.clip(nx3 * light[0] + ny3 * light[1] + z * light[2], 0.0, 1.0)
    diffuse = 0.50 + 0.66 * ndotl
    refl = 2.0 * ndotl[..., None] * np.stack([nx3, ny3, z], axis=-1) - light
    spec = np.clip(refl[..., 2], 0.0, 1.0) ** 34 * 0.34
    rim = np.clip((1.0 - z) ** 4.0, 0.0, 1.0) * 0.45

    out = out * diffuse[..., None]
    out = out + 255.0 * spec[..., None]
    out = out + np.stack([rim * 40, rim * 62, rim * 92], axis=-1)
    out = np.clip(out, 0, 255).astype(np.uint8)

    globe = Image.fromarray(out, "RGB").convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)

    globe.putalpha(mask)
    return globe


def build_ring(globe_size):
    pad = int(globe_size * 0.15)
    size = globe_size + pad * 2
    ring = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(ring)
    outer = size // 2
    inner = globe_size // 2
    rng = random.Random(99)
    for i in range(pad):
        t = i / pad
        radius = inner + pad - i
        colour = mix((108, 176, 90), (60, 118, 68), t ** 0.7)
        draw.ellipse([outer - radius, outer - radius, outer + radius, outer + radius],
                     outline=jit(colour, rng, 6), width=2)
    for i in range(max(2, pad // 6)):
        radius = outer - i
        draw.ellipse([outer - radius, outer - radius, outer + radius, outer + radius],
                     outline=mix((72, 56, 44), (40, 30, 26), i / max(1, pad // 6)), width=2)
    river_r = inner + int(pad * 0.5)
    draw.arc([outer - river_r, outer - river_r, outer + river_r, outer + river_r],
             205, 330, fill=(86, 158, 214), width=max(3, pad // 8))
    for _ in range(80):
        angle = rng.uniform(0, math.tau)
        radius = inner + rng.randint(int(pad * 0.3), pad)
        x = outer + math.cos(angle) * radius
        y = outer + math.sin(angle) * radius
        block = max(3, pad // 12)
        tree(draw, int(x), int(y), rng.randint(block * 3, block * 6), (54, 128, 60),
             (78, 56, 36), rng, block)
    for _ in range(12):
        angle = rng.uniform(0, math.tau)
        radius = inner + rng.randint(0, pad)
        x = int(outer + math.cos(angle) * radius)
        y = int(outer + math.sin(angle) * radius)
        cw = rng.randint(pad // 4, pad // 2)
        for dx in range(0, cw, max(3, pad // 14)):
            draw.rectangle([x + dx, y, x + dx + cw // 2, y + max(3, pad // 16)],
                           fill=(246, 250, 255, 215))
    shadow = Image.new("L", (size, size), 0)
    sd = ImageDraw.Draw(shadow)
    for i in range(20):
        radius = inner + i
        sd.ellipse([outer - radius, outer - radius, outer + radius, outer + radius],
                   outline=int(110 * (1 - i / 20)), width=2)
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    dark = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    ring = Image.composite(dark, ring, ImageChops.multiply(shadow, ring.getchannel("A")))
    return ring


REALM_COLOURS = [
    (150, 214, 255),   # frost
    (255, 124, 186),   # candy
    (255, 104, 32),    # scorched
    (255, 202, 92),    # elmyia
    (120, 226, 138),   # garden
    (186, 132, 255),   # eternity
    (168, 204, 250),   # moon
    (236, 176, 132),   # ash
]


def realm_gradient(width, height):
    """Horizontal gradient that walks through the theme colours of the eight realms."""
    img = Image.new("RGB", (width, height))
    draw = ImageDraw.Draw(img)
    segments = len(REALM_COLOURS) - 1
    for x in range(width):
        t = x / max(1, width - 1) * segments
        i = min(segments - 1, int(t))
        colour = mix(REALM_COLOURS[i], REALM_COLOURS[i + 1], t - i)
        draw.line([(x, 0), (x, height)], fill=colour)
    return img


def flower(draw, cx, cy, radius, colour, alpha=255):
    """Four petal mark used as an ornament on both sides of the wordmark."""
    for i in range(4):
        angle = i * math.pi / 2 + math.pi / 4
        px = cx + math.cos(angle) * radius * 0.52
        py = cy + math.sin(angle) * radius * 0.52
        r = radius * 0.44
        draw.ellipse([px - r, py - r, px + r, py + r], fill=colour + (alpha,))
    core = radius * 0.26
    draw.ellipse([cx - core, cy - core, cx + core, cy + core], fill=(255, 255, 255, alpha))


def glyph_images(text, font, pad):
    """Renders each character on its own, so the wordmark can follow an arc."""
    probe = Image.new("L", (10, 10), 0)
    pd = ImageDraw.Draw(probe)
    out = []
    for ch in text:
        width = int(math.ceil(pd.textlength(ch, font=font)))
        if ch == " ":
            out.append((None, width))
            continue
        box = font.getbbox(ch)
        img = Image.new("L", (width + pad * 2, (box[3] - box[1]) + pad * 2), 0)
        ImageDraw.Draw(img).text((pad - box[0], pad - box[1]), ch, font=font, fill=255)
        out.append((img, width))
    return out


def wordmark(text, size, spacing, arc_px, max_angle):
    """Builds the logotype mask: characters laid on a shallow arc."""
    font = ImageFont.truetype(FONT_LOGO, size)
    glyphs = glyph_images(text, font, 6)
    widths = [w for _, w in glyphs]
    total = sum(widths) + spacing * (len(widths) - 1)

    layer_w = total + size
    layer_h = size * 2 + arc_px * 2
    mask = Image.new("L", (layer_w, layer_h), 0)
    x = (layer_w - total) / 2
    centre = layer_w / 2
    for img, width in glyphs:
        if img is not None:
            t = (x + width / 2 - centre) / (total / 2)
            angle = -t * max_angle
            dy = arc_px * t * t
            rotated = img.rotate(angle, resample=Image.BICUBIC, expand=True)
            mask.paste(rotated, (int(x + width / 2 - rotated.width / 2),
                                 int(layer_h / 2 - rotated.height / 2 + dy)), rotated)
        x += width + spacing
    return mask


def paint_wordmark(canvas, text, centre_x, centre_y, target_width, arc_px, max_angle):
    size = int(H * 0.16)
    spacing = int(size * 0.125)
    mask = wordmark(text, size, spacing, arc_px, max_angle)
    # scale to the requested width
    scale = target_width / mask.width
    mask = mask.resize((int(mask.width * scale), int(mask.height * scale)), Image.LANCZOS)
    if mask.getbbox():
        mask = mask.crop(mask.getbbox())

    pad = int(H * 0.03)
    layer = Image.new("L", (mask.width + pad * 2, mask.height + pad * 2), 0)
    layer.paste(mask, (pad, pad))

    # glow
    glow = layer.filter(ImageFilter.GaussianBlur(int(H * 0.022)))
    glow_img = Image.new("RGBA", layer.size, (96, 168, 240, 0))
    glow_img.putalpha(glow.point(lambda v: int(v * 0.85)))

    # dark outline
    outline = layer.filter(ImageFilter.MaxFilter(9))

    # gradient body
    body = Image.new("RGB", layer.size)
    bd = ImageDraw.Draw(body)
    for y in range(layer.size[1]):
        t = y / max(1, layer.size[1] - 1)
        if t < 0.38:
            colour = mix((255, 255, 255), (214, 240, 255), t / 0.38)
        elif t < 0.72:
            colour = mix((214, 240, 255), (146, 194, 240), (t - 0.38) / 0.34)
        else:
            colour = mix((146, 194, 240), (86, 132, 196), (t - 0.72) / 0.28)
        bd.line([(0, y), (layer.size[0], y)], fill=colour)
    # faint pixel grid, so the logotype belongs to a block game
    grid = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid)
    step = max(6, layer.size[1] // 14)
    for gx in range(0, layer.size[0], step):
        gd.line([(gx, 0), (gx, layer.size[1])], fill=(40, 70, 110, 26))
    for gy in range(0, layer.size[1], step):
        gd.line([(0, gy), (layer.size[0], gy)], fill=(40, 70, 110, 26))
    body = Image.alpha_composite(body.convert("RGBA"), grid).convert("RGB")

    art = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    art.paste(glow_img, (0, 0), glow_img)
    dark = Image.new("RGBA", layer.size, (8, 14, 26, 255))
    art.paste(dark, (0, 0), outline)
    art.paste(body.convert("RGBA"), (0, 0), layer)

    # a thin highlight along the top edge, a soft shadow along the bottom edge
    top = layer.filter(ImageFilter.MaxFilter(3))
    top_mask = ImageChops.subtract(top, layer)
    art.paste(Image.new("RGBA", layer.size, (255, 255, 255, 130)), (0, 0), top_mask)
    shifted = ImageChops.offset(layer, 0, -max(2, int(H * 0.004)))
    bottom_mask = ImageChops.subtract(shifted, layer)
    art.paste(Image.new("RGBA", layer.size, (30, 56, 96, 90)), (0, 0), bottom_mask)

    pos = (int(centre_x - art.width / 2), int(centre_y - art.height / 2))
    canvas.alpha_composite(art, pos)
    return art.width, art.height


def paint_colour_line(canvas, text, centre_x, centre_y, target_width, size, arc_px, max_angle):
    """One line of the logotype: arced letters, realm gradient, bevel, glow."""
    font = ImageFont.truetype(FONT_LOGO, size)
    spacing = int(size * 0.13)
    mask = wordmark(text, size, spacing, arc_px, max_angle)
    scale = target_width / mask.width
    mask = mask.resize((max(1, int(mask.width * scale)), max(1, int(mask.height * scale))),
                       Image.LANCZOS)
    if mask.getbbox():
        mask = mask.crop(mask.getbbox())

    pad = int(H * 0.04)
    layer = Image.new("L", (mask.width + pad * 2, mask.height + pad * 2), 0)
    layer.paste(mask, (pad, pad))

    # outer glow in realm colours
    glow = layer.filter(ImageFilter.GaussianBlur(int(H * 0.018)))
    glow_img = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    glow_img.putalpha(glow.point(lambda v: int(v * 0.9)))
    glow_tint = realm_gradient(*layer.size).convert("RGBA")
    glow_img = Image.composite(glow_tint, glow_img, glow_img.getchannel("A"))

    outline = layer.filter(ImageFilter.MaxFilter(11))
    inner = layer.filter(ImageFilter.MaxFilter(3))

    body = realm_gradient(*layer.size)
    # vertical shading so the gradient does not look like a flat rainbow
    shade_im = Image.new("L", layer.size)
    sd = ImageDraw.Draw(shade_im)
    for y in range(layer.size[1]):
        sd.line([(0, y), (layer.size[0], y)], fill=int(255 * (1.0 - 0.42 * (y / layer.size[1]))))
    body = Image.composite(body, Image.new("RGB", layer.size, (18, 28, 46)),
                           shade_im)
    body = ImageEnhance.Color(body).enhance(1.45)
    body = ImageEnhance.Contrast(body).enhance(1.12)

    art = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    art.paste(glow_img, (0, 0), glow_img)
    art.paste(Image.new("RGBA", layer.size, (6, 10, 20, 255)), (0, 0), outline)
    art.paste(Image.new("RGBA", layer.size, (240, 248, 255, 235)), (0, 0), inner)
    art.paste(body.convert("RGBA"), (0, 0), layer)

    # bevel: bright top edge, dark bottom edge
    top_mask = ImageChops.subtract(inner, layer)
    art.paste(Image.new("RGBA", layer.size, (255, 255, 255, 160)), (0, 0), top_mask)
    shifted = ImageChops.offset(layer, 0, -max(2, int(H * 0.005)))
    bottom_mask = ImageChops.subtract(ImageChops.offset(layer, 0, max(2, int(H * 0.006))), layer)
    art.paste(Image.new("RGBA", layer.size, (10, 18, 34, 120)), (0, 0), bottom_mask)

    # diagonal sheen
    sheen = Image.new("L", layer.size, 0)
    shd = ImageDraw.Draw(sheen)
    band = int(layer.size[1] * 1.4)
    for i in range(band):
        x = int(layer.size[0] * 0.15) + i
        shd.line([(x, 0), (x + int(layer.size[1] * 0.6), layer.size[1])], fill=90)
    sheen = sheen.filter(ImageFilter.GaussianBlur(layer.size[1] // 6))
    sheen = ImageChops.multiply(sheen, layer)
    art.paste(Image.new("RGBA", layer.size, (255, 255, 255, 90)), (0, 0), sheen)

    pos = (int(centre_x - art.width / 2), int(centre_y - art.height / 2))
    canvas.alpha_composite(art, pos)
    return mask.width


def main():
    rng = random.Random(2027)
    bg = Image.new("RGB", (W, H), (8, 10, 18))
    draw = ImageDraw.Draw(bg)
    for y in range(H):
        draw.line([(0, y), (W, y)], fill=mix((22, 30, 48), (4, 5, 10), (y / H) ** 0.75))
    nebula = Image.new("RGB", (W, H), (0, 0, 0))
    nd = ImageDraw.Draw(nebula)
    for _ in range(46):
        x, y = rng.randint(0, W), rng.randint(0, H)
        r = rng.randint(W // 14, W // 5)
        nd.ellipse([x - r, y - r, x + r, y + r],
                   fill=rng.choice([(30, 46, 84), (44, 30, 66), (20, 50, 62), (24, 40, 60)]))
    nebula = nebula.filter(ImageFilter.GaussianBlur(W // 80))
    bg = Image.blend(bg, nebula, 0.5)
    draw = ImageDraw.Draw(bg)
    for _ in range(1600):
        draw.point((rng.randint(0, W), rng.randint(0, H)),
                   fill=rng.choice([(130, 150, 200), (76, 92, 128), (180, 200, 240)]))

    globe = build_globe()
    ring = build_ring(R * 2)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for i in range(46):
        t = i / 45.0
        radius = R + int(R * 0.14 * (1 - t))
        gd.ellipse([CX - radius, CY - radius, CX + radius, CY + radius],
                   outline=(120, 190, 240, int(24 * (1 - t))), width=max(2, R // 70))
    glow = glow.filter(ImageFilter.GaussianBlur(R // 45))
    canvas = Image.alpha_composite(bg.convert("RGBA"), glow)
    canvas.alpha_composite(ring, (CX - ring.width // 2, CY - ring.height // 2))
    canvas.alpha_composite(globe, (CX - R, CY - R))

    # the logotype sits in the middle of the globe, tinted with the realm colours
    draw = ImageDraw.Draw(canvas)
    plate = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pd = ImageDraw.Draw(plate)
    plate_r = int(R * 1.15)
    for i in range(70):
        t = i / 69.0
        radius = int(plate_r * (1.0 - t * 0.9))
        pd.ellipse([CX - radius, CY - int(radius * 0.82), CX + radius, CY + int(radius * 0.82)],
                   fill=(4, 8, 16, int(96 * t ** 1.5)))
    plate = plate.filter(ImageFilter.GaussianBlur(R // 6))
    canvas = Image.alpha_composite(canvas, plate)
    draw = ImageDraw.Draw(canvas)

    halo = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    for ring_r, alpha in ((int(R * 1.30), 46), (int(R * 1.19), 30)):
        hd.ellipse([CX - ring_r, CY - ring_r + int(R * 0.10), CX + ring_r, CY + ring_r],
                   outline=(186, 214, 246, alpha), width=3)
    halo = halo.filter(ImageFilter.GaussianBlur(2))
    canvas = Image.alpha_composite(canvas, halo)
    draw = ImageDraw.Draw(canvas)

    line_size = int(H * 0.088)
    line_gap = int(line_size * 0.94)
    a_w = paint_colour_line(canvas, "MYRIAD", CX, CY - int(line_gap * 0.56), int(R * 1.62),
                            line_size, int(H * 0.012), 0.05)
    b_w = paint_colour_line(canvas, "BLOOM", CX, CY + int(line_gap * 0.44), int(R * 1.42),
                            line_size, int(H * 0.012), 0.05)

    draw = ImageDraw.Draw(canvas)
    mark_y = CY + int(line_gap * 0.42)
    for side in (-1, 1):
        fx = CX + side * (b_w / 2 + int(H * 0.034))
        flower(draw, fx, mark_y, int(H * 0.022), (206, 230, 252), 245)

    # a slim rule between the two words
    draw.line([(CX - int(b_w * 0.36), CY - int(line_gap * 0.02)),
               (CX + int(b_w * 0.36), CY - int(line_gap * 0.02))], fill=(120, 150, 196), width=2)

    # vignette
    vig = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vig)
    vd.rectangle([0, 0, W, H], fill=120)
    vd.ellipse([-W * 0.12, -H * 0.18, W * 1.12, H * 1.18], fill=0)
    vig = vig.filter(ImageFilter.GaussianBlur(W // 26))
    canvas = Image.composite(Image.new("RGB", (W, H), (3, 4, 8)), canvas, vig)

    canvas.save("cover@2x.png")
    canvas.resize((1920, 1080), Image.LANCZOS).save("cover.png")
    print("saved", canvas.size)


if __name__ == "__main__":
    main()
