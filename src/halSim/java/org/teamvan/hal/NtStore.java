package org.teamvan.hal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.wpilib.networktables.ConnectionInfo;
import org.wpilib.networktables.LogMessage;
import org.wpilib.networktables.NetworkTableEvent;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.NetworkTableType;
import org.wpilib.networktables.NetworkTableValue;
import org.wpilib.networktables.TimeSyncEventData;
import org.wpilib.networktables.TimestampedBoolean;
import org.wpilib.networktables.TimestampedBooleanArray;
import org.wpilib.networktables.TimestampedDouble;
import org.wpilib.networktables.TimestampedDoubleArray;
import org.wpilib.networktables.TimestampedFloat;
import org.wpilib.networktables.TimestampedFloatArray;
import org.wpilib.networktables.TimestampedInteger;
import org.wpilib.networktables.TimestampedIntegerArray;
import org.wpilib.networktables.TimestampedRaw;
import org.wpilib.networktables.TimestampedString;
import org.wpilib.networktables.TimestampedStringArray;
import org.wpilib.networktables.TopicInfo;

/**
 * In-memory NetworkTables. Java publishes call the JS consumer; the JS supplier fills values the
 * robot reads.
 */
public final class NtStore {
  public static final NtStore TABLES = new NtStore();

  public interface Consumer {
    void accept(String key, String type, String value);
  }

  public interface Supplier {
    String get(String key);
  }

  private static final int DEFAULT_INSTANCE = 1;

  static final class Topic {
    final int handle;
    final int instance;
    String name;
    int type;
    String typeStr = "";
    boolean persistent;
    boolean retained = true;
    boolean cached = true;
    boolean exists = true;
    final Map<String, String> properties = new LinkedHashMap<>();
    Object value;
    long time;
    final List<Object> queue = new ArrayList<>();

    Topic(int handle, int instance, String name) {
      this.handle = handle;
      this.instance = instance;
      this.name = name;
    }
  }

  static final class Handle {
    final int id;
    final Topic topic;
    final boolean subscriber;
    final String[] prefixes;

    Handle(int id, Topic topic, boolean subscriber, String[] prefixes) {
      this.id = id;
      this.topic = topic;
      this.subscriber = subscriber;
      this.prefixes = prefixes;
    }
  }

  private final Object lock = new Object();
  private final Map<Integer, Map<String, Topic>> instances = new LinkedHashMap<>();
  private final Map<Integer, Topic> topics = new LinkedHashMap<>();
  private final Map<Integer, Handle> handles = new LinkedHashMap<>();
  private int nextTopic = 1;
  private int nextHandle = 1;
  private int nextInstance = 2;
  private Consumer consumer = (key, type, value) -> {};
  private Supplier supplier = key -> null;

  private NtStore() {
    instances.put(DEFAULT_INSTANCE, new LinkedHashMap<>());
  }

  public void setConsumer(Consumer consumer) {
    this.consumer = consumer == null ? (key, type, value) -> {} : consumer;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier == null ? key -> null : supplier;
  }

  public int defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public int createInstance() {
    synchronized (lock) {
      int id = nextInstance++;
      instances.put(id, new LinkedHashMap<>());
      return id;
    }
  }

  public void destroyInstance(int inst) {
    synchronized (lock) {
      Map<String, Topic> removed = instances.remove(inst);
      if (removed == null) {
        return;
      }
      for (Topic topic : removed.values()) {
        topics.remove(topic.handle);
      }
    }
  }

  public int instanceFromHandle(int handle) {
    synchronized (lock) {
      Topic topic = topics.get(handle);
      if (topic != null) {
        return topic.instance;
      }
      Handle stored = handles.get(handle);
      return stored == null || stored.topic == null ? 0 : stored.topic.instance;
    }
  }

  public int getTopic(int inst, String name) {
    synchronized (lock) {
      return topic(inst, name).handle;
    }
  }

