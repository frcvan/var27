package org.teamvan.robot.wasm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSBoolean;
import org.graalvm.webimage.api.JSNumber;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSString;
import org.graalvm.webimage.api.JSValue;
import org.wpilib.telemetry.TelemetryBackend;
import org.wpilib.telemetry.TelemetryEntry;
import org.wpilib.telemetry.TelemetryRegistry;
import org.wpilib.util.protobuf.Protobuf;
import org.wpilib.util.struct.Struct;

/** Sends {@link org.wpilib.telemetry.Telemetry} writes to {@code wasmNtPut}. */
public final class JsTelemetry implements TelemetryBackend {
  private final Map<String, Entry> entries = new LinkedHashMap<>();

  /** Registers this backend at the telemetry root. Call before the first log. */
  public static void install() {
    TelemetryRegistry.registerBackend("", new JsTelemetry());
  }

  @Override
  public TelemetryEntry getEntry(String path) {
    return entries.computeIfAbsent(path, Entry::new);
  }

  @Override
  public void close() {}

  private static final class Entry implements TelemetryEntry {
    private final String path;

    private Entry(String path) {
      this.path = path;
    }

    @Override
    public void keepDuplicates() {}

    @Override
    public void setProperty(String key, String value) {
      put(JSString.of(key + "=" + value));
    }

    @Override
    public <T> void logStruct(T value, Struct<? super T> struct, long timestamp) {
      ByteBuffer buffer = ByteBuffer.allocate(struct.getSize()).order(ByteOrder.LITTLE_ENDIAN);
      struct.pack(buffer, value);
      put(JSString.of(bytesToHex(buffer.array())));
    }

    @Override
    public <T> void logProtobuf(T value, Protobuf<? super T, ?> proto, long timestamp) {
      put(JSString.of(String.valueOf(value)));
    }

    @Override
    public <T> void logStructArray(T[] value, Struct<? super T> struct, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        ByteBuffer buffer = ByteBuffer.allocate(struct.getSize()).order(ByteOrder.LITTLE_ENDIAN);
        struct.pack(buffer, value[i]);
        array.set(Integer.toString(i), JSString.of(bytesToHex(buffer.array())));
      }
      put(array);
    }

    @Override
    public void logBoolean(boolean value, long timestamp) {
      put(JSBoolean.of(value));
    }

    @Override
    public void logLong(long value, long timestamp) {
      put(JSNumber.of(value));
    }

    @Override
    public void logFloat(float value, long timestamp) {
      put(JSNumber.of(value));
    }

    @Override
    public void logDouble(double value, long timestamp) {
      put(JSNumber.of(value));
    }

    @Override
    public void logString(String value, String typeString, long timestamp) {
      put(JSString.of(value));
    }

    @Override
    public void logBooleanArray(boolean[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSBoolean.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logShortArray(short[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSNumber.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logIntArray(int[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSNumber.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logLongArray(long[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSNumber.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logFloatArray(float[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSNumber.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logDoubleArray(double[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSNumber.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logStringArray(String[] value, long timestamp) {
      JSObject array = createArray();
      for (int i = 0; i < value.length; i++) {
        array.set(Integer.toString(i), JSString.of(value[i]));
      }
      put(array);
    }

    @Override
    public void logRaw(byte[] value, String typeString, long timestamp) {
      put(JSString.of(bytesToHex(value)));
    }

    private void put(JSValue value) {
      JsBridge.ntPut(path, value);
    }
  }

  @JS("return [];")
  private static native JSObject createArray();

  private static String bytesToHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(Character.forDigit((value >> 4) & 0xF, 16));
      builder.append(Character.forDigit(value & 0xF, 16));
    }
    return builder.toString();
  }
}
