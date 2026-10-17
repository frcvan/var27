package org.teamvan.wasmbot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;
import org.graalvm.webimage.api.JS;
import org.wpilib.framework.RobotBase;

/** Nonblocking JavaScript-hosted launcher for ordinary WPILib robot programs. */
public final class WasmApplication {
  /** Host clock advancement between ticks, in seconds. */
  public static final double PERIOD_SECONDS = 0.02;

  private final Supplier<? extends RobotBase> factory;
  private RobotBase robot;
  private boolean started;
  private RuntimeException startupFailure;

  private WasmApplication(Supplier<? extends RobotBase> factory) {
    this.factory = Objects.requireNonNull(factory);
  }

  /**
   * Exports {@code wasmStart} and the serialized asynchronous {@code wasmTick} entry point.
   *
   * <p>The host installs {@code wasmNowNanos}, {@code wasmDs}, {@code wasmNtGet}, and {@code
   * wasmNtPut} before calling {@code wasmStart()}. The factory runs once, with the {@code wasmbot}
   * system property set to {@code true}, after telemetry installation and the initial driver-station
   * refresh. Framework startup initializes simulation without blocking on a native notifier.
   *
   * @param factory constructs the consumer's normal robot
   */
  public static void launch(Supplier<? extends RobotBase> factory) {
    WasmApplication application = new WasmApplication(factory);
    exportStart(application::start);
    exportTick(application);
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
  private static native void exportTick(WasmApplication tick);

  @JS.Coerce
  @JS(
      args = {"msg"},
      value = "console.log(msg);")
  private static native void log(String msg);

  private void start() {
    if (startupFailure != null) {
      throw startupFailure;
    }
    if (started) {
      return;
    }
    started = true;
    try {
      System.setProperty("wasmbot", "true");
      JsTelemetry.install();
      WasmRuntime.refresh();
      robot = Objects.requireNonNull(factory.get(), "Robot constructor returned null");
      robot.startCompetition();
    } catch (Throwable t) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
      log(out.toString(StandardCharsets.UTF_8));
      startupFailure = new RuntimeException(t);
      throw startupFailure;
    }
  }

  /**
   * Refreshes the driver station and delivers one host tick to the robot framework, including while
   * disabled. The robot's own {@code robotPeriodic()} owns any command-scheduler invocation.
   *
   * <p>This public instance method is the raw JSPI tick target registered in reflection metadata.
   */
  public void tick() {
    start();
    WasmRuntime.refresh();
    robot.wasmTick();
  }
}
