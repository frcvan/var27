package org.wpilib.framework;

import java.util.function.Supplier;
import org.teamvan.wasmbot.WasmApplication;
import org.wpilib.driverstation.RobotState;
import org.wpilib.math.util.MathShared;
import org.wpilib.math.util.MathSharedStore;
import org.wpilib.system.RuntimeType;
import org.wpilib.system.Timer;

/**
 * HAL-free robot framework entry point for the JavaScript host.
 *
 * <p>Construction does not start NetworkTables, HAL, or native threads. Telemetry is installed by
 * {@link WasmApplication} before constructing the robot. Framework subclasses initialize once in
 * {@link #startCompetition()} and receive serialized, nonblocking {@link #wasmTick()} calls instead
 * of waiting on a HAL notifier.
 */
public abstract class RobotBase implements AutoCloseable {
  private static long mainThreadId = -1;
  private boolean competitionStarted;
  private boolean competitionRunning;

  protected RobotBase() {
    mainThreadId = Thread.currentThread().threadId();
    MathSharedStore.setMathShared(
        new MathShared() {
          @Override
          public void reportError(String error, StackTraceElement[] stackTrace) {
            System.err.println(error);
            for (StackTraceElement element : stackTrace) {
              System.err.println("\tat " + element);
            }
          }

          @Override
          public double getTimestamp() {
            return Timer.getTimestamp();
          }
        });
  }

  public static long getMainThreadId() {
    return mainThreadId;
  }

  public static RuntimeType getRuntimeType() {
    return RuntimeType.SIMULATION;
  }

  public static boolean isSimulation() {
    return true;
  }

  public static boolean isReal() {
    return false;
  }

  public static boolean isDisabled() {
    return RobotState.isDisabled();
  }

  public static boolean isEnabled() {
    return RobotState.isEnabled();
  }

  public static boolean isAutonomous() {
    return RobotState.isAutonomous();
  }

  public static boolean isAutonomousEnabled() {
    return RobotState.isAutonomousEnabled();
  }

  public static boolean isTeleop() {
    return RobotState.isTeleop();
  }

  public static boolean isTeleopEnabled() {
    return RobotState.isTeleopEnabled();
  }

  public static boolean isUtility() {
    return RobotState.isUtility();
  }

  public static boolean isUtilityEnabled() {
    return RobotState.isUtilityEnabled();
  }

  public static long getOpModeId() {
    return RobotState.getOpModeId();
  }

  public static String getOpMode() {
    return RobotState.getOpMode();
  }

  /** Exports the host startup and asynchronous tick functions without constructing the robot yet. */
  public static <T extends RobotBase> void startRobot(Supplier<T> robotConstructor) {
    WasmApplication.launch(robotConstructor);
  }

  /** Initializes competition without blocking; the host supplies subsequent loop ticks. */
  public abstract void startCompetition();

  /** Permanently stops competition. Later host ticks must not invoke user callbacks. */
  public abstract void endCompetition();

  /** Starts the framework once, including when startup or close is called more than once. */
  protected final boolean beginCompetition() {
    if (competitionStarted) {
      return false;
    }
    competitionStarted = true;
    competitionRunning = true;
    return true;
  }

  protected final void stopCompetition() {
    competitionStarted = true;
    competitionRunning = false;
  }

  protected final boolean isCompetitionRunning() {
    return competitionRunning;
  }

  /** Internal host hook; normal robot code overrides the standard framework callbacks instead. */
  public final void wasmTick() {
    if (competitionRunning) {
      wasmLoop();
    }
  }

  /** Implemented by the host-driven periodic frameworks, not by hardware-backed loops. */
  protected void wasmLoop() {
    throw new UnsupportedOperationException(
        "Custom RobotBase loops must implement a nonblocking wasmLoop() and call beginCompetition()");
  }

  @Override
  public void close() {
    endCompetition();
  }
}
