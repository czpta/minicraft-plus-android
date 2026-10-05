package awtshim;

/** Minimal java.awt.Color replacement (ARGB). */
public class Color {
	public static final Color BLACK = new Color(0, 0, 0);
	public static final Color WHITE = new Color(255, 255, 255);
	private final int argb;

	public Color(int rgb) { this.argb = 0xFF000000 | rgb; }
	public Color(int r, int g, int b) { this(r, g, b, 255); }
	public Color(int r, int g, int b, int a) {
		argb = ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}
	public int getRGB() { return argb; }
	public int getAlpha() { return argb >>> 24; }
	public int getRed() { return (argb >> 16) & 255; }
	public int getGreen() { return (argb >> 8) & 255; }
	public int getBlue() { return argb & 255; }
}
