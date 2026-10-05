package com.studiohartman.jamepad;

import com.zapata.minicraftplus.Pad;

/** jamepad's API, fed by Android input (see com.zapata.minicraftplus.InputRouter). */
public class ControllerManager {
	public void initSDLGamepad() {}
	public void update() {}
	public int getNumControllers() { return Pad.present ? 1 : 0; }
	public ControllerIndex getControllerIndex(int i) { return new ControllerIndex(); }
	public void quitSDLGamepad() {}
}
