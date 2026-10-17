package org.wpilib.framework;

import java.util.Objects;
import java.util.PriorityQueue;
import org.wpilib.system.RobotController;

/** The nonblocking, host-clock equivalent of WPILib's notifier callback queue. */
final class HostPeriodicCallbacks {
  private static final class Callback implements Comparable<Callback> {
    final Runnable action;
    final long period;
    final long order;
    long deadline;

    Callback(Runnable action, long period, long order, long deadline) {
      this.action = action;
      this.period = period;
      this.order = order;
      this.deadline = deadline;
    }

    @Override
    public int compareTo(Callback other) {
      int timeOrder = Long.compare(deadline, other.deadline);
      return timeOrder != 0 ? timeOrder : Long.compare(order, other.order);
    }
  }

  private final PriorityQueue<Callback> callbacks = new PriorityQueue<>();
  private final long startTime = RobotController.getMonotonicTime();
  private long nextOrder;
  private long loopStartTime;

  static long periodNanos(double seconds) {
    if (!Double.isFinite(seconds) || seconds < 1e-9 || seconds >= Long.MAX_VALUE * 1e-9) {
      throw new IllegalArgumentException("Callback period must be a positive finite nanosecond duration");
    }
    return (long) (seconds * 1e9);
  }

  void add(Runnable action, double periodSeconds, double offsetSeconds) {
    long period = periodNanos(periodSeconds);
    if (!Double.isFinite(offsetSeconds) || Math.abs(offsetSeconds) >= Long.MAX_VALUE * 1e-9) {
      throw new IllegalArgumentException("Callback offset must be a finite nanosecond duration");
    }
    long offset = (long) (offsetSeconds * 1e9);
    long now = RobotController.getMonotonicTime();
    long deadline = startTime + offset + (1 + (now - startTime - offset) / period) * period;
    callbacks.add(new Callback(Objects.requireNonNull(action), period, nextOrder++, deadline));
  }

  void runDue() {
    long now = RobotController.getMonotonicTime();
    if (callbacks.isEmpty() || callbacks.peek().deadline > now) {
      return;
    }
    loopStartTime = now;
    while (!callbacks.isEmpty() && callbacks.peek().deadline <= now) {
      Callback callback = callbacks.remove();
      // Skip missed deadlines rather than replaying a burst. Advance before calling user code so
      // an exception or a JSPI suspension cannot cause the same expiration to run a second time.
      callback.deadline +=
          callback.period + (now - callback.deadline) / callback.period * callback.period;
      callbacks.add(callback);
      callback.action.run();
    }
  }

  long getLoopStartTime() {
    return loopStartTime;
  }

  void clear() {
    callbacks.clear();
  }
}
