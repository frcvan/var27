package org.teamvan.robot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.graalvm.webimage.api.JS;
import org.teamvan.robot.generated.TunerConstants;
import org.teamvan.robot.manager.Manager;
import org.teamvan.robot.mechanisms.drive.Drive;
import org.teamvan.robot.mechanisms.drive.GyroIO;
import org.teamvan.robot.mechanisms.drive.ModuleIOSim;
import org.teamvan.robot.mechanisms.flag.Flag;
import org.teamvan.robot.wasm.JsBridge;
import org.teamvan.robot.wasm.JsTelemetry;
import org.teamvan.robot.wasm.WasmRuntime;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.DriverStation;

/**
 * Wasm entry. Does not construct {@link Robot} or call {@code RobotBase.startRobot}.
 *
 * <p>The browser calls {@code wasmTick()} once per period. A blocking sleep cannot run on GitHub
 * Pages because it requires cross-origin isolation. {@link #run()} remains for a host that can
 * sleep between ticks.
 */
public final class WasmMain {
  /** Same period {@code OpModeRobot} uses. */
  public static final double PERIOD_SECONDS = 0.02;

  private static Manager manager;
  private static Scheduler scheduler;

  private WasmMain() {}

  /**
   * Builds the sim robot and exports {@code wasmTick}.
   *
   * <p>The host installs {@code wasmNowNanos}, {@code wasmDs}, {@code wasmNtGet}, and {@code
   * wasmNtPut} before calling {@code wasmStart()}.
   */
  public static void main(String... args) {
    exportStart();
  }

  @JS(
      args = {"start"},
      value = "globalThis.wasmStart = start;")
  private static native void exportStart(Runnable start);

  @JS(
      args = {"tick"},
      value = "globalThis.wasmTick = tick;")
  private static native void exportTick(Runnable tick);

  private static void exportStart() {
    exportStart(WasmMain::start);
    exportTick(WasmMain::tick);
  }

  @JS.Coerce
  @JS(
      args = {"n"},
      value = "console.log('MARK ' + n);")
  public static native void marker(int n);

  @JS.Coerce
  @JS(
      args = {"msg"},
      value = "console.log(msg);")
  private static native void log(String msg);

  /** Constructs the sim robot. */
  public static void start() {
    if (manager != null) {
      return;
    }
    try {
      build();
    } catch (Throwable t) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
      log(out.toString(StandardCharsets.UTF_8));
      throw new RuntimeException(t);
    }
  }

  private static void build() {
    marker(1);
    JsTelemetry.install();
    marker(2);
    WasmRuntime.refresh();
    marker(3);
    ModuleIOSim fl = new ModuleIOSim(TunerConstants.FrontLeft);
    ModuleIOSim fr = new ModuleIOSim(TunerConstants.FrontRight);
    ModuleIOSim bl = new ModuleIOSim(TunerConstants.BackLeft);
    ModuleIOSim br = new ModuleIOSim(TunerConstants.BackRight);
    marker(4);
    Drive drive = new Drive(new GyroIO() {}, fl, fr, bl, br);
    marker(5);
    marker(50);
    org.wpilib.driverstation.Gamepad gamepad = DriverStation.getGamepad(0);
    marker(51);
    org.wpilib.command3.Command command = drive.joystickDrive(gamepad);
    marker(52);
    drive.setDefaultCommand(command);
    marker(6);
    manager = new Manager(drive, new Flag());
    marker(7);
    scheduler = Scheduler.getDefault();
    marker(8);
  }

  /**
   * Runs one 20 ms cycle and publishes the pose as plain numbers. The host advances {@code
   * wasmNowNanos} by {@link #PERIOD_SECONDS} between calls.
   */
  public static void tick() {
    if (manager == null) {
      start();
    }
    WasmRuntime.refresh();
    manager.updateInputs();
    scheduler.run();
    manager.updateOutputs();
    publishPose();
  }

  private static void publishPose() {
    var pose = manager.pose();
    JsBridge.ntPut("/Sim/Pose/X", pose.getX());
    JsBridge.ntPut("/Sim/Pose/Y", pose.getY());
    JsBridge.ntPut("/Sim/Pose/Rotation", pose.getRotation().getRadians());
  }

  /** Loops until JavaScript sets {@code wasmStop}. Not used by the browser page. */
  public static void run() {
    start();
    while (!JsBridge.stopped()) {
      tick();
      sleepPeriod();
    }
  }

  @JS.Coerce
  @JS("Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 20);")
  private static native void sleepPeriod();
}
