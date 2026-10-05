package com.zapata.minicraftplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.widget.Button;
import android.widget.FrameLayout;
import minicraft.core.AndroidBridge;

public class MainActivity extends Activity {
	private final InputRouter router = new InputRouter();
	private SharedPreferences prefs;
	private TouchOverlay overlay;
	private ImeView ime;
	private boolean textWasActive = false;
	private int imeHeight = 0;
	private final Handler handler = new Handler();

	@Override protected void onCreate(Bundle b) {
		super.onCreate(b);
		prefs = getSharedPreferences("minicraft", MODE_PRIVATE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		AndroidBridge.attach(this);

		FrameLayout root = new FrameLayout(this);
		root.setBackgroundColor(Color.BLACK);
		SurfaceView sv = new SurfaceView(this);
		root.addView(sv);
		overlay = new TouchOverlay(this, router);
		root.addView(overlay);
		Button gear = new Button(this);
		gear.setText("⚙");
		gear.setAlpha(0.35f);
		gear.setOnClickListener(v -> showSettings());
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(140, 140, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
		root.addView(gear, lp);
		ime = new ImeView(this);
		root.addView(ime, new FrameLayout.LayoutParams(1, 1));
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(root);
		root.setOnApplyWindowInsetsListener((v, insets) -> {
			imeHeight = insets.isVisible(android.view.WindowInsets.Type.ime()) ? insets.getInsets(android.view.WindowInsets.Type.ime()).bottom : 0;
			AndroidBridge.yShift = -imeHeight / 2;
			return insets;
		});
		pollTextFields();

		sv.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override public void surfaceCreated(SurfaceHolder h) {}
			@Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
				AndroidBridge.surfaceChanged(h, w, hh);
				AndroidBridge.startGame();
			}
			@Override public void surfaceDestroyed(SurfaceHolder h) { AndroidBridge.surfaceDestroyed(); }
		});
		applySettings();
	}

	/** Show Android's keyboard when the game focuses a text field; hide it when the field loses focus. */
	private void pollTextFields() {
		boolean active = AndroidBridge.textFieldActive();
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if (active && !textWasActive) {
			ime.requestFocus();
			imm.restartInput(ime);
			imm.showSoftInput(ime, InputMethodManager.SHOW_FORCED);
		} else if (!active && textWasActive) {
			imm.hideSoftInputFromWindow(ime.getWindowToken(), 0);
		}
		textWasActive = active;
		handler.postDelayed(this::pollTextFields, 100);
	}

	private boolean pref(String k, boolean def) { return prefs.getBoolean(k, def); }

	private void applySettings() {
		boolean pad = InputRouter.hasPhysicalGamepad();
		boolean touch = pref("touch", !pad); // default: on-screen controls only when there is no built-in pad
		overlay.setVisibility(touch ? View.VISIBLE : View.GONE);
		router.swapFace = pref("swap", false);
		AndroidBridge.crisp = pref("crisp", true);
		AndroidBridge.applyScale();
		Pad.present = pad || touch;
	}

	private void showSettings() {
		final boolean[] v = { pref("touch", !InputRouter.hasPhysicalGamepad()), pref("swap", false), pref("crisp", true) };
		new AlertDialog.Builder(this).setTitle("Minicraft+ controls & display")
			.setMultiChoiceItems(new String[] { "Show on-screen controls", "Swap A/B and X/Y (Nintendo layout)", "Crisp integer scaling (off = stretch to fit)" },
				v, (d, i, on) -> v[i] = on)
			.setPositiveButton("Apply", (d, w) -> {
				prefs.edit().putBoolean("touch", v[0]).putBoolean("swap", v[1]).putBoolean("crisp", v[2]).apply();
				applySettings();
			})
			.setNeutralButton("Control map", (d, w) -> new AlertDialog.Builder(this).setTitle("Control map").setMessage(
				"D-pad / left stick: move, menu cursor\nA: attack / select\nB (or Back): exit / back\nX: inventory / menu\nY: crafting\n"
				+ "L1: pick up   R1: drop one   R3: drop stack\nL2 / R2: previous / next tab\nStart: pause (and inventory search)\nD-pad / stick: hold to repeat in menus\nChest / creative picker: hold R1 + A = whole stack\nQuests screen: hold R1 + D-pad = scroll\n\n"
				+ "Hold SELECT +\n  A: quick-save\n  B: toggle HUD\n  X: potion effects\n  Y: player info\n  L1: screenshot\n  R1: simple potion list\n  L2 / R2: page up / down (search)\n\nL3: quest panel").setPositiveButton("OK", null).show())
			.setNegativeButton("Cancel", null).show();
	}

	@Override public void onWindowFocusChanged(boolean focus) {
		super.onWindowFocusChanged(focus);
		if (focus) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
	}

	@Override public boolean dispatchKeyEvent(KeyEvent e) {
		int kc = e.getKeyCode();
		boolean dpad = kc >= KeyEvent.KEYCODE_DPAD_UP && kc <= KeyEvent.KEYCODE_DPAD_RIGHT;
		if (InputRouter.isGamepadEvent(e) || kc == KeyEvent.KEYCODE_BACK || dpad) {
			if (router.onKey(e)) { Pad.present = true; return true; }
		}
		if (forwardKeyboard(e)) return true;
		return super.dispatchKeyEvent(e);
	}

	@Override public boolean dispatchGenericMotionEvent(MotionEvent e) {
		if (router.onMotion(e)) return true;
		return super.dispatchGenericMotionEvent(e);
	}

	/** Physical/Bluetooth keyboard -> the game's own keyboard bindings. */
	private boolean forwardKeyboard(KeyEvent e) {
		int vk = Keys.toVk(e.getKeyCode());
		if (vk < 0) return false;
		boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
		if (e.getAction() == KeyEvent.ACTION_MULTIPLE) return false;
		if (!down || e.getRepeatCount() == 0) AndroidBridge.key(vk, down);
		if (down) {
			int u = e.getUnicodeChar();
			if (u >= 32 && u != 127 && !e.isCtrlPressed()) AndroidBridge.typed((char) u);
		}
		return true;
	}
}
