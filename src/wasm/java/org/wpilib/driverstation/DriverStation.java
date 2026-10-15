package org.wpilib.driverstation;

/** Wasm stand-in. Joystick values come from JavaScript, not HAL. */
public final class DriverStation {
  private static final Gamepad GAMEPAD = new Gamepad();

  private DriverStation() {}

  /**
   * Returns the single JavaScript-backed gamepad. The port is accepted so existing {@code
   * DriverStation.getGamepad(port)} callsites keep compiling.
   *
   * @param port ignored; JavaScript publishes one driver gamepad
   * @return the gamepad
   */
  public static Gamepad getGamepad(int port) {
    return GAMEPAD;
  }
}
