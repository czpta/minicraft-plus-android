package awtshim.image;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Presents the game's 288x192 frame with OpenGL ES 2.0: one small texture upload per frame and one textured quad, scaled by
 * the GPU with nearest-neighbour filtering. The game thread only copies the frame (~0.05 ms); the GL thread owns the
 * swap/vsync wait, so presenting no longer costs the game thread a CPU blit of 1920x1080 pixels.
 */
public final class GlPresenter implements GLSurfaceView.Renderer {
	private static final String VS = "attribute vec2 aPos; attribute vec2 aUv; varying vec2 vUv;"
		+ "void main(){ gl_Position = vec4(aPos, 0.0, 1.0); vUv = aUv; }";
	// ints are 0xAARRGGBB; uploaded as RGBA bytes they arrive as (B,G,R,A) in memory order, so swizzle back
	private static final String FS = "precision mediump float; varying vec2 vUv; uniform sampler2D uTex;"
		+ "void main(){ vec4 c = texture2D(uTex, vUv); gl_FragColor = vec4(c.b, c.g, c.r, 1.0); }";

	private final GLSurfaceView view;
	private final Object lock = new Object();
	private IntBuffer buf;
	private int fw, fh, rx, ry, rw, rh;
	private boolean dirty;

	private int surfW, surfH, tex, prog, aPos, aUv, uTex, texW, texH;
	private FloatBuffer quad;

	public GlPresenter(GLSurfaceView v) { view = v; }

	/** Game thread: hand over the finished frame and where to draw it (surface pixels, top-left origin). */
	public void submit(int[] px, int w, int h, int x, int y, int dw, int dh) {
		synchronized (lock) {
			if (buf == null || fw != w || fh != h) {
				buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
				fw = w; fh = h;
			}
			buf.position(0);
			buf.put(px, 0, w * h);
			rx = x; ry = y; rw = dw; rh = dh;
			dirty = true;
		}
		view.requestRender();
	}

	@Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		int vs = compile(GLES20.GL_VERTEX_SHADER, VS), fs = compile(GLES20.GL_FRAGMENT_SHADER, FS);
		prog = GLES20.glCreateProgram();
		GLES20.glAttachShader(prog, vs); GLES20.glAttachShader(prog, fs); GLES20.glLinkProgram(prog);
		aPos = GLES20.glGetAttribLocation(prog, "aPos"); aUv = GLES20.glGetAttribLocation(prog, "aUv");
		uTex = GLES20.glGetUniformLocation(prog, "uTex");
		int[] t = new int[1];
		GLES20.glGenTextures(1, t, 0); tex = t[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		texW = texH = 0;                                  // (re)allocate on the first frame; the old context is gone after a pause
		// x, y, u, v - the first row of the frame is at the top of the quad
		float[] q = { -1, -1, 0, 1,   1, -1, 1, 1,   -1, 1, 0, 0,   1, 1, 1, 0 };
		quad = ByteBuffer.allocateDirect(q.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		quad.put(q).position(0);
		GLES20.glClearColor(0, 0, 0, 1);
	}

	@Override public void onSurfaceChanged(GL10 gl, int w, int h) {
		surfW = w; surfH = h;
		minicraft.core.AndroidBridge.surfaceChanged(null, w, h);
		minicraft.core.AndroidBridge.startGame();
	}

	@Override public void onDrawFrame(GL10 gl) {
		GLES20.glViewport(0, 0, surfW, surfH);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		int x, y, w, h;
		synchronized (lock) {
			if (buf == null) return;
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
			buf.position(0);
			if (texW != fw || texH != fh) {
				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, fw, fh, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
				texW = fw; texH = fh; dirty = false;
			} else if (dirty) {
				GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, fw, fh, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
				dirty = false;
			}
			x = rx; y = ry; w = rw; h = rh;
		}
		GLES20.glViewport(x, surfH - y - h, w, h);       // GL's origin is bottom-left
		GLES20.glUseProgram(prog);
		GLES20.glUniform1i(uTex, 0);
		quad.position(0);
		GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quad);
		GLES20.glEnableVertexAttribArray(aPos);
		quad.position(2);
		GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 16, quad);
		GLES20.glEnableVertexAttribArray(aUv);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	private static int compile(int type, String src) {
		int s = GLES20.glCreateShader(type);
		GLES20.glShaderSource(s, src); GLES20.glCompileShader(s);
		return s;
	}
}
