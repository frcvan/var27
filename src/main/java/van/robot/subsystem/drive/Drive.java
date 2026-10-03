// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package van.robot.subsystem.drive;

import static org.wpilib.units.Units.*;
import static van.robot.subsystem.drive.DriveConstants.*;

import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.command2.sysid.SysIdRoutine;
import org.wpilib.math.estimator.DifferentialDrivePoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Twist2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.math.kinematics.DifferentialDriveKinematics;
import van.robot.Constants;
import van.robot.Constants.Mode;

public class Drive extends SubsystemBase {
  private final DriveIO io;
  private final DriveIOInputsAutoLogged inputs = new DriveIOInputsAutoLogged();
  private final GyroIO gyroIO;
  private final GyroIOInputsAutoLogged gyroInputs = new GyroIOInputsAutoLogged();

  private final DifferentialDriveKinematics kinematics =
      new DifferentialDriveKinematics(trackWidth);
  private final double kS = Constants.currentMode == Mode.SIM ? simKs : realKs;
  private final double kV = Constants.currentMode == Mode.SIM ? simKv : realKv;
  private final DifferentialDrivePoseEstimator poseEstimator =
      new DifferentialDrivePoseEstimator(Rotation2d.ZERO, 0.0, 0.0, Pose2d.ZERO);
  private final SysIdRoutine sysId;
  private Rotation2d rawGyroRotation = Rotation2d.ZERO;
  private double lastLeftPositionMeters = 0.0;
  private double lastRightPositionMeters = 0.0;

  public Drive(DriveIO io, GyroIO gyroIO) {
    this.io = io;
    this.gyroIO = gyroIO;

    // Configure AutoBuilder for PathPlanner
    // AutoBuilder.configure(
    // this::getPose,
    // this::setPose,
    // () -> kinematics.toChassisSpeeds(
    // new DifferentialDriveWheelVelocities(
    // getLeftVelocityMetersPerSec(), getRightVelocityMetersPerSec())),
    // (ChassisSpeeds speeds) -> runClosedLoop(speeds),
    // new PPLTVController(0.02, maxSpeedMetersPerSec),
    // ppConfig,
    // () -> DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red,
    // this);
    // Pathfinding.setPathfinder(new LocalADStarAK());
    // PathPlannerLogging.setLogActivePathCallback(
    // (activePath) -> {
    // Logger.recordOutput("Odometry/Trajectory", activePath.toArray(new
    // Pose2d[0]));
    // });
    // PathPlannerLogging.setLogTargetPoseCallback(
    // (targetPose) -> {
    // Logger.recordOutput("Odometry/TrajectorySetpoint", targetPose);
    // });

    // Configure SysId
    sysId =
        new SysIdRoutine(
            new SysIdRoutine.Config(
                null,
                null,
                null,
                (state) -> Logger.recordOutput("Drive/SysIdState", state.toString())),
            new SysIdRoutine.Mechanism(
                (voltage) -> runOpenLoop(voltage.in(Volts), voltage.in(Volts)), null, this));
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    gyroIO.updateInputs(gyroInputs);
    Logger.processInputs("Drive", inputs);
    Logger.processInputs("Drive/Gyro", gyroInputs);

    // Update gyro angle
    if (gyroInputs.connected) {
      // Use the real gyro angle
      rawGyroRotation = gyroInputs.yawPosition;
    } else {
      // Use the angle delta from the kinematics and module deltas
      Twist2d twist =
          kinematics.toTwist2d(
              getLeftPositionMeters() - lastLeftPositionMeters,
              getRightPositionMeters() - lastRightPositionMeters);
      rawGyroRotation = rawGyroRotation.plus(new Rotation2d(twist.dtheta));
      lastLeftPositionMeters = getLeftPositionMeters();
      lastRightPositionMeters = getRightPositionMeters();
    }

    // Update odometry
    poseEstimator.update(rawGyroRotation, getLeftPositionMeters(), getRightPositionMeters());
  }

  /** Runs the drive at the desired velocity. */
  public void runClosedLoop(ChassisVelocities speeds) {
    var wheelSpeeds = kinematics.toWheelVelocities(speeds);
    runClosedLoop(wheelSpeeds.left, wheelSpeeds.right);
  }

  /** Runs the drive at the desired left and right velocities. */
  public void runClosedLoop(double leftMetersPerSec, double rightMetersPerSec) {
    double leftRadPerSec = leftMetersPerSec / wheelRadiusMeters;
    double rightRadPerSec = rightMetersPerSec / wheelRadiusMeters;
    Logger.recordOutput("Drive/LeftSetpointRadPerSec", leftRadPerSec);
    Logger.recordOutput("Drive/RightSetpointRadPerSec", rightRadPerSec);

    double leftFFVolts = kS * Math.signum(leftRadPerSec) + kV * leftRadPerSec;
    double rightFFVolts = kS * Math.signum(rightRadPerSec) + kV * rightRadPerSec;
    io.setVelocity(leftRadPerSec, rightRadPerSec, leftFFVolts, rightFFVolts);
  }

  /** Runs the drive in open loop. */
  public void runOpenLoop(double leftVolts, double rightVolts) {
    io.setVoltage(leftVolts, rightVolts);
  }

  /** Stops the drive. */
  public void stop() {
    runOpenLoop(0.0, 0.0);
  }

  /** Returns a command to run a quasistatic test in the specified direction. */
  public Command sysIdQuasistatic(SysIdRoutine.Direction direction) {
    return sysId.quasistatic(direction);
  }

  /** Returns a command to run a dynamic test in the specified direction. */
  public Command sysIdDynamic(SysIdRoutine.Direction direction) {
    return sysId.dynamic(direction);
  }

  /** Returns the current odometry pose. */
  @AutoLogOutput(key = "Odometry/Robot")
  public Pose2d getPose() {
    return poseEstimator.getEstimatedPosition();
  }

  /** Returns the current odometry rotation. */
  public Rotation2d getRotation() {
    return getPose().getRotation();
  }

  /** Resets the current odometry pose. */
  public void setPose(Pose2d pose) {
    poseEstimator.resetPosition(
        rawGyroRotation, getLeftPositionMeters(), getRightPositionMeters(), pose);
  }

  /**
   * Adds a vision measurement to the pose estimator.
   *
   * @param visionPose The pose of the robot as measured by the vision camera.
   * @param timestamp The timestamp of the vision measurement in seconds.
   */
  public void addVisionMeasurement(Pose2d visionPose, double timestamp) {
    poseEstimator.addVisionMeasurement(visionPose, timestamp);
  }

  /** Returns the position of the left wheels in meters. */
  @AutoLogOutput
  public double getLeftPositionMeters() {
    return inputs.leftPositionRad * wheelRadiusMeters;
  }

  /** Returns the position of the right wheels in meters. */
  @AutoLogOutput
  public double getRightPositionMeters() {
    return inputs.rightPositionRad * wheelRadiusMeters;
  }

  /** Returns the velocity of the left wheels in meters/second. */
  @AutoLogOutput
  public double getLeftVelocityMetersPerSec() {
    return inputs.leftVelocityRadPerSec * wheelRadiusMeters;
  }

  /** Returns the velocity of the right wheels in meters/second. */
  @AutoLogOutput
  public double getRightVelocityMetersPerSec() {
    return inputs.rightVelocityRadPerSec * wheelRadiusMeters;
  }

  /** Returns the average velocity in radians/second. */
  public double getCharacterizationVelocity() {
    return (inputs.leftVelocityRadPerSec + inputs.rightVelocityRadPerSec) / 2.0;
  }
}
