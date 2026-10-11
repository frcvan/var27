package org.teamvan.robot;

import org.teamvan.robot.util.Profiler;
import org.wpilib.command3.Scheduler;
import org.wpilib.framework.OpModeRobot;

public class Robot extends OpModeRobot {

  public Robot() {
    if (Boolean.getBoolean("debugMode")) {
      Profiler.start();
    }
  }

  @Override
  public void robotPeriodic() {
    Scheduler.getDefault().run();
  }
}
