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
		if (getIntent().getStringExtra("font") != null) prefs.edit().putString("font", getIntent().getStringExtra("font")).apply(); // test hook
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		AndroidBridge.attach(this);

		FrameLayout root = new FrameLayout(this);
		root.setBackgroundColor(Color.BLACK);
		SurfaceView sv = new SurfaceView(this);
		root.addView(sv);
		overlay = new TouchOverlay(this, router);
		root.addView(overlay);
		gear = new Button(this);
		gear.setText("⚙");
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

	private Button gear;

	private void applySettings() {
		boolean pad = InputRouter.hasPhysicalGamepad();
		boolean touch = pref("touch", !pad); // default: on-screen controls only when there is no built-in pad
		overlay.setVisibility(touch ? View.VISIBLE : View.GONE);
		overlay.setAlpha(prefs.getInt("touchAlpha", 100) / 100f);
		if (gear != null) gear.setAlpha(prefs.getInt("gearAlpha", 35) / 100f);
		router.swapFace = pref("swap", false);
		AndroidBridge.crisp = pref("crisp", true);
		AndroidBridge.applyScale();
		Pad.present = pad || touch;
	}

	private android.widget.SeekBar slider(android.widget.LinearLayout box, String label, int value, java.util.function.IntConsumer live) {
		android.widget.TextView t = new android.widget.TextView(this);
		t.setText(label + ": " + value + "%");
		t.setPadding(0, 24, 0, 0);
		android.widget.SeekBar sb = new android.widget.SeekBar(this);
		sb.setMax(90); // 10..100 %
		sb.setProgress(value - 10);
		sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
			@Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean user) {
				t.setText(label + ": " + (p + 10) + "%");
				live.accept(p + 10);
			}
			@Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
			@Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
		});
		box.addView(t);
		box.addView(sb);
		return sb;
	}

	private void showSettings() {
		final boolean[] v = { pref("touch", !InputRouter.hasPhysicalGamepad()), pref("swap", false), pref("crisp", true) };
		final String oldFont = "monocraft".equals(prefs.getString("font", "classic")) ? "monocraft" : "classic";
		final int oldGear = prefs.getInt("gearAlpha", 35), oldTouch = prefs.getInt("touchAlpha", 100);

		android.widget.LinearLayout box = new android.widget.LinearLayout(this);
		box.setOrientation(android.widget.LinearLayout.VERTICAL);
		box.setPadding(48, 24, 48, 0);
		String[] labels = { "Show on-screen controls", "Swap A/B and X/Y (Nintendo layout)", "Crisp integer scaling (off = stretch to fit)" };
		for (int i = 0; i < labels.length; i++) {
			final int idx = i;
			android.widget.CheckBox cb = new android.widget.CheckBox(this);
			cb.setText(labels[i]); cb.setChecked(v[i]);
			cb.setOnCheckedChangeListener((c, on) -> v[idx] = on);
			box.addView(cb);
		}
		android.widget.TextView ft = new android.widget.TextView(this);
		ft.setText("Game font (restart to apply)"); ft.setPadding(0, 24, 0, 0);
		box.addView(ft);
		android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
		String[][] fonts = { { "classic", "Classic" }, { "monocraft", "Minecraft style (Monocraft, free OFL font)" } };
		for (String[] f : fonts) {
			android.widget.RadioButton rb = new android.widget.RadioButton(this);
			rb.setText(f[1]); rb.setTag(f[0]); rb.setId(View.generateViewId());
			rg.addView(rb);
			if (f[0].equals(oldFont)) rg.check(rb.getId());
		}
		box.addView(rg);
		final int[] gearVal = { oldGear }, touchVal = { oldTouch };
		slider(box, "Settings button opacity", oldGear, p -> { gearVal[0] = p; gear.setAlpha(p / 100f); });
		slider(box, "On-screen controls opacity", oldTouch, p -> { touchVal[0] = p; overlay.setAlpha(p / 100f); });

		android.widget.ScrollView sv = new android.widget.ScrollView(this);
		sv.addView(box);
		new AlertDialog.Builder(this).setTitle("Minicraft+ controls & display").setView(sv)
			.setPositiveButton("Apply", (d, w) -> {
				View sel = rg.findViewById(rg.getCheckedRadioButtonId());
				String font = sel != null ? (String) sel.getTag() : oldFont;
				prefs.edit().putBoolean("touch", v[0]).putBoolean("swap", v[1]).putBoolean("crisp", v[2])
					.putString("font", font).putInt("gearAlpha", gearVal[0]).putInt("touchAlpha", touchVal[0]).apply();
				applySettings();
				if (!font.equals(oldFont)) {
					new AlertDialog.Builder(this).setTitle("Restart to change font")
						.setMessage("The new font applies on the next start. Restarting now loses unsaved progress (quick-save with Select + A first).")
						.setPositiveButton("Restart now", (d2, w2) -> restartApp())
						.setNegativeButton("Later", null).show();
				}
			})
			.setNeutralButton("Control map", (d, w) -> new AlertDialog.Builder(this).setTitle("Control map").setMessage(
				"D-pad / left stick: move, menu cursor\nA: attack / select\nB (or Back): exit / back\nX: inventory / menu\nY: crafting\n"
				+ "L1: pick up   R1: drop one   R3: drop stack\nL2 / R2: previous / next tab\nStart: pause (and inventory search)\nD-pad / stick: hold to repeat in menus\nChest / creative picker: hold R1 + A = whole stack\nQuests screen: hold R1 + D-pad = scroll\n\n"
				+ "Hold SELECT +\n  A: quick-save\n  B: toggle HUD\n  X: potion effects\n  Y: player info\n  L1: screenshot\n  R1: simple potion list\n  L2 / R2: page up / down (search)\n\nL3: quest panel").setPositiveButton("OK", null).show())
			.setNegativeButton("Cancel", (d, w) -> {  // undo the live opacity preview
				gear.setAlpha(oldGear / 100f); overlay.setAlpha(oldTouch / 100f);
			})
			.setOnCancelListener(d -> { gear.setAlpha(oldGear / 100f); overlay.setAlpha(oldTouch / 100f); })
			.show();
	}

	private void restartApp() {
		android.content.Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
		i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
		Runtime.getRuntime().exit(0);
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
