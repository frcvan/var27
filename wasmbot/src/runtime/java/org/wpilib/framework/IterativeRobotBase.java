package org.wpilib.framework;

import org.wpilib.driverstation.RobotState;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.tunable.TunableRegistry;

/**
 * HAL-free implementation of WPILib's iterative mode callbacks.
 *
 * <p>As in the 2027 SDK, initialization belongs in the robot constructor, not a robotInit hook.
 * Each loop invokes the selected mode's exit/init/periodic hooks, then robotPeriodic, tunable
 * updates, and simulationPeriodic. An enabled driver station with no selected mode remains UNKNOWN
 * and uses the disabled callbacks, just as the SDK's UNKNOWN mode does.
 */
public abstract class IterativeRobotBase extends RobotBase {
  private final double period;
  private RobotMode lastMode;
  private boolean calledDriverStationConnected;

  protected IterativeRobotBase(double period) {
    HostPeriodicCallbacks.periodNanos(period);
    this.period = period;
  }

  @Override
  public abstract void startCompetition();

  public void driverStationConnected() {}

  public void simulationInit() {}

  public void disabledInit() {}

  public void autonomousInit() {}

  public void teleopInit() {}

  public void utilityInit() {}

  public void robotPeriodic() {}

  public void simulationPeriodic() {}

  public void disabledPeriodic() {}

  public void autonomousPeriodic() {}

  public void teleopPeriodic() {}

  public void utilityPeriodic() {}

  public void disabledExit() {}

  public void autonomousExit() {}

  public void teleopExit() {}

  public void utilityExit() {}

  public double getPeriod() {
    return period;
  }

  protected final void loopFunc() {
    RobotMode mode = RobotMode.UNKNOWN;
    if (RobotState.isEnabled()) {
      if (RobotState.isAutonomous()) {
        mode = RobotMode.AUTONOMOUS;
      } else if (RobotState.isTeleop()) {
        mode = RobotMode.TELEOPERATED;
      } else if (RobotState.isUtility()) {
        mode = RobotMode.UTILITY;
      }
    }

    if (!calledDriverStationConnected && RobotState.isDSAttached()) {
      calledDriverStationConnected = true;
      driverStationConnected();
    }

    if (lastMode != mode) {
      if (lastMode != null) {
        switch (lastMode) {
          case UNKNOWN -> disabledExit();
          case AUTONOMOUS -> autonomousExit();
          case TELEOPERATED -> teleopExit();
          case UTILITY -> utilityExit();
          default -> {
            // Other SDK modes do not define iterative callbacks.
          }
        }
      }
      switch (mode) {
        case UNKNOWN -> disabledInit();
        case AUTONOMOUS -> autonomousInit();
        case TELEOPERATED -> teleopInit();
        case UTILITY -> utilityInit();
        default -> {
          // Other SDK modes do not define iterative callbacks.
        }
      }
      lastMode = mode;
    }

    switch (mode) {
      case UNKNOWN -> disabledPeriodic();
      case AUTONOMOUS -> autonomousPeriodic();
      case TELEOPERATED -> teleopPeriodic();
      case UTILITY -> utilityPeriodic();
      default -> {
        // Other SDK modes do not define iterative callbacks.
      }
    }
    robotPeriodic();
    TunableRegistry.update();
    simulationPeriodic();
  }

  public void printWatchdogEpochs() {
    throw new UnsupportedOperationException("The host-driven framework does not run a native watchdog");
  }
}
