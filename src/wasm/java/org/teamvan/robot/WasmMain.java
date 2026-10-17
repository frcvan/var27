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
 * Wasm entry. Does not construct {@link Robot} or call {@code RobotBase.startRobot}. The browser
 * calls {@code wasmTick()} once per period.
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
      """
      const engine = globalThis.wasmContinuations;
      engine.bindTick(conversion.unproxy(tick));
      globalThis.wasmTick = async () => {
        try {
          await engine.tick();
        } catch (error) {
          if (error instanceof WebAssembly.Exception && error.is(getExport('tag.throwable'))) {
            throw conversion.javaToJavaScript(error);
          }
          throw error;
        }
      };
      """)
  private static native void exportTick(WasmMain tick);

  private static void exportStart() {
    exportStart(WasmMain::start);
    exportTick(new WasmMain());
  }

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
    JsTelemetry.install();
    WasmRuntime.refresh();
    ModuleIOSim fl = new ModuleIOSim(TunerConstants.FrontLeft);
    ModuleIOSim fr = new ModuleIOSim(TunerConstants.FrontRight);
    ModuleIOSim bl = new ModuleIOSim(TunerConstants.BackLeft);
    ModuleIOSim br = new ModuleIOSim(TunerConstants.BackRight);
    Drive drive = new Drive(new GyroIO() {}, fl, fr, bl, br);
    drive.setDefaultCommand(drive.joystickDrive(DriverStation.getGamepad(0)));
    manager = new Manager(drive, new Flag());
    scheduler = Scheduler.getDefault();
  }

  /**
   * Runs one 20 ms cycle and publishes the pose as plain numbers. The host advances {@code
   * wasmNowNanos} by {@link #PERIOD_SECONDS} between calls.
   */
  public void tick() {
    if (manager == null) {
      start();
    }
    WasmRuntime.refresh();
    manager.updateInputs();
    if (WasmRuntime.isEnabled()) {
      scheduler.run();
    } else {
      scheduler.cancelAll();
    }
    manager.updateOutputs();
    publishPose();
  }

  private static void publishPose() {
    var pose = manager.pose();
    JsBridge.ntPut("/Sim/Pose/X", pose.getX());
    JsBridge.ntPut("/Sim/Pose/Y", pose.getY());
    JsBridge.ntPut("/Sim/Pose/Rotation", pose.getRotation().getRadians());
  }
}
