package org.teamvan.hal;

import java.nio.ByteBuffer;
import java.util.OptionalLong;
import org.wpilib.networktables.ConnectionInfo;
import org.wpilib.networktables.NetworkTableEvent;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.NetworkTableValue;
import org.wpilib.networktables.PubSubOptions;
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

/** Java bodies for the former NetworkTablesJNI native methods. */
public final class NtBridge {
  private NtBridge() {}

  public static int NetworkTablesJNI_getDefaultInstance() {
    return NtStore.TABLES.defaultInstance();
  }

  public static int NetworkTablesJNI_createInstance() {
    return NtStore.TABLES.createInstance();
  }

  public static void NetworkTablesJNI_destroyInstance(int a0) {
    NtStore.TABLES.destroyInstance(a0);
  }

  public static int NetworkTablesJNI_getInstanceFromHandle(int a0) {
    return NtStore.TABLES.instanceFromHandle(a0);
  }

  public static int NetworkTablesJNI_getEntryImpl(int a0, int a1, String a2, PubSubOptions a3) {
    return NtStore.TABLES.subscribe(a0, a1, a2);
  }

  public static int NetworkTablesJNI_getEntry(int a0, String a1) {
    return NtStore.TABLES.getEntry(a0, a1);
  }

  public static String NetworkTablesJNI_getEntryName(int a0) {
    return NtStore.TABLES.entryName(a0);
  }

  public static long NetworkTablesJNI_getEntryLastChange(int a0) {
    return NtStore.TABLES.lastChange(a0);
  }

  public static int NetworkTablesJNI_getType(int a0) {
    return NtStore.TABLES.type(a0);
  }

  public static int[] NetworkTablesJNI_getTopics(int a0, String a1, int a2) {
    return NtStore.TABLES.topics(a0, a1, a2);
  }

  public static int[] NetworkTablesJNI_getTopicsStr(int a0, String a1, String[] a2) {
    return NtStore.TABLES.topics(a0, a1, a2);
  }

  public static TopicInfo[] NetworkTablesJNI_getTopicInfos(
      NetworkTableInstance a0, int a1, String a2, int a3) {
    return NtStore.TABLES.infos(a0, a1, a2, a3);
  }

  public static TopicInfo[] NetworkTablesJNI_getTopicInfosStr(
      NetworkTableInstance a0, int a1, String a2, String[] a3) {
    return NtStore.TABLES.infos(a0, a1, a2, 0);
  }

  public static int NetworkTablesJNI_getTopic(int a0, String a1) {
    return NtStore.TABLES.getTopic(a0, a1);
  }

  public static String NetworkTablesJNI_getTopicName(int a0) {
    return NtStore.TABLES.entryName(a0);
  }

  public static int NetworkTablesJNI_getTopicType(int a0) {
    return NtStore.TABLES.type(a0);
  }

  public static void NetworkTablesJNI_setTopicPersistent(int a0, boolean a1) {
    NtStore.TABLES.setFlag(a0, "persistent", a1);
  }

  public static boolean NetworkTablesJNI_getTopicPersistent(int a0) {
    return NtStore.TABLES.flag(a0, "persistent");
  }

  public static void NetworkTablesJNI_setTopicRetained(int a0, boolean a1) {
    NtStore.TABLES.setFlag(a0, "retained", a1);
  }

  public static boolean NetworkTablesJNI_getTopicRetained(int a0) {
    return NtStore.TABLES.flag(a0, "retained");
  }

  public static void NetworkTablesJNI_setTopicCached(int a0, boolean a1) {
    NtStore.TABLES.setFlag(a0, "cached", a1);
  }

  public static boolean NetworkTablesJNI_getTopicCached(int a0) {
    return NtStore.TABLES.flag(a0, "cached");
  }

  public static String NetworkTablesJNI_getTopicTypeString(int a0) {
    return NtStore.TABLES.typeString(a0);
  }

  public static boolean NetworkTablesJNI_getTopicExists(int a0) {
    return NtStore.TABLES.flag(a0, "exists");
  }