  public int getEntry(int inst, String key) {
    synchronized (lock) {
      return subscribe(topic(inst, key));
    }
  }

  public int subscribe(int topicHandle, int type, String typeStr) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null) {
        return 0;
      }
      if (topic.type == 0 && type != 0) {
        topic.type = type;
        topic.typeStr = typeStr == null ? NetworkTableType.getFromInt(type).getValueStr() : typeStr;
      }
      return subscribe(topic);
    }
  }

  public int publish(int topicHandle, int type, String typeStr) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null) {
        return 0;
      }
      topic.type = type;
      topic.typeStr = typeStr == null ? "" : typeStr;
      int id = nextHandle++;
      handles.put(id, new Handle(id, topic, false, null));
      return id;
    }
  }

  public void release(int handle) {
    synchronized (lock) {
      handles.remove(handle);
    }
  }

  public String entryName(int entry) {
    Topic topic = topicOf(entry);
    return topic == null ? "" : topic.name;
  }

  public long lastChange(int entry) {
    Topic topic = topicOf(entry);
    return topic == null ? 0 : topic.time;
  }

  public int type(int entry) {
    Topic topic = topicOf(entry);
    return topic == null ? 0 : topic.type;
  }

  public String typeString(int topicHandle) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      return topic == null ? "" : topic.typeStr;
    }
  }

  public int[] topics(int inst, String prefix, int typeMask) {
    synchronized (lock) {
      Map<String, Topic> table = instances.get(inst);
      if (table == null) {
        return new int[0];
      }
      String p = prefix == null ? "" : prefix;
      return table.values().stream()
          .filter(topic -> topic.name.startsWith(p))
          .filter(topic -> typeMask == 0 || (topic.type & typeMask) != 0)
          .mapToInt(topic -> topic.handle)
          .toArray();
    }
  }

  public int[] topics(int inst, String prefix, String[] types) {
    int mask = 0;
    if (types != null) {
      for (String type : types) {
        mask |= NetworkTableType.getFromString(type).getValue();
      }
    }
    return topics(inst, prefix, mask);
  }

  public TopicInfo[] infos(NetworkTableInstance instObject, int inst, String prefix, int types) {
    int[] handles = topics(inst, prefix, types);
    TopicInfo[] infos = new TopicInfo[handles.length];
    for (int i = 0; i < handles.length; i++) {
      infos[i] = info(instObject, handles[i]);
    }
    return infos;
  }

  public TopicInfo info(NetworkTableInstance instObject, int topicHandle) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null) {
        return new TopicInfo(instObject, 0, "", 0, "");
      }
      return new TopicInfo(instObject, topic.handle, topic.name, topic.type, topic.typeStr);
    }
  }

  public void setFlag(int topicHandle, String flag, boolean value) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null) {
        return;
      }
      switch (flag) {
        case "persistent" -> topic.persistent = value;
        case "retained" -> topic.retained = value;
        case "cached" -> topic.cached = value;
        default -> {}
      }
    }
  }

  public boolean flag(int topicHandle, String flag) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null) {
        return false;
      }
      return switch (flag) {
        case "persistent" -> topic.persistent;
        case "retained" -> topic.retained;
        case "cached" -> topic.cached;
        case "exists" -> topic.exists;
        default -> false;
      };
    }
  }

  public String property(int topicHandle, String name) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      return topic == null ? "" : topic.properties.getOrDefault(name, "");
    }
  }

  public void setProperty(int topicHandle, String name, String value) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic != null && name != null) {
        if (value == null) {
          topic.properties.remove(name);
        } else {
          topic.properties.put(name, value);
        }
      }
    }
  }

  public String properties(int topicHandle) {
    synchronized (lock) {
      Topic topic = topics.get(topicHandle);
      if (topic == null || topic.properties.isEmpty()) {
        return "{}";
      }
      StringBuilder builder = new StringBuilder("{");
      topic.properties.forEach(
          (key, value) -> {
            if (builder.length() > 1) {
              builder.append(',');
            }
            builder.append('"').append(key).append("\":\"").append(value).append('"');
          });
      return builder.append('}').toString();
    }
  }

  public boolean set(int entry, long time, int type, Object value) {
    Topic topic;
    String key;
    String typeStr;
    String encoded;
    synchronized (lock) {
      topic = topicOf(entry);
      if (topic == null) {
        return false;
      }
      if (topic.type == 0) {
        topic.type = type;
        topic.typeStr = NetworkTableType.getFromInt(type).getValueStr();
      }
      topic.value = copy(value);
      topic.time = time == 0 ? System.nanoTime() : time;
      topic.queue.add(topic.value);
      if (topic.queue.size() > 64) {
        topic.queue.remove(0);
      }
      key = topic.name;
      typeStr = topic.typeStr;
      encoded = encode(topic.type, topic.value);
    }
    consumer.accept(key, typeStr, encoded);
    return true;
  }

  public Object get(int entry, int type, Object fallback) {
    Topic topic = topicOf(entry);
    if (topic == null) {
      return fallback;
    }
    pull(topic);
    synchronized (lock) {
      if (topic.value == null || (type != 0 && topic.type != type)) {
        return fallback;
      }
      return copy(topic.value);
    }
  }

  public NetworkTableValue value(int entry) {
    Topic topic = topicOf(entry);
    if (topic == null || topic.value == null) {
      return NetworkTableValue.makeString("");
    }
    pull(topic);
    return toValue(topic);
  }

  public NetworkTableValue[] queueValues(int entry) {
    Topic topic = topicOf(entry);
    if (topic == null) {
      return new NetworkTableValue[0];
    }
    pull(topic);
    synchronized (lock) {
      NetworkTableValue[] values = new NetworkTableValue[topic.queue.size()];
      for (int i = 0; i < topic.queue.size(); i++) {
        values[i] = valueOf(topic.type, topic.queue.get(i), topic.time);
      }
      topic.queue.clear();
      return values;
    }
  }

  public Object[] queue(int entry) {
    Topic topic = topicOf(entry);
    if (topic == null) {
      return new Object[0];
    }
    synchronized (lock) {
      Object[] values = topic.queue.toArray();
      topic.queue.clear();
      return values;
    }
  }

  public long now() {
    return System.nanoTime();
  }

  public boolean connected(int inst) {
    return instances.containsKey(inst);
  }

  public ConnectionInfo[] connections(int inst) {
    if (!connected(inst)) {
      return new ConnectionInfo[0];
    }
    return new ConnectionInfo[] {new ConnectionInfo("js", "127.0.0.1", 0, now(), 0x0401)};
  }

  public OptionalLong serverOffset(int inst) {
    return connected(inst) ? OptionalLong.of(0) : OptionalLong.empty();
  }

  public int startLocal(int inst) {
    return inst;
  }

  public NetworkTableEvent[] events(NetworkTableInstance inst, int poller) {
    return new NetworkTableEvent[] {
      new NetworkTableEvent(
          inst,
          poller,
          0,
          null,
          null,
          null,
          new LogMessage(LogMessage.INFO, "NtStore", 0, "js"),
          new TimeSyncEventData(0, 0, false))
    };
  }

  public static String encode(int type, Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof boolean[] booleans) {
      return Arrays.toString(booleans);
    }
    if (value instanceof long[] longs) {
      return Arrays.toString(longs);
    }
    if (value instanceof float[] floats) {
      return Arrays.toString(floats);
    }
    if (value instanceof double[] doubles) {
      return Arrays.toString(doubles);
    }
    if (value instanceof String[] strings) {
      return String.join(",", strings);
    }
    if (value instanceof byte[] raw) {
      return Arrays.toString(raw);
    }
    return String.valueOf(value);
  }

  public static Object decode(String type, String raw) {
    if (raw == null) {
      return null;
    }
    return switch (type == null ? "" : type) {
      case "boolean" -> Boolean.valueOf(raw);
      case "double" -> Double.valueOf(raw);
      case "float" -> Float.valueOf(raw);
      case "int" -> Long.valueOf(raw);
      case "boolean[]" -> decodeBooleans(raw);
      case "double[]" -> decodeDoubles(raw);
      case "float[]" -> decodeFloats(raw);
      case "int[]" -> decodeLongs(raw);
      case "string[]" -> raw.isEmpty() ? new String[0] : raw.split(",");
      default -> raw;
    };
  }

  private void pull(Topic topic) {
    String supplied = supplier.get(topic.name);
    if (supplied == null) {
      return;
    }
    synchronized (lock) {
      String type = topic.typeStr.isEmpty() ? "string" : topic.typeStr;
      topic.value = decode(type, supplied);
      topic.type = NetworkTableType.getFromString(type).getValue();
      topic.typeStr = type;
      topic.time = System.nanoTime();
    }
  }

  private Topic topic(int inst, String name) {
    Map<String, Topic> table = instances.computeIfAbsent(inst, ignored -> new LinkedHashMap<>());
    return table.computeIfAbsent(
        name,
        key -> {
          Topic created = new Topic(nextTopic++, inst, key);
          topics.put(created.handle, created);
          return created;
        });
  }

  private int subscribe(Topic topic) {
    int id = nextHandle++;
    handles.put(id, new Handle(id, topic, true, null));
    return id;
  }

  private Topic topicOf(int handle) {
    synchronized (lock) {
      Topic topic = topics.get(handle);
      if (topic != null) {
        return topic;
      }
      Handle stored = handles.get(handle);
      return stored == null ? null : stored.topic;
    }
  }

  private static NetworkTableValue toValue(Topic topic) {
    return valueOf(topic.type, topic.value, topic.time);
  }

  private static NetworkTableValue valueOf(int type, Object value, long time) {
    return switch (type) {
      case 0x01 -> NetworkTableValue.makeBoolean(Boolean.TRUE.equals(value), time);
      case 0x02 ->
          NetworkTableValue.makeDouble(
              value instanceof Number number ? number.doubleValue() : 0, time);
      case 0x100 ->
          NetworkTableValue.makeInteger(
              value instanceof Number number ? number.longValue() : 0, time);
      case 0x200 ->
          NetworkTableValue.makeFloat(
              value instanceof Number number ? number.floatValue() : 0, time);
      case 0x08 -> NetworkTableValue.makeRaw(value instanceof byte[] raw ? raw : new byte[0], time);
      case 0x10 ->
          NetworkTableValue.makeBooleanArray(
              value instanceof boolean[] items ? items : new boolean[0], time);
      case 0x20 ->
          NetworkTableValue.makeDoubleArray(
              value instanceof double[] items ? items : new double[0], time);
      case 0x40 ->
          NetworkTableValue.makeStringArray(
              value instanceof String[] items ? items : new String[0], time);
      case 0x400 ->
          NetworkTableValue.makeIntegerArray(
              value instanceof long[] items ? items : new long[0], time);
      case 0x800 ->
          NetworkTableValue.makeFloatArray(
              value instanceof float[] items ? items : new float[0], time);
      default -> NetworkTableValue.makeString(value == null ? "" : String.valueOf(value), time);
    };
  }

  public TimestampedBoolean timestampedBoolean(int entry, boolean fallback) {
    boolean value = Boolean.TRUE.equals(get(entry, 0x01, fallback));
    return new TimestampedBoolean(lastChange(entry), 1, value);
  }

  public TimestampedInteger timestampedInteger(int entry, long fallback) {
    Object value = get(entry, 0x100, fallback);
    long number = value instanceof Number boxed ? boxed.longValue() : fallback;
    return new TimestampedInteger(lastChange(entry), 1, number);
  }

  public TimestampedFloat timestampedFloat(int entry, float fallback) {
    Object value = get(entry, 0x200, fallback);
    float number = value instanceof Number boxed ? boxed.floatValue() : fallback;
    return new TimestampedFloat(lastChange(entry), 1, number);
  }

  public TimestampedDouble timestampedDouble(int entry, double fallback) {
    Object value = get(entry, 0x02, fallback);
    double number = value instanceof Number boxed ? boxed.doubleValue() : fallback;
    return new TimestampedDouble(lastChange(entry), 1, number);
  }

  public TimestampedString timestampedString(int entry, String fallback) {
    Object value = get(entry, 0x04, fallback);
    return new TimestampedString(lastChange(entry), 1, value == null ? "" : String.valueOf(value));
  }

  public TimestampedRaw timestampedRaw(int entry, byte[] fallback) {
    Object value = get(entry, 0x08, fallback);
    return new TimestampedRaw(lastChange(entry), 1, value instanceof byte[] raw ? raw : fallback);
  }

  public TimestampedBooleanArray timestampedBooleanArray(int entry, boolean[] fallback) {
    Object value = get(entry, 0x10, fallback);
    return new TimestampedBooleanArray(
        lastChange(entry), 1, value instanceof boolean[] items ? items : fallback);
  }

  public TimestampedIntegerArray timestampedIntegerArray(int entry, long[] fallback) {
    Object value = get(entry, 0x400, fallback);
    return new TimestampedIntegerArray(
        lastChange(entry), 1, value instanceof long[] items ? items : fallback);
  }

  public TimestampedFloatArray timestampedFloatArray(int entry, float[] fallback) {
    Object value = get(entry, 0x800, fallback);
    return new TimestampedFloatArray(
        lastChange(entry), 1, value instanceof float[] items ? items : fallback);
  }

  public TimestampedDoubleArray timestampedDoubleArray(int entry, double[] fallback) {
    Object value = get(entry, 0x20, fallback);
    return new TimestampedDoubleArray(
        lastChange(entry), 1, value instanceof double[] items ? items : fallback);
  }

  public TimestampedStringArray timestampedStringArray(int entry, String[] fallback) {
    Object value = get(entry, 0x40, fallback);
    return new TimestampedStringArray(
        lastChange(entry), 1, value instanceof String[] items ? items : fallback);
  }

  private static Object copy(Object value) {
    if (value instanceof boolean[] items) {
      return items.clone();
    }
    if (value instanceof long[] items) {
      return items.clone();
    }
    if (value instanceof float[] items) {
      return items.clone();
    }
    if (value instanceof double[] items) {
      return items.clone();
    }
    if (value instanceof String[] items) {
      return items.clone();
    }
    if (value instanceof byte[] items) {
      return items.clone();
    }
    return value;
  }

  private static String[] split(String raw) {
    String body = raw.replace("[", "").replace("]", "").trim();
    return body.isEmpty() ? new String[0] : body.split("\\s*,\\s*");
  }

  private static boolean[] decodeBooleans(String raw) {
    String[] parts = split(raw);
    boolean[] values = new boolean[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Boolean.parseBoolean(parts[i]);
    }
    return values;
  }

  private static double[] decodeDoubles(String raw) {
    String[] parts = split(raw);
    double[] values = new double[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Double.parseDouble(parts[i]);
    }
    return values;
  }

  private static float[] decodeFloats(String raw) {
    String[] parts = split(raw);
    float[] values = new float[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Float.parseFloat(parts[i]);
    }
    return values;
  }

  private static long[] decodeLongs(String raw) {
    String[] parts = split(raw);
    long[] values = new long[parts.length];
    for (int i = 0; i < parts.length; i++) {
      values[i] = Long.parseLong(parts[i]);
    }
    return values;
  }
}
