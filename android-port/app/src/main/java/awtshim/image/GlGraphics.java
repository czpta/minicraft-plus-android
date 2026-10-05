package awtshim.image;

import awtshim.Graphics2D;

/** Graphics2D for the GL path: the game's finished frame is handed to the GPU presenter; nothing is drawn on the CPU. */
class GlGraphics extends Graphics2D {
	private final GlPresenter presenter;

	GlGraphics(GlPresenter p) { presenter = p; }

	@Override public void clearRect(int x, int y, int w, int h) { /* the GL pass clears */ }

	@Override public void drawImage(BufferedImage src, int x, int y, int w, int h, Object observer) {
		long t0 = System.nanoTime();
		presenter.submit(((DataBufferInt) src.getRaster().getDataBuffer()).getData(), src.getWidth(), src.getHeight(), x, y, w, h);
		minicraft.core.AndroidBridge.presentNs += System.nanoTime() - t0;
	}
}
