package van.robot.util;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Utility;

@Utility
public class HelloWorldUtil implements OpMode {
  @Override
  public void periodic() {
    System.out.println("Hello, World");
  }
}
