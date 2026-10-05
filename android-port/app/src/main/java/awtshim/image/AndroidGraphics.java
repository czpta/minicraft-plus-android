package awtshim.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import awtshim.Graphics2D;

/** Graphics2D that paints onto an Android Canvas (the window). Only what Renderer needs. */
class AndroidGraphics extends Graphics2D {
	private final Canvas canvas;
	private final Paint paint = new Paint();
	private static Bitmap bitmap;

	AndroidGraphics(Canvas c) { canvas = c; paint.setFilterBitmap(false); paint.setAntiAlias(false); }

	@Override public void clearRect(int x, int y, int w, int h) { if (canvas != null) canvas.drawColor(0xFF000000); }

	@Override public void drawImage(BufferedImage src, int x, int y, int w, int h, Object observer) {
		if (canvas == null) return;
		long t0 = System.nanoTime();
		int sw = src.getWidth(), sh = src.getHeight();
		if (bitmap == null || bitmap.getWidth() != sw || bitmap.getHeight() != sh)
			bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
		int[] px = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
		if (src.getType() == BufferedImage.TYPE_INT_RGB) {
			int[] tmp = new int[px.length];
			for (int i = 0; i < px.length; i++) tmp[i] = px[i] | 0xFF000000;
			px = tmp;
		}
		bitmap.setPixels(px, 0, sw, 0, 0, sw, sh);
		canvas.drawBitmap(bitmap, null, new Rect(x, y, x + w, y + h), paint);
		minicraft.core.AndroidBridge.presentNs += System.nanoTime() - t0;
	}
}
