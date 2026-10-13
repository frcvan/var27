package org.teamvan.robot.mechanisms.drive;

import static org.wpilib.units.Units.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.DoubleSupplier;
import org.teamvan.robot.Constants;
import org.teamvan.robot.Constants.Mode;
import org.teamvan.robot.generated.TunerConstants;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.driverstation.MatchState;
import org.wpilib.driverstation.RobotState;
import org.wpilib.math.estimator.SwerveDrivePoseEstimator;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.geometry.Twist2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.math.kinematics.SwerveDriveKinematics;
import org.wpilib.math.kinematics.SwerveModulePosition;
import org.wpilib.math.kinematics.SwerveModuleVelocity;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.math.util.MathUtil;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.util.Alert;

/**
 * Swerve drivetrain mechanism, ported from the AdvantageKit TalonFX swerve template to the WPILib
 * 2027 {@link Mechanism} API.
 *
 * <p>Commands claim sole ownership of the drivetrain through the command scheduler, so no command
 * can drive the robot while another one already does. Per-loop work is split into {@link
 * #updateInputs()} (read hardware, sample odometry) and {@link #updateOutputs()} (publish
 * telemetry), which the robot calls around {@code Scheduler.getDefault().run()}.
 */
public class Drive implements Mechanism {
  /** Joystick deadband applied to the linear and angular drive inputs. */
  private static final double DEADBAND = 0.1;

  /** The frequency at which the Phoenix odometry thread samples module and gyro signals. */
  public static final double ODOMETRY_FREQUENCY =
      TunerConstants.kCANBus.isNetworkFD() ? 250.0 : 100.0;

  public static final double DRIVE_BASE_RADIUS =
      Math.max(
          Math.max(
              Math.hypot(TunerConstants.FrontLeft.LocationX, TunerConstants.FrontLeft.LocationY),
              Math.hypot(TunerConstants.FrontRight.LocationX, TunerConstants.FrontRight.LocationY)),
          Math.max(
              Math.hypot(TunerConstants.BackLeft.LocationX, TunerConstants.BackLeft.LocationY),
              Math.hypot(TunerConstants.BackRight.LocationX, TunerConstants.BackRight.LocationY)));

  public static final Lock odometryLock = new ReentrantLock();
  private final GyroIO gyroIO;
  private final GyroIO.GyroIOInputs gyroInputs = new GyroIO.GyroIOInputs();
  private final Module[] modules = new Module[4]; // FL, FR, BL, BR
  private final Alert gyroDisconnectedAlert =
      new Alert(
          "Drive/GyroDisconnected",
          "Disconnected gyro, using kinematics as fallback.",
          Alert.Level.HIGH);

  private final SwerveDriveKinematics kinematics =
      new SwerveDriveKinematics(getModuleTranslations());
  private Rotation2d rawGyroRotation = Rotation2d.ZERO;
  private SwerveModulePosition[] lastModulePositions = // For delta tracking
      new SwerveModulePosition[] {
        new SwerveModulePosition(),
        new SwerveModulePosition(),
        new SwerveModulePosition(),
        new SwerveModulePosition()
      };
  private final SwerveDrivePoseEstimator poseEstimator =
      new SwerveDrivePoseEstimator(kinematics, rawGyroRotation, lastModulePositions, Pose2d.ZERO);

  public Drive(
      GyroIO gyroIO,
      ModuleIO flModuleIO,
      ModuleIO frModuleIO,
      ModuleIO blModuleIO,
      ModuleIO brModuleIO) {
    this.gyroIO = gyroIO;
    modules[0] = new Module(flModuleIO, 0, TunerConstants.FrontLeft);
    modules[1] = new Module(frModuleIO, 1, TunerConstants.FrontRight);
    modules[2] = new Module(blModuleIO, 2, TunerConstants.BackLeft);
    modules[3] = new Module(brModuleIO, 3, TunerConstants.BackRight);

    // Start odometry thread
    PhoenixOdometryThread.getInstance().start();
  }

