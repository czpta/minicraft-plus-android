package awtshim.event;

import awtshim.Component;

/** java.awt.event.KeyEvent look-alike: VK_* constants (names drive InputHandler's key table) + the accessors it uses. */
public class KeyEvent {
	public static final int KEY_TYPED = 400, KEY_PRESSED = 401, KEY_RELEASED = 402;
	public static final int VK_A = 65;
	public static final int VK_B = 66;
	public static final int VK_C = 67;
	public static final int VK_D = 68;
	public static final int VK_E = 69;
	public static final int VK_F = 70;
	public static final int VK_G = 71;
	public static final int VK_H = 72;
	public static final int VK_I = 73;
	public static final int VK_J = 74;
	public static final int VK_K = 75;
	public static final int VK_L = 76;
	public static final int VK_M = 77;
	public static final int VK_N = 78;
	public static final int VK_O = 79;
	public static final int VK_P = 80;
	public static final int VK_Q = 81;
	public static final int VK_R = 82;
	public static final int VK_S = 83;
	public static final int VK_T = 84;
	public static final int VK_U = 85;
	public static final int VK_V = 86;
	public static final int VK_W = 87;
	public static final int VK_X = 88;
	public static final int VK_Y = 89;
	public static final int VK_Z = 90;
	public static final int VK_0 = 48;
	public static final int VK_1 = 49;
	public static final int VK_2 = 50;
	public static final int VK_3 = 51;
	public static final int VK_4 = 52;
	public static final int VK_5 = 53;
	public static final int VK_6 = 54;
	public static final int VK_7 = 55;
	public static final int VK_8 = 56;
	public static final int VK_9 = 57;
	public static final int VK_ENTER = 10;
	public static final int VK_BACK_SPACE = 8;
	public static final int VK_TAB = 9;
	public static final int VK_CANCEL = 3;
	public static final int VK_CLEAR = 12;
	public static final int VK_SHIFT = 16;
	public static final int VK_CONTROL = 17;
	public static final int VK_ALT = 18;
	public static final int VK_PAUSE = 19;
	public static final int VK_CAPS_LOCK = 20;
	public static final int VK_ESCAPE = 27;
	public static final int VK_SPACE = 32;
	public static final int VK_PAGE_UP = 33;
	public static final int VK_PAGE_DOWN = 34;
	public static final int VK_END = 35;
	public static final int VK_HOME = 36;
	public static final int VK_LEFT = 37;
	public static final int VK_UP = 38;
	public static final int VK_RIGHT = 39;
	public static final int VK_DOWN = 40;
	public static final int VK_COMMA = 44;
	public static final int VK_MINUS = 45;
	public static final int VK_PERIOD = 46;
	public static final int VK_SLASH = 47;
	public static final int VK_SEMICOLON = 59;
	public static final int VK_EQUALS = 61;
	public static final int VK_OPEN_BRACKET = 91;
	public static final int VK_BACK_SLASH = 92;
	public static final int VK_CLOSE_BRACKET = 93;
	public static final int VK_DELETE = 127;
	public static final int VK_INSERT = 155;
	public static final int VK_META = 157;
	public static final int VK_BACK_QUOTE = 192;
	public static final int VK_QUOTE = 222;
	public static final int VK_UNDEFINED = 0;
	public static final int VK_NUMPAD0 = 96;
	public static final int VK_NUMPAD1 = 97;
	public static final int VK_NUMPAD2 = 98;
	public static final int VK_NUMPAD3 = 99;
	public static final int VK_NUMPAD4 = 100;
	public static final int VK_NUMPAD5 = 101;
	public static final int VK_NUMPAD6 = 102;
	public static final int VK_NUMPAD7 = 103;
	public static final int VK_NUMPAD8 = 104;
	public static final int VK_NUMPAD9 = 105;
	public static final int VK_MULTIPLY = 106;
	public static final int VK_ADD = 107;
	public static final int VK_SUBTRACT = 109;
	public static final int VK_DECIMAL = 110;
	public static final int VK_DIVIDE = 111;
	public static final int VK_F1 = 112;
	public static final int VK_F2 = 113;
	public static final int VK_F3 = 114;
	public static final int VK_F4 = 115;
	public static final int VK_F5 = 116;
	public static final int VK_F6 = 117;
	public static final int VK_F7 = 118;
	public static final int VK_F8 = 119;
	public static final int VK_F9 = 120;
	public static final int VK_F10 = 121;
	public static final int VK_F11 = 122;
	public static final int VK_F12 = 123;

	private final int id, keyCode;
	private final char keyChar;
	private final long when;

	public KeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar) {
		this.id = id; this.when = when; this.keyCode = keyCode; this.keyChar = keyChar;
	}
	public int getID() { return id; }
	public int getKeyCode() { return keyCode; }
	public int getExtendedKeyCode() { return keyCode; }
	public char getKeyChar() { return keyChar; }
	public long getWhen() { return when; }
}
