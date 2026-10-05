package awtshim.image;

import awtshim.Graphics2D;
import awtshim.Image;

/** Pixel-array backed stand-in for java.awt.image.BufferedImage (INT_RGB, INT_ARGB, BYTE_GRAY). */
public class BufferedImage extends Image {
	public static final int TYPE_INT_RGB = 1;
	public static final int TYPE_INT_ARGB = 2;
	public static final int TYPE_BYTE_GRAY = 10;

	private final int w, h, type;
	private final int[] ints;
	private final byte[] bytes;
	private final WritableRaster raster;

	public BufferedImage(int w, int h, int type) {
		this.w = w; this.h = h; this.type = type;
		if (type == TYPE_BYTE_GRAY) {
			bytes = new byte[w * h]; ints = null;
			raster = new WritableRaster(new DataBufferByte(bytes));
		} else {
			ints = new int[w * h]; bytes = null;
			raster = new WritableRaster(new DataBufferInt(ints));
		}
	}

	public int getWidth() { return w; }
	public int getHeight() { return h; }
	public int getType() { return type; }
	public boolean hasAlpha() { return type == TYPE_INT_ARGB; }
	public WritableRaster getRaster() { return raster; }
	public Graphics2D createGraphics() { return new Graphics2D(this); }

	/** Storage-format access (used by the software Graphics2D). */
	public int rawGet(int x, int y) {
		return bytes != null ? (bytes[x + y * w] & 255) : ints[x + y * w];
	}
	public void rawSet(int x, int y, int v) {
		if (bytes != null) bytes[x + y * w] = (byte) v; else ints[x + y * w] = v;
	}

	/** ARGB as java.awt does it. */
	public int getRGB(int x, int y) {
		if (bytes != null) { int g = bytes[x + y * w] & 255; return 0xFF000000 | (g << 16) | (g << 8) | g; }
		int p = ints[x + y * w];
		return type == TYPE_INT_RGB ? (p | 0xFF000000) : p;
	}
	public void setRGB(int x, int y, int argb) {
		if (bytes != null) { bytes[x + y * w] = (byte) (((argb >> 16) & 255) * 0.299 + ((argb >> 8) & 255) * 0.587 + (argb & 255) * 0.114); return; }
		ints[x + y * w] = type == TYPE_INT_RGB ? (argb & 0xFFFFFF) : argb;
	}
	public int[] getRGB(int sx, int sy, int sw, int sh, int[] out, int off, int scan) {
		if (out == null) out = new int[off + sh * scan];
		for (int yy = 0; yy < sh; yy++) for (int xx = 0; xx < sw; xx++) out[off + yy * scan + xx] = getRGB(sx + xx, sy + yy);
		return out;
	}
	public void setRGB(int sx, int sy, int sw, int sh, int[] in, int off, int scan) {
		for (int yy = 0; yy < sh; yy++) for (int xx = 0; xx < sw; xx++) setRGB(sx + xx, sy + yy, in[off + yy * scan + xx]);
	}
	public BufferedImage getSubimage(int x, int y, int sw, int sh) {
		BufferedImage out = new BufferedImage(sw, sh, type == TYPE_INT_RGB ? TYPE_INT_RGB : TYPE_INT_ARGB);
		for (int yy = 0; yy < sh; yy++) for (int xx = 0; xx < sw; xx++) out.setRGB(xx, yy, getRGB(x + xx, y + yy));
		return out;
	}
}
