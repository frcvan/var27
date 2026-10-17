package com.ctre.phoenix6;

/**
 * Wasm stand-in for Phoenix {@code CANBus}. The real class resolves native symbols while its class
 * initializes, which exits the process when the vendor library is absent. Sim only needs the name
 * and a non-FD answer so consumer CAN configuration can load.
 */
public class CANBus implements Cloneable, Comparable<CANBus> {
  private final String name;

  /** Creates the default bus. Sim ignores the platform-specific name. */
  public CANBus() {
    this("");
  }

  /**
   * @param canbus bus name; stored and otherwise ignored
   */
  public CANBus(String canbus) {
    name = canbus;
  }

  /**
   * @return false; sim does not use the CANivore FD sample rate
   */
  public boolean isNetworkFD() {
    return false;
  }

  /**
   * @return the name passed to the constructor
   */
  public String getName() {
    return name;
  }

  @Override
  public int compareTo(CANBus other) {
    return name.compareTo(other.name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof CANBus canbus && name.equals(canbus.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public CANBus clone() {
    return new CANBus(name);
  }
}
