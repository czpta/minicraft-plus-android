package com.zapata.minicraftplus;

import com.studiohartman.jamepad.ControllerButton;

/** Shared virtual-gamepad state read by the jamepad shim on the game thread. */
public final class Pad {
	private Pad() {}
	public static final boolean[] down = new boolean[ControllerButton.values().length];
	private static final boolean[] just = new boolean[down.length];
	public static volatile float lt, rt;
	/** True when a physical pad is attached or on-screen controls are on (game then shows pad prompts + on-screen keyboard). */
	public static volatile boolean present = false;

	public static synchronized void set(ControllerButton b, boolean pressed) {
		int i = b.ordinal();
		if (pressed && !down[i]) just[i] = true;
		down[i] = pressed;
	}
	public static synchronized boolean consumeJust(int i) { boolean v = just[i]; just[i] = false; return v; }
}
