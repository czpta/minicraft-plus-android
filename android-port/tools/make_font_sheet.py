#!/usr/bin/env python3
"""Builds the "Minecraft-style" game font sheet from Monocraft (github.com/IdreesInc/Monocraft, SIL OFL 1.1).

The game draws text from a 256x256 sheet of 8x8 cells (32 per row, order = Font.chars). Monocraft's native grid is
9 px/em (caps 7 px tall, glyphs 5 wide, 1 px descender), so at size 9 it renders pixel-exact and fits an 8-row cell.
Each glyph is centred and thickened by 1 px horizontally to match the weight of the game's own font. Glyphs that
would not fit the cell (accented capitals) or are missing keep the original game glyph, as do the custom symbols.
usage: make_font_sheet.py <game font.png> <Monocraft.ttf> <out.png>
"""
import sys
from PIL import Image, ImageFont, ImageDraw, ImageChops
from fontTools.ttLib import TTFont

src, ttf, out = sys.argv[1:4]
CHARS = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345" "6789.,!?'\"-+=/\\%()<>:;^@ÁÉÍÓÚÑ¿¡" "ÃÊÇÔÕĞÇÜİÖŞÆØÅŰŐ[]#|{}_АБВГДЕЁЖЗ"
         "ИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯÀÂÄÈÎÌÏÒ" "ÙÛÝ*«»£$&€§ªºabcdefghijklmnopqrs" "tuvwxyzáàãâäéèêëíìîïóòõôöúùûüçñý"
         "ÿабвгдеёжзийклмнопрстуфхцчшщъыьэ" "юяışő✓")
KEEP = set("^✓@")                       # custom symbols in the original sheet (infinity, check mark, ...)
cmap = TTFont(ttf).getBestCmap()
f = ImageFont.truetype(ttf, 9)
top = f.getbbox("H")[1]                 # put cap height at row 0
sheet = Image.open(src).convert("RGBA")
done = kept = 0
for i, ch in enumerate(CHARS):
    if ch in KEEP or ord(ch) not in cmap:
        kept += 1; continue
    PAD = 4                             # scratch rows above the cell, so accents above caps are visible to the fit test
    base = Image.new("L", (8, 16), 0)
    d = ImageDraw.Draw(base); d.fontmode = "1"
    d.text((1, PAD - top), ch, font=f, fill=255)
    bold = ImageChops.lighter(base, ImageChops.offset(base, 1, 0))      # +1 px horizontal weight
    bb = bold.getbbox()
    if bb is None or bb[1] < PAD or bb[3] > PAD + 8 or bb[2] > 8:       # empty, or would be clipped by the 8x8 cell
        kept += 1; continue
    ink = lambda im: sum(1 for p in im.crop((0, PAD, 8, PAD + 8)).tobytes() if p)
    def merges(im):                                                     # rows with a 1 px gap between strokes ("#.#"): bolding would fill them in
        px = im.load()
        return sum(1 for r in range(16) if any(px[x, r] and not px[x + 1, r] and px[x + 2, r] for x in range(6)))
    tall = base if merges(base) >= 3 else bold                          # dense letters (Ш, Щ, ж, ы ...) stay unbolded
    b0 = base.getbbox()
    if ink(base) == (b0[2] - b0[0]) * (b0[3] - b0[1]):                  # fully solid rectangle = the font's "missing glyph" box
        kept += 1; continue
    tall = tall.crop((0, PAD, 8, PAD + 8))
    rgba = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    rgba.paste(Image.new("RGBA", (8, 8), (255, 255, 255, 255)), (0, 0), tall)
    x, y = (i % 32) * 8, (i // 32) * 8
    sheet.paste(Image.new("RGBA", (8, 8), (0, 0, 0, 0)), (x, y))
    sheet.paste(rgba, (x, y))
    done += 1
sheet.save(out)
print("replaced", done, "glyphs, kept", kept, "original ->", out)
