package com.zapata.minicraftplus;

import android.view.InputDevice;
import android.view.MotionEvent;
import com.studiohartman.jamepad.ControllerButton;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import minicraft.core.AndroidBridge;

/**
 * Translates AYN Thor (or any Android gamepad) and the on-screen controls into the game's own controller scheme.
 *
 * Game's native pad bindings (unchanged): D-pad move/cursor, A attack/select, B exit, X menu/inventory, Y craft,
 * L1 pick up, R1 drop one, R3 drop stack, Start pause/search, L2/R2 page tabs.
 * Actions the game only has keyboard keys for are reached with SELECT held (SELECT + button), plus L3 alone:
 *   SELECT+A quick-save (R)   SELECT+B toggle HUD (F1)   SELECT+X potion effects (P)   SELECT+Y player info (Shift+I)
 *   SELECT+L1 screenshot (F2) SELECT+R1 simple potion list (O)   SELECT+L2/R2 page up/down (inventory search)
 *   L3 quest panel (L)
 * A held D-pad direction auto-repeats in menus (like a keyboard key), after 400 ms at ~11 steps/s.
 */
public final class InputRouter {
	public enum Btn { UP, DOWN, LEFT, RIGHT, A, B, X, Y, L1, R1, L2, R2, L3, R3, START, SELECT }

	private static final Map<Btn, int[]> COMBOS = new EnumMap<>(Btn.class);
	static {
		COMBOS.put(Btn.A, new int[] { awtshim.event.KeyEvent.VK_R });
		COMBOS.put(Btn.B, new int[] { awtshim.event.KeyEvent.VK_F1 });
		COMBOS.put(Btn.X, new int[] { awtshim.event.KeyEvent.VK_P });
		COMBOS.put(Btn.Y, new int[] { awtshim.event.KeyEvent.VK_SHIFT, awtshim.event.KeyEvent.VK_I });
		COMBOS.put(Btn.L1, new int[] { awtshim.event.KeyEvent.VK_F2 });
		COMBOS.put(Btn.R1, new int[] { awtshim.event.KeyEvent.VK_O });
		COMBOS.put(Btn.L2, new int[] { awtshim.event.KeyEvent.VK_PAGE_UP });   // inventory search: previous match
		COMBOS.put(Btn.R2, new int[] { awtshim.event.KeyEvent.VK_PAGE_DOWN }); // inventory search: next match
	}

	private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
	private boolean repeating = false;
	private final Runnable repeat = new Runnable() {
		@Override public void run() {
			synchronized (InputRouter.this) {
				boolean any = false;
				if (held.contains(Btn.UP)) { Pad.pulse(ControllerButton.DPAD_UP); any = true; }
				if (held.contains(Btn.DOWN)) { Pad.pulse(ControllerButton.DPAD_DOWN); any = true; }
				if (held.contains(Btn.LEFT)) { Pad.pulse(ControllerButton.DPAD_LEFT); any = true; }
				if (held.contains(Btn.RIGHT)) { Pad.pulse(ControllerButton.DPAD_RIGHT); any = true; }
				repeating = any;
				if (any) handler.postDelayed(this, 90);
			}
		}
	};

	public volatile boolean swapFace = false; // Nintendo-style A<->B, X<->Y
	private boolean selectHeld = false;
	private final EnumSet<Btn> held = EnumSet.noneOf(Btn.class);
	private final EnumSet<Btn> comboActive = EnumSet.noneOf(Btn.class);
	// direction sources are merged so a stick, hat and d-pad key can't cancel each other
	private final boolean[] dirKey = new boolean[4], dirStick = new boolean[4], dirHat = new boolean[4];

	public synchronized void set(Btn b, boolean down) {
		if (down == held.contains(b)) return;
		if (down) held.add(b); else held.remove(b);

		if (b == Btn.SELECT) {
			selectHeld = down;
			if (!down) for (Btn c : EnumSet.copyOf(comboActive)) releaseCombo(c);
			return;
		}
		if (!down && comboActive.contains(b)) { releaseCombo(b); return; }
		if (down && selectHeld && COMBOS.containsKey(b)) {
			comboActive.add(b);
			for (int vk : COMBOS.get(b)) AndroidBridge.key(vk, true);
			return;
		}
		switch (b) {
			case UP: Pad.set(ControllerButton.DPAD_UP, down); break;
			case DOWN: Pad.set(ControllerButton.DPAD_DOWN, down); break;
			case LEFT: Pad.set(ControllerButton.DPAD_LEFT, down); break;
			case RIGHT: Pad.set(ControllerButton.DPAD_RIGHT, down); break;
			case A: Pad.set(swapFace ? ControllerButton.B : ControllerButton.A, down); break;
			case B: Pad.set(swapFace ? ControllerButton.A : ControllerButton.B, down); break;
			case X: Pad.set(swapFace ? ControllerButton.Y : ControllerButton.X, down); break;
			case Y: Pad.set(swapFace ? ControllerButton.X : ControllerButton.Y, down); break;
			case L1: Pad.set(ControllerButton.LEFTBUMPER, down); break;
			case R1: Pad.set(ControllerButton.RIGHTBUMPER, down); break;
			case R3: Pad.set(ControllerButton.RIGHTSTICK, down); break;
			case START: Pad.set(ControllerButton.START, down); break;
			case L2: Pad.lt = down ? 1f : 0f; break;
			case R2: Pad.rt = down ? 1f : 0f; break;
			case L3: AndroidBridge.key(awtshim.event.KeyEvent.VK_L, down); break;
			default: break;
		}
		if (!repeating && (held.contains(Btn.UP) || held.contains(Btn.DOWN) || held.contains(Btn.LEFT) || held.contains(Btn.RIGHT))) {
			repeating = true;
			handler.postDelayed(repeat, 400);
		}
	}

