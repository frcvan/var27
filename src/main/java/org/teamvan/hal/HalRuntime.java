package org.teamvan.hal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import org.wpilib.hardware.hal.AllianceStationID;
import org.wpilib.hardware.hal.HALValue;
import org.wpilib.hardware.hal.MatchInfoData;
import org.wpilib.hardware.hal.OpModeOption;
import org.wpilib.hardware.hal.PowerDistributionVersion;
import org.wpilib.hardware.hal.REVPHVersion;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.hardware.hal.can.CANReceiveMessage;
import org.wpilib.hardware.hal.can.CANStatus;
import org.wpilib.hardware.hal.can.CANStreamMessage;
import org.wpilib.hardware.hal.simulation.BufferCallback;
import org.wpilib.hardware.hal.simulation.ConstBufferCallback;
import org.wpilib.hardware.hal.simulation.NotifyCallback;
import org.wpilib.hardware.hal.simulation.SimDeviceCallback;
import org.wpilib.hardware.hal.simulation.SimDeviceDataJNI;
import org.wpilib.hardware.hal.simulation.SimValueCallback;
import org.wpilib.util.AlertDataJNI;
import org.wpilib.util.AlertException;

/**
 * Pure-Java stand-in for the WPILib 2027 alpha-7 HAL and WPIUtil native libraries.
 *
 * <p>Behavior follows {@code hal/src/main/native/sim} and {@code wpiutil} at tag {@code
 * v2027.0.0-alpha-7}. No {@code System.loadLibrary} and no JNI.
 */
public final class HalRuntime {
  public static final int NUM_CAN_BUSES = 5;
  public static final int NUM_SMART_IO = 6;
  public static final int NUM_ANALOG_INPUTS = 8;
  public static final int NUM_COUNTERS = 8;
  public static final int NUM_DIGITAL_CHANNELS = 31;
  public static final int NUM_PWM_CHANNELS = 20;
  public static final int NUM_PWM_HEADERS = 10;
  public static final int NUM_DIGITAL_PWM_OUTPUTS = 6;
  public static final int NUM_ENCODERS = 8;
  public static final int NUM_CTREPCM_MODULES = 63;
  public static final int NUM_CTRE_SOLENOID_CHANNELS = 8;
  public static final int NUM_CTREPDP_MODULES = 63;
  public static final int NUM_CTREPDP_CHANNELS = 16;
  public static final int NUM_REVPDH_MODULES = 63;
  public static final int NUM_REVPDH_CHANNELS = 24;
  public static final int NUM_DUTY_CYCLES = 6;
  public static final int NUM_ADDRESSABLE_LEDS = 6;
  public static final int NUM_REVPH_MODULES = 63;
  public static final int NUM_REVPH_CHANNELS = 16;
  public static final int NUM_JOYSTICKS = 6;
  public static final int MAX_AXES = 12;
  public static final int MAX_POVS = 8;
  public static final int MAX_BUTTONS = 64;
  public static final int MAX_TOUCHPADS = 2;
  public static final int MAX_FINGERS = 2;

  public static final int HAL_SUCCESS = 0;
  public static final int HAL_VOLTAGE_OUT_OF_RANGE = 1002;
  public static final int HAL_INCOMPATIBLE_STATE = 1015;
  public static final int HAL_NO_AVAILABLE_RESOURCES = -1004;
  public static final int HAL_NULL_PARAMETER = -1005;
  public static final int HAL_PARAMETER_OUT_OF_RANGE = -1028;
  public static final int HAL_RESOURCE_IS_ALLOCATED = -1029;
  public static final int HAL_RESOURCE_OUT_OF_RANGE = -1030;
  public static final int HAL_HANDLE_ERROR = -1098;
  public static final int HAL_CAN_TIMEOUT = -1154;
  public static final int HAL_CAN_BUFFER_OVERRUN = -35007;
  public static final int HAL_ERR_CAN_MESSAGE_NOT_FOUND = -44002;
  public static final int INVALID_HANDLE = 0;
  public static final long NO_ALARM = Long.MAX_VALUE;
  public static final int HANDLE_ERROR_BIT = 0x80000000;
  public static final int HANDLE_TYPE_EVENT = 1;
  public static final int HANDLE_TYPE_SEMAPHORE = 2;
  public static final int HANDLE_TYPE_NOTIFIER = 3;
  public static final int ALERT_HANDLE_TYPE = 23;
  public static final int ALERT_ERROR = -1;
  public static final int ALERT_ALREADY_ALLOCATED = -2;

  public static final long OPMODE_HASH_MASK = 0x00FFFFFFFFFFFFFFL;
  public static final long ROBOT_MODE_MASK = 0x0300000000000000L;
  public static final long ROBOT_MODE_SHIFT = 56;
  public static final long ENABLED_MASK = 0x0400000000000000L;
  public static final long ESTOP_MASK = 0x0800000000000000L;
  public static final long FMS_ATTACHED_MASK = 0x1000000000000000L;
  public static final long DS_ATTACHED_MASK = 0x2000000000000000L;

  private static final int COLOR_ORDER_RGB = 0;
  private static final int COLOR_ORDER_RBG = 1;
  private static final int COLOR_ORDER_BGR = 2;
  private static final int COLOR_ORDER_BRG = 3;
  private static final int COLOR_ORDER_GBR = 4;
  private static final int COLOR_ORDER_GRB = 5;

  private HalRuntime() {}

  // ---------------------------------------------------------------------------
  // Synchronization. Handle layout matches wpiutil Synchronization.cpp.
  // ---------------------------------------------------------------------------

  static final class SignalState {
    int signaled;
    boolean autoReset;
    final List<Condition> waiters = new ArrayList<>();
  }

  private static final ReentrantLock syncLock = new ReentrantLock();
  private static final Map<Integer, SignalState> signals = new HashMap<>();
  private static final Map<Integer, Integer> semaphoreMax = new HashMap<>();
  private static int nextEventIndex = 1;
  private static int nextSemaphoreIndex = 1;

  private static int handleType(int handle) {
    return (handle >>> 24) & 0xff;
  }

  public static int makeEvent(boolean manualReset, boolean initialState) {
    syncLock.lock();
    try {
      int handle = (HANDLE_TYPE_EVENT << 24) | (nextEventIndex++ & 0xffffff);
      SignalState state = new SignalState();
      state.signaled = initialState ? 1 : 0;
      state.autoReset = !manualReset;
      signals.put(handle, state);
      return handle;
    } finally {
      syncLock.unlock();
    }
  }

  public static void destroyEvent(int handle) {
    if (handleType(handle) != HANDLE_TYPE_EVENT) {
      return;
    }
    destroySignalObject(handle);
  }

  public static void setEvent(int handle) {
    if (handleType(handle) == HANDLE_TYPE_EVENT) {
      setSignalObject(handle);
    }
  }

  public static void resetEvent(int handle) {
    if (handleType(handle) == HANDLE_TYPE_EVENT) {
      resetSignalObject(handle);
    }
  }

  public static int makeSemaphore(int initialCount, int maximumCount) {
    syncLock.lock();
    try {
      int handle = (HANDLE_TYPE_SEMAPHORE << 24) | (nextSemaphoreIndex++ & 0xffffff);
      SignalState state = new SignalState();
      state.signaled = initialCount;
      state.autoReset = true;
      signals.put(handle, state);
      semaphoreMax.put(handle, maximumCount);
      return handle;
    } finally {
      syncLock.unlock();
    }
  }

  public static void destroySemaphore(int handle) {
    if (handleType(handle) != HANDLE_TYPE_SEMAPHORE) {
      return;
    }
    destroySignalObject(handle);
    syncLock.lock();
    try {
      semaphoreMax.remove(handle);
    } finally {
      syncLock.unlock();
    }
  }

  public static boolean releaseSemaphore(int handle, int releaseCount) {
    if (handleType(handle) != HANDLE_TYPE_SEMAPHORE || releaseCount <= 0) {
      return false;
    }
    syncLock.lock();
    try {
      SignalState state = signals.get(handle);
      Integer max = semaphoreMax.get(handle);
      if (state == null || max == null) {
        return false;
      }
      if (max - state.signaled < releaseCount) {
        return false;
      }
      state.signaled += releaseCount;
      for (Condition waiter : state.waiters) {
        waiter.signalAll();
      }
      return true;
    } finally {
      syncLock.unlock();
    }
  }

  public static void createSignalObject(int handle, boolean manualReset, boolean initialState) {
    syncLock.lock();
    try {
      SignalState state = signals.computeIfAbsent(handle, ignored -> new SignalState());
      state.signaled = initialState ? 1 : 0;
      state.autoReset = !manualReset;
    } finally {
      syncLock.unlock();
    }
  }

  public static void setSignalObject(int handle) {
    syncLock.lock();
    try {
      SignalState state = signals.get(handle);
      if (state == null) {
        return;
      }
      state.signaled = 1;
      for (Condition waiter : state.waiters) {
        waiter.signalAll();
        if (state.autoReset) {
          break;
        }
      }
    } finally {
      syncLock.unlock();
    }
  }

  public static void resetSignalObject(int handle) {
    syncLock.lock();
    try {
      SignalState state = signals.get(handle);
      if (state != null) {
        state.signaled = 0;
      }
    } finally {
      syncLock.unlock();
    }
  }

  public static void destroySignalObject(int handle) {
    syncLock.lock();
    try {
      SignalState state = signals.remove(handle);
      if (state != null) {
        for (Condition waiter : state.waiters) {
          waiter.signalAll();
        }
      }
    } finally {
      syncLock.unlock();
    }
  }

  /**
   * @return true if signaled, false if destroyed or timed out. timedOut[0] set when non-null.
   */
  public static boolean waitForObject(int handle, double timeoutSeconds, boolean[] timedOut)
      throws InterruptedException {
    int[] signaled = waitForObjects(new int[] {handle}, timeoutSeconds, timedOut);
    if (signaled.length == 0) {
      return false;
    }
    return (signaled[0] & HANDLE_ERROR_BIT) == 0;
  }

