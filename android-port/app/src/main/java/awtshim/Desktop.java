package awtshim;

import java.io.IOException;
import java.net.URI;

/** Routes "open link" to the Android bridge. */
public class Desktop {
	public enum Action { BROWSE }
	private static final Desktop INSTANCE = new Desktop();
	public static boolean isDesktopSupported() { return true; }
	public static Desktop getDesktop() { return INSTANCE; }
	public boolean isSupported(Action a) { return true; }
	public void browse(URI uri) throws IOException { minicraft.core.AndroidBridge.openUrl(uri.toString()); }
}