  /**
   * Reads the drivetrain hardware and integrates the high-frequency odometry samples. Call this
   * before running the command scheduler so commands act on fresh data.
   */
  public void updateInputs() {
    odometryLock.lock(); // Prevents odometry updates while reading data
    gyroIO.updateInputs(gyroInputs);
    gyroInputs.logTo(Telemetry.getTable("Drive/Gyro"));
    for (var module : modules) {
      module.updateInputs();
    }
    odometryLock.unlock();

    // Stop moving when disabled
    if (RobotState.isDisabled()) {
      for (var module : modules) {
        module.stop();
      }
    }

    // Update odometry
    double[] sampleTimestamps =
        modules[0].getOdometryTimestamps(); // All signals are sampled together
    int sampleCount = sampleTimestamps.length;
    for (int i = 0; i < sampleCount; i++) {
      // Read wheel positions and deltas from each module
      SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
      SwerveModulePosition[] moduleDeltas = new SwerveModulePosition[4];
      for (int moduleIndex = 0; moduleIndex < 4; moduleIndex++) {
        modulePositions[moduleIndex] = modules[moduleIndex].getOdometryPositions()[i];
        moduleDeltas[moduleIndex] =
            new SwerveModulePosition(
                modulePositions[moduleIndex].distance - lastModulePositions[moduleIndex].distance,
                modulePositions[moduleIndex].angle);
        lastModulePositions[moduleIndex] = modulePositions[moduleIndex];
      }

      // Update gyro angle
      if (gyroInputs.connected) {
        // Use the real gyro angle
        rawGyroRotation = gyroInputs.odometryYawPositions[i];
      } else {
        // Use the angle delta from the kinematics and module deltas
        Twist2d twist = kinematics.toTwist2d(moduleDeltas);
        rawGyroRotation = rawGyroRotation.plus(new Rotation2d(twist.dtheta));
      }

      // Apply update
      poseEstimator.updateWithTime(sampleTimestamps[i], rawGyroRotation, modulePositions);
    }

    // Update gyro alert
    gyroDisconnectedAlert.set(!gyroInputs.connected && Constants.currentMode != Mode.SIM);
  }

  /**
   * Publishes the drivetrain's measured state to telemetry. Call this after running the command
   * scheduler so the reported state reflects the outputs commands just wrote.
   */
  public void updateOutputs() {
    if (RobotState.isDisabled()) {
      // Log empty setpoint states when disabled
      Telemetry.log("SwerveStates/Setpoints", new SwerveModuleVelocity[] {});
      Telemetry.log("SwerveStates/SetpointsOptimized", new SwerveModuleVelocity[] {});
    }
    Telemetry.log("Odometry/Robot", getPose());
    Telemetry.log("SwerveStates/Measured", getModuleVelocities());
    Telemetry.log("SwerveChassisVelocities/Measured", getChassisVelocities());
  }

  /**
   * Runs the drive at the desired velocity.
   *
   * @param velocities Velocities in meters/sec
   */
  public void runVelocity(ChassisVelocities velocities) {
    // Calculate module setpoints
    ChassisVelocities discreteVelocities = velocities.discretize(0.02);
    SwerveModuleVelocity[] setpointStates = kinematics.toSwerveModuleVelocities(discreteVelocities);
    setpointStates =
        SwerveDriveKinematics.desaturateWheelVelocities(
            setpointStates, TunerConstants.kSpeedAt12Volts);

    // Log unoptimized setpoints and setpoint velocities
    Telemetry.log("SwerveStates/Setpoints", setpointStates);
    Telemetry.log("SwerveChassisVelocities/Setpoints", discreteVelocities);

    // Send setpoints to modules
    SwerveModuleVelocity[] optimizedStates = new SwerveModuleVelocity[4];
    for (int i = 0; i < 4; i++) {
      optimizedStates[i] = modules[i].runSetpoint(setpointStates[i]);
    }

    // Log optimized setpoints
    Telemetry.log("SwerveStates/SetpointsOptimized", optimizedStates);
  }

  /** Runs the drive in a straight line with the specified drive output. */
  public void runCharacterization(double output) {
    for (int i = 0; i < 4; i++) {
      modules[i].runCharacterization(output);
    }
  }

  /**
   * Returns a field-relative drive command that reads the gamepad's left stick for linear velocity
   * and its right stick for rotation. Intended to be set as the drivetrain's default command.
   *
   * @param gamepad the driver's gamepad
   * @return the joystick drive command
   */
  public Command joystickDrive(Gamepad gamepad) {
    return joystickDrive(
        () -> -gamepad.getLeftY(), // Forward
        () -> -gamepad.getLeftX(), // Left
        () -> -gamepad.getRightX()); // Counter-clockwise
  }

  /**
   * Returns a field-relative drive command from the given stick suppliers.
   *
   * @param xSupplier forward input in [-1, 1], positive forward
   * @param ySupplier leftward input in [-1, 1], positive left
   * @param omegaSupplier counter-clockwise input in [-1, 1], positive counter-clockwise
   * @return the joystick drive command
   */
  public Command joystickDrive(
      DoubleSupplier xSupplier, DoubleSupplier ySupplier, DoubleSupplier omegaSupplier) {
    return runRepeatedly(
            () -> {
              // Get linear velocity
              Translation2d linearVelocity =
                  getLinearVelocityFromJoysticks(xSupplier.getAsDouble(), ySupplier.getAsDouble());

              // Apply rotation deadband and square it for more precise control
              double omega = MathUtil.applyDeadband(omegaSupplier.getAsDouble(), DEADBAND);
              omega = Math.copySign(omega * omega, omega);

              // Convert to field relative velocities and send them to the drivetrain
              ChassisVelocities velocities =
                  new ChassisVelocities(
                      linearVelocity.getX() * getMaxLinearSpeedMetersPerSec(),
                      linearVelocity.getY() * getMaxLinearSpeedMetersPerSec(),
                      omega * getMaxAngularSpeedRadPerSec());
              runVelocity(velocities.toRobotRelative(getDriverRotation()));
            })
        .named(getName() + " Joystick");
  }

