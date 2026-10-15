package org.teamvan.hal;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.BiConsumer;
import org.wpilib.hardware.hal.HALValue;
import org.wpilib.hardware.hal.MatchInfoData;
import org.wpilib.hardware.hal.OpModeOption;
import org.wpilib.hardware.hal.PowerDistributionVersion;
import org.wpilib.hardware.hal.REVPHVersion;
import org.wpilib.hardware.hal.can.CANReceiveMessage;
import org.wpilib.hardware.hal.can.CANStatus;
import org.wpilib.hardware.hal.can.CANStreamMessage;
import org.wpilib.hardware.hal.simulation.BufferCallback;
import org.wpilib.hardware.hal.simulation.ConstBufferCallback;
import org.wpilib.hardware.hal.simulation.NotifyCallback;
import org.wpilib.hardware.hal.simulation.SimDeviceCallback;
import org.wpilib.hardware.hal.simulation.SimDeviceDataJNI;
import org.wpilib.hardware.hal.simulation.SimValueCallback;
import org.wpilib.hardware.hal.util.UncleanStatusException;
import org.wpilib.util.AlertDataJNI;

/** Java bodies for the WPILib 2027 HAL and WPIUtil methods that were native. */
public final class JniBridge {
  private JniBridge() {}

  private static void status(int code) {
    if (code != 0) {
      HalRuntime.setErrno(code);
      throw new UncleanStatusException(code, HalRuntime.errorMessage(code));
    }
  }

  private static int scale(int encoding) {
    return switch (encoding) {
      case 1 -> 1;
      case 2 -> 2;
      default -> 4;
    };
  }

  private static double decode(int encoding) {
    return switch (encoding) {
      case 1 -> 1.0;
      case 2 -> 0.5;
      default -> 0.25;
    };
  }

  public static void JNIWrapper_forceLoad() {}

  public static void JNIWrapper_suppressUnused(Object object) {}

  public static void WPIUtilJNIWrapper_forceLoad() {}

  public static void WPIUtilJNIWrapper_suppressUnused(Object object) {}

  public static boolean HAL_initialize() {
    return HalRuntime.initialize();
  }

  public static void HAL_shutdown() {
    HalRuntime.shutdownHal();
  }

  public static boolean HAL_hasMain() {
    return HalRuntime.hasMain();
  }

  public static void HAL_runMain() {
    HalRuntime.runMain();
  }

  public static void HAL_exitMain() {
    HalRuntime.exitMain();
  }

  public static void HAL_terminate() {}

  public static void HAL_simPeriodicBeforeNative() {}

  public static void HAL_simPeriodicAfterNative() {}

  public static boolean HAL_getBrownedOut() {
    return HalRuntime.RIO.vinVoltage < HalRuntime.RIO.brownoutVoltage;
  }

  public static boolean HAL_getSystemActive() {
    return HalRuntime.DS.enabled && HalRuntime.DS.dsAttached;
  }

  public static boolean HAL_getRSLState() {
    return false;
  }

  public static int HAL_getCommsDisableCount() {
    return 0;
  }

  public static boolean HAL_getSystemTimeValid() {
    return true;
  }

  public static String HALUtil_getSerialNumber() {
    return HalRuntime.RIO.serialNumber;
  }

  public static String HALUtil_getComments() {
    return HalRuntime.RIO.comments;
  }

  public static int HALUtil_getTeamNumber() {
    return HalRuntime.RIO.teamNumber;
  }

  public static long HALUtil_getMonotonicTime() {
    return HalRuntime.monotonicTime();
  }

  public static int HALUtil_getHALRuntimeType() {
    return HalRuntime.getRuntimeType();
  }

  public static String HALUtil_getHALErrorMessage(int code) {
    return HalRuntime.errorMessage(code);
  }

  public static int HALUtil_getHALErrno() {
    return HalRuntime.getErrno();
  }

  public static String HALUtil_getHALstrerror(int errno) {
    return HalRuntime.strerror(errno);
  }

  public static void UsageReportingJNI_report(String resource, String data) {}

  public static int UsageReportingJNI_publishCanVersion(
      int busId, int deviceId, String name, String version) {
    return 0;
  }

  public static int UsageReportingJNI_publishVersion(String name, String version) {
    return 0;
  }

  public static int UsageReportingJNI_publishWpilibVersion(String version) {
    return 0;
  }

  public static int PortsJNI_getNumCanBuses() {
    return HalRuntime.NUM_CAN_BUSES;
  }

  public static int PortsJNI_getNumSmartIo() {
    return HalRuntime.NUM_SMART_IO;
  }

  public static int PortsJNI_getNumCTREPCMModules() {
    return HalRuntime.NUM_CTREPCM_MODULES;
  }

  public static int PortsJNI_getNumCTRESolenoidChannels() {
    return HalRuntime.NUM_CTRE_SOLENOID_CHANNELS;
  }

  public static int PortsJNI_getNumCTREPDPModules() {
    return HalRuntime.NUM_CTREPDP_MODULES;
  }

  public static int PortsJNI_getNumCTREPDPChannels() {
    return HalRuntime.NUM_CTREPDP_CHANNELS;
  }

  public static int PortsJNI_getNumREVPDHModules() {
    return HalRuntime.NUM_REVPDH_MODULES;
  }

  public static int PortsJNI_getNumREVPDHChannels() {
    return HalRuntime.NUM_REVPDH_CHANNELS;
  }

  public static int PortsJNI_getNumREVPHModules() {
    return HalRuntime.NUM_REVPH_MODULES;
  }

  public static int PortsJNI_getNumREVPHChannels() {
    return HalRuntime.NUM_REVPH_CHANNELS;
  }

  public static int NotifierJNI_createNotifier() {
    return HalRuntime.Notifiers.create();
  }

  public static void NotifierJNI_setNotifierName(int handle, String name) {
    HalRuntime.Notifiers.setName(handle, name);
  }

  public static void NotifierJNI_destroyNotifier(int handle) {
    HalRuntime.Notifiers.destroy(handle);
  }

  public static void NotifierJNI_setNotifierAlarm(
      int handle, long alarm, long interval, boolean absolute, boolean ack) {
    HalRuntime.Notifiers.setAlarm(handle, alarm, interval, absolute, ack);
  }

  public static void NotifierJNI_cancelNotifierAlarm(int handle, boolean ack) {
    HalRuntime.Notifiers.cancel(handle, ack);
  }

  public static void NotifierJNI_acknowledgeNotifierAlarm(int handle) {
    HalRuntime.Notifiers.acknowledge(handle);
  }

  public static int NotifierJNI_getNotifierOverrun(int handle) {
    return HalRuntime.Notifiers.overrun(handle);
  }

  public static void DriverStationJNI_observeUserProgramStarting() {
    HalRuntime.observeStarting();
  }

  public static void DriverStationJNI_observeUserProgram(long word) {
    HalRuntime.observe(word);
  }

  public static long DriverStationJNI_nativeGetControlWord() {
    return HalRuntime.cachedControlWord();
  }

  public static long DriverStationJNI_nativeGetUncachedControlWord() {
    return HalRuntime.liveControlWord();
  }

  public static void DriverStationJNI_setOpModeOptions(OpModeOption[] options) {
    HalRuntime.DS.opModeOptions = options == null ? new OpModeOption[0] : options.clone();
    HalRuntime.Callbacks.fireOptions(HalRuntime.DS.opModeOptions);
  }

  public static int DriverStationJNI_nativeGetAllianceStation() {
    return HalRuntime.DS.cachedAlliance;
  }

  public static void DriverStationJNI_getAllJoystickData(
      int stick, float[] axes, short[] raw, byte[] povs, float[] fingers, long[] meta) {
    HalRuntime.getAllJoystick(stick, axes, raw, povs, fingers, meta);
  }

