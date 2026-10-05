package javax.imageio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import awtshim.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** ImageIO via Android's BitmapFactory (read) and Bitmap.compress (write). */
public class ImageIO {
	public static BufferedImage read(InputStream in) throws IOException {
		if (in == null) throw new IllegalArgumentException("input == null!");
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inPreferredConfig = Bitmap.Config.ARGB_8888;
		o.inPremultiplied = false;
		o.inScaled = false;
		Bitmap b = BitmapFactory.decodeStream(in, null, o);
		if (b == null) return null;
		int w = b.getWidth(), h = b.getHeight();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] px = new int[w * h];
		b.getPixels(px, 0, w, 0, 0, w, h);
		System.arraycopy(px, 0, ((awtshim.image.DataBufferInt) img.getRaster().getDataBuffer()).getData(), 0, px.length);
		b.recycle();
		return img;
	}

	public static boolean write(BufferedImage img, String format, File f) throws IOException {
		try (OutputStream out = new FileOutputStream(f)) { return write(img, format, out); }
	}

	public static boolean write(BufferedImage img, String format, OutputStream out) throws IOException {
		int w = img.getWidth(), h = img.getHeight();
		int[] px = new int[w * h];
		img.getRGB(0, 0, w, h, px, 0, w);
		Bitmap b = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
		boolean ok = b.compress(Bitmap.CompressFormat.PNG, 100, out);
		b.recycle();
		return ok;
	}
}
