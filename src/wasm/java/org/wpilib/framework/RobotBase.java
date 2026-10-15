package org.wpilib.framework;

/**
 * Wasm stand-in for the runtime checks existing robot code makes. The Wasm image is never a real
 * robot, and this class does not initialize HAL or NetworkTables.
 */
public abstract class RobotBase {
  private RobotBase() {}

  /**
   * @return false; Wasm is not a Systemcore robot
   */
  public static boolean isReal() {
    return false;
  }

  /**
   * @return true; Wasm runs the simulation IO implementations
   */
  public static boolean isSimulation() {
    return true;
  }
}
