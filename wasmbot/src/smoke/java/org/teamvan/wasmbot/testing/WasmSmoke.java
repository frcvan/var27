package org.teamvan.wasmbot.testing;

import com.ctre.phoenix6.CANBus;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.graalvm.webimage.api.JS;
import org.teamvan.wasmbot.JsTelemetry;
import org.teamvan.wasmbot.WasmRuntime;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.SchedulerEvent;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.jni.EigenJNI;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.LinearSystem;
import org.wpilib.math.util.Nat;
import org.wpilib.simulation.DCMotorSim;
import org.wpilib.system.RobotController;
import org.wpilib.system.Timer;
import org.wpilib.telemetry.TelemetryEntry;

/** Native-image smoke entry: exercises the real WASM paths, not desktop substitutes or mocks. */
public final class WasmSmoke {
  private static final ArrayList<String> events = new ArrayList<>();
  private static final Mechanism mechanism = new Mechanism() {};
  private static final Mechanism childMechanism = new Mechanism() {};
  private static Command active;
  private static IllegalStateException expectedFailure;
  private static int robotInstances;
  private static int simulationInitCalls;

  public static void main(String... args) {
    checkDynamics();
    FrameworkSmoke.run();
    var backend = new JsTelemetry();
    backend.getEntry("/Review/Boolean").logBoolean(true, 0);
    backend.getEntry("/Review/Number").logDouble(1.25, 0);
    var text = backend.getEntry("/Review/String");
    text.logString("hello", "string", 0);
    text.setProperty("unit", "m");
    backend.getEntry("/Review/Booleans").logBooleanArray(new boolean[] {true, false}, 0);
    backend.getEntry("/Review/Shorts").logShortArray(new short[] {-3, 7}, 0);
    backend.getEntry("/Review/Ints").logIntArray(new int[] {-100000, 200000}, 0);
    backend.getEntry("/Review/Floats").logFloatArray(new float[] {1.25f, -2.5f}, 0);
    backend.getEntry("/Review/Doubles").logDoubleArray(new double[] {1.25, -2.5}, 0);
    backend.getEntry("/Review/Strings").logStringArray(new String[] {"a", "b"}, 0);
    backend.getEntry("/Review/Empty").logDoubleArray(new double[] {}, 0);
    backend.getEntry("/Review/SafeLong").logLong(9007199254740991L, 0);
    backend.getEntry("/Review/UnsafeLong").logLong(9007199254740992L, 0);
    backend
        .getEntry("/Review/Longs")
        .logLongArray(
            new long[] {0, 9007199254740991L, 9007199254740992L, Long.MIN_VALUE, Long.MAX_VALUE},
            0);
    backend.getEntry("/Review/Raw").logRaw(new byte[] {0, -1, 16}, "raw", 0);
    var first = new Pose2d(1.25, -2.5, new Rotation2d(0.5));
    var second = new Pose2d(3, 4, new Rotation2d(-1));
    var struct = backend.getEntry("/Review/Struct");
    struct.logStruct(first, Pose2d.struct, 0);
    struct.logStruct(second, Pose2d.struct, 0);
    backend
        .getEntry("/Review/Structs")
        .logStructArray(new Pose2d[] {first, second}, Pose2d.struct, 0);
    backend.getEntry("/Review/EmptyStructs").logStructArray(new Pose2d[] {}, Pose2d.struct, 0);
    var proto = backend.getEntry("/Review/Proto");
    proto.logProtobuf(first, Pose2d.proto, 0);
    proto.logProtobuf(second, Pose2d.proto, 0);
    RobotBase.startRobot(() -> new SmokeRobot(backend));
    Scheduler.getDefault()
        .addEventListener(
            event -> {
              if (event instanceof SchedulerEvent.CompletedWithError failure
                  && failure.command() == active) {
                if (failure.error() != expectedFailure) {
                  throw new AssertionError("Continuation changed the Java exception identity");
                }
                events.add("original-error");
              }
            });
    expose(
        () -> begin(1),
        () -> begin(2),
        () -> begin(3),
        () -> begin(4),
        () -> begin(5),
        () -> begin(6),
        () -> events.toString());
  }

  private static final class SmokeRobot extends OpModeRobot {
    private final TelemetryEntry periodicTicks;
    private final TelemetryEntry completedTicks;
    private final TelemetryEntry schedulerRuns;
    private final TelemetryEntry simulationTicks;
    private final TelemetryEntry simulationInits;
    private int periodicCalls;
    private int completedCalls;
    private int schedulerCalls;
    private int simulationCalls;

    private SmokeRobot(JsTelemetry backend) {
      if (!Boolean.getBoolean("wasmbot")) {
        throw new AssertionError("WASM detection must be available in the Robot constructor");
      }
      if (++robotInstances != 1) {
        throw new AssertionError("Robot must be constructed exactly once");
      }
      backend.getEntry("/Review/WasmbotInConstructor").logBoolean(true, 0);
      backend.getEntry("/Review/RobotInstances").logLong(robotInstances, 0);
      periodicTicks = backend.getEntry("/Review/RobotPeriodicTicks");
      completedTicks = backend.getEntry("/Review/CompletedPeriodicTicks");
      schedulerRuns = backend.getEntry("/Review/SchedulerTicks");
      simulationTicks = backend.getEntry("/Review/SimulationPeriodicTicks");
      simulationInits = backend.getEntry("/Review/SimulationInitCalls");
      periodicTicks.logLong(0, 0);
      completedTicks.logLong(0, 0);
      schedulerRuns.logLong(0, 0);
      simulationTicks.logLong(0, 0);
      Scheduler.getDefault()
          .addPeriodic(
              () -> {
                if (++schedulerCalls != periodicCalls) {
                  throw new AssertionError("Scheduler must run exactly once inside robotPeriodic");
                }
                schedulerRuns.logLong(schedulerCalls, WasmRuntime.nowNanos());
              });
    }

