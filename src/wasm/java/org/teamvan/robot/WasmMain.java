package org.teamvan.robot;

import org.teamvan.robot.generated.TunerConstants;
import org.teamvan.robot.manager.Manager;
import org.teamvan.robot.mechanisms.drive.Drive;
import org.teamvan.robot.mechanisms.drive.GyroIO;
import org.teamvan.robot.mechanisms.drive.ModuleIOSim;
import org.teamvan.robot.mechanisms.flag.Flag;
import org.teamvan.wasm.JsBridge;
import org.teamvan.wasm.WasmApplication;
import org.teamvan.wasm.WasmRobot;
import org.wpilib.driverstation.DriverStation;

/** Robot-specific simulation wiring. The reusable library owns the WASM runtime and Scheduler. */
public final class WasmMain implements WasmRobot {
  private final Manager manager;

  private WasmMain() {
    ModuleIOSim fl = new ModuleIOSim(TunerConstants.FrontLeft);
    ModuleIOSim fr = new ModuleIOSim(TunerConstants.FrontRight);
    ModuleIOSim bl = new ModuleIOSim(TunerConstants.BackLeft);
    ModuleIOSim br = new ModuleIOSim(TunerConstants.BackRight);
    Drive drive = new Drive(new GyroIO() {}, fl, fr, bl, br);
    drive.setDefaultCommand(drive.joystickDrive(DriverStation.getGamepad(0)));
    manager = new Manager(drive, new Flag());
  }

  public static void main(String... args) {
    WasmApplication.launch(WasmMain::new);
  }

  @Override
  public void updateInputs() {
    manager.updateInputs();
  }

  @Override
  public void updateOutputs() {
    manager.updateOutputs();
    var pose = manager.pose();
    JsBridge.ntPut("/Sim/Pose/X", pose.getX());
    JsBridge.ntPut("/Sim/Pose/Y", pose.getY());
    JsBridge.ntPut("/Sim/Pose/Rotation", pose.getRotation().getRadians());
  }
}
