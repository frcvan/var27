package org.wpilib.driverstation;

import org.teamvan.wasmbot.WasmRuntime;
import org.wpilib.hardware.hal.RobotMode;

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

  /** @return whether the browser has delivered a Driver Station snapshot */
  public static boolean isDSAttached() {
    return WasmRuntime.isDSAttached();
  }

  /** @return false; the browser is not connected to an FMS */
  public static boolean isFMSAttached() {
    return false;
  }

  /** @return false; the host snapshot has no emergency-stop input */
  public static boolean isEStopped() {
    return false;
  }

  /** @return unknown; the browser does not select a robot mode */
  public static RobotMode getRobotMode() {
    return RobotMode.UNKNOWN;
  }

  /** @return false; no autonomous mode is selected */
  public static boolean isAutonomous() {
    return false;
  }

  /** @return false; no autonomous mode is selected */
  public static boolean isAutonomousEnabled() {
    return false;
  }

  /** @return false; no teleoperated mode is selected */
  public static boolean isTeleop() {
    return false;
  }

  /** @return false; no teleoperated mode is selected */
  public static boolean isTeleopEnabled() {
    return false;
  }

  /** @return false; no utility mode is selected */
  public static boolean isUtility() {
    return false;
  }

  /** @return false; no utility mode is selected */
  public static boolean isUtilityEnabled() {
    return false;
  }

  /** @return an empty string; no operating mode is selected */
  public static String getOpMode() {
    return "";
  }

  /** @return whether the ID matches the no-selected-mode sentinel */
  public static boolean isOpMode(long id) {
    return id == 0;
  }

  /** @return whether the name matches the no-selected-mode sentinel */
  public static boolean isOpMode(String mode) {
    return "".equals(mode);
  }
}
