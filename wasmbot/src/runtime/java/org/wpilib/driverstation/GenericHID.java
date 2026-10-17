// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.wpilib.driverstation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.teamvan.wasmbot.WasmRuntime;
import org.wpilib.event.BooleanEvent;
import org.wpilib.event.EventLoop;
import org.wpilib.util.Pair;

/**
 * HAL-free HID backed by the browser Driver Station snapshot.
 *
 * <p>Port zero provides raw axes 0 (left X), 1 (left Y), and 2 (right X), matching the
 * WPILib gamepad axis mapping. The host does not publish other axes, buttons, POVs, or touchpads:
 * those inputs remain neutral and are absent from the availability masks. Ports one through five
 * are disconnected. No native HID outputs are available.
 */
public final class GenericHID implements HIDDevice {
  /** A rumble output on a controller. */
  public enum RumbleType {
    LEFT_RUMBLE,
    RIGHT_RUMBLE,
    LEFT_TRIGGER_RUMBLE,
    RIGHT_TRIGGER_RUMBLE
  }

  /** Output capabilities, with the same bit values as WPILib. */
  public enum SupportedOutput {
    NONE(0x0),
    MONO_LED(0x1),
    RGB_LED(0x2),
    PLAYER_LED(0x4),
    RUMBLE(0x8),
    TRIGGER_RUMBLE(0x10);

    private final int value;

