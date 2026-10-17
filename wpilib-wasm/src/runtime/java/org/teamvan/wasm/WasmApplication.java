package org.teamvan.wasm;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.graalvm.webimage.api.JS;
import org.wpilib.command3.Scheduler;

/** JavaScript-hosted robot lifecycle using the real Commands V3 scheduler. */
public final class WasmApplication {
  /** Host clock advancement between ticks, in seconds. */
  public static final double PERIOD_SECONDS = 0.02;

  private final Supplier<? extends WasmRobot> factory;
  private WasmRobot robot;
  private Scheduler scheduler;

  private WasmApplication(Supplier<? extends WasmRobot> factory) {
    this.factory = factory;
  }

  /**
   * Exports {@code wasmStart} and the serialized asynchronous {@code wasmTick} entry point.
   *
   * <p>The host installs {@code wasmNowNanos}, {@code wasmDs}, {@code wasmNtGet}, and {@code
   * wasmNtPut} before calling {@code wasmStart()}. The factory runs once on successful startup,
   * after telemetry installation and the initial driver-station refresh.
   *
   * @param factory constructs the consumer's simulation adapter
   */
  public static void launch(Supplier<? extends WasmRobot> factory) {
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
    if (robot != null) {
      return;
    }
    try {
      JsTelemetry.install();
      WasmRuntime.refresh();
      robot = factory.get();
      scheduler = Scheduler.getDefault();
    } catch (Throwable t) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
      log(out.toString(StandardCharsets.UTF_8));
      throw new RuntimeException(t);
    }
  }

  /**
   * Runs one 20 ms cycle: refresh driver station, update inputs, run enabled commands or cancel all
   * disabled commands, then update outputs. The host advances its clock between calls.
   *
   * <p>This public instance method is the raw JSPI tick target registered in reflection metadata.
   */
  public void tick() {
    if (robot == null) {
      start();
    }
    WasmRuntime.refresh();
    robot.updateInputs();
    if (WasmRuntime.isEnabled()) {
      scheduler.run();
    } else {
      scheduler.cancelAll();
    }
    robot.updateOutputs();
  }
}
