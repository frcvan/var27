package example;

import org.wpilib.command3.Mechanism;

/** A hardware-free mechanism whose real default command advances once per scheduler tick. */
public final class Counter implements Mechanism {
  private int ticks;

  public Counter() {
    setDefaultCommand(runRepeatedly(() -> ticks++).named("Count scheduler ticks"));
  }

  public int ticks() {
    return ticks;
  }
}
