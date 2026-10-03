// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package van.robot.subsystems.drive;

import static van.robot.subsystems.drive.DriveConstants.*;

import org.wpilib.math.controller.PIDController;
import org.wpilib.simulation.DifferentialDrivetrainSim;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotGearing;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotMotor;
import org.wpilib.simulation.DifferentialDrivetrainSim.KitbotWheelSize;

public class DriveIOSim implements DriveIO {
  private DifferentialDrivetrainSim sim =
      DifferentialDrivetrainSim.createKitbotSim(
          KitbotMotor.DUAL_CIM_PER_SIDE, KitbotGearing.RATIO_10P71, KitbotWheelSize.SIX_INCH, null);

  private double leftAppliedVolts = 0.0;
  private double rightAppliedVolts = 0.0;
  private boolean closedLoop = false;
  private PIDController leftPID = new PIDController(simKp, 0.0, simKd);
  private PIDController rightPID = new PIDController(simKp, 0.0, simKd);
  private double leftFFVolts = 0.0;
  private double rightFFVolts = 0.0;

  @Override
  public void updateInputs(DriveIOInputs inputs) {
    if (closedLoop) {
      leftAppliedVolts = leftFFVolts + leftPID.calculate(sim.getLeftVelocity() / wheelRadiusMeters);
      rightAppliedVolts =
          rightFFVolts + rightPID.calculate(sim.getRightVelocity() / wheelRadiusMeters);
    }

    // Update simulation state
    sim.setInputs(
        Math.clamp(leftAppliedVolts, -12.0, 12.0), Math.clamp(rightAppliedVolts, -12.0, 12.0));
    sim.update(0.02);

    inputs.leftPositionRad = sim.getLeftPosition() / wheelRadiusMeters;
    inputs.leftVelocityRadPerSec = sim.getLeftVelocity() / wheelRadiusMeters;
    inputs.leftAppliedVolts = leftAppliedVolts;
    inputs.leftCurrentAmps = new double[] {sim.getLeftCurrentDraw()};

    inputs.rightPositionRad = sim.getRightPosition() / wheelRadiusMeters;
    inputs.rightVelocityRadPerSec = sim.getRightVelocity() / wheelRadiusMeters;
    inputs.rightAppliedVolts = rightAppliedVolts;
    inputs.rightCurrentAmps = new double[] {sim.getRightCurrentDraw()};
  }

  @Override
  public void setVoltage(double leftVolts, double rightVolts) {
    closedLoop = false;
    leftAppliedVolts = leftVolts;
    rightAppliedVolts = rightVolts;
  }

  @Override
  public void setVelocity(
      double leftRadPerSec, double rightRadPerSec, double leftFFVolts, double rightFFVolts) {
    closedLoop = true;
    this.leftFFVolts = leftFFVolts;
    this.rightFFVolts = rightFFVolts;
    leftPID.setSetpoint(leftRadPerSec);
    rightPID.setSetpoint(rightRadPerSec);
  }
}