  public static int DriverStationJNI_setJoystickRumble(
      byte stick, int left, int right, int leftTrigger, int rightTrigger) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS) {
      return HalRuntime.HAL_PARAMETER_OUT_OF_RANGE;
    }
    int[] rumble = HalRuntime.DS.joysticks[stick].rumble;
    rumble[0] = left;
    rumble[1] = right;
    rumble[2] = leftTrigger;
    rumble[3] = rightTrigger;
    return 0;
  }

  public static int DriverStationJNI_setJoystickLeds(byte stick, int leds) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS) {
      return HalRuntime.HAL_PARAMETER_OUT_OF_RANGE;
    }
    HalRuntime.DS.joysticks[stick].leds = leds;
    return 0;
  }

  public static int DriverStationJNI_getJoystickIsGamepad(byte stick) {
    return stick >= 0
            && stick < HalRuntime.NUM_JOYSTICKS
            && HalRuntime.DS.cachedJoysticks[stick].gamepad
        ? 1
        : 0;
  }

  public static int DriverStationJNI_getJoystickGamepadType(byte stick) {
    return stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS
        ? HalRuntime.DS.cachedJoysticks[stick].gamepadType
        : 0;
  }

  public static int DriverStationJNI_getJoystickSupportedOutputs(byte stick) {
    return stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS
        ? HalRuntime.DS.cachedJoysticks[stick].supportedOutputs
        : 0;
  }

  public static String DriverStationJNI_getJoystickName(byte stick) {
    return stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS
        ? HalRuntime.DS.cachedJoysticks[stick].name
        : "";
  }

  public static double DriverStationJNI_getMatchTime() {
    return HalRuntime.DS.cachedMatchTime;
  }

  public static int DriverStationJNI_getMatchInfo(MatchInfoData info) {
    HalRuntime.matchInfo(info);
    return 0;
  }

  public static String DriverStationJNI_getGameData(String existing) {
    String data = HalRuntime.DS.cachedGameData;
    return data == null || data.isEmpty() ? existing : data;
  }

  public static int DriverStationJNI_sendError(
      boolean isError, int code, String details, String location, String stack, boolean print) {
    return HalRuntime.sendError(
        isError, code, details, location, stack, print && HalRuntime.DS.sendError);
  }

  public static int DriverStationJNI_sendConsoleLine(String line) {
    if (HalRuntime.DS.sendConsole && line != null) {
      System.out.println(line);
    }
    return 0;
  }

  public static int DriverStationJNI_sendProgramCrash(
      String details, String location, String stack) {
    System.err.println("Program Crash: " + details + "\nLocation: " + location + "\n" + stack);
    return 0;
  }

  public static boolean DriverStationJNI_refreshDSData() {
    return HalRuntime.refreshDs();
  }

  public static void DriverStationJNI_provideNewDataEventHandle(int handle) {
    synchronized (HalRuntime.DS) {
      HalRuntime.DS.newDataEvents.add(handle);
    }
  }

  public static void DriverStationJNI_removeNewDataEventHandle(int handle) {
    synchronized (HalRuntime.DS) {
      HalRuntime.DS.newDataEvents.remove(Integer.valueOf(handle));
    }
  }

  public static boolean DriverStationJNI_getOutputsActive() {
    return HalRuntime.outputsActive();
  }

  public static void DriverStationJNI_writeDisplayAnsi(String data) {
    HalRuntime.writeAnsi(data);
  }

  public static int DIOJNI_initializeDIOPort(int channel, boolean input) {
    int handle = HalRuntime.initializeDio(channel, input);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static boolean DIOJNI_checkDIOChannel(int channel) {
    return channel >= 0 && channel < HalRuntime.NUM_DIGITAL_CHANNELS;
  }

  public static void DIOJNI_freeDIOPort(int handle) {
    HalRuntime.freeDio(handle);
  }

  public static void DIOJNI_setDIOSimDevice(int handle, int device) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio != null) {
      dio.simDevice = device;
    }
  }

  public static void DIOJNI_setDIO(int handle, boolean value) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    if (dio.input) {
      status(HalRuntime.HAL_PARAMETER_OUT_OF_RANGE);
    }
    dio.value = value;
    HalRuntime.Callbacks.fire(
        "dio.value." + HalRuntime.dioChannel(handle), HALValue.makeBoolean(value));
  }

  public static void DIOJNI_setDIODirection(int handle, boolean input) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    dio.input = input;
    HalRuntime.Callbacks.fire(
        "dio.isInput." + HalRuntime.dioChannel(handle), HALValue.makeBoolean(input));
  }

  public static boolean DIOJNI_getDIO(int handle) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return false;
    }
    return dio.value;
  }

  public static boolean DIOJNI_getDIODirection(int handle) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return false;
    }
    return dio.input;
  }

  public static void DIOJNI_pulse(int handle, double length) {
    HalRuntime.Dio dio = HalRuntime.dio(handle);
    if (dio == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    dio.pulseLength = length;
    HalRuntime.Callbacks.fire(
        "dio.pulse." + HalRuntime.dioChannel(handle), HALValue.makeDouble(length));
  }

  public static void DIOJNI_pulseMultiple(long mask, double length) {}

  public static boolean DIOJNI_isPulsing(int handle) {
    return false;
  }

  public static boolean DIOJNI_isAnyPulsing() {
    return false;
  }

  public static int DIOJNI_allocateDigitalPWM() {
    int handle = HalRuntime.allocateDigitalPwm();
    if (handle == 0) {
      status(HalRuntime.HAL_NO_AVAILABLE_RESOURCES);
    }
    return handle;
  }

  public static void DIOJNI_freeDigitalPWM(int handle) {
    HalRuntime.freeDigitalPwm(handle);
  }

  public static void DIOJNI_setDigitalPWMRate(double rate) {}

  public static void DIOJNI_setDigitalPWMDutyCycle(int handle, double duty) {
    HalRuntime.DigitalPwm pwm = HalRuntime.digitalPwm(handle);
    if (pwm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    pwm.duty = Math.clamp(duty, 0, 1);
    HalRuntime.Callbacks.fire(
        "dpwm.duty." + HalRuntime.digitalPwmIndex(handle), HALValue.makeDouble(pwm.duty));
  }

  public static void DIOJNI_setDigitalPWMPPS(int handle, double duty) {
    DIOJNI_setDigitalPWMDutyCycle(handle, duty);
  }

  public static void DIOJNI_setDigitalPWMOutputChannel(int handle, int channel) {
    HalRuntime.DigitalPwm pwm = HalRuntime.digitalPwm(handle);
    if (pwm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    pwm.pin = channel;
    HalRuntime.Callbacks.fire(
        "dpwm.pin." + HalRuntime.digitalPwmIndex(handle), HALValue.makeInt(channel));
  }

  public static int PWMJNI_initializePWMPort(int channel) {
    int handle = HalRuntime.initializePwm(channel);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static boolean PWMJNI_checkPWMChannel(int channel) {
    return channel >= 0 && channel < HalRuntime.NUM_PWM_CHANNELS;
  }

  public static void PWMJNI_freePWMPort(int handle) {
    HalRuntime.freePwm(handle);
  }

  public static void PWMJNI_setPWMSimDevice(int handle, int device) {
    HalRuntime.Pwm pwm = HalRuntime.pwm(handle);
    if (pwm != null) {
      pwm.simDevice = device;
    }
  }

  public static void PWMJNI_setPulseTimeMicroseconds(int handle, int micros) {
    HalRuntime.Pwm pwm = HalRuntime.pwm(handle);
    if (pwm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    pwm.pulse = micros;
    HalRuntime.Callbacks.fire(
        "pwm.pulse." + HalRuntime.pwmChannel(handle), HALValue.makeInt(micros));
  }

  public static int PWMJNI_getPulseTimeMicroseconds(int handle) {
    HalRuntime.Pwm pwm = HalRuntime.pwm(handle);
    if (pwm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return pwm.pulse;
  }

  public static void PWMJNI_setPWMOutputPeriod(int handle, int period) {
    HalRuntime.Pwm pwm = HalRuntime.pwm(handle);
    if (pwm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    pwm.period = period;
    HalRuntime.Callbacks.fire(
        "pwm.period." + HalRuntime.pwmChannel(handle), HALValue.makeInt(period));
  }

  public static int AnalogInputJNI_initializeAnalogInputPort(int channel) {
    int handle = HalRuntime.initializeAnalog(channel);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void AnalogInputJNI_freeAnalogInputPort(int handle) {
    HalRuntime.freeAnalog(handle);
  }

  public static boolean AnalogInputJNI_checkAnalogModule(byte module) {
    return module == 1;
  }

  public static boolean AnalogInputJNI_checkAnalogInputChannel(int channel) {
    return channel >= 0 && channel < HalRuntime.NUM_ANALOG_INPUTS;
  }

  public static void AnalogInputJNI_setAnalogInputSimDevice(int handle, int device) {
    HalRuntime.Analog analog = HalRuntime.analog(handle);
    if (analog != null) {
      analog.simDevice = device;
    }
  }

  public static short AnalogInputJNI_getAnalogValue(int handle) {
    return (short)
        AnalogInputJNI_getAnalogVoltsToValue(handle, AnalogInputJNI_getAnalogVoltage(handle));
  }

  public static int AnalogInputJNI_getAnalogVoltsToValue(int handle, double voltage) {
    return (int) (voltage * 4095.0 / 3.3);
  }

  public static double AnalogInputJNI_getAnalogValueToVolts(int handle, int value) {
    return value / 4095.0 * 3.3;
  }

  public static double AnalogInputJNI_getAnalogVoltage(int handle) {
    HalRuntime.Analog analog = HalRuntime.analog(handle);
    if (analog == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return analog.voltage;
  }

  public static int EncoderJNI_initializeEncoder(int a, int b, boolean reverse, int encoding) {
    int handle = HalRuntime.initializeEncoder(a, b, reverse, encoding);
    if (handle == 0) {
      status(HalRuntime.HAL_NO_AVAILABLE_RESOURCES);
    }
    return handle;
  }

  public static void EncoderJNI_freeEncoder(int handle) {
    HalRuntime.freeEncoder(handle);
  }

  public static void EncoderJNI_setEncoderSimDevice(int handle, int device) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder != null) {
      encoder.simDevice = device;
    }
  }

  public static int EncoderJNI_getEncoder(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return encoder.count;
  }

  public static int EncoderJNI_getEncoderRaw(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    double factor = decode(encoder.encoding);
    return factor == 0 ? 0 : (int) (encoder.count / factor);
  }

  public static int EncoderJNI_getEncodingScaleFactor(int handle) {
    return EncoderJNI_getEncoderEncodingScale(handle);
  }

  public static int EncoderJNI_getEncoderEncodingScale(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return scale(encoder.encoding);
  }

  public static void EncoderJNI_resetEncoder(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    encoder.reset = true;
    encoder.count = 0;
    int index = HalRuntime.encoderIndex(handle);
    HalRuntime.Callbacks.fire("encoder.reset." + index, HALValue.makeBoolean(true));
    HalRuntime.Callbacks.fire("encoder.count." + index, HALValue.makeInt(0));
  }

  public static boolean EncoderJNI_getEncoderStopped(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return false;
    }
    return encoder.rate == 0;
  }

  public static boolean EncoderJNI_getEncoderDirection(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return false;
    }
    return encoder.direction;
  }

  public static double EncoderJNI_getEncoderDistance(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return encoder.count * encoder.distancePerPulse;
  }

  public static double EncoderJNI_getEncoderRate(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return encoder.rate;
  }

  public static void EncoderJNI_setEncoderRateWindow(int handle, int window) {
    if (HalRuntime.encoder(handle) == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
    }
    if (window < 5 || window > 255) {
      status(HalRuntime.HAL_PARAMETER_OUT_OF_RANGE);
    }
  }

  public static void EncoderJNI_setEncoderDistancePerPulse(int handle, double distance) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    if (distance == 0) {
      status(HalRuntime.HAL_PARAMETER_OUT_OF_RANGE);
    }
    encoder.distancePerPulse = distance;
    HalRuntime.Callbacks.fire(
        "encoder.dpp." + HalRuntime.encoderIndex(handle), HALValue.makeDouble(distance));
  }

  public static void EncoderJNI_setEncoderReverseDirection(int handle, boolean reverse) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    encoder.reverse = reverse;
    HalRuntime.Callbacks.fire(
        "encoder.reverse." + HalRuntime.encoderIndex(handle), HALValue.makeBoolean(reverse));
  }

  public static int EncoderJNI_getEncoderFPGAIndex(int handle) {
    int index = HalRuntime.encoderIndex(handle);
    if (index < 0) {
      status(HalRuntime.HAL_HANDLE_ERROR);
    }
    return index;
  }

  public static double EncoderJNI_getEncoderDecodingScaleFactor(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return decode(encoder.encoding);
  }

  public static double EncoderJNI_getEncoderDistancePerPulse(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 0;
    }
    return encoder.distancePerPulse;
  }

  public static int EncoderJNI_getEncoderEncodingType(int handle) {
    HalRuntime.Encoder encoder = HalRuntime.encoder(handle);
    if (encoder == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return 4;
    }
    return encoder.encoding;
  }

  public static int CounterJNI_initializeCounter(int channel, boolean rising) {
    int handle = HalRuntime.initializeCounter(channel, rising);
    if (handle == 0) {
      status(HalRuntime.HAL_NO_AVAILABLE_RESOURCES);
    }
    return handle;
  }

  public static void CounterJNI_freeCounter(int handle) {
    HalRuntime.freeCounter(handle);
  }

  public static void CounterJNI_setCounterEdgeConfiguration(int handle, boolean rising) {
    HalRuntime.Counter counter = HalRuntime.counter(handle);
    if (counter != null) {
      counter.rising = rising;
    }
  }

  public static void CounterJNI_setCounterRateWindow(int handle, int window) {
    if (window < 5 || window > 255) {
      status(HalRuntime.HAL_PARAMETER_OUT_OF_RANGE);
    }
  }

  public static void CounterJNI_resetCounter(int handle) {
    HalRuntime.Counter counter = HalRuntime.counter(handle);
    if (counter != null) {
      counter.count = 0;
    }
  }

  public static int CounterJNI_getCounter(int handle) {
    HalRuntime.Counter counter = HalRuntime.counter(handle);
    return counter == null ? 0 : counter.count;
  }

  public static double CounterJNI_getCounterRate(int handle) {
    HalRuntime.Counter counter = HalRuntime.counter(handle);
    return counter == null ? 0 : counter.rate;
  }

  public static boolean CounterJNI_getCounterStopped(int handle) {
    HalRuntime.Counter counter = HalRuntime.counter(handle);
    return counter == null || counter.stopped;
  }

  public static int DutyCycleJNI_initialize(int channel) {
    int handle = HalRuntime.initializeDuty(channel);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void DutyCycleJNI_free(int handle) {
    HalRuntime.freeDuty(handle);
  }

  public static double DutyCycleJNI_getFrequency(int handle) {
    HalRuntime.Duty duty = HalRuntime.duty(handle);
    return duty == null ? 0 : duty.frequency;
  }

  public static double DutyCycleJNI_getOutput(int handle) {
    HalRuntime.Duty duty = HalRuntime.duty(handle);
    return duty == null ? 0 : duty.output;
  }

  public static int DutyCycleJNI_getHighTime(int handle) {
    HalRuntime.Duty duty = HalRuntime.duty(handle);
    if (duty == null || duty.frequency == 0) {
      return 0;
    }
    return (int) (duty.output / duty.frequency * 1e6);
  }

  public static int AddressableLEDJNI_initialize(int channel) {
    int handle = HalRuntime.initializeLed(channel);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void AddressableLEDJNI_free(int handle) {
    HalRuntime.freeLed(handle);
  }

  public static void AddressableLEDJNI_setStart(int handle, int start) {
    HalRuntime.Led led = HalRuntime.led(handle);
    if (led != null) {
      led.start = start;
      HalRuntime.Callbacks.fire("led.start." + led.channel, HALValue.makeInt(start));
    }
  }

  public static void AddressableLEDJNI_setLength(int handle, int length) {
    HalRuntime.Led led = HalRuntime.led(handle);
    if (led != null) {
      led.length = length;
      HalRuntime.Callbacks.fire("led.length." + led.channel, HALValue.makeInt(length));
    }
  }

  public static void AddressableLEDJNI_setData(
      int outStart, int order, byte[] data, int start, int len) {
    HalRuntime.setLedData(outStart, order, data, start, len);
  }

  public static void AddressableLEDJNI_setDataFromBuffer(
      int outStart, int order, ByteBuffer data, int start, int len) {
    byte[] bytes = new byte[len];
    int pos = data.position();
    data.position(start);
    data.get(bytes, 0, Math.min(len, data.remaining()));
    data.position(pos);
    HalRuntime.setLedData(outStart, order, bytes, 0, bytes.length);
  }

  public static int SerialPortJNI_serialInitializePort(byte port) {
    return HalRuntime.initializeSerial(port, "serial-" + port);
  }

  public static int SerialPortJNI_serialInitializePortDirect(byte port, String name) {
    return HalRuntime.initializeSerial(port, name);
  }

  public static void SerialPortJNI_serialSetBaudRate(int handle, int baud) {
    HalRuntime.Serial serial = HalRuntime.serial(handle);
    if (serial != null) {
      serial.baud = baud;
    }
  }

  public static void SerialPortJNI_serialSetDataBits(int handle, byte bits) {}

  public static void SerialPortJNI_serialSetParity(int handle, byte parity) {}

  public static void SerialPortJNI_serialSetStopBits(int handle, byte bits) {}

  public static void SerialPortJNI_serialSetWriteMode(int handle, byte mode) {}

  public static void SerialPortJNI_serialSetFlowControl(int handle, byte flow) {}

  public static void SerialPortJNI_serialSetTimeout(int handle, double timeout) {}

  public static void SerialPortJNI_serialEnableTermination(int handle, char terminator) {}

  public static void SerialPortJNI_serialDisableTermination(int handle) {}

  public static void SerialPortJNI_serialSetReadBufferSize(int handle, int size) {}

  public static void SerialPortJNI_serialSetWriteBufferSize(int handle, int size) {}

  public static int SerialPortJNI_serialGetBytesReceived(int handle) {
    HalRuntime.Serial serial = HalRuntime.serial(handle);
    return serial == null ? 0 : serial.incoming.size();
  }

  public static int SerialPortJNI_serialRead(int handle, byte[] buffer, int count) {
    HalRuntime.Serial serial = HalRuntime.serial(handle);
    if (serial == null || buffer == null) {
      return 0;
    }
    int n = Math.min(count, Math.min(buffer.length, serial.incoming.size()));
    for (int i = 0; i < n; i++) {
      buffer[i] = serial.incoming.remove(0);
    }
    return n;
  }

  public static int SerialPortJNI_serialWrite(int handle, byte[] buffer, int count) {
    return buffer == null ? 0 : Math.min(count, buffer.length);
  }

  public static void SerialPortJNI_serialFlush(int handle) {}

  public static void SerialPortJNI_serialClear(int handle) {
    HalRuntime.Serial serial = HalRuntime.serial(handle);
    if (serial != null) {
      serial.incoming.clear();
    }
  }

  public static void SerialPortJNI_serialClose(int handle) {
    HalRuntime.closeSerial(handle);
  }

  public static void I2CJNI_i2CInitialize(int port) {}

  public static int I2CJNI_i2CTransaction(
      int port, byte address, ByteBuffer send, byte sendSize, ByteBuffer recv, byte recvSize) {
    HalRuntime.Callbacks.fireBuffer(
        "i2c.write." + port, HalRuntime.bytes(send, sendSize & 0xff), true);
    HalRuntime.Callbacks.fireBuffer("i2c.read." + port, new byte[recvSize & 0xff], false);
    return recvSize & 0xff;
  }

  public static int I2CJNI_i2CTransactionB(
      int port, byte address, byte[] send, byte sendSize, byte[] recv, byte recvSize) {
    HalRuntime.Callbacks.fireBuffer(
        "i2c.write." + port,
        send == null ? new byte[0] : Arrays.copyOf(send, sendSize & 0xff),
        true);
    return recvSize & 0xff;
  }

  public static int I2CJNI_i2CWrite(int port, byte address, ByteBuffer data, byte size) {
    return size & 0xff;
  }

  public static int I2CJNI_i2CWriteB(int port, byte address, byte[] data, byte size) {
    return size & 0xff;
  }

  public static int I2CJNI_i2CRead(int port, byte address, ByteBuffer data, byte size) {
    return size & 0xff;
  }

  public static int I2CJNI_i2CReadB(int port, byte address, byte[] data, byte size) {
    return size & 0xff;
  }

  public static void I2CJNI_i2CClose(int port) {}

  public static void IMUJNI_getIMUAcceleration(double[] accel) {
    if (accel != null && accel.length >= 3) {
      System.arraycopy(HalRuntime.IMU_ACCEL, 0, accel, 0, 3);
    }
  }

  public static void IMUJNI_getIMUGyroRates(double[] rates) {
    if (rates != null && rates.length >= 3) {
      System.arraycopy(HalRuntime.IMU_RATE, 0, rates, 0, 3);
    }
  }

  public static void IMUJNI_getIMUEulerAnglesFlat(double[] angles) {
    HalRuntime.euler(angles, 0);
  }

  public static void IMUJNI_getIMUEulerAnglesLandscape(double[] angles) {
    HalRuntime.euler(angles, 1);
  }

  public static void IMUJNI_getIMUEulerAnglesPortrait(double[] angles) {
    HalRuntime.euler(angles, 2);
  }

  public static void IMUJNI_getIMUQuaternion(double[] quat) {
    HalRuntime.quaternion(quat);
  }

  public static double IMUJNI_getIMUYawFlat() {
    return HalRuntime.imuYaw;
  }

  public static double IMUJNI_getIMUYawLandscape() {
    return HalRuntime.imuYaw;
  }

  public static double IMUJNI_getIMUYawPortrait() {
    return HalRuntime.imuYaw;
  }

  public static double PowerJNI_getVinVoltage() {
    return HalRuntime.RIO.vinVoltage;
  }

  public static double PowerJNI_getUserVoltage3V3() {
    return HalRuntime.RIO.userVoltage3V3;
  }

  public static double PowerJNI_getUserCurrent3V3() {
    return HalRuntime.RIO.userCurrent3V3;
  }

  public static void PowerJNI_setUserEnabled3V3(boolean enabled) {
    HalRuntime.RIO.userActive3V3 = enabled;
    HalRuntime.Callbacks.fire("rio.userActive3V3", HALValue.makeBoolean(enabled));
  }

  public static boolean PowerJNI_getUserActive3V3() {
    return HalRuntime.RIO.userActive3V3;
  }

  public static int PowerJNI_getUserCurrentFaults3V3() {
    return HalRuntime.RIO.userFaults3V3;
  }

  public static void PowerJNI_resetUserCurrentFaults() {
    HalRuntime.RIO.userFaults3V3 = 0;
    HalRuntime.Callbacks.fire("rio.userFaults3V3", HALValue.makeInt(0));
  }

  public static void PowerJNI_setBrownoutVoltages(double brownout, double recovery) {
    try {
      HalRuntime.setBrownout(brownout, recovery);
    } catch (IllegalArgumentException ex) {
      status(HalRuntime.HAL_PARAMETER_OUT_OF_RANGE);
    }
  }

  public static double PowerJNI_getCPUTemp() {
    return HalRuntime.RIO.cpuTemp;
  }

  public static int CTREPCMJNI_initialize(int bus, int module) {
    int handle = HalRuntime.initializePcm(module);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void CTREPCMJNI_free(int handle) {
    HalRuntime.freePcm(handle);
  }

  public static boolean CTREPCMJNI_checkSolenoidChannel(int channel) {
    return channel >= 0 && channel < HalRuntime.NUM_CTRE_SOLENOID_CHANNELS;
  }

  public static boolean CTREPCMJNI_getCompressor(int handle) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    return pcm != null && pcm.compressorOn;
  }

  public static void CTREPCMJNI_setClosedLoopControl(int handle, boolean enabled) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    if (pcm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    pcm.closedLoop = enabled;
    HalRuntime.Callbacks.fire(
        "pcm.closedLoop." + HalRuntime.pcmModule(handle), HALValue.makeBoolean(enabled));
  }

  public static boolean CTREPCMJNI_getClosedLoopControl(int handle) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    return pcm != null && pcm.closedLoop;
  }

  public static boolean CTREPCMJNI_getPressureSwitch(int handle) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    return pcm != null && pcm.pressureSwitch;
  }

  public static double CTREPCMJNI_getCompressorCurrent(int handle) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    return pcm == null ? 0 : pcm.current;
  }

  public static boolean CTREPCMJNI_getCompressorCurrentTooHighFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getCompressorCurrentTooHighStickyFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getCompressorShortedFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getCompressorShortedStickyFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getCompressorNotConnectedFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getCompressorNotConnectedStickyFault(int handle) {
    return false;
  }

  public static int CTREPCMJNI_getSolenoids(int handle) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    if (pcm == null) {
      return 0;
    }
    int value = 0;
    for (int i = 0; i < pcm.solenoids.length; i++) {
      if (pcm.solenoids[i]) {
        value |= 1 << i;
      }
    }
    return value;
  }

  public static void CTREPCMJNI_setSolenoids(int handle, int mask, int values) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    if (pcm == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    for (int i = 0; i < pcm.solenoids.length; i++) {
      if ((mask & (1 << i)) != 0) {
        pcm.solenoids[i] = (values & (1 << i)) != 0;
        HalRuntime.Callbacks.fire(
            "pcm.solenoid." + HalRuntime.pcmModule(handle) + "." + i,
            HALValue.makeBoolean(pcm.solenoids[i]));
      }
    }
  }

  public static int CTREPCMJNI_getSolenoidDisabledList(int handle) {
    return 0;
  }

  public static boolean CTREPCMJNI_getSolenoidVoltageFault(int handle) {
    return false;
  }

  public static boolean CTREPCMJNI_getSolenoidVoltageStickyFault(int handle) {
    return false;
  }

  public static void CTREPCMJNI_clearAllStickyFaults(int handle) {}

  public static void CTREPCMJNI_fireOneShot(int handle, int index) {}

  public static void CTREPCMJNI_setOneShotDuration(int handle, int index, int dur) {
    HalRuntime.Pcm pcm = HalRuntime.pcm(handle);
    if (pcm != null && index >= 0 && index < pcm.oneShot.length) {
      pcm.oneShot[index] = dur;
    }
  }

  public static int REVPHJNI_initialize(int bus, int module) {
    int handle = HalRuntime.initializePh(module);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void REVPHJNI_free(int handle) {
    HalRuntime.freePh(handle);
  }

  public static boolean REVPHJNI_checkSolenoidChannel(int channel) {
    return channel >= 0 && channel < HalRuntime.NUM_REVPH_CHANNELS;
  }

  public static boolean REVPHJNI_getCompressor(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    return ph != null && ph.compressorOn;
  }

  public static void REVPHJNI_setCompressorConfig(
      int handle, double min, double max, boolean forceDisable, boolean useDigital) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph == null) {
      return;
    }
    ph.configType = forceDisable ? 0 : useDigital ? 1 : 2;
  }

  public static void REVPHJNI_setClosedLoopControlDisabled(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph != null) {
      ph.configType = 0;
    }
  }

  public static void REVPHJNI_setClosedLoopControlDigital(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph != null) {
      ph.configType = 1;
    }
  }

  public static void REVPHJNI_setClosedLoopControlAnalog(int handle, double min, double max) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph != null) {
      ph.configType = 2;
    }
  }

  public static void REVPHJNI_setClosedLoopControlHybrid(int handle, double min, double max) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph != null) {
      ph.configType = 3;
    }
  }

  public static int REVPHJNI_getCompressorConfig(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    return ph == null ? 0 : ph.configType;
  }

  public static boolean REVPHJNI_getPressureSwitch(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    return ph != null && ph.pressureSwitch;
  }

  public static double REVPHJNI_getAnalogVoltage(int handle, int channel) {
    return 0;
  }

  public static double REVPHJNI_getCompressorCurrent(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    return ph == null ? 0 : ph.current;
  }

  public static int REVPHJNI_getSolenoids(int handle) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph == null) {
      return 0;
    }
    int value = 0;
    for (int i = 0; i < ph.solenoids.length; i++) {
      if (ph.solenoids[i]) {
        value |= 1 << i;
      }
    }
    return value;
  }

  public static void REVPHJNI_setSolenoids(int handle, int mask, int values) {
    HalRuntime.Ph ph = HalRuntime.ph(handle);
    if (ph == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    for (int i = 0; i < ph.solenoids.length; i++) {
      if ((mask & (1 << i)) != 0) {
        ph.solenoids[i] = (values & (1 << i)) != 0;
        HalRuntime.Callbacks.fire(
            "ph.solenoid." + HalRuntime.phModule(handle) + "." + i,
            HALValue.makeBoolean(ph.solenoids[i]));
      }
    }
  }

  public static void REVPHJNI_fireOneShot(int handle, int index, int dur) {}

  public static void REVPHJNI_clearStickyFaults(int handle) {}

  public static double REVPHJNI_getInputVoltage(int handle) {
    return 0;
  }

  public static double REVPHJNI_get5VVoltage(int handle) {
    return 0;
  }

  public static double REVPHJNI_getSolenoidCurrent(int handle) {
    return 0;
  }

  public static double REVPHJNI_getSolenoidVoltage(int handle) {
    return 0;
  }

  public static int REVPHJNI_getStickyFaultsNative(int handle) {
    return 0;
  }

  public static int REVPHJNI_getFaultsNative(int handle) {
    return 0;
  }

  public static int REVPHJNI_getSolenoidDisabledList(int handle) {
    return 0;
  }

  public static REVPHVersion REVPHJNI_getVersion(int handle) {
    return HalRuntime.phVersion();
  }

  public static int PowerDistributionJNI_initialize(int bus, int module, int type) {
    int handle = HalRuntime.initializePd(module, type);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void PowerDistributionJNI_free(int handle) {
    HalRuntime.freePd(handle);
  }

  public static int PowerDistributionJNI_getModuleNumber(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd == null ? 0 : pd.module;
  }

  public static boolean PowerDistributionJNI_checkModule(int module, int type) {
    if (type == 1) {
      return module >= 0 && module < HalRuntime.NUM_CTREPDP_MODULES;
    }
    return module >= 1 && module <= HalRuntime.NUM_REVPDH_MODULES;
  }

  public static boolean PowerDistributionJNI_checkChannel(int handle, int channel) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    int limit =
        pd != null && pd.type == 1
            ? HalRuntime.NUM_CTREPDP_CHANNELS
            : HalRuntime.NUM_REVPDH_CHANNELS;
    return channel >= 0 && channel < limit;
  }

  public static int PowerDistributionJNI_getType(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd == null ? 1 : pd.type;
  }

  public static int PowerDistributionJNI_getNumChannels(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd != null && pd.type == 1
        ? HalRuntime.NUM_CTREPDP_CHANNELS
        : HalRuntime.NUM_REVPDH_CHANNELS;
  }

  public static double PowerDistributionJNI_getTemperature(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd == null ? 0 : pd.temperature;
  }

  public static double PowerDistributionJNI_getVoltage(int handle) {
    return PowerDistributionJNI_getVoltageNoError(handle);
  }

  public static double PowerDistributionJNI_getVoltageNoError(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd == null ? 0 : pd.voltage;
  }

  public static double PowerDistributionJNI_getChannelCurrent(int handle, int channel) {
    return PowerDistributionJNI_getChannelCurrentNoError(handle, channel);
  }

  public static double PowerDistributionJNI_getChannelCurrentNoError(int handle, int channel) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    if (pd == null || channel < 0 || channel >= pd.current.length) {
      return 0;
    }
    return pd.current[channel];
  }

  public static double[] PowerDistributionJNI_getAllCurrents(int handle) {
    return PowerDistributionJNI_getAllCurrentsNoError(handle);
  }

  public static double[] PowerDistributionJNI_getAllCurrentsNoError(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd == null ? new double[0] : pd.current.clone();
  }

  public static double PowerDistributionJNI_getTotalCurrent(int handle) {
    return PowerDistributionJNI_getTotalCurrentNoError(handle);
  }

  public static double PowerDistributionJNI_getTotalCurrentNoError(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    if (pd == null) {
      return 0;
    }
    double total = 0;
    for (double current : pd.current) {
      total += current;
    }
    return total;
  }

  public static double PowerDistributionJNI_getTotalPower(int handle) {
    return PowerDistributionJNI_getVoltage(handle) * PowerDistributionJNI_getTotalCurrent(handle);
  }

  public static double PowerDistributionJNI_getTotalEnergy(int handle) {
    return 0;
  }

  public static void PowerDistributionJNI_resetTotalEnergy(int handle) {}

  public static void PowerDistributionJNI_clearStickyFaults(int handle) {}

  public static boolean PowerDistributionJNI_getSwitchableChannel(int handle) {
    return PowerDistributionJNI_getSwitchableChannelNoError(handle);
  }

  public static boolean PowerDistributionJNI_getSwitchableChannelNoError(int handle) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    return pd != null && pd.switchable;
  }

  public static void PowerDistributionJNI_setSwitchableChannel(int handle, boolean enabled) {
    PowerDistributionJNI_setSwitchableChannelNoError(handle, enabled);
  }

  public static void PowerDistributionJNI_setSwitchableChannelNoError(int handle, boolean enabled) {
    HalRuntime.Pd pd = HalRuntime.pd(handle);
    if (pd != null) {
      pd.switchable = enabled;
    }
  }

  public static int PowerDistributionJNI_getFaultsNative(int handle) {
    return 0;
  }

  public static int PowerDistributionJNI_getStickyFaultsNative(int handle) {
    return 0;
  }

  public static PowerDistributionVersion PowerDistributionJNI_getVersion(int handle) {
    return HalRuntime.pdVersion();
  }

  public static int CANAPIJNI_initializeCAN(int bus, int manufacturer, int deviceId, int type) {
    int handle = HalRuntime.initializeCan(bus, manufacturer, deviceId, type);
    if (handle == 0) {
      status(HalRuntime.getErrno());
    }
    return handle;
  }

  public static void CANAPIJNI_cleanCAN(int handle) {
    HalRuntime.freeCan(handle);
  }

  private static void writeCan(int handle, int api, byte[] data, int len, int flags, boolean rtr) {
    HalRuntime.CanDevice device = HalRuntime.can(handle);
    if (device == null) {
      status(HalRuntime.HAL_HANDLE_ERROR);
      return;
    }
    int id = HalRuntime.canId(device, api);
    if (rtr) {
      id |= 0x40000000;
    }
    HalRuntime.sendCan(device.bus, id, data, len, flags);
  }

  public static void CANAPIJNI_writeCANPacket(
      int handle, int api, byte[] data, int len, int flags) {
    writeCan(handle, api, data, len, flags, false);
  }

  public static void CANAPIJNI_writeCANPacketRepeating(
      int handle, int api, byte[] data, int len, int flags, int repeat) {
    writeCan(handle, api, data, len, flags, false);
  }

  public static void CANAPIJNI_writeCANRTRFrame(
      int handle, int api, byte[] data, int len, int flags) {
    writeCan(handle, api, data, len, flags, true);
  }

  public static int CANAPIJNI_writeCANPacketNoThrow(
      int handle, int api, byte[] data, int len, int flags) {
    HalRuntime.CanDevice device = HalRuntime.can(handle);
    if (device == null) {
      return HalRuntime.HAL_HANDLE_ERROR;
    }
    return HalRuntime.sendCan(device.bus, HalRuntime.canId(device, api), data, len, flags);
  }

  public static int CANAPIJNI_writeCANPacketRepeatingNoThrow(
      int handle, int api, byte[] data, int len, int flags, int repeat) {
    return CANAPIJNI_writeCANPacketNoThrow(handle, api, data, len, flags);
  }

  public static int CANAPIJNI_writeCANRTRFrameNoThrow(
      int handle, int api, byte[] data, int len, int flags) {
    return CANAPIJNI_writeCANPacketNoThrow(handle, api, data, len, flags | 0x40000000);
  }

  public static void CANAPIJNI_stopCANPacketRepeating(int handle, int api) {}

  public static boolean CANAPIJNI_readCANPacketNew(int handle, int api, CANReceiveMessage message) {
    HalRuntime.CanDevice device = HalRuntime.can(handle);
    if (device == null) {
      return false;
    }
    return HalRuntime.receiveCan(device.bus, HalRuntime.canId(device, api), message);
  }

  public static boolean CANAPIJNI_readCANPacketLatest(
      int handle, int api, CANReceiveMessage message) {
    boolean found = CANAPIJNI_readCANPacketNew(handle, api, message);
    if (found) {
      HalRuntime.CanDevice device = HalRuntime.can(handle);
      device.latest.put(api, message);
    }
    return found
        || (HalRuntime.can(handle) != null && HalRuntime.can(handle).latest.containsKey(api));
  }

  public static boolean CANAPIJNI_readCANPacketTimeout(
      int handle, int api, CANReceiveMessage message, int timeoutMs) {
    if (CANAPIJNI_readCANPacketNew(handle, api, message)) {
      return true;
    }
    HalRuntime.CanDevice device = HalRuntime.can(handle);
    if (device == null) {
      return false;
    }
    CANReceiveMessage cached = device.latest.get(api);
    if (cached == null) {
      return false;
    }
    if (HalRuntime.now() - cached.timestamp > timeoutMs * 1_000_000L) {
      status(HalRuntime.HAL_CAN_TIMEOUT);
      return false;
    }
    message.length = cached.length;
    message.flags = cached.flags;
    message.timestamp = cached.timestamp;
    System.arraycopy(cached.data, 0, message.data, 0, Math.min(cached.length, message.data.length));
    return true;
  }

  public static int CANJNI_sendMessage(
      int bus, int messageId, byte[] data, int length, int flags, int period) {
    return HalRuntime.sendCan(bus, messageId, data, length, flags);
  }

  public static int CANJNI_receiveMessage(int bus, int messageId, CANReceiveMessage message) {
    return HalRuntime.receiveCan(bus, messageId, message)
        ? 0
        : HalRuntime.HAL_ERR_CAN_MESSAGE_NOT_FOUND;
  }

  public static void CANJNI_getCANStatus(int bus, CANStatus status) {
    HalRuntime.canStatus(bus, status);
  }

  public static int CANJNI_openCANStreamSession(int bus, int messageId, int mask, int max) {
    return HalRuntime.openCanStream(bus, messageId, mask, max);
  }

  public static void CANJNI_closeCANStreamSession(int handle) {
    HalRuntime.closeCanStream(handle);
  }

  public static int CANJNI_readCANStreamSession(
      int handle, CANStreamMessage[] messages, int toRead) {
    return HalRuntime.readCanStream(handle, messages, toRead);
  }

  public static int SimDeviceJNI_createSimDevice(String name) {
    return HalRuntime.SimDevices.create(name);
  }

  public static void SimDeviceJNI_freeSimDevice(int handle) {
    HalRuntime.SimDevices.free(handle);
  }

  public static String SimDeviceJNI_getSimDeviceName(int handle) {
    return HalRuntime.SimDevices.name(handle);
  }

  public static int SimDeviceJNI_createSimValueNative(
      int device, String name, int direction, int type, long value1, double value2) {
    return HalRuntime.SimDevices.createValue(
        device, name, direction, HALValue.fromNative(type, value1, value2), null, null);
  }

  public static int SimDeviceJNI_createSimValueEnum(
      int device, String name, int direction, String[] options, int initial) {
    return HalRuntime.SimDevices.createValue(
        device, name, direction, HALValue.makeEnum(initial), options, null);
  }

  public static int SimDeviceJNI_createSimValueEnumDouble(
      int device, String name, int direction, String[] options, double[] values, int initial) {
    return HalRuntime.SimDevices.createValue(
        device, name, direction, HALValue.makeEnum(initial), options, values);
  }

  public static HALValue SimDeviceJNI_getSimValue(int handle) {
    return HalRuntime.SimDevices.get(handle);
  }

  public static int SimDeviceJNI_getSimValueInt(int handle) {
    return (int) HalRuntime.SimDevices.get(handle).getLong();
  }

  public static long SimDeviceJNI_getSimValueLong(int handle) {
    return HalRuntime.SimDevices.get(handle).getLong();
  }

  public static double SimDeviceJNI_getSimValueDouble(int handle) {
    return HalRuntime.SimDevices.get(handle).getDouble();
  }

  public static int SimDeviceJNI_getSimValueEnum(int handle) {
    return (int) HalRuntime.SimDevices.get(handle).getLong();
  }

  public static boolean SimDeviceJNI_getSimValueBoolean(int handle) {
    return HalRuntime.SimDevices.get(handle).getBoolean();
  }

  public static void SimDeviceJNI_setSimValueNative(
      int handle, int type, long value1, double value2) {
    HalRuntime.SimDevices.set(handle, HALValue.fromNative(type, value1, value2));
  }

  public static void SimDeviceJNI_resetSimValue(int handle) {
    HalRuntime.SimDevices.reset(handle);
  }

  public static int SystemServerJNI_getSystemServerHandle() {
    return 0;
  }

  public static int ThreadsJNI_getCurrentThreadPriority() {
    return Thread.currentThread().getPriority();
  }

  public static boolean ThreadsJNI_setCurrentThreadPriority(int priority) {
    try {
      Thread.currentThread().setPriority(priority);
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public static void SimulatorJNI_setRuntimeType(int type) {
    HalRuntime.setRuntimeType(type);
  }

  public static void SimulatorJNI_waitForProgramStart(boolean waitForFirst) {
    HalRuntime.waitForProgramStart(waitForFirst);
  }

  public static void SimulatorJNI_setProgramStarted(boolean started) {
    HalRuntime.setProgramStarted(started);
  }

  public static boolean SimulatorJNI_getProgramStarted() {
    return HalRuntime.getProgramStarted();
  }

  public static void SimulatorJNI_setProgramState(long word) {
    HalRuntime.setProgramState(word);
  }

  public static long SimulatorJNI_nativeGetProgramState() {
    return HalRuntime.getProgramState();
  }

  public static void SimulatorJNI_restartTiming() {
    HalRuntime.restartTiming();
  }

  public static void SimulatorJNI_pauseTiming() {
    HalRuntime.pauseTiming();
  }

  public static void SimulatorJNI_resumeTiming() {
    HalRuntime.resumeTiming();
  }

  public static boolean SimulatorJNI_isTimingPaused() {
    return HalRuntime.isTimingPaused();
  }

  public static void SimulatorJNI_stepTiming(long delta) {
    HalRuntime.stepTimingSync(delta);
  }

  public static void SimulatorJNI_stepTimingAsync(long delta) {
    HalRuntime.stepTimingAsync(delta);
  }

  public static void SimulatorJNI_resetHandles() {
    HalRuntime.resetHardware();
  }

  public static long NotifierDataJNI_getNextTimeout() {
    return HalRuntime.Notifiers.nextTimeout();
  }

  public static int NotifierDataJNI_getNumNotifiers() {
    return HalRuntime.Notifiers.count();
  }

  private static int register(
      String key, NotifyCallback callback, boolean initial, HALValue value) {
    return HalRuntime.Callbacks.register(key, callback, initial, value);
  }

  public static int AddressableLEDDataJNI_registerInitializedCallback(
      int index, NotifyCallback callback, boolean initial) {
    return register(
        "led.initialized." + index,
        callback,
        initial,
        HALValue.makeBoolean(
            index >= 0 && index < HalRuntime.LEDS.length && HalRuntime.LEDS[index].initialized));
  }

  public static void AddressableLEDDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean AddressableLEDDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.LEDS.length && HalRuntime.LEDS[index].initialized;
  }

  public static void AddressableLEDDataJNI_setInitialized(int index, boolean initialized) {
    if (index >= 0 && index < HalRuntime.LEDS.length) {
      HalRuntime.LEDS[index].initialized = initialized;
    }
  }

  public static int AddressableLEDDataJNI_registerStartCallback(
      int index, NotifyCallback callback, boolean initial) {
    return register(
        "led.start." + index,
        callback,
        initial,
        HALValue.makeInt(AddressableLEDDataJNI_getStart(index)));
  }

  public static void AddressableLEDDataJNI_cancelStartCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int AddressableLEDDataJNI_getStart(int index) {
    return index >= 0 && index < HalRuntime.LEDS.length ? HalRuntime.LEDS[index].start : 0;
  }

  public static void AddressableLEDDataJNI_setStart(int index, int start) {
    if (index >= 0 && index < HalRuntime.LEDS.length) {
      HalRuntime.LEDS[index].start = start;
    }
  }

  public static int AddressableLEDDataJNI_registerLengthCallback(
      int index, NotifyCallback callback, boolean initial) {
    return register(
        "led.length." + index,
        callback,
        initial,
        HALValue.makeInt(AddressableLEDDataJNI_getLength(index)));
  }

  public static void AddressableLEDDataJNI_cancelLengthCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int AddressableLEDDataJNI_getLength(int index) {
    return index >= 0 && index < HalRuntime.LEDS.length ? HalRuntime.LEDS[index].length : 0;
  }

  public static void AddressableLEDDataJNI_setLength(int index, int length) {
    if (index >= 0 && index < HalRuntime.LEDS.length) {
      HalRuntime.LEDS[index].length = length;
    }
  }

  public static int AddressableLEDDataJNI_registerDataCallback(ConstBufferCallback callback) {
    return HalRuntime.Callbacks.registerConstBuffer("led.data", callback);
  }

  public static void AddressableLEDDataJNI_cancelDataCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static byte[] AddressableLEDDataJNI_getData(int start, int length) {
    return HalRuntime.getLedData(start, length);
  }

  public static void AddressableLEDDataJNI_setData(int start, byte[] data) {
    HalRuntime.setLedData(start, 0, data, 0, data == null ? 0 : data.length);
  }

  public static void AddressableLEDDataJNI_resetData(int index) {
    if (index >= 0 && index < HalRuntime.LEDS.length) {
      HalRuntime.LEDS[index].initialized = false;
      HalRuntime.LEDS[index].start = 0;
      HalRuntime.LEDS[index].length = 0;
    }
  }

  public static int AnalogInDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "analog.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(AnalogInDataJNI_getInitialized(index)));
  }

  public static void AnalogInDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean AnalogInDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.ANALOGS.length && HalRuntime.ANALOGS[index].initialized;
  }

  public static void AnalogInDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.ANALOGS.length) {
      HalRuntime.ANALOGS[index].initialized = value;
    }
  }

  public static int AnalogInDataJNI_registerVoltageCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "analog.voltage." + index,
        cb,
        initial,
        HALValue.makeDouble(AnalogInDataJNI_getVoltage(index)));
  }

  public static void AnalogInDataJNI_cancelVoltageCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double AnalogInDataJNI_getVoltage(int index) {
    return index >= 0 && index < HalRuntime.ANALOGS.length ? HalRuntime.ANALOGS[index].voltage : 0;
  }

  public static void AnalogInDataJNI_setVoltage(int index, double voltage) {
    if (index >= 0 && index < HalRuntime.ANALOGS.length) {
      HalRuntime.ANALOGS[index].voltage = voltage;
      HalRuntime.Callbacks.fire("analog.voltage." + index, HALValue.makeDouble(voltage));
    }
  }

  public static void AnalogInDataJNI_resetData(int index) {
    AnalogInDataJNI_setInitialized(index, false);
    AnalogInDataJNI_setVoltage(index, 0);
  }

  public static int DIODataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dio.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(DIODataJNI_getInitialized(index)));
  }

  public static void DIODataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DIODataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.DIOS.length && HalRuntime.DIOS[index].initialized;
  }

  public static void DIODataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].initialized = value;
    }
  }

  public static int DIODataJNI_registerValueCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dio.value." + index, cb, initial, HALValue.makeBoolean(DIODataJNI_getValue(index)));
  }

  public static void DIODataJNI_cancelValueCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DIODataJNI_getValue(int index) {
    return index >= 0 && index < HalRuntime.DIOS.length && HalRuntime.DIOS[index].value;
  }

  public static void DIODataJNI_setValue(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].value = value;
      HalRuntime.Callbacks.fire("dio.value." + index, HALValue.makeBoolean(value));
    }
  }

  public static int DIODataJNI_registerPulseLengthCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dio.pulse." + index, cb, initial, HALValue.makeDouble(DIODataJNI_getPulseLength(index)));
  }

  public static void DIODataJNI_cancelPulseLengthCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double DIODataJNI_getPulseLength(int index) {
    return index >= 0 && index < HalRuntime.DIOS.length ? HalRuntime.DIOS[index].pulseLength : 0;
  }

  public static void DIODataJNI_setPulseLength(int index, double value) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].pulseLength = value;
    }
  }

  public static int DIODataJNI_registerIsInputCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dio.isInput." + index, cb, initial, HALValue.makeBoolean(DIODataJNI_getIsInput(index)));
  }

  public static void DIODataJNI_cancelIsInputCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DIODataJNI_getIsInput(int index) {
    return index < 0 || index >= HalRuntime.DIOS.length || HalRuntime.DIOS[index].input;
  }

  public static void DIODataJNI_setIsInput(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].input = value;
    }
  }

  public static int DIODataJNI_registerFilterIndexCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dio.filter." + index, cb, initial, HALValue.makeInt(DIODataJNI_getFilterIndex(index)));
  }

  public static void DIODataJNI_cancelFilterIndexCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int DIODataJNI_getFilterIndex(int index) {
    return index >= 0 && index < HalRuntime.DIOS.length ? HalRuntime.DIOS[index].filterIndex : -1;
  }

  public static void DIODataJNI_setFilterIndex(int index, int value) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].filterIndex = value;
    }
  }

  public static void DIODataJNI_resetData(int index) {
    if (index >= 0 && index < HalRuntime.DIOS.length) {
      HalRuntime.DIOS[index].initialized = false;
      HalRuntime.DIOS[index].value = true;
      HalRuntime.DIOS[index].input = true;
    }
  }

  public static int PWMDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pwm.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(PWMDataJNI_getInitialized(index)));
  }

  public static void PWMDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean PWMDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.PWMS.length && HalRuntime.PWMS[index].initialized;
  }

  public static void PWMDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PWMS.length) {
      HalRuntime.PWMS[index].initialized = value;
    }
  }

  public static int PWMDataJNI_registerPulseMicrosecondCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pwm.pulse." + index, cb, initial, HALValue.makeInt(PWMDataJNI_getPulseMicrosecond(index)));
  }

  public static void PWMDataJNI_cancelPulseMicrosecondCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int PWMDataJNI_getPulseMicrosecond(int index) {
    return index >= 0 && index < HalRuntime.PWMS.length ? HalRuntime.PWMS[index].pulse : 0;
  }

  public static void PWMDataJNI_setPulseMicrosecond(int index, int value) {
    if (index >= 0 && index < HalRuntime.PWMS.length) {
      HalRuntime.PWMS[index].pulse = value;
      HalRuntime.Callbacks.fire("pwm.pulse." + index, HALValue.makeInt(value));
    }
  }

  public static int PWMDataJNI_registerOutputPeriodCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pwm.period." + index, cb, initial, HALValue.makeInt(PWMDataJNI_getOutputPeriod(index)));
  }

  public static void PWMDataJNI_cancelOutputPeriodCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int PWMDataJNI_getOutputPeriod(int index) {
    return index >= 0 && index < HalRuntime.PWMS.length ? HalRuntime.PWMS[index].period : 0;
  }

  public static void PWMDataJNI_setOutputPeriod(int index, int value) {
    if (index >= 0 && index < HalRuntime.PWMS.length) {
      HalRuntime.PWMS[index].period = value;
    }
  }

  public static void PWMDataJNI_resetData(int index) {
    PWMDataJNI_setInitialized(index, false);
    PWMDataJNI_setPulseMicrosecond(index, 0);
  }

  public static int DutyCycleDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "duty.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(DutyCycleDataJNI_getInitialized(index)));
  }

  public static void DutyCycleDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DutyCycleDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.DUTIES.length && HalRuntime.DUTIES[index].initialized;
  }

  public static void DutyCycleDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.DUTIES.length) {
      HalRuntime.DUTIES[index].initialized = value;
    }
  }

  public static int DutyCycleDataJNI_registerFrequencyCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "duty.frequency." + index,
        cb,
        initial,
        HALValue.makeDouble(DutyCycleDataJNI_getFrequency(index)));
  }

  public static void DutyCycleDataJNI_cancelFrequencyCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double DutyCycleDataJNI_getFrequency(int index) {
    return index >= 0 && index < HalRuntime.DUTIES.length ? HalRuntime.DUTIES[index].frequency : 0;
  }

  public static void DutyCycleDataJNI_setFrequency(int index, double value) {
    if (index >= 0 && index < HalRuntime.DUTIES.length) {
      HalRuntime.DUTIES[index].frequency = value;
      HalRuntime.Callbacks.fire("duty.frequency." + index, HALValue.makeDouble(value));
    }
  }

  public static int DutyCycleDataJNI_registerOutputCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "duty.output." + index,
        cb,
        initial,
        HALValue.makeDouble(DutyCycleDataJNI_getOutput(index)));
  }

  public static void DutyCycleDataJNI_cancelOutputCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double DutyCycleDataJNI_getOutput(int index) {
    return index >= 0 && index < HalRuntime.DUTIES.length ? HalRuntime.DUTIES[index].output : 0;
  }

  public static void DutyCycleDataJNI_setOutput(int index, double value) {
    if (index >= 0 && index < HalRuntime.DUTIES.length) {
      HalRuntime.DUTIES[index].output = value;
      HalRuntime.Callbacks.fire("duty.output." + index, HALValue.makeDouble(value));
    }
  }

  public static void DutyCycleDataJNI_resetData(int index) {
    DutyCycleDataJNI_setInitialized(index, false);
    DutyCycleDataJNI_setFrequency(index, 0);
    DutyCycleDataJNI_setOutput(index, 0);
  }

  public static int EncoderDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(EncoderDataJNI_getInitialized(index)));
  }

  public static void EncoderDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean EncoderDataJNI_getInitialized(int index) {
    return index >= 0
        && index < HalRuntime.ENCODERS.length
        && HalRuntime.ENCODERS[index].initialized;
  }

  public static void EncoderDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].initialized = value;
    }
  }

  public static int EncoderDataJNI_registerCountCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.count." + index, cb, initial, HALValue.makeInt(EncoderDataJNI_getCount(index)));
  }

  public static void EncoderDataJNI_cancelCountCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int EncoderDataJNI_getCount(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length ? HalRuntime.ENCODERS[index].count : 0;
  }

  public static void EncoderDataJNI_setCount(int index, int count) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].count = count;
      HalRuntime.Callbacks.fire("encoder.count." + index, HALValue.makeInt(count));
    }
  }

  public static int EncoderDataJNI_registerRateCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.rate." + index, cb, initial, HALValue.makeDouble(EncoderDataJNI_getRate(index)));
  }

  public static void EncoderDataJNI_cancelRateCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int EncoderDataJNI_registerResetCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register("encoder.reset." + index, cb, initial, HALValue.makeBoolean(false));
  }

  public static void EncoderDataJNI_cancelResetCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean EncoderDataJNI_getReset(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length && HalRuntime.ENCODERS[index].reset;
  }

  public static void EncoderDataJNI_setReset(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].reset = value;
      if (value) {
        HalRuntime.ENCODERS[index].count = 0;
      }
    }
  }

  public static int EncoderDataJNI_registerDirectionCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.direction." + index,
        cb,
        initial,
        HALValue.makeBoolean(EncoderDataJNI_getDirection(index)));
  }

  public static void EncoderDataJNI_cancelDirectionCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean EncoderDataJNI_getDirection(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length && HalRuntime.ENCODERS[index].direction;
  }

  public static void EncoderDataJNI_setDirection(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].direction = value;
    }
  }

  public static int EncoderDataJNI_registerReverseDirectionCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.reverse." + index,
        cb,
        initial,
        HALValue.makeBoolean(EncoderDataJNI_getReverseDirection(index)));
  }

  public static void EncoderDataJNI_cancelReverseDirectionCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean EncoderDataJNI_getReverseDirection(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length && HalRuntime.ENCODERS[index].reverse;
  }

  public static void EncoderDataJNI_setReverseDirection(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].reverse = value;
    }
  }

  public static int EncoderDataJNI_registerDistancePerPulseCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "encoder.dpp." + index,
        cb,
        initial,
        HALValue.makeDouble(EncoderDataJNI_getDistancePerPulse(index)));
  }

  public static void EncoderDataJNI_cancelDistancePerPulseCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double EncoderDataJNI_getDistancePerPulse(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length
        ? HalRuntime.ENCODERS[index].distancePerPulse
        : 1;
  }

  public static void EncoderDataJNI_setDistancePerPulse(int index, double value) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].distancePerPulse = value;
    }
  }

  public static void EncoderDataJNI_setDistance(int index, double distance) {
    double dpp = EncoderDataJNI_getDistancePerPulse(index);
    if (dpp != 0) {
      EncoderDataJNI_setCount(index, (int) (distance / dpp));
    }
  }

  public static double EncoderDataJNI_getDistance(int index) {
    return EncoderDataJNI_getCount(index) * EncoderDataJNI_getDistancePerPulse(index);
  }

  public static void EncoderDataJNI_setRate(int index, double rate) {
    if (index >= 0 && index < HalRuntime.ENCODERS.length) {
      HalRuntime.ENCODERS[index].rate = rate;
      HalRuntime.Callbacks.fire("encoder.rate." + index, HALValue.makeDouble(rate));
    }
  }

  public static double EncoderDataJNI_getRate(int index) {
    return index >= 0 && index < HalRuntime.ENCODERS.length ? HalRuntime.ENCODERS[index].rate : 0;
  }

  public static void EncoderDataJNI_resetData(int index) {
    EncoderDataJNI_setInitialized(index, false);
    EncoderDataJNI_setCount(index, 0);
    EncoderDataJNI_setRate(index, 0);
  }

  public static int EncoderDataJNI_findForChannel(int channel) {
    return HalRuntime.findEncoderForChannel(channel);
  }

  public static int DigitalPWMDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dpwm.init." + index,
        cb,
        initial,
        HALValue.makeBoolean(DigitalPWMDataJNI_getInitialized(index)));
  }

  public static void DigitalPWMDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DigitalPWMDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.DPWMS.length && HalRuntime.DPWMS[index].initialized;
  }

  public static void DigitalPWMDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.DPWMS.length) {
      HalRuntime.DPWMS[index].initialized = value;
    }
  }

  public static int DigitalPWMDataJNI_registerDutyCycleCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dpwm.duty." + index,
        cb,
        initial,
        HALValue.makeDouble(DigitalPWMDataJNI_getDutyCycle(index)));
  }

  public static void DigitalPWMDataJNI_cancelDutyCycleCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double DigitalPWMDataJNI_getDutyCycle(int index) {
    return index >= 0 && index < HalRuntime.DPWMS.length ? HalRuntime.DPWMS[index].duty : 0;
  }

  public static void DigitalPWMDataJNI_setDutyCycle(int index, double value) {
    if (index >= 0 && index < HalRuntime.DPWMS.length) {
      HalRuntime.DPWMS[index].duty = value;
    }
  }

  public static int DigitalPWMDataJNI_registerPinCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "dpwm.pin." + index, cb, initial, HALValue.makeInt(DigitalPWMDataJNI_getPin(index)));
  }

  public static void DigitalPWMDataJNI_cancelPinCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int DigitalPWMDataJNI_getPin(int index) {
    return index >= 0 && index < HalRuntime.DPWMS.length ? HalRuntime.DPWMS[index].pin : -1;
  }

  public static void DigitalPWMDataJNI_setPin(int index, int pin) {
    if (index >= 0 && index < HalRuntime.DPWMS.length) {
      HalRuntime.DPWMS[index].pin = pin;
    }
  }

  public static void DigitalPWMDataJNI_resetData(int index) {
    DigitalPWMDataJNI_setInitialized(index, false);
  }

  public static int DigitalPWMDataJNI_findForChannel(int channel) {
    return HalRuntime.findDigitalPwmForChannel(channel);
  }

  public static int CTREPCMDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(CTREPCMDataJNI_getInitialized(index)));
  }

  public static void CTREPCMDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean CTREPCMDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.PCMS.length && HalRuntime.PCMS[index].initialized;
  }

  public static void CTREPCMDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PCMS.length) {
      HalRuntime.PCMS[index].initialized = value;
    }
  }

  public static int CTREPCMDataJNI_registerSolenoidOutputCallback(
      int index, int channel, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.solenoid." + index + "." + channel,
        cb,
        initial,
        HALValue.makeBoolean(CTREPCMDataJNI_getSolenoidOutput(index, channel)));
  }

  public static void CTREPCMDataJNI_cancelSolenoidOutputCallback(int index, int channel, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean CTREPCMDataJNI_getSolenoidOutput(int index, int channel) {
    return index >= 0
        && index < HalRuntime.PCMS.length
        && channel >= 0
        && channel < HalRuntime.PCMS[index].solenoids.length
        && HalRuntime.PCMS[index].solenoids[channel];
  }

  public static void CTREPCMDataJNI_setSolenoidOutput(int index, int channel, boolean value) {
    if (index >= 0
        && index < HalRuntime.PCMS.length
        && channel >= 0
        && channel < HalRuntime.PCMS[index].solenoids.length) {
      HalRuntime.PCMS[index].solenoids[channel] = value;
    }
  }

  public static int CTREPCMDataJNI_registerCompressorOnCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.compressor." + index,
        cb,
        initial,
        HALValue.makeBoolean(CTREPCMDataJNI_getCompressorOn(index)));
  }

  public static void CTREPCMDataJNI_cancelCompressorOnCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean CTREPCMDataJNI_getCompressorOn(int index) {
    return index >= 0 && index < HalRuntime.PCMS.length && HalRuntime.PCMS[index].compressorOn;
  }

  public static void CTREPCMDataJNI_setCompressorOn(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PCMS.length) {
      HalRuntime.PCMS[index].compressorOn = value;
    }
  }

  public static int CTREPCMDataJNI_registerClosedLoopEnabledCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.closedLoop." + index,
        cb,
        initial,
        HALValue.makeBoolean(CTREPCMDataJNI_getClosedLoopEnabled(index)));
  }

  public static void CTREPCMDataJNI_cancelClosedLoopEnabledCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean CTREPCMDataJNI_getClosedLoopEnabled(int index) {
    return index >= 0 && index < HalRuntime.PCMS.length && HalRuntime.PCMS[index].closedLoop;
  }

  public static void CTREPCMDataJNI_setClosedLoopEnabled(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PCMS.length) {
      HalRuntime.PCMS[index].closedLoop = value;
    }
  }

  public static int CTREPCMDataJNI_registerPressureSwitchCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.pressure." + index,
        cb,
        initial,
        HALValue.makeBoolean(CTREPCMDataJNI_getPressureSwitch(index)));
  }

  public static void CTREPCMDataJNI_cancelPressureSwitchCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean CTREPCMDataJNI_getPressureSwitch(int index) {
    return index >= 0 && index < HalRuntime.PCMS.length && HalRuntime.PCMS[index].pressureSwitch;
  }

  public static void CTREPCMDataJNI_setPressureSwitch(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PCMS.length) {
      HalRuntime.PCMS[index].pressureSwitch = value;
    }
  }

  public static int CTREPCMDataJNI_registerCompressorCurrentCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pcm.current." + index,
        cb,
        initial,
        HALValue.makeDouble(CTREPCMDataJNI_getCompressorCurrent(index)));
  }

  public static void CTREPCMDataJNI_cancelCompressorCurrentCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double CTREPCMDataJNI_getCompressorCurrent(int index) {
    return index >= 0 && index < HalRuntime.PCMS.length ? HalRuntime.PCMS[index].current : 0;
  }

  public static void CTREPCMDataJNI_setCompressorCurrent(int index, double value) {
    if (index >= 0 && index < HalRuntime.PCMS.length) {
      HalRuntime.PCMS[index].current = value;
    }
  }

  public static void CTREPCMDataJNI_registerAllNonSolenoidCallbacks(
      int index, NotifyCallback cb, boolean initial) {
    CTREPCMDataJNI_registerInitializedCallback(index, cb, initial);
    CTREPCMDataJNI_registerCompressorOnCallback(index, cb, initial);
    CTREPCMDataJNI_registerClosedLoopEnabledCallback(index, cb, initial);
    CTREPCMDataJNI_registerPressureSwitchCallback(index, cb, initial);
    CTREPCMDataJNI_registerCompressorCurrentCallback(index, cb, initial);
  }

  public static void CTREPCMDataJNI_registerAllSolenoidCallbacks(
      int index, int channel, NotifyCallback cb, boolean initial) {
    CTREPCMDataJNI_registerSolenoidOutputCallback(index, channel, cb, initial);
  }

  public static void CTREPCMDataJNI_resetData(int index) {
    CTREPCMDataJNI_setInitialized(index, false);
    CTREPCMDataJNI_setCompressorOn(index, false);
  }

  public static int REVPHDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "ph.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(REVPHDataJNI_getInitialized(index)));
  }

  public static void REVPHDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean REVPHDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.PHS.length && HalRuntime.PHS[index].initialized;
  }

  public static void REVPHDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PHS.length) {
      HalRuntime.PHS[index].initialized = value;
    }
  }

  public static int REVPHDataJNI_registerSolenoidOutputCallback(
      int index, int channel, NotifyCallback cb, boolean initial) {
    return register(
        "ph.solenoid." + index + "." + channel,
        cb,
        initial,
        HALValue.makeBoolean(REVPHDataJNI_getSolenoidOutput(index, channel)));
  }

  public static void REVPHDataJNI_cancelSolenoidOutputCallback(int index, int channel, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean REVPHDataJNI_getSolenoidOutput(int index, int channel) {
    return index >= 0
        && index < HalRuntime.PHS.length
        && channel >= 0
        && channel < HalRuntime.PHS[index].solenoids.length
        && HalRuntime.PHS[index].solenoids[channel];
  }

  public static void REVPHDataJNI_setSolenoidOutput(int index, int channel, boolean value) {
    if (index >= 0
        && index < HalRuntime.PHS.length
        && channel >= 0
        && channel < HalRuntime.PHS[index].solenoids.length) {
      HalRuntime.PHS[index].solenoids[channel] = value;
    }
  }

  public static int REVPHDataJNI_registerCompressorOnCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "ph.compressor." + index,
        cb,
        initial,
        HALValue.makeBoolean(REVPHDataJNI_getCompressorOn(index)));
  }

  public static void REVPHDataJNI_cancelCompressorOnCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean REVPHDataJNI_getCompressorOn(int index) {
    return index >= 0 && index < HalRuntime.PHS.length && HalRuntime.PHS[index].compressorOn;
  }

  public static void REVPHDataJNI_setCompressorOn(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PHS.length) {
      HalRuntime.PHS[index].compressorOn = value;
    }
  }

  public static int REVPHDataJNI_registerCompressorConfigTypeCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "ph.config." + index,
        cb,
        initial,
        HALValue.makeInt(REVPHDataJNI_getCompressorConfigType(index)));
  }

  public static void REVPHDataJNI_cancelCompressorConfigTypeCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int REVPHDataJNI_getCompressorConfigType(int index) {
    return index >= 0 && index < HalRuntime.PHS.length ? HalRuntime.PHS[index].configType : 0;
  }

  public static void REVPHDataJNI_setCompressorConfigType(int index, int value) {
    if (index >= 0 && index < HalRuntime.PHS.length) {
      HalRuntime.PHS[index].configType = value;
    }
  }

  public static int REVPHDataJNI_registerPressureSwitchCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "ph.pressure." + index,
        cb,
        initial,
        HALValue.makeBoolean(REVPHDataJNI_getPressureSwitch(index)));
  }

  public static void REVPHDataJNI_cancelPressureSwitchCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean REVPHDataJNI_getPressureSwitch(int index) {
    return index >= 0 && index < HalRuntime.PHS.length && HalRuntime.PHS[index].pressureSwitch;
  }

  public static void REVPHDataJNI_setPressureSwitch(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PHS.length) {
      HalRuntime.PHS[index].pressureSwitch = value;
    }
  }

  public static int REVPHDataJNI_registerCompressorCurrentCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "ph.current." + index,
        cb,
        initial,
        HALValue.makeDouble(REVPHDataJNI_getCompressorCurrent(index)));
  }

  public static void REVPHDataJNI_cancelCompressorCurrentCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double REVPHDataJNI_getCompressorCurrent(int index) {
    return index >= 0 && index < HalRuntime.PHS.length ? HalRuntime.PHS[index].current : 0;
  }

  public static void REVPHDataJNI_setCompressorCurrent(int index, double value) {
    if (index >= 0 && index < HalRuntime.PHS.length) {
      HalRuntime.PHS[index].current = value;
    }
  }

  public static void REVPHDataJNI_registerAllNonSolenoidCallbacks(
      int index, NotifyCallback cb, boolean initial) {
    REVPHDataJNI_registerInitializedCallback(index, cb, initial);
    REVPHDataJNI_registerCompressorOnCallback(index, cb, initial);
    REVPHDataJNI_registerCompressorConfigTypeCallback(index, cb, initial);
    REVPHDataJNI_registerPressureSwitchCallback(index, cb, initial);
    REVPHDataJNI_registerCompressorCurrentCallback(index, cb, initial);
  }

  public static void REVPHDataJNI_registerAllSolenoidCallbacks(
      int index, int channel, NotifyCallback cb, boolean initial) {
    REVPHDataJNI_registerSolenoidOutputCallback(index, channel, cb, initial);
  }

  public static void REVPHDataJNI_resetData(int index) {
    REVPHDataJNI_setInitialized(index, false);
  }

  public static int PowerDistributionDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pd.initialized." + index,
        cb,
        initial,
        HALValue.makeBoolean(PowerDistributionDataJNI_getInitialized(index)));
  }

  public static void PowerDistributionDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean PowerDistributionDataJNI_getInitialized(int index) {
    return index >= 0 && index < HalRuntime.PDS.length && HalRuntime.PDS[index].initialized;
  }

  public static void PowerDistributionDataJNI_setInitialized(int index, boolean value) {
    if (index >= 0 && index < HalRuntime.PDS.length) {
      HalRuntime.PDS[index].initialized = value;
    }
  }

  public static int PowerDistributionDataJNI_registerTemperatureCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pd.temp." + index,
        cb,
        initial,
        HALValue.makeDouble(PowerDistributionDataJNI_getTemperature(index)));
  }

  public static void PowerDistributionDataJNI_cancelTemperatureCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double PowerDistributionDataJNI_getTemperature(int index) {
    return index >= 0 && index < HalRuntime.PDS.length ? HalRuntime.PDS[index].temperature : 0;
  }

  public static void PowerDistributionDataJNI_setTemperature(int index, double value) {
    if (index >= 0 && index < HalRuntime.PDS.length) {
      HalRuntime.PDS[index].temperature = value;
    }
  }

  public static int PowerDistributionDataJNI_registerVoltageCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register(
        "pd.voltage." + index,
        cb,
        initial,
        HALValue.makeDouble(PowerDistributionDataJNI_getVoltage(index)));
  }

  public static void PowerDistributionDataJNI_cancelVoltageCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double PowerDistributionDataJNI_getVoltage(int index) {
    return index >= 0 && index < HalRuntime.PDS.length ? HalRuntime.PDS[index].voltage : 0;
  }

  public static void PowerDistributionDataJNI_setVoltage(int index, double value) {
    if (index >= 0 && index < HalRuntime.PDS.length) {
      HalRuntime.PDS[index].voltage = value;
      HalRuntime.Callbacks.fire("pd.voltage." + index, HALValue.makeDouble(value));
    }
  }

  public static int PowerDistributionDataJNI_registerCurrentCallback(
      int index, int channel, NotifyCallback cb, boolean initial) {
    return register(
        "pd.current." + index + "." + channel,
        cb,
        initial,
        HALValue.makeDouble(PowerDistributionDataJNI_getCurrent(index, channel)));
  }

  public static void PowerDistributionDataJNI_cancelCurrentCallback(
      int index, int channel, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double PowerDistributionDataJNI_getCurrent(int index, int channel) {
    if (index < 0
        || index >= HalRuntime.PDS.length
        || channel < 0
        || channel >= HalRuntime.PDS[index].current.length) {
      return 0;
    }
    return HalRuntime.PDS[index].current[channel];
  }

  public static void PowerDistributionDataJNI_setCurrent(int index, int channel, double value) {
    if (index >= 0
        && index < HalRuntime.PDS.length
        && channel >= 0
        && channel < HalRuntime.PDS[index].current.length) {
      HalRuntime.PDS[index].current[channel] = value;
      HalRuntime.Callbacks.fire("pd.current." + index + "." + channel, HALValue.makeDouble(value));
    }
  }

  public static void PowerDistributionDataJNI_resetData(int index) {
    PowerDistributionDataJNI_setInitialized(index, false);
  }

  public static int I2CDataJNI_registerInitializedCallback(
      int index, NotifyCallback cb, boolean initial) {
    return register("i2c.initialized." + index, cb, initial, HALValue.makeBoolean(false));
  }

  public static void I2CDataJNI_cancelInitializedCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean I2CDataJNI_getInitialized(int index) {
    return false;
  }

  public static void I2CDataJNI_setInitialized(int index, boolean value) {}

  public static int I2CDataJNI_registerReadCallback(int index, BufferCallback cb) {
    return HalRuntime.Callbacks.registerBuffer("i2c.read." + index, cb);
  }

  public static void I2CDataJNI_cancelReadCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int I2CDataJNI_registerWriteCallback(int index, ConstBufferCallback cb) {
    return HalRuntime.Callbacks.registerConstBuffer("i2c.write." + index, cb);
  }

  public static void I2CDataJNI_cancelWriteCallback(int index, int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static void I2CDataJNI_resetData(int index) {}

  public static void IMUDataJNI_setAngleX(double value) {
    HalRuntime.IMU_ANGLE[0] = value;
  }

  public static void IMUDataJNI_setAngleY(double value) {
    HalRuntime.IMU_ANGLE[1] = value;
  }

  public static void IMUDataJNI_setAngleZ(double value) {
    HalRuntime.IMU_ANGLE[2] = value;
  }

  public static void IMUDataJNI_setGyroRateX(double value) {
    HalRuntime.IMU_RATE[0] = value;
  }

  public static void IMUDataJNI_setGyroRateY(double value) {
    HalRuntime.IMU_RATE[1] = value;
  }

  public static void IMUDataJNI_setGyroRateZ(double value) {
    HalRuntime.IMU_RATE[2] = value;
  }

  public static void IMUDataJNI_setAccelX(double value) {
    HalRuntime.IMU_ACCEL[0] = value;
  }

  public static void IMUDataJNI_setAccelY(double value) {
    HalRuntime.IMU_ACCEL[1] = value;
  }

  public static void IMUDataJNI_setAccelZ(double value) {
    HalRuntime.IMU_ACCEL[2] = value;
  }

  public static void IMUDataJNI_setYaw(double value) {
    HalRuntime.imuYaw = value;
  }

  private static int dsRegister(String key, NotifyCallback cb, boolean initial, HALValue value) {
    return register(key, cb, initial, value);
  }

  public static int DriverStationDataJNI_registerEnabledCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.enabled", cb, initial, HALValue.makeBoolean(HalRuntime.DS.enabled));
  }

  public static void DriverStationDataJNI_cancelEnabledCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DriverStationDataJNI_getEnabled() {
    return HalRuntime.DS.enabled;
  }

  public static void DriverStationDataJNI_setEnabled(boolean value) {
    HalRuntime.DS.enabled = value;
    HalRuntime.Callbacks.fire("ds.enabled", HALValue.makeBoolean(value));
  }

  public static int DriverStationDataJNI_registerRobotModeCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.mode", cb, initial, HALValue.makeEnum(HalRuntime.DS.robotMode));
  }

  public static void DriverStationDataJNI_cancelRobotModeCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int DriverStationDataJNI_nativeGetRobotMode() {
    return HalRuntime.DS.robotMode;
  }

  public static void DriverStationDataJNI_nativeSetRobotMode(int mode) {
    HalRuntime.DS.robotMode = mode;
    HalRuntime.Callbacks.fire("ds.mode", HALValue.makeEnum(mode));
  }

  public static int DriverStationDataJNI_registerEStopCallback(NotifyCallback cb, boolean initial) {
    return dsRegister("ds.estop", cb, initial, HALValue.makeBoolean(HalRuntime.DS.eStop));
  }

  public static void DriverStationDataJNI_cancelEStopCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DriverStationDataJNI_getEStop() {
    return HalRuntime.DS.eStop;
  }

  public static void DriverStationDataJNI_setEStop(boolean value) {
    HalRuntime.DS.eStop = value;
    HalRuntime.Callbacks.fire("ds.estop", HALValue.makeBoolean(value));
  }

  public static int DriverStationDataJNI_registerFmsAttachedCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.fms", cb, initial, HALValue.makeBoolean(HalRuntime.DS.fmsAttached));
  }

  public static void DriverStationDataJNI_cancelFmsAttachedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DriverStationDataJNI_getFmsAttached() {
    return HalRuntime.DS.fmsAttached;
  }

  public static void DriverStationDataJNI_setFmsAttached(boolean value) {
    HalRuntime.DS.fmsAttached = value;
  }

  public static int DriverStationDataJNI_registerDsAttachedCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.attached", cb, initial, HALValue.makeBoolean(HalRuntime.DS.dsAttached));
  }

  public static void DriverStationDataJNI_cancelDsAttachedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean DriverStationDataJNI_getDsAttached() {
    return HalRuntime.DS.dsAttached;
  }

  public static void DriverStationDataJNI_setDsAttached(boolean value) {
    HalRuntime.DS.dsAttached = value;
    HalRuntime.Callbacks.fire("ds.attached", HALValue.makeBoolean(value));
  }

  public static int DriverStationDataJNI_registerAllianceStationIdCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.alliance", cb, initial, HALValue.makeEnum(HalRuntime.DS.alliance));
  }

  public static void DriverStationDataJNI_cancelAllianceStationIdCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int DriverStationDataJNI_getAllianceStationId() {
    return HalRuntime.DS.alliance;
  }

  public static void DriverStationDataJNI_setAllianceStationId(int value) {
    HalRuntime.DS.alliance = value;
    HalRuntime.Callbacks.fire("ds.alliance", HALValue.makeEnum(value));
  }

  public static int DriverStationDataJNI_registerMatchTimeCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.matchTime", cb, initial, HALValue.makeDouble(HalRuntime.DS.matchTime));
  }

  public static void DriverStationDataJNI_cancelMatchTimeCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double DriverStationDataJNI_getMatchTime() {
    return HalRuntime.DS.matchTime;
  }

  public static void DriverStationDataJNI_setMatchTime(double value) {
    HalRuntime.DS.matchTime = value;
    HalRuntime.Callbacks.fire("ds.matchTime", HALValue.makeDouble(value));
  }

  public static int DriverStationDataJNI_registerOpModeCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("ds.opMode", cb, initial, HALValue.makeLong(HalRuntime.DS.opMode));
  }

  public static void DriverStationDataJNI_cancelOpModeCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static long DriverStationDataJNI_getOpMode() {
    return HalRuntime.DS.opMode;
  }

  public static void DriverStationDataJNI_setOpMode(long value) {
    HalRuntime.DS.opMode = value;
  }

  public static int DriverStationDataJNI_registerOpModeOptionsCallback(
      BiConsumer<String, OpModeOption[]> cb, boolean initial) {
    int uid = HalRuntime.Callbacks.registerOptions("ds.options", cb);
    if (initial && cb != null) {
      cb.accept("opModeOptions", HalRuntime.DS.opModeOptions);
    }
    return uid;
  }

  public static void DriverStationDataJNI_cancelOpModeOptionsCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static OpModeOption[] DriverStationDataJNI_getOpModeOptions() {
    return HalRuntime.DS.opModeOptions.clone();
  }

  public static void DriverStationDataJNI_setJoystickAxes(
      byte stick, float[] axes, short available) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS) {
      return;
    }
    HalRuntime.Joystick joystick = HalRuntime.DS.joysticks[stick];
    if (axes != null) {
      System.arraycopy(axes, 0, joystick.axes, 0, Math.min(axes.length, HalRuntime.MAX_AXES));
    }
    joystick.axisCount = available;
  }

  public static void DriverStationDataJNI_setJoystickPOVs(
      byte stick, byte[] povs, short available) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || povs == null) {
      return;
    }
    HalRuntime.Joystick joystick = HalRuntime.DS.joysticks[stick];
    System.arraycopy(povs, 0, joystick.povs, 0, Math.min(povs.length, HalRuntime.MAX_POVS));
    joystick.povCount = available;
  }

  public static void DriverStationDataJNI_setJoystickButtons(
      byte stick, long buttons, long available) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS) {
      return;
    }
    HalRuntime.DS.joysticks[stick].buttons = buttons;
    HalRuntime.DS.joysticks[stick].buttonCount = (int) available;
  }

  public static int DriverStationDataJNI_getJoystickLeds(int stick) {
    return stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS ? HalRuntime.DS.joysticks[stick].leds : 0;
  }

  public static int DriverStationDataJNI_getJoystickRumble(int stick, int rumble) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || rumble < 0 || rumble >= 4) {
      return 0;
    }
    return HalRuntime.DS.joysticks[stick].rumble[rumble];
  }

  public static void DriverStationDataJNI_setMatchInfo(
      String event, int match, int replay, int type) {
    HalRuntime.DS.eventName = event == null ? "" : event;
    HalRuntime.DS.matchNumber = match;
    HalRuntime.DS.replayNumber = replay;
    HalRuntime.DS.matchType = type;
  }

  public static void DriverStationDataJNI_setGameData(String data) {
    HalRuntime.DS.gameData = data == null ? "" : data;
  }

  public static void DriverStationDataJNI_registerAllCallbacks(NotifyCallback cb, boolean initial) {
    DriverStationDataJNI_registerEnabledCallback(cb, initial);
    DriverStationDataJNI_registerRobotModeCallback(cb, initial);
    DriverStationDataJNI_registerEStopCallback(cb, initial);
    DriverStationDataJNI_registerDsAttachedCallback(cb, initial);
    DriverStationDataJNI_registerAllianceStationIdCallback(cb, initial);
    DriverStationDataJNI_registerMatchTimeCallback(cb, initial);
  }

  public static void DriverStationDataJNI_notifyNewData() {
    HalRuntime.notifyNewData();
  }

  public static void DriverStationDataJNI_setSendError(boolean value) {
    HalRuntime.DS.sendError = value;
  }

  public static void DriverStationDataJNI_setSendConsoleLine(boolean value) {
    HalRuntime.DS.sendConsole = value;
  }

  public static void DriverStationDataJNI_setJoystickButton(int stick, int button, boolean state) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || button < 0 || button >= 64) {
      return;
    }
    long mask = 1L << button;
    if (state) {
      HalRuntime.DS.joysticks[stick].buttons |= mask;
    } else {
      HalRuntime.DS.joysticks[stick].buttons &= ~mask;
    }
  }

  public static void DriverStationDataJNI_setJoystickAxis(int stick, int axis, double value) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || axis < 0 || axis >= HalRuntime.MAX_AXES) {
      return;
    }
    HalRuntime.DS.joysticks[stick].axes[axis] = (float) value;
  }

  public static void DriverStationDataJNI_setJoystickPOV(int stick, int pov, byte value) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || pov < 0 || pov >= HalRuntime.MAX_POVS) {
      return;
    }
    HalRuntime.DS.joysticks[stick].povs[pov] = value;
  }

  public static void DriverStationDataJNI_setJoystickButtonsValue(int stick, long buttons) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].buttons = buttons;
    }
  }

  public static void DriverStationDataJNI_setJoystickAxesAvailable(int stick, int available) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].axisCount = available;
    }
  }

  public static void DriverStationDataJNI_setJoystickPOVsAvailable(int stick, int available) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].povCount = available;
    }
  }

  public static void DriverStationDataJNI_setJoystickButtonsAvailable(int stick, long available) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].buttonCount = (int) available;
    }
  }

  public static void DriverStationDataJNI_setTouchpadCounts(int stick, int count, int[] fingers) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS) {
      return;
    }
    HalRuntime.DS.joysticks[stick].touchpadCount = count;
    if (fingers != null) {
      System.arraycopy(
          fingers,
          0,
          HalRuntime.DS.joysticks[stick].fingerCounts,
          0,
          Math.min(fingers.length, HalRuntime.MAX_TOUCHPADS));
    }
  }

  public static void DriverStationDataJNI_setTouchpadFinger(
      int stick, int pad, int finger, boolean down, float x, float y) {
    if (stick < 0 || stick >= HalRuntime.NUM_JOYSTICKS || pad < 0 || finger < 0) {
      return;
    }
    int index = pad * HalRuntime.MAX_FINGERS + finger;
    HalRuntime.Joystick joystick = HalRuntime.DS.joysticks[stick];
    if (index < joystick.fingerDown.length) {
      joystick.fingerDown[index] = down;
      joystick.fingers[index * 2] = x;
      joystick.fingers[index * 2 + 1] = y;
    }
  }

  public static void DriverStationDataJNI_setJoystickIsGamepad(int stick, boolean value) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].gamepad = value;
    }
  }

  public static void DriverStationDataJNI_setJoystickGamepadType(int stick, int type) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].gamepadType = type;
    }
  }

  public static void DriverStationDataJNI_setJoystickName(int stick, String name) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].name = name == null ? "" : name;
    }
  }

  public static void DriverStationDataJNI_setJoystickSupportedOutputs(int stick, int outputs) {
    if (stick >= 0 && stick < HalRuntime.NUM_JOYSTICKS) {
      HalRuntime.DS.joysticks[stick].supportedOutputs = outputs;
    }
  }

  public static void DriverStationDataJNI_setEventName(String name) {
    HalRuntime.DS.eventName = name == null ? "" : name;
  }

  public static void DriverStationDataJNI_setMatchType(int type) {
    HalRuntime.DS.matchType = type;
  }

  public static void DriverStationDataJNI_setMatchNumber(int number) {
    HalRuntime.DS.matchNumber = number;
  }

  public static void DriverStationDataJNI_setReplayNumber(int number) {
    HalRuntime.DS.replayNumber = number;
  }

  public static void DriverStationDataJNI_resetData() {
    HalRuntime.DS.enabled = false;
    HalRuntime.DS.dsAttached = false;
    HalRuntime.DS.eStop = false;
    HalRuntime.DS.robotMode = 0;
    HalRuntime.DS.matchTime = -1;
  }

  public static int RoboRioDataJNI_registerVInVoltageCallback(NotifyCallback cb, boolean initial) {
    return dsRegister("rio.vin", cb, initial, HALValue.makeDouble(HalRuntime.RIO.vinVoltage));
  }

  public static void RoboRioDataJNI_cancelVInVoltageCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getVInVoltage() {
    return HalRuntime.RIO.vinVoltage;
  }

  public static void RoboRioDataJNI_setVInVoltage(double value) {
    HalRuntime.RIO.vinVoltage = value;
    HalRuntime.Callbacks.fire("rio.vin", HALValue.makeDouble(value));
  }

  public static int RoboRioDataJNI_registerUserVoltage3V3Callback(
      NotifyCallback cb, boolean initial) {
    return dsRegister("rio.3v3", cb, initial, HALValue.makeDouble(HalRuntime.RIO.userVoltage3V3));
  }

  public static void RoboRioDataJNI_cancelUserVoltage3V3Callback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getUserVoltage3V3() {
    return HalRuntime.RIO.userVoltage3V3;
  }

  public static void RoboRioDataJNI_setUserVoltage3V3(double value) {
    HalRuntime.RIO.userVoltage3V3 = value;
  }

  public static int RoboRioDataJNI_registerUserCurrent3V3Callback(
      NotifyCallback cb, boolean initial) {
    return dsRegister(
        "rio.current3v3", cb, initial, HALValue.makeDouble(HalRuntime.RIO.userCurrent3V3));
  }

  public static void RoboRioDataJNI_cancelUserCurrent3V3Callback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getUserCurrent3V3() {
    return HalRuntime.RIO.userCurrent3V3;
  }

  public static void RoboRioDataJNI_setUserCurrent3V3(double value) {
    HalRuntime.RIO.userCurrent3V3 = value;
  }

  public static int RoboRioDataJNI_registerUserActive3V3Callback(
      NotifyCallback cb, boolean initial) {
    return dsRegister(
        "rio.userActive3V3", cb, initial, HALValue.makeBoolean(HalRuntime.RIO.userActive3V3));
  }

  public static void RoboRioDataJNI_cancelUserActive3V3Callback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static boolean RoboRioDataJNI_getUserActive3V3() {
    return HalRuntime.RIO.userActive3V3;
  }

  public static void RoboRioDataJNI_setUserActive3V3(boolean value) {
    HalRuntime.RIO.userActive3V3 = value;
  }

  public static int RoboRioDataJNI_registerUserFaults3V3Callback(
      NotifyCallback cb, boolean initial) {
    return dsRegister(
        "rio.userFaults3V3", cb, initial, HALValue.makeInt(HalRuntime.RIO.userFaults3V3));
  }

  public static void RoboRioDataJNI_cancelUserFaults3V3Callback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int RoboRioDataJNI_getUserFaults3V3() {
    return HalRuntime.RIO.userFaults3V3;
  }

  public static void RoboRioDataJNI_setUserFaults3V3(int value) {
    HalRuntime.RIO.userFaults3V3 = value;
  }

  public static int RoboRioDataJNI_registerBrownoutVoltageCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister(
        "brownoutVoltage", cb, initial, HALValue.makeDouble(HalRuntime.RIO.brownoutVoltage));
  }

  public static void RoboRioDataJNI_cancelBrownoutVoltageCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getBrownoutVoltage() {
    return HalRuntime.RIO.brownoutVoltage;
  }

  public static void RoboRioDataJNI_setBrownoutVoltage(double value) {
    HalRuntime.RIO.brownoutVoltage = value;
  }

  public static int RoboRioDataJNI_registerBrownoutRecoveryVoltageCallback(
      NotifyCallback cb, boolean initial) {
    return dsRegister(
        "brownoutRecoveryVoltage",
        cb,
        initial,
        HALValue.makeDouble(HalRuntime.RIO.brownoutRecoveryVoltage));
  }

  public static void RoboRioDataJNI_cancelBrownoutRecoveryVoltageCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getBrownoutRecoveryVoltage() {
    return HalRuntime.RIO.brownoutRecoveryVoltage;
  }

  public static void RoboRioDataJNI_setBrownoutRecoveryVoltage(double value) {
    HalRuntime.RIO.brownoutRecoveryVoltage = value;
  }

  public static int RoboRioDataJNI_registerCPUTempCallback(NotifyCallback cb, boolean initial) {
    return dsRegister("rio.cpu", cb, initial, HALValue.makeDouble(HalRuntime.RIO.cpuTemp));
  }

  public static void RoboRioDataJNI_cancelCPUTempCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static double RoboRioDataJNI_getCPUTemp() {
    return HalRuntime.RIO.cpuTemp;
  }

  public static void RoboRioDataJNI_setCPUTemp(double value) {
    HalRuntime.RIO.cpuTemp = value;
  }

  public static int RoboRioDataJNI_registerTeamNumberCallback(NotifyCallback cb, boolean initial) {
    return dsRegister("rio.team", cb, initial, HALValue.makeInt(HalRuntime.RIO.teamNumber));
  }

  public static void RoboRioDataJNI_cancelTeamNumberCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int RoboRioDataJNI_getTeamNumber() {
    return HalRuntime.RIO.teamNumber;
  }

  public static void RoboRioDataJNI_setTeamNumber(int value) {
    HalRuntime.RIO.teamNumber = value;
  }

  public static String RoboRioDataJNI_getSerialNumber() {
    return HalRuntime.RIO.serialNumber;
  }

  public static void RoboRioDataJNI_setSerialNumber(String value) {
    HalRuntime.RIO.serialNumber = value == null ? "" : value;
  }

  public static String RoboRioDataJNI_getComments() {
    return HalRuntime.RIO.comments;
  }

  public static void RoboRioDataJNI_setComments(String value) {
    HalRuntime.RIO.comments = value == null ? "" : value;
  }

  public static void RoboRioDataJNI_resetData() {
    HalRuntime.RIO.vinVoltage = 12;
    HalRuntime.RIO.userVoltage3V3 = 3.3;
    HalRuntime.RIO.userActive3V3 = true;
    HalRuntime.RIO.userFaults3V3 = 0;
  }

  public static void SimDeviceDataJNI_setSimDeviceEnabled(String prefix, boolean enabled) {
    HalRuntime.SimDevices.setEnabled(prefix, enabled);
  }

  public static boolean SimDeviceDataJNI_isSimDeviceEnabled(String name) {
    return HalRuntime.SimDevices.enabled(name);
  }

  public static int SimDeviceDataJNI_registerSimDeviceCreatedCallback(
      String prefix, SimDeviceCallback cb, boolean initial) {
    return HalRuntime.Callbacks.registerDevice(prefix == null ? "" : prefix, cb);
  }

  public static void SimDeviceDataJNI_cancelSimDeviceCreatedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int SimDeviceDataJNI_registerSimDeviceFreedCallback(
      String prefix, SimDeviceCallback cb, boolean initial) {
    return HalRuntime.Callbacks.registerDevice(prefix == null ? "" : prefix, cb);
  }

  public static void SimDeviceDataJNI_cancelSimDeviceFreedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int SimDeviceDataJNI_getSimDeviceHandle(String name) {
    SimDeviceDataJNI.SimDeviceInfo[] infos = HalRuntime.SimDevices.enumerate(name);
    for (SimDeviceDataJNI.SimDeviceInfo info : infos) {
      if (info.name.equals(name)) {
        return info.handle;
      }
    }
    return 0;
  }

  public static String SimDeviceDataJNI_getSimDeviceName(int handle) {
    return HalRuntime.SimDevices.name(handle);
  }

  public static int SimDeviceDataJNI_getSimValueDeviceHandle(int handle) {
    return HalRuntime.SimDevices.deviceOfValue(handle);
  }

  public static SimDeviceDataJNI.SimDeviceInfo[] SimDeviceDataJNI_enumerateSimDevices(
      String prefix) {
    return HalRuntime.SimDevices.enumerate(prefix);
  }

  public static int SimDeviceDataJNI_registerSimValueCreatedCallback(
      int device, SimValueCallback cb, boolean initial) {
    return HalRuntime.Callbacks.registerValue("valueCreated." + device, cb);
  }

  public static void SimDeviceDataJNI_cancelSimValueCreatedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int SimDeviceDataJNI_registerSimValueChangedCallback(
      int handle, SimValueCallback cb, boolean initial) {
    return HalRuntime.Callbacks.registerValue("valueChanged." + handle, cb);
  }

  public static void SimDeviceDataJNI_cancelSimValueChangedCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int SimDeviceDataJNI_registerSimValueResetCallback(
      int handle, SimValueCallback cb, boolean initial) {
    return HalRuntime.Callbacks.registerValue("valueReset." + handle, cb);
  }

  public static void SimDeviceDataJNI_cancelSimValueResetCallback(int uid) {
    HalRuntime.Callbacks.cancel(uid);
  }

  public static int SimDeviceDataJNI_getSimValueHandle(int device, String name) {
    return HalRuntime.SimDevices.valueHandle(device, name);
  }

  public static SimDeviceDataJNI.SimValueInfo[] SimDeviceDataJNI_enumerateSimValues(int device) {
    return HalRuntime.SimDevices.enumerateValues(device);
  }

  public static String[] SimDeviceDataJNI_getSimValueEnumOptions(int handle) {
    return HalRuntime.SimDevices.enumOptions(handle);
  }

  public static double[] SimDeviceDataJNI_getSimValueEnumDoubleValues(int handle) {
    return HalRuntime.SimDevices.enumDoubles(handle);
  }

  public static void SimDeviceDataJNI_resetSimDeviceData() {
    HalRuntime.SimDevices.reset();
  }

  public static void WPIUtilJNI_checkMsvcRuntime() {}

  public static void WPIUtilJNI_writeStderr(String str) {
    if (str != null) {
      System.err.print(str);
    }
  }

  public static void WPIUtilJNI_enableMockTime() {
    HalRuntime.enableMockTime();
  }

  public static void WPIUtilJNI_disableMockTime() {
    HalRuntime.disableMockTime();
  }

  public static void WPIUtilJNI_setMockTime(long time) {
    HalRuntime.setMockTime(time);
  }

  public static long WPIUtilJNI_now() {
    return HalRuntime.now();
  }

  public static long WPIUtilJNI_getProgramStartTime() {
    return HalRuntime.getProgramStartTime();
  }

  public static long WPIUtilJNI_getSystemTime() {
    return HalRuntime.getSystemTime();
  }

  public static int WPIUtilJNI_createAlert(String group, String id, String text, int level) {
    return HalRuntime.createAlert(group, id, text, level);
  }

  public static void WPIUtilJNI_destroyAlert(int handle) {
    HalRuntime.destroyAlert(handle);
  }

  public static void WPIUtilJNI_setAlertActive(int handle, boolean active) {
    HalRuntime.setAlertActive(handle, active);
  }

  public static boolean WPIUtilJNI_isAlertActive(int handle) {
    return HalRuntime.isAlertActive(handle);
  }

  public static void WPIUtilJNI_setAlertText(int handle, String text) {
    HalRuntime.setAlertText(handle, text);
  }

  public static String WPIUtilJNI_getAlertText(int handle) {
    return HalRuntime.getAlertText(handle);
  }

  public static int WPIUtilJNI_makeEvent(boolean manual, boolean initial) {
    return HalRuntime.makeEvent(manual, initial);
  }

  public static void WPIUtilJNI_destroyEvent(int handle) {
    HalRuntime.destroyEvent(handle);
  }

  public static void WPIUtilJNI_setEvent(int handle) {
    HalRuntime.setEvent(handle);
  }

  public static void WPIUtilJNI_resetEvent(int handle) {
    HalRuntime.resetEvent(handle);
  }

  public static int WPIUtilJNI_makeSemaphore(int initial, int max) {
    return HalRuntime.makeSemaphore(initial, max);
  }

  public static void WPIUtilJNI_destroySemaphore(int handle) {
    HalRuntime.destroySemaphore(handle);
  }

  public static boolean WPIUtilJNI_releaseSemaphore(int handle, int count) {
    return HalRuntime.releaseSemaphore(handle, count);
  }

  public static long WPIUtilJNI_allocateRawFrame() {
    return HalRuntime.allocateRawFrame();
  }

  public static void WPIUtilJNI_freeRawFrame(long frame) {
    HalRuntime.freeRawFrame(frame);
  }

  public static long WPIUtilJNI_getRawFrameDataPtr(long frame) {
    return HalRuntime.getRawFrameDataPtr(frame);
  }

  public static void WPIUtilJNI_setRawFrameData(
      long frame, ByteBuffer data, int size, int width, int height, int stride, int format) {
    HalRuntime.Frame stored = HalRuntime.frame(frame);
    stored.data = data;
    stored.size = size;
    stored.width = width;
    stored.height = height;
    stored.stride = stride;
    stored.pixelFormat = format;
  }

  public static void WPIUtilJNI_setRawFrameInfo(
      long frame, int size, int width, int height, int stride, int format) {
    HalRuntime.Frame stored = HalRuntime.frame(frame);
    stored.size = size;
    stored.width = width;
    stored.height = height;
    stored.stride = stride;
    stored.pixelFormat = format;
  }

  public static void WPIUtilJNI_setRawFrameTime(long frame, long time, int source) {
    HalRuntime.Frame stored = HalRuntime.frame(frame);
    stored.timestamp = time;
    stored.timeSource = source;
  }

  public static void WPIUtilJNI_waitForObject(int handle) throws InterruptedException {
    boolean[] timed = new boolean[1];
    if (!HalRuntime.waitForObject(handle, -1, timed)) {
      throw new InterruptedException("WaitForObject interrupted");
    }
  }

  public static boolean WPIUtilJNI_waitForObjectTimeout(int handle, double timeout)
      throws InterruptedException {
    boolean[] timed = new boolean[1];
    boolean signaled = HalRuntime.waitForObject(handle, timeout, timed);
    if (!signaled && !timed[0]) {
      throw new InterruptedException("WaitForObject interrupted");
    }
    return timed[0];
  }

  public static int[] WPIUtilJNI_waitForObjects(int[] handles) throws InterruptedException {
    boolean[] timed = new boolean[1];
    int[] signaled = HalRuntime.waitForObjects(handles, -1, timed);
    if (signaled.length == 0) {
      throw new InterruptedException("WaitForObjects interrupted");
    }
    return signaled;
  }

  public static int[] WPIUtilJNI_waitForObjectsTimeout(int[] handles, double timeout)
      throws InterruptedException {
    boolean[] timed = new boolean[1];
    int[] signaled = HalRuntime.waitForObjects(handles, timeout, timed);
    if (signaled.length == 0 && !timed[0]) {
      throw new InterruptedException("WaitForObjects interrupted");
    }
    return signaled;
  }

  public static int AlertDataJNI_getNumAlerts() {
    return HalRuntime.getNumAlerts();
  }

  public static AlertDataJNI.AlertInfo[] AlertDataJNI_getAlerts() {
    return HalRuntime.getAlerts();
  }

  public static void AlertDataJNI_resetData() {
    HalRuntime.resetAlertData();
  }
}
