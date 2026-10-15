package org.wpilib.driverstation;

import org.teamvan.robot.wasm.WasmRuntime;

/** Wasm stand-in. Enable state comes from JavaScript, not HAL. */
public final class RobotState {
  private RobotState() {}

  /**
   * @return true when JavaScript reports the robot enabled
   */
  public static boolean isEnabled() {
    return WasmRuntime.isEnabled();
  }

  /**
   * @return true when JavaScript reports the robot disabled
   */
  public static boolean isDisabled() {
    return WasmRuntime.isDisabled();
  }

  /**
   * @return {@code 0}; Wasm does not select an opmode through the driver station
   */
  public static long getOpModeId() {
    return 0;
  }
}
