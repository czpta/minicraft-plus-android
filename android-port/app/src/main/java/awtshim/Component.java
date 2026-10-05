package awtshim;

import awtshim.event.KeyListener;
import java.util.concurrent.CopyOnWriteArrayList;

public class Component {
	public final CopyOnWriteArrayList<KeyListener> keyListeners = new CopyOnWriteArrayList<>();
	protected int width, height;
	public void addKeyListener(KeyListener l) { keyListeners.add(l); }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public boolean hasFocus() { return true; }
	public void requestFocus() {}
	public Container getParent() { return new Container(); }
	public void setMinimumSize(Dimension d) {}
	public void setPreferredSize(Dimension d) {}
	public void setBackground(Color c) {}
}
