package awtshim;
public class Rectangle {
	public int x, y, width, height;
	public Rectangle() {}
	public Rectangle(int x, int y, int w, int h) { this.x = x; this.y = y; width = w; height = h; }
	public boolean intersects(Rectangle r) {
		return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
	}
	public boolean contains(int px, int py) { return px >= x && py >= y && px < x + width && py < y + height; }
}
