package org.teamvan.robot;

import org.wpilib.framework.RobotBase;

/** Web Image entry point using the same Robot class as the normal WPILib entry point. */
public final class WasmMain {
  private WasmMain() {}

  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
