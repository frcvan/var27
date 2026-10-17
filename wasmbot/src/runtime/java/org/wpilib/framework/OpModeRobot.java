package org.wpilib.framework;

import java.util.function.Supplier;
import org.wpilib.driverstation.RobotState;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.opmode.OpMode;
import org.wpilib.tunable.TunableRegistry;
import org.wpilib.util.Color;

/**
 * Host-driven implementation of the WPILib 2027 opmode robot lifecycle.
 *
 * <p>The browser driver station has no opmode selection. Consequently {@link #nonePeriodic()} runs
 * before {@link #robotPeriodic()} on every robot-loop expiration, even when enabled. There is no
 * classpath scanning, native notifier, watchdog, or implicit command-scheduler invocation. Explicit
 * opmode registration fails rather than advertising a mode the host cannot select.
 */
public abstract class OpModeRobot extends RobotBase {
  public static final double DEFAULT_PERIOD = 0.02;

  private final HostPeriodicCallbacks callbacks = new HostPeriodicCallbacks();
  private final double period;
  private boolean calledDriverStationConnected;
  private boolean lastEnabled;

  public OpModeRobot() {
    this(DEFAULT_PERIOD);
  }

  public OpModeRobot(double period) {
    this.period = period;
    addPeriodic(this::loopFunc, period);
  }

  /** Registers a phase-aligned callback; only due callbacks run when the host next ticks. */
  public void addPeriodic(Runnable callback, double period) {
    callbacks.add(callback, period, 0);
  }

  public double getPeriod() {
    return period;
  }

  public long getLoopStartTime() {
    return callbacks.getLoopStartTime();
  }

  public void driverStationConnected() {}

  public void robotPeriodic() {}

  public void simulationInit() {}

  public void simulationPeriodic() {}

  public void disabledInit() {}

  public void disabledPeriodic() {}

  public void disabledExit() {}

  public void nonePeriodic() {}

  private void loopFunc() {
    boolean enabled = RobotState.isEnabled();
    if (!calledDriverStationConnected && RobotState.isDSAttached()) {
      calledDriverStationConnected = true;
      driverStationConnected();
    }

    // Match OpModeRobot, whose initial state is disabled: disabledInit is only a transition hook,
    // and disabledPeriodic is skipped on the same loop that calls disabledInit.
    boolean justDisabled = false;
    if (lastEnabled != enabled) {
      if (enabled) {
        disabledExit();
      } else {
        disabledInit();
        justDisabled = true;
      }
      lastEnabled = enabled;
    }
    if (!enabled && !justDisabled) {
      disabledPeriodic();
    }
    nonePeriodic();
    robotPeriodic();
    TunableRegistry.update();
    simulationPeriodic();
  }

  /** Initializes simulation exactly once and returns; the host drives the callback queue. */
  @Override
  public final void startCompetition() {
    if (beginCompetition()) {
      simulationInit();
    }
  }

  @Override
  protected final void wasmLoop() {
    callbacks.runDue();
  }

  @Override
  public final void endCompetition() {
    stopCompetition();
    callbacks.clear();
  }

  private static UnsupportedOperationException unsupportedOpModes() {
    return new UnsupportedOperationException("The browser driver station does not support opmode selection");
  }

  public void addOpMode(
      RobotMode mode,
      String name,
      String group,
      String description,
      Color textColor,
      Color backgroundColor,
      Supplier<OpMode> factory) {
    throw unsupportedOpModes();
  }

  public void addOpMode(
      RobotMode mode, String name, String group, String description, Supplier<OpMode> factory) {
    addOpMode(mode, name, group, description, null, null, factory);
  }

  public void addOpMode(RobotMode mode, String name, String group, Supplier<OpMode> factory) {
    addOpMode(mode, name, group, "", factory);
  }

  public void addOpMode(RobotMode mode, String name, Supplier<OpMode> factory) {
    addOpMode(mode, name, "", factory);
  }

  public void addOpMode(
      RobotMode mode,
      String name,
      String group,
      String description,
      Color textColor,
      Color backgroundColor,
      Class<? extends OpMode> cls) {
    throw unsupportedOpModes();
  }

  public void addOpMode(
      RobotMode mode, String name, String group, String description, Class<? extends OpMode> cls) {
    addOpMode(mode, name, group, description, null, null, cls);
  }

  public void addOpMode(RobotMode mode, String name, String group, Class<? extends OpMode> cls) {
    addOpMode(mode, name, group, "", cls);
  }

  public void addOpMode(RobotMode mode, String name, Class<? extends OpMode> cls) {
    addOpMode(mode, name, "", cls);
  }

  public void addAnnotatedOpMode(Class<? extends OpMode> cls) {
    throw unsupportedOpModes();
  }

  public void addAnnotatedOpModeClasses(Package pkg) {
    throw unsupportedOpModes();
  }

  public void removeOpMode(RobotMode mode, String name) {
    throw unsupportedOpModes();
  }

  public void publishOpModes() {
    throw unsupportedOpModes();
  }

  public void clearOpModes() {
    throw unsupportedOpModes();
  }

  public void printWatchdogEpochs() {
    throw new UnsupportedOperationException("The host-driven framework does not run a native watchdog");
  }
}
