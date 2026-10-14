package org.teamvan.robot.mechanisms.flag;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.util.Color8Bit;

/** Flag that raises while its command is running and lowers when that command is canceled. */
public class Flag implements Mechanism {
  /** Canvas units. 0° is +x (right); 90° is straight up. */
  private static final double POLE_LENGTH = 0.8;

  private static final double POLE_ANGLE_DEGREES = 90;
  private static final double RAISED_ANGLE_DEGREES = 0;
  private static final double LOWERED_ANGLE_DEGREES = -90;
  private static final double PENNANT_LENGTH = 0.35;

  private final Mechanism2d mechanism = new Mechanism2d(1, 1.2);
  private final MechanismLigament2d pennant;
  private boolean raised;

  public Flag() {
    var pole =
        mechanism
            .getRoot("Flag", 0.5, 0.1)
            .append(
                new MechanismLigament2d(
                    "Pole", POLE_LENGTH, POLE_ANGLE_DEGREES, 6, new Color8Bit(180, 180, 180)));
    pennant =
        pole.append(
            new MechanismLigament2d(
                "Pennant", PENNANT_LENGTH, LOWERED_ANGLE_DEGREES, 10, new Color8Bit(200, 30, 30)));
  }

  public boolean isRaised() {
    return raised;
  }

  /** Holds the flag up until canceled, then schedules {@link #lower()}. */
  public Command raise() {
    return run((co) -> {
          raised = true;
          System.out.println("Raising the flag!");
          co.park();
        })
        .whenCanceled(() -> Scheduler.getDefault().schedule(this.lower()))
        .named("Raise Flag");
  }

  /** Drops the flag. */
  public Command lower() {
    return run((co) -> {
          raised = false;
          System.out.println("Lowering the flag!");
        })
        .named("Lower Flag");
  }

  /** Publishes the flag Mechanism2d. Call after the scheduler so the pose matches the commands. */
  public void updateOutputs() {
    pennant.setAngle(raised ? RAISED_ANGLE_DEGREES : LOWERED_ANGLE_DEGREES);
    Telemetry.log("Flag/Mechanism", mechanism);
  }
}
