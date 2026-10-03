package van.robot;

import org.wpilib.command2.button.CommandXboxController;
import van.robot.command.DriveCommands;
import van.robot.subsystem.drive.Drive;
import van.robot.subsystem.drive.DriveIOSim;
import van.robot.subsystem.drive.GyroIO;

public class RobotContainer {
  private final Drive drive;

  private final CommandXboxController controller = new CommandXboxController(0);

  public RobotContainer() {
    drive = new Drive(new DriveIOSim(), new GyroIO() {});
    drive.setDefaultCommand(
        DriveCommands.arcadeDrive(
            drive, () -> -controller.getLeftY(), () -> -controller.getRightX()));
  }
}
