package org.teamvan.wasmbot;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSString;
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
  public static JSValue ntGet(String path) {
    return ntGet(JSString.of(path));
  }

  @JS("return globalThis.wasmNtGet(path);")
  private static native JSValue ntGet(JSString path);

  /**
   * Publishes one numeric telemetry value.
   *
   * @param path NetworkTables path
   * @param value numeric value
   */
  @JS.Coerce
  @JS("globalThis.wasmNtPut(path, value);")
  public static native void ntPut(String path, double value);

  /** Publishes a number without re-encoding a cached JavaScript topic string. */
  @JS.Coerce
  @JS("globalThis.wasmNtPut(path, value);")
  static native void ntPut(JSString path, double value);

  /** Publishes one boolean telemetry value. */
  @JS.Coerce
  @JS("globalThis.wasmNtPut(path, value);")
  static native void ntPut(JSString path, boolean value);

  /** Publishes one string telemetry value. */
  @JS.Coerce
  @JS("globalThis.wasmNtPut(path, value);")
  static native void ntPut(JSString path, String value);

  /**
   * Publishes one telemetry value. The value must already be a JavaScript value.
   *
   * @param path NetworkTables path
   * @param value JavaScript primitive, array, or plain object
   */
  public static void ntPut(String path, JSValue value) {
    ntPut(JSString.of(path), value);
  }

  @JS("globalThis.wasmNtPut(path, value);")
  static native void ntPut(JSString path, JSValue value);
}
