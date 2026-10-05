package com.studiohartman.jamepad;

import com.zapata.minicraftplus.Pad;

public class ControllerIndex {
	public boolean isButtonPressed(ControllerButton b) throws ControllerUnpluggedException { return Pad.down[b.ordinal()]; }
	/** Latched press edge; cleared when read (InputHandler reads each button once per tick). */
	public boolean isButtonJustPressed(ControllerButton b) throws ControllerUnpluggedException { return Pad.consumeJust(b.ordinal()); }
	public float getAxisState(ControllerAxis a) throws ControllerUnpluggedException {
		switch (a) { case TRIGGERLEFT: return Pad.lt; case TRIGGERRIGHT: return Pad.rt; default: return 0f; }
	}
	public String getName() throws ControllerUnpluggedException { return "Android gamepad"; }
	public boolean isConnected() { return Pad.present; }
	public boolean doVibration(float left, float right, int ms) throws ControllerUnpluggedException { return false; }
}
