package org.teamvan.robot.util;

import java.util.function.Supplier;
import org.wpilib.command3.Trigger;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Translation2d;

public class Util {
  public static Trigger areaTrigger(
      Supplier<Pose2d> supplier, Translation2d topLeft, Translation2d bottomRight) {
    return new Trigger(
        () -> {
          var pose = supplier.get();
          var inArea =
              pose.getX() <= topLeft.getX()
                  && pose.getX() >= bottomRight.getX()
                  && pose.getY() >= bottomRight.getY()
                  && pose.getY() <= topLeft.getY();
          return inArea;
        });
  }
}