	private void releaseCombo(Btn b) {
		comboActive.remove(b);
		int[] keys = COMBOS.get(b);
		for (int i = keys.length - 1; i >= 0; i--) AndroidBridge.key(keys[i], false);
	}

	private void dir(boolean[] src, int idx, boolean on) {
		src[idx] = on;
		Btn[] d = { Btn.UP, Btn.DOWN, Btn.LEFT, Btn.RIGHT };
		set(d[idx], dirKey[idx] || dirStick[idx] || dirHat[idx]);
	}

	// ---- physical input ----
	public static boolean isGamepadEvent(android.view.KeyEvent e) {
		int s = e.getSource();
		return (s & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
			|| (s & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
			|| (s & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
	}

	/** @return true if consumed. */
	public synchronized boolean onKey(android.view.KeyEvent e) {
		boolean down = e.getAction() == android.view.KeyEvent.ACTION_DOWN;
		if (e.getAction() != android.view.KeyEvent.ACTION_DOWN && e.getAction() != android.view.KeyEvent.ACTION_UP) return false;
		Btn b = null;
		switch (e.getKeyCode()) {
			case android.view.KeyEvent.KEYCODE_DPAD_UP: dir(dirKey, 0, down); return true;
			case android.view.KeyEvent.KEYCODE_DPAD_DOWN: dir(dirKey, 1, down); return true;
			case android.view.KeyEvent.KEYCODE_DPAD_LEFT: dir(dirKey, 2, down); return true;
			case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: dir(dirKey, 3, down); return true;
			case android.view.KeyEvent.KEYCODE_BUTTON_A: b = Btn.A; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_B: b = Btn.B; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_X: b = Btn.X; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_Y: b = Btn.Y; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_L1: b = Btn.L1; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_R1: b = Btn.R1; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_L2: b = Btn.L2; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_R2: b = Btn.R2; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_THUMBL: b = Btn.L3; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_THUMBR: b = Btn.R3; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_START: b = Btn.START; break;
			case android.view.KeyEvent.KEYCODE_BUTTON_SELECT: b = Btn.SELECT; break;
			case android.view.KeyEvent.KEYCODE_BACK: b = Btn.B; break; // Thor's Back key = game "exit"
			default: return false;
		}
		if (e.getRepeatCount() > 0) return true;
		set(b, down);
		return true;
	}

	public synchronized boolean onMotion(MotionEvent e) {
		if ((e.getSource() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK || e.getAction() != MotionEvent.ACTION_MOVE) return false;
		float x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
		dir(dirStick, 0, y < -0.5f); dir(dirStick, 1, y > 0.5f);
		dir(dirStick, 2, x < -0.5f); dir(dirStick, 3, x > 0.5f);
		float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
		dir(dirHat, 0, hy < -0.5f); dir(dirHat, 1, hy > 0.5f);
		dir(dirHat, 2, hx < -0.5f); dir(dirHat, 3, hx > 0.5f);
		float l = Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE));
		float r = Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER), e.getAxisValue(MotionEvent.AXIS_GAS));
		if (selectHeld) { Pad.lt = 0f; Pad.rt = 0f; } // SELECT+L2/R2 are page keys, not trigger actions
		else {
			if (l > 0 || !held.contains(Btn.L2)) Pad.lt = Math.max(l, held.contains(Btn.L2) ? 1f : 0f);
			if (r > 0 || !held.contains(Btn.R2)) Pad.rt = Math.max(r, held.contains(Btn.R2) ? 1f : 0f);
		}
		return true;
	}

	public static boolean hasPhysicalGamepad() {
		for (int id : InputDevice.getDeviceIds()) {
			InputDevice d = InputDevice.getDevice(id);
			if (d == null) continue;
			int s = d.getSources();
			if (((s & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) || ((s & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) return true;
		}
		return false;
	}
}
