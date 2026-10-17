package org.teamvan.wasmbot;

import java.util.Optional;
import java.util.function.LongSupplier;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.wpilib.driverstation.Alliance;

/**
 * Java-side copy of the values JavaScript supplies each loop. WPILib facades read this instead of
 * HAL.
 */
public final class WasmRuntime {
  private static final double BATTERY_VOLTS = 12.0;

  private static volatile boolean enabled;
  private static volatile boolean dsAttached;
  private static volatile Alliance alliance = Alliance.BLUE;
  private static volatile double leftX;
  private static volatile double leftY;
  private static volatile double rightX;
  private static volatile LongSupplier clock = () -> (long) JsBridge.nowNanos();

  private WasmRuntime() {}

  /** Pulls the current DriverStation snapshot from JavaScript. */
  public static void refresh() {
    JSObject ds = JsBridge.driverStation();
    setEnabled(enabled(ds));
    setAlliance(redAlliance(ds) ? Alliance.RED : Alliance.BLUE);
    setAxes(leftX(ds), leftY(ds), rightX(ds));
    dsAttached = true;
  }

  /**
   * Replaces the monotonic clock. Desktop tests use this because Web Image is not running.
   *
   * @param clock time in nanoseconds
   */
  public static void setClock(LongSupplier clock) {
    WasmRuntime.clock = clock;
  }

  /**
   * @param enabled whether JavaScript reports the robot enabled
   */
  public static void setEnabled(boolean enabled) {
    WasmRuntime.enabled = enabled;
  }

  /**
   * @param alliance alliance from the latest snapshot
   */
  public static void setAlliance(Alliance alliance) {
    WasmRuntime.alliance = alliance;
  }

  /**
   * @param leftX left stick X in [-1, 1]
   * @param leftY left stick Y in [-1, 1]
   * @param rightX right stick X in [-1, 1]
   */
  public static void setAxes(double leftX, double leftY, double rightX) {
    WasmRuntime.leftX = leftX;
    WasmRuntime.leftY = leftY;
    WasmRuntime.rightX = rightX;
  }

  /**
   * @return true when JavaScript says the robot is enabled
   */
  public static boolean isEnabled() {
    return enabled;
  }

  /**
   * @return true when JavaScript says the robot is disabled
   */
  public static boolean isDisabled() {
    return !enabled;
  }

  /** Returns whether a driver-station snapshot has been received from the host. */
  public static boolean isDSAttached() {
    return dsAttached;
  }

  /**
   * @return the alliance from the latest snapshot
   */
  public static Optional<Alliance> getAlliance() {
    return Optional.of(alliance);
  }

  /**
   * @return left stick X in [-1, 1]
   */
  public static double getLeftX() {
    return leftX;
  }

  /**
   * @return left stick Y in [-1, 1]
   */
  public static double getLeftY() {
    return leftY;
  }

  /**
   * @return right stick X in [-1, 1]
   */
  public static double getRightX() {
    return rightX;
  }

  /**
   * @return monotonic nanoseconds
   */
  public static long nowNanos() {
    return clock.getAsLong();
  }

  /**
   * @return the fixed simulated battery voltage
   */
  public static double getBatteryVoltage() {
    return BATTERY_VOLTS;
  }

  @JS.Coerce
  @JS("return ds.enabled === true;")
  private static native boolean enabled(JSObject ds);

  @JS.Coerce
  @JS("return typeof ds.alliance === 'string' && ds.alliance.toLowerCase() === 'red';")
  private static native boolean redAlliance(JSObject ds);

  @JS.Coerce
  @JS("return typeof ds.leftX === 'number' ? ds.leftX : 0;")
  private static native double leftX(JSObject ds);

  @JS.Coerce
  @JS("return typeof ds.leftY === 'number' ? ds.leftY : 0;")
  private static native double leftY(JSObject ds);

  @JS.Coerce
  @JS("return typeof ds.rightX === 'number' ? ds.rightX : 0;")
  private static native double rightX(JSObject ds);
}
