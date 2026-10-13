package org.teamvan.robot.mechanisms.drive;

import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.telemetry.TelemetryLoggable;
import org.wpilib.telemetry.TelemetryTable;

public interface GyroIO {
  public static class GyroIOInputs implements TelemetryLoggable {
    public boolean connected = false;
    public Rotation2d yawPosition = Rotation2d.ZERO;
    public double yawVelocityRadPerSec = 0.0;
    public double[] odometryYawTimestamps = new double[] {};
    public Rotation2d[] odometryYawPositions = new Rotation2d[] {};

    @Override
    public void logTo(TelemetryTable table) {
      table.log("Connected", connected);
      table.log("YawPosition", yawPosition);
      table.log("YawVelocityRadPerSec", yawVelocityRadPerSec);
      table.log("OdometryYawTimestamps", odometryYawTimestamps);
      table.log("OdometryYawPositions", odometryYawPositions);
    }
  }

  public default void updateInputs(GyroIOInputs inputs) {}
}
