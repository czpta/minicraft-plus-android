package com.zapata.minicraftplus;

import android.content.Context;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import minicraft.core.AndroidBridge;

/** Invisible view that owns Android's soft keyboard and forwards what is typed to the game as key-typed events. */
public class ImeView extends View {
	public ImeView(Context c) {
		super(c);
		setFocusable(true);
		setFocusableInTouchMode(true);
	}

	@Override public boolean onCheckIsTextEditor() { return true; }

	@Override public InputConnection onCreateInputConnection(EditorInfo ei) {
		ei.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
		ei.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_DONE;
		return new Connection(this);
	}

	private static final class Connection extends BaseInputConnection {
		private String composing = "";

		Connection(View v) { super(v, false); }

		/** Turns "composing text is now X" into the backspaces and characters needed to get there. */
		private void apply(CharSequence text, boolean commit) {
			String t = text.toString();
			int p = 0;
			while (p < composing.length() && p < t.length() && composing.charAt(p) == t.charAt(p)) p++;
			for (int i = composing.length() - p; i > 0; i--) AndroidBridge.typed('\b');
			for (int i = p; i < t.length(); i++) AndroidBridge.typed(t.charAt(i));
			composing = commit ? "" : t;
		}

		private void enter() {
			AndroidBridge.key(awtshim.event.KeyEvent.VK_ENTER, true);
			// hold for a few ticks so the game's per-tick key polling can't miss the press
			new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
				() -> AndroidBridge.key(awtshim.event.KeyEvent.VK_ENTER, false), 80);
		}

		@Override public boolean commitText(CharSequence text, int newCursorPosition) { apply(text, true); return true; }
		@Override public boolean setComposingText(CharSequence text, int newCursorPosition) { apply(text, false); return true; }
		@Override public boolean finishComposingText() { composing = ""; return true; }
		@Override public boolean deleteSurroundingText(int before, int after) {
			for (int i = 0; i < before; i++) AndroidBridge.typed('\b');
			composing = composing.substring(0, Math.max(0, composing.length() - before));
			return true;
		}
		@Override public boolean performEditorAction(int actionCode) { enter(); return true; }
		@Override public boolean sendKeyEvent(KeyEvent e) {
			if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
			if (e.getKeyCode() == KeyEvent.KEYCODE_DEL) AndroidBridge.typed('\b');
			else if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) enter();
			else { int u = e.getUnicodeChar(); if (u >= 32) AndroidBridge.typed((char) u); }
			return true;
		}
		@Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
		@Override public CharSequence getTextAfterCursor(int n, int flags) { return ""; }
		@Override public boolean beginBatchEdit() { return true; }
		@Override public boolean endBatchEdit() { return true; }
	}
}
