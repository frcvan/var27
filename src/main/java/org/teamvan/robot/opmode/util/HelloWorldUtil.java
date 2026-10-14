package org.teamvan.robot.opmode.util;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;

@Utility
public class HelloWorldUtil implements OpMode {

  @Override
  public void start() {}

  @Override
  public void periodic() {
    double res = 1.0;
    for (int i = 0; i < 100_000_000; i++) {
      res = res * (1.0 + Math.sin(i));
    }
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
