package org.wpilib.driverstation;

import org.teamvan.wasm.WasmRuntime;

/** Wasm stand-in for JavaScript-backed gamepad axes. */
public class Gamepad {
  /**
   * @return left stick X from the latest JavaScript snapshot
   */
  public double getLeftX() {
    return WasmRuntime.getLeftX();
  }

  /**
   * @return left stick Y from the latest JavaScript snapshot
   */
  public double getLeftY() {
    return WasmRuntime.getLeftY();
  }

  /**
   * @return right stick X from the latest JavaScript snapshot
   */
  public double getRightX() {
    return WasmRuntime.getRightX();
  }
}
