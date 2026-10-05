package awtshim;
public final class AlphaComposite {
	public static final int SRC_OVER = 3;
	public final float alpha;
	private AlphaComposite(float a) { alpha = a; }
	public static AlphaComposite getInstance(int rule, float alpha) { return new AlphaComposite(alpha); }
}
