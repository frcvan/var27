package org.teamvan.wasm;

/** Consumer lifecycle surrounding each simulated scheduler cycle. */
public interface WasmRobot {
  /** Reads simulated inputs before commands run. */
  void updateInputs();

  /** Applies simulated outputs after commands run or are canceled. */
  void updateOutputs();
}
