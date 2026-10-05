package com.zapata.minicraftplus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import com.zapata.minicraftplus.InputRouter.Btn;
import java.util.EnumMap;
import java.util.Map;

/** Optional on-screen controls: d-pad, A/B/X/Y, L/R, Start, Select. Multi-touch, no state outside the router. */
public class TouchOverlay extends View {
	private final InputRouter router;
	private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Map<Btn, RectF> zones = new EnumMap<>(Btn.class);
	private final Map<Integer, Btn> pointers = new java.util.HashMap<>();

	public TouchOverlay(Context c, InputRouter r) {
		super(c);
		router = r;
		text.setColor(0xFFFFFFFF);
		text.setTextAlign(Paint.Align.CENTER);
		setVisibility(GONE);
	}

	@Override protected void onSizeChanged(int w, int h, int ow, int oh) {
		zones.clear();
		float u = Math.min(w, h) / 7f; // button size
		float dx = u * 2.0f, dy = h - u * 2.3f; // d-pad centre
		zones.put(Btn.UP, new RectF(dx - u / 2, dy - u * 1.5f, dx + u / 2, dy - u / 2));
		zones.put(Btn.DOWN, new RectF(dx - u / 2, dy + u / 2, dx + u / 2, dy + u * 1.5f));
		zones.put(Btn.LEFT, new RectF(dx - u * 1.5f, dy - u / 2, dx - u / 2, dy + u / 2));
		zones.put(Btn.RIGHT, new RectF(dx + u / 2, dy - u / 2, dx + u * 1.5f, dy + u / 2));
		float fx = w - u * 2.3f, fy = h - u * 2.3f;
		zones.put(Btn.Y, new RectF(fx - u / 2, fy - u * 1.5f, fx + u / 2, fy - u / 2));
		zones.put(Btn.A, new RectF(fx - u / 2, fy + u / 2, fx + u / 2, fy + u * 1.5f));
		zones.put(Btn.X, new RectF(fx - u * 1.5f, fy - u / 2, fx - u / 2, fy + u / 2));
		zones.put(Btn.B, new RectF(fx + u / 2, fy - u / 2, fx + u * 1.5f, fy + u / 2));
		zones.put(Btn.L1, new RectF(u * 0.5f, u * 0.3f, u * 2.3f, u * 1.1f));
		zones.put(Btn.R1, new RectF(w - u * 2.3f, u * 0.3f, w - u * 0.5f, u * 1.1f));
		zones.put(Btn.SELECT, new RectF(w / 2f - u * 1.6f, h - u * 0.9f, w / 2f - u * 0.2f, h - u * 0.3f));
		zones.put(Btn.START, new RectF(w / 2f + u * 0.2f, h - u * 0.9f, w / 2f + u * 1.6f, h - u * 0.3f));
		text.setTextSize(u * 0.4f);
	}

	@Override protected void onDraw(Canvas c) {
		for (Map.Entry<Btn, RectF> e : zones.entrySet()) {
			boolean on = pointers.containsValue(e.getKey());
			fill.setColor(on ? 0x88FFFFFF : 0x44FFFFFF);
			c.drawRoundRect(e.getValue(), 24, 24, fill);
			String label = e.getKey() == Btn.UP ? "^" : e.getKey() == Btn.DOWN ? "v" : e.getKey() == Btn.LEFT ? "<"
				: e.getKey() == Btn.RIGHT ? ">" : e.getKey() == Btn.L1 ? "L" : e.getKey() == Btn.R1 ? "R"
				: e.getKey() == Btn.SELECT ? "SEL" : e.getKey() == Btn.START ? "START" : e.getKey().name();
			c.drawText(label, e.getValue().centerX(), e.getValue().centerY() + text.getTextSize() / 3, text);
		}
	}

	private Btn hit(float x, float y) {
		for (Map.Entry<Btn, RectF> e : zones.entrySet()) if (e.getValue().contains(x, y)) return e.getKey();
		return null;
	}

	@Override public boolean onTouchEvent(MotionEvent ev) {
		int act = ev.getActionMasked();
		if (act == MotionEvent.ACTION_CANCEL) { releaseAll(); return true; }
		for (int i = 0; i < ev.getPointerCount(); i++) {
			int id = ev.getPointerId(i);
			boolean lifted = (act == MotionEvent.ACTION_UP || (act == MotionEvent.ACTION_POINTER_UP && ev.getActionIndex() == i));
			Btn now = lifted ? null : hit(ev.getX(i), ev.getY(i));
			Btn was = pointers.get(id);
			if (now == was) continue;
			if (was != null) router.set(was, false);
			if (now != null) { pointers.put(id, now); router.set(now, true); } else pointers.remove(id);
		}
		invalidate();
		return true;
	}

	private void releaseAll() {
		for (Btn b : pointers.values()) router.set(b, false);
		pointers.clear();
		invalidate();
	}
}
