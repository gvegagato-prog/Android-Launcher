#!/usr/bin/env python3
"""Saca el icono adaptable de Carmesí (fondo, primer plano y monocromo) de docs/iconos/carmesi-original.jpg.

La imagen original es una tarjeta carmesí con esquinas redondas sobre un fondo oscuro, con un átomo (tres elipses) en líneas claras.
Android pone sus propias esquinas, así que:
  - la tarjeta se detecta (píxeles rojizos) y se mete un 9 % por cada lado;
  - el fondo es el degradado de la tarjeta (el interior muy desenfocado, sin líneas);
  - el primer plano son solo las líneas claras del átomo (lo que es más claro que el fondo), en blanco, escaladas para que quede
    dentro de la zona segura (66 % central);
  - el monocromo es el mismo átomo en negro (para los iconos con tema).
Uso: python3 herramientas/icono-carmesi.py     (necesita Pillow y numpy)
"""
import numpy as np
from PIL import Image, ImageFilter

RAIZ = __file__.rsplit('/herramientas/', 1)[0]
SRC = RAIZ + '/docs/iconos/carmesi-original.jpg'
RES = RAIZ + '/app/src/main/res/drawable-nodpi/'
C = 432

im = Image.open(SRC).convert('RGB')
a = np.asarray(im).astype(float)
m = (a[..., 0] > 90) & (a[..., 0] > a[..., 1] * 1.6) & (a[..., 0] > a[..., 2] * 1.6)
ys = np.where(m.mean(1) > 0.5)[0]; xs = np.where(m.mean(0) > 0.5)[0]
x0, x1, y0, y1 = xs[0], xs[-1], ys[0], ys[-1]
ins = int((x1 - x0) * 0.09)
inter = im.crop((x0 + ins, y0 + ins, x1 - ins, y1 - ins)).resize((1200, 1200), Image.LANCZOS)
L = np.asarray(inter).astype(float) @ np.array([.299, .587, .114])
bg = inter.filter(ImageFilter.GaussianBlur(120))
bgL = np.asarray(bg).astype(float) @ np.array([.299, .587, .114])
# Las líneas: lo que es más claro que el fondo
alfa = np.clip((L - bgL) / 40.0, 0, 1) ** 0.8

bg.resize((C, C), Image.BICUBIC).save(RES + 'ic_launcher_carmesi_bg.png')

def capa(color):
    out = Image.new('RGBA', (C, C), (0, 0, 0, 0))
    esc = int(C * 0.66)
    al = Image.fromarray((alfa * 255).astype(np.uint8)).resize((esc, esc), Image.LANCZOS)
    col = Image.new('RGBA', (esc, esc), tuple(int(v) for v in color) + (255,)); col.putalpha(al)
    out.paste(col, ((C - esc) // 2, (C - esc) // 2), col)
    return out

capa((255, 255, 255)).save(RES + 'ic_launcher_carmesi_fg.png')
capa((0, 0, 0)).save(RES + 'ic_launcher_carmesi_mono.png')
print('Hecho: fondo, primer plano y monocromo en', RES)
