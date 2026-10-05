package javax.swing;

import awtshim.event.ActionEvent;
import awtshim.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** javax.swing.Timer on a scheduler thread. */
public class Timer {
	private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "swing-timer-shim"); t.setDaemon(true); return t;
	});
	private int delay, initialDelay;
	private boolean repeats = true;
	private final ActionListener listener;
	private ScheduledFuture<?> future;

	public Timer(int delay, ActionListener l) { this.delay = delay; this.initialDelay = delay; this.listener = l; }
	public void setRepeats(boolean r) { repeats = r; }
	public void setInitialDelay(int d) { initialDelay = d; }
	public void setDelay(int d) { delay = d; }
	public boolean isRunning() { return future != null && !future.isDone(); }
	public synchronized void start() {
		stop();
		Runnable run = () -> listener.actionPerformed(new ActionEvent());
		future = repeats ? EXEC.scheduleWithFixedDelay(run, initialDelay, delay, TimeUnit.MILLISECONDS)
			: EXEC.schedule(run, initialDelay, TimeUnit.MILLISECONDS);
	}
	public synchronized void restart() { start(); }
	public synchronized void stop() { if (future != null) future.cancel(false); future = null; }
}
