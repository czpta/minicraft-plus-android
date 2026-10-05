package awtshim.image;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import awtshim.Graphics2D;

/** Lock the SurfaceView canvas in getDrawGraphics(), post it in show(). */
public class BufferStrategy {
	private final awtshim.Canvas owner;
	private Canvas locked;
	private SurfaceHolder lockedHolder;

	public BufferStrategy(awtshim.Canvas owner) { this.owner = owner; }

	public Graphics2D getDrawGraphics() {
		SurfaceHolder h = owner.holder;
		locked = null;
		if (h != null && h.getSurface() != null && h.getSurface().isValid()) {
			try {
				locked = h.lockCanvas();
				lockedHolder = h;
			} catch (Throwable t) {
				try { locked = h.lockCanvas(); lockedHolder = h; } catch (Throwable ignored) { locked = null; }
			}
		}
		return new AndroidGraphics(locked);
	}

	public void show() {
		if (locked != null && lockedHolder != null) {
			try { lockedHolder.unlockCanvasAndPost(locked); } catch (Throwable ignored) {}
		}
		locked = null;
	}
}
