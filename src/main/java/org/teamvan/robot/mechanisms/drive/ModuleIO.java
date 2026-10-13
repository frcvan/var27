package org.teamvan.robot.mechanisms.drive;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.TelemetryLoggable;
import org.wpilib.telemetry.TelemetryTable;

public interface ModuleIO {
  public static class ModuleIOInputs implements TelemetryLoggable {
    public boolean driveConnected = false;
    public double drivePositionRad = 0.0;
    public double driveVelocityRadPerSec = 0.0;
    public double driveAppliedVolts = 0.0;
    public double driveCurrentAmps = 0.0;

    public boolean turnConnected = false;
    public boolean turnEncoderConnected = false;
    public Rotation2d turnAbsolutePosition = Rotation2d.ZERO;
    public Rotation2d turnPosition = Rotation2d.ZERO;
    public double turnVelocityRadPerSec = 0.0;
    public double turnAppliedVolts = 0.0;
    public double turnCurrentAmps = 0.0;

    public double[] odometryTimestamps = new double[] {};
    public double[] odometryDrivePositionsRad = new double[] {};
    public Rotation2d[] odometryTurnPositions = new Rotation2d[] {};

    @Override
    public void logTo(TelemetryTable table) {
      table.log("DriveConnected", driveConnected);
      table.log("DrivePositionRad", drivePositionRad);
      table.log("DriveVelocityRadPerSec", driveVelocityRadPerSec);
      table.log("DriveAppliedVolts", driveAppliedVolts);
      table.log("DriveCurrentAmps", driveCurrentAmps);
      table.log("TurnConnected", turnConnected);
      table.log("TurnEncoderConnected", turnEncoderConnected);
      table.log("TurnAbsolutePosition", turnAbsolutePosition);
      table.log("TurnPosition", turnPosition);
      table.log("TurnVelocityRadPerSec", turnVelocityRadPerSec);
      table.log("TurnAppliedVolts", turnAppliedVolts);
      table.log("TurnCurrentAmps", turnCurrentAmps);
      table.log("OdometryTimestamps", odometryTimestamps);
      table.log("OdometryDrivePositionsRad", odometryDrivePositionsRad);
      table.log("OdometryTurnPositions", odometryTurnPositions);
    }
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(ModuleIOInputs inputs) {}

  /** Run the drive motor at the specified open loop value. */
  public default void setDriveOpenLoop(double output) {}

  /** Run the turn motor at the specified open loop value. */
  public default void setTurnOpenLoop(double output) {}

  /** Run the drive motor at the specified velocity. */
  public default void setDriveVelocity(double velocityRadPerSec) {}

  /** Run the turn motor to the specified rotation. */
  public default void setTurnPosition(Rotation2d rotation) {}
}
