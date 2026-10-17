package org.wpilib.system;

/** Wasm stand-in. Time comes from {@code wasmNowNanos}, not HAL. */
public class Timer {
  /**
   * @return configured robot time in seconds; JavaScript monotonic time by default
   */
  public static double getTimestamp() {
    return RobotController.getTime() / 1_000_000_000.0;
  }

  /**
   * @return JavaScript monotonic time in seconds
   */
  public static double getMonotonicTimestamp() {
    return RobotController.getMonotonicTime() / 1_000_000_000.0;
  }
}
