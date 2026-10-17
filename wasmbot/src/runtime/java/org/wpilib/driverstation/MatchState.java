package org.wpilib.driverstation;

import java.util.Optional;
import org.teamvan.wasmbot.WasmRuntime;

/** Wasm stand-in. Alliance comes from JavaScript, not the FMS. */
public final class MatchState {
  private MatchState() {}

  /**
   * @return the alliance from the latest JavaScript DriverStation snapshot
   */
  public static Optional<Alliance> getAlliance() {
    return WasmRuntime.getAlliance();
  }
}