  /** Stops the drive. */
  public void stop() {
    runVelocity(new ChassisVelocities());
  }

  /**
   * Stops the drive and turns the modules to an X arrangement to resist movement. The modules will
   * return to their normal orientations the next time a nonzero velocity is requested.
   */
  public void stopWithX() {
    Rotation2d[] headings = new Rotation2d[4];
    for (int i = 0; i < 4; i++) {
      headings[i] = getModuleTranslations()[i].getAngle().orElse(Rotation2d.ZERO);
    }
    kinematics.resetHeadings(headings);
    stop();
  }

  /** Returns the module velocities (turn angles and drive velocities) for all of the modules. */
  private SwerveModuleVelocity[] getModuleVelocities() {
    SwerveModuleVelocity[] velocities = new SwerveModuleVelocity[4];
    for (int i = 0; i < 4; i++) {
      velocities[i] = modules[i].getState();
    }
    return velocities;
  }

  /** Returns the module positions (turn angles and drive positions) for all of the modules. */
  private SwerveModulePosition[] getModulePositions() {
    SwerveModulePosition[] states = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      states[i] = modules[i].getPosition();
    }
    return states;
  }

  /** Returns the measured chassis velocities of the robot. */
  private ChassisVelocities getChassisVelocities() {
    return kinematics.toChassisVelocities(getModuleVelocities());
  }

  /** Returns the position of each module in radians. */
  public double[] getWheelRadiusCharacterizationPositions() {
    double[] values = new double[4];
    for (int i = 0; i < 4; i++) {
      values[i] = modules[i].getWheelRadiusCharacterizationPosition();
    }
    return values;
  }

  /** Returns the average velocity of the modules in rotations/sec (Phoenix native units). */
  public double getFFCharacterizationVelocity() {
    double output = 0.0;
    for (int i = 0; i < 4; i++) {
      output += modules[i].getFFCharacterizationVelocity() / 4.0;
    }
    return output;
  }

  /** Returns the current odometry pose. */
  public Pose2d getPose() {
    return poseEstimator.getEstimatedPosition();
  }

  /** Returns the current odometry rotation. */
  public Rotation2d getRotation() {
    return getPose().getRotation();
  }

  /** Resets the current odometry pose. */
  public void setPose(Pose2d pose) {
    poseEstimator.resetPosition(rawGyroRotation, getModulePositions(), pose);
  }

  /** Adds a new timestamped vision measurement. */
  public void addVisionMeasurement(
      Pose2d visionRobotPoseMeters,
      double timestampSeconds,
      Matrix<N3, N1> visionMeasurementStdDevs) {
    poseEstimator.addVisionMeasurement(
        visionRobotPoseMeters, timestampSeconds, visionMeasurementStdDevs);
  }

  /** Returns the maximum linear speed in meters per sec. */
  public double getMaxLinearSpeedMetersPerSec() {
    return TunerConstants.kSpeedAt12Volts.in(MetersPerSecond);
  }

  /** Returns the maximum angular speed in radians per sec. */
  public double getMaxAngularSpeedRadPerSec() {
    return getMaxLinearSpeedMetersPerSec() / DRIVE_BASE_RADIUS;
  }

  /**
   * Converts raw stick inputs into a unit-magnitude linear velocity vector. The magnitude is
   * deadbanded and squared for finer control near the center, and the direction follows the stick
   * angle.
   */
  private static Translation2d getLinearVelocityFromJoysticks(double x, double y) {
    // Apply deadband
    double linearMagnitude = MathUtil.applyDeadband(Math.hypot(x, y), DEADBAND);
    Rotation2d linearDirection = new Rotation2d(Math.atan2(y, x));

    // Square magnitude for more precise control
    linearMagnitude = linearMagnitude * linearMagnitude;

    // Return the linear velocity
    return new Translation2d(
        linearMagnitude * linearDirection.getCos(), linearMagnitude * linearDirection.getSin());
  }

  /**
   * Returns the rotation used to interpret driver stick inputs as field-relative. The heading is
   * flipped on the red alliance so that "forward" always points away from the driver.
   */
  private Rotation2d getDriverRotation() {
    boolean isFlipped = MatchState.getAlliance().orElse(Alliance.BLUE) == Alliance.RED;
    return isFlipped ? getRotation().plus(Rotation2d.PI) : getRotation();
  }

  /** Returns an array of module translations. */
  public static Translation2d[] getModuleTranslations() {
    return new Translation2d[] {
      new Translation2d(TunerConstants.FrontLeft.LocationX, TunerConstants.FrontLeft.LocationY),
      new Translation2d(TunerConstants.FrontRight.LocationX, TunerConstants.FrontRight.LocationY),
      new Translation2d(TunerConstants.BackLeft.LocationX, TunerConstants.BackLeft.LocationY),
      new Translation2d(TunerConstants.BackRight.LocationX, TunerConstants.BackRight.LocationY)
    };
  }
}
