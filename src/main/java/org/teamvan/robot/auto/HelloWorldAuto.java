package org.teamvan.robot.auto;

import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.OpMode;

@Autonomous
public class HelloWorldAuto implements OpMode {
  @Override
  public void periodic() {
    System.out.println("Hello, World");
  }
}
