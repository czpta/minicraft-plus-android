package awtshim;
public class RadialGradientPaint implements Paint {
	public final float cx, cy, radius;
	public final float[] fractions;
	public final Color[] colors;
	public RadialGradientPaint(float cx, float cy, float radius, float[] fractions, Color[] colors) {
		this.cx = cx; this.cy = cy; this.radius = radius; this.fractions = fractions; this.colors = colors;
	}
}
