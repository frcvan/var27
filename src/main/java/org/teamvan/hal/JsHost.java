package org.teamvan.hal;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSValue;
import org.wpilib.hardware.hal.RobotMode;

/**
 * JS host for the WASM image.
 *
 * <p>{@code globalThis.var27.networkTables.supplier(key)} returns a string value or null. {@code
 * globalThis.var27.networkTables.consumer(key, type, value)} receives Java publishes. {@code
 * globalThis.var27.driverStation} supplies FMS fields: enabled, dsAttached, fmsAttached, estop,
 * robotMode, alliance, matchTime, opMode, gameData, eventName, matchNumber, replayNumber,
 * matchType.
 */
public final class JsHost {
  private static boolean installed;

  private JsHost() {}

  public static void install() {
    if (installed) {
      return;
    }
    installed = true;
    JSObject root = global();
    if (root == null) {
      return;
    }
    JSObject networkTables = object(root, "networkTables");
    JSObject driverStation = object(root, "driverStation");
    NtStore.TABLES.setSupplier(key -> string(networkTables, "supplier", key));
    NtStore.TABLES.setConsumer(
        (key, type, value) -> call(networkTables, "consumer", key, type, value));
    applyDriverStation(driverStation);
  }

  public static void refreshDriverStation() {
    JSObject root = global();
    if (root != null) {
      applyDriverStation(object(root, "driverStation"));
    }
  }

  private static void applyDriverStation(JSObject driverStation) {
    if (driverStation == null) {
      return;
    }
    HalRuntime.Ds ds = HalRuntime.DS;
    Boolean enabled = bool(driverStation, "enabled");
    if (enabled != null) {
      ds.enabled = enabled;
    }
    Boolean dsAttached = bool(driverStation, "dsAttached");
    if (dsAttached != null) {
      ds.dsAttached = dsAttached;
    }
    Boolean fmsAttached = bool(driverStation, "fmsAttached");
    if (fmsAttached != null) {
      ds.fmsAttached = fmsAttached;
    }
    Boolean estop = bool(driverStation, "estop");
    if (estop != null) {
      ds.eStop = estop;
    }
    String mode = stringMember(driverStation, "robotMode");
    if (mode != null) {
      ds.robotMode = robotMode(mode);
    }
    Integer alliance = integer(driverStation, "alliance");
    if (alliance != null) {
      ds.alliance = alliance;
    }
    Double matchTime = number(driverStation, "matchTime");
    if (matchTime != null) {
      ds.matchTime = matchTime;
    }
    Long opMode = longValue(driverStation, "opMode");
    if (opMode != null) {
      ds.opMode = opMode;
    }
    String gameData = stringMember(driverStation, "gameData");
    if (gameData != null) {
      ds.gameData = gameData;
    }
    String eventName = stringMember(driverStation, "eventName");
    if (eventName != null) {
      ds.eventName = eventName;
    }
    Integer matchNumber = integer(driverStation, "matchNumber");
    if (matchNumber != null) {
      ds.matchNumber = matchNumber;
    }
    Integer replayNumber = integer(driverStation, "replayNumber");
    if (replayNumber != null) {
      ds.replayNumber = replayNumber;
    }
    Integer matchType = integer(driverStation, "matchType");
    if (matchType != null) {
      ds.matchType = matchType;
    }
    HalRuntime.refreshDs();
  }

  private static int robotMode(String mode) {
    return switch (mode.toLowerCase()) {
      case "auto", "autonomous" -> RobotMode.AUTONOMOUS.getValue();
      case "teleop", "teleoperated" -> RobotMode.TELEOPERATED.getValue();
      case "utility", "test" -> RobotMode.UTILITY.getValue();
      default -> RobotMode.UNKNOWN.getValue();
    };
  }

  @JS(
      value = "return globalThis.var27 || (globalThis.var27 = {});",
      args = {})
  private static JSObject global() {
    return JSObject.create();
  }

  private static JSObject object(JSObject parent, String name) {
    Object value = parent.get(name);
    if (value instanceof JSObject object) {
      return object;
    }
    JSObject created = JSObject.create();
    parent.set(name, created);
    return created;
  }

  private static String string(JSObject object, String function, String arg) {
    Object fn = object.get(function);
    if (!(fn instanceof JSObject functionObject)) {
      return null;
    }
    Object result = functionObject.call(object, arg);
    return result instanceof String text ? text : null;
  }

  private static void call(
      JSObject object, String function, String key, String type, String value) {
    Object fn = object.get(function);
    if (fn instanceof JSObject functionObject) {
      functionObject.call(object, key, type, value);
    }
  }

  private static String raw(JSObject object, String name) {
    Object value = object.get(name);
    if (!(value instanceof JSValue js) || !"string".equals(js.typeof())) {
      return null;
    }
    return js.asString();
  }

  private static String stringMember(JSObject object, String name) {
    return raw(object, name);
  }

  private static Boolean bool(JSObject object, String name) {
    String text = raw(object, name);
    return text == null ? null : Boolean.valueOf(text);
  }

  private static Integer integer(JSObject object, String name) {
    Double value = number(object, name);
    return value == null ? null : value.intValue();
  }

  private static Long longValue(JSObject object, String name) {
    Double value = number(object, name);
    return value == null ? null : value.longValue();
  }

  private static Double number(JSObject object, String name) {
    String text = raw(object, name);
    if (text == null) {
      return null;
    }
    try {
      return Double.valueOf(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
