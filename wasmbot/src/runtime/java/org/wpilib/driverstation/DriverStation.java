package org.wpilib.driverstation;

/** JavaScript-backed Driver Station controller factories, without HAL initialization. */
public final class DriverStation {
  // Match WPILib's six ports without referring to its native DriverStationBackend.
  private static final GenericHID[] HIDS = new GenericHID[6];
  private static final Gamepad[] GAMEPADS = new Gamepad[6];

  private DriverStation() {}

  /**
   * Returns the cached virtual HID for a Driver Station port.
   *
   * <p>Only port zero receives the browser's three-axis snapshot. Other valid ports are
   * disconnected and return neutral input; they do not mirror port zero.
   *
   * @param port Driver Station port, from zero through five
   * @return the port's HID
   */
  public static synchronized GenericHID getGenericHID(int port) {
    GenericHID hid = HIDS[port];
    if (hid == null) {
      hid = new GenericHID(port);
      HIDS[port] = hid;
    }
    return hid;
  }

  /**
   * Returns the cached gamepad wrapping the same HID as {@link #getGenericHID(int)}.
   *
   * @param port Driver Station port, from zero through five
   * @return the port's gamepad
   */
  public static synchronized Gamepad getGamepad(int port) {
    Gamepad gamepad = GAMEPADS[port];
    if (gamepad == null) {
      gamepad = new Gamepad(getGenericHID(port));
      GAMEPADS[port] = gamepad;
    }
    return gamepad;
  }
}