    @Override
    public void simulationInit() {
      if (++simulationInitCalls != 1) {
        throw new AssertionError("simulationInit must run exactly once");
      }
      simulationInits.logLong(simulationInitCalls, WasmRuntime.nowNanos());
    }

    @Override
    public void robotPeriodic() {
      if (simulationInitCalls != 1) {
        throw new AssertionError("simulationInit must precede robotPeriodic");
      }
      periodicTicks.logLong(++periodicCalls, WasmRuntime.nowNanos());
      Scheduler.getDefault().run();
      completedTicks.logLong(++completedCalls, WasmRuntime.nowNanos());
    }

    @Override
    public void simulationPeriodic() {
      simulationTicks.logLong(++simulationCalls, WasmRuntime.nowNanos());
    }
  }

  private static void near(double actual, double expected) {
    if (!Double.isFinite(actual) || Math.abs(actual - expected) > 1e-10) {
      throw new AssertionError(actual + " != " + expected);
    }
  }

  private static void checkDynamics() {
    double[] large = {-1e9, 0, 0, 0};
    EigenJNI.exp(large, 2, large);
    near(large[0], 0);
    near(large[3], 1);
    double[] nilpotent = {0, 2, 0, 0, 0, 3, 0, 0, 0};
    double[] result = new double[9];
    EigenJNI.exp(nilpotent, 3, result);
    double[] expected = {1, 2, 3, 0, 1, 3, 0, 0, 1};
    for (int i = 0; i < result.length; i++) {
      near(result[i], expected[i]);
    }
    var plant =
        new LinearSystem<>(
            new Matrix<>(Nat.N2(), Nat.N2(), new double[] {0, 1, 0, -2}),
            new Matrix<>(Nat.N2(), Nat.N1(), new double[] {0, 3}),
            new Matrix<>(Nat.N2(), Nat.N2(), new double[] {1, 0, 0, 1}),
            new Matrix<>(Nat.N2(), Nat.N1()));
    var sim = new DCMotorSim(plant, DCMotor.getKrakenX60Foc(1));
    sim.setInputVoltage(4);
    double time = 0;
    for (int i = 0; i < 101; i++) {
      double dt = i < 100 ? 0.02 : 0.01;
      time += dt;
      sim.update(dt);
      near(sim.getAngularVelocity(), 6 * (1 - Math.exp(-2 * time)));
      near(sim.getAngularPosition(), 6 * (time - (1 - Math.exp(-2 * time)) / 2));
    }
    var clamped = new DCMotorSim(plant, DCMotor.getKrakenX60Foc(1));
    clamped.setInputVoltage(100);
    clamped.update(0.02);
    near(clamped.getAngularVelocity(), 18 * (1 - Math.exp(-0.04)));
    RobotController.setTimeSource(() -> 7_000_000_000L);
    near(Timer.getTimestamp(), 7);
    near(Timer.getMonotonicTimestamp(), WasmRuntime.nowNanos() / 1e9);
    RobotController.setTimeSource(WasmRuntime::nowNanos);
    var bus = new CANBus("canivore");
    if (!bus.equals(bus.clone())
        || bus.hashCode() != bus.clone().hashCode()
        || !bus.toString().equals("canivore")) {
      throw new AssertionError("CANBus value semantics");
    }
  }

  @JS(
      args = {
        "beginYield",
        "beginPark",
        "cancel",
        "beginAwait",
        "beginFailure",
        "reschedule",
        "state"
      },
      value =
          "globalThis.wasmProbe = {beginYield, beginPark, cancel, beginAwait, beginFailure, reschedule, state};")
  private static native void expose(
      Runnable beginYield,
      Runnable beginPark,
      Runnable cancel,
      Runnable beginAwait,
      Runnable beginFailure,
      Runnable reschedule,
      Supplier<String> state);

  private static void begin(int scenario) {
    var scheduler = Scheduler.getDefault();
    if (scenario == 3) {
      scheduler.cancel(active);
      return;
    }
    if (scenario == 6) {
      scheduler.schedule(active);
      return;
    }
    events.clear();
    if (scenario == 1) {
      active =
          mechanism
              .run(
                  co -> {
                    int local = 41;
                    events.add("first");
                    co.yield();
                    events.add("local=" + (++local));
                    co.yield();
                    events.add("last=" + (++local));
                  })
              .named("Yield smoke");
    } else if (scenario == 2) {
      active =
          mechanism
              .run(
                  co -> {
                    events.add("parked");
                    co.park();
                  })
              .whenCanceled(() -> events.add("canceled"))
              .named("Cancel smoke");
    } else if (scenario == 4) {
      var child =
          childMechanism
              .run(
                  co -> {
                    events.add("child-start");
                    co.yield();
                    events.add("child-end");
                  })
              .named("Await child");
      active =
          mechanism
              .run(
                  co -> {
                    events.add("parent-start");
                    co.await(child);
                    events.add("parent-end");
                  })
              .named("Await parent");
    } else if (scenario == 5) {
      expectedFailure = new IllegalStateException("probe-target-error");
      active =
          mechanism
              .run(
                  co -> {
                    events.add("throws");
                    throw expectedFailure;
                  })
              .named("Failure smoke");
    } else {
      throw new IllegalArgumentException("Unknown scenario " + scenario);
    }
    scheduler.schedule(active);
  }
}
