package org.teamvan.robot.wasm;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSValue;

/**
 * JavaScript functions the Wasm image reads instead of DriverStation and NetworkTables JNI.
 *
 * <p>The host installs these on {@code globalThis} before calling {@code wasmStart}:
 *
 * <ul>
 *   <li>{@code wasmNowNanos(): number} monotonic time in nanoseconds
 *   <li>{@code wasmDs(): {enabled, alliance, leftX, leftY, rightX}} alliance is {@code "red"} or
 *       {@code "blue"}
 *   <li>{@code wasmNtGet(path): any} current NetworkTables value, or {@code undefined}
 *   <li>{@code wasmNtPut(path, value)} publish a telemetry value
 * </ul>
 */
public final class JsBridge {
  private JsBridge() {}

  /**
   * @return monotonic time in nanoseconds from {@code wasmNowNanos}
   */
  @JS.Coerce
  @JS("return globalThis.wasmNowNanos();")
  public static native double nowNanos();

  /**
   * @return the current DriverStation snapshot from {@code wasmDs}
   */
  @JS("return globalThis.wasmDs();")
  public static native JSObject driverStation();

  /**
   * @param path NetworkTables path
   * @return the current value, or a JS {@code undefined}
   */
  @JS("return globalThis.wasmNtGet(path);")
  public static native JSValue ntGet(String path);

  /**
   * Publishes one telemetry value. The value must already be a JavaScript value.
   *
   * @param path NetworkTables path
   * @param value JavaScript primitive, array, or plain object
   */
  @JS.Coerce
  @JS("globalThis.wasmNtPut(path, value);")
  public static native void ntPut(String path, double value);

  /**
   * Publishes one telemetry value. The value must already be a JavaScript value.
   *
   * @param path NetworkTables path
   * @param value JavaScript primitive, array, or plain object
   */
  @JS("globalThis.wasmNtPut(path, value);")
  public static native void ntPut(String path, JSValue value);

  /**
   * @return true after the host sets {@code globalThis.wasmStop}
   */
  @JS.Coerce
  @JS("return globalThis.wasmStop === true;")
  public static native boolean stopped();
}
