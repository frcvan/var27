package org.teamvan.wasmbot.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.teamvan.wasmbot.JsBridge;
import org.teamvan.wasmbot.WasmRuntime;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.framework.TimedRobot;
import org.wpilib.math.util.MathSharedStore;
import org.wpilib.system.RobotController;

/** Real framework hooks, callback deadlines, transitions, and stopping on the native runtime. */
final class FrameworkSmoke {
  private FrameworkSmoke() {}

  static void run() {
    AtomicLong clock = new AtomicLong();
    WasmRuntime.setClock(clock::get);
    WasmRuntime.refresh();
    WasmRuntime.setEnabled(false);
    try {
      OpModeProbe opMode = new OpModeProbe();
      RobotController.setTimeSource(() -> 7_000_000_000L);
      check(MathSharedStore.getTimestamp() == 7.0, "framework math timestamps must respect the configured clock");
      RobotController.setTimeSource(WasmRuntime::nowNanos);
      opMode.startCompetition();
      opMode.startCompetition();
      expect(opMode.events, "simulationInit");
      clock.set(20_000_000);
      opMode.wasmTick();
      expect(opMode.events, "simulationInit", "connected", "disabledPeriodic", "none", "robot", "simulation");
      opMode.events.clear();
      WasmRuntime.setEnabled(true);
      clock.set(40_000_000);
      opMode.wasmTick();
      expect(opMode.events, "disabledExit", "none", "robot", "simulation");
      opMode.events.clear();
      WasmRuntime.setEnabled(false);
      clock.set(60_000_000);
      opMode.wasmTick();
      expect(opMode.events, "disabledInit", "none", "robot", "simulation");
      opMode.events.clear();
      clock.set(80_000_000);
      opMode.wasmTick();
      expect(opMode.events, "disabledPeriodic", "none", "robot", "simulation");
      opMode.endCompetition();
      opMode.events.clear();
      clock.set(100_000_000);
      opMode.wasmTick();
      expect(opMode.events);

      clock.set(0);
      TimedProbe timed = new TimedProbe();
      timed.startCompetition();
      timed.startCompetition();
      clock.set(20_000_000);
      timed.wasmTick();
      check(timed.periodics == 0 && timed.extras == 0, "custom periods must not run early");
      clock.set(30_000_000);
      timed.wasmTick();
      check(timed.periodics == 0 && timed.extras == 1, "additional callback must honor its own deadline");
      clock.set(40_000_000);
      timed.wasmTick();
      check(timed.periodics == 1 && timed.simulations == 1 && timed.disabledInits == 1,
          "TimedRobot first mode init and periodic sequence");
      clock.set(90_000_000);
      timed.wasmTick();
      check(timed.periodics == 2 && timed.extras == 2 && timed.simulations == 2,
          "overdue callbacks must coalesce rather than replay missed expirations");
      check(timed.getLoopStartTime() == clock.get(), "loop timestamp must use the host clock");
      check(timed.initializations == 1, "simulationInit must only run once");
      timed.close();
      clock.set(200_000_000);
      timed.wasmTick();
      check(timed.periodics == 2 && timed.extras == 2, "close must stop all callbacks");

      clock.set(0);
      ThrowingProbe failing = new ThrowingProbe();
      failing.startCompetition();
      clock.set(20_000_000);
      try {
        failing.wasmTick();
        throw new AssertionError("robotPeriodic exception must propagate");
      } catch (IllegalStateException failure) {
        check(failure == failing.failure, "robotPeriodic exception identity must be retained");
      }
      failing.wasmTick();
      check(failing.calls == 1, "failed expiration must not replay at the same host time");
      clock.set(40_000_000);
      failing.wasmTick();
      check(failing.calls == 2, "the next scheduled robotPeriodic must still run");
      failing.close();
    } finally {
      RobotController.setTimeSource(WasmRuntime::nowNanos);
      WasmRuntime.setClock(() -> (long) JsBridge.nowNanos());
      WasmRuntime.refresh();
    }
  }

  private static void expect(List<String> actual, String... expected) {
    if (!actual.equals(List.of(expected))) {
      throw new AssertionError("Lifecycle order " + actual + " != " + List.of(expected));
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static final class OpModeProbe extends OpModeRobot {
    final List<String> events = new ArrayList<>();

    @Override public void simulationInit() { events.add("simulationInit"); }
    @Override public void driverStationConnected() { events.add("connected"); }
    @Override public void disabledInit() { events.add("disabledInit"); }
    @Override public void disabledExit() { events.add("disabledExit"); }
    @Override public void disabledPeriodic() { events.add("disabledPeriodic"); }
    @Override public void nonePeriodic() { events.add("none"); }
    @Override public void robotPeriodic() { events.add("robot"); }
    @Override public void simulationPeriodic() { events.add("simulation"); }
  }

  private static final class TimedProbe extends TimedRobot {
    int periodics;
    int extras;
    int simulations;
    int initializations;
    int disabledInits;

    TimedProbe() {
      super(0.04);
      addPeriodic(() -> extras++, 0.03);
    }

    @Override public void simulationInit() { initializations++; }
    @Override public void disabledInit() { disabledInits++; }
    @Override public void robotPeriodic() { periodics++; }
    @Override public void simulationPeriodic() { simulations++; }
  }

  private static final class ThrowingProbe extends OpModeRobot {
    final IllegalStateException failure = new IllegalStateException("framework-periodic-failure");
    int calls;

    @Override public void robotPeriodic() {
      if (++calls == 1) {
        throw failure;
      }
    }
  }
}
