package minicraft.core.io;

import minicraft.core.AndroidBridge;

public final class ClipboardHandler {
	/** Give the system clipboard data. */
	public void setClipboardContents(String string) { AndroidBridge.setClipboard(string); }

	/** Get the string from the system clipboard data. */
	public String getClipboardContents() { return AndroidBridge.getClipboard(); }
}
