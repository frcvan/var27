package example;

import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.RobotState;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.telemetry.Telemetry;

/** The same hardware-free robot and scheduler lifecycle runs on desktop and in WASM. */
public final class Robot extends OpModeRobot {
  private final Counter counter = new Counter();

  @Override
  public void robotPeriodic() {
    if (RobotState.isEnabled()) {
      Scheduler.getDefault().run();
    } else {
      Scheduler.getDefault().cancelAll();
    }
    Telemetry.log("Example/Counter", counter.ticks());
    Telemetry.log("Example/Enabled", RobotState.isEnabled());
    Telemetry.log("Example/Wasmbot", Boolean.getBoolean("wasmbot"));
  }
}