  public static String NetworkTablesJNI_getTopicProperty(int a0, String a1) {
    return NtStore.TABLES.property(a0, a1);
  }

  public static void NetworkTablesJNI_setTopicProperty(int a0, String a1, String a2) {
    NtStore.TABLES.setProperty(a0, a1, a2);
  }

  public static void NetworkTablesJNI_deleteTopicProperty(int a0, String a1) {
    NtStore.TABLES.setProperty(a0, a1, null);
  }

  public static String NetworkTablesJNI_getTopicProperties(int a0) {
    return NtStore.TABLES.properties(a0);
  }

  public static void NetworkTablesJNI_setTopicProperties(int a0, String a1) {}

  public static int NetworkTablesJNI_subscribe(int a0, int a1, String a2, PubSubOptions a3) {
    return NtStore.TABLES.subscribe(a0, a1, a2);
  }

  public static void NetworkTablesJNI_unsubscribe(int a0) {
    NtStore.TABLES.release(a0);
  }

  public static int NetworkTablesJNI_publish(int a0, int a1, String a2, PubSubOptions a3) {
    return NtStore.TABLES.publish(a0, a1, a2);
  }

  public static int NetworkTablesJNI_publishEx(
      int a0, int a1, String a2, String a3, PubSubOptions a4) {
    return NtStore.TABLES.publish(a0, a1, a2);
  }

  public static void NetworkTablesJNI_unpublish(int a0) {
    NtStore.TABLES.release(a0);
  }

  public static void NetworkTablesJNI_releaseEntry(int a0) {
    NtStore.TABLES.release(a0);
  }

  public static void NetworkTablesJNI_release(int a0) {
    NtStore.TABLES.release(a0);
  }

  public static int NetworkTablesJNI_getTopicFromHandle(int a0) {
    return a0;
  }

  public static int NetworkTablesJNI_subscribeMultiple(int a0, String[] a1, PubSubOptions a2) {
    return 0;
  }

  public static void NetworkTablesJNI_unsubscribeMultiple(int a0) {
    NtStore.TABLES.release(a0);
  }

  public static TimestampedBoolean NetworkTablesJNI_getAtomicBoolean(int a0, boolean a1) {
    return NtStore.TABLES.timestampedBoolean(a0, a1);
  }

  public static TimestampedBoolean[] NetworkTablesJNI_readQueueBoolean(int a0) {
    return new TimestampedBoolean[0];
  }

  public static boolean[] NetworkTablesJNI_readQueueValuesBoolean(int a0) {
    return new boolean[0];
  }

  public static boolean NetworkTablesJNI_setBoolean(int a0, long a1, boolean a2) {
    return NtStore.TABLES.set(a0, a1, 0x01, a2);
  }

  public static boolean NetworkTablesJNI_getBoolean(int a0, boolean a1) {
    return Boolean.TRUE.equals(NtStore.TABLES.get(a0, 0x01, a1));
  }

  public static boolean NetworkTablesJNI_setDefaultBoolean(int a0, long a1, boolean a2) {
    return NtStore.TABLES.set(a0, a1, 0x01, a2);
  }

  public static TimestampedInteger NetworkTablesJNI_getAtomicInteger(int a0, long a1) {
    return NtStore.TABLES.timestampedInteger(a0, a1);
  }

  public static TimestampedInteger[] NetworkTablesJNI_readQueueInteger(int a0) {
    return new TimestampedInteger[0];
  }

  public static long[] NetworkTablesJNI_readQueueValuesInteger(int a0) {
    return new long[0];
  }

  public static boolean NetworkTablesJNI_setInteger(int a0, long a1, long a2) {
    return NtStore.TABLES.set(a0, a1, 0x100, a2);
  }

