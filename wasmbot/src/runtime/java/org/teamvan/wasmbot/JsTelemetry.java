package org.teamvan.wasmbot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSString;
import org.graalvm.webimage.api.JSValue;
import org.wpilib.telemetry.TelemetryBackend;
import org.wpilib.telemetry.TelemetryEntry;
import org.wpilib.telemetry.TelemetryRegistry;
import org.wpilib.util.protobuf.Protobuf;
import org.wpilib.util.struct.Struct;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSink;

/**
 * Sends telemetry values to {@code wasmNtPut}. Arrays are JavaScript arrays; raw, struct, and
 * protobuf payloads are lowercase hex strings. Longs outside JavaScript's safe integer range are
 * decimal strings instead of rounded numbers. The host API carries neither timestamps nor metadata.
 */
public final class JsTelemetry implements TelemetryBackend {
  private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
  private static final HexFormat HEX = HexFormat.of();

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
    private final JSString jsPath;
    private ByteBuffer structBuffer;
    private Protobuf<?, ?> protobuf;
    private ProtoMessage<?> protobufMessage;
    private ProtoSink protobufSink;
    private ByteBuffer protobufBytes;

    private Entry(String path) {
      this.path = path;
      jsPath = JSString.of(path);
    }

    @Override
    public void keepDuplicates() {}

    @Override
    public void setProperty(String key, String value) {
      // There is no host metadata channel. Never replace a topic's value with its properties.
    }

    @Override
    public <T> void logStruct(T value, Struct<? super T> struct, long timestamp) {
      ByteBuffer buffer = structBuffer(struct.getSize());
      struct.pack(buffer, value);
      JsBridge.ntPut(jsPath, HEX.formatHex(buffer.array()));
    }

    @Override
    public <T> void logProtobuf(T value, Protobuf<? super T, ?> proto, long timestamp) {
      writeProtobuf(value, proto);
    }

    private <T, M extends ProtoMessage<?>> void writeProtobuf(T value, Protobuf<T, M> proto) {
      if (protobuf != proto) {
        protobuf = proto;
        protobufMessage = proto.createMessage();
        protobufSink = ProtoSink.newBufferSink();
      }
      @SuppressWarnings("unchecked")
      M message = (M) protobufMessage;
      message.clearQuick();
      proto.pack(message, value);
      int size = message.getSerializedSize();
      if (protobufBytes == null || protobufBytes.capacity() < size) {
        protobufBytes = ByteBuffer.allocate(Math.max(128, size * 2));
      }
      protobufBytes.clear();
      protobufSink.setOutput(protobufBytes);
      try {
        message.writeTo(protobufSink);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to serialize protobuf at " + path, e);
      }
      JsBridge.ntPut(jsPath, HEX.formatHex(protobufBytes.array(), 0, size));
    }

    @Override
    public <T> void logStructArray(T[] value, Struct<? super T> struct, long timestamp) {
      JSObject array = createArray(value.length);
      ByteBuffer buffer = structBuffer(struct.getSize());
      for (int i = 0; i < value.length; i++) {
        buffer.clear();
        struct.pack(buffer, value[i]);
        setArrayElement(array, i, HEX.formatHex(buffer.array()));
      }
      put(array);
    }

    @Override
    public void logBoolean(boolean value, long timestamp) {
      JsBridge.ntPut(jsPath, value);
    }

    @Override
    public void logLong(long value, long timestamp) {
      if (isSafeInteger(value)) {
        JsBridge.ntPut(jsPath, (double) value);
      } else {
        JsBridge.ntPut(jsPath, Long.toString(value));
      }
    }

    @Override
    public void logFloat(float value, long timestamp) {
      JsBridge.ntPut(jsPath, value);
    }

    @Override
    public void logDouble(double value, long timestamp) {
      JsBridge.ntPut(jsPath, value);
    }

    @Override
    public void logString(String value, String typeString, long timestamp) {
      JsBridge.ntPut(jsPath, value);
    }

    @Override
    public void logBooleanArray(boolean[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logShortArray(short[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logIntArray(int[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logLongArray(long[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        if (isSafeInteger(value[i])) {
          setArrayElement(array, i, (double) value[i]);
        } else {
          setArrayElement(array, i, Long.toString(value[i]));
        }
      }
      put(array);
    }

    @Override
    public void logFloatArray(float[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logDoubleArray(double[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logStringArray(String[] value, long timestamp) {
      JSObject array = createArray(value.length);
      for (int i = 0; i < value.length; i++) {
        setArrayElement(array, i, value[i]);
      }
      put(array);
    }

    @Override
    public void logRaw(byte[] value, String typeString, long timestamp) {
      JsBridge.ntPut(jsPath, HEX.formatHex(value));
    }

    private ByteBuffer structBuffer(int size) {
      if (structBuffer == null || structBuffer.capacity() != size) {
        structBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
      }
      structBuffer.clear();
      return structBuffer;
    }

    private void put(JSValue value) {
      JsBridge.ntPut(jsPath, value);
    }
  }

  private static boolean isSafeInteger(long value) {
    return value >= -MAX_SAFE_INTEGER && value <= MAX_SAFE_INTEGER;
  }

  @JS.Coerce
  @JS("return new Array(length);")
  private static native JSObject createArray(int length);

  // JSObject.set(Object, Object) does not coerce Java keys or boxed values.
  @JS.Coerce
  @JS("array[index] = value;")
  private static native void setArrayElement(JSObject array, int index, boolean value);

  @JS.Coerce
  @JS("array[index] = value;")
  private static native void setArrayElement(JSObject array, int index, double value);

  @JS.Coerce
  @JS("array[index] = value;")
  private static native void setArrayElement(JSObject array, int index, String value);
}