    SupportedOutput(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  /** USB HID interface type, with the same values as WPILib. */
  public enum HIDType {
    UNKNOWN(0),
    STANDARD(1),
    XBOX_360(2),
    XBOX_ONE(3),
    PS3(4),
    PS4(5),
    PS5(6),
    SWITCH_PRO(7),
    SWITCH_JOYCON_LEFT(8),
    SWITCH_JOYCON_RIGHT(9),
    SWITCH_JOYCON_PAIR(10),
    GAMECUBE(11),
    STEAM(12);

    public final int value;
    private static final Map<Integer, HIDType> TYPES = new HashMap<>();

    HIDType(int value) {
      this.value = value;
    }

    static {
      for (HIDType type : values()) {
        TYPES.put(type.value, type);
      }
    }

    public static HIDType of(int value) {
      return TYPES.getOrDefault(value, UNKNOWN);
    }
  }

  private static final TouchpadFinger NEUTRAL_FINGER = new TouchpadFinger(false, 0.0f, 0.0f);
  private final int m_port;
  private final Map<EventLoop, Map<Integer, BooleanEvent>> m_buttonCache = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, BooleanEvent>> m_axisLessThanCache =
      new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, BooleanEvent>> m_axisGreaterThanCache =
      new HashMap<>();
  private final Map<EventLoop, Map<Integer, BooleanEvent>> m_povCache = new HashMap<>();

  GenericHID(int port) {
    m_port = port;
  }

  @Override
  public GenericHID getHID() {
    return this;
  }

  /** @return false; the snapshot provides no buttons */
  public boolean getRawButton(int button) {
    return false;
  }

  /** @return false; an unavailable button has no press edges */
  public boolean getRawButtonPressed(int button) {
    return false;
  }

  /** @return false; an unavailable button has no release edges */
  public boolean getRawButtonReleased(int button) {
    return false;
  }

  public BooleanEvent button(int button, EventLoop loop) {
    synchronized (m_buttonCache) {
      var cache = m_buttonCache.computeIfAbsent(loop, k -> new HashMap<>());
      return cache.computeIfAbsent(button, k -> new BooleanEvent(loop, () -> getRawButton(k)));
    }
  }

  /** @return the latest supported axis value, or zero for an unavailable axis or port */
  public double getRawAxis(int axis) {
    if (m_port != 0) {
      return 0.0;
    }
    return switch (axis) {
      case 0 -> WasmRuntime.getLeftX();
      case 1 -> WasmRuntime.getLeftY();
      case 2 -> WasmRuntime.getRightX();
      default -> 0.0;
    };
  }

  /** @return center; the snapshot provides no POVs */
  public POVDirection getPOV(int pov) {
    return POVDirection.CENTER;
  }

  public POVDirection getPOV() {
    return getPOV(0);
  }

  public BooleanEvent pov(POVDirection angle, EventLoop loop) {
    return pov(0, angle, loop);
  }

  public BooleanEvent pov(int pov, POVDirection angle, EventLoop loop) {
    synchronized (m_povCache) {
      var cache = m_povCache.computeIfAbsent(loop, k -> new HashMap<>());
      return cache.computeIfAbsent(
          pov * 16 + angle.value, k -> new BooleanEvent(loop, () -> getPOV(pov) == angle));
    }
  }

  public BooleanEvent povUp(EventLoop loop) {
    return pov(POVDirection.UP, loop);
  }

  public BooleanEvent povUpRight(EventLoop loop) {
    return pov(POVDirection.UP_RIGHT, loop);
  }

  public BooleanEvent povRight(EventLoop loop) {
    return pov(POVDirection.RIGHT, loop);
  }

  public BooleanEvent povDownRight(EventLoop loop) {
    return pov(POVDirection.DOWN_RIGHT, loop);
  }

  public BooleanEvent povDown(EventLoop loop) {
    return pov(POVDirection.DOWN, loop);
  }

  public BooleanEvent povDownLeft(EventLoop loop) {
    return pov(POVDirection.DOWN_LEFT, loop);
  }

  public BooleanEvent povLeft(EventLoop loop) {
    return pov(POVDirection.LEFT, loop);
  }

  public BooleanEvent povUpLeft(EventLoop loop) {
    return pov(POVDirection.UP_LEFT, loop);
  }

  public BooleanEvent povCenter(EventLoop loop) {
    return pov(POVDirection.CENTER, loop);
  }

  public BooleanEvent axisLessThan(int axis, double threshold, EventLoop loop) {
    synchronized (m_axisLessThanCache) {
      var cache = m_axisLessThanCache.computeIfAbsent(loop, k -> new HashMap<>());
      return cache.computeIfAbsent(
          Pair.of(axis, threshold),
          k -> new BooleanEvent(loop, () -> getRawAxis(axis) < threshold));
    }
  }

  public BooleanEvent axisGreaterThan(int axis, double threshold, EventLoop loop) {
    synchronized (m_axisGreaterThanCache) {
      var cache = m_axisGreaterThanCache.computeIfAbsent(loop, k -> new HashMap<>());
      return cache.computeIfAbsent(
          Pair.of(axis, threshold),
          k -> new BooleanEvent(loop, () -> getRawAxis(axis) > threshold));
    }
  }

  /** @return the same contiguous axis upper bound used by WPILib */
  public int getAxesMaximumIndex() {
    return m_port == 0 ? 3 : 0;
  }

  /** @return the bitmask of available axes, not an axis count */
  public int getAxesAvailable() {
    return m_port == 0 ? 0b111 : 0;
  }

  public int getPOVsMaximumIndex() {
    return 0;
  }

  public int getPOVsAvailable() {
    return 0;
  }

  public int getButtonsMaximumIndex() {
    return 0;
  }

  public long getButtonsAvailable() {
    return 0L;
  }

  /** @return whether port zero has received a browser Driver Station snapshot */
  public boolean isConnected() {
    return m_port == 0 && WasmRuntime.isDSAttached();
  }

  /** @return unknown; a keyboard-backed virtual HID is not a physical controller */
  public HIDType getGamepadType() {
    return HIDType.UNKNOWN;
  }

  /** @return an empty set; no controller outputs are supported */
  public EnumSet<SupportedOutput> getSupportedOutputs() {
    return EnumSet.noneOf(SupportedOutput.class);
  }

  public String getName() {
    return m_port == 0 ? "Browser keyboard" : "";
  }

  public int getPort() {
    return m_port;
  }

  /** @throws UnsupportedOperationException because the browser bridge has no LED output */
  public void setLeds(int r, int g, int b) {
    throw new UnsupportedOperationException("Controller LED output is unavailable in wasmbot");
  }

  /** @throws UnsupportedOperationException because the browser bridge has no rumble output */
  public void setRumble(RumbleType type, double value) {
    throw new UnsupportedOperationException("Controller rumble output is unavailable in wasmbot");
  }

  /** @throws UnsupportedOperationException because the browser bridge has no rumble output */
  public void setRawRumble(RumbleType type, int value) {
    throw new UnsupportedOperationException("Controller rumble output is unavailable in wasmbot");
  }

  public boolean getTouchpadFingerAvailable(int touchpad, int finger) {
    return false;
  }

  public TouchpadFinger getTouchpadFinger(int touchpad, int finger) {
    return NEUTRAL_FINGER;
  }
}
