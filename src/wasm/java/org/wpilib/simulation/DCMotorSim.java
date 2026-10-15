package org.wpilib.simulation;

import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N2;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.LinearSystem;
import org.wpilib.system.RobotController;

/**
 * Wasm stand-in for {@code DCMotorSim}. Identical to WPILib except {@link #setInputVoltage} reads
 * the shadowed {@link RobotController} instead of {@code PowerJNI}.
 */
public class DCMotorSim extends LinearSystemSim<N2, N1, N2> {
  private final DCMotor gearbox;
  private final double gearing;
  private final double j;

  /**
   * @param plant DC motor plant
   * @param gearbox motor model
   * @param measurementStdDevs unused measurement noise; accepted to match WPILib's signature
   */
  public DCMotorSim(LinearSystem<N2, N1, N2> plant, DCMotor gearbox, double... measurementStdDevs) {
    super(plant, measurementStdDevs);
    this.gearbox = gearbox;
    gearing = -gearbox.Kv * plant.getA(1, 1) / plant.getB(1, 0);
    j = gearing * gearbox.Kt / (gearbox.R * plant.getB(1, 0));
  }

  /**
   * @return output position in radians
   */
  public double getAngularPosition() {
    return getOutput(0);
  }

  /**
   * @return output velocity in radians per second
   */
  public double getAngularVelocity() {
    return getOutput(1);
  }

  /**
   * @return motor current in amps, signed by input voltage
   */
  public double getCurrentDraw() {
    return gearbox.getCurrent(m_x.get(1, 0) * gearing, m_u.get(0, 0)) * Math.signum(m_u.get(0, 0));
  }

  /**
   * Sets the input and clamps it to the simulated battery voltage.
   *
   * @param volts commanded voltage
   */
  public void setInputVoltage(double volts) {
    setInput(volts);
    clampInput(RobotController.getBatteryVoltage());
  }
}
