package com.zapata.minicraftplus;

import android.view.KeyEvent;

/** Android keycode -> java.awt VK_ code for hardware keyboards. */
final class Keys {
	private Keys() {}
	static int toVk(int k) {
		if (k >= KeyEvent.KEYCODE_A && k <= KeyEvent.KEYCODE_Z) return 'A' + (k - KeyEvent.KEYCODE_A);
		if (k >= KeyEvent.KEYCODE_0 && k <= KeyEvent.KEYCODE_9) return '0' + (k - KeyEvent.KEYCODE_0);
		if (k >= KeyEvent.KEYCODE_F1 && k <= KeyEvent.KEYCODE_F12) return awtshim.event.KeyEvent.VK_F1 + (k - KeyEvent.KEYCODE_F1);
		switch (k) {
			case KeyEvent.KEYCODE_ENTER: return awtshim.event.KeyEvent.VK_ENTER;
			case KeyEvent.KEYCODE_ESCAPE: return awtshim.event.KeyEvent.VK_ESCAPE;
			case KeyEvent.KEYCODE_SPACE: return awtshim.event.KeyEvent.VK_SPACE;
			case KeyEvent.KEYCODE_DEL: return awtshim.event.KeyEvent.VK_BACK_SPACE;
			case KeyEvent.KEYCODE_FORWARD_DEL: return awtshim.event.KeyEvent.VK_DELETE;
			case KeyEvent.KEYCODE_TAB: return awtshim.event.KeyEvent.VK_TAB;
			case KeyEvent.KEYCODE_SHIFT_LEFT: case KeyEvent.KEYCODE_SHIFT_RIGHT: return awtshim.event.KeyEvent.VK_SHIFT;
			case KeyEvent.KEYCODE_CTRL_LEFT: case KeyEvent.KEYCODE_CTRL_RIGHT: return awtshim.event.KeyEvent.VK_CONTROL;
			case KeyEvent.KEYCODE_PAGE_UP: return awtshim.event.KeyEvent.VK_PAGE_UP;
			case KeyEvent.KEYCODE_PAGE_DOWN: return awtshim.event.KeyEvent.VK_PAGE_DOWN;
			case KeyEvent.KEYCODE_MOVE_HOME: return awtshim.event.KeyEvent.VK_HOME;
			case KeyEvent.KEYCODE_MOVE_END: return awtshim.event.KeyEvent.VK_END;
			case KeyEvent.KEYCODE_COMMA: return awtshim.event.KeyEvent.VK_COMMA;
			case KeyEvent.KEYCODE_PERIOD: return awtshim.event.KeyEvent.VK_PERIOD;
			case KeyEvent.KEYCODE_MINUS: return awtshim.event.KeyEvent.VK_MINUS;
			case KeyEvent.KEYCODE_SLASH: return awtshim.event.KeyEvent.VK_SLASH;
			default: return -1;
		}
	}
}
