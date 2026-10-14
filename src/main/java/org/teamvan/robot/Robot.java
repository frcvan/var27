package org.teamvan.robot;

import org.teamvan.robot.generated.TunerConstants;
import org.teamvan.robot.manager.Manager;
import org.teamvan.robot.mechanisms.drive.Drive;
import org.teamvan.robot.mechanisms.drive.GyroIO;
import org.teamvan.robot.mechanisms.drive.GyroIOPigeon2;
import org.teamvan.robot.mechanisms.drive.ModuleIO;
import org.teamvan.robot.mechanisms.drive.ModuleIOSim;
import org.teamvan.robot.mechanisms.drive.ModuleIOTalonFX;
import org.teamvan.robot.mechanisms.flag.Flag;
import org.teamvan.robot.util.Profiler;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.framework.OpModeRobot;

public class Robot extends OpModeRobot {
  /** The driver's gamepad, shared with opmodes that want to bind extra buttons. */
  public final CommandGamepad driverController = new CommandGamepad(0);

  final Manager manager;

  public Robot() {
    if (Boolean.getBoolean("debugMode")) {
      Profiler.start();
    }

    final Drive drive;
    switch (Constants.currentMode) {
      case REAL:
        // Real robot, instantiate hardware IO implementations
        drive =
            new Drive(
                new GyroIOPigeon2(),
                new ModuleIOTalonFX(TunerConstants.FrontLeft),
                new ModuleIOTalonFX(TunerConstants.FrontRight),
                new ModuleIOTalonFX(TunerConstants.BackLeft),
                new ModuleIOTalonFX(TunerConstants.BackRight));
        break;

      case SIM:
        // Sim robot, instantiate physics sim IO implementations
        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIOSim(TunerConstants.FrontLeft),
                new ModuleIOSim(TunerConstants.FrontRight),
                new ModuleIOSim(TunerConstants.BackLeft),
                new ModuleIOSim(TunerConstants.BackRight));
        break;

      default:
        // Replayed robot, disable IO implementations
        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {});
        break;
    }

    // Field-relative teleop drive is the drivetrain's default command. It runs whenever no other
    // command requires the drivetrain, and any command scheduled by an opmode or button binding
    // takes over for as long as it runs.
    drive.setDefaultCommand(drive.joystickDrive(driverController.getGamepad()));

    final Flag flag = new Flag();

    manager = new Manager(drive, flag);
  }

  @Override
  public void robotPeriodic() {
    // Read hardware and integrate odometry before running commands
    manager.updateInputs();
    Scheduler.getDefault().run();
    // Publish telemetry after commands have written their outputs
    manager.updateOutputs();
  }
}
