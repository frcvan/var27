package org.wpilib.framework;

import static org.wpilib.units.Units.Seconds;

import org.wpilib.units.measure.Frequency;
import org.wpilib.units.measure.Time;

/**
 * Iterative robot framework scheduled by the host's monotonic clock instead of a HAL notifier.
 *
 * <p>Host ticks drain due callbacks without waiting. Periods shorter than the host tick interval are
 * quantized to that interval; missed expirations are skipped rather than replayed. The default
 * 20 ms robot period therefore runs once per normal 20 ms host tick, including while disabled.
 */
public class TimedRobot extends IterativeRobotBase {
  public static final double DEFAULT_PERIOD = 0.02;

  private final HostPeriodicCallbacks callbacks = new HostPeriodicCallbacks();

  protected TimedRobot() {
    this(DEFAULT_PERIOD);
  }

  protected TimedRobot(double period) {
    super(period);
    addPeriodic(this::loopFunc, period);
  }

  protected TimedRobot(Time period) {
    this(period.in(Seconds));
  }

  protected TimedRobot(Frequency frequency) {
    this(frequency.asPeriod());
  }

  /** Initializes simulation exactly once and returns; no native alarm or thread is created. */
  @Override
  public void startCompetition() {
    if (beginCompetition()) {
      simulationInit();
    }
  }

  @Override
  protected void wasmLoop() {
    callbacks.runDue();
  }

  @Override
  public void endCompetition() {
    stopCompetition();
    callbacks.clear();
  }

  public long getLoopStartTime() {
    return callbacks.getLoopStartTime();
  }

  public final void addPeriodic(Runnable callback, double period) {
    addPeriodic(callback, period, period);
  }

  public final void addPeriodic(Runnable callback, double period, double offset) {
    callbacks.add(callback, period, offset);
  }
}
