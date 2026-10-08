package org.teamvan.robot.teleop;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

@Teleop
public class MainTeleop implements OpMode {
  @Override
  public void periodic() {
    System.out.println("Hello, World");
  }
}
