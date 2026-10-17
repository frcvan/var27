package org.wpilib.system;

import java.util.function.LongSupplier;
import org.teamvan.wasmbot.WasmRuntime;

/**
 * Wasm stand-in for the clock and battery reads sim physics makes. The default time source is the
 * JavaScript clock, so a caller that still uses {@link #setTimeSource} keeps working.
 */
public final class RobotController {
  private static volatile LongSupplier timeSource = WasmRuntime::nowNanos;

  private RobotController() {}

  /**
   * Replaces the clock used by {@link #getTime}.
   *
   * @param supplier time in nanoseconds
   */
  public static void setTimeSource(LongSupplier supplier) {
    timeSource = supplier;
  }

  /**
   * @return the current time source value, in nanoseconds
   */
  public static long getTime() {
    return timeSource.getAsLong();
  }

  /**
   * @return JavaScript monotonic time in nanoseconds
   */
  public static long getMonotonicTime() {
    return WasmRuntime.nowNanos();
  }

  /**
   * @return the fixed simulated battery voltage
   */
  public static double getBatteryVoltage() {
    return WasmRuntime.getBatteryVoltage();
  }
}
