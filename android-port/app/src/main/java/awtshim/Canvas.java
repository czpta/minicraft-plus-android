package awtshim;

import awtshim.image.BufferStrategy;

/** Stand-in for java.awt.Canvas backed by an Android SurfaceHolder (see BufferStrategy). */
public class Canvas extends Component {
	private BufferStrategy strategy;
	public volatile android.view.SurfaceHolder holder;
	/** When set, frames go to the GPU (OpenGL ES) instead of a CPU-locked canvas. */
	public volatile awtshim.image.GlPresenter presenter;

	public void setSize(int w, int h) { width = w; height = h; }
	public void createBufferStrategy(int n) { strategy = new BufferStrategy(this); }
	public BufferStrategy getBufferStrategy() {
		if (strategy == null) strategy = new BufferStrategy(this);
		return strategy;
	}
}
