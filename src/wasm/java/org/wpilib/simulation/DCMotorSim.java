package org.wpilib.simulation;

import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N2;
import org.wpilib.math.random.Normal;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Discretization;
import org.wpilib.math.system.LinearSystem;
import org.wpilib.system.RobotController;

/**
 * Wasm DC motor simulation with cached discretization and in-place state/output updates. Voltage
 * clamping uses the shadowed {@link RobotController} instead of {@code PowerJNI}.
 */
public class DCMotorSim extends LinearSystemSim<N2, N1, N2> {
  private final DCMotor gearbox;
  private final double gearing;
  private final boolean measurementNoise;
  private double discreteDt = Double.NaN;
  private double a00;
  private double a01;
  private double a10;
  private double a11;
  private double b0;
  private double b1;

  /**
   * @param plant DC motor plant
   * @param gearbox motor model
   * @param measurementStdDevs optional position and velocity measurement standard deviations
   */
  public DCMotorSim(LinearSystem<N2, N1, N2> plant, DCMotor gearbox, double... measurementStdDevs) {
    super(plant, measurementStdDevs);
    this.gearbox = gearbox;
    gearing = -gearbox.Kv * plant.getA(1, 1) / plant.getB(1, 0);
    measurementNoise =
        measurementStdDevs.length != 0
            && (measurementStdDevs[0] != 0.0 || measurementStdDevs[1] != 0.0);
  }

  /** Updates the exact zero-order-hold dynamics without rebuilding matrices every robot tick. */
  @Override
  public void update(double dt) {
    if (dt != discreteDt) {
      // LinearSystem.calculateX otherwise exponentiates the same augmented 3x3 matrix every tick.
      var discrete = Discretization.discretizeAB(m_plant.getA(), m_plant.getB(), dt);
      var a = discrete.getFirst();
      var b = discrete.getSecond();
      a00 = a.get(0, 0);
      a01 = a.get(0, 1);
      a10 = a.get(1, 0);
      a11 = a.get(1, 1);
      b0 = b.get(0, 0);
      b1 = b.get(1, 0);
      discreteDt = dt;
    }

    double position = m_x.get(0, 0);
    double velocity = m_x.get(1, 0);
    double input = m_u.get(0, 0);
    double nextPosition = a00 * position + a01 * velocity + b0 * input;
    double nextVelocity = a10 * position + a11 * velocity + b1 * input;
    m_x.set(0, 0, nextPosition);
    m_x.set(1, 0, nextVelocity);
    for (int row = 0; row < 2; row++) {
      m_y.set(
          row,
          0,
          m_plant.getC(row, 0) * nextPosition
              + m_plant.getC(row, 1) * nextVelocity
              + m_plant.getD(row, 0) * input);
    }
    if (measurementNoise) {
      var noise = Normal.normal(m_measurementStdDevs);
      for (int row = 0; row < 2; row++) {
        m_y.set(row, 0, m_y.get(row, 0) + noise.get(row, 0));
      }
    }
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
    double batteryVoltage = RobotController.getBatteryVoltage();
    setInput(0, Math.clamp(volts, -batteryVoltage, batteryVoltage));
  }
}
