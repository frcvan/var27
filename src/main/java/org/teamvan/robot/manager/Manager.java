package org.teamvan.robot.manager;

import static org.teamvan.robot.util.Util.areaTrigger;

import org.teamvan.robot.mechanisms.drive.Drive;
import org.teamvan.robot.mechanisms.flag.Flag;
import org.wpilib.math.geometry.Translation2d;

public class Manager {
  private final Drive drive;
  private final Flag flag;

  public Manager(Drive drive, Flag flag) {
    this.drive = drive;
    this.flag = flag;

    bindTriggers();
  }

  private void bindTriggers() {
    System.out.println("Binding triggers");
    areaTrigger(drive::getPose, new Translation2d(1, 1), new Translation2d(0, 0))
        .whileTrue(flag.raise());
  }

  public void updateInputs() {
    drive.updateInputs();
  }

  public void updateOutputs() {
    drive.updateOutputs();
    flag.updateOutputs();
  }
}
