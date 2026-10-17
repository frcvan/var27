package example;

import org.teamvan.wasm.WasmApplication;
import org.teamvan.wasm.WasmRobot;
import org.teamvan.wasm.WasmRuntime;
import org.wpilib.telemetry.Telemetry;

/** The only WASM-specific application code needed by this example. */
public final class ExampleWasm implements WasmRobot {
  private final Counter counter = new Counter();
  private boolean enabled;

  public static void main(String... args) {
    WasmApplication.launch(ExampleWasm::new);
  }

  @Override
  public void updateInputs() {
    enabled = WasmRuntime.isEnabled();
  }

  @Override
  public void updateOutputs() {
    Telemetry.log("Example/Counter", counter.ticks());
    Telemetry.log("Example/Enabled", enabled);
  }
}
