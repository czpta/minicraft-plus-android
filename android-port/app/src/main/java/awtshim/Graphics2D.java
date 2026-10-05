package awtshim;

import awtshim.image.BufferedImage;

/**
 * The subset of java.awt.Graphics2D used by the game, with a software implementation
 * that draws straight into a BufferedImage's pixel array.
 */
public class Graphics2D {
	protected final BufferedImage img;
	protected Color color = Color.WHITE;
	protected Color background = Color.BLACK;
	protected RadialGradientPaint gradient = null;
	protected float alpha = 1f;

	protected Graphics2D() { img = null; }
	public Graphics2D(BufferedImage img) { this.img = img; }

	public void setColor(Color c) { color = c; gradient = null; }
	public void setPaint(Paint p) {
		if (p instanceof RadialGradientPaint) gradient = (RadialGradientPaint) p;
	}
	public void setBackground(Color c) { background = c; }
	public void setComposite(AlphaComposite c) { alpha = c.alpha; }
	public void dispose() {}

	public void clearRect(int x, int y, int w, int h) {
		clip(x, y, w, h, (px, py) -> img.rawSet(px, py, background.getRGB()));
	}

	public void fillRect(int x, int y, int w, int h) {
		clip(x, y, w, h, (px, py) -> blend(px, py, color.getRGB(), alpha));
	}

	public void drawRect(int x, int y, int w, int h) {
		drawLine(x, y, x + w, y);
		drawLine(x, y + h, x + w, y + h);
		drawLine(x, y, x, y + h);
		drawLine(x + w, y, x + w, y + h);
	}

	public void drawLine(int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
		while (true) {
			if (x0 >= 0 && y0 >= 0 && x0 < img.getWidth() && y0 < img.getHeight()) blend(x0, y0, color.getRGB(), alpha);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) { err += dy; x0 += sx; }
			if (e2 <= dx) { err += dx; y0 += sy; }
		}
	}

	public void fillOval(int x, int y, int w, int h) {
		float cx = x + w / 2f, cy = y + h / 2f, rx = w / 2f, ry = h / 2f;
		clip(x, y, w, h, (px, py) -> {
			float dx = (px + 0.5f - cx) / rx, dy = (py + 0.5f - cy) / ry;
			float d2 = dx * dx + dy * dy;
			if (d2 > 1f) return;
			if (gradient == null) { blend(px, py, color.getRGB(), alpha); return; }
			float t = (float) Math.sqrt(d2);
			blend(px, py, gradientColor(t), alpha);
		});
	}

	private int gradientColor(float t) {
		float[] f = gradient.fractions;
		Color[] c = gradient.colors;
		if (t <= f[0]) return c[0].getRGB();
		for (int i = 1; i < f.length; i++) {
			if (t <= f[i]) {
				float k = (t - f[i - 1]) / Math.max(1e-6f, f[i] - f[i - 1]);
				int a = lerp(c[i - 1].getAlpha(), c[i].getAlpha(), k);
				int r = lerp(c[i - 1].getRed(), c[i].getRed(), k);
				int g = lerp(c[i - 1].getGreen(), c[i].getGreen(), k);
				int b = lerp(c[i - 1].getBlue(), c[i].getBlue(), k);
				return (a << 24) | (r << 16) | (g << 8) | b;
			}
		}
		return c[c.length - 1].getRGB();
	}
	private static int lerp(int a, int b, float k) { return Math.round(a + (b - a) * k); }

	/** drawImage(img, op, x, y) - op is always null in the game. */
	public void drawImage(BufferedImage src, Object op, int x, int y) {
		int w = src.getWidth(), h = src.getHeight();
		clip(x, y, w, h, (px, py) -> blend(px, py, src.getRGB(px - x, py - y), alpha));
	}

	/** Nearest-neighbour scaled draw. */
	public void drawImage(BufferedImage src, int x, int y, int w, int h, Object observer) {
		int sw = src.getWidth(), sh = src.getHeight();
		clip(x, y, w, h, (px, py) -> {
			int sx = Math.min(sw - 1, (px - x) * sw / w), sy = Math.min(sh - 1, (py - y) * sh / h);
			blend(px, py, src.getRGB(sx, sy), alpha);
		});
	}

	// SRC_OVER of an ARGB source onto the target, honouring the composite alpha.
	protected void blend(int px, int py, int argb, float comp) {
		float sa = ((argb >>> 24) / 255f) * comp;
		if (sa <= 0f) return;
		int dst = img.rawGet(px, py);
		if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			int sg = (int) (0.299f * ((argb >> 16) & 255) + 0.587f * ((argb >> 8) & 255) + 0.114f * (argb & 255));
			img.rawSet(px, py, Math.round(sg * sa + dst * (1f - sa)));
			return;
		}
		float da = img.hasAlpha() ? (dst >>> 24) / 255f : 1f;
		float oa = sa + da * (1f - sa);
		if (oa <= 0f) { img.rawSet(px, py, 0); return; }
		int r = mix((argb >> 16) & 255, (dst >> 16) & 255, sa, da, oa);
		int g = mix((argb >> 8) & 255, (dst >> 8) & 255, sa, da, oa);
		int b = mix(argb & 255, dst & 255, sa, da, oa);
		img.rawSet(px, py, (Math.round(oa * 255f) << 24) | (r << 16) | (g << 8) | b);
	}
	private static int mix(int s, int d, float sa, float da, float oa) {
		return Math.min(255, Math.round((s * sa + d * da * (1f - sa)) / oa));
	}

	protected interface Px { void at(int x, int y); }
	protected void clip(int x, int y, int w, int h, Px p) {
		int x0 = Math.max(0, x), y0 = Math.max(0, y);
		int x1 = Math.min(img.getWidth(), x + w), y1 = Math.min(img.getHeight(), y + h);
		for (int yy = y0; yy < y1; yy++) for (int xx = x0; xx < x1; xx++) p.at(xx, yy);
	}
}