  public static int[] waitForObjects(int[] handles, double timeoutSeconds, boolean[] timedOut)
      throws InterruptedException {
    syncLock.lock();
    try {
      Condition cv = syncLock.newCondition();
      boolean added = false;
      boolean timedOutVal = false;
      List<Integer> found = new ArrayList<>();
      long deadline =
          timeoutSeconds < 0
              ? Long.MAX_VALUE
              : System.nanoTime() + (long) (timeoutSeconds * 1_000_000_000.0);
      try {
        for (; ; ) {
          found.clear();
          for (int handle : handles) {
            SignalState state = signals.get(handle);
            if (state == null) {
              found.add(handle | HANDLE_ERROR_BIT);
            } else if (state.signaled > 0) {
              found.add(handle);
              if (state.autoReset) {
                state.signaled--;
                if (state.signaled < 0) {
                  state.signaled = 0;
                }
              }
            }
          }
          if (timedOutVal || !found.isEmpty()) {
            break;
          }
          if (timeoutSeconds == 0) {
            timedOutVal = true;
            break;
          }
          if (!added) {
            added = true;
            for (int handle : handles) {
              signals.computeIfAbsent(handle, ignored -> new SignalState()).waiters.add(cv);
            }
          }
          if (timeoutSeconds < 0) {
            cv.await();
          } else {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0 || cv.awaitNanos(remaining) <= 0) {
              timedOutVal = true;
            }
          }
        }
      } finally {
        if (added) {
          for (int handle : handles) {
            SignalState state = signals.get(handle);
            if (state != null) {
              state.waiters.remove(cv);
            }
          }
        }
      }
      if (timedOut != null && timedOut.length > 0) {
        timedOut[0] = timedOutVal;
      }
      int[] out = new int[found.size()];
      for (int i = 0; i < found.size(); i++) {
        out[i] = found.get(i);
      }
      return out;
    } finally {
      syncLock.unlock();
    }
  }

  // ---------------------------------------------------------------------------
  // Time. WPI_Now is redirected to HAL monotonic time by HAL.initialize().
  // ---------------------------------------------------------------------------

  private static final long originalProgramStart = System.nanoTime();
  private static final AtomicLong programStartTime = new AtomicLong(originalProgramStart);
  private static final AtomicBoolean mockTimeEnabled = new AtomicBoolean();
  private static final AtomicLong mockNow = new AtomicLong();
  private static final AtomicBoolean halNowInstalled = new AtomicBoolean();
  private static final AtomicLong pauseTime = new AtomicLong();
  private static final AtomicLong offsetTime = new AtomicLong();

  public static long nowDefault() {
    return System.nanoTime();
  }

  public static long now() {
    if (mockTimeEnabled.get()) {
      return mockNow.get();
    }
    if (halNowInstalled.get()) {
      return monotonicTime();
    }
    return nowDefault();
  }

  public static long monotonicTime() {
    long cur = pauseTime.get();
    if (cur == 0) {
      cur = nowDefault();
    }
    return cur + offsetTime.get();
  }

  public static void enableMockTime() {
    mockTimeEnabled.set(true);
    mockNow.set(0);
    programStartTime.set(0);
  }

  public static void disableMockTime() {
    mockTimeEnabled.set(false);
    programStartTime.set(originalProgramStart);
  }

  public static void setMockTime(long time) {
    mockNow.set(time);
  }

  public static long getProgramStartTime() {
    return programStartTime.get();
  }

  public static long getSystemTime() {
    return System.currentTimeMillis() * 1_000_000L;
  }

  public static void installHalNow() {
    halNowInstalled.set(true);
    programStartTime.set(monotonicTime());
  }

  public static void restartTiming() {
    programStartTime.set(monotonicTime());
  }

  public static void pauseTiming() {
    if (pauseTime.get() == 0) {
      pauseTime.set(nowDefault());
    }
    Notifiers.pause();
  }

  public static void resumeTiming() {
    long paused = pauseTime.get();
    if (paused != 0) {
      offsetTime.addAndGet(-(nowDefault() - paused));
      pauseTime.set(0);
    }
    Notifiers.resume();
  }

  public static boolean isTimingPaused() {
    return pauseTime.get() != 0;
  }

  public static void stepTiming(long delta) {
    offsetTime.addAndGet(delta);
  }

  // ---------------------------------------------------------------------------
  // Alerts.
  // ---------------------------------------------------------------------------

  static final class AlertRecord {
    final String group;
    final String id;
    String text;
    final int level;
    final int generation;
    long activeStartTime;

    AlertRecord(String group, String id, String text, int level, int generation) {
      this.group = group == null ? "" : group;
      this.id = id == null ? "" : id;
      this.text = text == null ? "" : text;
      this.level = level;
      this.generation = generation;
    }
  }

  private static final Object alertLock = new Object();
  private static final List<AlertRecord> alerts = new ArrayList<>();
  private static int nextAlertGeneration;

  private static int alertHandle(int index, int generation) {
    return (ALERT_HANDLE_TYPE << 24) | ((generation & 0xff) << 16) | (index & 0xffff);
  }

  private static AlertRecord alert(int handle) {
    if (((handle >>> 24) & 0xff) != ALERT_HANDLE_TYPE) {
      return null;
    }
    int index = handle & 0xffff;
    int generation = (handle >>> 16) & 0xff;
    if (index >= alerts.size()) {
      return null;
    }
    AlertRecord record = alerts.get(index);
    if (record == null || record.generation != generation) {
      return null;
    }
    return record;
  }

  public static int createAlert(String group, String id, String text, int level) {
    synchronized (alertLock) {
      String g = group == null ? "" : group;
      String i = id == null ? "" : id;
      for (AlertRecord existing : alerts) {
        if (existing != null
            && existing.group.equals(g)
            && existing.id.equals(i)
            && existing.level == level) {
          throw new AlertException("Alert already allocated", ALERT_ALREADY_ALLOCATED);
        }
      }
      int generation = nextAlertGeneration++ & 0xff;
      int index = alerts.size();
      alerts.add(new AlertRecord(g, i, text, level, generation));
      return alertHandle(index, generation);
    }
  }

  public static void destroyAlert(int handle) {
    synchronized (alertLock) {
      if (((handle >>> 24) & 0xff) != ALERT_HANDLE_TYPE) {
        return;
      }
      int index = handle & 0xffff;
      if (index < alerts.size() && alert(handle) != null) {
        alerts.set(index, null);
      }
    }
  }

  public static void setAlertActive(int handle, boolean active) {
    synchronized (alertLock) {
      AlertRecord record = alert(handle);
      if (record == null) {
        throw new AlertException("Alert operation failed", ALERT_ERROR);
      }
      if (active) {
        if (record.activeStartTime == 0) {
          long time = now();
          record.activeStartTime = time == 0 ? 1 : time;
        }
      } else {
        record.activeStartTime = 0;
      }
    }
  }

  public static boolean isAlertActive(int handle) {
    synchronized (alertLock) {
      AlertRecord record = alert(handle);
      if (record == null) {
        throw new AlertException("Alert operation failed", ALERT_ERROR);
      }
      return record.activeStartTime != 0;
    }
  }

  public static void setAlertText(int handle, String text) {
    synchronized (alertLock) {
      AlertRecord record = alert(handle);
      if (record == null) {
        throw new AlertException("Alert operation failed", ALERT_ERROR);
      }
      record.text = text == null ? "" : text;
    }
  }

  public static String getAlertText(int handle) {
    synchronized (alertLock) {
      AlertRecord record = alert(handle);
      if (record == null) {
        throw new AlertException("Alert operation failed", ALERT_ERROR);
      }
      return record.text;
    }
  }

  public static int getNumAlerts() {
    synchronized (alertLock) {
      int count = 0;
      for (AlertRecord record : alerts) {
        if (record != null) {
          count++;
        }
      }
      return count;
    }
  }

  public static AlertDataJNI.AlertInfo[] getAlerts() {
    synchronized (alertLock) {
      List<AlertDataJNI.AlertInfo> infos = new ArrayList<>();
      for (AlertRecord record : alerts) {
        if (record == null) {
          continue;
        }
        infos.add(
            new AlertDataJNI.AlertInfo(
                record.group, record.id, record.text, record.activeStartTime, record.level));
      }
      return infos.toArray(AlertDataJNI.AlertInfo[]::new);
    }
  }

  public static void resetAlertData() {
    synchronized (alertLock) {
      alerts.clear();
    }
  }

  // ---------------------------------------------------------------------------
  // Raw frames. Pointers are Java object identities, never native addresses.
  // ---------------------------------------------------------------------------

  static final class Frame {
    ByteBuffer data;
    int size;
    int width;
    int height;
    int stride;
    int pixelFormat;
    long timestamp;
    int timeSource;
  }

  private static final ConcurrentHashMap<Long, Frame> frames = new ConcurrentHashMap<>();
  private static final AtomicLong nextFrame = new AtomicLong(1);

  public static long allocateRawFrame() {
    long id = nextFrame.getAndIncrement();
    frames.put(id, new Frame());
    return id;
  }

  public static void freeRawFrame(long frame) {
    frames.remove(frame);
  }

  public static Frame frame(long id) {
    Frame frame = frames.get(id);
    if (frame == null) {
      throw new NullPointerException("frame is null");
    }
    return frame;
  }

  public static long getRawFrameDataPtr(long frame) {
    Frame value = frame(frame);
    return value.data == null ? 0 : System.identityHashCode(value.data);
  }

  // ---------------------------------------------------------------------------
  // HAL lifecycle.
  // ---------------------------------------------------------------------------

  private static final Object initLock = new Object();
  private static boolean initialized;
  private static boolean shutdown;
  private static int runtimeType = 1;
  private static int halErrno;
  private static boolean hasMain;
  private static final Object mainLock = new Object();
  private static boolean mainExited;
  private static final List<Runnable> shutdownHooks = new ArrayList<>();

  public static boolean initialize() {
    synchronized (initLock) {
      if (initialized) {
        return true;
      }
      installHalNow();
      Notifiers.start();
      JsHost.install();
      initialized = true;
      shutdown = false;
      return true;
    }
  }

  public static boolean isInitialized() {
    return initialized;
  }

  public static void shutdownHal() {
    List<Runnable> hooks;
    synchronized (initLock) {
      hooks = new ArrayList<>(shutdownHooks);
      shutdownHooks.clear();
      shutdown = true;
    }
    for (Runnable hook : hooks) {
      hook.run();
    }
    Notifiers.stop();
  }

  public static void addShutdownHook(Runnable hook) {
    synchronized (initLock) {
      shutdownHooks.add(hook);
    }
  }

  public static boolean isShutdown() {
    return shutdown;
  }

  public static void setRuntimeType(int type) {
    runtimeType = type;
  }

  public static int getRuntimeType() {
    return runtimeType;
  }

  public static void setErrno(int code) {
    halErrno = code;
  }

  public static int getErrno() {
    return halErrno;
  }

  public static String errorMessage(int code) {
    return switch (code) {
      case 0 -> "";
      case HAL_VOLTAGE_OUT_OF_RANGE ->
          "HAL: Voltage to convert to raw value is out of range [0; 5]";
      case HAL_INCOMPATIBLE_STATE -> "HAL: Incompatible State: The operation cannot be completed";
      case HAL_NO_AVAILABLE_RESOURCES -> "HAL: No available resources to allocate";
      case HAL_NULL_PARAMETER -> "HAL: A pointer parameter to a method is NULL";
      case HAL_PARAMETER_OUT_OF_RANGE -> "HAL: A parameter is out of range.";
      case HAL_RESOURCE_IS_ALLOCATED -> "HAL: Resource already allocated";
      case HAL_RESOURCE_OUT_OF_RANGE -> "HAL: The requested resource is out of range.";
      case HAL_HANDLE_ERROR -> "HAL: A handle parameter was passed incorrectly";
      case HAL_CAN_TIMEOUT -> "HAL: CAN Receive has Timed Out";
      case -1155 -> "HAL: Method not supported in sim";
      case HAL_CAN_BUFFER_OVERRUN -> "HAL: CAN Output Buffer Full. Ensure a device is attached";
      default -> "Unknown error status";
    };
  }

  public static String strerror(int errno) {
    if (errno == 0) {
      return errorMessage(halErrno);
    }
    return errorMessage(errno);
  }

  public static boolean hasMain() {
    return hasMain;
  }

  public static void setMain() {
    hasMain = true;
  }

  public static void runMain() {
    synchronized (mainLock) {
      while (!mainExited) {
        try {
          mainLock.wait();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  public static void exitMain() {
    synchronized (mainLock) {
      mainExited = true;
      mainLock.notifyAll();
    }
  }

  // ---------------------------------------------------------------------------
  // Notifiers. Alarm processing matches hal sim Notifier.cpp.
  // ---------------------------------------------------------------------------

  public static final class Notifiers {
    static final class Notifier {
      String name = "";
      long alarmTime = NO_ALARM;
      long intervalTime;
      int overrunCount;
      int userOverrunCount;
      boolean handlerSignaled;
      boolean alive = true;
    }

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition cond = lock.newCondition();
    private static final Map<Integer, Notifier> notifiers = new HashMap<>();
    private static final PriorityQueue<Integer> alarms =
        new PriorityQueue<>(
            (a, b) -> {
              Notifier left = notifiers.get(a);
              Notifier right = notifiers.get(b);
              long at = left == null ? NO_ALARM : left.alarmTime;
              long bt = right == null ? NO_ALARM : right.alarmTime;
              return Long.compare(at, bt);
            });
    private static int nextIndex = 1;
    private static final AtomicLong alarmSetCount = new AtomicLong();
    private static volatile boolean paused;
    private static volatile boolean running;
    private static Thread thread;

    private Notifiers() {}

    static void start() {
      if (running) {
        return;
      }
      running = true;
      thread =
          new Thread(
              () -> {
                lock.lock();
                try {
                  while (running) {
                    if (paused || alarms.isEmpty()) {
                      cond.await();
                      continue;
                    }
                    Notifier next = notifiers.get(alarms.peek());
                    long now = monotonicTime();
                    if (next != null && next.alarmTime > now) {
                      cond.awaitNanos(next.alarmTime - now);
                    }
                    if (!running) {
                      break;
                    }
                    if (paused) {
                      continue;
                    }
                    processAlarms(false);
                  }
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                } finally {
                  lock.unlock();
                }
              },
              "HAL Notifier");
      thread.setDaemon(true);
      thread.start();
    }

    static void stop() {
      lock.lock();
      try {
        running = false;
        cond.signalAll();
      } finally {
        lock.unlock();
      }
    }

    public static int create() {
      initialize();
      lock.lock();
      try {
        int handle = (HANDLE_TYPE_NOTIFIER << 24) | (nextIndex++ & 0xffffff);
        notifiers.put(handle, new Notifier());
        createSignalObject(handle, false, false);
        return handle;
      } finally {
        lock.unlock();
      }
    }

    public static void setName(int handle, String name) {
      lock.lock();
      try {
        Notifier notifier = notifiers.get(handle);
        if (notifier != null) {
          notifier.name = name == null ? "" : name;
        }
      } finally {
        lock.unlock();
      }
    }

    public static void destroy(int handle) {
      destroySignalObject(handle);
      lock.lock();
      try {
        Notifier notifier = notifiers.remove(handle);
        if (notifier != null) {
          notifier.alive = false;
          notifier.alarmTime = NO_ALARM;
        }
        alarms.remove(handle);
        cond.signalAll();
      } finally {
        lock.unlock();
      }
    }

    public static void setAlarm(
        int handle, long alarmTime, long intervalTime, boolean absolute, boolean ack) {
      lock.lock();
      try {
        Notifier notifier = notifiers.get(handle);
        if (notifier == null) {
          return;
        }
        if (ack) {
          notifier.handlerSignaled = false;
          resetSignalObject(handle);
        }
        if (!absolute) {
          alarmTime += monotonicTime();
        }
        long prev = alarms.isEmpty() ? NO_ALARM : alarmTimeOf(alarms.peek());
        alarms.remove(handle);
        notifier.alarmTime = alarmTime;
        notifier.intervalTime = intervalTime;
        notifier.overrunCount = 0;
        alarms.add(handle);
        alarmSetCount.incrementAndGet();
        if (alarmTime < prev) {
          cond.signalAll();
        }
      } finally {
        lock.unlock();
      }
    }

    public static void cancel(int handle, boolean ack) {
      lock.lock();
      try {
        Notifier notifier = notifiers.get(handle);
        if (notifier == null) {
          return;
        }
        if (ack) {
          notifier.handlerSignaled = false;
          resetSignalObject(handle);
        }
        alarms.remove(handle);
        notifier.alarmTime = NO_ALARM;
      } finally {
        lock.unlock();
      }
    }

    public static void acknowledge(int handle) {
      lock.lock();
      try {
        Notifier notifier = notifiers.get(handle);
        if (notifier == null) {
          return;
        }
        notifier.handlerSignaled = false;
        resetSignalObject(handle);
        cond.signalAll();
      } finally {
        lock.unlock();
      }
    }

    public static int overrun(int handle) {
      lock.lock();
      try {
        Notifier notifier = notifiers.get(handle);
        return notifier == null ? -1 : notifier.userOverrunCount;
      } finally {
        lock.unlock();
      }
    }

    public static long nextTimeout() {
      lock.lock();
      try {
        return alarms.isEmpty() ? NO_ALARM : alarmTimeOf(alarms.peek());
      } finally {
        lock.unlock();
      }
    }

    public static int count() {
      lock.lock();
      try {
        return notifiers.size();
      } finally {
        lock.unlock();
      }
    }

    public static long alarmSetCount() {
      return alarmSetCount.get();
    }

    static void pause() {
      paused = true;
    }

    static void resume() {
      lock.lock();
      try {
        paused = false;
        cond.signalAll();
      } finally {
        lock.unlock();
      }
    }

    public static void wakeup() {
      lock.lock();
      try {
        processAlarms(false);
      } finally {
        lock.unlock();
      }
    }

    public static void waitForHandlers() {
      lock.lock();
      try {
        List<Integer> signaled = new ArrayList<>();
        for (Map.Entry<Integer, Notifier> entry : notifiers.entrySet()) {
          if (entry.getValue().handlerSignaled) {
            signaled.add(entry.getKey());
          }
        }
        waitAck(signaled);
      } finally {
        lock.unlock();
      }
    }

    public static void wakeupAndWait() {
      lock.lock();
      try {
        processAlarms(true);
      } finally {
        lock.unlock();
      }
    }

    private static void processAlarms(boolean wait) {
      long cur = monotonicTime();
      List<Integer> signaled = wait ? new ArrayList<>() : null;
      while (!alarms.isEmpty() && alarmTimeOf(alarms.peek()) <= cur) {
        int handle = alarms.poll();
        Notifier notifier = notifiers.get(handle);
        if (notifier == null || !notifier.alive) {
          continue;
        }
        if (notifier.intervalTime > 0) {
          notifier.alarmTime += notifier.intervalTime;
          if (cur >= notifier.alarmTime) {
            int missed = (int) ((cur - notifier.alarmTime) / notifier.intervalTime) + 1;
            notifier.overrunCount += missed;
            notifier.alarmTime += missed * notifier.intervalTime;
          }
          alarms.add(handle);
        } else {
          notifier.alarmTime = NO_ALARM;
        }
        if (!notifier.handlerSignaled) {
          notifier.handlerSignaled = true;
          notifier.userOverrunCount = notifier.overrunCount;
          notifier.overrunCount = 0;
          setSignalObject(handle);
          if (signaled != null) {
            signaled.add(handle);
          }
        }
      }
      if (signaled != null) {
        waitAck(signaled);
      }
    }

    private static void waitAck(List<Integer> signaled) {
      while (!signaled.isEmpty()) {
        Iterator<Integer> it = signaled.iterator();
        while (it.hasNext()) {
          Notifier notifier = notifiers.get(it.next());
          if (notifier == null || !notifier.handlerSignaled) {
            it.remove();
          }
        }
        if (signaled.isEmpty()) {
          break;
        }
        try {
          cond.awaitNanos(1_000_000L);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    private static long alarmTimeOf(Integer handle) {
      Notifier notifier = notifiers.get(handle);
      return notifier == null ? NO_ALARM : notifier.alarmTime;
    }
  }

  // ---------------------------------------------------------------------------
  // Simulator timing hooks.
  // ---------------------------------------------------------------------------

  private static final AtomicBoolean programStarted = new AtomicBoolean();
  private static final AtomicLong programState = new AtomicLong();
  private static final AtomicLong programStartAlarmCount = new AtomicLong();

  public static void setProgramStarted(boolean started) {
    programStartAlarmCount.set(Notifiers.alarmSetCount());
    programStarted.set(started);
  }

  public static boolean getProgramStarted() {
    return programStarted.get();
  }

  public static void setProgramState(long word) {
    programState.set(word);
  }

  public static long getProgramState() {
    return programState.get();
  }

  public static void waitForProgramStart(boolean waitForFirstNotifier) {
    int count = 0;
    while (!programStarted.get()) {
      count++;
      sleepMillis(1);
      if (count > 30_000) {
        return;
      }
    }
    while (waitForFirstNotifier
        && (Notifiers.alarmSetCount() == programStartAlarmCount.get()
            || Notifiers.nextTimeout() == NO_ALARM)) {
      count++;
      sleepMillis(1);
      if (count > 30_000) {
        return;
      }
    }
  }

  public static void stepTimingSync(long delta) {
    Notifiers.waitForHandlers();
    while (delta > 0) {
      long cur = monotonicTime();
      long next = Notifiers.nextTimeout();
      long step = next <= cur ? 0 : Math.min(delta, next - cur);
      stepTiming(step);
      delta -= step;
      Notifiers.wakeupAndWait();
      if (step == 0 && Notifiers.nextTimeout() <= cur) {
        break;
      }
    }
  }

  public static void stepTimingAsync(long delta) {
    stepTiming(delta);
    Notifiers.wakeup();
  }

  private static void sleepMillis(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  // ---------------------------------------------------------------------------
  // Driver station and robot controller data.
  // ---------------------------------------------------------------------------

  public static final class Joystick {
    public final float[] axes = new float[MAX_AXES];
    public final short[] rawAxes = new short[MAX_AXES];
    public int axisCount;
    public final byte[] povs = new byte[MAX_POVS];
    public int povCount;
    public long buttons;
    public int buttonCount;
    public final float[] fingers = new float[MAX_TOUCHPADS * MAX_FINGERS * 2];
    public final boolean[] fingerDown = new boolean[MAX_TOUCHPADS * MAX_FINGERS];
    public final int[] fingerCounts = new int[MAX_TOUCHPADS];
    public int touchpadCount;
    public String name = "";
    public boolean gamepad;
    public int gamepadType;
    public int supportedOutputs;
    public int leds;
    public final int[] rumble = new int[4];
  }

  public static final class Ds {
    public boolean enabled;
    public int robotMode;
    public boolean eStop;
    public boolean fmsAttached;
    public boolean dsAttached;
    public int alliance = AllianceStationID.UNKNOWN.ordinal();
    public double matchTime = -1;
    public long opMode;
    public OpModeOption[] opModeOptions = new OpModeOption[0];
    public String gameData = "";
    public String eventName = "";
    public int matchNumber;
    public int replayNumber;
    public int matchType;
    public final Joystick[] joysticks = new Joystick[NUM_JOYSTICKS];
    public boolean sendError = true;
    public boolean sendConsole = true;
    public long cachedWord;
    public int cachedAlliance;
    public double cachedMatchTime = -1;
    public String cachedGameData = "";
    public final Joystick[] cachedJoysticks = new Joystick[NUM_JOYSTICKS];
    public final List<Integer> newDataEvents = new ArrayList<>();

    Ds() {
      for (int i = 0; i < NUM_JOYSTICKS; i++) {
        joysticks[i] = new Joystick();
        cachedJoysticks[i] = new Joystick();
        Arrays.fill(povsOf(joysticks[i]), (byte) -1);
        Arrays.fill(povsOf(cachedJoysticks[i]), (byte) -1);
      }
    }

    private static byte[] povsOf(Joystick joystick) {
      return joystick.povs;
    }
  }

  public static final Ds DS = new Ds();
  private static final Object dsLock = new Object();

  public static long makeControlWord(
      long opMode, int robotMode, boolean enabled, boolean eStop, boolean fms, boolean ds) {
    return (opMode & OPMODE_HASH_MASK)
        | (((long) robotMode & 0x3) << ROBOT_MODE_SHIFT)
        | (enabled ? ENABLED_MASK : 0)
        | (eStop ? ESTOP_MASK : 0)
        | (fms ? FMS_ATTACHED_MASK : 0)
        | (ds ? DS_ATTACHED_MASK : 0);
  }

  public static long liveControlWord() {
    synchronized (dsLock) {
      if (!DS.dsAttached) {
        return 0;
      }
      return makeControlWord(DS.opMode, DS.robotMode, DS.enabled, DS.eStop, DS.fmsAttached, true);
    }
  }

  public static long cachedControlWord() {
    synchronized (dsLock) {
      return shutdown ? 0 : DS.cachedWord;
    }
  }

  public static boolean refreshDs() {
    synchronized (dsLock) {
      if (shutdown) {
        return false;
      }
      copyJoysticks(DS.joysticks, DS.cachedJoysticks);
      DS.cachedAlliance = DS.alliance;
      DS.cachedMatchTime = DS.matchTime;
      DS.cachedGameData = DS.gameData;
      DS.cachedWord = DS.dsAttached ? liveControlWord() : 0;
      return true;
    }
  }

  private static void copyJoysticks(Joystick[] from, Joystick[] to) {
    for (int i = 0; i < from.length; i++) {
      Joystick src = from[i];
      Joystick dst = to[i];
      System.arraycopy(src.axes, 0, dst.axes, 0, MAX_AXES);
      System.arraycopy(src.rawAxes, 0, dst.rawAxes, 0, MAX_AXES);
      System.arraycopy(src.povs, 0, dst.povs, 0, MAX_POVS);
      System.arraycopy(src.fingers, 0, dst.fingers, 0, src.fingers.length);
      System.arraycopy(src.fingerDown, 0, dst.fingerDown, 0, src.fingerDown.length);
      System.arraycopy(src.fingerCounts, 0, dst.fingerCounts, 0, src.fingerCounts.length);
      dst.axisCount = src.axisCount;
      dst.povCount = src.povCount;
      dst.buttons = src.buttons;
      dst.buttonCount = src.buttonCount;
      dst.touchpadCount = src.touchpadCount;
      dst.name = src.name;
      dst.gamepad = src.gamepad;
      dst.gamepadType = src.gamepadType;
      dst.supportedOutputs = src.supportedOutputs;
    }
  }

  public static void notifyNewData() {
    int[] handles;
    synchronized (dsLock) {
      DS.dsAttached = true;
      refreshDs();
      handles = DS.newDataEvents.stream().mapToInt(Integer::intValue).toArray();
    }
    for (int handle : handles) {
      setEvent(handle);
    }
    Callbacks.fire("newData", HALValue.makeBoolean(true));
  }

  public static final class Rio {
    public double vinVoltage = 12.0;
    public double userVoltage3V3 = 3.3;
    public double userCurrent3V3;
    public boolean userActive3V3 = true;
    public int userFaults3V3;
    public double brownoutVoltage = 6.3;
    public double brownoutRecoveryVoltage = 7.0;
    public double cpuTemp = 45.0;
    public int teamNumber;
    public String serialNumber = "sim-0000";
    public String comments = "";
  }

  public static final Rio RIO = new Rio();

  public static void setBrownout(double brownout, double recovery) {
    if (!Double.isFinite(brownout)
        || !Double.isFinite(recovery)
        || brownout < 5.0
        || brownout > 8.0
        || recovery < 5.0
        || recovery > 8.5
        || Math.round(recovery * 1000) < Math.round(brownout * 1000) + 500) {
      throw new IllegalArgumentException(errorMessage(HAL_PARAMETER_OUT_OF_RANGE));
    }
    RIO.brownoutVoltage = Math.round(brownout * 1000) / 1000.0;
    RIO.brownoutRecoveryVoltage = Math.round(recovery * 1000) / 1000.0;
    Callbacks.fire("brownoutVoltage", HALValue.makeDouble(RIO.brownoutVoltage));
    Callbacks.fire("brownoutRecoveryVoltage", HALValue.makeDouble(RIO.brownoutRecoveryVoltage));
  }

  // ---------------------------------------------------------------------------
  // Callbacks used by the simulation data JNI classes.
  // ---------------------------------------------------------------------------

  public static final class Callbacks {
    private static final Object lock = new Object();
    private static int nextUid = 1;
    private static final Map<Integer, Registration> registrations = new HashMap<>();

    private Callbacks() {}

    static final class Registration {
      final String key;
      final NotifyCallback notify;
      final SimDeviceCallback device;
      final SimValueCallback value;
      final BiConsumer<String, OpModeOption[]> options;
      final BufferCallback buffer;
      final ConstBufferCallback constBuffer;

      Registration(
          String key,
          NotifyCallback notify,
          SimDeviceCallback device,
          SimValueCallback value,
          BiConsumer<String, OpModeOption[]> options,
          BufferCallback buffer,
          ConstBufferCallback constBuffer) {
        this.key = key;
        this.notify = notify;
        this.device = device;
        this.value = value;
        this.options = options;
        this.buffer = buffer;
        this.constBuffer = constBuffer;
      }
    }

    public static int register(
        String key, NotifyCallback callback, boolean initial, HALValue value) {
      int uid;
      synchronized (lock) {
        uid = nextUid++;
        registrations.put(uid, new Registration(key, callback, null, null, null, null, null));
      }
      if (initial && callback != null) {
        callback.callback(key, value);
      }
      return uid;
    }

    public static int registerDevice(String key, SimDeviceCallback callback) {
      synchronized (lock) {
        int uid = nextUid++;
        registrations.put(uid, new Registration(key, null, callback, null, null, null, null));
        return uid;
      }
    }

    public static int registerValue(String key, SimValueCallback callback) {
      synchronized (lock) {
        int uid = nextUid++;
        registrations.put(uid, new Registration(key, null, null, callback, null, null, null));
        return uid;
      }
    }

    public static int registerOptions(String key, BiConsumer<String, OpModeOption[]> callback) {
      synchronized (lock) {
        int uid = nextUid++;
        registrations.put(uid, new Registration(key, null, null, null, callback, null, null));
        return uid;
      }
    }

    public static int registerBuffer(String key, BufferCallback callback) {
      synchronized (lock) {
        int uid = nextUid++;
        registrations.put(uid, new Registration(key, null, null, null, null, callback, null));
        return uid;
      }
    }

    public static int registerConstBuffer(String key, ConstBufferCallback callback) {
      synchronized (lock) {
        int uid = nextUid++;
        registrations.put(uid, new Registration(key, null, null, null, null, null, callback));
        return uid;
      }
    }

    public static void cancel(int uid) {
      synchronized (lock) {
        registrations.remove(uid);
      }
    }

    public static void fire(String key, HALValue value) {
      List<Registration> copy;
      synchronized (lock) {
        copy = new ArrayList<>(registrations.values());
      }
      for (Registration registration : copy) {
        if (registration.notify != null
            && (registration.key.equals(key) || registration.key.equals("*"))) {
          registration.notify.callback(key, value);
        }
      }
    }

    public static void fireDevice(String key, String name, int handle) {
      List<Registration> copy;
      synchronized (lock) {
        copy = new ArrayList<>(registrations.values());
      }
      for (Registration registration : copy) {
        if (registration.device != null && name.startsWith(registration.key)) {
          registration.device.callback(name, handle);
        }
      }
    }

    public static void fireValue(
        String key, String name, int handle, int direction, HALValue value) {
      List<Registration> copy;
      synchronized (lock) {
        copy = new ArrayList<>(registrations.values());
      }
      for (Registration registration : copy) {
        if (registration.value != null && registration.key.equals(key)) {
          registration.value.callback(name, handle, direction, value);
        }
      }
    }

    public static void fireOptions(OpModeOption[] options) {
      List<Registration> copy;
      synchronized (lock) {
        copy = new ArrayList<>(registrations.values());
      }
      for (Registration registration : copy) {
        if (registration.options != null) {
          registration.options.accept("opModeOptions", options);
        }
      }
    }

    public static void fireBuffer(String key, byte[] data, boolean write) {
      List<Registration> copy;
      synchronized (lock) {
        copy = new ArrayList<>(registrations.values());
      }
      for (Registration registration : copy) {
        if (!registration.key.equals(key)) {
          continue;
        }
        if (write && registration.constBuffer != null) {
          registration.constBuffer.callback(key, data, data.length);
        }
        if (!write && registration.buffer != null) {
          registration.buffer.callback(key, data, data.length);
        }
      }
    }

    public static void reset() {
      synchronized (lock) {
        registrations.clear();
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Indexed hardware. Handles are (type << 24) | index, matching HAL handles.
  // ---------------------------------------------------------------------------

  public static final int TYPE_DIO = 1;
  public static final int TYPE_PWM = 2;
  public static final int TYPE_ANALOG = 3;
  public static final int TYPE_ENCODER = 4;
  public static final int TYPE_COUNTER = 5;
  public static final int TYPE_DUTY = 6;
  public static final int TYPE_LED = 7;
  public static final int TYPE_SERIAL = 8;
  public static final int TYPE_PCM = 9;
  public static final int TYPE_PH = 10;
  public static final int TYPE_PD = 11;
  public static final int TYPE_CAN = 12;
  public static final int TYPE_DPWM = 13;
  public static final int TYPE_SIM_DEVICE = 14;
  public static final int TYPE_SIM_VALUE = 15;

  public static final class Dio {
    public boolean initialized;
    public boolean input = true;
    public boolean value = true;
    public int simDevice;
    public double pulseLength;
    public int filterIndex = -1;
  }

  public static final class Pwm {
    public boolean initialized;
    public int pulse;
    public int period = 5050;
    public int simDevice;
  }

  public static final class Analog {
    public boolean initialized;
    public double voltage;
    public int simDevice;
  }

  public static final class Encoder {
    public boolean initialized;
    public int count;
    public double rate;
    public boolean direction;
    public boolean reverse;
    public boolean reset;
    public double distancePerPulse = 1;
    public int encoding = 4;
    public int a = -1;
    public int b = -1;
    public int simDevice;
  }

  public static final class Counter {
    public boolean initialized;
    public int count;
    public double rate;
    public boolean rising = true;
    public boolean stopped = true;
    public int channel = -1;
  }

  public static final class Duty {
    public boolean initialized;
    public double frequency;
    public double output;
  }

  public static final class Led {
    public boolean initialized;
    public int start;
    public int length;
    public int channel;
  }

  public static final class Serial {
    public boolean open;
    public String name = "";
    public int baud = 9600;
    public final List<Byte> incoming = new ArrayList<>();
  }

  public static final class Pcm {
    public boolean initialized;
    public boolean compressorOn;
    public boolean closedLoop = true;
    public boolean pressureSwitch;
    public double current;
    public final boolean[] solenoids = new boolean[NUM_CTRE_SOLENOID_CHANNELS];
    public final int[] oneShot = new int[NUM_CTRE_SOLENOID_CHANNELS];
  }

  public static final class Ph {
    public boolean initialized;
    public boolean compressorOn;
    public int configType = 1;
    public boolean pressureSwitch;
    public double current;
    public final boolean[] solenoids = new boolean[NUM_REVPH_CHANNELS];
  }

  public static final class Pd {
    public boolean initialized;
    public int type = 1;
    public int module;
    public double temperature = 25;
    public double voltage = 12;
    public final double[] current = new double[NUM_REVPDH_CHANNELS];
    public boolean switchable;
  }

  public static final class CanDevice {
    public int bus;
    public int manufacturer;
    public int deviceId;
    public int deviceType;
    public final Map<Integer, byte[]> repeating = new HashMap<>();
    public final Map<Integer, CANReceiveMessage> latest = new HashMap<>();
  }

  public static final class CanFrame {
    public int bus;
    public int id;
    public byte[] data = new byte[0];
    public int flags;
    public long timestamp;
  }

  public static final class DigitalPwm {
    public boolean initialized;
    public double duty;
    public int pin = -1;
  }

  public static final Dio[] DIOS = new Dio[NUM_DIGITAL_CHANNELS];
  public static final Pwm[] PWMS = new Pwm[NUM_PWM_CHANNELS];
  public static final Analog[] ANALOGS = new Analog[NUM_ANALOG_INPUTS];
  public static final Encoder[] ENCODERS = new Encoder[NUM_ENCODERS];
  public static final Counter[] COUNTERS = new Counter[NUM_COUNTERS];
  public static final Duty[] DUTIES = new Duty[NUM_DUTY_CYCLES];
  public static final Led[] LEDS = new Led[NUM_ADDRESSABLE_LEDS];
  public static final byte[] LED_DATA = new byte[NUM_ADDRESSABLE_LEDS * 4096];
  public static final Pcm[] PCMS = new Pcm[NUM_CTREPCM_MODULES];
  public static final Ph[] PHS = new Ph[NUM_REVPH_MODULES + 1];
  public static final Pd[] PDS = new Pd[NUM_REVPDH_MODULES + 1];
  public static final DigitalPwm[] DPWMS = new DigitalPwm[NUM_DIGITAL_PWM_OUTPUTS];
  public static final boolean[] DIO_ALLOC = new boolean[NUM_DIGITAL_CHANNELS + NUM_PWM_HEADERS];
  public static final Map<Integer, CanDevice> CANS = new HashMap<>();
  public static final List<CanFrame> CAN_RX = new ArrayList<>();
  public static final Map<Integer, List<CanFrame>> CAN_STREAMS = new HashMap<>();
  public static final double[] IMU_ANGLE = new double[3];
  public static final double[] IMU_RATE = new double[3];
  public static final double[] IMU_ACCEL = new double[3];
  public static double imuYaw;

  static {
    for (int i = 0; i < DIOS.length; i++) {
      DIOS[i] = new Dio();
    }
    for (int i = 0; i < PWMS.length; i++) {
      PWMS[i] = new Pwm();
    }
    for (int i = 0; i < ANALOGS.length; i++) {
      ANALOGS[i] = new Analog();
    }
    for (int i = 0; i < ENCODERS.length; i++) {
      ENCODERS[i] = new Encoder();
    }
    for (int i = 0; i < COUNTERS.length; i++) {
      COUNTERS[i] = new Counter();
    }
    for (int i = 0; i < DUTIES.length; i++) {
      DUTIES[i] = new Duty();
    }
    for (int i = 0; i < LEDS.length; i++) {
      LEDS[i] = new Led();
    }
    for (int i = 0; i < PCMS.length; i++) {
      PCMS[i] = new Pcm();
    }
    for (int i = 0; i < PHS.length; i++) {
      PHS[i] = new Ph();
    }
    for (int i = 0; i < PDS.length; i++) {
      PDS[i] = new Pd();
    }
    for (int i = 0; i < DPWMS.length; i++) {
      DPWMS[i] = new DigitalPwm();
    }
  }

  private static final Object hwLock = new Object();
  private static int nextCan = 1;
  private static int nextSerial = 1;
  private static int nextStream = 1;
  private static final Map<Integer, Serial> serials = new HashMap<>();
  private static final Map<Integer, Integer> dioHandleChannel = new HashMap<>();
  private static final Map<Integer, Integer> pwmHandleChannel = new HashMap<>();
  private static final Map<Integer, Integer> analogHandleChannel = new HashMap<>();
  private static final Map<Integer, Integer> encoderHandleIndex = new HashMap<>();
  private static final Map<Integer, Integer> counterHandleIndex = new HashMap<>();
  private static final Map<Integer, Integer> dutyHandleChannel = new HashMap<>();
  private static final Map<Integer, Integer> ledHandleChannel = new HashMap<>();
  private static final Map<Integer, Integer> pcmHandleModule = new HashMap<>();
  private static final Map<Integer, Integer> phHandleModule = new HashMap<>();
  private static final Map<Integer, Integer> pdHandleModule = new HashMap<>();
  private static final Map<Integer, Integer> dpwmHandleIndex = new HashMap<>();

  public static int handle(int type, int index) {
    return (type << 24) | (index & 0xffffff);
  }

  public static int initializeDio(int channel, boolean input) {
    if (channel < 0 || channel >= NUM_DIGITAL_CHANNELS) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      if (DIO_ALLOC[channel]) {
        setErrno(HAL_RESOURCE_IS_ALLOCATED);
        return INVALID_HANDLE;
      }
      DIO_ALLOC[channel] = true;
      Dio dio = DIOS[channel];
      dio.initialized = true;
      dio.input = input;
      dio.value = true;
      dio.simDevice = 0;
      int allocated = handle(TYPE_DIO, channel + 1);
      dioHandleChannel.put(allocated, channel);
      Callbacks.fire("dio.initialized." + channel, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Dio dio(int handle) {
    Integer channel = dioHandleChannel.get(handle);
    return channel == null ? null : DIOS[channel];
  }

  public static int dioChannel(int handle) {
    Integer channel = dioHandleChannel.get(handle);
    return channel == null ? -1 : channel;
  }

  public static void freeDio(int handle) {
    synchronized (hwLock) {
      Integer channel = dioHandleChannel.remove(handle);
      if (channel == null) {
        return;
      }
      DIO_ALLOC[channel] = false;
      DIOS[channel].initialized = false;
      Callbacks.fire("dio.initialized." + channel, HALValue.makeBoolean(false));
    }
  }

  public static int initializePwm(int channel) {
    if (channel < 0 || channel >= NUM_PWM_CHANNELS) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    int slot = channel < NUM_PWM_HEADERS ? NUM_DIGITAL_CHANNELS + channel : channel;
    synchronized (hwLock) {
      if (slot < DIO_ALLOC.length && DIO_ALLOC[slot]) {
        setErrno(HAL_RESOURCE_IS_ALLOCATED);
        return INVALID_HANDLE;
      }
      if (slot < DIO_ALLOC.length) {
        DIO_ALLOC[slot] = true;
      }
      PWMS[channel].initialized = true;
      PWMS[channel].pulse = 0;
      int allocated = handle(TYPE_PWM, channel + 1);
      pwmHandleChannel.put(allocated, channel);
      Callbacks.fire("pwm.initialized." + channel, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Pwm pwm(int handle) {
    Integer channel = pwmHandleChannel.get(handle);
    return channel == null ? null : PWMS[channel];
  }

  public static int pwmChannel(int handle) {
    Integer channel = pwmHandleChannel.get(handle);
    return channel == null ? -1 : channel;
  }

  public static void freePwm(int handle) {
    synchronized (hwLock) {
      Integer channel = pwmHandleChannel.remove(handle);
      if (channel == null) {
        return;
      }
      int slot = channel < NUM_PWM_HEADERS ? NUM_DIGITAL_CHANNELS + channel : channel;
      if (slot < DIO_ALLOC.length) {
        DIO_ALLOC[slot] = false;
      }
      PWMS[channel].initialized = false;
      Callbacks.fire("pwm.initialized." + channel, HALValue.makeBoolean(false));
    }
  }

  public static int initializeAnalog(int channel) {
    if (channel < 0 || channel >= NUM_ANALOG_INPUTS) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      if (analogHandleChannel.containsValue(channel)) {
        setErrno(HAL_RESOURCE_IS_ALLOCATED);
        return INVALID_HANDLE;
      }
      ANALOGS[channel].initialized = true;
      int allocated = handle(TYPE_ANALOG, channel + 1);
      analogHandleChannel.put(allocated, channel);
      Callbacks.fire("analog.initialized." + channel, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Analog analog(int handle) {
    Integer channel = analogHandleChannel.get(handle);
    return channel == null ? null : ANALOGS[channel];
  }

  public static int analogChannel(int handle) {
    Integer channel = analogHandleChannel.get(handle);
    return channel == null ? -1 : channel;
  }

  public static void freeAnalog(int handle) {
    synchronized (hwLock) {
      Integer channel = analogHandleChannel.remove(handle);
      if (channel != null) {
        ANALOGS[channel].initialized = false;
        Callbacks.fire("analog.initialized." + channel, HALValue.makeBoolean(false));
      }
    }
  }

  public static int initializeEncoder(int a, int b, boolean reverse, int encoding) {
    synchronized (hwLock) {
      for (int i = 0; i < ENCODERS.length; i++) {
        if (!ENCODERS[i].initialized) {
          Encoder encoder = ENCODERS[i];
          encoder.initialized = true;
          encoder.a = a;
          encoder.b = b;
          encoder.reverse = reverse;
          encoder.encoding = encoding <= 0 ? 4 : encoding;
          encoder.count = 0;
          encoder.distancePerPulse = 1;
          int allocated = handle(TYPE_ENCODER, i + 1);
          encoderHandleIndex.put(allocated, i);
          Callbacks.fire("encoder.initialized." + i, HALValue.makeBoolean(true));
          return allocated;
        }
      }
    }
    setErrno(HAL_NO_AVAILABLE_RESOURCES);
    return INVALID_HANDLE;
  }

  public static Encoder encoder(int handle) {
    Integer index = encoderHandleIndex.get(handle);
    return index == null ? null : ENCODERS[index];
  }

  public static int encoderIndex(int handle) {
    Integer index = encoderHandleIndex.get(handle);
    return index == null ? -1 : index;
  }

  public static void freeEncoder(int handle) {
    synchronized (hwLock) {
      Integer index = encoderHandleIndex.remove(handle);
      if (index != null) {
        ENCODERS[index].initialized = false;
        Callbacks.fire("encoder.initialized." + index, HALValue.makeBoolean(false));
      }
    }
  }

  public static int findEncoderForChannel(int channel) {
    for (int i = 0; i < ENCODERS.length; i++) {
      if (ENCODERS[i].initialized && (ENCODERS[i].a == channel || ENCODERS[i].b == channel)) {
        return i;
      }
    }
    return -1;
  }

  public static int initializeCounter(int channel, boolean rising) {
    synchronized (hwLock) {
      for (int i = 0; i < COUNTERS.length; i++) {
        if (!COUNTERS[i].initialized) {
          COUNTERS[i].initialized = true;
          COUNTERS[i].channel = channel;
          COUNTERS[i].rising = rising;
          COUNTERS[i].count = 0;
          int allocated = handle(TYPE_COUNTER, i + 1);
          counterHandleIndex.put(allocated, i);
          return allocated;
        }
      }
    }
    setErrno(HAL_NO_AVAILABLE_RESOURCES);
    return INVALID_HANDLE;
  }

  public static Counter counter(int handle) {
    Integer index = counterHandleIndex.get(handle);
    return index == null ? null : COUNTERS[index];
  }

  public static void freeCounter(int handle) {
    synchronized (hwLock) {
      Integer index = counterHandleIndex.remove(handle);
      if (index != null) {
        COUNTERS[index].initialized = false;
      }
    }
  }

  public static int initializeDuty(int channel) {
    if (channel < 0 || channel >= NUM_DUTY_CYCLES) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      DUTIES[channel].initialized = true;
      int allocated = handle(TYPE_DUTY, channel + 1);
      dutyHandleChannel.put(allocated, channel);
      Callbacks.fire("duty.initialized." + channel, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Duty duty(int handle) {
    Integer channel = dutyHandleChannel.get(handle);
    return channel == null ? null : DUTIES[channel];
  }

  public static int dutyChannel(int handle) {
    Integer channel = dutyHandleChannel.get(handle);
    return channel == null ? -1 : channel;
  }

  public static void freeDuty(int handle) {
    synchronized (hwLock) {
      Integer channel = dutyHandleChannel.remove(handle);
      if (channel != null) {
        DUTIES[channel].initialized = false;
      }
    }
  }

  public static int initializeLed(int channel) {
    if (channel < 0 || channel >= NUM_ADDRESSABLE_LEDS) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      LEDS[channel].initialized = true;
      LEDS[channel].channel = channel;
      int allocated = handle(TYPE_LED, channel + 1);
      ledHandleChannel.put(allocated, channel);
      Callbacks.fire("led.initialized." + channel, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Led led(int handle) {
    Integer channel = ledHandleChannel.get(handle);
    return channel == null ? null : LEDS[channel];
  }

  public static void freeLed(int handle) {
    synchronized (hwLock) {
      Integer channel = ledHandleChannel.remove(handle);
      if (channel != null) {
        LEDS[channel].initialized = false;
      }
    }
  }

  public static void setLedData(int outStart, int colorOrder, byte[] data, int start, int len) {
    if (data == null || start < 0 || len < 0 || start + len > data.length) {
      return;
    }
    int count = len / 3;
    for (int i = 0; i < count; i++) {
      int dest = (outStart + i) * 3;
      if (dest < 0 || dest + 2 >= LED_DATA.length) {
        break;
      }
      int src = start + i * 3;
      byte r = data[src];
      byte g = data[src + 1];
      byte b = data[src + 2];
      switch (colorOrder) {
        case COLOR_ORDER_RBG -> {
          LED_DATA[dest] = r;
          LED_DATA[dest + 1] = b;
          LED_DATA[dest + 2] = g;
        }
        case COLOR_ORDER_BGR -> {
          LED_DATA[dest] = b;
          LED_DATA[dest + 1] = g;
          LED_DATA[dest + 2] = r;
        }
        case COLOR_ORDER_BRG -> {
          LED_DATA[dest] = b;
          LED_DATA[dest + 1] = r;
          LED_DATA[dest + 2] = g;
        }
        case COLOR_ORDER_GBR -> {
          LED_DATA[dest] = g;
          LED_DATA[dest + 1] = b;
          LED_DATA[dest + 2] = r;
        }
        case COLOR_ORDER_GRB -> {
          LED_DATA[dest] = g;
          LED_DATA[dest + 1] = r;
          LED_DATA[dest + 2] = b;
        }
        default -> {
          LED_DATA[dest] = r;
          LED_DATA[dest + 1] = g;
          LED_DATA[dest + 2] = b;
        }
      }
    }
    Callbacks.fire("led.data", HALValue.makeInt(outStart));
  }

  public static byte[] getLedData(int start, int length) {
    byte[] out = new byte[Math.max(0, length) * 3];
    int from = start * 3;
    if (from < 0) {
      return out;
    }
    int n = Math.min(out.length, LED_DATA.length - from);
    if (n > 0) {
      System.arraycopy(LED_DATA, from, out, 0, n);
    }
    return out;
  }

  public static int initializeSerial(int port, String name) {
    synchronized (hwLock) {
      int allocated = handle(TYPE_SERIAL, nextSerial++);
      Serial serial = new Serial();
      serial.open = true;
      serial.name = name == null ? "serial-" + port : name;
      serials.put(allocated, serial);
      return allocated;
    }
  }

  public static Serial serial(int handle) {
    return serials.get(handle);
  }

  public static void closeSerial(int handle) {
    Serial serial = serials.remove(handle);
    if (serial != null) {
      serial.open = false;
    }
  }

  public static int initializePcm(int module) {
    if (module < 0 || module >= NUM_CTREPCM_MODULES) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      PCMS[module].initialized = true;
      PCMS[module].closedLoop = true;
      int allocated = handle(TYPE_PCM, module + 1);
      pcmHandleModule.put(allocated, module);
      Callbacks.fire("pcm.initialized." + module, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Pcm pcm(int handle) {
    Integer module = pcmHandleModule.get(handle);
    return module == null ? null : PCMS[module];
  }

  public static int pcmModule(int handle) {
    Integer module = pcmHandleModule.get(handle);
    return module == null ? -1 : module;
  }

  public static void freePcm(int handle) {
    synchronized (hwLock) {
      Integer module = pcmHandleModule.remove(handle);
      if (module != null) {
        PCMS[module].initialized = false;
      }
    }
  }

  public static int initializePh(int module) {
    if (module < 1 || module > NUM_REVPH_MODULES) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      PHS[module].initialized = true;
      PHS[module].configType = 1;
      int allocated = handle(TYPE_PH, module);
      phHandleModule.put(allocated, module);
      Callbacks.fire("ph.initialized." + module, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Ph ph(int handle) {
    Integer module = phHandleModule.get(handle);
    return module == null ? null : PHS[module];
  }

  public static int phModule(int handle) {
    Integer module = phHandleModule.get(handle);
    return module == null ? -1 : module;
  }

  public static void freePh(int handle) {
    synchronized (hwLock) {
      Integer module = phHandleModule.remove(handle);
      if (module != null) {
        PHS[module].initialized = false;
      }
    }
  }

  public static int initializePd(int module, int type) {
    int resolvedType = type == 0 ? 1 : type;
    int resolved = module < 0 ? 0 : module;
    if (resolvedType == 1) {
      if (resolved < 0 || resolved >= NUM_CTREPDP_MODULES) {
        setErrno(HAL_RESOURCE_OUT_OF_RANGE);
        return INVALID_HANDLE;
      }
    } else if (resolved < 1 || resolved > NUM_REVPDH_MODULES) {
      setErrno(HAL_RESOURCE_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      PDS[resolved].initialized = true;
      PDS[resolved].type = resolvedType;
      PDS[resolved].module = resolved;
      int allocated = handle(TYPE_PD, resolved + 1);
      pdHandleModule.put(allocated, resolved);
      Callbacks.fire("pd.initialized." + resolved, HALValue.makeBoolean(true));
      return allocated;
    }
  }

  public static Pd pd(int handle) {
    Integer module = pdHandleModule.get(handle);
    return module == null ? null : PDS[module];
  }

  public static void freePd(int handle) {
    synchronized (hwLock) {
      Integer module = pdHandleModule.remove(handle);
      if (module != null) {
        PDS[module].initialized = false;
      }
    }
  }

  public static PowerDistributionVersion pdVersion() {
    return new PowerDistributionVersion(0, 0, 0, 0, 0, 0);
  }

  public static REVPHVersion phVersion() {
    return new REVPHVersion(0, 0, 0, 0, 0, 0);
  }

  public static int allocateDigitalPwm() {
    synchronized (hwLock) {
      for (int i = 0; i < DPWMS.length; i++) {
        if (!DPWMS[i].initialized) {
          DPWMS[i].initialized = true;
          int allocated = handle(TYPE_DPWM, i + 1);
          dpwmHandleIndex.put(allocated, i);
          return allocated;
        }
      }
    }
    setErrno(HAL_NO_AVAILABLE_RESOURCES);
    return INVALID_HANDLE;
  }

  public static DigitalPwm digitalPwm(int handle) {
    Integer index = dpwmHandleIndex.get(handle);
    return index == null ? null : DPWMS[index];
  }

  public static int digitalPwmIndex(int handle) {
    Integer index = dpwmHandleIndex.get(handle);
    return index == null ? -1 : index;
  }

  public static void freeDigitalPwm(int handle) {
    synchronized (hwLock) {
      Integer index = dpwmHandleIndex.remove(handle);
      if (index != null) {
        DPWMS[index].initialized = false;
      }
    }
  }

  public static int findDigitalPwmForChannel(int channel) {
    for (int i = 0; i < DPWMS.length; i++) {
      if (DPWMS[i].initialized && DPWMS[i].pin == channel) {
        return i;
      }
    }
    return -1;
  }

  public static int initializeCan(int bus, int manufacturer, int deviceId, int deviceType) {
    if (bus < 0 || bus > NUM_CAN_BUSES) {
      setErrno(HAL_PARAMETER_OUT_OF_RANGE);
      return INVALID_HANDLE;
    }
    synchronized (hwLock) {
      CanDevice device = new CanDevice();
      device.bus = bus;
      device.manufacturer = manufacturer;
      device.deviceId = deviceId;
      device.deviceType = deviceType;
      int allocated = handle(TYPE_CAN, nextCan++);
      CANS.put(allocated, device);
      return allocated;
    }
  }

  public static CanDevice can(int handle) {
    return CANS.get(handle);
  }

  public static void freeCan(int handle) {
    CANS.remove(handle);
  }

  public static int canId(CanDevice device, int apiId) {
    return (device.deviceType & 0x1f) << 24
        | (device.manufacturer & 0xff) << 16
        | (apiId & 0x3ff) << 6
        | (device.deviceId & 0x3f);
  }

  public static int sendCan(int bus, int messageId, byte[] data, int length, int flags) {
    synchronized (hwLock) {
      CanFrame frame = new CanFrame();
      frame.bus = bus;
      frame.id = messageId;
      frame.flags = flags;
      frame.timestamp = now();
      int n = Math.max(0, Math.min(length, data == null ? 0 : data.length));
      frame.data = new byte[n];
      if (n > 0) {
        System.arraycopy(data, 0, frame.data, 0, n);
      }
      CAN_RX.add(frame);
      if (CAN_RX.size() > 1024) {
        CAN_RX.remove(0);
      }
      for (List<CanFrame> stream : CAN_STREAMS.values()) {
        stream.add(frame);
      }
      return HAL_SUCCESS;
    }
  }

  public static boolean receiveCan(int bus, int messageId, CANReceiveMessage message) {
    synchronized (hwLock) {
      for (int i = CAN_RX.size() - 1; i >= 0; i--) {
        CanFrame frame = CAN_RX.get(i);
        if (frame.bus == bus && frame.id == messageId) {
          fill(message, frame);
          return true;
        }
      }
    }
    return false;
  }

  private static void fill(CANReceiveMessage message, CanFrame frame) {
    message.length = Math.min(frame.data.length, message.data.length);
    System.arraycopy(frame.data, 0, message.data, 0, message.length);
    message.flags = frame.flags;
    message.timestamp = frame.timestamp;
  }

  public static int openCanStream(int bus, int messageId, int mask, int max) {
    synchronized (hwLock) {
      int allocated = nextStream++;
      CAN_STREAMS.put(allocated, new ArrayList<>());
      return allocated;
    }
  }

  public static void closeCanStream(int handle) {
    CAN_STREAMS.remove(handle);
  }

  public static int readCanStream(int handle, CANStreamMessage[] messages, int toRead) {
    List<CanFrame> stream = CAN_STREAMS.get(handle);
    if (stream == null) {
      return 0;
    }
    int n = 0;
    synchronized (hwLock) {
      Iterator<CanFrame> it = stream.iterator();
      while (it.hasNext() && n < toRead && n < messages.length) {
        CanFrame frame = it.next();
        CANStreamMessage message = messages[n];
        message.length = Math.min(frame.data.length, message.data.length);
        System.arraycopy(frame.data, 0, message.data, 0, message.length);
        message.flags = frame.flags;
        message.timestamp = frame.timestamp;
        message.messageId = frame.id;
        it.remove();
        n++;
      }
      if (stream.size() > 256) {
        stream.subList(0, stream.size() - 256).clear();
      }
    }
    return n;
  }

  public static void canStatus(int bus, CANStatus status) {
    status.setStatus(0, 0, 0, 0, 0);
  }

  public static void getAllJoystick(
      int stick, float[] axes, short[] raw, byte[] povs, float[] fingers, long[] meta) {
    if (stick < 0 || stick >= NUM_JOYSTICKS) {
      return;
    }
    Joystick joystick;
    synchronized (dsLock) {
      joystick = DS.cachedJoysticks[stick];
      if (axes != null) {
        System.arraycopy(joystick.axes, 0, axes, 0, Math.min(axes.length, MAX_AXES));
      }
      if (raw != null) {
        System.arraycopy(joystick.rawAxes, 0, raw, 0, Math.min(raw.length, MAX_AXES));
      }
      if (povs != null) {
        System.arraycopy(joystick.povs, 0, povs, 0, Math.min(povs.length, MAX_POVS));
      }
      if (fingers != null) {
        System.arraycopy(
            joystick.fingers, 0, fingers, 0, Math.min(fingers.length, joystick.fingers.length));
      }
      if (meta != null && meta.length >= 7) {
        meta[0] = joystick.axisCount;
        meta[1] = joystick.povCount;
        meta[2] = joystick.buttonCount;
        meta[3] = joystick.buttons;
        meta[4] = joystick.touchpadCount;
        meta[5] = touchpadMeta(joystick, 0);
        meta[6] = touchpadMeta(joystick, 1);
      }
    }
  }

  private static long touchpadMeta(Joystick joystick, int pad) {
    int base = pad * MAX_FINGERS;
    long value = 0;
    if (joystick.fingerDown[base]) {
      value |= 0x1;
    }
    if (joystick.fingerDown[base + 1]) {
      value |= 0x2;
    }
    value |= ((long) joystick.fingerCounts[pad] & 0x3) << 2;
    return value;
  }

  public static void matchInfo(MatchInfoData info) {
    synchronized (dsLock) {
      info.setData(DS.eventName, DS.matchNumber, DS.replayNumber, DS.matchType);
    }
  }

  public static void observeStarting() {
    setProgramStarted(true);
  }

  public static void observe(long word) {
    setProgramState(word);
  }

  public static boolean outputsActive() {
    long word = cachedControlWord();
    return (word & ENABLED_MASK) != 0 && (word & DS_ATTACHED_MASK) != 0;
  }

  public static void writeAnsi(String data) {
    if (data != null && !data.isEmpty()) {
      System.out.print(data);
    }
  }

  public static int sendError(
      boolean isError, int code, String details, String location, String stack, boolean print) {
    if (print) {
      System.err.println((isError ? "Error" : "Warning") + " " + code + ": " + details);
      if (location != null && !location.isEmpty()) {
        System.err.println("  at " + location);
      }
    }
    return 0;
  }

  public static void resetHardware() {
    synchronized (hwLock) {
      Arrays.fill(DIO_ALLOC, false);
      dioHandleChannel.clear();
      pwmHandleChannel.clear();
      analogHandleChannel.clear();
      encoderHandleIndex.clear();
      counterHandleIndex.clear();
      dutyHandleChannel.clear();
      ledHandleChannel.clear();
      pcmHandleModule.clear();
      phHandleModule.clear();
      pdHandleModule.clear();
      dpwmHandleIndex.clear();
      CANS.clear();
      CAN_RX.clear();
      CAN_STREAMS.clear();
      serials.clear();
      for (Dio dio : DIOS) {
        dio.initialized = false;
        dio.value = true;
        dio.input = true;
      }
      for (Pwm pwm : PWMS) {
        pwm.initialized = false;
        pwm.pulse = 0;
      }
      for (Analog analog : ANALOGS) {
        analog.initialized = false;
        analog.voltage = 0;
      }
      for (Encoder encoder : ENCODERS) {
        encoder.initialized = false;
        encoder.count = 0;
        encoder.rate = 0;
      }
      Arrays.fill(LED_DATA, (byte) 0);
      Arrays.fill(IMU_ANGLE, 0);
      Arrays.fill(IMU_RATE, 0);
      Arrays.fill(IMU_ACCEL, 0);
      imuYaw = 0;
    }
    Callbacks.reset();
    SimDevices.reset();
  }

  // ---------------------------------------------------------------------------
  // Sim devices.
  // ---------------------------------------------------------------------------

  public static final class SimDevices {
    static final class Device {
      final String name;
      boolean enabled = true;
      final Map<String, Value> values = new HashMap<>();

      Device(String name) {
        this.name = name;
      }
    }

    static final class Value {
      final String name;
      final int direction;
      final Device device;
      HALValue current;
      HALValue initial;
      String[] options = new String[0];
      double[] optionValues = new double[0];

      Value(Device device, String name, int direction, HALValue initial) {
        this.device = device;
        this.name = name;
        this.direction = direction;
        this.initial = initial;
        this.current = initial;
      }
    }

    private static final Object lock = new Object();
    private static final Map<Integer, Device> devices = new HashMap<>();
    private static final Map<String, Integer> byName = new HashMap<>();
    private static final Map<Integer, Value> values = new HashMap<>();
    private static final Map<String, Boolean> prefixes = new HashMap<>();
    private static int nextDevice = 1;
    private static int nextValue = 1;

    private SimDevices() {}

    public static boolean enabled(String name) {
      synchronized (lock) {
        String best = "";
        boolean value = true;
        for (Map.Entry<String, Boolean> entry : prefixes.entrySet()) {
          if (name.startsWith(entry.getKey()) && entry.getKey().length() >= best.length()) {
            best = entry.getKey();
            value = entry.getValue();
          }
        }
        return value;
      }
    }

    public static void setEnabled(String prefix, boolean enabled) {
      synchronized (lock) {
        prefixes.put(prefix == null ? "" : prefix, enabled);
        for (Device device : devices.values()) {
          if (device.name.startsWith(prefix == null ? "" : prefix)) {
            device.enabled = enabled;
          }
        }
      }
    }

    public static int create(String name) {
      if (name == null || !enabled(name)) {
        return 0;
      }
      synchronized (lock) {
        Integer existing = byName.get(name);
        if (existing != null) {
          return existing;
        }
        int handle = handle(TYPE_SIM_DEVICE, nextDevice++);
        Device device = new Device(name);
        device.enabled = enabled(name);
        devices.put(handle, device);
        byName.put(name, handle);
        Callbacks.fireDevice("created", name, handle);
        return handle;
      }
    }

    public static void free(int handle) {
      synchronized (lock) {
        Device device = devices.remove(handle);
        if (device == null) {
          return;
        }
        byName.remove(device.name);
        values.entrySet().removeIf(entry -> entry.getValue().device == device);
        Callbacks.fireDevice("freed", device.name, handle);
      }
    }

    public static String name(int handle) {
      synchronized (lock) {
        Device device = devices.get(handle);
        return device == null ? "" : device.name;
      }
    }

    public static int createValue(
        int deviceHandle,
        String name,
        int direction,
        HALValue initial,
        String[] options,
        double[] optionValues) {
      synchronized (lock) {
        Device device = devices.get(deviceHandle);
        if (device == null || !device.enabled) {
          return 0;
        }
        Value existing = device.values.get(name);
        if (existing != null) {
          return values.entrySet().stream()
              .filter(entry -> entry.getValue() == existing)
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(0);
        }
        Value value = new Value(device, name, direction, initial);
        if (options != null) {
          value.options = options.clone();
        }
        if (optionValues != null) {
          value.optionValues = optionValues.clone();
        }
        int handle = handle(TYPE_SIM_VALUE, nextValue++);
        device.values.put(name, value);
        values.put(handle, value);
        Callbacks.fireValue("valueCreated." + deviceHandle, name, handle, direction, initial);
        return handle;
      }
    }

    public static HALValue get(int handle) {
      synchronized (lock) {
        Value value = values.get(handle);
        return value == null ? HALValue.makeUnassigned() : value.current;
      }
    }

    public static void set(int handle, HALValue value) {
      Value stored;
      synchronized (lock) {
        stored = values.get(handle);
        if (stored == null) {
          return;
        }
        stored.current = value;
      }
      Callbacks.fireValue("valueChanged." + handle, stored.name, handle, stored.direction, value);
    }

    public static void reset(int handle) {
      Value stored;
      HALValue old;
      synchronized (lock) {
        stored = values.get(handle);
        if (stored == null) {
          return;
        }
        old = stored.current;
        stored.current = stored.initial;
      }
      Callbacks.fireValue("valueReset." + handle, stored.name, handle, stored.direction, old);
    }

    public static int deviceOfValue(int handle) {
      synchronized (lock) {
        Value value = values.get(handle);
        if (value == null) {
          return 0;
        }
        return byName.getOrDefault(value.device.name, 0);
      }
    }

    public static int valueHandle(int device, String name) {
      synchronized (lock) {
        Device stored = devices.get(device);
        if (stored == null) {
          return 0;
        }
        Value value = stored.values.get(name);
        if (value == null) {
          return 0;
        }
        for (Map.Entry<Integer, Value> entry : values.entrySet()) {
          if (entry.getValue() == value) {
            return entry.getKey();
          }
        }
        return 0;
      }
    }

    public static SimDeviceDataJNI.SimDeviceInfo[] enumerate(String prefix) {
      synchronized (lock) {
        String p = prefix == null ? "" : prefix;
        return devices.entrySet().stream()
            .filter(entry -> entry.getValue().name.startsWith(p))
            .map(entry -> new SimDeviceDataJNI.SimDeviceInfo(entry.getValue().name, entry.getKey()))
            .toArray(SimDeviceDataJNI.SimDeviceInfo[]::new);
      }
    }

    public static SimDeviceDataJNI.SimValueInfo[] enumerateValues(int device) {
      synchronized (lock) {
        Device stored = devices.get(device);
        if (stored == null) {
          return new SimDeviceDataJNI.SimValueInfo[0];
        }
        List<SimDeviceDataJNI.SimValueInfo> infos = new ArrayList<>();
        for (Map.Entry<Integer, Value> entry : values.entrySet()) {
          Value value = entry.getValue();
          if (value.device != stored) {
            continue;
          }
          infos.add(
              new SimDeviceDataJNI.SimValueInfo(
                  value.name,
                  entry.getKey(),
                  value.direction,
                  value.current.getType(),
                  value.current.getNativeLong(),
                  value.current.getNativeDouble()));
        }
        return infos.toArray(SimDeviceDataJNI.SimValueInfo[]::new);
      }
    }

    public static String[] enumOptions(int handle) {
      synchronized (lock) {
        Value value = values.get(handle);
        return value == null ? new String[0] : value.options.clone();
      }
    }

    public static double[] enumDoubles(int handle) {
      synchronized (lock) {
        Value value = values.get(handle);
        return value == null ? new double[0] : value.optionValues.clone();
      }
    }

    public static void reset() {
      synchronized (lock) {
        devices.clear();
        byName.clear();
        values.clear();
      }
    }
  }

  public static void euler(double[] angles, int order) {
    if (angles == null || angles.length < 3) {
      return;
    }
    angles[0] = IMU_ANGLE[0];
    angles[1] = IMU_ANGLE[1];
    angles[2] = order == 0 ? imuYaw : IMU_ANGLE[2];
  }

  public static void quaternion(double[] quat) {
    if (quat == null || quat.length < 4) {
      return;
    }
    double yaw = imuYaw * 0.5;
    quat[0] = Math.cos(yaw);
    quat[1] = 0;
    quat[2] = 0;
    quat[3] = Math.sin(yaw);
  }

  public static AllianceStationID allianceFromInt(int id) {
    return switch (id) {
      case 1 -> AllianceStationID.RED_1;
      case 2 -> AllianceStationID.RED_2;
      case 3 -> AllianceStationID.RED_3;
      case 4 -> AllianceStationID.BLUE_1;
      case 5 -> AllianceStationID.BLUE_2;
      case 6 -> AllianceStationID.BLUE_3;
      default -> AllianceStationID.UNKNOWN;
    };
  }

  public static RobotMode robotMode() {
    return RobotMode.fromInt(DS.robotMode);
  }

  public static void copy(byte[] src, int start, int len, ByteBuffer dest) {
    if (src == null || dest == null) {
      return;
    }
    int n = Math.min(len, Math.min(src.length - start, dest.remaining()));
    if (n > 0) {
      dest.put(src, start, n);
    }
  }

  public static byte[] bytes(ByteBuffer buffer, int size) {
    if (buffer == null) {
      return new byte[0];
    }
    int n = Math.max(0, Math.min(size, buffer.remaining()));
    byte[] out = new byte[n];
    int pos = buffer.position();
    buffer.get(out);
    buffer.position(pos);
    return out;
  }
}
