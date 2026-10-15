package org.wpilib.system;

import org.teamvan.robot.wasm.WasmRuntime;

/** Wasm stand-in. Time comes from {@code wasmNowNanos}, not HAL. */
public class Timer {
  /**
   * @return JavaScript monotonic time in seconds
   */
  public static double getTimestamp() {
    return WasmRuntime.nowNanos() / 1_000_000_000.0;
  }

  /**
   * @return JavaScript monotonic time in seconds
   */
  public static double getMonotonicTimestamp() {
    return getTimestamp();
  }
}