  public static long NetworkTablesJNI_getInteger(int a0, long a1) {
    Object v = NtStore.TABLES.get(a0, 0x100, a1);
    return v instanceof Number n ? n.longValue() : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultInteger(int a0, long a1, long a2) {
    return NtStore.TABLES.set(a0, a1, 0x100, a2);
  }

  public static TimestampedFloat NetworkTablesJNI_getAtomicFloat(int a0, float a1) {
    return NtStore.TABLES.timestampedFloat(a0, a1);
  }

  public static TimestampedFloat[] NetworkTablesJNI_readQueueFloat(int a0) {
    return new TimestampedFloat[0];
  }

  public static float[] NetworkTablesJNI_readQueueValuesFloat(int a0) {
    return new float[0];
  }

  public static boolean NetworkTablesJNI_setFloat(int a0, long a1, float a2) {
    return NtStore.TABLES.set(a0, a1, 0x200, a2);
  }

  public static float NetworkTablesJNI_getFloat(int a0, float a1) {
    Object v = NtStore.TABLES.get(a0, 0x200, a1);
    return v instanceof Number n ? n.floatValue() : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultFloat(int a0, long a1, float a2) {
    return NtStore.TABLES.set(a0, a1, 0x200, a2);
  }

  public static TimestampedDouble NetworkTablesJNI_getAtomicDouble(int a0, double a1) {
    return NtStore.TABLES.timestampedDouble(a0, a1);
  }

  public static TimestampedDouble[] NetworkTablesJNI_readQueueDouble(int a0) {
    return new TimestampedDouble[0];
  }

  public static double[] NetworkTablesJNI_readQueueValuesDouble(int a0) {
    return new double[0];
  }

  public static boolean NetworkTablesJNI_setDouble(int a0, long a1, double a2) {
    return NtStore.TABLES.set(a0, a1, 0x02, a2);
  }

  public static double NetworkTablesJNI_getDouble(int a0, double a1) {
    Object v = NtStore.TABLES.get(a0, 0x02, a1);
    return v instanceof Number n ? n.doubleValue() : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultDouble(int a0, long a1, double a2) {
    return NtStore.TABLES.set(a0, a1, 0x02, a2);
  }

  public static TimestampedString NetworkTablesJNI_getAtomicString(int a0, String a1) {
    return NtStore.TABLES.timestampedString(a0, a1);
  }

  public static TimestampedString[] NetworkTablesJNI_readQueueString(int a0) {
    return new TimestampedString[0];
  }

  public static String[] NetworkTablesJNI_readQueueValuesString(int a0) {
    return new String[0];
  }

  public static boolean NetworkTablesJNI_setString(int a0, long a1, String a2) {
    return NtStore.TABLES.set(a0, a1, 0x04, a2);
  }

  public static String NetworkTablesJNI_getString(int a0, String a1) {
    Object v = NtStore.TABLES.get(a0, 0x04, a1);
    return v == null ? a1 : String.valueOf(v);
  }

  public static boolean NetworkTablesJNI_setDefaultString(int a0, long a1, String a2) {
    return NtStore.TABLES.set(a0, a1, 0x04, a2);
  }

  public static TimestampedRaw NetworkTablesJNI_getAtomicRaw(int a0, byte[] a1) {
    return NtStore.TABLES.timestampedRaw(a0, a1);
  }

  public static TimestampedRaw[] NetworkTablesJNI_readQueueRaw(int a0) {
    return new TimestampedRaw[0];
  }

  public static byte[][] NetworkTablesJNI_readQueueValuesRaw(int a0) {
    return new byte[0][];
  }

  public static boolean NetworkTablesJNI_setRaw(int a0, long a1, byte[] a2, int a3, int a4) {
    return NtStore.TABLES.set(a0, a1, 0x08, a2);
  }

  public static boolean NetworkTablesJNI_setRawBuffer(
      int a0, long a1, ByteBuffer a2, int a3, int a4) {
    return false;
  }

  public static byte[] NetworkTablesJNI_getRaw(int a0, byte[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x08, a1);
    return v instanceof byte[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultRaw(int a0, long a1, byte[] a2, int a3, int a4) {
    return NtStore.TABLES.set(a0, a1, 0x08, a2);
  }

  public static boolean NetworkTablesJNI_setDefaultRawBuffer(
      int a0, long a1, ByteBuffer a2, int a3, int a4) {
    return false;
  }

  public static TimestampedBooleanArray NetworkTablesJNI_getAtomicBooleanArray(
      int a0, boolean[] a1) {
    return NtStore.TABLES.timestampedBooleanArray(a0, a1);
  }

  public static TimestampedBooleanArray[] NetworkTablesJNI_readQueueBooleanArray(int a0) {
    return new TimestampedBooleanArray[0];
  }

  public static boolean[][] NetworkTablesJNI_readQueueValuesBooleanArray(int a0) {
    return new boolean[0][];
  }

  public static boolean NetworkTablesJNI_setBooleanArray(int a0, long a1, boolean[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x10, a2);
  }

  public static boolean[] NetworkTablesJNI_getBooleanArray(int a0, boolean[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x10, a1);
    return v instanceof boolean[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultBooleanArray(int a0, long a1, boolean[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x10, a2);
  }

  public static TimestampedIntegerArray NetworkTablesJNI_getAtomicIntegerArray(int a0, long[] a1) {
    return NtStore.TABLES.timestampedIntegerArray(a0, a1);
  }

  public static TimestampedIntegerArray[] NetworkTablesJNI_readQueueIntegerArray(int a0) {
    return new TimestampedIntegerArray[0];
  }

  public static long[][] NetworkTablesJNI_readQueueValuesIntegerArray(int a0) {
    return new long[0][];
  }

  public static boolean NetworkTablesJNI_setIntegerArray(int a0, long a1, long[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x400, a2);
  }

  public static long[] NetworkTablesJNI_getIntegerArray(int a0, long[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x400, a1);
    return v instanceof long[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultIntegerArray(int a0, long a1, long[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x400, a2);
  }

  public static TimestampedFloatArray NetworkTablesJNI_getAtomicFloatArray(int a0, float[] a1) {
    return NtStore.TABLES.timestampedFloatArray(a0, a1);
  }

  public static TimestampedFloatArray[] NetworkTablesJNI_readQueueFloatArray(int a0) {
    return new TimestampedFloatArray[0];
  }

  public static float[][] NetworkTablesJNI_readQueueValuesFloatArray(int a0) {
    return new float[0][];
  }

  public static boolean NetworkTablesJNI_setFloatArray(int a0, long a1, float[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x800, a2);
  }

  public static float[] NetworkTablesJNI_getFloatArray(int a0, float[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x800, a1);
    return v instanceof float[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultFloatArray(int a0, long a1, float[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x800, a2);
  }

  public static TimestampedDoubleArray NetworkTablesJNI_getAtomicDoubleArray(int a0, double[] a1) {
    return NtStore.TABLES.timestampedDoubleArray(a0, a1);
  }

  public static TimestampedDoubleArray[] NetworkTablesJNI_readQueueDoubleArray(int a0) {
    return new TimestampedDoubleArray[0];
  }

  public static double[][] NetworkTablesJNI_readQueueValuesDoubleArray(int a0) {
    return new double[0][];
  }

  public static boolean NetworkTablesJNI_setDoubleArray(int a0, long a1, double[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x20, a2);
  }

  public static double[] NetworkTablesJNI_getDoubleArray(int a0, double[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x20, a1);
    return v instanceof double[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultDoubleArray(int a0, long a1, double[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x20, a2);
  }

  public static TimestampedStringArray NetworkTablesJNI_getAtomicStringArray(int a0, String[] a1) {
    return NtStore.TABLES.timestampedStringArray(a0, a1);
  }

  public static TimestampedStringArray[] NetworkTablesJNI_readQueueStringArray(int a0) {
    return new TimestampedStringArray[0];
  }

  public static String[][] NetworkTablesJNI_readQueueValuesStringArray(int a0) {
    return new String[0][];
  }

  public static boolean NetworkTablesJNI_setStringArray(int a0, long a1, String[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x40, a2);
  }

  public static String[] NetworkTablesJNI_getStringArray(int a0, String[] a1) {
    Object v = NtStore.TABLES.get(a0, 0x40, a1);
    return v instanceof String[] b ? b : a1;
  }

  public static boolean NetworkTablesJNI_setDefaultStringArray(int a0, long a1, String[] a2) {
    return NtStore.TABLES.set(a0, a1, 0x40, a2);
  }

  public static NetworkTableValue[] NetworkTablesJNI_readQueueValue(int a0) {
    return NtStore.TABLES.queueValues(a0);
  }

  public static NetworkTableValue NetworkTablesJNI_getValue(int a0) {
    return NtStore.TABLES.value(a0);
  }

  public static void NetworkTablesJNI_setEntryFlags(int a0, int a1) {}

  public static int NetworkTablesJNI_getEntryFlags(int a0) {
    return 0;
  }

  public static TopicInfo NetworkTablesJNI_getTopicInfo(NetworkTableInstance a0, int a1) {
    return NtStore.TABLES.info(a0, a1);
  }

  public static int NetworkTablesJNI_createListenerPoller(int a0) {
    return 1;
  }

  public static void NetworkTablesJNI_destroyListenerPoller(int a0) {}

  public static int NetworkTablesJNI_addListener(int a0, String[] a1, int a2) {
    return 1;
  }

  public static int NetworkTablesJNI_addListener(int a0, int a1, int a2) {
    return 1;
  }

  public static NetworkTableEvent[] NetworkTablesJNI_readListenerQueue(
      NetworkTableInstance a0, int a1) {
    return NtStore.TABLES.events(a0, a1);
  }

  public static void NetworkTablesJNI_removeListener(int a0) {}

  public static int NetworkTablesJNI_getNetworkMode(int a0) {
    return 0;
  }

  public static void NetworkTablesJNI_startLocal(int a0) {}

  public static void NetworkTablesJNI_stopLocal(int a0) {}

  public static void NetworkTablesJNI_startServer(
      int a0, String a1, String a2, String a3, int a4) {}

  public static void NetworkTablesJNI_stopServer(int a0) {}

  public static void NetworkTablesJNI_startClient(int a0, String a1) {}

  public static void NetworkTablesJNI_stopClient(int a0) {}

  public static void NetworkTablesJNI_setServer(int a0, String a1, int a2) {}

  public static void NetworkTablesJNI_setServer(int a0, String[] a1, int[] a2) {}

  public static void NetworkTablesJNI_setServerMdns(int a0, String a1) {}

  public static void NetworkTablesJNI_setServerMdns(int a0, String a1, String a2, int a3) {}

  public static void NetworkTablesJNI_setServerMdns(int a0, String a1, String[] a2, int[] a3) {}

  public static void NetworkTablesJNI_setServerMdns(
      int a0, String a1, int a2, String[] a3, int[] a4) {}

  public static void NetworkTablesJNI_setServerTeam(int a0, String a1, int a2) {}

  public static void NetworkTablesJNI_setServerFixed(int a0, String a1, int a2) {}

  public static void NetworkTablesJNI_disconnect(int a0) {}

  public static void NetworkTablesJNI_startDSClient(int a0, int a1) {}

  public static void NetworkTablesJNI_stopDSClient(int a0) {}

  public static void NetworkTablesJNI_flushLocal(int a0) {}

  public static void NetworkTablesJNI_flush(int a0) {}

  public static ConnectionInfo[] NetworkTablesJNI_getConnections(int a0) {
    return NtStore.TABLES.connections(a0);
  }

  public static boolean NetworkTablesJNI_isConnected(int a0) {
    return NtStore.TABLES.connected(a0);
  }

  public static OptionalLong NetworkTablesJNI_getServerTimeOffset(int a0) {
    return NtStore.TABLES.serverOffset(a0);
  }

  public static long NetworkTablesJNI_now() {
    return NtStore.TABLES.now();
  }

  public static int NetworkTablesJNI_startEntryDataLog(int a0, long a1, String a2, String a3) {
    return 0;
  }

  public static void NetworkTablesJNI_stopEntryDataLog(int a0) {}

  public static int NetworkTablesJNI_startConnectionDataLog(int a0, long a1, String a2) {
    return 0;
  }

  public static void NetworkTablesJNI_stopConnectionDataLog(int a0) {}

  public static int NetworkTablesJNI_addLogger(int a0, int a1, int a2) {
    return 1;
  }
}
