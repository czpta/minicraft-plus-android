package awtshim;

import awtshim.image.BufferStrategy;

/** Stand-in for java.awt.Canvas backed by an Android SurfaceHolder (see BufferStrategy). */
public class Canvas extends Component {
	private BufferStrategy strategy;
	public volatile android.view.SurfaceHolder holder;

	public void setSize(int w, int h) { width = w; height = h; }
	public void createBufferStrategy(int n) { strategy = new BufferStrategy(this); }
	public BufferStrategy getBufferStrategy() {
		if (strategy == null) strategy = new BufferStrategy(this);
		return strategy;
	}
}
